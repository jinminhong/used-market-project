package com.side.project.web.exception.orders;

import org.springframework.http.HttpStatus;

public class OrdersException extends RuntimeException {
    private final HttpStatus httpStatus;

    public OrdersException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
