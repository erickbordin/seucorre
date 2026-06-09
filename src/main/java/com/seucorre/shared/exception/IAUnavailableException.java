package com.seucorre.shared.exception;

public class IAUnavailableException extends RuntimeException {

    public IAUnavailableException(String message) {
        super(message);
    }

    public IAUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}   