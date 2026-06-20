package com.side.project.domain.member;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/members/add")
    public String addForm(@ModelAttribute("memberSaveForm") MemberSaveForm form) {
        // TODO: Prepare the memberSaveForm object for the signup page.
        return "members/addMemberForm";
    }

    @PostMapping("/members/add")
    public ResponseEntity<MemberSaveForm> save(@Valid @RequestBody MemberSaveForm form) {
        Long memberId = memberService.join(form);

        return new ResponseEntity<>(form , HttpStatus.valueOf(201));
    }
}
