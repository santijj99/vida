package com.vida.apirest.servicies;

import com.vida.apirest.dto.finanzas.CreateTipoCambioRequest;
import com.vida.apirest.dto.finanzas.TipoCambioResponse;
import com.vida.apirest.model.finanzas.Moneda;
import com.vida.apirest.model.finanzas.TipoCambio;
import com.vida.apirest.repositories.MonedaRepository;
import com.vida.apirest.repositories.TipoCambioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TipoCambioService {

    private final TipoCambioRepository tipoCambioRepository;
    private final MonedaRepository monedaRepository;

    @Transactional
    public TipoCambioResponse createTipoCambio(CreateTipoCambioRequest request) {
        // Validar moneda
        Moneda moneda = monedaRepository.findById(request.getMonedaId())
                .orElseThrow(() -> new RuntimeException("Moneda no encontrada con ID: " + request.getMonedaId()));

        // Validar que no exista tipo de cambio para la misma moneda y fecha
        if (tipoCambioRepository.findByMonedaAndFecha(moneda, request.getFecha()).isPresent()) {
            throw new RuntimeException("Ya existe un tipo de cambio para la moneda " + moneda.getCodigo() + " en la fecha " + request.getFecha());
        }

        // Calcular tasa promedio si no se proporciona
        BigDecimal tasaPromedio = request.getTasaPromedio();
        if (tasaPromedio == null && request.getTasaCompra() != null && request.getTasaVenta() != null) {
            tasaPromedio = request.getTasaCompra().add(request.getTasaVenta()).divide(BigDecimal.valueOf(2));
        }

        TipoCambio tipoCambio = new TipoCambio();
        tipoCambio.setMoneda(moneda);
        tipoCambio.setFecha(request.getFecha());
        tipoCambio.setTasaCompra(request.getTasaCompra());
        tipoCambio.setTasaVenta(request.getTasaVenta());
        tipoCambio.setTasaPromedio(tasaPromedio);
        tipoCambio.setFuente(request.getFuente());
        tipoCambio.setObservaciones(request.getObservaciones());
        tipoCambio.setUsuario("sistema"); // TODO: Obtener del contexto de seguridad

        TipoCambio saved = tipoCambioRepository.save(tipoCambio);

        // Actualizar la tasa de cambio en la moneda si es del día actual
        if (request.getFecha().equals(LocalDate.now()) && tasaPromedio != null) {
            moneda.setTasaCambio(tasaPromedio);
            monedaRepository.save(moneda);
        }

        return mapTipoCambioResponse(saved);
    }

    @Transactional
    public TipoCambioResponse updateTipoCambio(Long id, CreateTipoCambioRequest request) {
        TipoCambio tipoCambio = tipoCambioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de cambio no encontrado con ID: " + id));

        // Validar moneda si cambió
        if (!tipoCambio.getMoneda().getId().equals(request.getMonedaId())) {
            Moneda nuevaMoneda = monedaRepository.findById(request.getMonedaId())
                    .orElseThrow(() -> new RuntimeException("Moneda no encontrada con ID: " + request.getMonedaId()));

            // Verificar que no exista para la nueva moneda y fecha
            if (tipoCambioRepository.findByMonedaAndFecha(nuevaMoneda, request.getFecha()).isPresent()) {
                throw new RuntimeException("Ya existe un tipo de cambio para la moneda " + nuevaMoneda.getCodigo() + " en la fecha " + request.getFecha());
            }
            tipoCambio.setMoneda(nuevaMoneda);
        }

        // Calcular tasa promedio si no se proporciona
        BigDecimal tasaPromedio = request.getTasaPromedio();
        if (tasaPromedio == null && request.getTasaCompra() != null && request.getTasaVenta() != null) {
            tasaPromedio = request.getTasaCompra().add(request.getTasaVenta()).divide(BigDecimal.valueOf(2));
        }

        tipoCambio.setFecha(request.getFecha());
        tipoCambio.setTasaCompra(request.getTasaCompra());
        tipoCambio.setTasaVenta(request.getTasaVenta());
        tipoCambio.setTasaPromedio(tasaPromedio);
        tipoCambio.setFuente(request.getFuente());
        tipoCambio.setObservaciones(request.getObservaciones());

        TipoCambio saved = tipoCambioRepository.save(tipoCambio);

        // Actualizar la tasa de cambio en la moneda si es del día actual
        if (request.getFecha().equals(LocalDate.now()) && tasaPromedio != null) {
            Moneda moneda = tipoCambio.getMoneda();
            moneda.setTasaCambio(tasaPromedio);
            monedaRepository.save(moneda);
        }

        return mapTipoCambioResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TipoCambioResponse> findAll() {
        return tipoCambioRepository.findAll().stream()
                .map(this::mapTipoCambioResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TipoCambioResponse findById(Long id) {
        TipoCambio tipoCambio = tipoCambioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de cambio no encontrado con ID: " + id));
        return mapTipoCambioResponse(tipoCambio);
    }

    @Transactional(readOnly = true)
    public List<TipoCambioResponse> findByMoneda(Long monedaId) {
        Moneda moneda = monedaRepository.findById(monedaId)
                .orElseThrow(() -> new RuntimeException("Moneda no encontrada con ID: " + monedaId));

        return tipoCambioRepository.findByMonedaOrderByFechaDesc(moneda).stream()
                .map(this::mapTipoCambioResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TipoCambioResponse findByMonedaAndFecha(Long monedaId, LocalDate fecha) {
        Moneda moneda = monedaRepository.findById(monedaId)
                .orElseThrow(() -> new RuntimeException("Moneda no encontrada con ID: " + monedaId));

        TipoCambio tipoCambio = tipoCambioRepository.findByMonedaAndFecha(moneda, fecha)
                .orElseThrow(() -> new RuntimeException("Tipo de cambio no encontrado para la moneda " + moneda.getCodigo() + " en la fecha " + fecha));

        return mapTipoCambioResponse(tipoCambio);
    }

    @Transactional(readOnly = true)
    public List<TipoCambioResponse> findByFecha(LocalDate fecha) {
        return tipoCambioRepository.findByFecha(fecha).stream()
                .map(this::mapTipoCambioResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TipoCambioResponse findUltimoTipoCambio(Long monedaId) {
        Moneda moneda = monedaRepository.findById(monedaId)
                .orElseThrow(() -> new RuntimeException("Moneda no encontrada con ID: " + monedaId));

        List<TipoCambio> tiposCambio = tipoCambioRepository.findByMonedaOrderByFechaDesc(moneda);
        if (tiposCambio.isEmpty()) {
            throw new RuntimeException("No hay tipos de cambio registrados para la moneda " + moneda.getCodigo());
        }

        return mapTipoCambioResponse(tiposCambio.get(0));
    }

    private TipoCambioResponse mapTipoCambioResponse(TipoCambio tipoCambio) {
        TipoCambioResponse response = new TipoCambioResponse();
        response.setId(tipoCambio.getId());
        response.setMonedaId(tipoCambio.getMoneda().getId());
        response.setMonedaCodigo(tipoCambio.getMoneda().getCodigo());
        response.setMonedaNombre(tipoCambio.getMoneda().getNombre());
        response.setFecha(tipoCambio.getFecha());
        response.setTasaCompra(tipoCambio.getTasaCompra());
        response.setTasaVenta(tipoCambio.getTasaVenta());
        response.setTasaPromedio(tipoCambio.getTasaPromedio());
        response.setFuente(tipoCambio.getFuente());
        response.setObservaciones(tipoCambio.getObservaciones());
        response.setCreatedAt(tipoCambio.getCreatedAt());
        response.setUsuario(tipoCambio.getUsuario());
        return response;
    }
}