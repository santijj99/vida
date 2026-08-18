package com.vida.apirest.controller;

import com.vida.apirest.config.AuthRateLimitFilter;
import com.vida.apirest.dto.sistema.SistemaInfoResponse;
import com.vida.apirest.exception.TooManyRequestsException;
import com.vida.apirest.security.AuthRateLimiter;
import com.vida.apirest.security.Authz;
import com.vida.apirest.servicies.licencia.SistemaLicenciaService;
import com.vida.apirest.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sistema")
@RequiredArgsConstructor
public class SistemaController {

    private static final int MAX_VALIDAR_POR_USUARIO = 5;
    private static final int MAX_VALIDAR_POR_IP = 10;
    private static final String MSG_RATE_LIMIT =
            "Demasiadas revalidaciones. Probá de nuevo en unos minutos.";

    private final SistemaLicenciaService sistemaLicenciaService;
    private final AuthRateLimiter limiter;

    @GetMapping("/info")
    @PreAuthorize(Authz.VER_O_GESTIONAR_ORGANIZACION)
    public ResponseEntity<SistemaInfoResponse> info() {
        return ResponseEntity.ok(sistemaLicenciaService.obtenerInfo(false));
    }

    @PostMapping("/licencia/validar")
    @PreAuthorize(Authz.GESTIONAR_ORGANIZACION)
    public ResponseEntity<SistemaInfoResponse> validarAhora(HttpServletRequest request) {
        String tenant = tenantPart();
        String user = currentUsername();
        String ip = AuthRateLimitFilter.clientIp(request);
        if (!limiter.tryConsume("sistema-validar-user:" + tenant + ":" + user, MAX_VALIDAR_POR_USUARIO)
                || !limiter.tryConsume("sistema-validar-ip:" + tenant + ":" + ip, MAX_VALIDAR_POR_IP)) {
            throw new TooManyRequestsException(MSG_RATE_LIMIT);
        }
        return ResponseEntity.ok(sistemaLicenciaService.obtenerInfo(true));
    }

    private static String tenantPart() {
        String codigo = TenantContext.getCodigoLicencia();
        return codigo == null || codigo.isBlank() ? "-" : codigo;
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return "anon";
        }
        return auth.getName();
    }
}
