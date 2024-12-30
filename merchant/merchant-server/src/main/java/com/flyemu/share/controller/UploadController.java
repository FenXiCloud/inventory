package com.flyemu.share.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.flyemu.share.aliyuncs.OssService;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.config.AppConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Controller
@RequestMapping
@RequiredArgsConstructor
public class UploadController {

    private final AppConfig appConfig;

    @Autowired
    private OssService ossService;

    @Value("${aliyun.bucketName}")
    private String bucketName;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM");

    public static File imageGenerateSmall(File file, double smallSize) {
        try {
            Image image = ImageIO.read(file);
            int width = image.getWidth(null);
            int height = image.getHeight(null);
            int widthSmall = (int) (width / smallSize);
            int heightSmall = (int) (height / smallSize);
            BufferedImage buff = new BufferedImage(widthSmall, heightSmall, BufferedImage.TYPE_INT_RGB);
            Graphics g = buff.getGraphics();
            g.drawImage(image, 0, 0, widthSmall, heightSmall, null);
            g.dispose();
            ImageIO.write(buff, "jpg", file);
        } catch (IOException e) {
            log.error("图片读取失败~~");
            return null;
        }
        return file;
    }


    @GetMapping("/attachment/**")
    public ResponseEntity<byte[]> attachment(HttpServletRequest request, String name) {
        try {
            File file = new File(appConfig.getUploadRoot(), request.getServletPath().replace("/attachment/", ""));
            HttpHeaders headers = new HttpHeaders();
            headers.add("Cache-Control", "max-age=2592000");
            if (file.exists()) {
                ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity.ok();
                String fileName = URLEncoder.encode(StrUtil.emptyToDefault(name, FileNameUtil.getName(file)), "UTF-8");
                String mimeType = FileUtil.getMimeType(file.getAbsolutePath());
                if (mimeType != null) {
                    try {
                        MediaType mediaType = MediaType.parseMediaType(mimeType);
                        switch (mediaType.toString()) {
                            case MediaType.IMAGE_JPEG_VALUE:
                            case MediaType.IMAGE_PNG_VALUE:
                            case MediaType.IMAGE_GIF_VALUE:
                            case MediaType.APPLICATION_PDF_VALUE:
                                bodyBuilder.contentType(mediaType);
                                break;
                            default:
                                headers.setContentDispositionFormData("attachment", fileName);
                                break;
                        }
                    } catch (Exception e) {
                        headers.setContentDispositionFormData("attachment", fileName);
                    }
                } else {
                    headers.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());
                }

                return bodyBuilder
                        .headers(headers)
                        .contentLength(file.length())
                        .body(FileUtil.readBytes(file));
            }
        } catch (Exception e) {
            log.error("文件读取失败~", e);
        }
        return ResponseEntity.notFound().build();
    }

    public static File imageCompress(File file,int kb) throws Exception {
        long fileSize = file.length();
        if (fileSize > 200 * 1024) {
            int smallSize = (int) (fileSize % kb == 0 ? fileSize / kb : fileSize / kb + 1);
            double size = Math.ceil(Math.sqrt(smallSize));
//            file = imageGenerateSmall(file, size);
            file = imageGenerateSmall(file, 2);
        }
        return file;
    }

    @PostMapping("/upload/{type}")
    @ResponseBody
    public JsonResult uploadPhoto(@RequestParam("file") MultipartFile file, @RequestParam(value = "name", required = false) String name, @PathVariable String type) {
        try {
            File photoDir = new File(appConfig.getUploadRoot(), type);
            if (!photoDir.exists()) {
                photoDir.mkdirs();
            }
            String suffix = FileNameUtil.getSuffix(file.getOriginalFilename());
            if (StrUtil.isEmpty(name)) {
                name = UUID.fastUUID() + "." + (StrUtil.isNotBlank(suffix) ? suffix : "png");
            }
            File file1 = new File(photoDir, name);
            file.transferTo(file1);
            if (StrUtil.equals(type,"description")){
                imageCompress(file1,200 * 1024);//压制为200kb以下
            }
            return JsonResult.successful()
                    .data("filename", file.getOriginalFilename())
                    .data("path", "/attachment/" + type + "/" + name);
        } catch (Exception e) {
            log.error("上传图片失败,请检查配置路径~", e);
            return JsonResult.failure(e.getMessage());
        }
    }



    @PostMapping("/oss/upload/{type}")
    @ResponseBody
    public JsonResult ossUpload(@RequestParam("file") MultipartFile file,@RequestParam(value = "name", required = false) String name,
                                       @PathVariable String type,@SaMerchantId Long merchantId) {
        try {
            String path = (merchantId+"/chapters/"+type).toLowerCase() + "/" + sdf.format(new Date()) + "/" + file.getOriginalFilename();
            OssService.UploadBody body = new OssService.UploadBody(bucketName, file.getInputStream(), path);
            ossService.upload(body);
            return JsonResult.successful(body.getUrl());
        } catch (Exception e) {
            return JsonResult.failure(e.getMessage());
        }
    }
}
