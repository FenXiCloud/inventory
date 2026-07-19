package com.flyemu.share.controller.setting;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.setting.Admin;
import com.flyemu.share.entity.setting.DataBackup;
import com.flyemu.share.entity.setting.SystemLog;
import com.flyemu.share.service.setting.AdminService;
import com.flyemu.share.service.setting.DataBackupService;
import com.flyemu.share.service.setting.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @功能描述: 备份与恢复
 */
@RestController
@RequestMapping("/dataBackup")
@RequiredArgsConstructor
public class DataBackupController {

    private final DataBackupService dataBackupService;
    private final AdminService adminService;
    private final SystemLogService systemLogService;

    @GetMapping
    public JsonResult list(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(dataBackupService.list(merchantId, accountBookId));
    }

    @PostMapping("/create")
    public JsonResult create(@RequestBody(required = false) Map<String, String> body,
                             @SaMerchantId Long merchantId,
                             @SaAccountBookId Long accountBookId,
                             @SaAdminId Long adminId) {
        String remarks = body == null ? null : body.get("remarks");
        String adminName = resolveAdminName(adminId);
        DataBackup backup = dataBackupService.create(merchantId, accountBookId, adminId, adminName, remarks);
        systemLogService.record("备份与恢复", SystemLog.OperationType.新增,
                "创建备份「" + backup.getFileName() + "」",
                null, remarks, backup.getId(), adminId, merchantId, accountBookId);
        return JsonResult.successful(backup);
    }

    @PostMapping("/restore/{id}")
    public JsonResult restore(@PathVariable Long id,
                              @SaMerchantId Long merchantId,
                              @SaAccountBookId Long accountBookId,
                              @SaAdminId Long adminId) {
        DataBackup backup = dataBackupService.get(id, merchantId, accountBookId);
        dataBackupService.restoreFromBackup(id, merchantId, accountBookId);
        systemLogService.record("备份与恢复", SystemLog.OperationType.导入,
                "从备份「" + backup.getFileName() + "」恢复设置",
                null, null, id, adminId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @PostMapping("/restoreUpload")
    public JsonResult restoreUpload(@RequestParam("file") MultipartFile file,
                                    @SaMerchantId Long merchantId,
                                    @SaAccountBookId Long accountBookId,
                                    @SaAdminId Long adminId) {
        dataBackupService.restoreFromUpload(file, merchantId, accountBookId);
        systemLogService.record("备份与恢复", SystemLog.OperationType.导入,
                "上传恢复备份「" + (file == null ? "-" : file.getOriginalFilename()) + "」",
                null, null, null, adminId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @DeleteMapping("/{id}")
    public JsonResult delete(@PathVariable Long id,
                             @SaMerchantId Long merchantId,
                             @SaAccountBookId Long accountBookId,
                             @SaAdminId Long adminId) {
        DataBackup backup = dataBackupService.get(id, merchantId, accountBookId);
        dataBackupService.delete(id, merchantId, accountBookId);
        systemLogService.record("备份与恢复", SystemLog.OperationType.删除,
                "删除备份「" + backup.getFileName() + "」",
                null, null, id, adminId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<FileSystemResource> download(@PathVariable Long id,
                                                       @SaMerchantId Long merchantId,
                                                       @SaAccountBookId Long accountBookId) {
        DataBackup backup = dataBackupService.get(id, merchantId, accountBookId);
        File file = dataBackupService.resolveFile(backup);
        String encoded = URLEncoder.encode(backup.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(file.length())
                .body(new FileSystemResource(file));
    }

    private String resolveAdminName(Long adminId) {
        Admin admin = adminService.selectByPrimaryKey(adminId);
        return admin == null ? String.valueOf(adminId) : admin.getName();
    }
}
