package com.flyemu.share.service.setting;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.entity.setting.QVoucherTemplate;
import com.flyemu.share.entity.setting.VoucherTemplate;
import com.flyemu.share.repository.VoucherTemplateRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @功能描述: 凭证模板
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VoucherTemplateService extends AbsService {

    private final static QVoucherTemplate qVoucherTemplate = QVoucherTemplate.voucherTemplate;

    private final VoucherTemplateRepository voucherTemplateRepository;

    public List<VoucherTemplate> query(Query query) {
        List<VoucherTemplate> voucherTemplates = bqf.selectFrom(qVoucherTemplate)
                .where(query.builder)
                .orderBy(qVoucherTemplate.id.desc())
                .fetch();
        return voucherTemplates;
    }

    @Transactional
    public VoucherTemplate save(VoucherTemplate voucherTemplate) {
        if (voucherTemplate.getId() != null) {
            //更新
            VoucherTemplate original = voucherTemplateRepository.getById(voucherTemplate.getId());
            BeanUtil.copyProperties(voucherTemplate, original, CopyOptions.create().ignoreNullValue());
            return voucherTemplateRepository.save(original);
        }
        return voucherTemplateRepository.save(voucherTemplate);
    }

    @Transactional
    public void delete(Long voucherTemplateId, Long merchantId, Long accountBookId) {
        jqf.delete(qVoucherTemplate)
                .where(qVoucherTemplate.id.eq(voucherTemplateId).and(qVoucherTemplate.merchantId.eq(merchantId)).and(qVoucherTemplate.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<VoucherTemplate> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qVoucherTemplate).where(qVoucherTemplate.merchantId.eq(merchantId).and(qVoucherTemplate.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qVoucherTemplate.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qVoucherTemplate.accountBookId.eq(accountBookId));
            }
        }
    }
}
