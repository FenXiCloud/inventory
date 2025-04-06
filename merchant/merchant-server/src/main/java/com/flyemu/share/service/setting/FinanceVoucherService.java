package com.flyemu.share.service.setting;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.setting.FinanceVoucher;
import com.flyemu.share.entity.setting.QFinanceVoucher;
import com.flyemu.share.repository.FinanceVoucherRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


/**
 * @功能描述: 云财务凭证
 * @创建时间: 2025年03月11日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FinanceVoucherService extends AbsService {

    private final static QFinanceVoucher qFinanceVoucher = QFinanceVoucher.financeVoucher;

    private final FinanceVoucherRepository financeVoucherRepository;


    public List<FinanceVoucher> query(FinanceVoucherService.Query query) {
        return bqf.selectFrom(qFinanceVoucher)
                .where(query.builder)
                .orderBy(qFinanceVoucher.id.desc())
                .fetch();
    }

    @Transactional
    public FinanceVoucher save(FinanceVoucher financeVoucher, AccountDto accountDto) {
        if (financeVoucher.getId() != null) {
            //更新
            FinanceVoucher original = financeVoucherRepository.getById(financeVoucher.getId());
            BeanUtil.copyProperties(financeVoucher, original, CopyOptions.create().ignoreNullValue());
            original.setUpdatedAt(LocalDateTime.now());
            return financeVoucherRepository.save(original);
        }
        financeVoucher.setCreatedAt(LocalDateTime.now());
        financeVoucher.setCreatedBy(accountDto.getAdminId());
        return financeVoucherRepository.save(financeVoucher);
    }

    @Transactional
    public void delete(Long id, Long merchantId, Long accountBookId) {
        jqf.delete(qFinanceVoucher)
                .where(qFinanceVoucher.id.eq(id).and(qFinanceVoucher.merchantId.eq(merchantId)).and(qFinanceVoucher.accountBookId.eq(accountBookId)))
                .execute();
    }

    public FinanceVoucher load(Long id) {
        return financeVoucherRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qFinanceVoucher.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qFinanceVoucher.accountBookId.eq(accountBookId));
            }
        }

    }
}
