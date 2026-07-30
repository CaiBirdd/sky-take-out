package com.sky.controller.common;

import com.sky.properties.LocalUploadProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
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

/**
 * 本地上传文件访问接口
 */
@RestController
@RequestMapping("/upload/images")
public class LocalFileController {

    @Autowired
    private LocalUploadProperties localUploadProperties;

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> getImage(@PathVariable String fileName) throws IOException {
        if (fileName.contains("/") || fileName.contains("\\")) {
            return ResponseEntity.notFound().build();
        }

        Path uploadPath = Paths.get(localUploadProperties.getPath()).toAbsolutePath().normalize();
        Path filePath = uploadPath.resolve(fileName).normalize();
        if (!filePath.startsWith(uploadPath) || !Files.isRegularFile(filePath)) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(filePath);
        MediaType mediaType = contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);

        Resource resource = new FileSystemResource(filePath);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }
}
