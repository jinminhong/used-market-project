package com.side.project.web.login;

import lombok.Data;

@Data
public class LoginMember {

    private Long memberId;

    private String loginId;

    private String nickname;

    public LoginMember(Long memberId, String loginId, String nickname) {
        this.memberId = memberId;
        this.loginId = loginId;
        this.nickname = nickname;
    }
}
