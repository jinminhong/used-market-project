package com.side.project.web.interceptor;

import com.side.project.web.SessionConst;
import com.side.project.web.exception.login.LoginFailException;
import com.side.project.web.exception.login.UnauthorizedException;
import com.side.project.web.login.LoginMember;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.HandlerInterceptor;

public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        String requestURI = request.getRequestURI();

        if (method.equals("GET") && requestURI.contains("items")){ //get으로 보낸 items는 실행되기 위해서
            return true;
        }

        HttpSession session = request.getSession(false);

        if (session == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        LoginMember sessionMember = (LoginMember) session.getAttribute(SessionConst.LOGIN_MEMBER);

        if (sessionMember == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        return true;
    }
}
