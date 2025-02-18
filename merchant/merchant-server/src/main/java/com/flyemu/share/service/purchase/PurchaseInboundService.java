package com.flyemu.share.service.purchase;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.purchase.PurchaseInboundDto;
import com.flyemu.share.entity.basic.PriceRecord;
import com.flyemu.share.entity.basic.QSupplier;
import com.flyemu.share.entity.purchase.*;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.PurchaseInboundForm;
import com.flyemu.share.repository.PurchaseInboundItemRepository;
import com.flyemu.share.repository.PurchaseInboundRepository;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @功能描述: 销售出库单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PurchaseInboundService extends AbsService {

    private final static QPurchaseInbound qPurchaseInbound = QPurchaseInbound.purchaseInbound;
    private final static QSupplier qSupplier = QSupplier.supplier;
    private final static QMerchantUser qMerchantUser = QMerchantUser.merchantUser;

    private final PurchaseInboundRepository purchaseInboundRepository;
    private final PurchaseInboundItemRepository inboundItemRepository;

    private final PriceRecordService priceRecordService;
    private final CodeSeedService codeSeedService;

    public PageResults<PurchaseInboundDto> query(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qPurchaseInbound)
                .select(qPurchaseInbound, qSupplier.name, qMerchantUser.name)
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseInbound.supplierId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qPurchaseInbound.createdBy))
                .where(query.builder).orderBy(qPurchaseInbound.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<PurchaseInboundDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PurchaseInboundDto dto = BeanUtil.toBean(tuple.get(qPurchaseInbound), PurchaseInboundDto.class);
            dto.setSupplierName(tuple.get(qSupplier.name));
            dto.setCreatedName(tuple.get(qMerchantUser.name));
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public PurchaseInbound save(PurchaseInboundForm purchaseInboundForm, Long merchantId) {
        PurchaseInbound purchaseInbound = purchaseInboundForm.getPurchaseInbound();
        if (purchaseInbound.getId() != null) {
            PurchaseInbound original = purchaseInboundRepository.getById(purchaseInbound.getId());
            Assert.isFalse(original.getOrderStatus().equals(OrderStatus.已审核), "已审核订单不能更新~");
            BeanUtil.copyProperties(purchaseInbound, original, CopyOptions.create().ignoreNullValue());

            Set<Long> ids = new HashSet<>();
            for (PurchaseInboundItem d : purchaseInboundForm.getPurchaseInboundItemList()) {
                //计算基本单价
                d.setUnitPrice(BigDecimal.valueOf(NumberUtil.div(d.getSecondaryPrice(), d.getQuantity(), 2)));
//                保存更新购货商品价格
                PriceRecord priceRecord = new PriceRecord();
                priceRecord.setUnitPrice(d.getUnitPrice());
                priceRecord.setBaseUnitId(d.getBaseUnitId());
                priceRecord.setProductId(d.getProductId());
                priceRecord.setMerchantId(merchantId);
                priceRecord.setAccountBookId(purchaseInbound.getAccountBookId());
                priceRecord.setSupplierId(purchaseInbound.getSupplierId());
                priceRecordService.save(priceRecord);

                if (d.getId() != null) {
                    ids.add(d.getId());
                }
                d.setAccountBookId(purchaseInbound.getAccountBookId());
                d.setPurchaseInboundId(purchaseInbound.getId());
                d.setMerchantId(merchantId);
            }
            inboundItemRepository.saveAll(purchaseInboundForm.getPurchaseInboundItemList());
            return purchaseInboundRepository.save(original);
        } else {
            purchaseInbound.setOrderNo(codeSeedService.generateCode(merchantId, "采购入库单"));
            purchaseInbound.setOrderStatus(OrderStatus.已保存);
            purchaseInbound = purchaseInboundRepository.save(purchaseInbound);
            for (PurchaseInboundItem d : purchaseInboundForm.getPurchaseInboundItemList()) {
                //计算基本单价
                d.setUnitPrice(BigDecimal.valueOf(NumberUtil.div(d.getSecondaryPrice(), d.getQuantity(), 2)));

                //保存更新购货商品价格
                PriceRecord priceRecord = new PriceRecord();
                priceRecord.setUnitPrice(d.getUnitPrice());
                priceRecord.setBaseUnitId(d.getBaseUnitId());
                priceRecord.setProductId(d.getProductId());
                priceRecord.setMerchantId(merchantId);
                priceRecord.setSupplierId(purchaseInbound.getSupplierId());
                priceRecord.setAccountBookId(purchaseInbound.getAccountBookId());
                priceRecordService.save(priceRecord);


                d.setAccountBookId(purchaseInbound.getAccountBookId());
                d.setPurchaseInboundId(purchaseInbound.getId());
                d.setMerchantId(merchantId);
            }
            inboundItemRepository.saveAll(purchaseInboundForm.getPurchaseInboundItemList());
            return purchaseInbound;
        }
    }

    @Transactional
    public void delete(Long purchaseInboundId, Long merchantId, Long accountBookId) {
        jqf.delete(qPurchaseInbound)
                .where(qPurchaseInbound.id.eq(purchaseInboundId).and(qPurchaseInbound.merchantId.eq(merchantId)).and(qPurchaseInbound.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<PurchaseInbound> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qPurchaseInbound).where(qPurchaseInbound.merchantId.eq(merchantId).and(qPurchaseInbound.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();


        public void setState(OrderStatus state) {
            if (state != null) {
                builder.and(qPurchaseInbound.orderStatus.eq(state));
            }
        }

        public void setFilter(String filter) {
            if (StrUtil.isNotEmpty(filter)) {
                builder.and(qPurchaseInbound.orderNo.contains(filter).or(qSupplier.name.contains(filter)));
            }
        }

        public void setStart(LocalDate start) {
            if (start != null) {
                builder.and(qPurchaseInbound.inboundDate.goe(start));
            }
        }

        public void setEnd(LocalDate end) {
            if (end != null) {
                builder.and(qPurchaseInbound.inboundDate.loe(end));
            }
        }


        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qPurchaseInbound.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qPurchaseInbound.accountBookId.eq(accountBookId));
            }
        }
    }
}
