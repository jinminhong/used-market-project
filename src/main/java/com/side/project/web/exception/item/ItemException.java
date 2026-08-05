package com.side.project.web.exception.item;

import org.springframework.http.HttpStatus;

public class ItemException extends RuntimeException {
    private final HttpStatus httpStatus;

    public ItemException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
