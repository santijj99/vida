package com.vida.apirest.servicies;

import com.vida.apirest.dto.ariticulo.CreatePromocionRequest;
import com.vida.apirest.dto.ariticulo.PromocionResponse;
import com.vida.apirest.dto.ariticulo.PromocionVarianteRequest;
import com.vida.apirest.dto.ariticulo.PromocionVarianteResponse;
import com.vida.apirest.model.articulo.Articulo;
import com.vida.apirest.model.articulo.HistorialPrecio;
import com.vida.apirest.model.articulo.Promocion;
import com.vida.apirest.model.articulo.PromocionVariante;
import com.vida.apirest.model.articulo.VarianteArticulo;
import com.vida.apirest.repositories.HistorialPrecioRepository;
import com.vida.apirest.repositories.PromocionRepository;
import com.vida.apirest.repositories.PromocionVarianteRepository;
import com.vida.apirest.repositories.VarianteArticuloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PromocionService {

    private final PromocionRepository promocionRepository;
    private final PromocionVarianteRepository promocionVarianteRepository;
    private final VarianteArticuloRepository varianteArticuloRepository;
    private final HistorialPrecioRepository historialPrecioRepository;

    @Transactional(readOnly = true)
    public List<PromocionResponse> findAll() {
        return promocionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PromocionResponse findById(Long id) {
        Promocion promocion = promocionRepository.findByIdWithVariantes(id)
                .orElseThrow(() -> new RuntimeException("Promoción no encontrada con ID: " + id));
        return toDetailResponse(promocion);
    }

    @Transactional
    public PromocionResponse create(CreatePromocionRequest request) {
        validarCabecera(request);
        if (request.getVariantes() == null || request.getVariantes().isEmpty()) {
            throw new RuntimeException("Debe incluir al menos una variante en la promoción");
        }

        Promocion promocion = new Promocion();
        aplicarCabecera(promocion, request);
        promocion.setVariantes(construirDetalles(promocion, request.getPorcentajeDescuento(), request.getVariantes()));

        promocion = promocionRepository.save(promocion);
        return toDetailResponse(promocionRepository.findByIdWithVariantes(promocion.getId()).orElseThrow());
    }

    @Transactional
    public PromocionResponse update(Long id, CreatePromocionRequest request) {
        validarCabecera(request);
        if (request.getVariantes() == null || request.getVariantes().isEmpty()) {
            throw new RuntimeException("Debe incluir al menos una variante en la promoción");
        }

        Promocion promocion = promocionRepository.findByIdWithVariantes(id)
                .orElseThrow(() -> new RuntimeException("Promoción no encontrada con ID: " + id));

        aplicarCabecera(promocion, request);
        promocion.getVariantes().clear();
        promocion.getVariantes().addAll(construirDetalles(promocion, request.getPorcentajeDescuento(), request.getVariantes()));

        promocion = promocionRepository.save(promocion);
        return toDetailResponse(promocionRepository.findByIdWithVariantes(promocion.getId()).orElseThrow());
    }

    @Transactional
    public void delete(Long id) {
        if (!promocionRepository.existsById(id)) {
            throw new RuntimeException("Promoción no encontrada con ID: " + id);
        }
        promocionRepository.deleteById(id);
    }

    /**
     * Devuelve el mejor precio promocional vigente para la variante, o el precio de lista si no aplica promoción.
     */
    @Transactional(readOnly = true)
    public BigDecimal resolverPrecioVenta(Long varianteId, BigDecimal precioLista) {
        if (varianteId == null || precioLista == null || precioLista.compareTo(BigDecimal.ZERO) <= 0) {
            return precioLista;
        }
        BigDecimal promo = mejorPrecioPromocional(varianteId, precioLista);
        return promo != null ? promo : precioLista;
    }

    /**
     * Aplica promociones vigentes sobre un lote de variantes (catálogo de ventas).
     */
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> resolverPreciosVenta(Map<Long, BigDecimal> preciosPorVariante) {
        if (preciosPorVariante == null || preciosPorVariante.isEmpty()) {
            return Map.of();
        }
        LocalDate hoy = LocalDate.now();
        List<PromocionVariante> activas = promocionVarianteRepository.findActivasByVarianteIds(
                preciosPorVariante.keySet(), hoy);

        Map<Long, List<PromocionVariante>> porVariante = new HashMap<>();
        for (PromocionVariante detalle : activas) {
            Long varianteId = detalle.getVariante().getId();
            porVariante.computeIfAbsent(varianteId, ignored -> new ArrayList<>()).add(detalle);
        }

        Map<Long, BigDecimal> resultado = new HashMap<>();
        for (Map.Entry<Long, BigDecimal> entry : preciosPorVariante.entrySet()) {
            BigDecimal precioLista = entry.getValue();
            if (precioLista == null || precioLista.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            List<PromocionVariante> detalles = porVariante.get(entry.getKey());
            BigDecimal mejor = null;
            if (detalles != null) {
                for (PromocionVariante detalle : detalles) {
                    BigDecimal candidato = resolverPrecioDetalle(detalle, precioLista);
                    if (esPrecioPromocionalValido(candidato, precioLista)
                            && (mejor == null || candidato.compareTo(mejor) < 0)) {
                        mejor = candidato;
                    }
                }
            }
            if (mejor != null) {
                resultado.put(entry.getKey(), mejor);
            }
        }
        return resultado;
    }

    private BigDecimal mejorPrecioPromocional(Long varianteId, BigDecimal precioLista) {
        List<PromocionVariante> activas = promocionVarianteRepository.findActivasByVarianteId(varianteId, LocalDate.now());
        BigDecimal mejor = null;
        for (PromocionVariante detalle : activas) {
            BigDecimal candidato = resolverPrecioDetalle(detalle, precioLista);
            if (esPrecioPromocionalValido(candidato, precioLista)
                    && (mejor == null || candidato.compareTo(mejor) < 0)) {
                mejor = candidato;
            }
        }
        return mejor;
    }

    private BigDecimal resolverPrecioDetalle(PromocionVariante detalle, BigDecimal precioLista) {
        if (detalle.getPrecioPromocional() != null) {
            return detalle.getPrecioPromocional();
        }
        Promocion promocion = detalle.getPromocion();
        if (promocion == null) {
            return null;
        }
        return calcularPrecioConDescuento(precioLista, promocion.getPorcentajeDescuento());
    }

    private boolean esPrecioPromocionalValido(BigDecimal precioPromo, BigDecimal precioLista) {
        return precioPromo != null
                && precioPromo.compareTo(BigDecimal.ZERO) > 0
                && precioLista != null
                && precioPromo.compareTo(precioLista) < 0;
    }

    private void validarCabecera(CreatePromocionRequest request) {
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new RuntimeException("El nombre de la promoción es obligatorio");
        }
        BigDecimal pct = request.getPorcentajeDescuento() != null ? request.getPorcentajeDescuento() : BigDecimal.ZERO;
        if (pct.compareTo(BigDecimal.ZERO) < 0 || pct.compareTo(new BigDecimal("100")) > 0) {
            throw new RuntimeException("El porcentaje de descuento debe estar entre 0 y 100");
        }
        if (request.getFechaInicio() != null && request.getFechaFin() != null
                && request.getFechaFin().isBefore(request.getFechaInicio())) {
            throw new RuntimeException("La fecha de fin no puede ser anterior a la de inicio");
        }
    }

    private void aplicarCabecera(Promocion promocion, CreatePromocionRequest request) {
        promocion.setNombre(request.getNombre().trim());
        promocion.setDescripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null);
        promocion.setPorcentajeDescuento(request.getPorcentajeDescuento() != null
                ? request.getPorcentajeDescuento()
                : BigDecimal.ZERO);
        promocion.setFechaInicio(request.getFechaInicio());
        promocion.setFechaFin(request.getFechaFin());
        promocion.setActivo(request.getActivo() != null ? request.getActivo() : true);
    }

    private List<PromocionVariante> construirDetalles(
            Promocion promocion,
            BigDecimal porcentajeDescuento,
            List<PromocionVarianteRequest> items
    ) {
        Set<Long> vistos = new HashSet<>();
        List<PromocionVariante> detalles = new ArrayList<>();

        for (PromocionVarianteRequest item : items) {
            if (item.getVarianteId() == null) {
                throw new RuntimeException("Cada ítem debe indicar varianteId");
            }
            if (!vistos.add(item.getVarianteId())) {
                continue;
            }

            VarianteArticulo variante = varianteArticuloRepository.findByIdWithRelations(item.getVarianteId())
                    .orElseThrow(() -> new RuntimeException("Variante no encontrada: " + item.getVarianteId()));
            if (variante.getEstado() != VarianteArticulo.EstadoVariante.ACTIVO) {
                throw new RuntimeException("La variante " + item.getVarianteId() + " no está activa");
            }

            BigDecimal precioOriginal = getPrecioActual(variante.getId());
            BigDecimal precioPromo = item.getPrecioPromocional();
            if (precioPromo == null && precioOriginal != null) {
                precioPromo = calcularPrecioConDescuento(precioOriginal, porcentajeDescuento);
            }

            PromocionVariante detalle = new PromocionVariante();
            detalle.setPromocion(promocion);
            detalle.setVariante(variante);
            detalle.setPrecioPromocional(precioPromo);
            detalles.add(detalle);
        }

        if (detalles.isEmpty()) {
            throw new RuntimeException("Debe incluir al menos una variante válida");
        }
        return detalles;
    }

    private BigDecimal calcularPrecioConDescuento(BigDecimal precioOriginal, BigDecimal porcentaje) {
        if (precioOriginal == null) {
            return null;
        }
        BigDecimal pct = porcentaje != null ? porcentaje : BigDecimal.ZERO;
        BigDecimal factor = BigDecimal.ONE.subtract(pct.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        return precioOriginal.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getPrecioActual(Long varianteId) {
        return historialPrecioRepository.findFirstByVarianteArticuloIdOrderByFechaDesc(varianteId)
                .map(HistorialPrecio::getPrecioNuevo)
                .orElse(null);
    }

    private PromocionResponse toListResponse(Promocion promocion) {
        PromocionResponse response = new PromocionResponse();
        response.setId(promocion.getId());
        response.setNombre(promocion.getNombre());
        response.setDescripcion(promocion.getDescripcion());
        response.setPorcentajeDescuento(promocion.getPorcentajeDescuento());
        response.setFechaInicio(promocion.getFechaInicio());
        response.setFechaFin(promocion.getFechaFin());
        response.setActivo(promocion.getActivo());
        response.setCantidadVariantes(promocion.getVariantes() != null ? promocion.getVariantes().size() : 0);
        response.setCreatedAt(promocion.getCreatedAt());
        return response;
    }

    private PromocionResponse toDetailResponse(Promocion promocion) {
        PromocionResponse response = toListResponse(promocion);
        List<PromocionVarianteResponse> variantes = new ArrayList<>();
        if (promocion.getVariantes() != null) {
            for (PromocionVariante detalle : promocion.getVariantes()) {
                variantes.add(toVarianteResponse(detalle, promocion.getPorcentajeDescuento()));
            }
        }
        response.setVariantes(variantes);
        return response;
    }

    private PromocionVarianteResponse toVarianteResponse(PromocionVariante detalle, BigDecimal porcentajeCabecera) {
        PromocionVarianteResponse response = new PromocionVarianteResponse();
        response.setId(detalle.getId());
        response.setVarianteId(detalle.getVariante().getId());
        response.setPrecioPromocional(detalle.getPrecioPromocional());

        VarianteArticulo variante = detalle.getVariante();
        if (variante != null) {
            Articulo articulo = variante.getArticulo();
            response.setArticuloId(variante.getArticuloId());
            if (articulo != null) {
                response.setCodigo(articulo.getCodigo());
                response.setModelo(articulo.getModelo());
                response.setMarca(articulo.getMarca() != null ? articulo.getMarca().getNombre() : null);
            }
            response.setTalle(variante.getTalle() != null ? variante.getTalle().getNumero() : null);
            response.setColor(variante.getColor() != null ? variante.getColor().getNombre() : null);
            BigDecimal original = getPrecioActual(variante.getId());
            response.setPrecioOriginal(original);
            if (response.getPrecioPromocional() == null && original != null) {
                response.setPrecioPromocional(calcularPrecioConDescuento(original, porcentajeCabecera));
            }
        }
        return response;
    }
}
