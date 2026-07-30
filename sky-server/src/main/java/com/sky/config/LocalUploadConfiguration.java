package com.sky.config;

import com.sky.properties.LocalUploadProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.nio.file.Paths;

/**
 * 本地上传默认配置
 */
@Configuration
public class LocalUploadConfiguration {

    private static final String DEFAULT_URL_PREFIX = "http://localhost:8080/upload/images";

    @Autowired
    private LocalUploadProperties localUploadProperties;

    @PostConstruct
    public void init() {
        if (localUploadProperties.getPath() == null || localUploadProperties.getPath().isEmpty()) {
            localUploadProperties.setPath(Paths.get(System.getProperty("user.dir"), "upload", "images").toString());
        }
        if (localUploadProperties.getUrlPrefix() == null || localUploadProperties.getUrlPrefix().isEmpty()) {
            localUploadProperties.setUrlPrefix(DEFAULT_URL_PREFIX);
        }
    }
}
