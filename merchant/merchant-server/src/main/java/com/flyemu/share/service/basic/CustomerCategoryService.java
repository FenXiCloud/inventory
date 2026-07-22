package com.flyemu.share.service.basic;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Assert;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.entity.basic.CustomerCategory;
import com.flyemu.share.entity.basic.QCustomer;
import com.flyemu.share.entity.basic.QCustomerCategory;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.basic.CustomerCategoryRepository;
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
public class CustomerCategoryService extends BaseService {

    private final static QCustomerCategory qCustomerCategory = QCustomerCategory.customerCategory;
    private final QCustomer qCustomer = QCustomer.customer;

    private final CustomerCategoryRepository customerCategoryRepository;

    public List<CustomerCategory> query(Query query) {
        return bqf.selectFrom(qCustomerCategory)
                .where(query.builder)
                .orderBy(qCustomerCategory.id.desc())
                .fetch();
    }

    @Transactional
    public CustomerCategory save(CustomerCategory customerCategory) {
        if (customerCategory.getId() != null) {
            //更新
            CustomerCategory original = customerCategoryRepository.getById(customerCategory.getId());

            //检查重复
            long count = bqf.selectFrom(qCustomerCategory)
                    .where(qCustomerCategory.merchantId.eq(original.getMerchantId()).and(qCustomerCategory.name.eq(customerCategory.getName()))
                            .and(qCustomerCategory.id.ne(customerCategory.getId())).and(qCustomerCategory.accountBookId.eq(original.getAccountBookId())))
                    .fetchCount();
            Assert.isTrue(count == 0, customerCategory.getName() + "名称已存在~");
            BeanUtil.copyProperties(customerCategory, original, CopyOptions.create().ignoreNullValue());
            return customerCategoryRepository.save(original);
        }

        //检查重复
        long count = bqf.selectFrom(qCustomerCategory)
                .where(qCustomerCategory.merchantId.eq(customerCategory.getMerchantId()).and(qCustomerCategory.name.eq(customerCategory.getName()))
                        .and(qCustomerCategory.accountBookId.eq(customerCategory.getAccountBookId())))
                .fetchCount();
        Assert.isTrue(count == 0, customerCategory.getName() + "名称已存在~");
        return customerCategoryRepository.save(customerCategory);
    }

    /**
     * 删除
     *
     * @param customersCategoryId
     */
    @Transactional
    public void delete(Long customersCategoryId, Long merchantId, Long accountBookId) {
        long customerCount = jqf.selectFrom(qCustomer)
                .where(qCustomer.customerCategoryId.eq(customersCategoryId)
                        .and(qCustomer.merchantId.eq(merchantId))
                        .and(qCustomer.accountBookId.eq(accountBookId)))
                .fetchCount();

        if (customerCount > 0) {
            throw new ServiceException("该分类下存在客户数据，不能删除");
        }
        jqf.delete(qCustomerCategory)
                .where(qCustomerCategory.id.eq(customersCategoryId).and(qCustomerCategory.merchantId.eq(merchantId)).and(qCustomerCategory.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<CustomerCategory> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qCustomerCategory).where(qCustomerCategory.merchantId.eq(merchantId).and(qCustomerCategory.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qCustomerCategory.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qCustomerCategory.accountBookId, accountBookId);
        }
    }
}
