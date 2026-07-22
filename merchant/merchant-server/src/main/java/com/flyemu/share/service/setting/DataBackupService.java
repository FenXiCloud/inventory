package com.flyemu.share.service.setting;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flyemu.share.config.AppConfig;
import com.flyemu.share.entity.basic.AccountType;
import com.flyemu.share.entity.basic.PaymentMethod;
import com.flyemu.share.entity.basic.QAccountType;
import com.flyemu.share.entity.basic.QPaymentMethod;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.entity.setting.DataBackup;
import com.flyemu.share.entity.setting.PrintTemplate;
import com.flyemu.share.entity.setting.QCodeRule;
import com.flyemu.share.entity.setting.QPrintTemplate;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.basic.AccountTypeRepository;
import com.flyemu.share.repository.setting.CodeRuleRepository;
import com.flyemu.share.repository.setting.DataBackupRepository;
import com.flyemu.share.repository.basic.PaymentMethodRepository;
import com.flyemu.share.repository.setting.PrintTemplateRepository;
import com.flyemu.share.service.BaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 数据备份与恢复（系统设置） */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DataBackupService extends BaseService {

    private static final String BACKUP_VERSION = "1.0";
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final DataBackupRepository dataBackupRepository;
    private final PrintTemplateRepository printTemplateRepository;
    private final CodeRuleRepository codeRuleRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final AppConfig appConfig;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public List<DataBackup> list(Long merchantId, Long accountBookId) {
        return dataBackupRepository.findByMerchantIdAndAccountBookIdOrderByCreatedAtDesc(merchantId, accountBookId);
    }

    public DataBackup get(Long id, Long merchantId, Long accountBookId) {
        return dataBackupRepository.findByIdAndMerchantIdAndAccountBookId(id, merchantId, accountBookId)
                .orElseThrow(() -> new ServiceException("备份记录不存在"));
    }

    @Transactional
    public DataBackup create(Long merchantId, Long accountBookId, Long adminId, String adminName, String remarks) {
        try {
            Map<String, Object> payload = buildPayload(merchantId, accountBookId);
            String ts = LocalDateTime.now().format(FILE_TS);
            String fileName = "backup_" + accountBookId + "_" + ts + ".json";
            File dir = appConfig.getUploadRoot("backup/" + merchantId + "/" + accountBookId);
            File file = new File(dir, fileName);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, payload);

            DataBackup backup = new DataBackup();
            backup.setFileName(fileName);
            backup.setFilePath("backup/" + merchantId + "/" + accountBookId + "/" + fileName);
            backup.setFileSize(file.length());
            backup.setRemarks(StrUtil.blankToDefault(remarks, "系统设置备份（打印模板/编码规则/收支类别/结算方式）"));
            backup.setCreatedBy(adminId);
            backup.setCreatedByName(adminName);
            backup.setCreatedAt(LocalDateTime.now());
            backup.setMerchantId(merchantId);
            backup.setAccountBookId(accountBookId);
            return dataBackupRepository.save(backup);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建备份失败", e);
            throw new ServiceException("创建备份失败：" + e.getMessage());
        }
    }

    public File resolveFile(DataBackup backup) {
        File file = new File(appConfig.getUploadRoot(), backup.getFilePath());
        if (!file.exists()) {
            throw new ServiceException("备份文件不存在或已被删除");
        }
        return file;
    }

    @Transactional
    public void restoreFromBackup(Long id, Long merchantId, Long accountBookId) {
        DataBackup backup = get(id, merchantId, accountBookId);
        restoreFromFile(resolveFile(backup), merchantId, accountBookId);
    }

    @Transactional
    public void restoreFromUpload(MultipartFile multipartFile, Long merchantId, Long accountBookId) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new ServiceException("请上传备份文件");
        }
        String original = multipartFile.getOriginalFilename();
        if (StrUtil.isNotBlank(original) && !StrUtil.endWithIgnoreCase(original, ".json")) {
            throw new ServiceException("请上传 JSON 格式的备份文件");
        }
        try {
            File temp = File.createTempFile("restore_", ".json");
            multipartFile.transferTo(temp);
            try {
                restoreFromFile(temp, merchantId, accountBookId);
            } finally {
                FileUtil.del(temp);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("恢复备份失败", e);
            throw new ServiceException("恢复备份失败：" + e.getMessage());
        }
    }

    @Transactional
    public void delete(Long id, Long merchantId, Long accountBookId) {
        DataBackup backup = get(id, merchantId, accountBookId);
        File file = new File(appConfig.getUploadRoot(), backup.getFilePath());
        FileUtil.del(file);
        dataBackupRepository.delete(backup);
    }

    @SuppressWarnings("unchecked")
    private void restoreFromFile(File file, Long merchantId, Long accountBookId) {
        try {
            Map<String, Object> payload = objectMapper.readValue(file, Map.class);
            Object version = payload.get("version");
            if (version != null && !BACKUP_VERSION.equals(String.valueOf(version))) {
                log.warn("备份版本不匹配: expected={}, actual={}", BACKUP_VERSION, version);
            }
            if (payload.get("printTemplates") != null) {
                replacePrintTemplates(objectMapper.convertValue(payload.get("printTemplates"),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, PrintTemplate.class)), merchantId, accountBookId);
            }
            if (payload.get("codeRules") != null) {
                replaceCodeRules(objectMapper.convertValue(payload.get("codeRules"),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, CodeRule.class)), merchantId, accountBookId);
            }
            if (payload.get("accountTypes") != null) {
                mergeAccountTypes(objectMapper.convertValue(payload.get("accountTypes"),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, AccountType.class)), merchantId, accountBookId);
            }
            if (payload.get("paymentMethods") != null) {
                mergePaymentMethods(objectMapper.convertValue(payload.get("paymentMethods"),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, PaymentMethod.class)), merchantId, accountBookId);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析备份失败", e);
            throw new ServiceException("备份文件格式不正确");
        }
    }

    private Map<String, Object> buildPayload(Long merchantId, Long accountBookId) {
        List<PrintTemplate> printTemplates = fetchPrintTemplates(merchantId, accountBookId);
        List<CodeRule> codeRules = fetchCodeRules(merchantId, accountBookId);
        List<AccountType> accountTypes = fetchAccountTypes(merchantId, accountBookId);
        List<PaymentMethod> paymentMethods = fetchPaymentMethods(merchantId, accountBookId);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("printTemplates", printTemplates.size());
        summary.put("codeRules", codeRules.size());
        summary.put("accountTypes", accountTypes.size());
        summary.put("paymentMethods", paymentMethods.size());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("version", BACKUP_VERSION);
        payload.put("merchantId", merchantId);
        payload.put("accountBookId", accountBookId);
        payload.put("exportedAt", LocalDateTime.now().toString());
        payload.put("modules", List.of("printTemplates", "codeRules", "accountTypes", "paymentMethods"));
        payload.put("summary", summary);
        payload.put("printTemplates", printTemplates);
        payload.put("codeRules", codeRules);
        payload.put("accountTypes", accountTypes);
        payload.put("paymentMethods", paymentMethods);
        return payload;
    }

    private List<PrintTemplate> fetchPrintTemplates(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(QPrintTemplate.printTemplate)
                .where(QPrintTemplate.printTemplate.merchantId.eq(merchantId)
                        .and(QPrintTemplate.printTemplate.accountBookId.eq(accountBookId)))
                .fetch();
    }

    private List<CodeRule> fetchCodeRules(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(QCodeRule.codeRule)
                .where(QCodeRule.codeRule.merchantId.eq(merchantId)
                        .and(QCodeRule.codeRule.accountBookId.eq(accountBookId)))
                .fetch();
    }

    private List<AccountType> fetchAccountTypes(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(QAccountType.accountType)
                .where(QAccountType.accountType.merchantId.eq(merchantId)
                        .and(QAccountType.accountType.accountBookId.eq(accountBookId)))
                .fetch();
    }

    private List<PaymentMethod> fetchPaymentMethods(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(QPaymentMethod.paymentMethod)
                .where(QPaymentMethod.paymentMethod.merchantId.eq(merchantId)
                        .and(QPaymentMethod.paymentMethod.accountBookId.eq(accountBookId)))
                .fetch();
    }

    private void replacePrintTemplates(List<PrintTemplate> list, Long merchantId, Long accountBookId) {
        jqf.delete(QPrintTemplate.printTemplate)
                .where(QPrintTemplate.printTemplate.merchantId.eq(merchantId)
                        .and(QPrintTemplate.printTemplate.accountBookId.eq(accountBookId)))
                .execute();
        if (list == null) return;
        for (PrintTemplate item : list) {
            item.setId(null);
            item.setMerchantId(merchantId);
            item.setAccountBookId(accountBookId);
            if (item.getCreatedAt() == null) {
                item.setCreatedAt(LocalDateTime.now());
            }
            if (item.getSystemDefault() == null) {
                item.setSystemDefault(false);
            }
            printTemplateRepository.save(item);
        }
    }

    private void replaceCodeRules(List<CodeRule> list, Long merchantId, Long accountBookId) {
        jqf.delete(QCodeRule.codeRule)
                .where(QCodeRule.codeRule.merchantId.eq(merchantId)
                        .and(QCodeRule.codeRule.accountBookId.eq(accountBookId)))
                .execute();
        if (list == null) return;
        for (CodeRule item : list) {
            item.setId(null);
            item.setMerchantId(merchantId);
            item.setAccountBookId(accountBookId);
            if (item.getCreatedAt() == null) {
                item.setCreatedAt(LocalDateTime.now());
            }
            if (item.getSystemDefault() == null) {
                item.setSystemDefault(false);
            }
            codeRuleRepository.save(item);
        }
    }

    /**
     * 按名称合并收支类别，保留已有 ID，避免单据外键失效；并重建父级关系。
     */
    private void mergeAccountTypes(List<AccountType> list, Long merchantId, Long accountBookId) {
        if (list == null) return;
        Map<String, AccountType> existingByName = new HashMap<>();
        for (AccountType item : fetchAccountTypes(merchantId, accountBookId)) {
            if (StrUtil.isNotBlank(item.getName())) {
                existingByName.put(item.getName(), item);
            }
        }

        Map<Long, Long> idMap = new HashMap<>();
        List<AccountType> pendingPid = new ArrayList<>();

        for (AccountType item : list) {
            if (StrUtil.isBlank(item.getName())) {
                continue;
            }
            Long oldId = item.getId();
            Long oldPid = item.getPid();
            AccountType target = existingByName.get(item.getName());
            if (target != null) {
                target.setCostType(item.getCostType());
                if (item.getEnabled() != null) {
                    target.setEnabled(item.getEnabled());
                }
                accountTypeRepository.save(target);
                if (oldId != null) {
                    idMap.put(oldId, target.getId());
                }
                if (oldPid != null) {
                    AccountType forPid = new AccountType();
                    forPid.setId(target.getId());
                    forPid.setPid(oldPid);
                    pendingPid.add(forPid);
                }
            } else {
                item.setId(null);
                item.setMerchantId(merchantId);
                item.setAccountBookId(accountBookId);
                item.setPid(null);
                if (item.getEnabled() == null) {
                    item.setEnabled(true);
                }
                AccountType saved = accountTypeRepository.save(item);
                existingByName.put(saved.getName(), saved);
                if (oldId != null) {
                    idMap.put(oldId, saved.getId());
                }
                if (oldPid != null) {
                    AccountType forPid = new AccountType();
                    forPid.setId(saved.getId());
                    forPid.setPid(oldPid);
                    pendingPid.add(forPid);
                }
            }
        }

        for (AccountType pending : pendingPid) {
            Long mappedPid = idMap.get(pending.getPid());
            if (mappedPid == null) {
                continue;
            }
            AccountType target = accountTypeRepository.findById(pending.getId()).orElse(null);
            if (target == null || mappedPid.equals(target.getId())) {
                continue;
            }
            target.setPid(mappedPid);
            accountTypeRepository.save(target);
        }
    }

    /**
     * 按名称合并结算方式，保留已有 ID，避免收款/付款明细外键失效。
     */
    private void mergePaymentMethods(List<PaymentMethod> list, Long merchantId, Long accountBookId) {
        if (list == null) return;
        Map<String, PaymentMethod> existingByName = new HashMap<>();
        for (PaymentMethod item : fetchPaymentMethods(merchantId, accountBookId)) {
            if (StrUtil.isNotBlank(item.getName())) {
                existingByName.put(item.getName(), item);
            }
        }
        for (PaymentMethod item : list) {
            if (StrUtil.isBlank(item.getName())) {
                continue;
            }
            PaymentMethod target = existingByName.get(item.getName());
            if (target != null) {
                if (item.getEnabled() != null) {
                    target.setEnabled(item.getEnabled());
                }
                paymentMethodRepository.save(target);
            } else {
                item.setId(null);
                item.setMerchantId(merchantId);
                item.setAccountBookId(accountBookId);
                if (item.getEnabled() == null) {
                    item.setEnabled(true);
                }
                PaymentMethod saved = paymentMethodRepository.save(item);
                existingByName.put(saved.getName(), saved);
            }
        }
    }
}
