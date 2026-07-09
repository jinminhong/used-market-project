package com.side.project.domain.item.itemdto;

import com.side.project.domain.item.Category;
import com.side.project.domain.item.Item;
import com.side.project.domain.item.ItemStatus;
import com.side.project.domain.itemimage.ItemImage;
import com.side.project.domain.itemimage.file.UploadFile;
import com.side.project.domain.itemimage.file.UploadFileDto;
import com.side.project.domain.member.Member;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class ItemDto {

    private Long itemId;
    private Long memberId;
    private String name ;
    private String description;
    private Integer price;
    private ItemStatus status;
    private Category category;
    private String nickName;
    private List<UploadFileDto> itemImages = new ArrayList<>();
//    private UploadFileDto uploadFileDto;

    public ItemDto(Item item) {
        this.itemId = item.getId();
        this.name = item.getName();
        this.description = item.getDescription();
        this.price = item.getPrice();
        this.status = item.getStatus();
        this.category = item.getCategory();
        this.memberId = item.getMember().getId();
        this.nickName = item.getMember().getNickName();
        this.itemImages = item.getItemImages().stream().map(itemImage ->
                new UploadFileDto(itemImage.getId(),itemImage.getOriginalFilename(),itemImage.getStoredFilename()))
                .collect(Collectors.toList());
//        this.uploadFileDto = this.itemImages.get(0);
    }
}
