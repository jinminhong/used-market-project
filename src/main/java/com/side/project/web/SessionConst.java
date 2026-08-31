package com.side.project.web;

public class SessionConst {
    // HTTP 인증은 Spring Security SecurityContext를 사용하므로, 이 키는 STOMP 세션 attribute 용도로만 쓰인다 (StompAuthChannelInterceptor에서 설정).
    public static final String LOGIN_MEMBER="loginMember";
    public static final String REFRESH_TOKEN_COOKIE="refreshToken";
}
