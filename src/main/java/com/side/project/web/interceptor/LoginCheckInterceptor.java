package com.side.project.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // TODO:
        // 1. Get the current request URI.
        // 2. Check whether a login session exists.
        // 3. If the user is not logged in, redirect to /login.
        // 4. If the user is logged in, return true.
        return true;
    }
}
