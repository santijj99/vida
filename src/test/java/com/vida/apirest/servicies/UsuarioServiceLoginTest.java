package com.vida.apirest.servicies;

import com.vida.apirest.config.LicenciaProperties;
import com.vida.apirest.dto.usuario.LoginRequest;
import com.vida.apirest.exception.ForbiddenException;
import com.vida.apirest.model.auth.Usuario;
import com.vida.apirest.repositories.UsuarioRepository;
import com.vida.apirest.security.AuthRateLimiter;
import com.vida.apirest.tenant.TenantDataSourceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceLoginTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TenantDataSourceManager tenantDataSourceManager;
    @Mock
    private LicenciaProperties licenciaProperties;
    @Mock
    private AuthRateLimiter authRateLimiter;
    @InjectMocks
    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        when(tenantDataSourceManager.isMultiTenantEnabled()).thenReturn(false);
        when(licenciaProperties.getCodigo()).thenReturn("");
        when(authRateLimiter.tryConsume(anyString(), anyInt())).thenReturn(true);
    }

    @Test
    void usuarioInactivoNoEntra() {
        Usuario usuario = usuario("cajero", false);
        when(usuarioRepository.findByIdentificadorWithRolesAndRolPrincipal("cajero"))
                .thenReturn(Optional.of(usuario));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> usuarioService.login(request("cajero")));
        assertEquals("El usuario/email o password no son validos", ex.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void passwordIncorrectaUsaElMismoMensaje() {
        Usuario usuario = usuario("cajero", true);
        when(usuarioRepository.findByIdentificadorWithRolesAndRolPrincipal("cajero"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> usuarioService.login(request("cajero")));
        assertEquals("El usuario/email o password no son validos", ex.getMessage());
    }

    @Test
    void soporteNoEntraPorPassword() {
        Usuario usuario = usuario("soporte", true);
        usuario.setEsSoporte(true);
        when(usuarioRepository.findByIdentificadorWithRolesAndRolPrincipal("soporte"))
                .thenReturn(Optional.of(usuario));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> usuarioService.login(request("soporte")));
        assertEquals("Usá el ticket de soporte para entrar", ex.getMessage());
    }

    private static Usuario usuario(String nombre, boolean activo) {
        Usuario usuario = new Usuario();
        usuario.setUsuario(nombre);
        usuario.setActivo(activo);
        usuario.setPassword("hash");
        return usuario;
    }

    private static LoginRequest request(String identificador) {
        LoginRequest request = new LoginRequest();
        request.setIdentificador(identificador);
        request.setPassword("secret");
        return request;
    }
}
