package com.vida.apirest.servicies;

import com.vida.apirest.dto.ariticulo.ArticuloCodigoSugerenciaResponse;
import com.vida.apirest.dto.ariticulo.ArticuloCompactResponse;
import com.vida.apirest.dto.ariticulo.ArticuloFiltrosResponse;
import com.vida.apirest.dto.ariticulo.ArticuloParaVentaResponse;
import com.vida.apirest.dto.ariticulo.ArticuloTablaRowResponse;
import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.model.almacen.Stock;
import com.vida.apirest.model.articulo.Articulo;
import com.vida.apirest.dto.ariticulo.VarianteCompactResponse;
import com.vida.apirest.model.articulo.VarianteArticulo;
import com.vida.apirest.repositories.ArticuloRepository;
import com.vida.apirest.repositories.ArticuloTablaQueryRepository;
import com.vida.apirest.repositories.CategoriaRepository;
import com.vida.apirest.repositories.ColorRepository;
import com.vida.apirest.repositories.GeneroRepository;
import com.vida.apirest.repositories.HistorialPrecioRepository;
import com.vida.apirest.repositories.MarcaRepository;
import com.vida.apirest.repositories.StockRepository;
import com.vida.apirest.repositories.SucursalRepository;
import com.vida.apirest.repositories.TalleRepository;
import com.vida.apirest.repositories.TaxonRepository;
import com.vida.apirest.security.SucursalScopeService;
import com.vida.apirest.utils.ClasificacionArticuloSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
public class ArticuloConsultaService {

    private final ArticuloRepository articuloRepository;
    private final ArticuloTablaQueryRepository articuloTablaQueryRepository;
    private final CategoriaRepository categoriaRepository;
    private final GeneroRepository generoRepository;
    private final MarcaRepository marcaRepository;
    private final TalleRepository talleRepository;
    private final ColorRepository colorRepository;
    private final TaxonRepository taxonRepository;
    private final HistorialPrecioRepository historialPrecioRepository;
    private final StockRepository stockRepository;
    private final SucursalRepository sucursalRepository;
    private final PromocionService promocionService;
    private final SucursalScopeService sucursalScopeService;
    private final ClasificacionArticuloSupport clasificacionArticuloSupport;

    @Transactional(readOnly = true)
    public PageResponse<ArticuloTablaRowResponse> findTablaPage(
            String categoria,
            String subCategoria,
            List<String> clasificaciones,
            String genero,
            String marca,
            String talle,
            String color,
            String q,
            Long depositoId,
            int page,
            int size
    ) {
        return articuloTablaQueryRepository.findTablaPage(
                categoria, subCategoria, clasificaciones, genero, marca, talle, color, q, depositoId, page, size);
    }

    @Transactional(readOnly = true)
    public ArticuloFiltrosResponse obtenerFiltrosTabla() {
        List<String> subCategorias = taxonRepository.findDistinctSubCategoriaNombresUsadosEnArticulos();
        List<String> clasificaciones = taxonRepository.findDistinctClasificacionNombresUsadosEnArticulos();
        return new ArticuloFiltrosResponse(
                categoriaRepository.findDistinctNombres(),
                subCategorias,
                clasificaciones,
                generoRepository.findDistinctNombres(),
                marcaRepository.findDistinctNombres(),
                talleRepository.findDistinctNumeros(),
                colorRepository.findDistinctNombres()
        );
    }

    @Transactional(readOnly = true)
    public Articulo loadWithDetalle(Long id) {
        Articulo articulo = articuloRepository.findByIdWithVariantes(id)
                .orElseThrow(() -> new RuntimeException("Artículo no encontrado con id: " + id));
        articuloRepository.findByIdWithTaxones(id)
                .ifPresent(conTaxones -> {
                    if (conTaxones.getTaxones() != null) {
                        conTaxones.getTaxones().size();
                    }
                });
        return articulo;
    }

    @Transactional(readOnly = true)
    public List<ArticuloCodigoSugerenciaResponse> buscarSugerenciasCodigo(String q, int limit) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        List<Object[]> rows = articuloRepository.buscarSugerenciasCodigoActivos(
                q.trim(), PageRequest.of(0, safeLimit));
        List<ArticuloCodigoSugerenciaResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new ArticuloCodigoSugerenciaResponse(
                    (Long) row[0],
                    (String) row[1],
                    (String) row[2],
                    (String) row[3]));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ArticuloCompactResponse getCompactByCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new RuntimeException("Código requerido");
        }
        Articulo articulo = articuloRepository.findByCodigo(codigo.trim())
                .orElseThrow(() -> new RuntimeException("No existe un artículo con el código: " + codigo.trim()));
        return getCompactById(articulo.getId());
    }

    @Transactional(readOnly = true)
    public ArticuloCompactResponse getCompactById(Long id) {
        return toCompactResponse(loadWithDetalle(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ArticuloParaVentaResponse> findParaVentaPage(
            Long sucursalId,
            String categoria,
            String subCategoria,
            List<String> clasificaciones,
            String genero,
            String marca,
            String talle,
            String color,
            String q,
            int page,
            int size
    ) {
        if (sucursalId == null) {
            throw new RuntimeException("Sucursal requerida para listar artículos de venta");
        }
        sucursalScopeService.assertCanUse(sucursalId);
        if (!sucursalRepository.existsById(sucursalId)) {
            throw new RuntimeException("Sucursal no encontrada con ID: " + sucursalId);
        }
        PageResponse<ArticuloParaVentaResponse> resultado =
                articuloTablaQueryRepository.findParaVentaPage(
                        sucursalId, categoria, subCategoria, clasificaciones, genero, marca, talle, color, q, page, size);
        aplicarPreciosPromocionales(resultado.getContent());
        return resultado;
    }

    public ArticuloCompactResponse toCompactResponse(Articulo articulo) {
        ArticuloCompactResponse response = new ArticuloCompactResponse();
        response.setId(articulo.getId());
        response.setCodigo(articulo.getCodigo());
        response.setModelo(articulo.getModelo());
        response.setDescripcion(articulo.getDescripcion());
        response.setCategoria(articulo.getCategoria() != null ? articulo.getCategoria().getNombre() : null);
        response.setGenero(articulo.getGenero() != null ? articulo.getGenero().getNombre() : null);
        response.setMarca(articulo.getMarca() != null ? articulo.getMarca().getNombre() : null);

        response.setSubCategoria(clasificacionArticuloSupport.obtenerSubCategoria(articulo));
        response.setClasificaciones(clasificacionArticuloSupport.obtenerClasificaciones(articulo));

        List<VarianteCompactResponse> variants = new ArrayList<>();
        if (articulo.getVariantes() != null) {
            List<Long> varianteIds = articulo.getVariantes().stream()
                    .filter(v -> v.getEstado() != VarianteArticulo.EstadoVariante.INACTIVO)
                    .map(VarianteArticulo::getId)
                    .toList();
            Map<Long, BigDecimal> precios = loadPreciosActuales(varianteIds);

            for (VarianteArticulo variante : articulo.getVariantes()) {
                if (variante.getEstado() == VarianteArticulo.EstadoVariante.INACTIVO) {
                    continue;
                }
                com.vida.apirest.dto.ariticulo.VarianteCompactResponse variantDto =
                        new VarianteCompactResponse();
                variantDto.setId(variante.getId());
                variantDto.setColor(variante.getColor() != null ? variante.getColor().getNombre() : null);
                variantDto.setTalle(variante.getTalle() != null ? variante.getTalle().getNumero() : null);
                variantDto.setPais(variante.getTalle() != null && variante.getTalle().getPais() != null
                        ? variante.getTalle().getPais().name()
                        : null);
                variantDto.setCodigoBarras(variante.getCodigoBarras());
                variantDto.setPrecio(precios.get(variante.getId()));
                variantDto.setCantidad(getCantidadDisponibleForVariante(articulo.getId(), variante.getId()));
                variants.add(variantDto);
            }
        }
        response.setVariantes(variants);
        return response;
    }

    private Map<Long, BigDecimal> loadPreciosActuales(Collection<Long> varianteIds) {
        if (varianteIds == null || varianteIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, BigDecimal> precios = new HashMap<>();
        for (Object[] row : historialPrecioRepository.findLatestPreciosByVarianteIds(varianteIds)) {
            precios.put((Long) row[0], (BigDecimal) row[1]);
        }
        return precios;
    }

    private Integer getCantidadDisponibleForVariante(Long articuloId, Long varianteId) {
        return stockRepository.findAllByArticulo_IdAndVariante_Id(articuloId, varianteId).stream()
                .map(Stock::getCantidadDisponible)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);
    }

    private void aplicarPreciosPromocionales(List<ArticuloParaVentaResponse> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<Long, BigDecimal> preciosLista = new HashMap<>();
        for (ArticuloParaVentaResponse item : items) {
            if (item.getVarianteId() != null && item.getPrecio() != null) {
                preciosLista.put(item.getVarianteId(), item.getPrecio());
            }
        }
        Map<Long, BigDecimal> preciosPromo = promocionService.resolverPreciosVenta(preciosLista);
        for (ArticuloParaVentaResponse item : items) {
            BigDecimal precioLista = item.getPrecio();
            BigDecimal precioPromo = preciosPromo.get(item.getVarianteId());
            if (precioPromo != null && precioLista != null && precioPromo.compareTo(precioLista) < 0) {
                item.setPrecioLista(precioLista);
                item.setPrecio(precioPromo);
                item.setEnPromocion(true);
            } else {
                item.setPrecioLista(null);
                item.setEnPromocion(false);
            }
        }
    }
}
