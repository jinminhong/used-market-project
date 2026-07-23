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
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

public class LoginCheckInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        String requestURI = request.getRequestURI();

        // get으로 보낸 상품 조회는 로그인 없이 허용 (startsWith는 /api/itemsFoo 같은 무관한 경로까지 오탐하므로 AntPathMatcher로 정확히 매칭)
        if (method.equals("GET") &&
                (PATH_MATCHER.match("/api/items", requestURI) || PATH_MATCHER.match("/api/items/**", requestURI))) {
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
