package com.side.project.domain.member;

import com.side.project.domain.member.memberdto.MemberInfoDto;
import com.side.project.domain.member.memberdto.MemberSaveDto;
import com.side.project.domain.member.memberdto.MemberUpdateDto;
import com.side.project.domain.member.Role;
import com.side.project.web.exception.login.UnauthorizedException;
import com.side.project.web.exception.member.DuplicateMemberException;
import com.side.project.web.exception.member.MemberException;
import com.side.project.config.TestcontainersConfig;
import com.side.project.web.login.LoginMember;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfig.class)
@SpringBootTest
@Transactional
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    private Member createAndSaveMember(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Member member = new Member(prefix + suffix, "홍길동", "password123", prefix + "Nick" + suffix,
                new Address("12345", "서울시", "서울시 지번", "101호"));
        return memberRepository.save(member);
    }

    private MemberSaveDto createSaveDto() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        MemberSaveDto dto = new MemberSaveDto();
        dto.setLoginId("newLoginId" + suffix);
        dto.setName("홍길동");
        dto.setPassword("password123");
        dto.setNickname("newNickname" + suffix);
        dto.setAddress(new Address("12345", "서울시", "서울시 지번", "101호"));
        return dto;
    }

    @Test
    void join_성공() {
        MemberSaveDto dto = createSaveDto();

        Long savedId = memberService.join(dto);

        Member saved = memberRepository.findById(savedId).orElseThrow();
        assertThat(saved.getLoginId()).isEqualTo(dto.getLoginId());
        assertThat(saved.getNickName()).isEqualTo(dto.getNickname());
    }

    @Test
    void join_로그인아이디_중복() {
        Member existing = createAndSaveMember("dup");
        MemberSaveDto dto = createSaveDto();
        dto.setLoginId(existing.getLoginId());

        assertThatThrownBy(() -> memberService.join(dto))
                .isInstanceOf(DuplicateMemberException.class);
    }

    @Test
    void join_닉네임_중복() {
        Member existing = createAndSaveMember("dup");
        MemberSaveDto dto = createSaveDto();
        dto.setNickname(existing.getNickName());

        assertThatThrownBy(() -> memberService.join(dto))
                .isInstanceOf(DuplicateMemberException.class);
    }

    @Test
    void getMyInfo_성공() {
        Member member = createAndSaveMember("info");

        MemberInfoDto result = memberService.getMyInfo(member.getId());

        assertThat(result.getMemberId()).isEqualTo(member.getId());
        assertThat(result.getNickName()).isEqualTo(member.getNickName());
    }

    @Test
    void getMyInfo_존재하지않는_회원() {
        assertThatThrownBy(() -> memberService.getMyInfo(-1L))
                .isInstanceOf(MemberException.class);
    }

    @Test
    void getMemberInfo_존재하지않는_회원() {
        assertThatThrownBy(() -> memberService.getMemberInfo(-1L))
                .isInstanceOf(MemberException.class);
    }

    @Test
    void update_성공() {
        Member member = createAndSaveMember("upd");
        LoginMember loginMember = new LoginMember(member.getId(), member.getLoginId(), member.getNickName(), member.getRole());
        MemberUpdateDto updateDto = new MemberUpdateDto();
        updateDto.setNickname("updatedNick" + UUID.randomUUID().toString().substring(0, 8));
        updateDto.setName("새이름");

        memberService.update(loginMember, updateDto);

        Member reloaded = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(reloaded.getNickName()).isEqualTo(updateDto.getNickname());
        assertThat(reloaded.getName()).isEqualTo("새이름");
    }

    @Test
    void update_존재하지않는_회원() {
        LoginMember loginMember = new LoginMember(-1L, "loginId", "nickname", Role.USER);
        MemberUpdateDto updateDto = new MemberUpdateDto();

        assertThatThrownBy(() -> memberService.update(loginMember, updateDto))
                .isInstanceOf(MemberException.class);
    }

    @Test
    void update_로그인아이디_불일치() {
        Member member = createAndSaveMember("mismatch");
        LoginMember loginMember = new LoginMember(member.getId(), "otherLoginId", member.getNickName(), member.getRole());
        MemberUpdateDto updateDto = new MemberUpdateDto();

        assertThatThrownBy(() -> memberService.update(loginMember, updateDto))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void update_닉네임_중복_타인() {
        Member member = createAndSaveMember("me");
        Member other = createAndSaveMember("other");
        LoginMember loginMember = new LoginMember(member.getId(), member.getLoginId(), member.getNickName(), member.getRole());
        MemberUpdateDto updateDto = new MemberUpdateDto();
        updateDto.setNickname(other.getNickName());

        assertThatThrownBy(() -> memberService.update(loginMember, updateDto))
                .isInstanceOf(DuplicateMemberException.class);
    }
}
