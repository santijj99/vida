package com.vida.apirest.controller;

import com.vida.apirest.dto.usuario.CambiarPasswordInicialRequest;
import com.vida.apirest.dto.usuario.CreateUsuarioRequest;
import com.vida.apirest.dto.usuario.ForgotPasswordRequest;
import com.vida.apirest.dto.usuario.LoginRequest;
import com.vida.apirest.dto.usuario.LoginResponse;
import com.vida.apirest.dto.usuario.ResetPasswordRequest;
import com.vida.apirest.dto.usuario.SoporteLoginRequest;
import com.vida.apirest.dto.usuario.UsuarioResponse;
import com.vida.apirest.servicies.UsuarioService;
import com.vida.apirest.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;

    @PostMapping(value = "/register")
    public ResponseEntity<LoginResponse> create(@RequestBody CreateUsuarioRequest request) {
        LoginResponse usuario = usuarioService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }

    @PostMapping(value = "/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(usuarioService.login(request));
    }

    @PostMapping(value = "/soporte")
    public ResponseEntity<LoginResponse> loginSoporte(@RequestBody SoporteLoginRequest request) {
        return ResponseEntity.ok(usuarioService.loginSoporte(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        usuarioService.forgotPassword(request);
        return ResponseEntity.ok(Map.of(
                "message", "Si el email está registrado, te enviamos un código",
                "statusCode", 200));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody ResetPasswordRequest request) {
        usuarioService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada", "statusCode", 200));
    }

    @PostMapping("/cambiar-password-inicial")
    public ResponseEntity<LoginResponse> cambiarPasswordInicial(
            @RequestBody CambiarPasswordInicialRequest request) {
        return ResponseEntity.ok(usuarioService.cambiarPasswordInicial(request));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal AppUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "message", "No autenticado",
                    "statusCode", HttpStatus.UNAUTHORIZED.value()
            ));
        }
        UsuarioResponse response = usuarioService.buildProfileResponse(userDetails.getUsuario());
        return ResponseEntity.ok(response);
    }
}
