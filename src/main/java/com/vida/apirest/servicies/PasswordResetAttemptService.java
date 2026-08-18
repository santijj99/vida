package com.vida.apirest.servicies;

import com.vida.apirest.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * TX propia para que el conteo de fallos no se deshaga al lanzar la excepción de reset.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetAttemptService {

    public static final int MAX_INTENTOS = 5;

    private final UsuarioRepository usuarioRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFallo(Long usuarioId) {
        if (usuarioId == null) {
            return;
        }
        usuarioRepository.findById(usuarioId).ifPresent(usuario -> {
            int n = usuario.getResetIntentos() == null ? 0 : usuario.getResetIntentos();
            n++;
            usuario.setResetIntentos(n);
            if (n >= MAX_INTENTOS) {
                usuario.setResetCodigo(null);
                usuario.setResetCodigoExpiraAt(null);
            }
            usuarioRepository.save(usuario);
        });
    }
}
