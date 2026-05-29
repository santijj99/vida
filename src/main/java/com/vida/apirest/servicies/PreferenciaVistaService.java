package com.vida.apirest.servicies;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vida.apirest.dto.config.ColumnaVistaItem;
import com.vida.apirest.dto.config.ColumnasVistaResponse;
import com.vida.apirest.model.auth.Usuario;
import com.vida.apirest.model.config.PreferenciaUsuario;
import com.vida.apirest.repositories.PreferenciaUsuarioRepository;
import com.vida.apirest.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PreferenciaVistaService {

    public static final String CLAVE_COLUMNAS_ARTICULOS = "vista_articulos_columnas";

    private static final Map<String, String> COLUMNAS_ARTICULOS = new LinkedHashMap<>();

    static {
        COLUMNAS_ARTICULOS.put("codigo", "CÓDIGO");
        COLUMNAS_ARTICULOS.put("marca", "MARCA");
        COLUMNAS_ARTICULOS.put("modelo", "MODELO");
        COLUMNAS_ARTICULOS.put("categoria", "CATEGORÍA");
        COLUMNAS_ARTICULOS.put("subCategoria", "SUBCATEGORÍA");
        COLUMNAS_ARTICULOS.put("genero", "GÉNERO");
        COLUMNAS_ARTICULOS.put("talle", "TALLE");
        COLUMNAS_ARTICULOS.put("color", "COLOR");
        COLUMNAS_ARTICULOS.put("codigoBarras", "CÓD. BARRAS");
        COLUMNAS_ARTICULOS.put("precio", "PRECIO");
        COLUMNAS_ARTICULOS.put("cantidad", "STOCK");
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private final PreferenciaUsuarioRepository preferenciaUsuarioRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public ColumnasVistaResponse obtenerColumnasArticulos() {
        List<String> activas = cargarColumnasActivas();
        List<ColumnaVistaItem> disponibles = COLUMNAS_ARTICULOS.entrySet().stream()
                .map(e -> new ColumnaVistaItem(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        return new ColumnasVistaResponse(disponibles, activas);
    }

    @Transactional
    public ColumnasVistaResponse guardarColumnasArticulos(List<String> columnas) {
        List<String> validadas = validarColumnas(columnas);
        Long usuarioId = obtenerUsuarioIdActual();
        String json = escribirJson(validadas);

        PreferenciaUsuario preferencia = buscarPreferencia(usuarioId, CLAVE_COLUMNAS_ARTICULOS)
                .orElseGet(() -> {
                    PreferenciaUsuario nueva = new PreferenciaUsuario();
                    nueva.setUsuarioId(usuarioId);
                    nueva.setClave(CLAVE_COLUMNAS_ARTICULOS);
                    return nueva;
                });

        preferencia.setValor(json);
        preferenciaUsuarioRepository.save(preferencia);
        return obtenerColumnasArticulos();
    }

    private List<String> cargarColumnasActivas() {
        Long usuarioId = obtenerUsuarioIdActual();
        String json = buscarPreferencia(usuarioId, CLAVE_COLUMNAS_ARTICULOS)
                .map(PreferenciaUsuario::getValor)
                .orElseGet(() -> usuarioId != null
                        ? buscarPreferencia(null, CLAVE_COLUMNAS_ARTICULOS)
                                .map(PreferenciaUsuario::getValor)
                                .orElse(null)
                        : null);

        if (json == null || json.isBlank()) {
            return new ArrayList<>(COLUMNAS_ARTICULOS.keySet());
        }
        return validarColumnas(leerJson(json));
    }

    private List<String> validarColumnas(List<String> columnas) {
        if (columnas == null || columnas.isEmpty()) {
            return new ArrayList<>(COLUMNAS_ARTICULOS.keySet());
        }
        List<String> validadas = columnas.stream()
                .filter(COLUMNAS_ARTICULOS::containsKey)
                .distinct()
                .collect(Collectors.toList());
        if (validadas.isEmpty()) {
            return new ArrayList<>(COLUMNAS_ARTICULOS.keySet());
        }
        return validadas;
    }

    private Optional<PreferenciaUsuario> buscarPreferencia(Long usuarioId, String clave) {
        if (usuarioId == null) {
            return preferenciaUsuarioRepository.findByUsuarioIdIsNullAndClave(clave);
        }
        return preferenciaUsuarioRepository.findByUsuarioIdAndClave(usuarioId, clave);
    }

    private Long obtenerUsuarioIdActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return usuarioRepository.findByEmail(auth.getName())
                .map(Usuario::getId)
                .orElse(null);
    }

    private List<String> leerJson(String json) {
        try {
            return JSON.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>(COLUMNAS_ARTICULOS.keySet());
        }
    }

    private String escribirJson(List<String> columnas) {
        try {
            return JSON.writeValueAsString(columnas);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo guardar la configuración de columnas");
        }
    }
}
