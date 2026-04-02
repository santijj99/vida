package com.vida.apirest.controller;

import com.vida.apirest.dto.usuario.CreateUsuarioRequest;
import com.vida.apirest.dto.usuario.UsuarioResponse;
import com.vida.apirest.dto.usuario.UpdateUsuarioRequest;
import com.vida.apirest.servicies.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/usuario")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

//    @PostMapping
//    public ResponseEntity<UsuarioResponse> create(@RequestBody CreateUsuarioRequest request){
//        UsuarioResponse usuario = usuarioService.create(request);
//        return ResponseEntity.ok(usuario);
//    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        try {
           UsuarioResponse response = usuarioService.findById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "message", e.getMessage(),
                    "statusCode", HttpStatus.BAD_REQUEST.value()
            ));
        }
    }

    @PutMapping(value = "/upload/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @ModelAttribute UpdateUsuarioRequest request) {
        try {
            UsuarioResponse response = usuarioService.updateUsuarioConImagen(id,request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "message", e.getMessage(),
                    "statusCode", HttpStatus.BAD_REQUEST.value()
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", e.getMessage(),
                    "statusCode", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
