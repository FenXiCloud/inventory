package com.flyemu.share.service.basic;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.entity.basic.CustomerLevel;
import com.flyemu.share.entity.basic.QCustomerLevel;
import com.flyemu.share.entity.basic.QCustomerLevelPrice;
import com.flyemu.share.repository.basic.CustomerLevelRepository;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerLevelService extends BaseService {

    private final static QCustomerLevel qCustomerLevel = QCustomerLevel.customerLevel;

    private final static QCustomerLevelPrice qCustomerLevelPrice = QCustomerLevelPrice.customerLevelPrice;

    private final CustomerLevelRepository customerLevelRepository;

    public List<CustomerLevel> query(Query query) {
        return bqf.selectFrom(qCustomerLevel)
                .where(query.builder)
                .orderBy(qCustomerLevel.id.desc())
                .fetch();
    }

    @Transactional
    public CustomerLevel save(CustomerLevel customerLevel) {
        if (customerLevel.getId() != null) {
            //更新
            CustomerLevel original = customerLevelRepository.getById(customerLevel.getId());

            //检查重复
            long count = bqf.selectFrom(qCustomerLevel)
                    .where(qCustomerLevel.merchantId.eq(original.getMerchantId()).and(qCustomerLevel.name.eq(customerLevel.getName()))
                            .and(qCustomerLevel.id.ne(customerLevel.getId())).and(qCustomerLevel.accountBookId.eq(original.getAccountBookId())))
                    .fetchCount();
            Assert.isTrue(count == 0, customerLevel.getName() + "名称已存在~");
            BeanUtil.copyProperties(customerLevel, original, CopyOptions.create().ignoreNullValue());
            return customerLevelRepository.save(original);
        }

        //检查重复
        long count = bqf.selectFrom(qCustomerLevel)
                .where(qCustomerLevel.merchantId.eq(customerLevel.getMerchantId()).and(qCustomerLevel.name.eq(customerLevel.getName()))
                        .and(qCustomerLevel.accountBookId.eq(customerLevel.getAccountBookId())))
                .fetchCount();
        Assert.isTrue(count == 0, customerLevel.getName() + "名称已存在~");
        return customerLevelRepository.save(customerLevel);
    }

    /**
     * 删除
     *
     * @param customersLevelId
     */
    @Transactional
    public void delete(Long customersLevelId, Long merchantId, Long accountBookId) {
        Assert.isFalse(bqf.selectFrom(qCustomerLevelPrice).where(qCustomerLevelPrice.customerLevelId.eq(customersLevelId).and(qCustomerLevelPrice.merchantId.eq(merchantId)).and(qCustomerLevelPrice.accountBookId.eq(accountBookId))).fetchCount() > 0, "等级已使用，不能删除");
        jqf.delete(qCustomerLevel).where(qCustomerLevel.id.eq(customersLevelId).and(qCustomerLevel.merchantId.eq(merchantId)).and(qCustomerLevel.accountBookId.eq(accountBookId))).execute();
    }

    public List<CustomerLevel> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qCustomerLevel).where(qCustomerLevel.merchantId.eq(merchantId).and(qCustomerLevel.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qCustomerLevel.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qCustomerLevel.accountBookId, accountBookId);
        }

        public void setName(String name) {
            if (StrUtil.isNotBlank(name)) {
                builder.and(qCustomerLevel.name.contains(name));
            }
        }

    }

}
