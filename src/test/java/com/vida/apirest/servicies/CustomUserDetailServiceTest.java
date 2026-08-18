package com.vida.apirest.servicies;

import com.vida.apirest.model.auth.Usuario;
import com.vida.apirest.repositories.UsuarioRepository;
import com.vida.apirest.security.AppUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PermissionResolverService permissionResolverService;
    @InjectMocks
    private CustomUserDetailService service;

    @Test
    void usuarioInactivoNoCarga() {
        Usuario usuario = new Usuario();
        usuario.setUsuario("cajero");
        usuario.setActivo(false);
        when(usuarioRepository.findByIdentificadorWithRolesAndRolPrincipal("cajero"))
                .thenReturn(Optional.of(usuario));

        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("cajero"));
        assertEquals("Usuario inactivo", ex.getMessage());
    }

    @Test
    void soporteVencidoSeDesactiva() {
        Usuario usuario = new Usuario();
        usuario.setUsuario("soporte");
        usuario.setActivo(true);
        usuario.setEsSoporte(true);
        usuario.setSoporteExpiraAt(Instant.now().minusSeconds(60));
        when(usuarioRepository.findByIdentificadorWithRolesAndRolPrincipal("soporte"))
                .thenReturn(Optional.of(usuario));

        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("soporte"));
        assertEquals("Sesión de soporte vencida", ex.getMessage());
        verify(usuarioRepository).save(usuario);
        assertEquals(false, usuario.getActivo());
    }

    @Test
    void usuarioActivoDevuelveAppUserDetails() {
        Usuario usuario = new Usuario();
        usuario.setUsuario("cajero");
        usuario.setActivo(true);
        when(usuarioRepository.findByIdentificadorWithRolesAndRolPrincipal("cajero"))
                .thenReturn(Optional.of(usuario));
        when(permissionResolverService.buildAuthorities(usuario)).thenReturn(List.of());

        assertInstanceOf(AppUserDetails.class, service.loadUserByUsername("cajero"));
    }
}
