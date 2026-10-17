package com.murilodcosta.flashpad_api.exception;

public class InvalidSlugException extends RuntimeException {

    public InvalidSlugException(String message) {
        super(message);
    }
}
