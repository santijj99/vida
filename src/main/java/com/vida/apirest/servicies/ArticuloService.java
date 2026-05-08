package com.vida.apirest.servicies;

import com.vida.apirest.dto.ariticulo.ArticuloCreateRequest;
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
import java.util.List;

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

    @Transactional
    public Articulo createArticulo(ArticuloCreateRequest request) {
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

        // Buscar Color
        Color color = colorRepository.findByNombre(request.getColor())
                .orElseGet(() -> {
                    Color newColor = new Color();
                    newColor.setNombre(request.getColor());
                    return colorRepository.save(newColor);
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

            // Crear VarianteArticulo
            VarianteArticulo variante = new VarianteArticulo();
            variante.setArticuloId(articulo.getId());
            variante.setColorId(color.getId());
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
}