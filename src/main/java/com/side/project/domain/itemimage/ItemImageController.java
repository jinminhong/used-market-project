package com.side.project.domain.itemimage;

import com.side.project.domain.itemimage.file.FileStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ItemImageController {

    private final FileStore fileStore;

    @GetMapping("/{storedFilename:.+}")
    public ResponseEntity<Resource> image(@PathVariable String storedFilename) throws IOException {
        log.info("storedFilename ={}" , storedFilename);
        if (storedFilename.contains("..") || storedFilename.contains("/") || storedFilename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        Path imagePath = Paths.get(fileStore.getFullPath(storedFilename)).normalize();
        Resource resource = new UrlResource(imagePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(imagePath);
        MediaType mediaType = contentType == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(contentType);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }
}
