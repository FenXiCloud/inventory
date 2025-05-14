package com.flyemu.share.service.basic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.CustomerDto;
import com.flyemu.share.dto.CustomerImportVo;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.CustomerRepository;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.way.CodeGenerator;
import com.flyemu.share.way.ProductExistenceChecker;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


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
        if (StringUtils.isEmpty(customer.getCode())){
            customer.setCode(CodeGenerator.generateCode(CodeGenerator.CodeType.CUSTOMER));
        }
        Customer m = customerRepository.save(customer);

        return m;
    }

    private final ProductExistenceChecker existenceChecker;
    @Transactional
    public void delete(Long customersId, Long merchantId, Long accountBookId) {

        if (existenceChecker.existsInSalesOrder(customersId, 2)) {
            throw new ServiceException("该档案已存在销售单,不能删除");
        }
        if (existenceChecker.existsInSalesOutbound(customersId, 2)) {
            throw new ServiceException("该档案已存在销售出库单,不能删除");
        }
        if (existenceChecker.existsInSalesReturn(customersId, 2)) {
            throw new ServiceException("该档案已存在销售退货单,不能删除");
        }
        if (existenceChecker.existsInOtherInbound(customersId, 2)) {
            throw new ServiceException("该档案已存在其他入库单,不能删除");
        }
        if (existenceChecker.existsInOtherOutbound(customersId, 2)) {
            throw new ServiceException("该档案已存在其他出库单,不能删除");
        }
        jqf.delete(qCustomer)
                .where(qCustomer.id.eq(customersId).and(qCustomer.merchantId.eq(merchantId)).and(qCustomer.accountBookId.eq(accountBookId))).execute();
    }


    public List<Customer> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qCustomer).where(qCustomer.merchantId.eq(merchantId).and(qCustomer.accountBookId.eq(accountBookId))).fetch();
    }

    // 导入
    @Transactional
    public void importData(List<CustomerImportVo> rows, Long merchantId) {

        //TODO: 校验客户档案字段不能为空，请补充
        Assert.isFalse(rows.stream().filter(c -> StrUtil.isEmpty(c.getName())).count() > 0, "客户名称不能为空");

        CustomerLevel level = jqf.selectFrom(qCustomerLevel).where(qCustomerLevel.merchantId.eq(merchantId)).fetchFirst();
        CustomerCategory category = jqf.selectFrom(qCustomerCategory).where(qCustomerCategory.merchantId.eq(merchantId)).fetchFirst();

        for (CustomerImportVo row : rows) {
            Customer customer = new Customer();
            customer.setCustomerCategoryId(category.getId());
            customer.setCustomerLevelId(level.getId());
            customer.setMerchantId(merchantId);
            customer.setAccountBookId(merchantId);
            customer.setCode(row.getCode());
            customer.setName(row.getName());
            customer.setPhone(row.getPhone());
            customer.setContact(row.getContact());
            customer.setRemarks(row.getRemarks());
            customerRepository.save(customer);
        }
    }

    //  导出
    public List<JSONObject> exportList(Long merchantId, Set<Long> ids, Query query) {
        BooleanBuilder builder = new BooleanBuilder();
        if (ids != null) {
            builder.and(qCustomer.id.in(ids));
        }
        if (query != null) {
            builder.and(query.builder);
        }

        List<JSONObject> list = new ArrayList<>();
        bqf.selectFrom(qCustomer)
                .select(qCustomer, qCustomerCategory.name, qCustomerCategory.id, qCustomerLevel.name, qCustomerLevel.id)
                .leftJoin(qCustomerCategory).on(qCustomerCategory.id.eq(qCustomer.customerCategoryId).and(qCustomerCategory.merchantId.eq(merchantId)))
                .leftJoin(qCustomerLevel).on(qCustomerLevel.id.eq(qCustomer.customerCategoryId).and(qCustomer.merchantId.eq(merchantId)))
                .orderBy(qCustomer.code.desc(), qCustomer.id.desc())
                .where(qCustomer.merchantId.eq(merchantId).and(builder)).fetch().forEach(tuple -> {
                    Customer customer = BeanUtil.toBean(tuple.get(qCustomer), Customer.class);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("分类编码", tuple.get(qCustomerCategory.id));
                    jsonObject.put("分类名称", tuple.get(qCustomerCategory.name));
                    jsonObject.put("客户编码", qCustomer.code);
                    jsonObject.put("客户名称", qCustomer.name);
                    jsonObject.put("客户级别ID", tuple.get(qCustomerLevel.id));
                    jsonObject.put("客户级别名称", tuple.get(qCustomerLevel.name));
                    jsonObject.put("应收账款", tuple.get(qCustomer.balance));
                    jsonObject.put("联系人", tuple.get(qCustomer.contact));
                    jsonObject.put("电话", tuple.get(qCustomer.phone));
                    jsonObject.put("备注", qCustomer.remarks);
                    jsonObject.put("状态", qCustomer.enabled);
                    list.add(jsonObject);
                });
        return list;
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


    public Customer selectByPrimaryKey(Long id) {
        return jqf.selectFrom(qCustomer).where(qCustomer.id.eq(id)).fetchOne();
    }
}
