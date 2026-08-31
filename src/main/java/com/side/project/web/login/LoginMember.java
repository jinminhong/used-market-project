package com.side.project.web.login;

import com.side.project.domain.member.Role;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
public class LoginMember implements UserDetails {

    private Long memberId;

    private String loginId;

    private String nickname;

    private Role role;

    public LoginMember(Long memberId, String loginId, String nickname, Role role) {
        this.memberId = memberId;
        this.loginId = loginId;
        this.nickname = nickname;
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return loginId;
    }
}
