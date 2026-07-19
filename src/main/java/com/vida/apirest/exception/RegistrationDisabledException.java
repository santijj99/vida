package com.vida.apirest.exception;

import org.springframework.http.HttpStatus;

public class RegistrationDisabledException extends ApiException {

    public RegistrationDisabledException() {
        super("El registro público está deshabilitado", HttpStatus.FORBIDDEN);
    }
}
