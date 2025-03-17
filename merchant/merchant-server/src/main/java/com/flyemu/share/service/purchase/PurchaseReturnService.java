package com.flyemu.share.service.purchase;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.purchase.*;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.purchase.*;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.enums.PriceSource;
import com.flyemu.share.enums.PriceType;
import com.flyemu.share.form.PurchaseReturnForm;
import com.flyemu.share.repository.PurchaseReturnItemRepository;
import com.flyemu.share.repository.PurchaseReturnRepository;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.service.basic.PriceRecordService;
import com.flyemu.share.service.setting.CodeSeedService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @功能描述: 采购退货单
 * @创建时间: 2025年02月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PurchaseReturnService extends AbsService {

    private final static QPurchaseReturn qPurchaseReturn = QPurchaseReturn.purchaseReturn;
    private final static QPurchaseReturnItem qPurchaseReturnItem = QPurchaseReturnItem.purchaseReturnItem;
    private final static QSupplier qSupplier = QSupplier.supplier;
    private final static QProduct qProduct = QProduct.product;
    private final static QWarehouse qWarehouse = QWarehouse.warehouse;
    private final static QUnit qUnit = QUnit.unit;
    private final static QMerchantUser qMerchantUser = QMerchantUser.merchantUser;

    private final PurchaseReturnRepository purchaseReturnRepository;
    private final PurchaseReturnItemRepository purchaseReturnItemRepository;
    private final CodeSeedService codeSeedService;
    private final PriceRecordService priceRecordService;

    public PageResults<PurchaseReturnDto> query(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qPurchaseReturn)
                .select(qPurchaseReturn, qSupplier.name, qMerchantUser.name)
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseReturn.supplierId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qPurchaseReturn.createdBy))
                .where(query.builder).orderBy(qPurchaseReturn.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<PurchaseReturnDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PurchaseReturnDto dto = BeanUtil.toBean(tuple.get(qPurchaseReturn), PurchaseReturnDto.class);
            dto.setSupplierName(tuple.get(qSupplier.name));
            dto.setCreatedName(tuple.get(qMerchantUser.name));
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public PurchaseReturn save(PurchaseReturnForm purchaseReturnForm, Long merchantId) {
        PurchaseReturn order = purchaseReturnForm.getPurchaseReturn();
        if (order.getId() != null) {
            PurchaseReturn original = purchaseReturnRepository.getById(order.getId());
            Assert.isFalse(original.getOrderStatus().equals(OrderStatus.已审核), "已审核订单不能更新~");
            BeanUtil.copyProperties(order, original, CopyOptions.create().ignoreNullValue());

            Set<Long> ids = new HashSet<>();
            for (PurchaseReturnItem d : purchaseReturnForm.getPurchaseReturnItemList()) {
                //计算基本单价
                d.setUnitPrice(BigDecimal.valueOf(NumberUtil.div(d.getSecondaryPrice(), d.getQuantity(), 2)));

                if (d.getId() != null) {
                    ids.add(d.getId());
                }
                d.setAccountBookId(order.getAccountBookId());
                d.setPurchaseReturnId(order.getId());
                d.setMerchantId(merchantId);
                //保存更新购货商品价格
                savePrice(d, order);
            }
            purchaseReturnItemRepository.saveAll(purchaseReturnForm.getPurchaseReturnItemList());
            return purchaseReturnRepository.save(original);
        } else {
            String code = codeSeedService.generateCode(order.getMerchantId(), "采购退货单");
            Assert.notNull(code, "生成单号失败~");
            order.setOrderNo(code);
            order.setOrderStatus(OrderStatus.已保存);
            purchaseReturnRepository.save(order);
            for (PurchaseReturnItem d : purchaseReturnForm.getPurchaseReturnItemList()) {
                //计算基本单价
                d.setUnitPrice(BigDecimal.valueOf(NumberUtil.div(d.getSecondaryPrice(), d.getQuantity(), 2)));

                d.setAccountBookId(order.getAccountBookId());
                d.setPurchaseReturnId(order.getId());
                d.setMerchantId(merchantId);
                //保存更新购货商品价格
                savePrice(d, order);
            }
            purchaseReturnItemRepository.saveAll(purchaseReturnForm.getPurchaseReturnItemList());
            return order;
        }
    }

    private void savePrice(PurchaseReturnItem item, PurchaseReturn order) {
        PriceRecord priceRecord = new PriceRecord();
        priceRecord.setUnitPrice(item.getUnitPrice());
        priceRecord.setBaseUnitId(item.getBaseUnitId());
        priceRecord.setProductId(item.getProductId());
        priceRecord.setMerchantId(item.getMerchantId());
        priceRecord.setSupplierId(order.getSupplierId());
        priceRecord.setAccountBookId(order.getAccountBookId());
        priceRecord.setOrderId(order.getId());
        priceRecord.setPriceSource(PriceSource.最近采购价格);
        priceRecord.setPriceType(PriceType.最近采购价格);
        priceRecordService.savePriceRecord(priceRecord);
    }

    @Transactional
    public void delete(Long PurchaseReturnId, Long merchantId, Long accountBookId) {
        jqf.delete(qPurchaseReturn)
                .where(qPurchaseReturn.id.eq(PurchaseReturnId).and(qPurchaseReturn.merchantId.eq(merchantId)).and(qPurchaseReturn.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<PurchaseReturn> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qPurchaseReturn).where(qPurchaseReturn.merchantId.eq(merchantId).and(qPurchaseReturn.accountBookId.eq(accountBookId))).fetch();
    }

    @Transactional
    public void approved(List<Long> ids, OrderStatus state, Long adminId, Long merchantId) {
        List<PurchaseReturn> orders = bqf.selectFrom(qPurchaseReturn).where(qPurchaseReturn.merchantId.eq(merchantId).and(qPurchaseReturn.id.in(ids))).fetch();
        Assert.isFalse(CollUtil.isEmpty(orders), "未找到数据~");
        List<Long> setIds = new ArrayList<>();
        if (OrderStatus.已审核.equals(state)) {
            for (PurchaseReturn order : orders) {
                if (OrderStatus.已保存.equals(order.getOrderStatus())) {
                    setIds.add(order.getId());
                } else {
                    log.error("批量操作,状态不一致-----orderId:{},State:{}", order.getId(), order.getOrderStatus());
                }
            }
        } else if (OrderStatus.已保存.equals(state)) {
            for (PurchaseReturn order : orders) {
                if (OrderStatus.已审核.equals(order.getOrderStatus())) {
                    setIds.add(order.getId());
                } else {
                    log.error("批量操作,状态不一致-----orderId:{},State:{}", order.getId(), order.getOrderStatus());
                }
            }
        }
        if (CollUtil.isNotEmpty(setIds)) {
            jqf.update(qPurchaseReturn)
                    .set(qPurchaseReturn.orderStatus, state)
                    .set(qPurchaseReturn.approvedAt, LocalDateTime.now()).
                    set(qPurchaseReturn.approvedBy, adminId)
                    .where(qPurchaseReturn.id.in(setIds))
                    .execute();
        }
    }


    public Dict load(Long merchantId, Long orderId) {
        Tuple fetchFirst = jqf.selectFrom(qPurchaseReturn)
                .select(qPurchaseReturn, qSupplier.name)
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseReturn.supplierId))
                .where(qPurchaseReturn.merchantId.eq(merchantId).and(qPurchaseReturn.id.eq(orderId))).fetchFirst();

        QUnit qUnit1 = new QUnit("id");

        PurchaseReturnDto orderDto = BeanUtil.toBean(fetchFirst.get(qPurchaseReturn), PurchaseReturnDto.class);
        orderDto.setSupplierName(fetchFirst.get(qSupplier.name));
        ArrayList<PurchaseReturnItemDto> collect = jqf.selectFrom(qPurchaseReturnItem)
                .select(qPurchaseReturnItem, qProduct.code, qProduct.name, qWarehouse.name,
                        qProduct.imgPath, qProduct.specification, qUnit.name, qUnit1.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qPurchaseReturnItem.productId).and(qProduct.merchantId.eq(merchantId)))
                .leftJoin(qUnit).on(qUnit.id.eq(qPurchaseReturnItem.baseUnitId).and(qUnit.merchantId.eq(merchantId)))
                .leftJoin(qUnit1).on(qUnit1.id.eq(qPurchaseReturnItem.secondaryUnitId).and(qUnit1.merchantId.eq(merchantId)))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qPurchaseReturnItem.warehouseId).and(qWarehouse.merchantId.eq(merchantId)))
                .where(qPurchaseReturnItem.purchaseReturnId.eq(orderId).and(qPurchaseReturnItem.merchantId.eq(merchantId)))
                .orderBy(qPurchaseReturnItem.id.asc())
                .fetch().stream().collect(ArrayList::new, (list, tuple) -> {
                    PurchaseReturnItemDto dto = BeanUtil.toBean(tuple.get(qPurchaseReturnItem), PurchaseReturnItemDto.class);
                    dto.setProductCode(tuple.get(qProduct.code));
                    dto.setProductName(tuple.get(qProduct.name));
                    dto.setBaseUnitName(tuple.get(qUnit.name));
                    dto.setWarehouseName(tuple.get(qWarehouse.name));
                    dto.setSecondaryUnitName(tuple.get(qUnit1.name));
                    list.add(dto);
                }, List::addAll);
        return Dict.create().set("purchaseReturn", orderDto).set("purchaseReturnItemList", collect);
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setStart(LocalDate start) {
            if (start != null) {
                builder.and(qPurchaseReturn.returnDate.goe(start));
            }
        }

        public void setEnd(LocalDate end) {
            if (end != null) {
                builder.and(qPurchaseReturn.returnDate.loe(end));
            }
        }

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qPurchaseReturn.merchantId.eq(merchantId));
            }
        }
        public void setFilter(String filter) {
            if (StrUtil.isNotEmpty(filter)) {
                builder.and(qPurchaseReturn.orderNo.contains(filter).or(qSupplier.name.contains(filter)));
            }
        }
        public void setSupplierId(Long supplierId) {
            if (supplierId != null) {
                builder.and(qPurchaseReturn.supplierId.eq(supplierId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qPurchaseReturn.accountBookId.eq(accountBookId));
            }
        }
    }
}
