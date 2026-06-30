package com.vida.apirest.utils;

/**
 * AFIP WSAA respondió que el certificado ya tiene un TA vigente (coe.alreadyAuthenticated).
 */
public class AfipTaAlreadyAuthenticatedException extends Exception {

    public AfipTaAlreadyAuthenticatedException(String message) {
        super(message);
    }
}
