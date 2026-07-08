package com.side.project.web.login;

import com.side.project.domain.member.Member;
import com.side.project.web.SessionConst;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginForm loginForm ,
                                @RequestParam(defaultValue = "/") String redirectUrl,
                                HttpServletRequest request) {
        Member member = loginService.authenticate(loginForm);
        LoginMember loginMember = new LoginMember(member.getId(),member.getLoginId(),member.getNickName());
        HttpSession session = request.getSession();
        session.setAttribute(SessionConst.LOGIN_MEMBER,loginMember);
        return ResponseEntity.status(HttpStatus.OK).body(loginMember);
    }
}
