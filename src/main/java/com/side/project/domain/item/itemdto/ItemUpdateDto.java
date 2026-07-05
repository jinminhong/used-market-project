package com.side.project.domain.item.itemdto;

import com.side.project.domain.item.Category;
import com.side.project.domain.item.ItemStatus;
import com.side.project.domain.itemimage.file.UploadFile;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ItemUpdateDto {

    private String name;
    private String description;
    private Integer price;
    private ItemStatus status;
    private Category category;
    private List<Long> deletedFileIds = new ArrayList<>();

    private List<UploadFile> uploadFiles = new ArrayList<>();
}
