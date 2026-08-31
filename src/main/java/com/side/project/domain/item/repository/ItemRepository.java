package com.side.project.domain.item.repository;

import com.side.project.domain.item.Item;
import com.side.project.domain.item.ItemStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> , ItemRepositoryCustom{
    @Query("select i from Item i join fetch i.seller where i.id=:id")
    Optional<Item> findByIdWithMember(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select i from Item i join fetch i.seller where i.id = :id")
    Optional<Item> findByIdWithMemberForUpdate(@Param("id") Long id);

    @Modifying
    @Query("update Item i set i.embedding = :embedding, i.embeddingSourceHash = :embeddingSourceHash where i.id = :id")
    void updateEmbedding(@Param("id") Long id, @Param("embedding") float[] embedding, @Param("embeddingSourceHash") String embeddingSourceHash);

    @Query("select i.id from Item i where i.embedding is null")
    List<Long> findIdsWithoutEmbedding();

}
