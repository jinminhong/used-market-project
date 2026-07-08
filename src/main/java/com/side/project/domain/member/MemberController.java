package com.side.project.domain.member;

import com.side.project.domain.member.memberdto.MemberInfoDto;
import com.side.project.domain.member.memberdto.MemberSaveDto;
import com.side.project.domain.member.memberdto.MemberUpdateDto;
import com.side.project.web.SessionConst;
import com.side.project.web.login.LoginMember;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;



    @PostMapping("/members")
    public ResponseEntity<MemberSaveDto> save(@Valid @RequestBody MemberSaveDto form) {
        Long memberId = memberService.join(form);
        return new ResponseEntity<>(form , HttpStatus.valueOf(201));
    }

    @GetMapping("/members/me")
    public ResponseEntity<?> getMyInfo(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        LoginMember sessionMember =
                (LoginMember) session.getAttribute(SessionConst.LOGIN_MEMBER);

        if (session == null ||sessionMember == null) { //세션이 없음
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("로그인이 필요합니다.");
        }

        Long memberId = ((LoginMember) session.getAttribute(SessionConst.LOGIN_MEMBER)).getMemberId();
        LoginMember loginMember = memberService.getMyInfo(memberId);

        return ResponseEntity.status(HttpStatus.OK).body(loginMember);
    }

    @PatchMapping("/members/{memberId}")
    public ResponseEntity<?> updateMyInfo(@PathVariable("memberId") Long memberId,
                                          HttpServletRequest request,
                                          @RequestBody MemberUpdateDto memberUpdateDto) {
        HttpSession session = request.getSession(false);
        LoginMember sessionMember =
                (LoginMember) session.getAttribute(SessionConst.LOGIN_MEMBER);

        if (session == null ||sessionMember == null) { //세션이 없음
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("로그인이 필요합니다.");
        }

        MemberUpdateDto update = memberService.update(memberId, memberUpdateDto, sessionMember);

        return ResponseEntity.ok(update);

    }

    @GetMapping("members/{memberId}")
    public ResponseEntity<MemberInfoDto> memberInfo(@PathVariable("memberId") Long memberId) {
        return ResponseEntity.ok(memberService.getMemberInfo(memberId));
    }
}
