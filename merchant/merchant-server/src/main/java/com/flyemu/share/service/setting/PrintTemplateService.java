package com.flyemu.share.service.setting;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.entity.setting.PrintTemplate;
import com.flyemu.share.entity.setting.QPrintTemplate;
import com.flyemu.share.repository.PrintTemplateRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @功能描述: 编码规则
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PrintTemplateService extends AbsService {

    private final static QPrintTemplate qPrintTemplate = QPrintTemplate.printTemplate;

    private final PrintTemplateRepository printTemplateRepository;

    public List<PrintTemplate> query(Query query) {
        List<PrintTemplate> printTemplates = bqf.selectFrom(qPrintTemplate)
                .where(query.builder)
                .orderBy(qPrintTemplate.id.desc())
                .fetch();
        return printTemplates;
    }

    @Transactional
    public PrintTemplate save(PrintTemplate printTemplate) {
        if (printTemplate.getId() != null) {
            //更新
            PrintTemplate original = printTemplateRepository.getById(printTemplate.getId());
            BeanUtil.copyProperties(printTemplate, original, CopyOptions.create().ignoreNullValue());
            return printTemplateRepository.save(original);
        }
        return printTemplateRepository.save(printTemplate);
    }

    @Transactional
    public void delete(Long printTemplateId, Long merchantId, Long accountBookId) {
        jqf.delete(qPrintTemplate)
                .where(qPrintTemplate.id.eq(printTemplateId).and(qPrintTemplate.merchantId.eq(merchantId)).and(qPrintTemplate.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<PrintTemplate> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qPrintTemplate).where(qPrintTemplate.merchantId.eq(merchantId).and(qPrintTemplate.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qPrintTemplate.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qPrintTemplate.accountBookId.eq(accountBookId));
            }
        }
    }
}
