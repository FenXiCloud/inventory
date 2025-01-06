package com.flyemu.share.service.fund;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.fund.QVerification;
import com.flyemu.share.entity.fund.Verification;
import com.flyemu.share.repository.VerificationRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @功能描述: 核销单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VerificationService extends AbsService {

    private final static QVerification qVerification = QVerification.verification;

    private final VerificationRepository verificationRepository;

    public PageResults<Verification> query(Page page, VerificationService.Query query) {
        PagedList<Verification> fetchPage = bqf.selectFrom(qVerification).where(query.builder).orderBy(qVerification.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<Verification> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            Verification verification1 = tuple;
            Verification verification = BeanUtil.toBean(verification1, Verification.class);
            dtos.add(verification);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public Verification save(Verification verification) {
        if (verification.getId() != null) {
            //更新
            Verification original = verificationRepository.getById(verification.getId());
            BeanUtil.copyProperties(verification, original, CopyOptions.create().ignoreNullValue());
            return verificationRepository.save(original);
        }
        return verificationRepository.save(verification);
    }

    @Transactional
    public void delete(Long supplierFlowId, Long merchantId, Long accountBookId) {
        jqf.delete(qVerification)
                .where(qVerification.id.eq(supplierFlowId).and(qVerification.merchantId.eq(merchantId)).and(qVerification.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<Verification> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qVerification).where(qVerification.merchantId.eq(merchantId).and(qVerification.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qVerification.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qVerification.accountBookId.eq(accountBookId));
            }
        }

    }
}
