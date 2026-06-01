package com.vida.apirest.dto.afip;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Builder
public class FacturaAFIPResponse {
    private Long id;
    private Long ventaId;
    private Integer cbteTipo;
    private String tipoComprobante;
    private Integer ptoVta;
    private Long cbteNro;
    private String cbteFch;
    private String cae;
    private String caeFchVto;
    private String resultado;
    private String estado;
    private String motivos;
    private String observaciones;
    private BigDecimal impTotal;
    private BigDecimal impNeto;
    private BigDecimal impIVA;
    private Date fechaEmision;
    private ClienteAFIPResponse cliente;
    private List<ItemResponse> items;

    @Data
    @Builder
    public static class ClienteAFIPResponse {
        private String razonSocial;
        private Integer docTipo;
        private String docNro;
        private Integer condicionIVAReceptorId;
        private String domicilio;
    }

    @Data
    @Builder
    public static class ItemResponse {
        private String descripcion;
        private BigDecimal cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal subtotal;
        private String codigo;
    }
}
