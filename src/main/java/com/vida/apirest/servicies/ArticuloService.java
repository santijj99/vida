package com.vida.apirest.servicies;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vida.apirest.dto.ariticulo.ArticuloCompactResponse;
import com.vida.apirest.dto.ariticulo.ArticuloCreateRequest;
import com.vida.apirest.dto.ariticulo.ArticuloFiltrosResponse;
import com.vida.apirest.dto.ariticulo.ArticuloParaVentaResponse;
import com.vida.apirest.dto.ariticulo.ArticuloTablaRowResponse;
import com.vida.apirest.dto.ariticulo.ArticuloUpdateRequest;
import com.vida.apirest.dto.ariticulo.VariantCreateRequest;
import com.vida.apirest.dto.ariticulo.VarianteCompactResponse;
import com.vida.apirest.dto.ariticulo.VarianteUpdateRequest;
import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.model.almacen.Deposito;
import com.vida.apirest.model.almacen.Stock;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.articulo.Articulo;
import com.vida.apirest.model.articulo.Categoria;
import com.vida.apirest.model.articulo.Color;
import com.vida.apirest.model.articulo.Genero;
import com.vida.apirest.model.articulo.HistorialPrecio;
import com.vida.apirest.model.articulo.Marca;
import com.vida.apirest.model.articulo.Talle;
import com.vida.apirest.model.articulo.Taxon;
import com.vida.apirest.model.articulo.TaxonArticulo;
import com.vida.apirest.model.articulo.VarianteArticulo;
import com.vida.apirest.repositories.ArticuloRepository;
import com.vida.apirest.repositories.ArticuloTablaQueryRepository;
import com.vida.apirest.repositories.CategoriaRepository;
import com.vida.apirest.repositories.ColorRepository;
import com.vida.apirest.repositories.DepositoRepository;
import com.vida.apirest.repositories.GeneroRepository;
import com.vida.apirest.repositories.HistorialPrecioRepository;
import com.vida.apirest.repositories.MarcaRepository;
import com.vida.apirest.repositories.StockRepository;
import com.vida.apirest.repositories.SucursalRepository;
import com.vida.apirest.repositories.TalleRepository;
import com.vida.apirest.repositories.TaxonArticuloRepository;
import com.vida.apirest.repositories.TaxonRepository;
import com.vida.apirest.repositories.VarianteArticuloRepository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;

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
                marcaRepository.findDistinctNombres(),
                talleRepository.findDistinctNumeros(), // Añadimos los talles
                colorRepository.findDistinctNombres()  // Añadimos los colores
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

        Marca marca = marcaRepository.findByNombre(request.getMarca())
                .orElseGet(() -> {
                    Marca newMarca = new Marca();
                    newMarca.setNombre(request.getMarca());
                    return marcaRepository.save(newMarca);
                });

        Categoria categoria = categoriaRepository.findByNombre(request.getCategoria())
                .orElseGet(() -> {
                    Categoria newCategoria = new Categoria();
                    newCategoria.setNombre(request.getCategoria());
                    return categoriaRepository.save(newCategoria);
                });

        Genero genero = generoRepository.findByNombre(request.getGenero())
                .orElseGet(() -> {
                    Genero newGenero = new Genero();
                    newGenero.setNombre(request.getGenero());
                    return generoRepository.save(newGenero);
                });

        Articulo articulo = new Articulo();
        articulo.setMarcaId(marca.getId());
        articulo.setCategoriaId(categoria.getId());
        articulo.setGeneroId(genero.getId());
        articulo.setCodigo(request.getCodigo());
        articulo.setModelo(request.getModelo());
        articulo.setDescripcion(request.getDescripcion());
        articulo.setEstado(Articulo.EstadoProducto.ACTIVO);
        articulo = articuloRepository.save(articulo);

        Taxon subCategoriaArticulo = resolverTaxon(request.getSubCategoria());
        vincularTaxonArticulo(articulo.getId(), subCategoriaArticulo);

        Deposito deposito;
        Sucursal sucursal;
        
        if (request.getDepositoId() != null) {
            deposito = depositoRepository.findById(request.getDepositoId())
                    .orElseThrow(() -> new RuntimeException("Depósito no encontrado con ID: " + request.getDepositoId()));
        } else {
            deposito = depositoRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("No hay depósitos disponibles."));
        }
        
        if (request.getSucursalId() != null) {
            sucursal = sucursalRepository.findById(request.getSucursalId())
                    .orElseThrow(() -> new RuntimeException("Sucursal no encontrada con ID: " + request.getSucursalId()));
        } else {
            sucursal = sucursalRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("No hay sucursales disponibles."));
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
                .orElseThrow(() -> new RuntimeException("No hay depósitos disponibles."));

        Sucursal sucursal = sucursalId != null
                ? sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada con ID: " + sucursalId))
                : sucursalRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No hay sucursales disponibles."));

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
        boolean existeActiva = varianteArticuloRepository.existsByArticuloIdAndColorIdAndTalleIdAndEstado(
                articulo.getId(), varianteColor.getId(), talle.getId(), VarianteArticulo.EstadoVariante.ACTIVO);
        
        if (existeActiva) {
            throw new RuntimeException("Ya existe una variante activa con ese talle y color");
        }

        VarianteArticulo variante = new VarianteArticulo();
        variante.setArticuloId(articulo.getId());
        variante.setColorId(varianteColor.getId());
        variante.setTalleId(talle.getId());
        variante.setEstado(VarianteArticulo.EstadoVariante.ACTIVO);
        
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

            // ¡NUEVO! Evitar devolver artículos archivados
            predicates.add(cb.notEqual(root.get("estado"), Articulo.EstadoProducto.ARCHIVADO));

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
                Join<Object, Object> variantesJoin = root.join("variantes", JoinType.LEFT);
                
                // ¡NUEVO! Evitar buscar en variantes inactivas
                predicates.add(cb.notEqual(variantesJoin.get("estado"), VarianteArticulo.EstadoVariante.INACTIVO));

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
                if (variante.getEstado() == VarianteArticulo.EstadoVariante.INACTIVO) {
                    continue;
                }

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

    @Transactional
    public void softDeleteArticulo(Long id) {
        Articulo articulo = getArticuloById(id);
        articulo.setEstado(Articulo.EstadoProducto.ARCHIVADO);
        if (articulo.getVariantes() != null) {
            articulo.getVariantes().forEach(v -> v.setEstado(VarianteArticulo.EstadoVariante.INACTIVO));
        }
        
        articuloRepository.save(articulo);
    }

    @Transactional
    public void softDeleteVariante(Long articuloId, Long varianteId) {
        Articulo articulo = getArticuloById(articuloId);
        
        boolean encontrada = false;
        if (articulo.getVariantes() != null) {
            for (VarianteArticulo v : articulo.getVariantes()) {
                if (v.getId().equals(varianteId)) {
                    v.setEstado(VarianteArticulo.EstadoVariante.INACTIVO);
                    encontrada = true;
                    break;
                }
            }
        }
        
        if (!encontrada) {
            throw new RuntimeException("Variante no encontrada en el artículo especificado.");
        }
        
        articuloRepository.save(articulo);
    }

    @Transactional
    public Articulo updateArticulo(Long id, ArticuloUpdateRequest request) {
        Articulo articulo = getArticuloById(id);

        if (request.getMarca() != null && !request.getMarca().isBlank()) {
            Marca marca = marcaRepository.findByNombre(request.getMarca())
                    .orElseGet(() -> {
                        Marca newMarca = new Marca();
                        newMarca.setNombre(request.getMarca());
                        return marcaRepository.save(newMarca);
                    });
            articulo.setMarcaId(marca.getId());
        }

        if (request.getCategoria() != null && !request.getCategoria().isBlank()) {
            Categoria categoria = categoriaRepository.findByNombre(request.getCategoria())
                    .orElseGet(() -> {
                        Categoria newCategoria = new Categoria();
                        newCategoria.setNombre(request.getCategoria());
                        return categoriaRepository.save(newCategoria);
                    });
            articulo.setCategoriaId(categoria.getId());
        }

        if (request.getGenero() != null && !request.getGenero().isBlank()) {
            Genero genero = generoRepository.findByNombre(request.getGenero())
                    .orElseGet(() -> {
                        Genero newGenero = new Genero();
                        newGenero.setNombre(request.getGenero());
                        return generoRepository.save(newGenero);
                    });
            articulo.setGeneroId(genero.getId());
        }

        if (request.getSubCategoria() != null && !request.getSubCategoria().isBlank()) {
            Taxon subCat = resolverTaxon(request.getSubCategoria());
            vincularTaxonArticulo(articulo.getId(), subCat);
        }
        articulo.setCodigo(request.getCodigo());
        articulo.setModelo(request.getModelo());
        articulo.setDescripcion(request.getDescripcion());
        if (request.getVariantes() != null) {
            List<Long> idsVariantesRecibidas = new ArrayList<>();

            for (VarianteUpdateRequest varReq : request.getVariantes()) {
                if (varReq.getId() != null) {
                    idsVariantesRecibidas.add(varReq.getId());
                    
                    VarianteArticulo variante = articulo.getVariantes().stream()
                            .filter(v -> v.getId().equals(varReq.getId()))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Variante a editar no encontrada: " + varReq.getId()));

                    if (varReq.getCodigoBarras() != null && !varReq.getCodigoBarras().isBlank()) {
                        variante.setCodigoBarras(varReq.getCodigoBarras().trim());
                    }

                    BigDecimal precioActual = getPrecioActual(variante.getId());
                    if (precioActual == null || precioActual.compareTo(varReq.getPrecio()) != 0) {
                        HistorialPrecio historial = new HistorialPrecio();
                        historial.setVarianteArticuloId(variante.getId());
                        historial.setPrecioNuevo(varReq.getPrecio());
                        historial.setCostoNuevo(varReq.getCosto());
                        historial.setFecha(LocalDateTime.now());
                        historialPrecioRepository.save(historial);
                    }

                    if (varReq.getCantidad() != null) {
                        List<Stock> stocks = stockRepository.findAllByArticulo_IdAndVariante_Id(articulo.getId(), variante.getId());
                        if (!stocks.isEmpty()) {
                            Stock stock = stocks.get(0);
                            stock.setCantidadActual(varReq.getCantidad());
                            stock.setCantidadDisponible(varReq.getCantidad());
                            stockRepository.save(stock);
                        }
                    }
                } 
            }
            for (VarianteArticulo v : articulo.getVariantes()) {
                if (!idsVariantesRecibidas.contains(v.getId())) {
                    v.setEstado(VarianteArticulo.EstadoVariante.INACTIVO);
                }
            }
        }

        return articuloRepository.save(articulo);
    }

    @Transactional
    public VarianteCompactResponse actualizarVarianteUnica(Long articuloId, Long varianteId, VariantCreateRequest request) {
        Articulo articulo = getArticuloById(articuloId);
        VarianteArticulo variante = articulo.getVariantes().stream()
                .filter(v -> v.getId().equals(varianteId) && v.getEstado() == VarianteArticulo.EstadoVariante.ACTIVO)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Variante no encontrada o está eliminada."));
        Color varianteColor = colorRepository.findByNombre(request.getColor())
                .orElseGet(() -> {
                    Color newColor = new Color();
                    newColor.setNombre(request.getColor());
                    return colorRepository.save(newColor);
                });
        variante.setColorId(varianteColor.getId());
        Talle.Pais pais;
        try {
            pais = Talle.Pais.valueOf(request.getPais().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("País de talle inválido. Valores válidos: AR, UK, BR, US, EU");
        }
        
        Talle talle = talleRepository.findByPaisAndNumero(pais, request.getTalleNumero())
                .orElseGet(() -> {
                    Talle newTalle = new Talle();
                    newTalle.setPais(pais);
                    newTalle.setNumero(request.getTalleNumero());
                    newTalle.setDescripcion("Talle " + request.getTalleNumero() + " - " + pais);
                    return talleRepository.save(newTalle);
                });
        variante.setTalleId(talle.getId());
        boolean existeDuplicado = articulo.getVariantes().stream()
                .anyMatch(v -> !v.getId().equals(varianteId) 
                            && v.getColorId().equals(varianteColor.getId()) 
                            && v.getTalleId().equals(talle.getId())
                            && v.getEstado() == VarianteArticulo.EstadoVariante.ACTIVO);
        if (existeDuplicado) {
            throw new RuntimeException("Ya existe otra variante activa con ese talle y color.");
        }
        if (request.getCodigoBarras() != null && !request.getCodigoBarras().isBlank()) {
            String nuevoCodigo = request.getCodigoBarras().trim();
            if (!nuevoCodigo.equals(variante.getCodigoBarras())) {
                if (varianteArticuloRepository.existsByCodigoBarras(nuevoCodigo)) {
                    throw new RuntimeException("El código de barras ya está registrado: " + nuevoCodigo);
                }
                variante.setCodigoBarras(nuevoCodigo);
            }
        } else {
            variante.setCodigoBarras(null); 
        }

        varianteArticuloRepository.save(variante);
        BigDecimal precioActual = getPrecioActual(variante.getId());
        if (precioActual == null || precioActual.compareTo(request.getPrecio()) != 0) {
            HistorialPrecio historial = new HistorialPrecio();
            historial.setVarianteArticuloId(variante.getId());
            historial.setPrecioNuevo(request.getPrecio());
            if (request.getCosto() != null && request.getCosto().compareTo(BigDecimal.ZERO) >= 0) {
                historial.setCostoNuevo(request.getCosto());
            }
            historial.setFecha(LocalDateTime.now());
            historialPrecioRepository.save(historial);
        }
        if (request.getCantidad() != null && request.getCantidad() >= 0) {
            List<Stock> stocks = stockRepository.findAllByArticulo_IdAndVariante_Id(articulo.getId(), variante.getId());
            if (!stocks.isEmpty()) {
                Stock stock = stocks.get(0);
                stock.setCantidadActual(request.getCantidad());
                stock.setCantidadDisponible(request.getCantidad());
                stockRepository.save(stock);
            }
        }

        return toVarianteCompact(articuloId, variante);
    }

    @Transactional(readOnly = true)
    public List<ArticuloCompactResponse> getArticulosArchivados() {
        Specification<Articulo> spec = (root, query, cb) -> 
                cb.equal(root.get("estado"), Articulo.EstadoProducto.ARCHIVADO);
        
        List<Articulo> archivados = articuloRepository.findAll(spec);
        return archivados.stream().map(a -> {
            ArticuloCompactResponse dto = new ArticuloCompactResponse();
            dto.setId(a.getId());
            dto.setModelo(a.getModelo());
            dto.setCodigo(a.getCodigo());
            dto.setMarca(a.getMarca() != null ? a.getMarca().getNombre() : null);
            dto.setCategoria(a.getCategoria() != null ? a.getCategoria().getNombre() : null);
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void restaurarArticulo(Long id) {
        Articulo articulo = getArticuloById(id);
        articulo.setEstado(Articulo.EstadoProducto.ACTIVO);
        if (articulo.getVariantes() != null) {
            articulo.getVariantes().forEach(v -> v.setEstado(VarianteArticulo.EstadoVariante.ACTIVO));
        }
        
        articuloRepository.save(articulo);
    }

    @Transactional
    public void restaurarVariante(Long articuloId, Long varianteId) {
        Articulo articulo = getArticuloById(articuloId);
        
        VarianteArticulo variante = articulo.getVariantes().stream()
                .filter(v -> v.getId().equals(varianteId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Variante no encontrada en este artículo"));
        variante.setEstado(VarianteArticulo.EstadoVariante.ACTIVO);
        articuloRepository.save(articulo);
    }

    @Transactional(readOnly = true)
    public List<VarianteCompactResponse> getVariantesArchivadas(Long articuloId) {
        Articulo articulo = getArticuloById(articuloId);
        List<VarianteCompactResponse> archivadas = new ArrayList<>();
        
        if (articulo.getVariantes() != null) {
            for (VarianteArticulo v : articulo.getVariantes()) {
                if (v.getEstado() == VarianteArticulo.EstadoVariante.INACTIVO) {
                    VarianteCompactResponse dto = new VarianteCompactResponse();
                    dto.setId(v.getId());
                    dto.setColor(v.getColor() != null ? v.getColor().getNombre() : null);
                    dto.setTalle(v.getTalle() != null ? v.getTalle().getNumero() : null);
                    dto.setCodigoBarras(v.getCodigoBarras());
                    dto.setPrecio(getPrecioActual(v.getId()));
                    dto.setCantidad(getCantidadDisponibleForVariante(articuloId, v.getId()));
                    archivadas.add(dto);
                }
            }
        }
        return archivadas;
    }
}