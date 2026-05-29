package com.vida.apirest.servicies;

import com.vida.apirest.dto.ariticulo.ArticuloCompactResponse;
import com.vida.apirest.dto.ariticulo.ArticuloCreateRequest;
import com.vida.apirest.dto.ariticulo.ArticuloFiltrosResponse;
import com.vida.apirest.dto.ariticulo.ArticuloParaVentaResponse;
import com.vida.apirest.dto.ariticulo.ArticuloTablaRowResponse;
import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.dto.ariticulo.VariantCreateRequest;
import com.vida.apirest.dto.ariticulo.VarianteCompactResponse;
import com.vida.apirest.model.almacen.Deposito;
import com.vida.apirest.model.almacen.Stock;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.articulo.*;
import com.vida.apirest.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

@Service
@RequiredArgsConstructor
public class ArticuloService {

    private final ArticuloRepository articuloRepository;
    private final MarcaRepository marcaRepository;
    private final CategoriaRepository categoriaRepository;
    private final GeneroRepository generoRepository;
    private final ColorRepository colorRepository;
    private final TalleRepository talleRepository;
    private final TaxonRepository taxonRepository;
    private final TaxonArticuloRepository taxonArticuloRepository;
    private final VarianteArticuloRepository varianteArticuloRepository;
    private final HistorialPrecioRepository historialPrecioRepository;
    private final StockRepository stockRepository;
    private final DepositoRepository depositoRepository;
    private final SucursalRepository sucursalRepository;
    private final ArticuloTablaQueryRepository articuloTablaQueryRepository;

    @Transactional(readOnly = true)
    public List<ArticuloCompactResponse> findAllCompact() {
        return articuloRepository.findAllWithDetalle().stream()
                .map(this::toCompactResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<ArticuloTablaRowResponse> findTablaPage(
            String categoria,
            String subCategoria,
            String genero,
            String marca,
            String q,
            int page,
            int size
    ) {
        return articuloTablaQueryRepository.findTablaPage(
                categoria, subCategoria, genero, marca, q, page, size);
    }

    @Transactional(readOnly = true)
    public ArticuloFiltrosResponse obtenerFiltrosTabla() {
        return new ArticuloFiltrosResponse(
                categoriaRepository.findDistinctNombres(),
                taxonRepository.findDistinctNombresUsadosEnArticulos(),
                generoRepository.findDistinctNombres(),
                marcaRepository.findDistinctNombres()
        );
    }

    private String obtenerSubCategoria(Articulo articulo) {
        if (articulo.getTaxones() == null || articulo.getTaxones().isEmpty()) {
            return null;
        }
        return articulo.getTaxones().stream()
                .map(TaxonArticulo::getTaxon)
                .filter(Objects::nonNull)
                .map(Taxon::getNombre)
                .findFirst()
                .orElse(null);
    }

    private String obtenerSubCategoriaVariante(VarianteArticulo variante, Articulo articulo) {
        return obtenerSubCategoria(articulo);
    }

    private Taxon resolverTaxon(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        String normalizado = nombre.trim();
        return taxonRepository.findByNombre(normalizado)
                .orElseGet(() -> {
                    Taxon nuevo = new Taxon();
                    nuevo.setNombre(normalizado);
                    nuevo.setNivel(1);
                    return taxonRepository.save(nuevo);
                });
    }

    private void vincularTaxonArticulo(Long articuloId, Taxon taxon) {
        if (taxon == null) {
            return;
        }
        if (!taxonArticuloRepository.existsByArticuloIdAndTaxonId(articuloId, taxon.getId())) {
            TaxonArticulo taxonArticulo = new TaxonArticulo();
            taxonArticulo.setArticuloId(articuloId);
            taxonArticulo.setTaxonId(taxon.getId());
            taxonArticuloRepository.save(taxonArticulo);
        }
    }

    private boolean coincideFiltros(
            ArticuloTablaRowResponse fila,
            String categoria,
            String subCategoria,
            String genero,
            String marca
    ) {
        if (categoria != null && !categoria.isBlank()
                && !categoria.equalsIgnoreCase(nullToDash(fila.getCategoria()))) {
            return false;
        }
        if (subCategoria != null && !subCategoria.isBlank()
                && !subCategoria.equalsIgnoreCase(nullToDash(fila.getSubCategoria()))) {
            return false;
        }
        if (genero != null && !genero.isBlank()
                && !genero.equalsIgnoreCase(nullToDash(fila.getGenero()))) {
            return false;
        }
        if (marca != null && !marca.isBlank()
                && !marca.equalsIgnoreCase(nullToDash(fila.getMarca()))) {
            return false;
        }
        return true;
    }

    private String nullToDash(String value) {
        return value == null ? "" : value;
    }

    private List<String> valoresUnicos(
            List<ArticuloTablaRowResponse> filas,
            java.util.function.Function<ArticuloTablaRowResponse, String> extractor
    ) {
        return filas.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Transactional
    public Articulo createArticulo(ArticuloCreateRequest request) {
        if (request.getVariantes() == null || request.getVariantes().isEmpty()) {
            throw new RuntimeException("Debe agregar al menos una variante (talle, color, precio y cantidad)");
        }

        // Buscar o crear Marca
        Marca marca = marcaRepository.findByNombre(request.getMarca())
                .orElseGet(() -> {
                    Marca newMarca = new Marca();
                    newMarca.setNombre(request.getMarca());
                    return marcaRepository.save(newMarca);
                });

        // Buscar o crear Categoria
        Categoria categoria = categoriaRepository.findByNombre(request.getCategoria())
                .orElseGet(() -> {
                    Categoria newCategoria = new Categoria();
                    newCategoria.setNombre(request.getCategoria());
                    return categoriaRepository.save(newCategoria);
                });

        // Buscar o crear Genero
        Genero genero = generoRepository.findByNombre(request.getGenero())
                .orElseGet(() -> {
                    Genero newGenero = new Genero();
                    newGenero.setNombre(request.getGenero());
                    return generoRepository.save(newGenero);
                });


        // Crear Articulo
        Articulo articulo = new Articulo();
        articulo.setMarcaId(marca.getId());
        articulo.setCategoriaId(categoria.getId());
        articulo.setGeneroId(genero.getId());
        articulo.setCodigo(request.getCodigo());
        articulo.setModelo(request.getModelo());
        articulo.setDescripcion(null);
        articulo = articuloRepository.save(articulo);

        Taxon subCategoriaArticulo = resolverTaxon(request.getSubCategoria());
        vincularTaxonArticulo(articulo.getId(), subCategoriaArticulo);

        // Obtener deposito y sucursal
        Deposito deposito;
        Sucursal sucursal;
        
        if (request.getDepositoId() != null) {
            deposito = depositoRepository.findById(request.getDepositoId())
                    .orElseThrow(() -> new RuntimeException("Depósito no encontrado con ID: " + request.getDepositoId()));
        } else {
            deposito = depositoRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("No hay depósitos disponibles. Por favor, crea uno primero."));
        }
        
        if (request.getSucursalId() != null) {
            sucursal = sucursalRepository.findById(request.getSucursalId())
                    .orElseThrow(() -> new RuntimeException("Sucursal no encontrada con ID: " + request.getSucursalId()));
        } else {
            sucursal = sucursalRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("No hay sucursales disponibles. Por favor, crea una primero."));
        }

        for (var variantReq : request.getVariantes()) {
            crearVariante(articulo, variantReq, deposito, sucursal);
        }

        return articulo;
    }

    @Transactional
    public VarianteCompactResponse agregarVariante(
            Long articuloId,
            VariantCreateRequest request,
            Long depositoId,
            Long sucursalId
    ) {
        Articulo articulo = articuloRepository.findById(articuloId)
                .orElseThrow(() -> new RuntimeException("Artículo no encontrado con ID: " + articuloId));

        Deposito deposito = depositoId != null
                ? depositoRepository.findById(depositoId)
                .orElseThrow(() -> new RuntimeException("Depósito no encontrado con ID: " + depositoId))
                : depositoRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No hay depósitos disponibles. Por favor, crea uno primero."));

        Sucursal sucursal = sucursalId != null
                ? sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada con ID: " + sucursalId))
                : sucursalRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No hay sucursales disponibles. Por favor, crea una primero."));

        VarianteArticulo variante = crearVariante(articulo, request, deposito, sucursal);
        return toVarianteCompact(articulo.getId(), variante);
    }

    private VarianteArticulo crearVariante(
            Articulo articulo,
            VariantCreateRequest variantReq,
            Deposito deposito,
            Sucursal sucursal
    ) {
        if (variantReq.getPrecio() == null || variantReq.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El precio de venta debe ser mayor a cero");
        }
        if (variantReq.getCantidad() == null || variantReq.getCantidad() < 0) {
            throw new RuntimeException("La cantidad debe ser cero o mayor");
        }

        Talle.Pais pais;
        try {
            pais = Talle.Pais.valueOf(variantReq.getPais().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("País de talle inválido. Valores válidos: AR, UK, BR, US, EU");
        }

        Talle talle = talleRepository.findByPaisAndNumero(pais, variantReq.getTalleNumero())
                .orElseGet(() -> {
                    Talle newTalle = new Talle();
                    newTalle.setPais(pais);
                    newTalle.setNumero(variantReq.getTalleNumero());
                    newTalle.setDescripcion("Talle " + variantReq.getTalleNumero() + " - " + pais);
                    return talleRepository.save(newTalle);
                });

        Color varianteColor = colorRepository.findByNombre(variantReq.getColor())
                .orElseGet(() -> {
                    Color newColor = new Color();
                    newColor.setNombre(variantReq.getColor());
                    return colorRepository.save(newColor);
                });

        if (varianteArticuloRepository.existsByArticuloIdAndColorIdAndTalleId(
                articulo.getId(), varianteColor.getId(), talle.getId())) {
            throw new RuntimeException("Ya existe una variante con ese talle y color para este artículo");
        }

        if (variantReq.getCodigoBarras() != null && !variantReq.getCodigoBarras().isBlank()) {
            String codigoBarras = variantReq.getCodigoBarras().trim();
            if (varianteArticuloRepository.existsByCodigoBarras(codigoBarras)) {
                throw new RuntimeException("El código de barras ya está registrado: " + codigoBarras);
            }
        }

        VarianteArticulo variante = new VarianteArticulo();
        variante.setArticuloId(articulo.getId());
        variante.setColorId(varianteColor.getId());
        variante.setTalleId(talle.getId());
        if (variantReq.getCodigoBarras() != null && !variantReq.getCodigoBarras().isBlank()) {
            variante.setCodigoBarras(variantReq.getCodigoBarras().trim());
        }
        variante = varianteArticuloRepository.save(variante);

        HistorialPrecio historial = new HistorialPrecio();
        historial.setVarianteArticuloId(variante.getId());
        historial.setPrecioNuevo(variantReq.getPrecio());
        if (variantReq.getCosto() != null && variantReq.getCosto().compareTo(BigDecimal.ZERO) >= 0) {
            historial.setCostoNuevo(variantReq.getCosto());
        }
        historial.setFecha(LocalDateTime.now());
        historialPrecioRepository.save(historial);

        Stock stock = new Stock();
        stock.setDeposito(deposito);
        stock.setSucursal(sucursal);
        stock.setArticulo(articulo);
        stock.setVariante(variante);
        stock.setCantidadActual(variantReq.getCantidad());
        stock.setCantidadDisponible(variantReq.getCantidad());
        stockRepository.save(stock);

        return variante;
    }

    private VarianteCompactResponse toVarianteCompact(Long articuloId, VarianteArticulo variante) {
        VarianteCompactResponse dto = new VarianteCompactResponse();
        dto.setId(variante.getId());
        dto.setColor(variante.getColor() != null ? variante.getColor().getNombre() : null);
        dto.setTalle(variante.getTalle() != null ? variante.getTalle().getNumero() : null);
        dto.setCodigoBarras(variante.getCodigoBarras());
        dto.setPrecio(getPrecioActual(variante.getId()));
        dto.setCantidad(getCantidadDisponibleForVariante(articuloId, variante.getId()));
        return dto;
    }

    public List<Articulo> searchArticulos(String codigo, String marca, String talleNumero, String color, String categoria, String modelo, String genero) {
        Specification<Articulo> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (codigo != null && !codigo.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("codigo")), codigo.toLowerCase()));
            }

            if (marca != null && !marca.isBlank()) {
                Join<Object, Object> marcaJoin = root.join("marca", JoinType.LEFT);
                predicates.add(cb.like(cb.lower(marcaJoin.get("nombre")), "%" + marca.toLowerCase() + "%"));
            }

            if (categoria != null && !categoria.isBlank()) {
                Join<Object, Object> catJoin = root.join("categoria", JoinType.LEFT);
                predicates.add(cb.like(cb.lower(catJoin.get("nombre")), "%" + categoria.toLowerCase() + "%"));
            }

            if (modelo != null && !modelo.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("modelo")), "%" + modelo.toLowerCase() + "%"));
            }

            if (genero != null && !genero.isBlank()) {
                Join<Object, Object> genJoin = root.join("genero", JoinType.LEFT);
                predicates.add(cb.like(cb.lower(genJoin.get("nombre")), "%" + genero.toLowerCase() + "%"));
            }

            if ((talleNumero != null && !talleNumero.isBlank()) || (color != null && !color.isBlank())) {
                // join variantes -> talle/color
                Join<Object, Object> variantesJoin = root.join("variantes", JoinType.LEFT);
                if (talleNumero != null && !talleNumero.isBlank()) {
                    Join<Object, Object> talleJoin = variantesJoin.join("talle", JoinType.LEFT);
                    predicates.add(cb.equal(talleJoin.get("numero"), talleNumero));
                }
                if (color != null && !color.isBlank()) {
                    Join<Object, Object> colorJoin = variantesJoin.join("color", JoinType.LEFT);
                    predicates.add(cb.like(cb.lower(colorJoin.get("nombre")), "%" + color.toLowerCase() + "%"));
                }
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return articuloRepository.findAll(spec);
    }

    @Transactional(readOnly = true)
    public Articulo getArticuloById(Long id) {
        return articuloRepository.findById(id).orElseThrow(() -> new RuntimeException("Artículo no encontrado con id: " + id));
    }

    @Transactional(readOnly = true)
    public ArticuloCompactResponse getCompactById(Long id) {
        Articulo articulo = articuloRepository.findByIdWithDetalle(id)
                .orElseThrow(() -> new RuntimeException("Artículo no encontrado con id: " + id));
        return toCompactResponse(articulo);
    }

    public Articulo getByCodigo(String codigo) {
        return articuloRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RuntimeException("Artículo no encontrado con código: " + codigo));
    }

    @Transactional(readOnly = true)
    public ArticuloCompactResponse getByCodigoCompact(String codigo) {
        return toCompactResponse(getByCodigo(codigo));
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

        if (articulo.getTaxones() != null) {
            articulo.getTaxones().stream()
                    .map(TaxonArticulo::getTaxon)
                    .filter(Objects::nonNull)
                    .map(Taxon::getNombre)
                    .findFirst()
                    .ifPresent(response::setSubCategoria);
        }

        List<VarianteCompactResponse> variants = new ArrayList<>();
        if (articulo.getVariantes() != null) {
            for (VarianteArticulo variante : articulo.getVariantes()) {
                VarianteCompactResponse variantDto = new VarianteCompactResponse();
                variantDto.setId(variante.getId());
                variantDto.setColor(variante.getColor() != null ? variante.getColor().getNombre() : null);
                variantDto.setTalle(variante.getTalle() != null ? variante.getTalle().getNumero() : null);
                variantDto.setCodigoBarras(variante.getCodigoBarras());
                variantDto.setPrecio(getPrecioActual(variante.getId()));
                variantDto.setCantidad(getCantidadDisponibleForVariante(articulo.getId(), variante.getId()));
                variants.add(variantDto);
            }
        }
        response.setVariantes(variants);
        return response;
    }

    private BigDecimal getPrecioActual(Long varianteId) {
        if (varianteId == null) {
            return null;
        }
        return historialPrecioRepository.findFirstByVarianteArticuloIdOrderByFechaDesc(varianteId)
                .map(HistorialPrecio::getPrecioNuevo)
                .orElse(null);
    }

    private Integer getCantidadDisponibleForVariante(Long articuloId, Long varianteId) {
        List<Stock> stocks = stockRepository.findAllByArticulo_IdAndVariante_Id(articuloId, varianteId);
        return stocks.stream()
                .map(Stock::getCantidadDisponible)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);
    }

    public List<Articulo> getByMarca(String marca) {
        return articuloRepository.findAllByMarcaNombreContainingIgnoreCase(marca);
    }

    public List<Articulo> getByTalle(String talleNumero) {
        return articuloRepository.findAllByTalleNumero(talleNumero);
    }

    public List<Articulo> getByColor(String color) {
        return articuloRepository.findAllByColorNombreContaining(color);
    }

    @Transactional(readOnly = true)
    public PageResponse<ArticuloParaVentaResponse> findParaVentaPage(
            Long sucursalId, String q, int page, int size) {
        if (sucursalId == null) {
            throw new RuntimeException("Sucursal requerida para listar artículos de venta");
        }
        if (!sucursalRepository.existsById(sucursalId)) {
            throw new RuntimeException("Sucursal no encontrada con ID: " + sucursalId);
        }
        return articuloTablaQueryRepository.findParaVentaPage(sucursalId, q, page, size);
    }

    private Integer getCantidadDisponibleEnSucursal(Long articuloId, Long varianteId, Long sucursalId) {
        return stockRepository.findByVarianteIdAndSucursalId(varianteId, sucursalId)
                .map(Stock::getCantidadDisponible)
                .orElse(0);
    }
}