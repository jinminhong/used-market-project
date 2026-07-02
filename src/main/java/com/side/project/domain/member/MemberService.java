package com.side.project.domain.member;

import com.side.project.domain.member.memberdto.MemberSaveDto;
import com.side.project.web.exception.member.DuplicateMemberException;
import com.side.project.web.exception.member.MemberException;
import com.side.project.web.login.LoginMember;
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
    public Long join(MemberSaveDto memberForm) {
        checkDuplicate(memberForm);
        Member member = new Member(memberForm.getLoginId(),memberForm.getName(),memberForm.getPassword(), memberForm.getNickname() ,memberForm.getAddress());
        return memberRepository.save(member).getId();
    }

    private void checkDuplicate(MemberSaveDto memberForm) {
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

    public LoginMember getMyInfo(Long memberId) {
        return findById(memberId)
               .map(member -> new LoginMember(member.getId(), member.getLoginId(), member.getNickName()))
               .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));
    }
}
