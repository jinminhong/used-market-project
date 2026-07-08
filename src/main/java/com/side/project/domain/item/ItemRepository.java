package com.side.project.domain.item;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {
    // MyBatis에서는 mapper XML에 SQL을 직접 작성했습니다.
    // JPA에서는 기본 CRUD(save, findById, findAll, deleteById)를 JpaRepository가 제공합니다.
    // 다형성이 필요한 상품 구조가 생기면 Item을 상속 루트로 두고 @Inheritance 전략을 선택하세요.

    @Query("select i from Item i join fetch i.member left join fetch i.thumbnailImage")
    Slice<Item> findAllSlice(Pageable pageable);

    @Query("select i from Item i join fetch i.member where i.id=:id")
    Optional<Item> findByIdWithMember(@Param("id") Long id);
}
