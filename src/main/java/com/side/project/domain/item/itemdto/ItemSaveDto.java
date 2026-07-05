package com.side.project.domain.item.itemdto;

import com.side.project.domain.item.Category;
import com.side.project.domain.itemimage.file.UploadFile;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Data
public class ItemSaveDto {

    private Long itemId;
    private Long itemImageId;

    @NotEmpty
    private String name;

    private String description;

    @NotNull
    private Integer price;

    private Category category;

    private List<UploadFile> uploadFiles = new ArrayList<>();

}
