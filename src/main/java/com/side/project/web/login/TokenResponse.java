package com.side.project.web.login;

import lombok.Getter;

@Getter
public class TokenResponse {

    private final String accessToken;
    private final long accessTokenExpiresIn;
    private final Long memberId;
    private final String loginId;
    private final String nickname;

    public TokenResponse(String accessToken, long accessTokenExpiresIn, Long memberId, String loginId, String nickname) {
        this.accessToken = accessToken;
        this.accessTokenExpiresIn = accessTokenExpiresIn;
        this.memberId = memberId;
        this.loginId = loginId;
        this.nickname = nickname;
    }
}
