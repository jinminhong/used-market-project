package com.side.project.web;

public class SessionConst {
    // 세션 attribute였으나 JWT 전환 후에는 request attribute 키로 재사용된다 (LoginCheckInterceptor에서 설정).
    public static final String LOGIN_MEMBER="loginMember";
    public static final String REFRESH_TOKEN_COOKIE="refreshToken";
}
