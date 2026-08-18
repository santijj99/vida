package com.vida.apirest.servicies;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vida.apirest.config.AfipProperties;
import com.vida.apirest.config.AppSecurityProperties;
import com.vida.apirest.config.LicenciaProperties;
import com.vida.apirest.exception.BadRequestException;
import com.vida.apirest.exception.ForbiddenException;
import com.vida.apirest.exception.RegistrationDisabledException;
import com.vida.apirest.exception.TooManyRequestsException;
import com.vida.apirest.security.AuthRateLimiter;
import com.vida.apirest.security.PasswordResetCodes;
import com.vida.apirest.tenant.TenantContext;
import com.vida.apirest.dto.afip.TokenValidationResponse;
import com.vida.apirest.dto.usuario.AdminUpdateUsuarioRequest;
import com.vida.apirest.dto.usuario.CambiarPasswordInicialRequest;
import com.vida.apirest.dto.usuario.CreateUsuarioRequest;
import com.vida.apirest.dto.usuario.ForgotPasswordRequest;
import com.vida.apirest.dto.usuario.LoginRequest;
import com.vida.apirest.dto.usuario.LoginResponse;
import com.vida.apirest.dto.usuario.ResetPasswordRequest;
import com.vida.apirest.dto.usuario.SoporteLoginRequest;
import com.vida.apirest.dto.usuario.UpdateUsuarioRequest;
import com.vida.apirest.dto.usuario.UsuarioResponse;
import com.vida.apirest.dto.usuario.UsuarioSucursalDTO;
import com.vida.apirest.dto.usuario.mapper.UsuarioMapper;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.auth.Role;
import com.vida.apirest.model.auth.Usuario;
import com.vida.apirest.model.auth.UsuarioHasRoles;
import com.vida.apirest.repositories.RoleRepository;
import com.vida.apirest.repositories.SucursalRepository;
import com.vida.apirest.repositories.UsuarioHasRoleRepository;
import com.vida.apirest.repositories.UsuarioRepository;
import com.vida.apirest.repositories.UsuarioSucursalRepository;
import com.vida.apirest.security.AppUserDetails;
import com.vida.apirest.servicies.afip.AFIPTokenValidatorService;
import com.vida.apirest.servicies.afip.AfipContextService;
import com.vida.apirest.tenant.TenantBootstrapService;
import com.vida.apirest.utils.FileUploadUtils;
import com.vida.apirest.utils.JwtUtil;
import com.vida.apirest.dto.auth.EffectivePermissions;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class UsuarioService {

    private static final String SOPORTE_USER = "soporte";
    private static final String SOPORTE_EMAIL = "soporte@athland.local";
    private static final String MSG_RESET_GENERICO = "Código inválido o expirado";
    private static final String MSG_RATE_LIMIT = "Demasiados intentos. Probá de nuevo en unos minutos.";
    private static final int MAX_LOGIN_POR_IDENT = 8;
    private static final int MAX_FORGOT_POR_EMAIL = 3;
    private static final int MAX_RESET_POR_EMAIL = 10;

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioHasRoleRepository usuarioHasRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UsuarioMapper usuarioMapper;

    @Autowired
    private UsuarioSucursalRepository usuarioSucursalRepository;

    @Autowired
    private SucursalRepository sucursalRepository;

    @Autowired
    private PermissionResolverService permissionResolverService;

    @Autowired
    private AFIPTokenValidatorService afipTokenValidatorService;

    @Autowired
    private AfipProperties afipProperties;

    @Autowired
    private AfipContextService afipContextService;

    @Autowired
    private AppSecurityProperties appSecurityProperties;

    @Autowired
    private LicenciaProperties licenciaProperties;

    @Autowired
    private com.vida.apirest.servicies.licencia.SistemaLicenciaService sistemaLicenciaService;

    @Autowired
    private com.vida.apirest.tenant.TenantDataSourceManager tenantDataSourceManager;

    @Autowired
    private com.vida.apirest.servicies.licencia.LicenciaServerClient licenciaServerClient;

    @Autowired
    private AuthRateLimiter authRateLimiter;

    @Autowired
    private PasswordResetAttemptService passwordResetAttemptService;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Transactional
    public LoginResponse create(CreateUsuarioRequest request) {
        if (!appSecurityProperties.isAllowPublicRegister()) {
            throw new RegistrationDisabledException();
        }
        if (usuarioRepository.existsByEmail(request.email)) {
            throw new RuntimeException("El correo ya esta en uso");
        }
        if (usuarioRepository.existsByUsuario(request.usuario)) {
            throw new RuntimeException("El nombre de usuario ya está en uso");
        }
        Usuario usuario = new Usuario();
        usuario.setUsuario(request.usuario);
        usuario.setEmail(request.email);
        usuario.setCelular(celularNormalizado(request.celular));

        String encryptedPassword = passwordEncoder.encode(request.password);
        usuario.setPassword(encryptedPassword);

        Usuario savedUser = usuarioRepository.save(usuario);
        Role clientRole = roleRepository.findByNombre("CLIENTE").orElseThrow(
                () -> new RuntimeException("El rol cliente no existe")
        );

        UsuarioHasRoles usuarioHasRoles = new UsuarioHasRoles(savedUser, clientRole);
        usuarioHasRoleRepository.save(usuarioHasRoles);

        return buildLoginResponse(savedUser, resolveCodigoLicencia(null));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> findAll() {
        List<Usuario> usuarios = usuarioRepository.findAllWithRolesAndRolPrincipal();
        return usuarios.stream().map(usuario -> {
            List<Role> roles = rolesFromUsuario(usuario);
            EffectivePermissions permissions = permissionResolverService.resolve(usuario);
            return usuarioMapper.toUsuarioResponse(usuario, roles, permissions);
        }).toList();
    }

    @Transactional
    public UsuarioResponse createByAdmin(CreateUsuarioRequest request) {
        if (request.email != null && usuarioRepository.existsByEmail(request.email)) {
            throw new RuntimeException("El correo ya está en uso");
        }
        if (usuarioRepository.existsByUsuario(request.usuario)) {
            throw new RuntimeException("El nombre de usuario ya está en uso");
        }
        String celular = celularNormalizado(request.celular);
        if (celular != null && usuarioRepository.existsByCelular(celular)) {
            throw new RuntimeException("El celular ya está en uso");
        }

        Usuario usuario = new Usuario();
        usuario.setUsuario(request.usuario);
        usuario.setEmail(request.email);
        usuario.setCelular(celular);
        usuario.setActivo(true);

        String encryptedPassword = passwordEncoder.encode(request.password);
        usuario.setPassword(encryptedPassword);
        Usuario savedUser = usuarioRepository.save(usuario);

        Long rolId = request.rolId;
        if (rolId == null) {
            rolId = roleRepository.findByNombre("CLIENTE")
                    .map(Role::getId)
                    .orElse(null);
        }
        if (rolId != null) {
            asignarRolSiNoExiste(savedUser, rolId);
        }

        return buildProfileResponse(savedUser);
    }

    @Transactional
    public UsuarioResponse updateByAdmin(Long id, AdminUpdateUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El usuario no existe"));

        if (request.usuario != null && !request.usuario.isBlank()) {
            String nuevoUsuario = request.usuario.trim();
            if (!nuevoUsuario.equalsIgnoreCase(usuario.getUsuario())) {
                usuarioRepository.findByUsuario(nuevoUsuario)
                        .filter(existente -> !existente.getId().equals(id))
                        .ifPresent(existente -> {
                            throw new RuntimeException("El nombre de usuario ya está en uso");
                        });
                usuario.setUsuario(nuevoUsuario);
            }
        }

        if (request.email != null) {
            String email = request.email.isBlank() ? null : request.email.trim();
            if (email != null && (usuario.getEmail() == null || !email.equalsIgnoreCase(usuario.getEmail()))) {
                usuarioRepository.findByEmail(email)
                        .filter(existente -> !existente.getId().equals(id))
                        .ifPresent(existente -> {
                            throw new RuntimeException("El correo ya está en uso");
                        });
            }
            usuario.setEmail(email);
        }

        if (request.celular != null) {
            String celular = celularNormalizado(request.celular);
            if (celular != null
                    && (usuario.getCelular() == null || !celular.equals(usuario.getCelular()))
                    && usuarioRepository.existsByCelular(celular)) {
                throw new RuntimeException("El celular ya está en uso");
            }
            usuario.setCelular(celular);
        }

        if (request.password != null && !request.password.isBlank()) {
            if (request.password.length() < 6) {
                throw new RuntimeException("La contraseña debe tener al menos 6 caracteres");
            }
            usuario.setPassword(passwordEncoder.encode(request.password));
            usuario.invalidarTokens();
        }

        if (request.activo != null) {
            if (Boolean.FALSE.equals(request.activo) && usuario.isEnabled()) {
                usuario.invalidarTokens();
            }
            usuario.setActivo(request.activo);
        }

        usuarioRepository.save(usuario);
        return buildProfileResponse(usuario);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String codigoLicencia = resolveCodigoLicencia(request.getCodigoLicencia());
        if (tenantDataSourceManager.isMultiTenantEnabled()) {
            tenantDataSourceManager.ensureTenantReady(codigoLicencia);
        }

        String identificador = request.getIdentificador();
        if (identificador == null || identificador.isBlank()) {
            identificador = request.getEmail();
        }
        if (identificador == null || identificador.isBlank()) {
            throw new RuntimeException("Debes ingresar usuario o email");
        }
        String identKey = rateKey("login-id", identificador.trim().toLowerCase());
        Usuario usuario = usuarioRepository.findByIdentificadorWithRolesAndRolPrincipal(identificador.trim())
                .orElse(null);
        if (usuario != null && usuario.esSoporte()) {
            rechazarLoginSiExcedio(identKey);
            throw new ForbiddenException("Usá el ticket de soporte para entrar");
        }
        if (usuario == null
                || !usuario.isEnabled()
                || usuario.soporteVencido()
                || !passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            rechazarLoginSiExcedio(identKey);
            throw new RuntimeException("El usuario/email o password no son validos");
        }
        authRateLimiter.reset(identKey);
        if (!tenantDataSourceManager.isMultiTenantEnabled()
                && licenciaProperties.isEnabled()
                && licenciaProperties.isBloquearSiInvalida()
                && !sistemaLicenciaService.isLicenciaOperativa()) {
            throw new ForbiddenException(
                    "La licencia del sistema no está activa. Contactá al proveedor para renovarla.");
        }
        return buildLoginResponse(usuario, codigoLicencia);
    }

    @Transactional
    public LoginResponse loginSoporte(SoporteLoginRequest request) {
        if (request == null || request.getToken() == null || request.getToken().isBlank()) {
            throw new BadRequestException("Falta el ticket de soporte");
        }
        String codigoLicencia = resolveCodigoLicencia(request.getCodigoLicencia());
        if (tenantDataSourceManager.isMultiTenantEnabled()) {
            tenantDataSourceManager.ensureTenantReady(codigoLicencia);
        } else if (licenciaProperties.isEnabled()
                && licenciaProperties.isBloquearSiInvalida()
                && !sistemaLicenciaService.isLicenciaOperativa()) {
            throw new ForbiddenException(
                    "La licencia del sistema no está activa. Contactá al proveedor para renovarla.");
        }

        var remoto = licenciaServerClient.consumirSoporte(codigoLicencia, request.getToken().trim());
        if (!remoto.isAlcanzable()) {
            throw new ForbiddenException("No se pudo contactar el servidor de licencias");
        }
        if (!remoto.isValido()) {
            throw new ForbiddenException(
                    remoto.getMensaje() == null || remoto.getMensaje().isBlank()
                            ? "Ticket de soporte inválido"
                            : remoto.getMensaje());
        }
        if (remoto.getExpiraEn() == null || !remoto.getExpiraEn().isAfter(java.time.Instant.now())) {
            throw new ForbiddenException("El ticket de soporte venció");
        }

        String tokenHash = hashSoporteToken(request.getToken().trim());
        Usuario usuario = asegurarUsuarioSoporte(tokenHash, remoto.getExpiraEn());
        return buildLoginResponse(usuario, codigoLicencia);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RuntimeException("Debes ingresar un email");
        }
        if (mailSender == null || mailFrom == null || mailFrom.isBlank()) {
            throw new RuntimeException("El envío de correos no está configurado en el servidor");
        }
        String email = request.getEmail().trim();
        exigirRateLimit(rateKey("forgot-email", email.toLowerCase()), MAX_FORGOT_POR_EMAIL);

        usuarioRepository.findByEmail(email)
                .filter(u -> u.isEnabled() && !u.esSoporte())
                .ifPresent(usuario -> {
                    String codigo = PasswordResetCodes.generate6Digits();
                    usuario.setResetCodigo(PasswordResetCodes.hash(codigo));
                    usuario.setResetCodigoExpiraAt(LocalDateTime.now().plusMinutes(15));
                    usuario.setResetIntentos(0);
                    usuarioRepository.save(usuario);
                    try {
                        enviarCodigoReset(usuario.getEmail(), codigo, usuario.getUsuario());
                    } catch (Exception ex) {
                        log.warn("No se pudo enviar el código de recuperación a {}: {}",
                                usuario.getEmail(), ex.getMessage());
                    }
                });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()
                || request.getCodigo() == null || request.getCodigo().isBlank()
                || request.getNuevaPassword() == null || request.getNuevaPassword().isBlank()) {
            throw new RuntimeException("Email, código y nueva contraseña son obligatorios");
        }
        if (request.getNuevaPassword().length() < 6) {
            throw new RuntimeException("La nueva contraseña debe tener al menos 6 caracteres");
        }
        String email = request.getEmail().trim();
        exigirRateLimit(rateKey("reset-email", email.toLowerCase()), MAX_RESET_POR_EMAIL);

        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null
                || usuario.getResetCodigo() == null
                || usuario.getResetCodigoExpiraAt() == null
                || usuario.getResetCodigoExpiraAt().isBefore(LocalDateTime.now())
                || !PasswordResetCodes.matches(request.getCodigo(), usuario.getResetCodigo())) {
            if (usuario != null && usuario.getId() != null) {
                passwordResetAttemptService.registrarFallo(usuario.getId());
            }
            throw new RuntimeException(MSG_RESET_GENERICO);
        }

        usuario.setPassword(passwordEncoder.encode(request.getNuevaPassword()));
        usuario.setResetCodigo(null);
        usuario.setResetCodigoExpiraAt(null);
        usuario.setResetIntentos(0);
        usuario.setDebeCambiarPassword(false);
        usuario.invalidarTokens();
        usuarioRepository.save(usuario);
    }

    @Transactional
    public LoginResponse cambiarPasswordInicial(CambiarPasswordInicialRequest request) {
        if (request == null
                || request.getPasswordActual() == null || request.getPasswordActual().isBlank()
                || request.getNuevaPassword() == null || request.getNuevaPassword().isBlank()) {
            throw new RuntimeException("La contraseña actual y la nueva son obligatorias");
        }
        String nueva = request.getNuevaPassword().trim();
        if (nueva.length() < 6) {
            throw new RuntimeException("La nueva contraseña debe tener al menos 6 caracteres");
        }
        if (nueva.equals(request.getPasswordActual())) {
            throw new RuntimeException("La nueva contraseña tiene que ser distinta a la temporal");
        }
        if (TenantBootstrapService.BOOTSTRAP_ADMIN_PASSWORD.equals(nueva)) {
            throw new RuntimeException("Elegí una contraseña distinta a la temporal de instalación");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails details)) {
            throw new RuntimeException("No autenticado");
        }
        Usuario usuario = usuarioRepository.findById(details.getUsuario().getId())
                .orElseThrow(() -> new RuntimeException("El usuario no existe"));
        if (!usuario.debeCambiarPassword()) {
            throw new RuntimeException("Este usuario no tiene una contraseña temporal pendiente");
        }
        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
            throw new RuntimeException("La contraseña actual no es válida");
        }

        usuario.setPassword(passwordEncoder.encode(nueva));
        usuario.setDebeCambiarPassword(false);
        usuario.invalidarTokens();
        usuarioRepository.save(usuario);
        Usuario recargado = usuarioRepository
                .findByIdentificadorWithRolesAndRolPrincipal(usuario.getUsuario())
                .orElse(usuario);
        return buildLoginResponse(recargado, resolveCodigoLicencia(null));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buildProfileResponse(Usuario usuario) {
        List<Role> roles = rolesFromUsuario(usuario);
        EffectivePermissions permissions = permissionResolverService.resolve(usuario);
        UsuarioResponse response = usuarioMapper.toUsuarioResponse(usuario, roles, permissions);
        response.setSucursales(resolveSucursalesPerfil(usuario, roles));
        return response;
    }

    private List<UsuarioSucursalDTO> resolveSucursalesPerfil(Usuario usuario, List<Role> roles) {
        boolean isAdmin = roles.stream().anyMatch(r -> "ADMINISTRADOR".equals(r.getNombre()));
        List<Sucursal> sucursales;
        if (isAdmin) {
            sucursales = sucursalRepository.findAll().stream()
                    .filter(s -> s.getEstado() == null || s.getEstado() == Sucursal.EstadoSucursal.ACTIVA)
                    .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(
                            a.getNombre() != null ? a.getNombre() : "",
                            b.getNombre() != null ? b.getNombre() : ""))
                    .toList();
        } else {
            sucursales = usuarioSucursalRepository.findSucursalesByUsuarioId(usuario.getId()).stream()
                    .filter(s -> s.getEstado() == null || s.getEstado() == Sucursal.EstadoSucursal.ACTIVA)
                    .toList();
        }
        return sucursales.stream()
                .map(s -> new UsuarioSucursalDTO(
                        s.getId(),
                        s.getNombre(),
                        s.getCodigo(),
                        s.getEstado() != null ? s.getEstado().name() : null))
                .toList();
    }

    @Transactional
    public UsuarioResponse findById(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El usuario no existe"));
        return buildProfileResponse(usuario);
    }

    @Transactional
    public UsuarioResponse updateUsuarioConImagen(Long id, UpdateUsuarioRequest request) throws IOException {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El usuario no existe"));

        if (request.getCelular() != null) {
            usuario.setCelular(request.getCelular());
        }

        if (request.getFile() != null && !request.getFile().isEmpty()) {
            String uploadDir = "uploads/usuario/" + usuario.getId();
            String filename = FileUploadUtils.safeProfileFileName(request.getFile().getOriginalFilename());
            var targetPath = Paths.get(uploadDir, filename).normalize();
            if (!targetPath.startsWith(Paths.get(uploadDir).normalize())) {
                throw new IllegalArgumentException("Nombre de archivo inválido");
            }

            Files.createDirectories(Paths.get(uploadDir));
            Files.copy(request.getFile().getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            usuario.setImage("/" + targetPath.toString().replace("\\", "/"));
        }

        usuarioRepository.save(usuario);
        return buildProfileResponse(usuario);
    }

    @Transactional
    public UsuarioResponse asignarRol(Long usuarioId, Long rolId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("El usuario no existe"));

        asignarRolSiNoExiste(usuario, rolId);
        usuario.invalidarTokens();
        usuarioRepository.save(usuario);
        return buildProfileResponse(usuario);
    }

    private LoginResponse buildLoginResponse(Usuario usuario) {
        return buildLoginResponse(usuario, resolveCodigoLicencia(null));
    }

    private LoginResponse buildLoginResponse(Usuario usuario, String codigoLicencia) {
        EffectivePermissions permissions = permissionResolverService.resolve(usuario);
        List<Role> roles = rolesFromUsuario(usuario);

        List<String> roleNames = roles.stream().map(Role::getNombre).collect(Collectors.toList());
        String token = jwtUtil.generateToken(
                usuario,
                roleNames,
                permissions.getPermisosEfectivos(),
                codigoLicencia,
                usuario.esSoporte() ? usuario.getSoporteExpiraAt() : null
        );

        LoginResponse response = new LoginResponse();
        response.setToken("Bearer " + token);
        UsuarioResponse usuarioResponse = usuarioMapper.toUsuarioResponse(usuario, roles, permissions);
        usuarioResponse.setSucursales(resolveSucursalesPerfil(usuario, roles));
        response.setUsuario(usuarioResponse);
        response.setDebeCambiarPassword(usuario.debeCambiarPassword());
        response.setAfipToken(validarTokenAfipEnLogin());
        return response;
    }

    private String resolveCodigoLicencia(String fromRequest) {
        if (fromRequest != null && !fromRequest.isBlank()) {
            return fromRequest.trim();
        }
        String fromContext = com.vida.apirest.tenant.TenantContext.getCodigoLicencia();
        if (fromContext != null && !fromContext.isBlank()) {
            return fromContext.trim();
        }
        if (tenantDataSourceManager.isMultiTenantEnabled()) {
            throw new ForbiddenException("Debés indicar el código de licencia de la empresa");
        }
        String configured = licenciaProperties.getCodigo();
        return configured == null || configured.isBlank() ? null : configured.trim();
    }

    private TokenValidationResponse validarTokenAfipEnLogin() {
        if (!afipProperties.isEnabled() || !afipProperties.isValidarTokenEnLogin()) {
            return null;
        }
        try {
            Optional<Long> empresaId = afipContextService.resolveEmpresaIdForCurrentUser();
            if (empresaId.isEmpty()
                    || afipContextService.resolveOptionalForEmpresaId(empresaId.get()).isEmpty()) {
                return null;
            }
            TokenValidationResponse resultado = afipTokenValidatorService.validarYRegenerarToken(empresaId.get());
            if (!resultado.isActivo()) {
                log.warn("Token AFIP no disponible al login: {}", resultado.getMensaje());
            }
            return resultado;
        } catch (Exception e) {
            log.error("Error verificando token AFIP al login: {}", e.getMessage());
            return TokenValidationResponse.builder()
                    .activo(false)
                    .mensaje("No se pudo verificar el token AFIP: " + e.getMessage())
                    .regenerado(false)
                    .build();
        }
    }

    private String celularNormalizado(String celular) {
        if (celular == null || celular.isBlank()) {
            return null;
        }
        return celular.trim();
    }

    private List<Role> rolesFromUsuario(Usuario usuario) {
        if (usuario.getUsuarioHasRoles() != null && !usuario.getUsuarioHasRoles().isEmpty()) {
            return usuario.getUsuarioHasRoles().stream()
                    .map(UsuarioHasRoles::getRole)
                    .toList();
        }
        return roleRepository.findAllByUsuariosHasRoles_Usuario_Id(usuario.getId());
    }

    private void asignarRolSiNoExiste(Usuario usuario, Long rolId) {
        Role role = roleRepository.findById(rolId)
                .orElseThrow(() -> new RuntimeException("El rol no existe"));

        boolean yaAsignado = usuarioHasRoleRepository.existsByUsuarioIdAndRoleId(usuario.getId(), rolId);
        if (!yaAsignado) {
            // Un solo rol de sistema efectivo: reemplaza asignaciones previas.
            if (usuario.getUsuarioHasRoles() != null) {
                usuario.getUsuarioHasRoles().clear();
            }
            usuarioHasRoleRepository.deleteByUsuarioId(usuario.getId());
            UsuarioHasRoles usuarioHasRoles = new UsuarioHasRoles(usuario, role);
            usuarioHasRoleRepository.save(usuarioHasRoles);
            if (usuario.getUsuarioHasRoles() != null) {
                usuario.getUsuarioHasRoles().add(usuarioHasRoles);
            }
        }
        usuario.setRolPrincipal(role);
        usuarioRepository.save(usuario);
    }

    private Usuario asegurarUsuarioSoporte(String tokenHash, java.time.Instant expiraEn) {
        Role adminRole = roleRepository.findByNombre("ADMINISTRADOR")
                .orElseThrow(() -> new RuntimeException("Falta el rol ADMINISTRADOR en este tenant"));

        Optional<Usuario> existente = usuarioRepository.findByIdentificadorWithRolesAndRolPrincipal(SOPORTE_USER);
        if (existente.isPresent()) {
            Usuario usuario = existente.get();
            if (!usuario.isEnabled()
                    && tokenHash.equals(usuario.getSoporteTokenHash())) {
                throw new ForbiddenException(
                        "El cliente cortó la sesión de soporte. Pedí un ticket nuevo.");
            }
            usuario.setEsSoporte(true);
            usuario.setActivo(true);
            usuario.setDebeCambiarPassword(false);
            usuario.setSoporteExpiraAt(expiraEn);
            usuario.setSoporteTokenHash(tokenHash);
            usuario.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
            usuario.invalidarTokens();
            usuarioRepository.save(usuario);
            asignarRolSiNoExiste(usuario, adminRole.getId());
            return usuarioRepository.findByIdentificadorWithRolesAndRolPrincipal(SOPORTE_USER)
                    .orElse(usuario);
        }

        Usuario usuario = new Usuario();
        usuario.setUsuario(SOPORTE_USER);
        if (!usuarioRepository.existsByEmail(SOPORTE_EMAIL)) {
            usuario.setEmail(SOPORTE_EMAIL);
        }
        usuario.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        usuario.setActivo(true);
        usuario.setDebeCambiarPassword(false);
        usuario.setEsSoporte(true);
        usuario.setSoporteExpiraAt(expiraEn);
        usuario.setSoporteTokenHash(tokenHash);
        usuario.setRolPrincipal(adminRole);
        usuario = usuarioRepository.save(usuario);
        asignarRolSiNoExiste(usuario, adminRole.getId());
        return usuarioRepository.findByIdentificadorWithRolesAndRolPrincipal(SOPORTE_USER)
                .orElse(usuario);
    }

    private static String hashSoporteToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(
                    digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    private void rechazarLoginSiExcedio(String identKey) {
        if (!authRateLimiter.tryConsume(identKey, MAX_LOGIN_POR_IDENT)) {
            throw new TooManyRequestsException(MSG_RATE_LIMIT);
        }
    }

    private void exigirRateLimit(String key, int max) {
        if (!authRateLimiter.tryConsume(key, max)) {
            throw new TooManyRequestsException(MSG_RATE_LIMIT);
        }
    }

    private static String rateKey(String tipo, String valor) {
        String tenant = TenantContext.getCodigoLicencia();
        String t = tenant == null || tenant.isBlank() ? "-" : tenant;
        return tipo + ":" + t + ":" + valor;
    }

    private void enviarCodigoReset(String emailDestino, String codigo, String usuario) {
        if (mailSender == null || mailFrom == null || mailFrom.isBlank()) {
            throw new RuntimeException("El envío de correos no está configurado en el servidor");
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(mailFrom);
        msg.setTo(emailDestino);
        msg.setSubject("Recuperación de contraseña ATHLAND");
        msg.setText("""
                Hola %s,

                Tu código para recuperar la contraseña es: %s

                Este código vence en 15 minutos.
                Si no solicitaste este cambio, ignora este mensaje.
                """.formatted(usuario != null ? usuario : "usuario", codigo));
        mailSender.send(msg);
    }
}
