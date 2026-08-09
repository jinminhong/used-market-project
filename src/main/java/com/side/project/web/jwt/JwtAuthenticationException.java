package com.side.project.web.jwt;

import com.side.project.web.exception.login.UnauthorizedException;

public class JwtAuthenticationException extends UnauthorizedException {
    public JwtAuthenticationException(String message) {
        super(message);
    }
}
