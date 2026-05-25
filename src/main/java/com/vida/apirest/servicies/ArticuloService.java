package com.vida.apirest.servicies;

import com.vida.apirest.dto.ariticulo.ArticuloCompactResponse;
import com.vida.apirest.dto.ariticulo.ArticuloCreateRequest;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
    private final VarianteArticuloRepository varianteArticuloRepository;
    private final HistorialPrecioRepository historialPrecioRepository;
    private final StockRepository stockRepository;
    private final DepositoRepository depositoRepository;
    private final SucursalRepository sucursalRepository;

    @Transactional(readOnly = true)
    public List<ArticuloCompactResponse> findAllCompact() {
        return articuloRepository.findAll().stream()
                .map(this::toCompactResponse)
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

        // Buscar o crear SubCategoria (Taxon)
        Taxon subCategoria = null;
        if (request.getSubCategoria() != null) {
            subCategoria = taxonRepository.findByNombre(request.getSubCategoria())
                    .orElseGet(() -> {
                        Taxon newTaxon = new Taxon();
                        newTaxon.setNombre(request.getSubCategoria());
                        newTaxon.setNivel(1);
                        return taxonRepository.save(newTaxon);
                    });
        }

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
        articulo.setDescripcion(request.getDescripcion());
        articulo = articuloRepository.save(articulo);

        // Crear TaxonArticulo si hay subCategoria
        if (subCategoria != null) {
            TaxonArticulo taxonArticulo = new TaxonArticulo();
            taxonArticulo.setArticuloId(articulo.getId());
            taxonArticulo.setTaxonId(subCategoria.getId());
            // Asumir que hay un repository para TaxonArticulo
            // taxonArticuloRepository.save(taxonArticulo);
        }

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

        // Crear variantes
        for (var variantReq : request.getVariantes()) {
            // Validar pais
            Talle.Pais pais;
            try {
                pais = Talle.Pais.valueOf(variantReq.getPais().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("País de talle inválido. Valores válidos: AR, UK, BR, US, EU");
            }

            // Buscar Talle
            Talle talle = talleRepository.findByPaisAndNumero(pais, variantReq.getTalleNumero())
                    .orElseGet(() -> {
                        Talle newTalle = new Talle();
                        newTalle.setPais(pais);
                        newTalle.setNumero(variantReq.getTalleNumero());
                        newTalle.setDescripcion("Talle " + variantReq.getTalleNumero() + " - " + pais);
                        return talleRepository.save(newTalle);
                    });

            // Buscar o crear Color para esta variante
            Color varianteColor = colorRepository.findByNombre(variantReq.getColor())
                    .orElseGet(() -> {
                        Color newColor = new Color();
                        newColor.setNombre(variantReq.getColor());
                        return colorRepository.save(newColor);
                    });

            // Crear VarianteArticulo
            VarianteArticulo variante = new VarianteArticulo();
            variante.setArticuloId(articulo.getId());
            variante.setColorId(varianteColor.getId());
            variante.setTalleId(talle.getId());
            variante = varianteArticuloRepository.save(variante);

            // Crear HistorialPrecio
            HistorialPrecio historial = new HistorialPrecio();
            historial.setVarianteArticuloId(variante.getId());
            historial.setPrecioNuevo(variantReq.getPrecio());
            historial.setFecha(LocalDateTime.now());
            historialPrecioRepository.save(historial);

            // Crear Stock
            Stock stock = new Stock();
            stock.setDeposito(deposito);
            stock.setSucursal(sucursal);
            stock.setArticulo(articulo);
            stock.setVariante(variante);
            stock.setCantidadActual(variantReq.getCantidad());
            stock.setCantidadDisponible(variantReq.getCantidad());
            stockRepository.save(stock);
        }

        return articulo;
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

    public Articulo getArticuloById(Long id) {
        return articuloRepository.findById(id).orElseThrow(() -> new RuntimeException("Artículo no encontrado con id: " + id));
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
                variantDto.setPrecio(getPrecioActual(variante));
                variantDto.setCantidad(getCantidadDisponibleForVariante(articulo.getId(), variante.getId()));
                variants.add(variantDto);
            }
        }
        response.setVariantes(variants);
        return response;
    }

    private BigDecimal getPrecioActual(VarianteArticulo variante) {
        if (variante.getHistorialPrecios() == null || variante.getHistorialPrecios().isEmpty()) {
            return null;
        }
        return variante.getHistorialPrecios().stream()
                .max(Comparator.comparing(HistorialPrecio::getFecha))
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
}