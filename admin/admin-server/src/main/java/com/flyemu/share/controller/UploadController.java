package com.flyemu.share.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.flyemu.share.config.AppConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;

/**
 * @功能描述: 文件上传
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Slf4j
@Controller
@RequestMapping
@RequiredArgsConstructor
@SaCheckLogin
public class UploadController {

    private final AppConfig appConfig;

    @PostMapping("/upload/{type}")
    @ResponseBody
    public JsonResult uploadPhoto(@RequestParam("file") MultipartFile file, @RequestParam(value = "name", required = false) String name, @PathVariable String type) {
        try {
            File photoDir = new File(appConfig.getUploadRoot(), type);
            if (!photoDir.exists()) {
                photoDir.mkdirs();
            }
            if (StrUtil.isEmpty(name)) {
                name = UUID.fastUUID() + "." + FileNameUtil.getSuffix(file.getOriginalFilename());
            }
            file.transferTo(new File(photoDir, name));
            return JsonResult.successful()
                    .data("filename", file.getOriginalFilename())
                    .data("path", "/attachment/" + type + "/" + name);
        } catch (IOException e) {
            log.error("上传图片失败,请检查配置路径~", e);
            return JsonResult.failure(e.getMessage());
        }
    }

    @GetMapping("/attachment/**")
    public ResponseEntity<byte[]> attachment(HttpServletRequest request, String name) {
        try {
            File file = new File(appConfig.getUploadRoot(), request.getServletPath().replace("/attachment/", ""));
            HttpHeaders headers = new HttpHeaders();
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");
            headers.add("Last-Modified", new Date().toString());
            headers.add("ETag", String.valueOf(System.currentTimeMillis()));
            if (file.exists()) {
                MediaType mediaType = MediaType.parseMediaType(FileUtil.getMimeType(file.getAbsolutePath()));
                switch (mediaType.toString()) {
                    case MediaType.IMAGE_JPEG_VALUE:
                    case MediaType.IMAGE_PNG_VALUE:
                    case MediaType.IMAGE_GIF_VALUE:
                        break;
                    default:
                        headers.setContentDispositionFormData("attachment", URLEncoder.encode(StrUtil.emptyToDefault(name, FileNameUtil.getName(file)), "UTF-8"));
                        break;
                }

                return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(file.length())
                        .contentType(mediaType)
                        .body(FileUtil.readBytes(file));
            }
        } catch (Exception e) {
            log.error("文件读取失败~", e);
        }
        return ResponseEntity.notFound().build();
    }
}
