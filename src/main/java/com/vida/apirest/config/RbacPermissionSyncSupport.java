package com.vida.apirest.config;

import com.vida.apirest.model.auth.PermisoCodigo;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

/**
 * Sync aditivo de permisos del catálogo hacia ADMINISTRADOR
 * (y GESTIONAR operativos hacia EMPLEADO).
 * Seguro para prod: solo inserta faltantes, no borra ni resetea asignaciones.
 */
@Slf4j
public final class RbacPermissionSyncSupport {

    /** Cajeros: cobrar, caja, clientes y cuotas. No historial/artículos/org. */
    private static final List<String> EMPLEADO_GESTIONAR = List.of(
            PermisoCodigo.GESTIONAR_VENTAS,
            PermisoCodigo.GESTIONAR_CAJA,
            PermisoCodigo.GESTIONAR_CLIENTES,
            PermisoCodigo.GESTIONAR_CUENTAS
    );

    private RbacPermissionSyncSupport() {
    }

    public static void syncCatalogoYAdmin(DataSource dataSource) {
        if (!tableExists(dataSource, "permiso") || !tableExists(dataSource, "roles")) {
            return;
        }
        int creados = 0;
        int asignados = 0;
        int empleado = 0;
        for (String codigo : PermisoCodigo.todos()) {
            if (ensurePermiso(dataSource, codigo)) {
                creados++;
            }
            if (ensureRoleLink(dataSource, codigo, "ADMINISTRADOR")) {
                asignados++;
            }
        }
        for (String codigo : EMPLEADO_GESTIONAR) {
            if (ensureRoleLink(dataSource, codigo, "EMPLEADO")) {
                empleado++;
            }
        }
        if (creados > 0 || asignados > 0 || empleado > 0) {
            log.info("RBAC sync: {} permisos creados, {} a ADMINISTRADOR, {} GESTIONAR a EMPLEADO",
                    creados, asignados, empleado);
        }
    }

    private static boolean ensurePermiso(DataSource dataSource, String codigo) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                     INSERT INTO permiso (codigo, modulo, nombre, descripcion)
                     VALUES (?, 'Sistema', ?, ?)
                     ON CONFLICT (codigo) DO NOTHING
                     """)) {
            ps.setString(1, codigo);
            ps.setString(2, codigo);
            ps.setString(3, codigo);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            log.warn("No se pudo asegurar permiso {}: {}", codigo, e.getMessage());
            return false;
        }
    }

    private static boolean ensureRoleLink(DataSource dataSource, String codigo, String rolNombre) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                     INSERT INTO role_permiso (role_id, permiso_id)
                     SELECT r.id, p.id
                     FROM roles r
                     CROSS JOIN permiso p
                     WHERE UPPER(TRIM(r.nombre)) = ? AND p.codigo = ?
                       AND NOT EXISTS (
                         SELECT 1 FROM role_permiso rp
                         WHERE rp.role_id = r.id AND rp.permiso_id = p.id
                       )
                     """)) {
            ps.setString(1, rolNombre);
            ps.setString(2, codigo);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            log.warn("No se pudo asignar permiso {} a {}: {}", codigo, rolNombre, e.getMessage());
            return false;
        }
    }

    private static boolean tableExists(DataSource dataSource, String table) {
        try (Connection c = dataSource.getConnection();
             ResultSet rs = c.getMetaData().getTables(null, "public", table, new String[]{"TABLE"})) {
            return rs.next();
        } catch (Exception e) {
            return false;
        }
    }
}
