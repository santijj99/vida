package com.vida.apirest.dto.usuario;

/**
 * Actualización admin de cuenta (login / email / celular / password).
 * {@code password} es opcional: si viene vacío no se cambia.
 */
public class AdminUpdateUsuarioRequest {

    public String usuario;
    public String email;
    public String celular;
    public String password;
    public Boolean activo;
}
