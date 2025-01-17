package com.flyemu.share.service.basic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.CustomerDto;
import com.flyemu.share.entity.basic.Customer;
import com.flyemu.share.entity.basic.QCustomer;
import com.flyemu.share.entity.basic.QCustomerCategory;
import com.flyemu.share.entity.basic.QCustomerLevel;
import com.flyemu.share.repository.CustomerRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


/**
 * @功能描述: 客户管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerService extends AbsService {


    private static final QCustomer qCustomer = QCustomer.customer;

    private static final QCustomerCategory qCustomerCategory = QCustomerCategory.customerCategory;

    private static final QCustomerLevel qCustomerLevel = QCustomerLevel.customerLevel;

    private final CustomerRepository customerRepository;

    public PageResults<CustomerDto> query(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qCustomer)
                .select(qCustomer, qCustomerCategory.name, qCustomerLevel.name)
                .leftJoin(qCustomerCategory).on(qCustomerCategory.id.eq(qCustomer.customerCategoryId))
                .leftJoin(qCustomerLevel).on(qCustomerLevel.id.eq(qCustomer.customerLevelId))
                .where(query.builder)
                .orderBy(qCustomer.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());

        ArrayList<CustomerDto> collect = fetchPage.stream().collect(ArrayList::new, (list, tuple) -> {
            CustomerDto dto = BeanUtil.toBean(tuple.get(qCustomer), CustomerDto.class);
            dto.setCategoryName(tuple.get(qCustomerCategory.name));
            dto.setLevelName(tuple.get(qCustomerLevel.name));
            list.add(dto);
        }, List::addAll);
        return new PageResults<>(collect, page, fetchPage.getTotalSize());
    }

    @Transactional
    public Customer save(Customer customer, String merchCode) {
        if (customer.getId() != null) {
            //更新
            Customer original = customerRepository.getById(customer.getId());
            if (!original.getCode().equals(customer.getCode())) {
                if (StrUtil.isEmpty(customer.getCode())) {
                    customer.setCode(original.getCode());
                }
            }
            BeanUtil.copyProperties(customer, original, CopyOptions.create().ignoreNullValue());
            return customerRepository.save(original);
        }
        Customer m = customerRepository.save(customer);

        return m;
    }


    @Transactional
    public void delete(Long customersId, Long merchantId, Long accountBookId) {
        jqf.delete(qCustomer)
                .where(qCustomer.id.eq(customersId).and(qCustomer.merchantId.eq(merchantId)).and(qCustomer.accountBookId.eq(accountBookId))).execute();
    }


    public List<Customer> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qCustomer).where(qCustomer.merchantId.eq(merchantId).and(qCustomer.accountBookId.eq(accountBookId))).fetch();
    }

    /**
     * 查询条件
     */
    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setName(String name) {
            if (StrUtil.isNotEmpty(name)) {
                builder.and(qCustomer.name.contains(name));
            }
        }

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qCustomer.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qCustomer.accountBookId.eq(accountBookId));
            }
        }

        public void setCustomerCategoryId(Long customerCategoryId) {
            if (customerCategoryId != null) {
                builder.and(qCustomer.customerCategoryId.eq(customerCategoryId));
            }
        }
    }

}
