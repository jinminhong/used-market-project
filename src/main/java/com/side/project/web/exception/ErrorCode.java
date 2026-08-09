package com.side.project.web.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_REQUEST("invalid_request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("forbidden", HttpStatus.FORBIDDEN),
    NOT_FOUND_ITEM("not_found_item", HttpStatus.NOT_FOUND),
    NOT_FOUND_MEMBER("not_found_member", HttpStatus.NOT_FOUND),
    NOT_FOUND_ORDER("not_found_order", HttpStatus.NOT_FOUND),
    NOT_FOUND_CHATROOM("not_found_chatroom", HttpStatus.NOT_FOUND),
    NOT_FOUND_WISHLIST("not_found_wishlist", HttpStatus.NOT_FOUND),
    DUPLICATE_MEMBER("duplicate_member", HttpStatus.CONFLICT),
    DUPLICATE_NICKNAME("duplicate_nickname", HttpStatus.CONFLICT),
    CONFLICT_STATE("conflict_state", HttpStatus.CONFLICT);

    private final String code;
    private final HttpStatus status;

    ErrorCode(String code, HttpStatus status) {
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
