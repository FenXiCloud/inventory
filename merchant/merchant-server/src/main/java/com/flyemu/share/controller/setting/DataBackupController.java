package com.flyemu.share.controller.setting;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
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

/** 备份与恢复 */
@RestController
@RequestMapping("/dataBackup")
@RequiredArgsConstructor
public class DataBackupController {

    private final DataBackupService dataBackupService;
    private final AdminService adminService;
    private final SystemLogService systemLogService;

    @GetMapping
    public JsonResult list(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(dataBackupService.list(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @PostMapping("/create")
    public JsonResult create(@RequestBody(required = false) Map<String, String> body,
                             @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        String remarks = body == null ? null : body.get("remarks");
        String adminName = resolveAdminName(adminId);
        DataBackup backup = dataBackupService.create(accountDto.getMerchantId(), accountDto.getAccountBookId(), adminId, adminName, remarks);
        systemLogService.record("备份与恢复", SystemLog.OperationType.新增,
                "创建备份「" + backup.getFileName() + "」",
                null, remarks, backup.getId(), adminId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful(backup);
    }

    @PostMapping("/restore/{id}")
    public JsonResult restore(@PathVariable Long id,
                              @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        DataBackup backup = dataBackupService.get(id, accountDto.getMerchantId(), accountDto.getAccountBookId());
        dataBackupService.restoreFromBackup(id, accountDto.getMerchantId(), accountDto.getAccountBookId());
        systemLogService.record("备份与恢复", SystemLog.OperationType.导入,
                "从备份「" + backup.getFileName() + "」恢复设置",
                null, null, id, adminId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @PostMapping("/restoreUpload")
    public JsonResult restoreUpload(@RequestParam("file") MultipartFile file,
                                    @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        dataBackupService.restoreFromUpload(file, accountDto.getMerchantId(), accountDto.getAccountBookId());
        systemLogService.record("备份与恢复", SystemLog.OperationType.导入,
                "上传恢复备份「" + (file == null ? "-" : file.getOriginalFilename()) + "」",
                null, null, null, adminId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @DeleteMapping("/{id}")
    public JsonResult delete(@PathVariable Long id,
                             @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        DataBackup backup = dataBackupService.get(id, accountDto.getMerchantId(), accountDto.getAccountBookId());
        dataBackupService.delete(id, accountDto.getMerchantId(), accountDto.getAccountBookId());
        systemLogService.record("备份与恢复", SystemLog.OperationType.删除,
                "删除备份「" + backup.getFileName() + "」",
                null, null, id, adminId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<FileSystemResource> download(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        DataBackup backup = dataBackupService.get(id, accountDto.getMerchantId(), accountDto.getAccountBookId());
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
