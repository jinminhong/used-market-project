package com.side.project.domain.wishlist;

import com.side.project.domain.BaseEntity;
import com.side.project.domain.item.Item;
import com.side.project.domain.member.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.*;

@Entity
@NoArgsConstructor
@Getter
@Table(
        name = "wishlist",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_wishlist_member_item",
                        columnNames = {"members_id", "item_id"}
                )
        }
)
public class WishList extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wishlist_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "members_id" , nullable = false)
    private Member member;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "item_id" , nullable = false)
    private Item item;

    public WishList(Item item, Member member) {
        this.item = item;
        this.member = member;
    }
}
