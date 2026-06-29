package com.side.project.web.login;

import com.side.project.domain.member.Member;
import com.side.project.domain.member.MemberRepository;
import com.side.project.web.exception.LoginFailException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final MemberRepository memberRepository;

    public Member authenticate(LoginForm loginForm) {
        return memberRepository.findByLoginId(loginForm.getLoginId())
                .filter(member -> member.getPassword().equals(loginForm.getPassword()))
                .orElseThrow(() -> new LoginFailException("회원정보가 맞지 않습니다."));
    }
}
