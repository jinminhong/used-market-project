package com.side.project.web.exception.login;

import com.side.project.web.exception.ApplicationException;
import com.side.project.web.exception.ErrorCode;

public class LoginFailException extends ApplicationException {
    public LoginFailException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
