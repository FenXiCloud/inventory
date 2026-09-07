package com.flyemu.share.service.setting;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import com.flyemu.share.common.RedisLock;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.entity.setting.CodeSeed;
import com.flyemu.share.entity.setting.QCodeRule;
import com.flyemu.share.entity.setting.QCodeSeed;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.setting.CodeSeedRepository;
import com.flyemu.share.service.BaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CodeSeedService extends BaseService {

    /**
     * 基础资料四类型的 format 是字面量（如「所属分类编码」），其余单据类型的 format 一律按日期模式解析
     * （此前按「类型名是否含『单』」判断，销售预订/进货预订被误判为字面量，编码里嵌进了 yyyyMMdd 原文）。
     */
    private static final Set<CodeRule.DocumentType> BASIC_DATA_TYPES = EnumSet.of(
            CodeRule.DocumentType.商品,
            CodeRule.DocumentType.仓库,
            CodeRule.DocumentType.客户,
            CodeRule.DocumentType.供货商);

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
        Integer next = this.next(merchantId, seedType(accountBookId, documentType, bucketSuffix(codeRule, currentDate)), codeRule);
        return this.generateSerialNumber(next, codeRule, currentDate);
    }

    private String monthIncrease(Long merchantId, Long accountBookId, String documentType, CodeRule codeRule) {
        Date currentDate = new Date();
        Integer next = this.next(merchantId, seedType(accountBookId, documentType, bucketSuffix(codeRule, currentDate)), codeRule);
        return this.generateSerialNumber(next, codeRule, currentDate);
    }

    private String dayIncrease(Long merchantId, Long accountBookId, String documentType, CodeRule codeRule) {
        Date currentDate = new Date();
        Integer next = this.next(merchantId, seedType(accountBookId, documentType, bucketSuffix(codeRule, currentDate)), codeRule);
        return this.generateSerialNumber(next, codeRule, currentDate);
    }

    private String quarterIncrease(Long merchantId, Long accountBookId, String documentType, CodeRule codeRule) {
        Date currentDate = new Date();
        Integer next = this.next(merchantId, seedType(accountBookId, documentType, bucketSuffix(codeRule, currentDate)), codeRule);
        return this.generateSerialNumber(next, codeRule, currentDate);
    }

    /**
     * 单据在 date 时刻创建时应落入的"归零桶"后缀，与 seedType 一起构成 code_seed.type。
     */
    private static String bucketSuffix(CodeRule codeRule, Date date) {
        CodeRule.ResetPeriod resetPeriod = codeRule.getResetPeriod();
        if (CodeRule.ResetPeriod.年.equals(resetPeriod)) {
            return DateUtil.format(date, "yyyy");
        } else if (CodeRule.ResetPeriod.月.equals(resetPeriod)) {
            return DateUtil.format(date, "yyyyMM");
        } else if (CodeRule.ResetPeriod.日.equals(resetPeriod)) {
            return DateUtil.format(date, "yyyyMMdd");
        } else if (CodeRule.ResetPeriod.季.equals(resetPeriod)) {
            return new SimpleDateFormat("yyyy").format(date) + DateUtil.quarterEnum(date).getValue() + "quarter";
        }
        throw new ServiceException("不支持的流水号重置周期");
    }

    /**
     * 一次性迁移辅助：把某一（商户, 账套, 单据类型）在 at 时刻归零桶下的流水计数器至少抬升到 minSerial。
     * 用于把历史按 max(id)+1 生成的旧号接续到 code_seed，切换取号方式后不重号、不倒退。
     *
     * @return 实际新建/抬升的种子条数（0 = 无需改动）
     */
    @Transactional
    public int raiseSeed(Long merchantId, Long accountBookId, String documentType, LocalDateTime at, Integer minSerial) {
        if (merchantId == null || accountBookId == null || at == null || minSerial == null) {
            return 0;
        }
        CodeRule.DocumentType documentTypeForQuery;
        try {
            documentTypeForQuery = CodeRule.DocumentType.valueOf(documentType);
        } catch (IllegalArgumentException e) {
            return 0;
        }
        CodeRule codeRule = bqf.selectFrom(qCodeRule)
                .where(qCodeRule.merchantId.eq(merchantId)
                        .and(qCodeRule.accountBookId.eq(accountBookId))
                        .and(qCodeRule.documentType.eq(documentTypeForQuery)))
                .orderBy(qCodeRule.systemDefault.desc(), qCodeRule.id.asc())
                .fetchFirst();
        if (codeRule == null) {
            return 0;
        }
        Date date = Date.from(at.atZone(ZoneId.systemDefault()).toInstant());
        String type = seedType(accountBookId, documentType, bucketSuffix(codeRule, date));
        CodeSeed codeSeed = bqf.selectFrom(qCodeSeed)
                .where(qCodeSeed.merchantId.eq(merchantId).and(qCodeSeed.type.eq(type)))
                .fetchFirst();
        if (codeSeed != null && codeSeed.getCode() != null && codeSeed.getCode() >= minSerial) {
            return 0;
        }
        if (codeSeed == null) {
            codeSeed = new CodeSeed();
            codeSeed.setMerchantId(merchantId);
            codeSeed.setType(type);
        }
        codeSeed.setCode(minSerial);
        codeSeedRepository.save(codeSeed);
        return 1;
    }

    private static String generateSerialNumber(Integer code, CodeRule codeRule, Date currentDate) {
        String codeStr = code.toString();
        String paddedCodeStr = StrUtil.padPre(codeStr, codeRule.getSerialNumberLength(), '0');

        String appendCharacter;
        if (BASIC_DATA_TYPES.contains(codeRule.getDocumentType())) {
            appendCharacter = codeRule.getFormat();
        } else {
            String formatted = null;
            try {
                formatted = DateUtil.format(currentDate, codeRule.getFormat());
            } catch (Exception ignore) {
                // 非法日期模式，退回字面量
            }
            appendCharacter = formatted != null ? formatted : codeRule.getFormat();
        }
        String str = StrUtil.addPrefixIfNot(paddedCodeStr, appendCharacter);
        return StrUtil.addPrefixIfNot(str, codeRule.getPrefix());
    }

    /**
     * 预览编码效果（只读，不取号）：按草稿规则拼装，流水号窥视当前归零桶的实际进度。
     * 用于编码规则新增/编辑弹窗与规则列表的「预览」，展示结果与真实取号逻辑完全同源。
     */
    public Dict preview(Long merchantId, Long accountBookId, CodeRule draft) {
        Assert.notNull(draft.getDocumentType(), "单据类型不能为空");
        String prefix = StrUtil.nullToEmpty(draft.getPrefix());
        String format = StrUtil.nullToEmpty(draft.getFormat());
        Integer length = draft.getSerialNumberLength() == null || draft.getSerialNumberLength() <= 0
                ? 4 : draft.getSerialNumberLength();
        Integer startValue = draft.getStartValue() == null ? 1 : draft.getStartValue();
        CodeRule.ResetPeriod resetPeriod = draft.getResetPeriod() == null
                ? CodeRule.ResetPeriod.日 : draft.getResetPeriod();

        Date now = new Date();

        // 复用真实拼装逻辑（临时对象仅用于字段读取）
        CodeRule effective = new CodeRule();
        effective.setPrefix(prefix);
        effective.setFormat(format);
        effective.setSerialNumberLength(length);
        effective.setStartValue(startValue);
        effective.setResetPeriod(resetPeriod);
        effective.setDocumentType(draft.getDocumentType());

        String type = seedType(accountBookId, draft.getDocumentType().name(), bucketSuffix(effective, now));
        Integer seed = jqf.select(qCodeSeed.code)
                .from(qCodeSeed)
                .where(qCodeSeed.merchantId.eq(merchantId).and(qCodeSeed.type.eq(type)))
                .fetchOne();
        // 与 next() 语义一致：桶内无种子则从起始值开始，否则接续 +1
        int nextSerial = seed == null ? startValue : seed + 1;

        List<String> samples = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            samples.add(generateSerialNumber(nextSerial + i, effective, now));
        }

        boolean isDocument = !BASIC_DATA_TYPES.contains(draft.getDocumentType());
        String middlePart;
        if (isDocument) {
            String formatted;
            try {
                formatted = DateUtil.format(now, format);
            } catch (Exception e) {
                formatted = format;
            }
            middlePart = formatted;
        } else {
            middlePart = format;
        }

        return Dict.create()
                .set("prefixPart", prefix)
                .set("middlePart", middlePart)
                .set("serialPart", StrUtil.padPre(String.valueOf(nextSerial), length, '0'))
                .set("firstCode", generateSerialNumber(startValue, effective, now))
                .set("nextCode", samples.get(0))
                .set("samples", samples)
                .set("seedUsed", seed != null)
                .set("resetHint", "流水号按「" + resetPeriod.name() + "」清零，新周期从起始值 " + startValue + " 重新开始");
    }
}
