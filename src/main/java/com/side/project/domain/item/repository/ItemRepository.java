package com.side.project.domain.item.repository;

import com.side.project.domain.item.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> , ItemRepositoryCustom{
    @Query("select i from Item i join fetch i.member where i.id=:id")
    Optional<Item> findByIdWithMember(@Param("id") Long id);
}
