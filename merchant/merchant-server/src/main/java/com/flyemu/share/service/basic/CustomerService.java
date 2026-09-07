package com.flyemu.share.service.basic;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.config.AppConfig;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.AuxiliaryUnitPrice;
import com.flyemu.share.dto.CustomerAttachment;
import com.flyemu.share.dto.CustomerDto;
import com.flyemu.share.dto.CustomerImportVo;
import com.flyemu.share.dto.SelectProductDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.fund.CustomerFlow;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.basic.CustomerRepository;
import com.flyemu.share.service.BaseService;
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

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerService extends BaseService {

    private static final QCustomer qCustomer = QCustomer.customer;

    private static final QCustomerCategory qCustomerCategory = QCustomerCategory.customerCategory;

    private static final QCustomerLevel qCustomerLevel = QCustomerLevel.customerLevel;

    private static final QProduct qProduct = QProduct.product;
    private static final QUnit qUnit = QUnit.unit;
    private static final QProductCategory qProductCategory = QProductCategory.productCategory;

    private final CustomerRepository customerRepository;
    private final CodeRuleService codeRuleService;
    private final CustomerFlowService customerFlowService;
    private final ProductExistenceChecker existenceChecker;
    private final PriceResolveService priceResolveService;
    private final AppConfig appConfig;

    public List<SelectProductDto> selectProducts(Long customerId, Long merchantId, Long accountBookId) {
        priceResolveService.ensureDefaultPolicies(merchantId, accountBookId);
        return bqf.selectFrom(qProduct)
                .select(qProduct.name, qProduct.code, qProduct.specification, qProduct.purchasePrice, qProduct.id, qProductCategory.path, qProduct.imgPath, qProduct.enableMultiUnit,
                        qProduct.auxiliaryUnitPrices, qProduct.unitId, qUnit.name, qProductCategory.name, qProduct.specification, qProduct.taxRate)
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .where(qProduct.merchantId.eq(merchantId).and(qProduct.enabled.isTrue()).and(qProduct.accountBookId.eq(accountBookId)))
                .orderBy(qProduct.sort.desc(), qProduct.id.desc())
                .fetch().stream().collect(ArrayList::new, (list, tuple) -> {
                    SelectProductDto dto = new SelectProductDto();
                    dto.setProductId(tuple.get(qProduct.id));
                    dto.setImgPath(tuple.get(qProduct.imgPath));
                    dto.setProductCode(tuple.get(qProduct.code));
                    dto.setProductName(tuple.get(qProduct.name));
                    dto.setPath(tuple.get(qProductCategory.path));
                    dto.setCategoryName(tuple.get(qProductCategory.name));
                    dto.setSpec(tuple.get(qProduct.specification));
                    dto.setUnitName(tuple.get(qUnit.name));
                    dto.setUnitId(tuple.get(qProduct.unitId));
                    dto.setTaxRate(tuple.get(qProduct.taxRate));
                    dto.setPrice(priceResolveService.resolveSalesPrice(tuple.get(qProduct.id), customerId, merchantId, accountBookId));
                    List<AuxiliaryUnitPrice> units = tuple.get(qProduct.auxiliaryUnitPrices);
                    if (CollUtil.isNotEmpty(units) && tuple.get(qProduct.enableMultiUnit)) {
                        units.add(0, new AuxiliaryUnitPrice(dto.getUnitId(), dto.getUnitName(), 1d, dto.getPrice()));
                        dto.setAuxiliaryUnitPrices(units);
                    }
                    dto.setTitle();
                    list.add(dto);
                }, List::addAll);
    }

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
            // 记录更新前的附件（用于清理被删除的文件）
            List<CustomerAttachment> oldAttachments = original.getAttachments();
            BeanUtil.copyProperties(customer, original, CopyOptions.create().ignoreNullValue());
            Customer saved = customerRepository.save(original);
            deleteRemovedAttachmentFiles(oldAttachments, original.getAttachments());
            return saved;
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
        // 档案余额由业务单据/期初流水维护，新建固定为 0
        customer.setBalance(BigDecimal.ZERO);
        Customer m = customerRepository.save(customer);
        CustomerFlow customerFlow = new CustomerFlow();
        customerFlow.setCustomerId(m.getId());
        customerFlow.setBalanceReceivables(BigDecimal.ZERO);
        customerFlow.setCustomerFlowType(CustomerFlow.CustomerFlowType.期初);
        customerFlow.setAccountBookId(customer.getAccountBookId());
        customerFlow.setMerchantId(customer.getMerchantId());
        customerFlow.setCreatedAt(LocalDateTime.now());
        customerFlowService.insert(customerFlow);
        return m;

    }

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
        Customer original = customerRepository.findById(customersId).orElse(null);
        jqf.delete(qCustomer).where(qCustomer.id.eq(customersId).and(qCustomer.merchantId.eq(merchantId)).and(qCustomer.accountBookId.eq(accountBookId))).execute();
        if (original != null) {
            // 档案删除时一并清理其附件文件
            deleteRemovedAttachmentFiles(original.getAttachments(), null);
        }
    }

    /** 清理本次变更中被移除的附件文件（newList 为 null 表示整档案删除，全部清理） */
    private void deleteRemovedAttachmentFiles(List<CustomerAttachment> oldList, List<CustomerAttachment> newList) {
        if (CollUtil.isEmpty(oldList)) {
            return;
        }
        Set<String> keep = new HashSet<>();
        if (CollUtil.isNotEmpty(newList)) {
            for (CustomerAttachment attachment : newList) {
                if (StrUtil.isNotBlank(attachment.getFilePath())) {
                    keep.add(attachment.getFilePath());
                }
            }
        }
        for (CustomerAttachment attachment : oldList) {
            if (StrUtil.isBlank(attachment.getFilePath()) || keep.contains(attachment.getFilePath())) {
                continue;
            }
            deleteAttachmentFile(attachment.getFilePath());
        }
    }

    private void deleteAttachmentFile(String filePath) {
        // 仅允许删除本地上传目录内、且形如 /attachment/xxx 的托管文件，防止越权路径
        if (StrUtil.isBlank(filePath) || !filePath.startsWith("/attachment/")) {
            return;
        }
        try {
            File file = new File(appConfig.getUploadRoot(), filePath.substring("/attachment/".length()));
            if (file.exists()) {
                Files.deleteIfExists(file.toPath());
            }
        } catch (IOException e) {
            log.error("删除客户附件文件失败: {}", filePath, e);
        }
    }

    public List<Customer> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qCustomer).where(qCustomer.merchantId.eq(merchantId).and(qCustomer.accountBookId.eq(accountBookId))).fetch();
    }

    // 导入
    @Transactional
    public void importData(List<CustomerImportVo> rows, Long merchantId, Long accountBookId) {
        for (int i = 0; i < rows.size(); i++) {
            if (StringUtils.isEmpty(rows.get(i).getName())){
                throw new ServiceException("第" + (i + 2) + "行：客户名称不能为空");
            }
            if (rows.get(i).getCustomerCategoryName() == null){
                throw new ServiceException("第" + (i + 2) + "行：客户分类不能为空");
            }
            if (rows.get(i).getCustomerLevelName() == null){
                throw new ServiceException("第" + (i + 2) + "行：客户等级不能为空");
            }
        }
        Set<String> codeSet = new HashSet<>();
        List<String> duplicateCodes = rows.stream()
                .map(CustomerImportVo::getCode)
                .filter(c -> StrUtil.isNotBlank(c) && !codeSet.add(c))
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
                .filter(c -> StrUtil.isNotBlank(c) && existingCodeSet.contains(c))
                .distinct()
                .toList();

        Assert.isTrue(duplicatedInDb.isEmpty(), "以下客户编码已在系统中存在，请修改后重新导入：" + String.join("、", duplicatedInDb));

        for (CustomerImportVo row : rows) {
            if (StringUtils.isNotBlank(row.getName())
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
                if (StrUtil.isNotBlank(row.getCode())) {
                    customer.setCode(row.getCode());
                } else {
                    CodeRule codeRule = codeRuleService.findByDocumentTypeAndMerchantIdAndAccountBookId(
                            CodeRule.DocumentType.客户, merchantId, accountBookId);
                    if (codeRule != null) {
                        StringBuilder codeBuilder = new StringBuilder();
                        if (StrUtil.isNotBlank(codeRule.getPrefix())) codeBuilder.append(codeRule.getPrefix());
                        if (StrUtil.isNotBlank(codeRule.getFormat())) codeBuilder.append(DateUtil.format(LocalDateTime.now(), codeRule.getFormat()));
                        Integer serialLength = codeRule.getSerialNumberLength();
                        if (serialLength != null && serialLength > 0) {
                            Long count = jqf.select(qCustomer.id.count()).from(qCustomer).where(qCustomer.merchantId.eq(merchantId).and(qCustomer.accountBookId.eq(accountBookId))).fetchOne();
                            codeBuilder.append(String.format("%0" + serialLength + "d", (count != null ? count : 0) + 1));
                        }
                        customer.setCode(codeBuilder.toString());
                    } else {
                        customer.setCode(CodeGenerator.generateCode());
                    }
                }
                String name = row.getName();
                if (name != null && name.length() > 32) {
                    name = name.substring(0, 32);
                }
                customer.setName(name);
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

    @Transactional
    public void updateCreditLimit(Long customerId, BigDecimal creditLimit, Long merchantId, Long accountBookId) {
        Customer customer = jqf.selectFrom(qCustomer)
                .where(qCustomer.id.eq(customerId)
                        .and(qCustomer.merchantId.eq(merchantId))
                        .and(qCustomer.accountBookId.eq(accountBookId)))
                .fetchOne();
        if (customer == null) {
            throw new ServiceException("客户不存在");
        }
        customer.setCreditLimit(creditLimit);
        customerRepository.save(customer);
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
    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setName(String name) {
            if (StrUtil.isNotEmpty(name)) {
                builder.and(qCustomer.name.contains(name));
            }
        }

        public void setTaxNo(String taxNo) {
            if (StrUtil.isNotBlank(taxNo)) {
                builder.and(qCustomer.taxNo.contains(taxNo.trim()));
            }
        }

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qCustomer.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qCustomer.accountBookId, accountBookId);
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
