package com.flyemu.share.service.basic;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.AuxiliaryUnitPrice;
import com.flyemu.share.dto.SelectProductDto;
import com.flyemu.share.dto.SupplierDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.fund.SupplierFlow;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.fund.SupplierFlowRepository;
import com.flyemu.share.repository.basic.SupplierRepository;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.service.fund.SupplierFlowService;
import com.flyemu.share.service.setting.CodeRuleService;
import com.flyemu.share.way.CodeGenerator;
import com.flyemu.share.way.ProductExistenceChecker;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SupplierService extends BaseService {

    private final static QSupplier qSupplier = QSupplier.supplier;

    private final static QProduct qProduct = QProduct.product;

    private final static QUnit qUnit = QUnit.unit;

    private final static QProductCategory qProductCategory = QProductCategory.productCategory;

    private final static QSupplierCategory qSupplierCategory = QSupplierCategory.supplierCategory;

    private final SupplierRepository supplierRepository;
    private final CodeRuleService codeRuleService;
    private final SupplierFlowService supplierFlowService;
    private final PriceResolveService priceResolveService;
    private final ProductExistenceChecker existenceChecker;

    public PageResults query(Page page, Query query) {
        PagedList<Tuple> pagedList = bqf.selectFrom(qSupplier).select(qSupplier, qSupplierCategory.name).leftJoin(qSupplierCategory).on(qSupplier.supplierCategoryId.eq(qSupplierCategory.id)).where(query.builder).orderBy(qSupplier.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());
        ArrayList<SupplierDto> collect = pagedList.stream().collect(ArrayList::new, (list, tuple) -> {
            SupplierDto dto = BeanUtil.toBean(tuple.get(qSupplier), SupplierDto.class);
            dto.setCategoryName(tuple.get(qSupplierCategory.name));
            list.add(dto);
        }, List::addAll);
        return new PageResults<>(collect, page, pagedList.getTotalSize());
    }
//添加
    @Transactional
    public Supplier save(Supplier supplier) {
        try {
            if (supplier.getId() != null) {
                //更新
                Supplier original = supplierRepository.getById(supplier.getId());
                if (original.getBalance()!=null&&original.getBalance().compareTo(supplier.getBalance())!=0){
                    throw new ServiceException("余额不允许修改");
                }
                BeanUtil.copyProperties(supplier, original, CopyOptions.create().ignoreNullValue());
                return supplierRepository.save(original);
            }

            if (io.micrometer.common.util.StringUtils.isEmpty(supplier.getCode())) {
                CodeRule codeRule = codeRuleService.findByDocumentTypeAndMerchantIdAndAccountBookId(
                        CodeRule.DocumentType.供货商,
                        supplier.getMerchantId(),
                        supplier.getAccountBookId()
                );

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
                        JPAQuery<Long> query = jqf.select(qSupplier.id.count())
                                .from(qSupplier)
                                .where(
                                        qSupplier.merchantId.eq(supplier.getMerchantId())
                                                .and(qSupplier.accountBookId.eq(supplier.getAccountBookId()))
                                );
                        Long count = query.fetchOne();
                        Integer currentSerial = Math.toIntExact(count != null ? count + 1 : 1L);
                        String serialStr = String.format("%0" + serialLength + "d", currentSerial);
                        codeBuilder.append(serialStr);
                    }

                    supplier.setCode(codeBuilder.toString());

                } else {
                    supplier.setCode(CodeGenerator.generateCode());
                }
            }else{
                Long count = jqf.select(qSupplier.id.count())
                        .from(qSupplier)
                        .where(qSupplier.code.eq(supplier.getCode())
                                .and(qSupplier.merchantId.eq(supplier.getMerchantId()))
                                .and(qSupplier.accountBookId.eq(supplier.getAccountBookId())))
                        .fetchOne();

                if (count != null && count > 0) {
                    throw new ServiceException("编码已存在，请重新输入！");
                }
            }
            // 档案余额由业务单据/期初流水维护，新建固定为 0
            supplier.setBalance(BigDecimal.ZERO);

            // 保存 Supplier，如果编码冲突（并发或随机码碰撞），自动重试随机码
            Supplier save = saveSupplierWithRetry(supplier);
            SupplierFlow supplierFlow=new SupplierFlow();
            supplierFlow.setSupplierId(save.getId());
            supplierFlow.setBalancePayable(BigDecimal.ZERO);
            supplierFlow.setSupplierFlowType(SupplierFlow.SupplierFlowType.期初);
            supplierFlow.setAccountBookId(supplier.getAccountBookId());
            supplierFlow.setMerchantId(supplier.getMerchantId());
            supplierFlow.setCreatedAt(LocalDateTime.now());
            supplierFlowService.insert(supplierFlow);
            return save;
        } catch (Exception e) {
            log.error("supplier save", e);
            if (e instanceof ServiceException) {
                throw (ServiceException) e;
            }
            // 提取根因，避免丢失关键错误信息（如唯一约束违反、字段不能为空等）
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            String rootMsg = root.getMessage() != null ? root.getMessage() : e.getMessage();
            throw new ServiceException(rootMsg, e);
        }
    }

    /**
     * 保存 Supplier，如果因编码唯一约束冲突则自动重试（最多 3 次）。
     * 解决并发场景下 count-based 或随机码的编码碰撞问题。
     */
    private Supplier saveSupplierWithRetry(Supplier supplier) {
        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return supplierRepository.save(supplier);
            } catch (DataIntegrityViolationException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                // 仅重试编码冲突；其他约束冲突（如 name 重复）直接抛出
                if (attempt < maxRetries - 1 && msg.contains("code")) {
                    log.warn("supplier code collision, retrying (attempt {})", attempt + 1);
                    supplier.setCode(CodeGenerator.generateCode());
                    continue;
                }
                throw e;
            }
        }
        // should never reach here
        throw new ServiceException("供应商保存失败，请重试");
    }

    @Transactional
    public void delete(Long supplierId, Long merchantId) {
        if (existenceChecker.existsInPurchaseOrder(supplierId, 3)) {
            throw new ServiceException("该档案已存在采购单,不能删除");
        }
        if (existenceChecker.existsInPurchaseInbound(supplierId, 3)) {
            throw new ServiceException("该档案已存在采购入库单,不能删除");
        }
        if (existenceChecker.existsInPurchaseReturn(supplierId, 3)) {
            throw new ServiceException("该档案已存在采购退货单,不能删除");
        }
        if (existenceChecker.existsInOtherInbound(supplierId, 3)) {
            throw new ServiceException("该档案已存在其他入库单,不能删除");
        }
        jqf.delete(qSupplier).where(qSupplier.id.eq(supplierId).and(qSupplier.merchantId.eq(merchantId))).execute();
    }

    public List<Supplier> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qSupplier).where(qSupplier.merchantId.eq(merchantId).and(qSupplier.accountBookId.eq(accountBookId))).fetch();
    }

    public List<SelectProductDto> selectProducts(Long supplierId, Long merchantId, Long accountBookId) {
        priceResolveService.ensureDefaultPolicies(merchantId, accountBookId);
        return bqf.selectFrom(qProduct)
                .select(qProduct.name, qProduct.code, qProduct.specification, qProduct.purchasePrice, qProduct.id, qProductCategory.path, qProduct.imgPath, qProduct.enableMultiUnit,
                        qProduct.auxiliaryUnitPrices, qProduct.unitId, qUnit.name, qProductCategory.name, qProduct.specification)
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
                    dto.setPrice(priceResolveService.resolvePurchasePrice(tuple.get(qProduct.id), merchantId, accountBookId));
                    List<AuxiliaryUnitPrice> units = tuple.get(qProduct.auxiliaryUnitPrices);

                    if (CollUtil.isNotEmpty(units) && tuple.get(qProduct.enableMultiUnit)) {
                        units.add(0, new AuxiliaryUnitPrice(dto.getUnitId(), dto.getUnitName(), 1d, dto.getPrice()));
                        dto.setAuxiliaryUnitPrices(units);
                    }
                    dto.setTitle();
                    list.add(dto);
                }, List::addAll);
    }

    /**
     * 根据主键获取供应商信息
     *
     * @param id 主键
     * @return supplier
     */
    public Supplier selectByPrimaryKey(Long id) {
        Supplier supplier = jqf.selectFrom(qSupplier).where(qSupplier.id.eq(id)).fetchOne();
        if (supplier ==null){
            throw new ServiceException("供应商不存在");
        }
        return supplier;
    }

    @Transactional
    public void updateTheBalance(Supplier supplier, SupplierFlow flow) {
        validateSupplierFlow(flow);
        jqf.update(qSupplier).set(qSupplier.balance, supplier.getBalance()).where(qSupplier.id.eq(supplier.getId())).execute();
        supplierFlowService.insert(flow);
    }

    public void validateSupplierFlow(SupplierFlow flow) {
        if (flow.getBusinessId() == null) {
            throw new ServiceException("单据ID不能为空");
        }
        if (flow.getBusinessNo() == null || flow.getBusinessNo().trim().isEmpty()) {
            throw new ServiceException("单据编号不能为空");
        }
        if (flow.getSupplierFlowType() == null) {
            throw new ServiceException("操作类型不能为空");
        }
        if (flow.getBalancePayable() == null) {
            throw new ServiceException("应付余额不能为空");
        }
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qSupplier.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qSupplier.accountBookId, accountBookId);
        }

        public void setSupplierCategoryId(Long supplierCategoryId) {
            if (supplierCategoryId != null) {
                builder.and(qSupplier.supplierCategoryId.eq(supplierCategoryId));
            }
        }

        public void setFilter(String filter) {
            if (StrUtil.isNotBlank(filter)) {
                builder.and(qSupplier.code.contains(filter).or(qSupplier.contact.contains(filter)).or(qSupplier.phone.contains(filter)).or(qSupplier.name.contains(filter)));
            }
        }
    }
}
