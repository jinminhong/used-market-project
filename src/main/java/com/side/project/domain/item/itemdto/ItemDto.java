package com.side.project.domain.item.itemdto;

import com.side.project.domain.item.Category;
import com.side.project.domain.item.Item;
import com.side.project.domain.item.ItemStatus;
import com.side.project.domain.member.Member;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ItemDto {

    private String name ;
    private String description;
    private Integer price;
    private ItemStatus status;
    private Category category;
    private String nickName;

    public ItemDto(String name, String description, Integer price, ItemStatus status, Category category, Member member) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.status = status;
        this.nickName = member.getNickName();
    }

    public ItemDto(Item item) {
        this.name = item.getName();
        this.description = item.getDescription();
        this.price = item.getPrice();
        this.status = item.getStatus();
        this.nickName = item.getMember().getNickName();
    }
}
