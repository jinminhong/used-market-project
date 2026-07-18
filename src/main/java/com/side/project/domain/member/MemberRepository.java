package com.side.project.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByLoginId(String loginId);

    Optional<Member> findByNickName(String nickname);

    boolean existsByLoginId(String loginId);

    boolean existsByNickName(String nickName);
}
