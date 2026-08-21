package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.properties.LocalUploadProperties;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 通用接口
 */
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {

    @Autowired
    private LocalUploadProperties localUploadProperties;

    /**
     * 本地文件上传
     *
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    //注意这里是文件不是json数据，所以没有@RequestBody注解
    //MultipartFile 是 Spring MVC 提供的文件上传对象
    //前端以表单方式上传文件时，后端就可以用它接收文件
    public Result<String> upload(MultipartFile file) {
        log.info("文件上传：{}", file);

        if (file == null || file.isEmpty()) {
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        //用 UUID 生成唯一文件名，避免上传文件重名，再保留原文件后缀
        String fileName = UUID.randomUUID() + suffix;
        //创建上传目录
        File uploadDir = new File(localUploadProperties.getPath());
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        try {
            file.transferTo(new File(uploadDir, fileName));
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }


        String url = localUploadProperties.getUrlPrefix() + "/" + fileName;
        return Result.success(url);
    }
}
