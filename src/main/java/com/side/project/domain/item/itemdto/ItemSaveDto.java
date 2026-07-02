package com.side.project.domain.item.itemdto;

import com.side.project.domain.item.Category;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class ItemSaveDto {

    private Long itemId;

    @NotEmpty
    private String name;

    private String description;

    @NotNull
    private Integer price;

    private Category category;

    private MultipartFile attachFile;

    private List<MultipartFile> imageFiles;

}
