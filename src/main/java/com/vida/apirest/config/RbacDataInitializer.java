package com.vida.apirest.config;

import com.vida.apirest.model.auth.Permiso;
import com.vida.apirest.model.auth.PermisoCodigo;
import com.vida.apirest.model.auth.Role;
import com.vida.apirest.model.auth.RolePermiso;
import com.vida.apirest.model.auth.Usuario;
import com.vida.apirest.model.auth.UsuarioHasRoles;
import com.vida.apirest.model.auth.UsuarioPermisoGrant;
import com.vida.apirest.model.auth.id.UsuarioRoleId;
import com.vida.apirest.repositories.PermisoRepository;
import com.vida.apirest.repositories.RolePermisoRepository;
import com.vida.apirest.repositories.RoleRepository;
import com.vida.apirest.repositories.UsuarioHasRoleRepository;
import com.vida.apirest.repositories.UsuarioPermisoDenyRepository;
import com.vida.apirest.repositories.UsuarioPermisoGrantRepository;
import com.vida.apirest.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class RbacDataInitializer {

    private static final Map<String, String> MIGRACION_CODIGOS = Map.ofEntries(
            Map.entry("STOCK_READ", PermisoCodigo.LEER_STOCK),
            Map.entry("STOCK_DELETE", PermisoCodigo.ELIMINAR_STOCK),
            Map.entry("USER_READ", PermisoCodigo.LEER_USUARIOS),
            Map.entry("USER_CREATE", PermisoCodigo.CREAR_USUARIOS),
            Map.entry("USER_UPDATE", PermisoCodigo.MODIFICAR_USUARIOS),
            Map.entry("PERMISSION_MANAGE", PermisoCodigo.ADMINISTRAR_PERMISOS)
    );

    private final PermisoRepository permisoRepository;
    private final RoleRepository roleRepository;
    private final RolePermisoRepository rolePermisoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioHasRoleRepository usuarioHasRoleRepository;
    private final UsuarioPermisoGrantRepository grantRepository;
    private final UsuarioPermisoDenyRepository denyRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    @Order(0)
    public CommandLineRunner seedPermisos() {
        return args -> {
            migrarCodigosAntiguos();

            Map<String, PermisoSeed> catalogo = new LinkedHashMap<>();
            catalogo.put(PermisoCodigo.LEER_STOCK, new PermisoSeed("Stock", "Leer stock", "Listar stock (GET /api/stock)"));
            catalogo.put(PermisoCodigo.ELIMINAR_STOCK, new PermisoSeed("Stock", "Eliminar stock", "Eliminar stock (DELETE /api/stock/{id})"));
            catalogo.put(PermisoCodigo.LEER_USUARIOS, new PermisoSeed("Usuarios", "Leer usuarios", "Listar usuarios (GET /usuario)"));
            catalogo.put(PermisoCodigo.CREAR_USUARIOS, new PermisoSeed("Usuarios", "Crear usuarios", "Alta de usuarios (POST /usuario/admin/create)"));
            catalogo.put(PermisoCodigo.MODIFICAR_USUARIOS, new PermisoSeed("Usuarios", "Modificar usuarios", "Asignar rol (POST /usuario/{id}/asignar-rol/{rolId})"));
            catalogo.put(PermisoCodigo.ADMINISTRAR_PERMISOS, new PermisoSeed("Permisos", "Administrar permisos", "Gestión RBAC (GET/PUT /api/rbac/**)"));

            catalogo.forEach((codigo, seed) -> {
                permisoRepository.findByCodigo(codigo).ifPresentOrElse(
                        existente -> {
                            existente.setNombre(seed.nombre());
                            existente.setModulo(seed.modulo());
                            existente.setDescripcion(seed.descripcion());
                            permisoRepository.save(existente);
                        },
                        () -> {
                            Permiso permiso = new Permiso();
                            permiso.setCodigo(codigo);
                            permiso.setNombre(seed.nombre());
                            permiso.setModulo(seed.modulo());
                            permiso.setDescripcion(seed.descripcion());
                            permisoRepository.save(permiso);
                        }
                );
            });

            eliminarPermisosObsoletos();
        };
    }

    @Bean
    @Order(5)
    public CommandLineRunner seedRbacRolesAndAssignments() {
        return args -> {
            ensureRole("DEPOSITO");

            Role admin = roleRepository.findByNombre("ADMINISTRADOR").orElse(null);
            Role empleado = roleRepository.findByNombre("EMPLEADO").orElse(null);
            Role deposito = roleRepository.findByNombre("DEPOSITO").orElse(null);

            if (admin != null) {
                assignAllPermissions(admin);
            }
            if (empleado != null) {
                assignPermissions(empleado, List.of(
                        PermisoCodigo.LEER_STOCK,
                        PermisoCodigo.LEER_USUARIOS
                ));
            }
            if (deposito != null) {
                assignPermissions(deposito, List.of(
                        PermisoCodigo.LEER_STOCK
                ));
            }

            seedDepositoExampleUser(deposito);
        };
    }

    private void migrarCodigosAntiguos() {
        MIGRACION_CODIGOS.forEach((viejo, nuevo) ->
                permisoRepository.findByCodigo(viejo).ifPresent(permisoViejo -> {
                    if (permisoRepository.findByCodigo(nuevo).isEmpty()) {
                        permisoViejo.setCodigo(nuevo);
                        permisoRepository.save(permisoViejo);
                    } else {
                        rolePermisoRepository.deleteByPermisoId(permisoViejo.getId());
                        grantRepository.deleteByPermisoId(permisoViejo.getId());
                        denyRepository.deleteByPermisoId(permisoViejo.getId());
                        permisoRepository.delete(permisoViejo);
                    }
                })
        );
    }

    private void eliminarPermisosObsoletos() {
        Set<String> validos = Set.copyOf(PermisoCodigo.todos());
        permisoRepository.findAll().stream()
                .filter(p -> !validos.contains(p.getCodigo()))
                .forEach(p -> {
                    rolePermisoRepository.deleteByPermisoId(p.getId());
                    grantRepository.deleteByPermisoId(p.getId());
                    denyRepository.deleteByPermisoId(p.getId());
                    permisoRepository.delete(p);
                });
    }

    private void seedDepositoExampleUser(Role deposito) {
        if (deposito == null) {
            return;
        }
        Usuario depositoUser = usuarioRepository.findByEmail("deposito@gmail.com").orElse(null);
        if (depositoUser == null) {
            depositoUser = new Usuario();
            depositoUser.setUsuario("deposito");
            depositoUser.setEmail("deposito@gmail.com");
            depositoUser.setCelular("3810000000");
            depositoUser.setPassword(passwordEncoder.encode("1234"));
            depositoUser.setActivo(true);
            depositoUser.setRolPrincipal(deposito);
            depositoUser = usuarioRepository.save(depositoUser);
        } else {
            depositoUser.setRolPrincipal(deposito);
            usuarioRepository.save(depositoUser);
        }

        UsuarioRoleId roleLinkId = new UsuarioRoleId(depositoUser.getId(), deposito.getId());
        if (!usuarioHasRoleRepository.existsById(roleLinkId)) {
            usuarioHasRoleRepository.save(new UsuarioHasRoles(depositoUser, deposito));
        }

        Permiso eliminarStock = permisoRepository.findByCodigo(PermisoCodigo.ELIMINAR_STOCK).orElse(null);
        if (eliminarStock != null) {
            var grantId = new com.vida.apirest.model.auth.id.UsuarioPermisoId(
                    depositoUser.getId(), eliminarStock.getId());
            if (!grantRepository.existsById(grantId)) {
                grantRepository.save(new UsuarioPermisoGrant(depositoUser, eliminarStock));
            }
        }
    }

    private void ensureRole(String nombre) {
        if (!roleRepository.existsByNombre(nombre)) {
            Role role = new Role();
            role.setNombre(nombre);
            roleRepository.save(role);
        }
    }

    private void assignAllPermissions(Role role) {
        permisoRepository.findAll().stream()
                .filter(p -> PermisoCodigo.todos().contains(p.getCodigo()))
                .forEach(permiso -> {
                    if (rolePermisoRepository.findById(
                            new com.vida.apirest.model.auth.id.RolePermisoId(role.getId(), permiso.getId())).isEmpty()) {
                        rolePermisoRepository.save(new RolePermiso(role, permiso));
                    }
                });
    }

    private void assignPermissions(Role role, List<String> codigos) {
        for (String codigo : codigos) {
            permisoRepository.findByCodigo(codigo).ifPresent(permiso -> {
                if (rolePermisoRepository.findById(
                        new com.vida.apirest.model.auth.id.RolePermisoId(role.getId(), permiso.getId())).isEmpty()) {
                    rolePermisoRepository.save(new RolePermiso(role, permiso));
                }
            });
        }
    }

    private record PermisoSeed(String modulo, String nombre, String descripcion) {
    }
}
