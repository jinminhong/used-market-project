package com.side.project.domain.item;

import com.side.project.domain.member.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "member_id")
    private Member seller;

    private void assignSeller(Member seller) {
        this.seller = seller;
        seller.getItemList().add(this);
    }

    public Item(String title, String description, Integer price, ItemStatus status, Member seller) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.status = status;
        assignSeller(seller);
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public void updateItem(ItemUpdateParam itemUpdateParam){
        this.title = itemUpdateParam.getTitle();
        this.description = itemUpdateParam.getDescription();
        this.price = itemUpdateParam.getPrice();
        this.status = itemUpdateParam.getStatus();
    }
}
