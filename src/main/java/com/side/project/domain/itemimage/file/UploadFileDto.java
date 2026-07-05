package com.side.project.domain.itemimage.file;

import lombok.Data;

@Data
public class UploadFileDto {
    private Long itemImageId;

    private String originalFilename;
    private String storedFileName;

    public UploadFileDto(Long itemImageId, String originalFilename, String storedFileName) {
        this.itemImageId = itemImageId;
        this.originalFilename = originalFilename;
        this.storedFileName = storedFileName;
    }
}
