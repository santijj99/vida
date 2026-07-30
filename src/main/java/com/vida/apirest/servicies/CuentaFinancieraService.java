package com.vida.apirest.servicies;

import com.vida.apirest.dto.finanzas.CreateCuentaFinancieraRequest;
import com.vida.apirest.dto.finanzas.CuentaFinancieraResponse;
import com.vida.apirest.dto.finanzas.TransferenciaCuentaRequest;
import com.vida.apirest.dto.finanzas.TransferenciaCuentaResponse;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.finanzas.CuentaFinanciera;
import com.vida.apirest.model.finanzas.Moneda;
import com.vida.apirest.model.persona.Empleado;
import com.vida.apirest.repositories.EmpleadoRepository;
import com.vida.apirest.repositories.FinanzasCuentaFinancieraRepository;
import com.vida.apirest.repositories.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CuentaFinancieraService {

    private final FinanzasCuentaFinancieraRepository cuentaRepository;
    private final SucursalRepository sucursalRepository;
    private final com.vida.apirest.repositories.MonedaRepository monedaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final CajaMovimientoService cajaMovimientoService;

    @Transactional
    public CuentaFinancieraResponse createCuentaFinanciera(CreateCuentaFinancieraRequest request) {
        // Validar sucursal
        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada con ID: " + request.getSucursalId()));

        // Validar moneda
        Moneda moneda = monedaRepository.findById(request.getMonedaId())
                .orElseThrow(() -> new RuntimeException("Moneda no encontrada con ID: " + request.getMonedaId()));

        // Validar tipo de cuenta
        if (request.getTipo() == null || request.getTipo().isBlank()) {
            throw new RuntimeException("El tipo de cuenta es obligatorio (CAJA, BANCO o AHORRO)");
        }
        CuentaFinanciera.TipoCuenta tipoCuenta;
        try {
            tipoCuenta = CuentaFinanciera.TipoCuenta.valueOf(request.getTipo().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Tipo de cuenta inválido. Valores válidos: CAJA, BANCO, AHORRO, TARJETA_DEBITO, TARJETA_CREDITO, BILLETERA, VIRTUAL");
        }

        String numero = request.getNumero() == null ? "" : request.getNumero().trim();
        if (numero.isBlank()) {
            numero = generarNumero(tipoCuenta);
        }

        // Verificar que no exista otra cuenta con el mismo número
        if (cuentaRepository.findByNumero(numero).isPresent()) {
            throw new RuntimeException("Ya existe una cuenta financiera con el número: " + numero);
        }

        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new RuntimeException("El nombre de la cuenta es obligatorio");
        }

        if (tipoCuenta == CuentaFinanciera.TipoCuenta.BANCO
                && (request.getBanco() == null || request.getBanco().isBlank())) {
            throw new RuntimeException("Para cuentas BANCO indicá el nombre del banco");
        }

        CuentaFinanciera cuenta = new CuentaFinanciera();
        cuenta.setSucursal(sucursal);
        cuenta.setMoneda(moneda);
        cuenta.setNombre(request.getNombre().trim());
        cuenta.setNumero(numero);
        cuenta.setTipo(tipoCuenta);
        cuenta.setBanco(blankToNull(request.getBanco()));
        cuenta.setSaldoInicial(request.getSaldoInicial() != null ? request.getSaldoInicial() : BigDecimal.ZERO);
        cuenta.setSaldoActual(request.getSaldoInicial() != null ? request.getSaldoInicial() : BigDecimal.ZERO);
        cuenta.setEmpleadoResponsable(resolverEmpleado(request.getEmpleadoId()));
        cuenta.setActivo(request.getActivo() != null ? request.getActivo() : true);

        CuentaFinanciera saved = cuentaRepository.save(cuenta);
        return mapCuentaFinancieraResponse(saved);
    }

    @Transactional
    public TransferenciaCuentaResponse transferir(TransferenciaCuentaRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String responsable = auth != null && auth.getName() != null ? auth.getName() : "sistema";

        CajaMovimientoService.TransferenciaResult result = cajaMovimientoService.transferir(
                request.getCuentaOrigenId(),
                request.getCuentaDestinoId(),
                request.getMonto(),
                request.getDescripcion(),
                responsable
        );

        return TransferenciaCuentaResponse.builder()
                .movimientoOrigenId(result.enviado().getId())
                .movimientoDestinoId(result.recibido().getId())
                .cuentaOrigenId(result.origen().getId())
                .cuentaOrigenNombre(result.origen().getNombre())
                .cuentaDestinoId(result.destino().getId())
                .cuentaDestinoNombre(result.destino().getNombre())
                .monto(request.getMonto())
                .saldoOrigenNuevo(result.enviado().getSaldoNuevo())
                .saldoDestinoNuevo(result.recibido().getSaldoNuevo())
                .referencia(result.referencia())
                .descripcion(result.descripcion())
                .build();
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
        Empleado empleado = cuenta.getEmpleadoResponsable();
        if (empleado != null) {
            response.setEmpleadoId(empleado.getId());
            response.setEmpleadoNombre(nombreCompletoEmpleado(empleado));
        }
        response.setActivo(cuenta.getActivo());
        response.setCreatedAt(cuenta.getCreatedAt());
        response.setUpdatedAt(cuenta.getUpdatedAt());
        return response;
    }

    private Empleado resolverEmpleado(Long empleadoId) {
        if (empleadoId == null) {
            return null;
        }
        return empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con ID: " + empleadoId));
    }

    private String nombreCompletoEmpleado(Empleado empleado) {
        String nombre = empleado.getNombre() != null ? empleado.getNombre() : "";
        String apellido = empleado.getApellido() != null ? empleado.getApellido() : "";
        String completo = (nombre + " " + apellido).trim();
        return completo.isEmpty() ? null : completo;
    }

    private String generarNumero(CuentaFinanciera.TipoCuenta tipo) {
        String prefix = switch (tipo) {
            case CAJA -> "CAJA";
            case BANCO -> "BANCO";
            case AHORRO -> "AHORRO";
            case TARJETA_DEBITO -> "TDEB";
            case TARJETA_CREDITO -> "TCRED";
            case BILLETERA -> "BILL";
            case VIRTUAL -> "VIRT";
        };
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
