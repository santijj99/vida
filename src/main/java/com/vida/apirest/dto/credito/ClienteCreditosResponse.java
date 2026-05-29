package com.vida.apirest.dto.credito;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ClienteCreditosResponse {
    private Long cuentaId;
    private String cuentaNumero;
    private Long clienteId;
    private String clienteNombre;
    private String clienteApellido;
    private String clienteDni;
    private String clienteTelefono;
    private Long sucursalId;
    private BigDecimal saldoCuenta;
    private BigDecimal limiteCredito;
    private List<CreditoClienteResponse> creditosActivos;
    private List<CreditoClienteResponse> creditosCancelados;
}
