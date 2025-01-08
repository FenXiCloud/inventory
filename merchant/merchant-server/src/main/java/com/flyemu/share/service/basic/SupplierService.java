package com.flyemu.share.service.basic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.AuxiliaryUnitPrice;
import com.flyemu.share.dto.SelectProductDto;
import com.flyemu.share.dto.SupplierDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.SupplierRepository;
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

    private final SupplierRepository supplierRepository;


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
            return supplierRepository.save(supplier);
        } catch (Exception e) {
            log.error("supplier save", e);
            throw new ServiceException(e.getMessage());
        }
    }

    @Transactional
    public void delete(Long supplierId, Long merchantId) {
        jqf.delete(qSupplier).where(qSupplier.id.eq(supplierId).and(qSupplier.merchantId.eq(merchantId))).execute();
    }

    public List<Supplier> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qSupplier).where(qSupplier.merchantId.eq(merchantId).and(qSupplier.accountBookId.eq(accountBookId))).fetch();
    }

    public List<SelectProductDto> selectProducts(Long supplierId, Long merchantId, Long organizationId) {
        List<SelectProductDto> dtoList = bqf.selectFrom(qProduct)
                .select(qProduct.name, qProduct.code, qPriceRecord.unitPrice, qPriceRecord.unitPrice, qProduct.specification, qProduct.id, qProductCategory.path, qProduct.imgPath, qProduct.enableMultiUnit, qProduct.auxiliaryUnitPrices, qProduct.unitId, qUnit.name)
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .leftJoin(qPriceRecord).on(qPriceRecord.productId.eq(qProduct.id).and(qPriceRecord.supplierId.eq(supplierId)).and(qPriceRecord.merchantId.eq(merchantId)).and(qPriceRecord.accountBookId.eq(organizationId)))
                .where(qProduct.merchantId.eq(merchantId).and(qProduct.enabled.isTrue()).and(qProduct.accountBookId.eq(organizationId)))
                .orderBy(qProduct.sort.desc(), qProduct.id.desc())
                .fetch().stream().collect(ArrayList::new, (list, tuple) -> {
                    SelectProductDto dto = new SelectProductDto();
                    dto.setProductId(tuple.get(qProduct.id));
                    dto.setImgPath(tuple.get(qProduct.imgPath));
                    dto.setProductCode(tuple.get(qProduct.code));
                    dto.setProductName(tuple.get(qProduct.name));
                    dto.setPath(tuple.get(qProductCategory.path));
                    dto.setSpec(tuple.get(qProduct.specification));
                    dto.setUnitName(tuple.get(qUnit.name));
                    dto.setUnitId(tuple.get(qProduct.unitId));
                    dto.setPrice(tuple.get(qPriceRecord.unitPrice));
                    List<AuxiliaryUnitPrice> units = tuple.get(qProduct.auxiliaryUnitPrices);


                    if (CollUtil.isNotEmpty(units) && tuple.get(qProduct.enableMultiUnit)) {
                        units.add(0, new AuxiliaryUnitPrice(dto.getUnitId(), dto.getUnitName(), 1d, dto.getPrice()));
                        List<AuxiliaryUnitPrice> finalUnits = units;
                        if (tuple.get(qPriceRecord.auxiliaryUnitPrices) != null && CollUtil.isNotEmpty(tuple.get(qPriceRecord.auxiliaryUnitPrices))) {
                            tuple.get(qPriceRecord.auxiliaryUnitPrices).forEach(item -> {
                                finalUnits.forEach(unitPrice -> {
                                    if (item.getUnitId().equals(unitPrice.getUnitId())) {
                                        unitPrice.setUnitPrice(item.getUnitPrice());
                                    }
                                });
                            });
                        }
                        dto.setAuxiliaryUnitPrices(finalUnits);
                    }
                    dto.setTitle();
                    list.add(dto);
                }, List::addAll);

        return dtoList;
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

        public void setFilter(String filter) {
            if (StrUtil.isNotBlank(filter)) {
                builder.and(qSupplier.code.contains(filter).or(qSupplier.contact.contains(filter)).or(qSupplier.phone.contains(filter)).or(qSupplier.name.contains(filter)));
            }
        }
    }
}
