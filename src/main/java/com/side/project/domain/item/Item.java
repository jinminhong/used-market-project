package com.side.project.domain.item;

import com.side.project.domain.item.itemdto.ItemDto;
import com.side.project.domain.itemimage.ItemImage;
import com.side.project.domain.member.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "items_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "members_id")
    private Member member;

    @OneToMany(
            mappedBy = "item",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ItemImage> itemImages = new ArrayList<>();

    public void addItemImage(ItemImage itemImage) {
        itemImages.add(itemImage);
        itemImage.changeItem(this);
    }

    public void removeItemImage(ItemImage itemImage) {
        itemImages.remove(itemImage);
        itemImage.changeItem(null);
    }

    public void assignSeller(Member member) {
        this.member = member;
        member.getItemList().add(this);
    }


    public Item(String name, String description, Integer price, ItemStatus status, Category category , Member member) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.status = status;
        this.category = category;
        assignSeller(member);
    }

    public void updateItem(ItemDto itemDto){
        if (itemDto.getName() != null) {
            this.name = itemDto.getName();
        }

        if (itemDto.getDescription() != null) {
            this.description = itemDto.getDescription();
        }

        if (itemDto.getPrice() != null) {
            this.price = itemDto.getPrice();
        }

        if (itemDto.getStatus() != null) {
            this.status = itemDto.getStatus();
        }
    }
}
