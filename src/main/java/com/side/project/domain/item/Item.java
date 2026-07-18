package com.side.project.domain.item;

import com.side.project.domain.item.itemdto.ItemDto;
import com.side.project.domain.item.itemdto.ItemUpdateDto;
import com.side.project.domain.itemimage.ItemImage;
import com.side.project.domain.member.Member;
import com.side.project.domain.orders.Orders;
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
    @Column(name = "item_id")
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

    @OneToMany(mappedBy = "item")
    private List<Orders> orders;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thumbnail_image_id")
    private ItemImage thumbnailImage;

    @OneToMany(
            mappedBy = "item",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ItemImage> itemImages = new ArrayList<>();

    public void addItemImage(ItemImage itemImage) {
        itemImages.add(itemImage);
        itemImage.changeItem(this);

        if (thumbnailImage == null) {
            thumbnailImage = itemImage;
        }
    }

    public void removeItemImage(ItemImage itemImage) {
        itemImages.remove(itemImage);
        itemImage.changeItem(null);
    }

    public void assignSeller(Member member) {
        this.member = member;
        member.getItemList().add(this);
    }


    public Item(String name, String description, Integer price, ItemStatus status, Category category , Member member ) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.status = status;
        this.category = category;
        assignSeller(member);
    }

    public void updateItem(ItemUpdateDto itemUpdateDto){
        if (itemUpdateDto.getName() != null) {
            this.name = itemUpdateDto.getName();
        }

        if (itemUpdateDto.getDescription() != null) {
            this.description = itemUpdateDto.getDescription();
        }

        if (itemUpdateDto.getPrice() != null) {
            this.price = itemUpdateDto.getPrice();
        }

        if (itemUpdateDto.getStatus() != null) {
            this.status = itemUpdateDto.getStatus();
        }

        if (itemUpdateDto.getCategory() != null) {
            this.category = itemUpdateDto.getCategory();
        }

    }

    public void changeStatus(ItemStatus itemStatus){
        this.status = itemStatus;
    }
}
