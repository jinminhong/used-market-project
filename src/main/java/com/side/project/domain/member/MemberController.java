package com.side.project.domain.member;

import com.side.project.domain.member.memberdto.ShopInfoDto;
import com.side.project.domain.member.memberdto.MemberSaveDto;
import com.side.project.domain.member.memberdto.MemberUpdateDto;
import com.side.project.web.argumentresolver.Login;
import com.side.project.web.login.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping()
    public ResponseEntity<MemberSaveDto> save(@Valid @RequestBody MemberSaveDto form) {
        Long memberId = memberService.join(form);
        return new ResponseEntity<>(form , HttpStatus.valueOf(201));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(@Login LoginMember loginMember) {
        LoginMember myInfo = memberService.getMyInfo(loginMember.getMemberId());
        return ResponseEntity.status(HttpStatus.OK).body(myInfo);
    }

    @PatchMapping("/me")
    public ResponseEntity<?> updateMyInfo(@Login LoginMember loginMember,
                                          @RequestBody MemberUpdateDto memberUpdateDto) {
        MemberUpdateDto update = memberService.update(loginMember, memberUpdateDto);
        return ResponseEntity.ok(update);
    }

    @GetMapping("/{memberId}/shop")
    public ResponseEntity<ShopInfoDto> memberInfo(@PathVariable("memberId") Long memberId) {
        return ResponseEntity.ok(memberService.getMemberInfo(memberId));
    }
}
