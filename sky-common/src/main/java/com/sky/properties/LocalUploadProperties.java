package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 本地文件上传配置
 */
@Component
@ConfigurationProperties(prefix = "sky.local-upload")
@Data
public class LocalUploadProperties {

    /**
     * 文件保存目录
     */
    private String path;

    /**
     * 访问上传文件的URL前缀
     */
    private String urlPrefix;

}
