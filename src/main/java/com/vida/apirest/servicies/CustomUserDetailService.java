package com.vida.apirest.servicies;

import com.vida.apirest.repositories.UsuarioRepository;
import com.vida.apirest.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PermissionResolverService permissionResolverService;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String identificador) throws UsernameNotFoundException {
        var usuario = usuarioRepository.findByIdentificadorWithRolesAndRolPrincipal(identificador)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        if (usuario.soporteVencido()) {
            usuario.setActivo(false);
            usuarioRepository.save(usuario);
            throw new UsernameNotFoundException("Sesión de soporte vencida");
        }
        if (!usuario.isEnabled()) {
            throw new UsernameNotFoundException("Usuario inactivo");
        }
        return new AppUserDetails(usuario, permissionResolverService.buildAuthorities(usuario));
    }
}
