package com.flyemu.share.service.basic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Assert;
import com.flyemu.share.entity.basic.CustomerCategory;
import com.flyemu.share.entity.basic.QCustomerCategory;
import com.flyemu.share.repository.CustomerCategoryRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @功能描述: 客户分类管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerCategoryService extends AbsService {

    private final static QCustomerCategory qCustomerCategory = QCustomerCategory.customerCategory;

    private final CustomerCategoryRepository customerCategoryRepository;


    public List<CustomerCategory> query(Query query) {
        List<CustomerCategory> customerCategories = bqf.selectFrom(qCustomerCategory)
                .where(query.builder)
                .orderBy(qCustomerCategory.id.desc())
                .fetch();
        return customerCategories;
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
        jqf.delete(qCustomerCategory)
                .where(qCustomerCategory.id.eq(customersCategoryId).and(qCustomerCategory.merchantId.eq(merchantId)).and(qCustomerCategory.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<CustomerCategory> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qCustomerCategory).where(qCustomerCategory.merchantId.eq(merchantId).and(qCustomerCategory.accountBookId.eq(accountBookId))).fetch();
    }


    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qCustomerCategory.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qCustomerCategory.accountBookId.eq(accountBookId));
            }
        }
    }
}
