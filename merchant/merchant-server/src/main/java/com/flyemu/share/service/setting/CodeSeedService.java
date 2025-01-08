package com.flyemu.share.service.setting;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.flyemu.share.common.RedisLock;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.entity.setting.QCodeRule;
import com.flyemu.share.entity.setting.QCodeSeed;
import com.flyemu.share.entity.setting.CodeSeed;
import com.flyemu.share.enums.DocumentType;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.CodeSeedRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.types.dsl.EnumPath;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
@RequiredArgsConstructor
public class CodeSeedService extends AbsService {

    private final CodeSeedRepository codeSeedRepository;

    private final QCodeSeed qCodeSeed = QCodeSeed.codeSeed;
    private final QCodeRule qCodeRule = QCodeRule.codeRule;

    private final StringRedisTemplate redisTemplate;


    /**
     * 对外提供的方法
     *
     * @param merchantId
     * @param documentType
     * @return
     * @throws InterruptedException
     */
    public  String generateCode(Long merchantId, String documentType) {
        String resultCode = "";

        DocumentType documentTypeForQuery = DocumentType.valueOf(documentType);
        EnumPath<DocumentType> documentTypeEnumPath = qCodeRule.documentType;
        CodeRule codeRule = bqf.selectFrom(qCodeRule).where(
                qCodeRule.merchantId.eq(merchantId).and(documentTypeEnumPath.eq(documentTypeForQuery))
        ).fetchFirst();

        if (codeRule != null) {
            if (CodeRule.ResetPeriod.年.equals(codeRule.getResetPeriod())){
                resultCode = this.yearIncrease(merchantId,documentType,codeRule);
            }else if (CodeRule.ResetPeriod.月.equals(codeRule.getResetPeriod())){
                resultCode = this.monthIncrease(merchantId,documentType,codeRule);
            }else if (CodeRule.ResetPeriod.日.equals(codeRule.getResetPeriod())){
                resultCode = this.dayIncrease(merchantId,documentType,codeRule);
            }else if (CodeRule.ResetPeriod.季.equals(codeRule.getResetPeriod())){
                resultCode = this.quarterIncrease(merchantId,documentType,codeRule);
            }
        }
        return resultCode;
    }

    /**
     * 获取连续编号
     *
     * @param merchantId
     * @param type
     * @return
     */
    private Integer next(Long merchantId, String type,CodeRule codeRule) {
        String lockName = type + ":" + merchantId;
        RedisLock rLock = new RedisLock(redisTemplate, lockName);
        try {
            //获取订单锁
            boolean res = rLock.lock();
            Assert.isTrue(res, "系统繁忙，请稍后~");
            CodeSeed codeSeed = bqf.selectFrom(qCodeSeed).where(qCodeSeed.merchantId.eq(merchantId).and(qCodeSeed.type.eq(type))).fetchFirst();
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


    /**
     * 按年份递增的编码：例如:2021001
     *
     * @param merchantId
     * @param documentType
     * @return
     * @throws InterruptedException
     */
    private String yearIncrease(Long merchantId, String documentType,CodeRule codeRule) {
        Date currentDate = new Date();
        String resetPeriod = DateUtil.format(currentDate, "yyyy");
        Integer next = this.next(merchantId, documentType + ":" + resetPeriod,codeRule);
        return this.generateSerialNumber(next, codeRule,currentDate);
    }

    /**
     * 按月份递增的编码：例如:202101001
     *
     * @param merchantId
     * @param documentType
     * @return
     * @throws InterruptedException
     */
    private String monthIncrease(Long merchantId, String documentType,CodeRule codeRule) {
        Date currentDate = new Date();
        String resetPeriod = DateUtil.format(currentDate, "yyyyMM");
        Integer next = this.next(merchantId, documentType + ":" + resetPeriod,codeRule);
        return this.generateSerialNumber(next, codeRule,currentDate);

    }

    /**
     * 按日递增的编码：例如:20210101001
     *
     * @param merchantId
     * @param documentType
     * @return
     * @throws InterruptedException
     */
    private String dayIncrease(Long merchantId, String documentType,CodeRule codeRule) {
        Date currentDate = new Date();
        String resetPeriod = DateUtil.format(currentDate, "yyyyMMdd");
        Integer next = this.next(merchantId, documentType + ":" + resetPeriod,codeRule);
        return this.generateSerialNumber(next, codeRule,currentDate);
    }

    /**
     * 按季度递增的编码：例如:202101001
     *
     * @param merchantId
     * @param documentType
     * @return
     * @throws InterruptedException
     */
    private String quarterIncrease(Long merchantId, String documentType,CodeRule codeRule) {
        Date currentDate = new Date();
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
        String year = yearFormat.format(currentDate);
        // 季度
        int quarter = DateUtil.quarterEnum(currentDate).getValue();
        // resetPeriod 末尾追加 字符 quarter 和  按月 做区分
        String resetPeriod = year + quarter +"quarter";
        Integer next = this.next(merchantId, documentType + ":" + resetPeriod,codeRule);

        return this.generateSerialNumber(next, codeRule,currentDate);
    }


    /**
     *
     * @param code
     * @param codeRule
     * @param currentDate
     * @return
     */
    private static String generateSerialNumber(Integer code, CodeRule codeRule,Date currentDate) {
        String codeStr = code.toString();
        String paddedCodeStr = StrUtil.padPre(codeStr, codeRule.getSerialNumberLength(), '0');

        // 商品编码和订单编码区别, 商品编码生成规则,format是分类编码,订单编码format是日期格式
        String appendCharacter = "";
        // 目前采用单据类型名称来区分 基础资料 还是 单据
        if (codeRule.getDocumentType().toString().contains("单")){
            appendCharacter = DateUtil.format(currentDate, codeRule.getFormat());
        }else {
            appendCharacter = codeRule.getFormat();

        }
        String str = StrUtil.addPrefixIfNot(paddedCodeStr, appendCharacter);

        return StrUtil.addPrefixIfNot(str, codeRule.getPrefix());
    }


}
