package com.side.project.domain.member;

import com.side.project.domain.member.memberdto.MemberSaveDto;
import com.side.project.web.SessionConst;
import com.side.project.web.exception.member.DuplicateMemberException;
import com.side.project.web.login.LoginMember;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/members")
    public String addForm(@ModelAttribute("memberSaveForm") MemberSaveDto form) {
        // TODO: Prepare the memberSaveForm object for the signup page.
        return "members/addMemberForm";
    }

    @PostMapping("/members")
    public ResponseEntity<MemberSaveDto> save(@Valid @RequestBody MemberSaveDto form) {
        Long memberId = memberService.join(form);
        return new ResponseEntity<>(form , HttpStatus.valueOf(201));
    }

    @GetMapping("/members/me")
    public ResponseEntity<?> getMyInfo(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) { //세션이 없음
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("로그인이 필요합니다.");
        }

        LoginMember sessionMember =
                (LoginMember) session.getAttribute(SessionConst.LOGIN_MEMBER);

        if (sessionMember == null) { //세션은 있는데 저장된 정보가 없음
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("로그인이 필요합니다.");
        }

        Long memberId = ((LoginMember) session.getAttribute(SessionConst.LOGIN_MEMBER)).getMemberId();
        LoginMember loginMember = memberService.getMyInfo(memberId);

        return ResponseEntity.status(HttpStatus.OK).body(loginMember);
    }

//    @PatchMapping("/members/me")
//    public ResponseEntity<MemberUpdateDto> updateMyInfo() {
//
//    }
}
