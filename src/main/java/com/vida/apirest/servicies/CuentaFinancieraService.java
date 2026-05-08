package com.vida.apirest.servicies;

import com.vida.apirest.dto.finanzas.CreateCuentaFinancieraRequest;
import com.vida.apirest.dto.finanzas.CuentaFinancieraResponse;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.finanzas.CuentaFinanciera;
import com.vida.apirest.model.finanzas.Moneda;
import com.vida.apirest.repositories.FinanzasCuentaFinancieraRepository;
import com.vida.apirest.repositories.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CuentaFinancieraService {

    private final FinanzasCuentaFinancieraRepository cuentaRepository;
    private final SucursalRepository sucursalRepository;
    private final com.vida.apirest.repositories.MonedaRepository monedaRepository;

    @Transactional
    public CuentaFinancieraResponse createCuentaFinanciera(CreateCuentaFinancieraRequest request) {
        // Validar sucursal
        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada con ID: " + request.getSucursalId()));

        // Validar moneda
        Moneda moneda = monedaRepository.findById(request.getMonedaId())
                .orElseThrow(() -> new RuntimeException("Moneda no encontrada con ID: " + request.getMonedaId()));

        // Validar tipo de cuenta
        CuentaFinanciera.TipoCuenta tipoCuenta;
        try {
            tipoCuenta = CuentaFinanciera.TipoCuenta.valueOf(request.getTipo().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Tipo de cuenta inválido. Valores válidos: CAJA, BANCO, TARJETA_DEBITO, TARJETA_CREDITO, BILLETERA, VIRTUAL");
        }

        // Verificar que no exista otra cuenta con el mismo número
        if (cuentaRepository.findByNumero(request.getNumero()).isPresent()) {
            throw new RuntimeException("Ya existe una cuenta financiera con el número: " + request.getNumero());
        }

        CuentaFinanciera cuenta = new CuentaFinanciera();
        cuenta.setSucursal(sucursal);
        cuenta.setMoneda(moneda);
        cuenta.setNombre(request.getNombre());
        cuenta.setNumero(request.getNumero());
        cuenta.setTipo(tipoCuenta);
        cuenta.setBanco(request.getBanco());
        cuenta.setSaldoInicial(request.getSaldoInicial() != null ? request.getSaldoInicial() : BigDecimal.ZERO);
        cuenta.setSaldoActual(request.getSaldoInicial() != null ? request.getSaldoInicial() : BigDecimal.ZERO);
        cuenta.setPersonaResponsable(request.getPersonaResponsable());
        cuenta.setActivo(request.getActivo() != null ? request.getActivo() : true);

        CuentaFinanciera saved = cuentaRepository.save(cuenta);
        return mapCuentaFinancieraResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CuentaFinancieraResponse> findAll() {
        return cuentaRepository.findAll().stream()
                .map(this::mapCuentaFinancieraResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CuentaFinancieraResponse findById(Long id) {
        CuentaFinanciera cuenta = cuentaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cuenta financiera no encontrada con ID: " + id));
        return mapCuentaFinancieraResponse(cuenta);
    }

    @Transactional(readOnly = true)
    public List<CuentaFinancieraResponse> findByTipo(String tipo) {
        try {
            CuentaFinanciera.TipoCuenta tipoCuenta = CuentaFinanciera.TipoCuenta.valueOf(tipo.toUpperCase());
            return cuentaRepository.findByTipoAndActivoTrue(tipoCuenta).stream()
                    .map(this::mapCuentaFinancieraResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Tipo de cuenta inválido: " + tipo);
        }
    }

    private CuentaFinancieraResponse mapCuentaFinancieraResponse(CuentaFinanciera cuenta) {
        CuentaFinancieraResponse response = new CuentaFinancieraResponse();
        response.setId(cuenta.getId());
        response.setSucursalId(cuenta.getSucursal().getId());
        response.setMonedaId(cuenta.getMoneda().getId());
        response.setNombre(cuenta.getNombre());
        response.setNumero(cuenta.getNumero());
        response.setTipo(cuenta.getTipo() != null ? cuenta.getTipo().name() : null);
        response.setBanco(cuenta.getBanco());
        response.setSaldoInicial(cuenta.getSaldoInicial());
        response.setSaldoActual(cuenta.getSaldoActual());
        response.setPersonaResponsable(cuenta.getPersonaResponsable());
        response.setActivo(cuenta.getActivo());
        response.setCreatedAt(cuenta.getCreatedAt());
        response.setUpdatedAt(cuenta.getUpdatedAt());
        return response;
    }
}
