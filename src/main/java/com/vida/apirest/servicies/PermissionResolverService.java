package com.vida.apirest.servicies;

import com.vida.apirest.dto.auth.EffectivePermissions;
import com.vida.apirest.dto.role.RoleDTO;
import com.vida.apirest.model.auth.Role;
import com.vida.apirest.model.auth.Usuario;
import com.vida.apirest.repositories.RolePermisoRepository;
import com.vida.apirest.repositories.UsuarioPermisoDenyRepository;
import com.vida.apirest.repositories.UsuarioPermisoGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionResolverService {

    private final RolePermisoRepository rolePermisoRepository;
    private final UsuarioPermisoGrantRepository grantRepository;
    private final UsuarioPermisoDenyRepository denyRepository;

    @Transactional(readOnly = true)
    public EffectivePermissions resolve(Usuario usuario) {
        List<Long> roleIds = usuario.getUsuarioHasRoles().stream()
                .map(uhr -> uhr.getRole().getId())
                .toList();

        Set<String> heredados = roleIds.isEmpty()
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(rolePermisoRepository.findCodigosByRoleIds(roleIds));

        Set<String> adicionales = new LinkedHashSet<>(grantRepository.findCodigosByUsuarioId(usuario.getId()));
        Set<String> denegados = new LinkedHashSet<>(denyRepository.findCodigosByUsuarioId(usuario.getId()));

        Set<String> efectivos = new LinkedHashSet<>(heredados);
        efectivos.addAll(adicionales);
        efectivos.removeAll(denegados);

        Role principal = resolveRolPrincipal(usuario);
        RoleDTO rolPrincipalDto = principal == null ? null : toRoleDto(principal);

        return new EffectivePermissions(
                rolPrincipalDto,
                heredados,
                adicionales,
                denegados,
                efectivos
        );
    }

    @Transactional(readOnly = true)
    public Collection<? extends GrantedAuthority> buildAuthorities(Usuario usuario) {
        EffectivePermissions effective = resolve(usuario);
        List<GrantedAuthority> authorities = new ArrayList<>();

        usuario.getUsuarioHasRoles().stream()
                .map(uhr -> uhr.getRole().getNombre())
                .distinct()
                .map(nombre -> new SimpleGrantedAuthority("ROLE_" + nombre))
                .forEach(authorities::add);

        effective.getPermisosEfectivos().stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return authorities;
    }

    private Role resolveRolPrincipal(Usuario usuario) {
        if (usuario.getRolPrincipal() != null) {
            return usuario.getRolPrincipal();
        }
        return usuario.getUsuarioHasRoles().stream()
                .map(uhr -> uhr.getRole())
                .findFirst()
                .orElse(null);
    }

    private RoleDTO toRoleDto(Role role) {
        return new RoleDTO(role.getId(), role.getNombre(), role.getImage(), role.getRoute());
    }
}
