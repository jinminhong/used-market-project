package com.side.project.web.exception.chat.room;

import org.springframework.http.HttpStatus;

public class ChatRoomException extends RuntimeException {
    private final HttpStatus httpStatus;

    public ChatRoomException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
