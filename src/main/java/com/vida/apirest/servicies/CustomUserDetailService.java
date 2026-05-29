package com.vida.apirest.servicies;

import com.vida.apirest.repositories.UsuarioRepository;
import com.vida.apirest.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PermissionResolverService permissionResolverService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var usuario = usuarioRepository.findByEmailWithRolesAndRolPrincipal(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return new AppUserDetails(usuario, permissionResolverService.buildAuthorities(usuario));
    }
}
