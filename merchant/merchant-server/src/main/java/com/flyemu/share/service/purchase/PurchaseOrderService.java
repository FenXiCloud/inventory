package com.flyemu.share.service.purchase;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.NumberUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.PurchaserOrderDto;
import com.flyemu.share.dto.purchase.PurchaseOrderItemDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.purchase.PurchaseOrder;
import com.flyemu.share.entity.purchase.PurchaseOrderItem;
import com.flyemu.share.entity.purchase.QPurchaseOrder;
import com.flyemu.share.entity.purchase.QPurchaseOrderItem;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.PurchaseOrderForm;
import com.flyemu.share.repository.PurchaseOrderItemRepository;
import com.flyemu.share.repository.PurchaseOrderRepository;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @功能描述: 销售订单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PurchaseOrderService extends AbsService {

    private final static QUnit qUnit = QUnit.unit;
    private final static QPurchaseOrder qPurchaseOrder = QPurchaseOrder.purchaseOrder;
    private final static QProduct qProduct = QProduct.product;
    private final static QWarehouse qWarehouse = QWarehouse.warehouse;
    private final static QPurchaseOrderItem qPurchaseOrderItem = QPurchaseOrderItem.purchaseOrderItem;
    private final static QSupplier qSupplier = QSupplier.supplier;
    private final static QMerchantUser qMerchantUser = QMerchantUser.merchantUser;
    private final CodeSeedService codeSeedService;
    private final PriceRecordService priceRecordService;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    public PageResults<PurchaserOrderDto> query(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qPurchaseOrder)
                .select(qPurchaseOrder, qSupplier.name, qMerchantUser.name)
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseOrder.supplierId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qPurchaseOrder.createdBy))
                .where(query.builder).orderBy(qPurchaseOrder.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<PurchaserOrderDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PurchaserOrderDto dto = BeanUtil.toBean(tuple.get(qPurchaseOrder), PurchaserOrderDto.class);
            dto.setSupplierName(tuple.get(qSupplier.name));
            dto.setCreatedName(tuple.get(qMerchantUser.name));
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public PurchaseOrder save(PurchaseOrderForm purchaseOrderForm, Long merchantId) {
        PurchaseOrder order = purchaseOrderForm.getPurchaseOrder();
        if (order.getId() != null) {
            PurchaseOrder original = purchaseOrderRepository.getById(purchaseOrderForm.getPurchaseOrder().getId());

            BeanUtil.copyProperties(order, original, CopyOptions.create().ignoreNullValue());

            Set<Long> ids = new HashSet<>();
            for (PurchaseOrderItem d : purchaseOrderForm.getPurchaseOrderItemList()) {
                //计算基本单价
                d.setUnitPrice(BigDecimal.valueOf(NumberUtil.div(d.getSecondaryPrice(),d.getQuantity(),2)));
//                保存更新购货商品价格
                PriceRecord priceRecord = new PriceRecord();
                priceRecord.setUnitPrice(d.getUnitPrice());
                priceRecord.setBaseUnitId(d.getBaseUnitId());
                priceRecord.setProductId(d.getProductId());
                priceRecord.setMerchantId(merchantId);
                priceRecord.setAccountBookId(order.getAccountBookId());
                priceRecord.setSupplierId(order.getSupplierId());
                priceRecordService.save(priceRecord);

                if (d.getId() != null) {
                    ids.add(d.getId());
                }
                d.setAccountBookId(order.getAccountBookId());
                d.setPurchaseOrderId(order.getId());
                d.setMerchantId(merchantId);
            }
            purchaseOrderItemRepository.saveAll(purchaseOrderForm.getPurchaseOrderItemList());
            return purchaseOrderRepository.save(original);
        } else {
            order.setOrderNo(codeSeedService.generateCode(purchaseOrderForm.getPurchaseOrder().getMerchantId(), "采购订单"));
            order.setOrderStatus(OrderStatus.已保存);
            purchaseOrderRepository.save(order);
            for (PurchaseOrderItem d : purchaseOrderForm.getPurchaseOrderItemList()) {
                //计算基本单价
                d.setUnitPrice(BigDecimal.valueOf(NumberUtil.div(d.getSecondaryPrice(),d.getQuantity(),2)));

//                保存更新购货商品价格
                PriceRecord priceRecord = new PriceRecord();
                priceRecord.setUnitPrice(d.getUnitPrice());
                priceRecord.setBaseUnitId(d.getBaseUnitId());
                priceRecord.setProductId(d.getProductId());
                priceRecord.setMerchantId(merchantId);
                priceRecord.setSupplierId(order.getSupplierId());
                priceRecord.setAccountBookId(order.getAccountBookId());
                priceRecordService.save(priceRecord);


                d.setAccountBookId(order.getAccountBookId());
                d.setPurchaseOrderId(order.getId());
                d.setMerchantId(merchantId);
            }
            purchaseOrderItemRepository.saveAll(purchaseOrderForm.getPurchaseOrderItemList());
            return purchaseOrderRepository.save(purchaseOrderForm.getPurchaseOrder());
        }
    }

    @Transactional
    public void delete(Long purchaseOrderId, Long merchantId, Long accountBookId) {
        jqf.delete(qPurchaseOrder)
                .where(qPurchaseOrder.id.eq(purchaseOrderId).and(qPurchaseOrder.merchantId.eq(merchantId)).and(qPurchaseOrder.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<PurchaseOrder> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qPurchaseOrder).where(qPurchaseOrder.merchantId.eq(merchantId).and(qPurchaseOrder.accountBookId.eq(accountBookId))).fetch();
    }

    public Dict load(Long merchantId, Long orderId) {
        Tuple fetchFirst = jqf.selectFrom(qPurchaseOrder)
                .select(qPurchaseOrder, qSupplier.name)
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseOrder.supplierId))
                .where(qPurchaseOrder.merchantId.eq(merchantId).and(qPurchaseOrder.id.eq(orderId))).fetchFirst();

        QUnit qUnit1 = new QUnit("id");

        PurchaserOrderDto orderDto = BeanUtil.toBean(fetchFirst.get(qPurchaseOrder), PurchaserOrderDto.class);
        ArrayList<PurchaseOrderItemDto> collect = jqf.selectFrom(qPurchaseOrderItem)
                .select(qPurchaseOrderItem, qProduct.code, qProduct.name, qWarehouse.name,
                        qProduct.imgPath, qProduct.specification, qUnit.name, qUnit1.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qPurchaseOrderItem.productId).and(qProduct.merchantId.eq(merchantId)))
                .leftJoin(qUnit).on(qUnit.id.eq(qPurchaseOrderItem.baseUnitId).and(qUnit.merchantId.eq(merchantId)))
                .leftJoin(qUnit1).on(qUnit1.id.eq(qPurchaseOrderItem.secondaryUnitId).and(qUnit1.merchantId.eq(merchantId)))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qPurchaseOrderItem.warehouseId).and(qWarehouse.merchantId.eq(merchantId)))
                .where(qPurchaseOrderItem.purchaseOrderId.eq(orderId).and(qPurchaseOrderItem.merchantId.eq(merchantId)))
                .orderBy(qPurchaseOrderItem.id.asc())
                .fetch().stream().collect(ArrayList::new, (list, tuple) -> {
                    PurchaseOrderItemDto dto = BeanUtil.toBean(tuple.get(qPurchaseOrderItem), PurchaseOrderItemDto.class);
                    dto.setProductCode(tuple.get(qProduct.code));
                    dto.setProductName(tuple.get(qProduct.name));
                    dto.setBaseUnitName(tuple.get(qUnit.name));
                    dto.setSecondaryUnitName(tuple.get(qUnit1.name));
                    list.add(dto);
                }, List::addAll);
        return Dict.create().set("purchaseOrder", orderDto).set("purchaseOrderItemList", collect);
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qPurchaseOrder.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qPurchaseOrder.accountBookId.eq(accountBookId));
            }
        }
    }
}
