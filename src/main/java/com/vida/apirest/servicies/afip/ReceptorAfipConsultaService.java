package com.vida.apirest.servicies.afip;

import com.vida.apirest.dto.afip.ReceptorAfipConsultaResponse;
import com.vida.apirest.model.afip.ClienteAFIP;
import com.vida.apirest.model.persona.Cliente;
import com.vida.apirest.model.persona.Direccion;
import com.vida.apirest.repositories.ClienteAFIPRepository;
import com.vida.apirest.repositories.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReceptorAfipConsultaService {

    private static final Logger log = LoggerFactory.getLogger(ReceptorAfipConsultaService.class);

    private final ClienteRepository clienteRepository;
    private final ClienteAFIPRepository clienteAFIPRepository;
    private final PadronA13Service padronA13Service;

    public ReceptorAfipConsultaResponse consultar(Integer docTipo, String docNro) {
        if (docTipo == null || docNro == null || docNro.isBlank()) {
            return noEncontrado("Ingresá tipo y número de documento");
        }

        String numero = docNro.trim();
        if (docTipo == 99) {
            return noEncontrado("Consumidor final sin identificar");
        }

        if (docTipo == 80 && numero.length() != 11) {
            return noEncontrado("El CUIT debe tener 11 dígitos");
        }
        if (docTipo == 96 && (numero.length() < 7 || numero.length() > 8)) {
            return noEncontrado("El DNI debe tener 7 u 8 dígitos");
        }

        return clienteRepository.findByDni(numero)
                .map(c -> desdeCliente(c, "Cliente registrado"))
                .orElseGet(() -> clienteAFIPRepository
                        .findFirstByDocTipoAndDocNroOrderByIdClienteAFIPDesc(docTipo, numero)
                        .map(c -> desdeClienteAfip(c, "Historial ARCA"))
                        .orElseGet(() -> consultarPadron(docTipo, numero)));
    }

    private ReceptorAfipConsultaResponse consultarPadron(Integer docTipo, String numero) {
        try {
            PadronA13Service.DatosPadron padron = docTipo == 80
                    ? padronA13Service.consultarPorCuit(numero)
                    : padronA13Service.consultarPorDni(numero);
            return ReceptorAfipConsultaResponse.builder()
                    .encontrado(true)
                    .razonSocial(padron.razonSocial())
                    .domicilio(padron.domicilio())
                    .condicionIVAReceptorId(padron.condicionIVAReceptorId())
                    .fuente("Padrón AFIP (A13)")
                    .mensaje("Datos obtenidos de ARCA/AFIP")
                    .build();
        } catch (Exception e) {
            log.warn("Consulta padrón docTipo={} nro={} falló: {}", docTipo, numero, e.getMessage());
            return noEncontrado(mensajePadronFallido(e));
        }
    }

    private String mensajePadronFallido(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        if (msg.contains("no tiene autorizado") || msg.contains("notAuthorized")) {
            return msg;
        }
        return "No se encontró en AFIP: " + msg;
    }

    private ReceptorAfipConsultaResponse desdeCliente(Cliente cliente, String fuente) {
        return ReceptorAfipConsultaResponse.builder()
                .encontrado(true)
                .razonSocial(formatearNombre(cliente))
                .domicilio(formatearDireccion(cliente.getDireccion()))
                .condicionIVAReceptorId(5)
                .fuente(fuente)
                .mensaje("Cliente encontrado en el sistema")
                .build();
    }

    private ReceptorAfipConsultaResponse desdeClienteAfip(ClienteAFIP clienteAFIP, String fuente) {
        return ReceptorAfipConsultaResponse.builder()
                .encontrado(true)
                .razonSocial(clienteAFIP.getRazonSocial())
                .domicilio(clienteAFIP.getDomicilio())
                .condicionIVAReceptorId(clienteAFIP.getCondicionIVAReceptorId())
                .fuente(fuente)
                .mensaje("Datos de facturación previa")
                .build();
    }

    private ReceptorAfipConsultaResponse noEncontrado(String mensaje) {
        return ReceptorAfipConsultaResponse.builder()
                .encontrado(false)
                .mensaje(mensaje)
                .build();
    }

    private String formatearNombre(Cliente cliente) {
        String apellido = cliente.getApellido() != null ? cliente.getApellido().trim() : "";
        String nombre = cliente.getNombre() != null ? cliente.getNombre().trim() : "";
        String completo = (apellido + " " + nombre).trim();
        return completo.isEmpty() ? "Consumidor Final" : completo;
    }

    private String formatearDireccion(Direccion direccion) {
        if (direccion == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (direccion.getCalle() != null) {
            sb.append(direccion.getCalle());
        }
        if (direccion.getNumero() != null) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(direccion.getNumero());
        }
        if (direccion.getLocalidad() != null) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(direccion.getLocalidad());
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
