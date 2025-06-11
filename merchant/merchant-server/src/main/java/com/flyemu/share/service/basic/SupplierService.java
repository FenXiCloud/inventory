package com.flyemu.share.service.basic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.AuxiliaryUnitPrice;
import com.flyemu.share.dto.SelectProductDto;
import com.flyemu.share.dto.SupplierDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.enums.PolicySource;
import com.flyemu.share.enums.PriceSource;
import com.flyemu.share.enums.PriceType;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.SupplierRepository;
import com.flyemu.share.service.AbsService;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.flyemu.share.enums.PolicySource.最近采购单价;
import static com.flyemu.share.enums.PolicyType.采购价格取数;

/**
 * @功能描述: 供货商管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SupplierService extends AbsService {

    private final static QSupplier qSupplier = QSupplier.supplier;

    private final static QProduct qProduct = QProduct.product;

    private final static QUnit qUnit = QUnit.unit;

    private final static QProductCategory qProductCategory = QProductCategory.productCategory;

    private final static QSupplierCategory qSupplierCategory = QSupplierCategory.supplierCategory;

    private final static QPriceRecord qPriceRecord = QPriceRecord.priceRecord;
    private final static QPricingPolicy qPricingPolicy = QPricingPolicy.pricingPolicy;

    private final SupplierRepository supplierRepository;
    private final CodeRuleService codeRuleService;


    public PageResults query(Page page, Query query) {
        PagedList<Tuple> pagedList = bqf.selectFrom(qSupplier).select(qSupplier, qSupplierCategory.name).leftJoin(qSupplierCategory).on(qSupplier.supplierCategoryId.eq(qSupplierCategory.id)).where(query.builder).orderBy(qSupplier.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());
        ArrayList<SupplierDto> collect = pagedList.stream().collect(ArrayList::new, (list, tuple) -> {
            SupplierDto dto = BeanUtil.toBean(tuple.get(qSupplier), SupplierDto.class);
            dto.setCategoryName(tuple.get(qSupplierCategory.name));
            list.add(dto);
        }, List::addAll);
        return new PageResults<>(collect, page, pagedList.getTotalSize());
    }

    @Transactional
    public Supplier save(Supplier supplier) {
        try {
            if (supplier.getId() != null) {
                //更新
                Supplier original = supplierRepository.getById(supplier.getId());
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
            }
            return supplierRepository.save(supplier);
        } catch (Exception e) {
            log.error("supplier save", e);
            throw new ServiceException(e.getMessage());
        }
    }
    private final ProductExistenceChecker existenceChecker;
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
        Boolean isPolicy = false;
        PolicySource policySource = bqf.selectFrom(qPricingPolicy)
                .select(qPricingPolicy.policySource)
                .where(qPricingPolicy.policyType.eq(采购价格取数))
                .orderBy(qPricingPolicy.priority.asc()).fetchFirst();
        if (policySource != null && (最近采购单价).equals(policySource)) {
            isPolicy = true;
        }
        Boolean finalIsPolicy = isPolicy;
        List<SelectProductDto> dtoList = bqf.selectFrom(qProduct)
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
                    dto.setPrice(tuple.get(qProduct.purchasePrice));
                    List<AuxiliaryUnitPrice> units = tuple.get(qProduct.auxiliaryUnitPrices);

                    /**
                     * 最近采购单价
                     */
                    if (finalIsPolicy) {
                        PriceRecord priceRecord = jqf.selectFrom(qPriceRecord).where(
                                qPriceRecord.productId.eq(tuple.get(qProduct.id))
                                        .and(qPriceRecord.priceSource.eq(PriceSource.最近采购价格))
                                        .and(qPriceRecord.priceType.eq(PriceType.最近采购价格))
                                        .and(qPriceRecord.accountBookId.eq(accountBookId))
                                        .and(qPriceRecord.merchantId.eq(merchantId))
                        ).orderBy(qPriceRecord.id.desc()).fetchFirst();
                        if (priceRecord != null) {
                            dto.setPrice(priceRecord.getUnitPrice());
                        }
                    }

                    if (CollUtil.isNotEmpty(units) && tuple.get(qProduct.enableMultiUnit)) {
                        units.add(0, new AuxiliaryUnitPrice(dto.getUnitId(), dto.getUnitName(), 1d, dto.getPrice()));
                        dto.setAuxiliaryUnitPrices(units);
                    }
                    dto.setTitle();
                    list.add(dto);
                }, List::addAll);

        return dtoList;
//        List<SelectProductDto> dtoList = bqf.selectFrom(qProduct)
//                .select(qProduct.name, qProduct.code, qPriceRecord.unitPrice, qPriceRecord.unitPrice, qProduct.specification, qProduct.id, qProductCategory.path, qProduct.imgPath, qProduct.enableMultiUnit, qProduct.auxiliaryUnitPrices, qProduct.unitId, qUnit.name)
//                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
//                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
//                .leftJoin(qPriceRecord).on(qPriceRecord.productId.eq(qProduct.id).and(qPriceRecord.supplierId.eq(supplierId)).and(qPriceRecord.merchantId.eq(merchantId)).and(qPriceRecord.accountBookId.eq(organizationId)))
//                .where(qProduct.merchantId.eq(merchantId).and(qProduct.enabled.isTrue()).and(qProduct.accountBookId.eq(organizationId)))
//                .orderBy(qProduct.sort.desc(), qProduct.id.desc())
//                .fetch().stream().collect(ArrayList::new, (list, tuple) -> {
//                    SelectProductDto dto = new SelectProductDto();
//                    dto.setProductId(tuple.get(qProduct.id));
//                    dto.setImgPath(tuple.get(qProduct.imgPath));
//                    dto.setProductCode(tuple.get(qProduct.code));
//                    dto.setProductName(tuple.get(qProduct.name));
//                    dto.setPath(tuple.get(qProductCategory.path));
//                    dto.setSpec(tuple.get(qProduct.specification));
//                    dto.setUnitName(tuple.get(qUnit.name));
//                    dto.setUnitId(tuple.get(qProduct.unitId));
//                    dto.setPrice(tuple.get(qPriceRecord.unitPrice));
//                    List<AuxiliaryUnitPrice> units = tuple.get(qProduct.auxiliaryUnitPrices);
//
//                    if (CollUtil.isNotEmpty(units) && tuple.get(qProduct.enableMultiUnit)) {
//                        units.add(0, new AuxiliaryUnitPrice(dto.getUnitId(), dto.getUnitName(), 1d, dto.getPrice()));
//                        List<AuxiliaryUnitPrice> finalUnits = units;
//                        if (tuple.get(qPriceRecord.auxiliaryUnitPrices) != null && CollUtil.isNotEmpty(tuple.get(qPriceRecord.auxiliaryUnitPrices))) {
//                            tuple.get(qPriceRecord.auxiliaryUnitPrices).forEach(item -> {
//                                finalUnits.forEach(unitPrice -> {
//                                    if (item.getUnitId().equals(unitPrice.getUnitId())) {
//                                        unitPrice.setUnitPrice(item.getUnitPrice());
//                                    }
//                                });
//                            });
//                        }
//                        dto.setAuxiliaryUnitPrices(finalUnits);
//                    }
//                    dto.setTitle();
//                    list.add(dto);
//                }, List::addAll);
//
//        return dtoList;
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

    public void updateTheBalance(Supplier supplier) {
        jqf.update(qSupplier).set(qSupplier.balance, supplier.getBalance()).where(qSupplier.id.eq(supplier.getId())).execute();


    }


    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSupplier.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSupplier.accountBookId.eq(accountBookId));
            }
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
