package com.side.project.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    // 메서드 이름만으로 SQL이 생성됩니다.
    // 예: findByLoginId -> select m from Member m where m.loginId = ?
    Optional<Member> findByLoginId(String loginId);

    Member save(Member member);
}
