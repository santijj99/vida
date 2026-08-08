package com.vida.apirest.servicies;

import com.vida.apirest.dto.auth.PermisoDTO;
import com.vida.apirest.dto.auth.RolePermisosResponse;
import com.vida.apirest.dto.auth.UpdateRolePermisosRequest;
import com.vida.apirest.dto.auth.UpdateUsuarioPermisosRequest;
import com.vida.apirest.dto.auth.UsuarioPermisosResponse;
import com.vida.apirest.dto.auth.EffectivePermissions;
import com.vida.apirest.dto.role.RoleDTO;
import com.vida.apirest.model.auth.Permiso;
import com.vida.apirest.model.auth.Role;
import com.vida.apirest.model.auth.RolePermiso;
import com.vida.apirest.model.auth.Usuario;
import com.vida.apirest.model.auth.UsuarioHasRoles;
import com.vida.apirest.model.auth.UsuarioPermisoDeny;
import com.vida.apirest.model.auth.UsuarioPermisoGrant;
import com.vida.apirest.repositories.PermisoRepository;
import com.vida.apirest.repositories.RolePermisoRepository;
import com.vida.apirest.repositories.RoleRepository;
import com.vida.apirest.repositories.UsuarioHasRoleRepository;
import com.vida.apirest.repositories.UsuarioPermisoDenyRepository;
import com.vida.apirest.repositories.UsuarioPermisoGrantRepository;
import com.vida.apirest.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RbacService {

    private final PermisoRepository permisoRepository;
    private final RoleRepository roleRepository;
    private final RolePermisoRepository rolePermisoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioHasRoleRepository usuarioHasRoleRepository;
    private final UsuarioPermisoGrantRepository grantRepository;
    private final UsuarioPermisoDenyRepository denyRepository;
    private final PermissionResolverService permissionResolverService;

    @Transactional(readOnly = true)
    public List<PermisoDTO> listarPermisos() {
        return permisoRepository.findAllByOrderByModuloAscCodigoAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public RolePermisosResponse obtenerPermisosRol(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
        List<RolePermiso> asignados = rolePermisoRepository.findByRoleId(roleId);
        Set<String> codigos = asignados.stream()
                .map(rp -> rp.getPermiso().getCodigo())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Long> ids = asignados.stream().map(rp -> rp.getPermiso().getId()).toList();
        return new RolePermisosResponse(role.getId(), role.getNombre(), codigos, ids);
    }

    @Transactional
    public RolePermisosResponse actualizarPermisosRol(Long roleId, UpdateRolePermisosRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
        rolePermisoRepository.deleteByRoleId(roleId);

        if (request.getPermisoIds() != null) {
            for (Long permisoId : request.getPermisoIds()) {
                Permiso permiso = permisoRepository.findById(permisoId)
                        .orElseThrow(() -> new RuntimeException("Permiso no encontrado: " + permisoId));
                rolePermisoRepository.save(new RolePermiso(role, permiso));
            }
        }
        return obtenerPermisosRol(roleId);
    }

    @Transactional(readOnly = true)
    public UsuarioPermisosResponse obtenerPermisosUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        EffectivePermissions effective = permissionResolverService.resolve(usuario);
        List<RoleDTO> roles = usuario.getUsuarioHasRoles().stream()
                .map(uhr -> new RoleDTO(
                        uhr.getRole().getId(),
                        uhr.getRole().getNombre(),
                        uhr.getRole().getImage(),
                        uhr.getRole().getRoute()))
                .toList();

        return new UsuarioPermisosResponse(
                usuario.getId(),
                usuario.getUsuario(),
                usuario.getEmail(),
                effective.getRolPrincipal(),
                roles,
                effective.getPermisosHeredados(),
                effective.getPermisosAdicionales(),
                effective.getPermisosDenegados(),
                effective.getPermisosEfectivos()
        );
    }

    @Transactional
    public UsuarioPermisosResponse actualizarPermisosUsuario(Long usuarioId, UpdateUsuarioPermisosRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (request.getRolPrincipalId() != null) {
            Role rolPrincipal = roleRepository.findById(request.getRolPrincipalId())
                    .orElseThrow(() -> new RuntimeException("Rol principal no encontrado"));
            usuario.setRolPrincipal(rolPrincipal);
            // Los permisos heredados salen de usuario_has_roles: al cambiar el rol del sistema
            // reemplazamos esa asignación para que DEPOSITO → EMPLEADO (caja) surta efecto.
            sincronizarRolAsignado(usuario, rolPrincipal);
        } else {
            usuario.setRolPrincipal(null);
        }

        grantRepository.deleteByUsuarioId(usuarioId);
        denyRepository.deleteByUsuarioId(usuarioId);

        if (request.getPermisosAdicionalesIds() != null) {
            for (Long permisoId : request.getPermisosAdicionalesIds()) {
                Permiso permiso = permisoRepository.findById(permisoId)
                        .orElseThrow(() -> new RuntimeException("Permiso no encontrado: " + permisoId));
                grantRepository.save(new UsuarioPermisoGrant(usuario, permiso));
            }
        }

        if (request.getPermisosDenegadosIds() != null) {
            for (Long permisoId : request.getPermisosDenegadosIds()) {
                Permiso permiso = permisoRepository.findById(permisoId)
                        .orElseThrow(() -> new RuntimeException("Permiso no encontrado: " + permisoId));
                denyRepository.save(new UsuarioPermisoDeny(usuario, permiso));
            }
        }

        usuarioRepository.save(usuario);
        return obtenerPermisosUsuario(usuarioId);
    }

    private void sincronizarRolAsignado(Usuario usuario, Role rol) {
        if (usuario.getUsuarioHasRoles() != null) {
            usuario.getUsuarioHasRoles().clear();
        }
        usuarioHasRoleRepository.deleteByUsuarioId(usuario.getId());
        UsuarioHasRoles link = new UsuarioHasRoles(usuario, rol);
        usuarioHasRoleRepository.save(link);
        if (usuario.getUsuarioHasRoles() != null) {
            usuario.getUsuarioHasRoles().add(link);
        }
    }

    private PermisoDTO toDto(Permiso permiso) {
        return new PermisoDTO(
                permiso.getId(),
                permiso.getCodigo(),
                permiso.getNombre(),
                permiso.getModulo(),
                permiso.getDescripcion()
        );
    }
}
