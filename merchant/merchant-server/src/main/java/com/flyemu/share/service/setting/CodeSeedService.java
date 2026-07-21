package com.flyemu.share.service.setting;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.flyemu.share.common.RedisLock;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.entity.setting.CodeSeed;
import com.flyemu.share.entity.setting.QCodeRule;
import com.flyemu.share.entity.setting.QCodeSeed;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.CodeSeedRepository;
import com.flyemu.share.service.AbsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @功能描述: 自动生成code编码
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CodeSeedService extends AbsService {

    private final CodeSeedRepository codeSeedRepository;
    private final CodeRuleService codeRuleService;
    private final StringRedisTemplate redisTemplate;

    private final QCodeSeed qCodeSeed = QCodeSeed.codeSeed;
    private final QCodeRule qCodeRule = QCodeRule.codeRule;

    /**
     * 兼容旧调用：仅按商户生成（不推荐，可能跨账套冲突）
     */
    @Transactional
    public String generateCode(Long merchantId, String documentType) {
        CodeRule codeRule = bqf.selectFrom(qCodeRule)
                .where(qCodeRule.merchantId.eq(merchantId)
                        .and(qCodeRule.documentType.eq(CodeRule.DocumentType.valueOf(documentType))))
                .orderBy(qCodeRule.id.asc())
                .fetchFirst();
        Long accountBookId = codeRule != null ? codeRule.getAccountBookId() : null;
        Assert.notNull(accountBookId, "未配置编码规则，请先在系统设置中初始化编码规则");
        return generateCode(merchantId, accountBookId, documentType);
    }

    /**
     * 按商户+账套生成单据编号
     */
    @Transactional
    public String generateCode(Long merchantId, Long accountBookId, String documentType) {
        Assert.notNull(merchantId, "商户ID不能为空");
        Assert.notNull(accountBookId, "账套ID不能为空");
        Assert.notBlank(documentType, "单据类型不能为空");

        CodeRule.DocumentType documentTypeForQuery = CodeRule.DocumentType.valueOf(documentType);
        CodeRule codeRule = bqf.selectFrom(qCodeRule).where(
                qCodeRule.merchantId.eq(merchantId)
                        .and(qCodeRule.accountBookId.eq(accountBookId))
                        .and(qCodeRule.documentType.eq(documentTypeForQuery))
        ).orderBy(qCodeRule.systemDefault.desc(), qCodeRule.id.asc()).fetchFirst();

        if (codeRule == null) {
            codeRuleService.ensureDefaultRules(merchantId, accountBookId);
            codeRule = bqf.selectFrom(qCodeRule).where(
                    qCodeRule.merchantId.eq(merchantId)
                            .and(qCodeRule.accountBookId.eq(accountBookId))
                            .and(qCodeRule.documentType.eq(documentTypeForQuery))
            ).orderBy(qCodeRule.systemDefault.desc(), qCodeRule.id.asc()).fetchFirst();
        }

        Assert.notNull(codeRule, "未找到【" + documentType + "】编码规则，请先在系统设置中配置");

        String resultCode;
        if (CodeRule.ResetPeriod.年.equals(codeRule.getResetPeriod())) {
            resultCode = this.yearIncrease(merchantId, accountBookId, documentType, codeRule);
        } else if (CodeRule.ResetPeriod.月.equals(codeRule.getResetPeriod())) {
            resultCode = this.monthIncrease(merchantId, accountBookId, documentType, codeRule);
        } else if (CodeRule.ResetPeriod.日.equals(codeRule.getResetPeriod())) {
            resultCode = this.dayIncrease(merchantId, accountBookId, documentType, codeRule);
        } else if (CodeRule.ResetPeriod.季.equals(codeRule.getResetPeriod())) {
            resultCode = this.quarterIncrease(merchantId, accountBookId, documentType, codeRule);
        } else {
            throw new ServiceException("不支持的流水号重置周期");
        }

        if (StrUtil.isBlank(resultCode)) {
            throw new ServiceException("生成【" + documentType + "】单号失败，请检查编码规则配置");
        }
        return resultCode;
    }

    private Integer next(Long merchantId, String type, CodeRule codeRule) {
        String lockName = type + ":" + merchantId;
        RedisLock rLock = new RedisLock(redisTemplate, lockName);
        try {
            boolean res = rLock.lock();
            Assert.isTrue(res, "系统繁忙，请稍后~");
            CodeSeed codeSeed = bqf.selectFrom(qCodeSeed)
                    .where(qCodeSeed.merchantId.eq(merchantId).and(qCodeSeed.type.eq(type)))
                    .fetchFirst();
            if (codeSeed == null) {
                codeSeed = new CodeSeed();
                codeSeed.setMerchantId(merchantId);
                codeSeed.setCode(codeRule.getStartValue());
                codeSeed.setType(type);
            } else {
                codeSeed.increase();
            }
            codeSeedRepository.save(codeSeed);
            return codeSeed.getCode();
        } catch (InterruptedException e) {
            throw new ServiceException("编码生成错误~", e);
        } finally {
            rLock.unlock();
        }
    }

    private String seedType(Long accountBookId, String documentType, String resetPeriod) {
        return documentType + ":" + accountBookId + ":" + resetPeriod;
    }

    private String yearIncrease(Long merchantId, Long accountBookId, String documentType, CodeRule codeRule) {
        Date currentDate = new Date();
        String resetPeriod = DateUtil.format(currentDate, "yyyy");
        Integer next = this.next(merchantId, seedType(accountBookId, documentType, resetPeriod), codeRule);
        return this.generateSerialNumber(next, codeRule, currentDate);
    }

    private String monthIncrease(Long merchantId, Long accountBookId, String documentType, CodeRule codeRule) {
        Date currentDate = new Date();
        String resetPeriod = DateUtil.format(currentDate, "yyyyMM");
        Integer next = this.next(merchantId, seedType(accountBookId, documentType, resetPeriod), codeRule);
        return this.generateSerialNumber(next, codeRule, currentDate);
    }

    private String dayIncrease(Long merchantId, Long accountBookId, String documentType, CodeRule codeRule) {
        Date currentDate = new Date();
        String resetPeriod = DateUtil.format(currentDate, "yyyyMMdd");
        Integer next = this.next(merchantId, seedType(accountBookId, documentType, resetPeriod), codeRule);
        return this.generateSerialNumber(next, codeRule, currentDate);
    }

    private String quarterIncrease(Long merchantId, Long accountBookId, String documentType, CodeRule codeRule) {
        Date currentDate = new Date();
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
        String year = yearFormat.format(currentDate);
        int quarter = DateUtil.quarterEnum(currentDate).getValue();
        String resetPeriod = year + quarter + "quarter";
        Integer next = this.next(merchantId, seedType(accountBookId, documentType, resetPeriod), codeRule);
        return this.generateSerialNumber(next, codeRule, currentDate);
    }

    private static String generateSerialNumber(Integer code, CodeRule codeRule, Date currentDate) {
        String codeStr = code.toString();
        String paddedCodeStr = StrUtil.padPre(codeStr, codeRule.getSerialNumberLength(), '0');

        String appendCharacter;
        if (codeRule.getDocumentType().toString().contains("单")) {
            appendCharacter = DateUtil.format(currentDate, codeRule.getFormat());
        } else {
            appendCharacter = codeRule.getFormat();
        }
        String str = StrUtil.addPrefixIfNot(paddedCodeStr, appendCharacter);
        return StrUtil.addPrefixIfNot(str, codeRule.getPrefix());
    }
}
