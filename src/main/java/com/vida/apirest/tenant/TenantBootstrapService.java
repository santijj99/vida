package com.vida.apirest.tenant;

import com.vida.apirest.config.CreditoSchemaSupport;
import com.vida.apirest.config.RbacPermissionSyncSupport;
import com.vida.apirest.config.SueldoSchemaSupport;
import com.vida.apirest.config.TicketSchemaSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Crea schema (hbm2ddl update) y un admin mínimo en DBs de tenant vacías.
 * En tenants ya poblados, sincroniza permisos faltantes y DDL de módulos nuevos.
 * Usa {@link ObjectProvider} para no crear un ciclo con el DataSource multi-tenant.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantBootstrapService {

    private final ObjectProvider<EntityManagerFactoryBuilder> entityManagerFactoryBuilder;
    private final PasswordEncoder passwordEncoder;

    public void bootstrapIfNeeded(DataSource dataSource, String codigoLicencia) {
        try {
            boolean hasUsuarioTable = tableExists(dataSource, "usuario");
            if (!hasUsuarioTable) {
                log.info("Tenant {}: DB sin schema, aplicando hibernate ddl update...", codigoLicencia);
                applySchema(dataSource);
            }
            if (!hasAnyUsuario(dataSource)) {
                log.info("Tenant {}: DB sin usuarios, creando admin de bootstrap...", codigoLicencia);
                seedMinimalAdmin(dataSource);
            }
            // Siempre: reparar permisos/módulos nuevos en bases existentes (prod multi-tenant).
            RbacPermissionSyncSupport.syncCatalogoYAdmin(dataSource);
            SueldoSchemaSupport.apply(dataSource);
            TicketSchemaSupport.apply(dataSource);
            CreditoSchemaSupport.apply(dataSource);
        } catch (Exception ex) {
            log.error("Tenant {}: falló el bootstrap de schema/seed: {}", codigoLicencia, ex.getMessage(), ex);
            throw new IllegalStateException(
                    "La base del tenant está vacía o incompleta y no se pudo inicializar: " + ex.getMessage(),
                    ex
            );
        }
    }

    private EntityManagerFactoryBuilder builder() {
        EntityManagerFactoryBuilder b = entityManagerFactoryBuilder.getIfAvailable();
        if (b == null) {
            throw new IllegalStateException("EntityManagerFactoryBuilder no disponible");
        }
        return b;
    }

    private void applySchema(DataSource dataSource) {
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        LocalContainerEntityManagerFactoryBean factoryBean = builder()
                .dataSource(dataSource)
                .packages("com.vida.apirest.model")
                .properties(props)
                .build();
        factoryBean.afterPropertiesSet();
        EntityManagerFactory emf = factoryBean.getObject();
        if (emf != null) {
            emf.createEntityManager().close();
            emf.close();
        }
        factoryBean.destroy();
    }

    private void seedMinimalAdmin(DataSource dataSource) {
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        LocalContainerEntityManagerFactoryBean factoryBean = builder()
                .dataSource(dataSource)
                .packages("com.vida.apirest.model")
                .properties(props)
                .build();
        factoryBean.afterPropertiesSet();
        EntityManagerFactory emf = factoryBean.getObject();
        if (emf == null) {
            throw new IllegalStateException("No se pudo crear EntityManagerFactory para seed del tenant");
        }

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            for (String roleName : List.of("CLIENTE", "EMPLEADO", "ADMINISTRADOR", "DEPOSITO")) {
                Long count = em.createQuery(
                                "select count(r) from Role r where r.nombre = :n", Long.class)
                        .setParameter("n", roleName)
                        .getSingleResult();
                if (count == 0) {
                    com.vida.apirest.model.auth.Role role = new com.vida.apirest.model.auth.Role();
                    role.setNombre(roleName);
                    em.persist(role);
                }
            }
            em.flush();

            com.vida.apirest.model.auth.Role adminRole = em.createQuery(
                            "select r from Role r where r.nombre = 'ADMINISTRADOR'",
                            com.vida.apirest.model.auth.Role.class)
                    .getSingleResult();

            com.vida.apirest.model.auth.Usuario admin = new com.vida.apirest.model.auth.Usuario();
            admin.setUsuario("admin");
            admin.setEmail("admin@tenant.local");
            admin.setCelular("0000000000");
            admin.setPassword(passwordEncoder.encode("Admin123!"));
            admin.setActivo(true);
            admin.setRolPrincipal(adminRole);
            em.persist(admin);
            em.flush();

            com.vida.apirest.model.auth.UsuarioHasRoles link =
                    new com.vida.apirest.model.auth.UsuarioHasRoles(admin, adminRole);
            em.persist(link);

            seedPermisosBasicos(em, adminRole);

            em.getTransaction().commit();
            log.info("Tenant bootstrap OK: usuario=admin password=Admin123!");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        } finally {
            em.close();
            emf.close();
            factoryBean.destroy();
        }
    }

    private void seedPermisosBasicos(EntityManager em, com.vida.apirest.model.auth.Role adminRole) {
        for (String codigo : com.vida.apirest.model.auth.PermisoCodigo.todos()) {
            Long exists = em.createQuery(
                            "select count(p) from Permiso p where p.codigo = :c", Long.class)
                    .setParameter("c", codigo)
                    .getSingleResult();
            com.vida.apirest.model.auth.Permiso permiso;
            if (exists == 0) {
                permiso = new com.vida.apirest.model.auth.Permiso();
                permiso.setCodigo(codigo);
                permiso.setModulo("Sistema");
                permiso.setNombre(codigo);
                permiso.setDescripcion(codigo);
                em.persist(permiso);
                em.flush();
            } else {
                permiso = em.createQuery(
                                "select p from Permiso p where p.codigo = :c",
                                com.vida.apirest.model.auth.Permiso.class)
                        .setParameter("c", codigo)
                        .getSingleResult();
            }

            Long linked = em.createQuery(
                            "select count(rp) from RolePermiso rp where rp.role.id = :rid and rp.permiso.id = :pid",
                            Long.class)
                    .setParameter("rid", adminRole.getId())
                    .setParameter("pid", permiso.getId())
                    .getSingleResult();
            if (linked == 0) {
                em.persist(new com.vida.apirest.model.auth.RolePermiso(adminRole, permiso));
            }
        }
    }

    private boolean tableExists(DataSource dataSource, String table) throws Exception {
        try (Connection c = dataSource.getConnection();
             ResultSet rs = c.getMetaData().getTables(null, "public", table, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private boolean hasAnyUsuario(DataSource dataSource) throws Exception {
        if (!tableExists(dataSource, "usuario")) {
            return false;
        }
        try (Connection c = dataSource.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("select 1 from usuario limit 1")) {
            return rs.next();
        }
    }
}
