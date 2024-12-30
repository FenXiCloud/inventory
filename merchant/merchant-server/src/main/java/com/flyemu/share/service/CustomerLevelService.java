package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Assert;
import com.flyemu.share.entity.basic.CustomerLevel;
import com.flyemu.share.entity.basic.QCustomerLevel;
import com.flyemu.share.entity.basic.QCustomerLevelPrice;
import com.flyemu.share.repository.CustomerLevelRepository;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @功能描述: 客户级别管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerLevelService extends AbsService {

    private final static QCustomerLevel qCustomerLevel = QCustomerLevel.customerLevel;

    private final static QCustomerLevelPrice qCustomerLevelPrice = QCustomerLevelPrice.customerLevelPrice;

    private final CustomerLevelRepository customerLevelRepository;


    public List<CustomerLevel> query(Query query) {
        List<CustomerLevel> customerLevels = bqf.selectFrom(qCustomerLevel)
                .where(query.builder)
                .orderBy(qCustomerLevel.id.desc())
                .fetch();
        return customerLevels;
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


    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qCustomerLevel.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qCustomerLevel.accountBookId.eq(accountBookId));
            }
        }

        public void setFilter(String filter) {
            if (filter != null) {
                builder.and(qCustomerLevel.name.contains(filter));
            }
        }
    }


}
