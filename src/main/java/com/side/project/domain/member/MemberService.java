package com.side.project.domain.member;

import com.side.project.domain.item.itemdto.ItemResponseDto;
import com.side.project.domain.member.memberdto.MemberInfoDto;
import com.side.project.domain.member.memberdto.ShopInfoDto;
import com.side.project.domain.member.memberdto.MemberSaveDto;
import com.side.project.domain.member.memberdto.MemberUpdateDto;
import com.side.project.web.exception.login.UnauthorizedException;
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
        if (checkLoginIdDuplicate(memberForm.getLoginId())) {
            throw new DuplicateMemberException("이미 사용 중인 로그인 ID입니다.");
        }
        if (checkNicknameDuplicate(memberForm.getNickname())) {
            throw new DuplicateMemberException("이미 사용 중인 닉네임입니다.");
        }
    }

    public boolean checkLoginIdDuplicate(String loginId) {
        return memberRepository.existsByLoginId(loginId);
    }

    public boolean checkNicknameDuplicate(String nickname) {
        return memberRepository.existsByNickName(nickname);
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

    public MemberInfoDto getMyInfo(Long memberId) {
        return findById(memberId)
               .map(member -> new MemberInfoDto(member.getId(), member.getLoginId(), member.getNickName(), member.getName(), member.getAddress()))
               .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));
    }

    public ShopInfoDto getMemberInfo(Long memberId) {
        Member member = findById(memberId)
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));

        ShopInfoDto shopInfoDto = new ShopInfoDto();
        shopInfoDto.setNickName(member.getNickName());
        shopInfoDto.setName(member.getName());
        shopInfoDto.setItemList(member.getItemList().stream().map(ItemResponseDto::new).toList());
        return shopInfoDto;
    }

    @Transactional
    public MemberUpdateDto update(LoginMember loginMember ,MemberUpdateDto memberUpdateDto) {
        Member member = findById(loginMember.getMemberId()).orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다."));
        if (!loginMember.getLoginId().equals(member.getLoginId())) {
            throw new UnauthorizedException("회원정보가 맞지 않습니다.");
        }
        memberRepository.findByNickName(memberUpdateDto.getNickname())
                .filter(existing -> !existing.getId().equals(member.getId()))
                .ifPresent(existing -> {
                    throw new DuplicateMemberException("이미 사용 중인 닉네임입니다.");
                });

        member.updateMember(memberUpdateDto);
        return memberUpdateDto;
    }
}
