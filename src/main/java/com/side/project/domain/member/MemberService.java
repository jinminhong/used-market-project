package com.side.project.domain.member;

import com.side.project.web.exception.DuplicateMemberException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public Long join(MemberSaveForm memberForm) {
        checkDuplicate(memberForm);
        Member member = new Member(memberForm.getLoginId(),memberForm.getName(),memberForm.getPassword(), memberForm.getNickname());
        return memberRepository.save(member).getId();
    }

    private void checkDuplicate(MemberSaveForm memberForm) {
        memberRepository.findByLoginId(memberForm.getLoginId())
                .ifPresent(existing -> {
                    throw new DuplicateMemberException("이미 사용 중인 로그인 ID입니다.");
                });
        memberRepository.findByNickName(memberForm.getNickname())
                .ifPresent(existing -> {
                    throw new DuplicateMemberException("이미 사용 중인 닉네임입니다.");
                });
    }

    public Optional<Member> findById(Long memberId) {
        return memberRepository.findById(memberId);
    }

    public Optional<Member> findByLoginId(String loginId) {
        return memberRepository.findByLoginId(loginId);
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }
}
