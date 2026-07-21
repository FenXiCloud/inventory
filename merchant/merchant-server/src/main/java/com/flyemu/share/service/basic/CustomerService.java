package com.flyemu.share.service.basic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.CustomerDto;
import com.flyemu.share.dto.CustomerImportVo;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.fund.CustomerFlow;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.CustomerRepository;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.service.fund.CustomerFlowService;
import com.flyemu.share.service.setting.CodeRuleService;
import com.flyemu.share.way.CodeGenerator;
import com.flyemu.share.way.ProductExistenceChecker;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

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
    private final CodeRuleService codeRuleService;
    private final CustomerFlowService customerFlowService;

    public PageResults<CustomerDto> query(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qCustomer).select(qCustomer, qCustomerCategory.name, qCustomerLevel.name).leftJoin(qCustomerCategory).on(qCustomerCategory.id.eq(qCustomer.customerCategoryId)).leftJoin(qCustomerLevel).on(qCustomerLevel.id.eq(qCustomer.customerLevelId)).where(query.builder).orderBy(qCustomer.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

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
            if (original.getBalance() != null && original.getBalance().compareTo(customer.getBalance()) != 0) {
                throw new ServiceException("余额不允许修改");
            }
            if (!original.getCode().equals(customer.getCode())) {
                if (StrUtil.isEmpty(customer.getCode())) {
                    customer.setCode(original.getCode());
                }
            }
            BeanUtil.copyProperties(customer, original, CopyOptions.create().ignoreNullValue());
            return customerRepository.save(original);
        }
        if (StringUtils.isEmpty(customer.getCode())) {
            CodeRule codeRule = codeRuleService.findByDocumentTypeAndMerchantIdAndAccountBookId(CodeRule.DocumentType.客户, customer.getMerchantId(), customer.getAccountBookId());

            if (codeRule != null) {
                StringBuilder codeBuilder = new StringBuilder();
                if (StrUtil.isNotBlank(codeRule.getPrefix())) {
                    codeBuilder.append(codeRule.getPrefix());
                }
                if (StrUtil.isNotBlank(codeRule.getFormat())) {
                    String formattedDate = DateUtil.format(LocalDateTime.now(), codeRule.getFormat());
                    codeBuilder.append(formattedDate);
                }
                Integer serialLength = codeRule.getSerialNumberLength();
                if (serialLength != null && serialLength > 0) {
                    JPAQuery<Long> query = jqf.select(qCustomer.id.count()).from(qCustomer).where(qCustomer.merchantId.eq(customer.getMerchantId()).and(qCustomer.accountBookId.eq(customer.getAccountBookId())));
                    Long count = query.fetchOne();
                    Integer currentSerial = Math.toIntExact(count + 1);
                    String serialStr = String.format("%0" + serialLength + "d", currentSerial);
                    codeBuilder.append(serialStr);
                }

                customer.setCode(codeBuilder.toString());
            } else {
                customer.setCode(CodeGenerator.generateCode());
            }
        } else {
            Long count = jqf.select(qCustomer.id.count())
                    .from(qCustomer)
                    .where(qCustomer.code.eq(customer.getCode())
                            .and(qCustomer.merchantId.eq(customer.getMerchantId()))
                            .and(qCustomer.accountBookId.eq(customer.getAccountBookId())))
                    .fetchOne();

            if (count != null && count > 0) {
                throw new ServiceException("编码已存在，请重新输入！");
            }
        }
        Customer m = customerRepository.save(customer);
        CustomerFlow customerFlow = new CustomerFlow();
        customerFlow.setCustomerId(m.getId());
        customerFlow.setBalanceReceivables(customer.getBalance());
        customerFlow.setCustomerFlowType(CustomerFlow.CustomerFlowType.期初);
        customerFlow.setAccountBookId(customer.getAccountBookId());
        customerFlow.setMerchantId(customer.getMerchantId());
        customerFlow.setCreatedAt(LocalDateTime.now());
        customerFlowService.insert(customerFlow);
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
        jqf.delete(qCustomer).where(qCustomer.id.eq(customersId).and(qCustomer.merchantId.eq(merchantId)).and(qCustomer.accountBookId.eq(accountBookId))).execute();
    }

    public List<Customer> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qCustomer).where(qCustomer.merchantId.eq(merchantId).and(qCustomer.accountBookId.eq(accountBookId))).fetch();
    }

    // 导入
    @Transactional
    public void importData(List<CustomerImportVo> rows, Long merchantId, Long accountBookId) {
        for (int i = 0; i < rows.size(); i++) {
            if (StringUtils.isEmpty(rows.get(i).getName())){
                throw new ServiceException("客户名称不能为空");
            }
            if (StringUtils.isEmpty(rows.get(i).getCode())){
                throw new ServiceException("客户编码不能为空");
            }
            if (StringUtils.isEmpty(rows.get(i).getPhone())){
                throw new ServiceException("手机号不能为空");
            }
            if (StringUtils.isEmpty(rows.get(i).getContact())){
                throw new ServiceException("联系人不能为空");
            }
            if (rows.get(i).getCustomerCategoryName() == null){
                throw new ServiceException("客户分类不能为空");
            }
            if (rows.get(i).getCustomerLevelName() == null){
                throw new ServiceException("客户等级不能为空");
            }
        }
        Set<String> codeSet = new HashSet<>();
        List<String> duplicateCodes = rows.stream()
                .filter(row -> !codeSet.add(row.getCode()))
                .map(CustomerImportVo::getCode)
                .distinct()
                .toList();

        Assert.isTrue(duplicateCodes.isEmpty(), "导入数据中存在重复的客户编码：" + String.join("、", duplicateCodes));
        List<String> existingCodes = jqf.select(qCustomer.code)
                .from(qCustomer)
                .where(qCustomer.merchantId.eq(merchantId).and(qCustomer.accountBookId.eq(accountBookId)))
                .fetch();

        Set<String> existingCodeSet = new HashSet<>(existingCodes);
        List<String> duplicatedInDb = rows.stream()
                .map(CustomerImportVo::getCode)
                .filter(existingCodeSet::contains)
                .distinct()
                .toList();

        Assert.isTrue(duplicatedInDb.isEmpty(), "以下客户编码已在系统中存在，请修改后重新导入：" + String.join("、", duplicatedInDb));


        for (CustomerImportVo row : rows) {
            if (row.getCode() != null && StringUtils.isNotBlank(row.getName())
                    && StringUtils.isNotBlank(row.getCustomerLevelName()) &&
                    StringUtils.isNotBlank(row.getCustomerCategoryName())) {
                Customer customer = new Customer();
                CustomerCategory category = jqf.selectFrom(qCustomerCategory).where(qCustomerCategory.merchantId.eq(merchantId)
                        .and(qCustomerCategory.accountBookId.eq(accountBookId)).and(qCustomerCategory.name.eq(row.getCustomerCategoryName()))).fetchFirst();
                if (category == null) {
                    throw new ServiceException("客户分类不存在");
                }
                CustomerLevel level = jqf.selectFrom(qCustomerLevel).where(qCustomerLevel.merchantId.eq(merchantId)
                        .and(qCustomerLevel.accountBookId.eq(accountBookId)).and(qCustomerLevel.name.eq(row.getCustomerLevelName()))).fetchFirst();
                if (level == null) {
                    throw new ServiceException("等级不存在");
                }
                customer.setCustomerCategoryId(category.getId());
                customer.setCustomerLevelId(level.getId());
                customer.setMerchantId(merchantId);
                customer.setAccountBookId(accountBookId);
                customer.setCode(row.getCode());
                customer.setName(row.getName());
                customer.setPhone(row.getPhone());
                customer.setContact(row.getContact());
                customer.setRemarks(row.getRemarks());
                customer.setBalance(BigDecimal.ZERO);
                customerRepository.save(customer);
            }
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
        bqf.selectFrom(qCustomer).select(qCustomer, qCustomerCategory.name, qCustomerCategory.id, qCustomerLevel.name, qCustomerLevel.id).leftJoin(qCustomerCategory).on(qCustomerCategory.id.eq(qCustomer.customerCategoryId).and(qCustomerCategory.merchantId.eq(merchantId))).leftJoin(qCustomerLevel).on(qCustomerLevel.id.eq(qCustomer.customerCategoryId).and(qCustomer.merchantId.eq(merchantId))).orderBy(qCustomer.code.desc(), qCustomer.id.desc()).where(qCustomer.merchantId.eq(merchantId).and(builder)).fetch().forEach(tuple -> {
            Customer customer = BeanUtil.toBean(tuple.get(qCustomer), Customer.class);
            JSONObject jsonObject = new JSONObject();
//            jsonObject.put("分类编码", tuple.get(qCustomerCategory.));
            jsonObject.put("分类名称", tuple.get(qCustomerCategory.name) != null ? tuple.get(qCustomerCategory.name) : "");
            jsonObject.put("客户编码", customer.getCode() != null ? customer.getCode() : "");
            jsonObject.put("客户名称", customer.getName() != null ? customer.getName() : "");
            jsonObject.put("客户级别ID", tuple.get(qCustomerLevel.id) != null ? tuple.get(qCustomerLevel.id).toString() : "");
            jsonObject.put("客户级别名称", tuple.get(qCustomerLevel.name) != null ? tuple.get(qCustomerLevel.name) : "");
            jsonObject.put("应收账款", customer.getBalance() != null ? customer.getBalance().toString() : "");
            jsonObject.put("联系人", customer.getContact() != null ? customer.getContact() : "");
            jsonObject.put("电话", customer.getPhone() != null ? customer.getPhone() : "");
            jsonObject.put("备注", customer.getRemarks() != null ? customer.getRemarks() : "");
            jsonObject.put("状态", customer.getEnabled() != null ? customer.getEnabled().toString() : "");

            list.add(jsonObject);
        });
        return list;
    }

    public Customer findById(Long customerId) {
        Optional<Customer> optionalAccount = customerRepository.findById(customerId);
        if (optionalAccount.isEmpty()) {
            throw new ServiceException("客户不存在");
        }
        return optionalAccount.get();

    }

    public void updateTheBalance(Customer customer, CustomerFlow flow) {
        validateCustomerFlow(flow);
        jqf.update(qCustomer).set(qCustomer.balance, customer.getBalance()).where(qCustomer.id.eq(customer.getId())).execute();
        customerFlowService.insert(flow);
    }

    public void validateCustomerFlow(CustomerFlow flow) {
        if (flow.getBusinessId() == null) {
            throw new ServiceException("单据ID不能为空");
        }
        if (flow.getBusinessNo() == null || flow.getBusinessNo().trim().isEmpty()) {
            throw new ServiceException("单据编号不能为空");
        }
        if (flow.getCustomerFlowType() == null) {
            throw new ServiceException("操作类型不能为空");
        }
        if (flow.getBalanceReceivables() == null) {
            throw new ServiceException("应收余额不能为空");
        }
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
