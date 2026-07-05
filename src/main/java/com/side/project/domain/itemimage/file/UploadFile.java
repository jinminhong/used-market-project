package com.side.project.domain.itemimage.file;

import lombok.Data;

@Data
public class UploadFile {

    private String originalFilename;
    private String storedFileName;

    public UploadFile(String originalFilename, String storedFileName) {
        this.originalFilename = originalFilename;
        this.storedFileName = storedFileName;
    }
}