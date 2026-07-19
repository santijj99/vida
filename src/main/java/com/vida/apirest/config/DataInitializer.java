package com.vida.apirest.config;

import com.vida.apirest.model.almacen.Deposito;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.auth.Role;
import com.vida.apirest.model.persona.Empleado;
import com.vida.apirest.model.persona.Proveedor;
import com.vida.apirest.model.auth.Usuario;
import com.vida.apirest.model.auth.UsuarioHasRoles;
import com.vida.apirest.model.auth.UsuarioSucursal;
import com.vida.apirest.model.auth.id.UsuarioRoleId;
import com.vida.apirest.model.empresa.Empresa;
import com.vida.apirest.model.empresa.EmpresaAfipConfig;
import com.vida.apirest.model.finanzas.CuentaFinanciera;
import com.vida.apirest.model.finanzas.Moneda;
import com.vida.apirest.repositories.DepositoRepository;
import com.vida.apirest.repositories.EmpleadoRepository;
import com.vida.apirest.repositories.EmpresaAfipConfigRepository;
import com.vida.apirest.repositories.EmpresaRepository;
import com.vida.apirest.repositories.FinanzasCuentaFinancieraRepository;
import com.vida.apirest.repositories.MonedaRepository;
import com.vida.apirest.repositories.ProveedorRepository;
import com.vida.apirest.repositories.RoleRepository;
import com.vida.apirest.servicies.ClasificacionService;
import com.vida.apirest.repositories.SucursalRepository;
import com.vida.apirest.repositories.UsuarioHasRoleRepository;
import com.vida.apirest.repositories.UsuarioRepository;
import com.vida.apirest.repositories.UsuarioSucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.annotation.Order;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@Profile("!prod")
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final MonedaRepository monedaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final EmpresaRepository empresaRepository;
    private final EmpresaAfipConfigRepository empresaAfipConfigRepository;
    private final SucursalRepository sucursalRepository;
    private final DepositoRepository depositoRepository;
    private final FinanzasCuentaFinancieraRepository cuentaFinancieraRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioHasRoleRepository usuarioHasRoleRepository;
    private final UsuarioSucursalRepository usuarioSucursalRepository;
    private final ProveedorRepository proveedorRepository;
    private final ClasificacionService clasificacionService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    @Order(1)
    public CommandLineRunner seedRoles() {
        return args -> {
            List<String> roles = List.of("CLIENTE", "EMPLEADO", "ADMINISTRADOR");
            for (String nombre : roles) {
                if (!roleRepository.existsByNombre(nombre)) {
                    Role role = new Role();
                    role.setNombre(nombre);
                    roleRepository.save(role);
                }
            }
        };
    }


    @Bean
    @Order(2)
    public CommandLineRunner seedUsuarios() {
        return args -> {
            List<Usuario> usuarios = List.of(
                    createUsuario("santi", "santi@gmail.com", "3813458630", "1234", true),
                    createUsuario("lucio", "lucio@gmail.com", "3816163989", "1234", true)
            );

            for (Usuario usuario : usuarios) {
                if (!usuarioRepository.existsByEmail(usuario.getEmail())) {
                    usuarioRepository.save(usuario);
                }
            }
        };
    }

    @Bean
    @Order(3)
    public CommandLineRunner seedUsuariosHasRoles() {
        return args -> {
            Usuario santi = usuarioRepository.findByEmail("santi@gmail.com").orElse(null);
            Usuario lucio = usuarioRepository.findByEmail("lucio@gmail.com").orElse(null);
            Role administrador = roleRepository.findByNombre("ADMINISTRADOR").orElse(null);
            Role empleado = roleRepository.findByNombre("EMPLEADO").orElse(null);

            if (santi != null && administrador != null && empleado != null) {
                List<UsuarioHasRoles> userhasroles = List.of(
                        createUsuarioHasRoles(santi, administrador),
                        createUsuarioHasRoles(santi, empleado)
                );

                for (UsuarioHasRoles uhr : userhasroles) {
                    UsuarioRoleId id = new UsuarioRoleId(uhr.getUsuario().getId(), uhr.getRole().getId());
                    if (!usuarioHasRoleRepository.existsById(id)) {
                        usuarioHasRoleRepository.save(uhr);
                    }
                }
            }

            if (lucio != null && empleado != null) {
                UsuarioHasRoles lucioRole = createUsuarioHasRoles(lucio, empleado);
                UsuarioRoleId idLucio = new UsuarioRoleId(lucioRole.getUsuario().getId(), lucioRole.getRole().getId());
                if (!usuarioHasRoleRepository.existsById(idLucio)) {
                    usuarioHasRoleRepository.save(lucioRole);
                }
            }
        };
    }

    @Bean
    @Order(4)
    public CommandLineRunner seedEmpleados() {
        return args -> {
            Usuario santi = usuarioRepository.findByEmail("santi@gmail.com").orElse(null);
            Usuario lucio = usuarioRepository.findByEmail("lucio@gmail.com").orElse(null);

            List<Empleado> empleados = List.of(
                    createEmpleado("santiago", "martin", "43336577", true, santi),
                    createEmpleado("lucio", "argiro", "42007256", true, lucio)
            );

            for (Empleado empleado : empleados) {
                if (empleadoRepository.findFirstByDniOrderByIdAsc(empleado.getDni()).isEmpty()) {
                    empleadoRepository.save(empleado);
                }
            }
        };
    }



    @Bean
    @Order(5)
    public CommandLineRunner seedMonedas() {
        return args -> {
            // Monedas básicas
            List<Moneda> monedas = List.of(
                createMoneda("ARS", "Peso Argentino", "$", BigDecimal.ONE, 2, true, true),
                createMoneda("USD", "Dólar Estadounidense", "USD", BigDecimal.valueOf(950), 2, true, false),
                createMoneda("EUR", "Euro", "€", BigDecimal.valueOf(1050), 2, true, false),
                createMoneda("BRL", "Real Brasileño", "R$", BigDecimal.valueOf(180), 2, true, false)
            );

            for (Moneda moneda : monedas) {
                if (!monedaRepository.findByCodigo(moneda.getCodigo()).isPresent()) {
                    monedaRepository.save(moneda);
                }
            }
        };
    }

 

    @Bean
    @Order(6)
    public CommandLineRunner seedEmpresas() {
        return args -> {
            List<Empresa> empresas = List.of(
                createEmpresa("Empresa A", "EMP001", "30-12345678-9", "Razón Social A", "Calle 123", "Buenos Aires", "Buenos Aires", "Argentina", "1000", "123-456-7890", "contacto@empresaa.com", "www.empresaa.com", Empresa.EstadoEmpresa.ACTIVA, true),
                createEmpresa("Empresa B", "EMP002", "30-98765432-1", "Razón Social B", "Avenida 456", "Córdoba", "Córdoba", "Argentina", "5000", "987-654-3210", "contacto@empresab.com", "www.empresab.com", Empresa.EstadoEmpresa.ACTIVA, true)
            );

            for (Empresa empresa : empresas) {
                if (empresaRepository.findByCodigo(empresa.getCodigo()).isEmpty()) {
                    empresaRepository.save(empresa);
                }
            }
        };
    }

    @Bean
    @Order(7)
    public CommandLineRunner seedSucursales() {
        return args -> {
            Empresa empresaA = empresaRepository.findByCodigo("EMP001").orElse(null);
            if (empresaA != null) {
                List<Sucursal> sucursales = List.of(
                    createSucursal(empresaA, "Sucursal Central", "SUC001", "Calle 123", "Buenos Aires", "Buenos Aires", "Argentina", "1000", "123-456-7890", "sucursal@empresaa.com", "Juan Pérez", "9:00-18:00", Sucursal.EstadoSucursal.ACTIVA, true),
                    createSucursal(empresaA, "Sucursal Norte", "SUC002", "Avenida Norte 456", "Buenos Aires", "Buenos Aires", "Argentina", "1001", "123-456-7891", "sucursalnorte@empresaa.com", "María García", "8:00-17:00", Sucursal.EstadoSucursal.ACTIVA, true)
                );

                for (Sucursal sucursal : sucursales) {
                    if (sucursalRepository.findByCodigo(sucursal.getCodigo()).isEmpty()) {
                        sucursalRepository.save(sucursal);
                    }
                }
            }
        };
    }

    @Bean
    @Order(8)
    public CommandLineRunner seedDepositos() {
        return args -> {
            Sucursal sucursalCentral = sucursalRepository.findByCodigo("SUC001").orElse(null);
            if (sucursalCentral != null) {
                List<Deposito> depositos = List.of(
                    createDeposito(sucursalCentral, "Depósito Principal", "DEP001", "Almacén Central", "Depósito principal de la sucursal central", Deposito.Tipo.PRINCIPAL, 1000, "Juan Pérez", true),
                    createDeposito(sucursalCentral, "Depósito Secundario", "DEP002", "Almacén Secundario", "Depósito secundario para productos adicionales", Deposito.Tipo.SECUNDARIO, 500, "María García", true)
                );

                for (Deposito deposito : depositos) {
                    if (depositoRepository.findByCodigo(deposito.getCodigo()).isEmpty()) {
                        depositoRepository.save(deposito);
                    }
                }
            }
        };
    }

    @Bean
    @Order(9)
    public CommandLineRunner seedCaja() {
        return args -> {
            Sucursal sucursalCentral = sucursalRepository.findByCodigo("SUC001").orElse(null);
            Moneda monedaArs = monedaRepository.findByCodigo("ARS").orElse(null);
            Empleado empleadoResponsable = empleadoRepository.findByDni("43336577").orElse(null);

            if (sucursalCentral == null || monedaArs == null) {
                return;
            }

            CuentaFinanciera cuenta = cuentaFinancieraRepository.findByNumero("CAJ001")
                    .orElseGet(() -> cuentaFinancieraRepository.save(
                            createCuentaFinanciera(
                                    sucursalCentral,
                                    monedaArs,
                                    "Caja Principal",
                                    "CAJ001",
                                    CuentaFinanciera.TipoCuenta.CAJA,
                                    BigDecimal.ZERO,
                                    empleadoResponsable,
                                    true
                            )
                    ));

            if (cuenta.getEmpleadoResponsable() == null && empleadoResponsable != null) {
                cuenta.setEmpleadoResponsable(empleadoResponsable);
                cuentaFinancieraRepository.save(cuenta);
            }
        };
    }

    @Bean
    @Order(10)
    public CommandLineRunner seedUsuarioSucursales() {
        return args -> {
            Usuario santi = usuarioRepository.findByEmail("santi@gmail.com").orElse(null);
            Usuario lucio = usuarioRepository.findByEmail("lucio@gmail.com").orElse(null);
            Sucursal suc001 = sucursalRepository.findByCodigo("SUC001").orElse(null);
            Sucursal suc002 = sucursalRepository.findByCodigo("SUC002").orElse(null);

            if (santi != null && suc001 != null) {
                vincularSucursalSiNoExiste(santi, suc001);
            }
            if (santi != null && suc002 != null) {
                vincularSucursalSiNoExiste(santi, suc002);
            }
            if (lucio != null && suc001 != null) {
                vincularSucursalSiNoExiste(lucio, suc001);
            }
        };
    }

    @Bean
    @Order(12)
    public CommandLineRunner seedEmpresaAfipConfig() {
        return args -> {
            for (Empresa empresa : empresaRepository.findAll()) {
                if (empresaAfipConfigRepository.findByEmpresaId(empresa.getId()).isPresent()) {
                    continue;
                }
                EmpresaAfipConfig config = new EmpresaAfipConfig();
                config.setEmpresa(empresa);
                config.setPtoVta(3);
                config.setCbteTipoDefault(6);
                config.setCondicionIva("IVA Responsable Inscripto");
                String certDir = System.getenv("AFIP_CERTIFICADOS_DIR");
                if (certDir != null && !certDir.isBlank()) {
                    config.setCertificadosDirectorio(certDir.trim());
                    config.setAfipHabilitado(true);
                } else {
                    config.setAfipHabilitado(false);
                }
                empresaAfipConfigRepository.save(config);
            }
        };
    }

    @Bean
    @Order(11)
    public CommandLineRunner seedProveedores() {
        return args -> {
            if (proveedorRepository.count() > 0) {
                return;
            }
            Proveedor proveedor = new Proveedor();
            proveedor.setCodigo("PROV001");
            proveedor.setRazonSocial("Proveedor Demo");
            proveedor.setNombre("Proveedor Demo");
            proveedor.setActivo(true);
            proveedorRepository.save(proveedor);
        };
    }

    @Bean
    @Order(12)
    public CommandLineRunner seedClasificaciones() {
        return args -> {
            List<String> clasificaciones = List.of(
                    "BASKET",
                    "FUTBOL",
                    "FUTBOL 5",
                    "FUTBOL 11",
                    "VERANO"
            );
            for (String nombre : clasificaciones) {
                clasificacionService.seedSiNoExiste(nombre);
            }
        };
    }

    private Deposito createDeposito(Sucursal sucursal, String nombre, String codigo, String ubicacion, String descripcion, Deposito.Tipo tipo, Integer capacidadMaxima, String responsable, boolean activo) {
        Deposito deposito = new Deposito();
        deposito.setSucursal(sucursal);
        deposito.setNombre(nombre);
        deposito.setCodigo(codigo);
        deposito.setUbicacion(ubicacion);
        deposito.setDescripcion(descripcion);
        deposito.setTipo(tipo);
        deposito.setCapacidadMaxima(capacidadMaxima);
        deposito.setResponsable(responsable);
        deposito.setActivo(activo);
        return deposito;
    }

    private CuentaFinanciera createCuentaFinanciera(
            Sucursal sucursal,
            Moneda moneda,
            String nombre,
            String numero,
            CuentaFinanciera.TipoCuenta tipo,
            BigDecimal saldoInicial,
            Empleado empleadoResponsable,
            boolean activo
    ) {
        CuentaFinanciera cuenta = new CuentaFinanciera();
        cuenta.setSucursal(sucursal);
        cuenta.setMoneda(moneda);
        cuenta.setNombre(nombre);
        cuenta.setNumero(numero);
        cuenta.setTipo(tipo);
        cuenta.setSaldoInicial(saldoInicial);
        cuenta.setSaldoActual(saldoInicial);
        cuenta.setEmpleadoResponsable(empleadoResponsable);
        cuenta.setActivo(activo);
        return cuenta;
    }

    private Moneda createMoneda(String codigo, String nombre, String simbolo, BigDecimal tasaCambio,
                               int decimalPlaces, boolean activo, boolean predeterminada) {
        Moneda moneda = new Moneda();
        moneda.setCodigo(codigo);
        moneda.setNombre(nombre);
        moneda.setSimbolo(simbolo);
        moneda.setTasaCambio(tasaCambio);
        moneda.setDecimalPlaces(decimalPlaces);
        moneda.setActivo(activo);
        moneda.setPredeterminada(predeterminada);
        return moneda;
    }

    private Empleado createEmpleado(String nombre, String apellido, String dni, boolean activo, Usuario iduser) {
        Empleado empleado = new Empleado();
        empleado.setNombre(nombre);
        empleado.setApellido(apellido);
        empleado.setDni(dni);
        empleado.setActivo(activo);
        empleado.setUsuario(iduser);
        return empleado;
    }

    private Empresa createEmpresa(String nombre, String codigo, String cuit, String razonSocial, String domicilio, String ciudad, String provincia, String pais, String codigoPostal, String telefono, String email, String website, Empresa.EstadoEmpresa estado, boolean activo) {
        Empresa empresa = new Empresa();
        empresa.setNombre(nombre);
        empresa.setCodigo(codigo);
        empresa.setCuit(cuit);
        empresa.setRazonSocial(razonSocial);
        empresa.setDomicilio(domicilio);
        empresa.setCiudad(ciudad);
        empresa.setProvincia(provincia);
        empresa.setPais(pais);
        empresa.setCodigoPostal(codigoPostal);
        empresa.setTelefono(telefono);
        empresa.setEmail(email);
        empresa.setWebsite(website);
        empresa.setEstado(estado);
        empresa.setActivo(activo);
        return empresa;
    }

    private Sucursal createSucursal(Empresa empresa, String nombre, String codigo, String domicilio, String ciudad, String provincia, String pais, String codigoPostal, String telefono, String email, String responsable, String horarioAtencion, Sucursal.EstadoSucursal estado, boolean activo) {
        Sucursal sucursal = new Sucursal();
        sucursal.setEmpresa(empresa);
        sucursal.setNombre(nombre);
        sucursal.setCodigo(codigo);
        sucursal.setDomicilio(domicilio);
        sucursal.setCiudad(ciudad);
        sucursal.setProvincia(provincia);
        sucursal.setPais(pais);
        sucursal.setCodigoPostal(codigoPostal);
        sucursal.setTelefono(telefono);
        sucursal.setEmail(email);
        sucursal.setResponsable(responsable);
        sucursal.setHorarioAtencion(horarioAtencion);
        sucursal.setEstado(estado);
        sucursal.setActivo(activo);
        return sucursal;
    }

    private Usuario createUsuario(String usuario, String email, String celular, String password, boolean activo) {
        Usuario user = new Usuario();
        user.setUsuario(usuario);
        user.setEmail(email);
        user.setCelular(celular);
        user.setPassword(passwordEncoder.encode(password));
        user.setActivo(activo);
        return user;
    }

    private UsuarioHasRoles createUsuarioHasRoles(Usuario usuario, Role roles) {
        UsuarioHasRoles userhasroles = new UsuarioHasRoles();
        userhasroles.setUsuario(usuario);
        userhasroles.setRole(roles);
        return userhasroles;
    }

    private void vincularSucursalSiNoExiste(Usuario usuario, Sucursal sucursal) {
        if (!usuarioSucursalRepository.existsByUsuario_IdAndSucursal_Id(usuario.getId(), sucursal.getId())) {
            usuarioSucursalRepository.save(new UsuarioSucursal(usuario, sucursal));
        }
    }

}
