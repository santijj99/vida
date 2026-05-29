package com.vida.apirest.servicies;

import com.vida.apirest.dto.finanzas.CreateMonedaRequest;
import com.vida.apirest.dto.finanzas.MonedaResponse;
import com.vida.apirest.model.finanzas.Moneda;
import com.vida.apirest.repositories.MonedaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonedaService {

    private final MonedaRepository monedaRepository;

    @Transactional
    public MonedaResponse createMoneda(CreateMonedaRequest request) {
        if (request.getCodigo() == null || request.getCodigo().isBlank()) {
            throw new RuntimeException("El código de la moneda es obligatorio");
        }

        String codigo = request.getCodigo().trim().toUpperCase();

        // Validar código único
        if (monedaRepository.findByCodigo(codigo).isPresent()) {
            throw new RuntimeException("Ya existe una moneda con el código: " + codigo);
        }

        // Si es predeterminada, quitar la predeterminada anterior
        if (request.getPredeterminada() != null && request.getPredeterminada()) {
            List<Moneda> predeterminadas = monedaRepository.findByPredeterminadaTrue();
            for (Moneda moneda : predeterminadas) {
                moneda.setPredeterminada(false);
                monedaRepository.save(moneda);
            }
        }

        Moneda moneda = new Moneda();
        moneda.setCodigo(codigo);
        moneda.setNombre(request.getNombre());
        moneda.setSimbolo(request.getSimbolo());
        moneda.setTasaCambio(request.getTasaCambio() != null ? request.getTasaCambio() : BigDecimal.ONE);
        moneda.setDecimalPlaces(request.getDecimalPlaces() != null ? request.getDecimalPlaces() : 2);
        moneda.setActivo(request.getActivo() != null ? request.getActivo() : true);
        moneda.setPredeterminada(request.getPredeterminada() != null ? request.getPredeterminada() : false);

        Moneda saved = monedaRepository.save(moneda);
        return mapMonedaResponse(saved);
    }

    @Transactional
    public MonedaResponse updateTasaCambio(String codigo, BigDecimal nuevaTasa) {
        Moneda moneda = monedaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RuntimeException("Moneda no encontrada con código: " + codigo));

        moneda.setTasaCambio(nuevaTasa);
        Moneda saved = monedaRepository.save(moneda);
        return mapMonedaResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MonedaResponse> findAll() {
        return monedaRepository.findAll().stream()
                .map(this::mapMonedaResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MonedaResponse findById(Long id) {
        Moneda moneda = monedaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Moneda no encontrada con ID: " + id));
        return mapMonedaResponse(moneda);
    }

    @Transactional(readOnly = true)
    public MonedaResponse findByCodigo(String codigo) {
        Moneda moneda = monedaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RuntimeException("Moneda no encontrada con código: " + codigo));
        return mapMonedaResponse(moneda);
    }

    @Transactional(readOnly = true)
    public MonedaResponse findPredeterminada() {
        List<Moneda> predeterminadas = monedaRepository.findByPredeterminadaTrue();
        if (predeterminadas.isEmpty()) {
            throw new RuntimeException("No hay moneda predeterminada configurada");
        }
        return mapMonedaResponse(predeterminadas.get(0));
    }

    private MonedaResponse mapMonedaResponse(Moneda moneda) {
        MonedaResponse response = new MonedaResponse();
        response.setId(moneda.getId());
        response.setCodigo(moneda.getCodigo());
        response.setNombre(moneda.getNombre());
        response.setSimbolo(moneda.getSimbolo());
        response.setTasaCambio(moneda.getTasaCambio());
        response.setDecimalPlaces(moneda.getDecimalPlaces());
        response.setActivo(moneda.getActivo());
        response.setPredeterminada(moneda.getPredeterminada());
        response.setCreatedAt(moneda.getCreatedAt());
        response.setUpdatedAt(moneda.getUpdatedAt());
        return response;
    }
}