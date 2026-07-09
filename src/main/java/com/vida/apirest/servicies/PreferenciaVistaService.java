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
    public static final String CLAVE_COLUMNAS_PEDIDOS = "vista_pedidos_columnas";

    private static final Map<String, String> COLUMNAS_ARTICULOS = new LinkedHashMap<>();
    private static final Map<String, String> COLUMNAS_PEDIDOS = new LinkedHashMap<>();

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
        COLUMNAS_PEDIDOS.put("barras", "BARRAS");
        COLUMNAS_PEDIDOS.put("codigo", "CÓDIGO");
        COLUMNAS_PEDIDOS.put("marca", "MARCA");
        COLUMNAS_PEDIDOS.put("modelo", "MODELO");
        COLUMNAS_PEDIDOS.put("categoria", "CATEGORÍA");
        COLUMNAS_PEDIDOS.put("sub_categoria", "SUBCATEGORÍA");
        COLUMNAS_PEDIDOS.put("genero", "GÉNERO");
        COLUMNAS_PEDIDOS.put("pais", "PAÍS");
        COLUMNAS_PEDIDOS.put("talle", "TALLE");
        COLUMNAS_PEDIDOS.put("color", "COLOR");
        COLUMNAS_PEDIDOS.put("costo", "COSTO");
        COLUMNAS_PEDIDOS.put("margen", "MARG%");
        COLUMNAS_PEDIDOS.put("p_venta", "P. VENTA");
        COLUMNAS_PEDIDOS.put("cant", "CANT.");
        COLUMNAS_PEDIDOS.put("desc", "DESC%");
        COLUMNAS_PEDIDOS.put("subtotal", "SUBTOTAL");
        COLUMNAS_PEDIDOS.put("estado", "ESTADO");
        COLUMNAS_PEDIDOS.put("obs", "OBS.");
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
        List<String> validadas = validarColumnas(columnas, COLUMNAS_ARTICULOS);
        guardarPreferenciaColumnas(CLAVE_COLUMNAS_ARTICULOS, validadas);
        return obtenerColumnasArticulos();
    }

    @Transactional(readOnly = true)
    public ColumnasVistaResponse obtenerColumnasPedidos() {
        List<String> activas = cargarColumnasActivas(CLAVE_COLUMNAS_PEDIDOS, COLUMNAS_PEDIDOS);
        List<ColumnaVistaItem> disponibles = COLUMNAS_PEDIDOS.entrySet().stream()
                .map(e -> new ColumnaVistaItem(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        return new ColumnasVistaResponse(disponibles, activas);
    }

    @Transactional
    public ColumnasVistaResponse guardarColumnasPedidos(List<String> columnas) {
        List<String> validadas = validarColumnas(columnas, COLUMNAS_PEDIDOS);
        guardarPreferenciaColumnas(CLAVE_COLUMNAS_PEDIDOS, validadas);
        return obtenerColumnasPedidos();
    }

    private void guardarPreferenciaColumnas(String clave, List<String> validadas) {
        Long usuarioId = obtenerUsuarioIdActual();
        String json = escribirJson(validadas);

        PreferenciaUsuario preferencia = buscarPreferencia(usuarioId, clave)
                .orElseGet(() -> {
                    PreferenciaUsuario nueva = new PreferenciaUsuario();
                    nueva.setUsuarioId(usuarioId);
                    nueva.setClave(clave);
                    return nueva;
                });

        preferencia.setValor(json);
        preferenciaUsuarioRepository.save(preferencia);
    }

    private List<String> cargarColumnasActivas() {
        return cargarColumnasActivas(CLAVE_COLUMNAS_ARTICULOS, COLUMNAS_ARTICULOS);
    }

    private List<String> cargarColumnasActivas(String clave, Map<String, String> catalogo) {
        Long usuarioId = obtenerUsuarioIdActual();
        String json = buscarPreferencia(usuarioId, clave)
                .map(PreferenciaUsuario::getValor)
                .orElseGet(() -> usuarioId != null
                        ? buscarPreferencia(null, clave)
                                .map(PreferenciaUsuario::getValor)
                                .orElse(null)
                        : null);

        if (json == null || json.isBlank()) {
            return new ArrayList<>(catalogo.keySet());
        }
        return validarColumnas(leerJson(json, catalogo), catalogo);
    }

    private List<String> validarColumnas(List<String> columnas, Map<String, String> catalogo) {
        if (columnas == null || columnas.isEmpty()) {
            return new ArrayList<>(catalogo.keySet());
        }
        List<String> validadas = columnas.stream()
                .filter(catalogo::containsKey)
                .distinct()
                .collect(Collectors.toList());
        if (validadas.isEmpty()) {
            return new ArrayList<>(catalogo.keySet());
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

    private List<String> leerJson(String json, Map<String, String> catalogo) {
        try {
            return JSON.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>(catalogo.keySet());
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
