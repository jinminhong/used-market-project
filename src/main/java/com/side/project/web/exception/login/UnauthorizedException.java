package com.side.project.web.exception.login;

import com.side.project.web.exception.ApplicationException;
import com.side.project.web.exception.ErrorCode;

public class UnauthorizedException extends ApplicationException {
    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
