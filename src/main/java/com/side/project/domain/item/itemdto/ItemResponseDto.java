package com.side.project.domain.item.itemdto;

import com.side.project.domain.item.Category;
import com.side.project.domain.item.Item;
import com.side.project.domain.item.ItemStatus;
import com.side.project.domain.itemimage.file.UploadFileDto;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ItemResponseDto {

    private Long itemId;
    private String name ;
    private String description;
    private Integer price;
    private ItemStatus status;
    private Category category;
    private String nickName;
    private UploadFileDto uploadFileDto;
    private Integer page;
    private Integer size;

    public ItemResponseDto(Item item) {
        this.itemId = item.getId();
        this.name = item.getName();
        this.description = item.getDescription();
        this.price = item.getPrice();
        this.status = item.getStatus();
        this.category = item.getCategory();
        this.nickName = item.getMember().getNickName();
        this.uploadFileDto = new UploadFileDto(item.getThumbnailImage().getId(),
                item.getThumbnailImage().getOriginalFilename(),
                item.getThumbnailImage().getStoredFilename());

    }
}
