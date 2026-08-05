package com.side.project.web.login;

import com.side.project.domain.member.Address;
import com.side.project.domain.member.Member;
import com.side.project.domain.member.MemberRepository;
import com.side.project.web.exception.login.LoginFailException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class LoginServiceTest {

    @Autowired
    private LoginService loginService;

    @Autowired
    private MemberRepository memberRepository;

    private Member createAndSaveMember(String password) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Member member = new Member("loginId" + suffix, "홍길동", password, "nickname" + suffix,
                new Address("12345", "서울시", "서울시 지번", "101호"));
        return memberRepository.save(member);
    }

    @Test
    void authenticate_성공() {
        Member member = createAndSaveMember("password123");
        LoginForm loginForm = new LoginForm();
        loginForm.setLoginId(member.getLoginId());
        loginForm.setPassword("password123");

        Member result = loginService.authenticate(loginForm);

        assertThat(result.getId()).isEqualTo(member.getId());
    }

    @Test
    void authenticate_존재하지않는_로그인아이디() {
        LoginForm loginForm = new LoginForm();
        loginForm.setLoginId("no-such-login-id-" + UUID.randomUUID());
        loginForm.setPassword("password123");

        assertThatThrownBy(() -> loginService.authenticate(loginForm))
                .isInstanceOf(LoginFailException.class);
    }

    @Test
    void authenticate_비밀번호_불일치() {
        Member member = createAndSaveMember("password123");
        LoginForm loginForm = new LoginForm();
        loginForm.setLoginId(member.getLoginId());
        loginForm.setPassword("wrongPassword");

        assertThatThrownBy(() -> loginService.authenticate(loginForm))
                .isInstanceOf(LoginFailException.class);
    }
}
