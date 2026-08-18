package com.vida.apirest.servicies;

import com.vida.apirest.dto.ticket.TicketConfigRequest;
import com.vida.apirest.dto.ticket.TicketConfigResponse;
import com.vida.apirest.model.empresa.Empresa;
import com.vida.apirest.model.empresa.EmpresaTicketConfig;
import com.vida.apirest.model.empresa.FormatoTicketPdf;
import com.vida.apirest.repositories.EmpresaRepository;
import com.vida.apirest.repositories.EmpresaTicketConfigRepository;
import com.vida.apirest.servicies.afip.AfipContextService;
import com.vida.apirest.servicies.afip.TicketPDFService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketConfigService {

    private final EmpresaTicketConfigRepository configRepository;
    private final EmpresaRepository empresaRepository;
    private final AfipContextService afipContextService;

    @Transactional(readOnly = true)
    public TicketConfigResponse obtener(Long empresaId) {
        Long resolved = resolverEmpresaId(empresaId);
        return toResponse(obtenerODefault(resolved), resolved);
    }

    @Transactional
    public TicketConfigResponse guardar(TicketConfigRequest request) {
        Long empresaId = resolverEmpresaId(request.getEmpresaId());
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        EmpresaTicketConfig config = configRepository.findByEmpresaId(empresaId)
                .orElseGet(() -> {
                    EmpresaTicketConfig nuevo = new EmpresaTicketConfig();
                    nuevo.setEmpresa(empresa);
                    nuevo.setAbrirAutomaticamente(true);
                    nuevo.setCabeceraMostrarEtiquetaRazonSocial(true);
                    nuevo.setCabeceraMostrarDireccion(true);
                    nuevo.setCabeceraMostrarCuit(true);
                    nuevo.setCabeceraMostrarCondicionIva(true);
                    return nuevo;
                });

        if (request.getFormato() != null && !request.getFormato().isBlank()) {
            config.setFormato(parseFormato(request.getFormato()));
        }
        if (request.getAbrirAutomaticamente() != null) {
            config.setAbrirAutomaticamente(request.getAbrirAutomaticamente());
        }

        if (request.getCabeceraRazonSocial() != null) {
            config.setCabeceraRazonSocial(blankToNull(request.getCabeceraRazonSocial()));
        }
        if (request.getCabeceraDireccion() != null) {
            config.setCabeceraDireccion(blankToNull(request.getCabeceraDireccion()));
        }
        if (request.getCabeceraCuit() != null) {
            config.setCabeceraCuit(blankToNull(request.getCabeceraCuit()));
        }
        if (request.getCabeceraCondicionIva() != null) {
            config.setCabeceraCondicionIva(blankToNull(request.getCabeceraCondicionIva()));
        }
        if (request.getCabeceraIibb() != null) {
            config.setCabeceraIibb(blankToNull(request.getCabeceraIibb()));
        }
        if (request.getCabeceraInicioActividad() != null) {
            config.setCabeceraInicioActividad(blankToNull(request.getCabeceraInicioActividad()));
        }
        if (request.getCabeceraMostrarEtiquetaRazonSocial() != null) {
            config.setCabeceraMostrarEtiquetaRazonSocial(request.getCabeceraMostrarEtiquetaRazonSocial());
        }
        if (request.getCabeceraMostrarDireccion() != null) {
            config.setCabeceraMostrarDireccion(request.getCabeceraMostrarDireccion());
        }
        if (request.getCabeceraMostrarCuit() != null) {
            config.setCabeceraMostrarCuit(request.getCabeceraMostrarCuit());
        }
        if (request.getCabeceraMostrarCondicionIva() != null) {
            config.setCabeceraMostrarCondicionIva(request.getCabeceraMostrarCondicionIva());
        }

        return toResponse(configRepository.save(config), empresaId);
    }

    @Transactional(readOnly = true)
    public FormatoTicketPdf resolverFormato(Long empresaId) {
        if (empresaId == null) {
            return FormatoTicketPdf.TERMICO_80MM;
        }
        return configRepository.findByEmpresaId(empresaId)
                .map(EmpresaTicketConfig::getFormato)
                .orElse(FormatoTicketPdf.TERMICO_80MM);
    }

    @Transactional(readOnly = true)
    public boolean resolverAbrirAutomaticamente(Long empresaId) {
        if (empresaId == null) {
            return true;
        }
        return configRepository.findByEmpresaId(empresaId)
                .map(c -> Boolean.TRUE.equals(c.getAbrirAutomaticamente()))
                .orElse(true);
    }

    /**
     * Datos de cabecera para tickets no fiscales.
     * Prioridad: config de ticket → datos AFIP de la empresa → ficha Empresa.
     * (Las facturas ARCA siguen usando AfipContext directamente.)
     */
    @Transactional(readOnly = true)
    public TicketPDFService.DatosEmpresaTicket resolverDatosCabecera(Empresa empresa) {
        if (empresa == null || empresa.getId() == null) {
            return new TicketPDFService.DatosEmpresaTicket(
                    null, "", "", "", "", "", "", true, true, true, true);
        }
        EmpresaTicketConfig config = configRepository.findByEmpresaId(empresa.getId()).orElse(null);
        TicketPDFService.DatosEmpresaTicket fallback = fallbackCabecera(empresa);
        boolean mostrarEtiqueta = flagOrTrue(config == null ? null : config.getCabeceraMostrarEtiquetaRazonSocial());
        boolean mostrarDireccion = flagOrTrue(config == null ? null : config.getCabeceraMostrarDireccion());
        boolean mostrarCuit = flagOrTrue(config == null ? null : config.getCabeceraMostrarCuit());
        boolean mostrarIva = flagOrTrue(config == null ? null : config.getCabeceraMostrarCondicionIva());

        String razon;
        String direccion;
        String cuit;
        String condicionIva;
        String iibb;
        String inicio;
        if (config != null && tieneCabeceraPersonalizada(config)) {
            razon = firstNonBlank(config.getCabeceraRazonSocial(), fallback.razonSocial());
            direccion = firstNonBlank(config.getCabeceraDireccion(), fallback.direccion());
            cuit = firstNonBlank(config.getCabeceraCuit(), fallback.cuit());
            condicionIva = firstNonBlank(config.getCabeceraCondicionIva(), fallback.condicionIva());
            iibb = firstNonBlank(config.getCabeceraIibb(), fallback.iibb());
            inicio = firstNonBlank(config.getCabeceraInicioActividad(), fallback.inicioActividad());
        } else {
            razon = fallback.razonSocial();
            direccion = fallback.direccion();
            cuit = fallback.cuit();
            condicionIva = fallback.condicionIva();
            iibb = fallback.iibb();
            inicio = fallback.inicioActividad();
        }
        return new TicketPDFService.DatosEmpresaTicket(
                empresa.getId(),
                razon,
                direccion,
                cuit,
                condicionIva,
                iibb,
                inicio,
                mostrarEtiqueta,
                mostrarDireccion,
                mostrarCuit,
                mostrarIva
        );
    }

    private TicketPDFService.DatosEmpresaTicket fallbackCabecera(Empresa empresa) {
        return afipContextService.resolveOptionalForEmpresaId(empresa.getId())
                .map(TicketPDFService.DatosEmpresaTicket::from)
                .orElseGet(() -> TicketPDFService.DatosEmpresaTicket.fromEmpresa(empresa));
    }

    private boolean tieneCabeceraPersonalizada(EmpresaTicketConfig config) {
        return notBlank(config.getCabeceraRazonSocial())
                || notBlank(config.getCabeceraDireccion())
                || notBlank(config.getCabeceraCuit())
                || notBlank(config.getCabeceraCondicionIva())
                || notBlank(config.getCabeceraIibb())
                || notBlank(config.getCabeceraInicioActividad());
    }

    private EmpresaTicketConfig obtenerODefault(Long empresaId) {
        return configRepository.findByEmpresaId(empresaId).orElseGet(() -> {
            EmpresaTicketConfig def = new EmpresaTicketConfig();
            def.setFormato(FormatoTicketPdf.TERMICO_80MM);
            def.setAbrirAutomaticamente(true);
            def.setCabeceraMostrarEtiquetaRazonSocial(true);
            def.setCabeceraMostrarDireccion(true);
            def.setCabeceraMostrarCuit(true);
            def.setCabeceraMostrarCondicionIva(true);
            if (empresaRepository.existsById(empresaId)) {
                def.setEmpresa(empresaRepository.findById(empresaId).orElse(null));
            }
            return def;
        });
    }

    private Long resolverEmpresaId(Long empresaId) {
        if (empresaId != null) {
            return empresaId;
        }
        return afipContextService.resolveEmpresaIdForCurrentUser()
                .orElseThrow(() -> new RuntimeException(
                        "No hay empresas en esta cuenta. Creá una en Organización → Empresas."));
    }

    private FormatoTicketPdf parseFormato(String valor) {
        try {
            return FormatoTicketPdf.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return FormatoTicketPdf.TERMICO_80MM;
        }
    }

    private TicketConfigResponse toResponse(EmpresaTicketConfig config, Long empresaId) {
        TicketConfigResponse dto = new TicketConfigResponse();
        dto.setId(config.getId());
        Empresa empresa = config.getEmpresa();
        if (empresa == null && empresaId != null) {
            empresa = empresaRepository.findById(empresaId).orElse(null);
        }
        if (empresa != null) {
            dto.setEmpresaId(empresa.getId());
            dto.setEmpresaNombre(empresa.getNombre());
        }
        dto.setFormato(config.getFormato() != null
                ? config.getFormato().name()
                : FormatoTicketPdf.TERMICO_80MM.name());
        dto.setAbrirAutomaticamente(config.getAbrirAutomaticamente() == null
                || Boolean.TRUE.equals(config.getAbrirAutomaticamente()));

        TicketPDFService.DatosEmpresaTicket fallback = empresa != null
                ? fallbackCabecera(empresa)
                : new TicketPDFService.DatosEmpresaTicket(
                        empresaId, "", "", "", "", "", "", true, true, true, true);

        // Mostrar valor guardado o, si aún no hay, el que se usaría hoy (empresa / AFIP).
        dto.setCabeceraRazonSocial(firstNonBlank(config.getCabeceraRazonSocial(), fallback.razonSocial()));
        dto.setCabeceraDireccion(firstNonBlank(config.getCabeceraDireccion(), fallback.direccion()));
        dto.setCabeceraCuit(firstNonBlank(config.getCabeceraCuit(), fallback.cuit()));
        dto.setCabeceraCondicionIva(firstNonBlank(config.getCabeceraCondicionIva(), fallback.condicionIva()));
        dto.setCabeceraIibb(firstNonBlank(config.getCabeceraIibb(), fallback.iibb()));
        dto.setCabeceraInicioActividad(firstNonBlank(config.getCabeceraInicioActividad(), fallback.inicioActividad()));
        dto.setCabeceraMostrarEtiquetaRazonSocial(flagOrTrue(config.getCabeceraMostrarEtiquetaRazonSocial()));
        dto.setCabeceraMostrarDireccion(flagOrTrue(config.getCabeceraMostrarDireccion()));
        dto.setCabeceraMostrarCuit(flagOrTrue(config.getCabeceraMostrarCuit()));
        dto.setCabeceraMostrarCondicionIva(flagOrTrue(config.getCabeceraMostrarCondicionIva()));
        return dto;
    }

    private static String blankToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean notBlank(String v) {
        return v != null && !v.isBlank();
    }

    private static boolean flagOrTrue(Boolean value) {
        return value == null || Boolean.TRUE.equals(value);
    }

    private static String firstNonBlank(String preferred, String fallback) {
        if (notBlank(preferred)) return preferred.trim();
        return fallback != null ? fallback : "";
    }
}
