package com.flyemu.share.service.inventory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.common.TenantAware;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.basic.Product;
import com.flyemu.share.entity.basic.QProduct;
import com.flyemu.share.entity.basic.QWarehouseLocation;
import com.flyemu.share.entity.basic.WarehouseLocation;
import com.flyemu.share.entity.inventory.*;
import com.flyemu.share.entity.sales.QSalesOutbound;
import com.flyemu.share.entity.sales.QSalesOutboundItem;
import com.flyemu.share.entity.sales.SalesOutbound;
import com.flyemu.share.entity.sales.SalesOutboundItem;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.inventory.PickOrderItemRepository;
import com.flyemu.share.repository.inventory.PickOrderRepository;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.service.setting.CodeSeedService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PickOrderService extends BaseService {

    private final static QPickOrder qPickOrder = QPickOrder.pickOrder;
    private final static QPickOrderItem qPickOrderItem = QPickOrderItem.pickOrderItem;
    private final static QProduct qProduct = QProduct.product;
    private final static QWarehouseLocation qWarehouseLocation = QWarehouseLocation.warehouseLocation;
    private final static QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;
    private final static QSalesOutboundItem qSalesOutboundItem = QSalesOutboundItem.salesOutboundItem;
    private final static QInventory qInventory = QInventory.inventory;

    private final PickOrderRepository pickOrderRepository;
    private final PickOrderItemRepository pickOrderItemRepository;
    private final CodeSeedService codeSeedService;

    /**
     * 分页查询
     */
    public PageResults<PickOrder> query(Page page, Query query) {
        PagedList<PickOrder> fetchPage = bqf.selectFrom(qPickOrder)
                .where(query.builder)
                .orderBy(qPickOrder.createdAt.desc(), qPickOrder.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());
        return new PageResults<>(fetchPage, page, fetchPage.getTotalSize());
    }

    /**
     * 根据ID查询（含明细）
     */
    public PickOrder getById(Long id, Long merchantId, Long accountBookId) {
        PickOrder pickOrder = bqf.selectFrom(qPickOrder)
                .where(qPickOrder.id.eq(id)
                        .and(qPickOrder.merchantId.eq(merchantId))
                        .and(qPickOrder.accountBookId.eq(accountBookId)))
                .fetchFirst();
        if (pickOrder != null) {
            pickOrder.setItems(pickOrderItemRepository.findByPickOrderIdOrderByLocationCodeAsc(id));
        }
        return pickOrder;
    }

    /**
     * 根据销售出库单生成拣货单（按库区拆分）
     */
    @Transactional
    public List<PickOrder> generateFromSalesOutbound(Long salesOutboundId, Long merchantId, Long accountBookId, Long adminId) {
        // 获取销售出库单
        SalesOutbound outbound = bqf.selectFrom(qSalesOutbound)
                .where(qSalesOutbound.id.eq(salesOutboundId)
                        .and(qSalesOutbound.merchantId.eq(merchantId))
                        .and(qSalesOutbound.accountBookId.eq(accountBookId)))
                .fetchFirst();
        if (outbound == null) {
            throw new ServiceException("销售出库单不存在");
        }

        // 获取出库明细
        List<SalesOutboundItem> outboundItems = bqf.selectFrom(qSalesOutboundItem)
                .where(qSalesOutboundItem.salesOutboundId.eq(salesOutboundId)
                        .and(qSalesOutboundItem.merchantId.eq(merchantId)))
                .fetch();
        if (CollUtil.isEmpty(outboundItems)) {
            throw new ServiceException("出库单没有明细");
        }

        // 按商品获取货位信息
        List<Long> productIds = outboundItems.stream()
                .map(SalesOutboundItem::getProductId)
                .distinct()
                .collect(Collectors.toList());

        // 获取商品的默认货位信息
        Map<Long, Product> productMap = bqf.selectFrom(qProduct)
                .where(qProduct.id.in(productIds))
                .fetch()
                .stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 按货位类型分组
        Map<String, List<SalesOutboundItem>> groupByType = outboundItems.stream()
                .collect(Collectors.groupingBy(item -> {
                    Product product = productMap.get(item.getProductId());
                    if (product != null && product.getDefaultWholeLocationId() != null) {
                        // 如果有默认整件货位，检查是否可以整件出库
                        Integer caseQuantity = product.getCaseQuantity();
                        if (caseQuantity != null && caseQuantity > 0) {
                            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                            if (qty.compareTo(new BigDecimal(caseQuantity)) >= 0) {
                                return "WHOLE";
                            }
                        }
                    }
                    return "ZERO";
                }));

        List<PickOrder> pickOrders = new ArrayList<>();

        // 生成整货区拣货单
        if (groupByType.containsKey("WHOLE")) {
            PickOrder wholePickOrder = createPickOrder(outbound, "WHOLE", groupByType.get("WHOLE"),
                    productMap, merchantId, accountBookId, adminId);
            pickOrders.add(wholePickOrder);
        }

        // 生成零货区拣货单
        if (groupByType.containsKey("ZERO")) {
            PickOrder zeroPickOrder = createPickOrder(outbound, "ZERO", groupByType.get("ZERO"),
                    productMap, merchantId, accountBookId, adminId);
            pickOrders.add(zeroPickOrder);
        }

        return pickOrders;
    }

    /**
     * 创建拣货单
     */
    private PickOrder createPickOrder(SalesOutbound outbound, String locationType,
                                       List<SalesOutboundItem> items, Map<Long, Product> productMap,
                                       Long merchantId, Long accountBookId, Long adminId) {
        // 从第一个明细项获取仓库ID
        Long warehouseId = items.get(0).getWarehouseId();

        PickOrder pickOrder = new PickOrder();
        pickOrder.setOrderNo(codeSeedService.generateCode(merchantId, accountBookId, "拣货单"));
        pickOrder.setSourceType("销售出库");
        pickOrder.setSourceId(outbound.getId());
        pickOrder.setSourceNo(outbound.getOrderNo());
        pickOrder.setPickDate(LocalDate.now());
        pickOrder.setWarehouseId(warehouseId);
        pickOrder.setLocationType(locationType);
        pickOrder.setStatus(0);
        pickOrder.setCreatedBy(adminId);
        pickOrder.setMerchantId(merchantId);
        pickOrder.setAccountBookId(accountBookId);
        pickOrder = pickOrderRepository.save(pickOrder);

        // 生成明细
        List<PickOrderItem> pickItems = new ArrayList<>();
        for (SalesOutboundItem item : items) {
            Product product = productMap.get(item.getProductId());
            if (product == null) continue;

            PickOrderItem pickItem = new PickOrderItem();
            pickItem.setPickOrderId(pickOrder.getId());
            pickItem.setProductId(item.getProductId());
            pickItem.setProductCode(product.getCode());
            pickItem.setProductName(product.getName());
            pickItem.setSpecification(product.getSpecification());

            // 设置货位
            if ("WHOLE".equals(locationType)) {
                pickItem.setLocationId(product.getDefaultWholeLocationId());
                pickItem.setIsCase(1);
                // 计算整件数
                Integer caseQty = product.getCaseQuantity();
                if (caseQty != null && caseQty > 0) {
                    BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                    pickItem.setPlanQuantity(qty);
                    pickItem.setCaseQuantity(qty.divide(new BigDecimal(caseQty), 2, RoundingMode.HALF_UP));
                } else {
                    pickItem.setPlanQuantity(item.getQuantity());
                }
            } else {
                pickItem.setLocationId(product.getDefaultZeroLocationId());
                pickItem.setIsCase(0);
                pickItem.setPlanQuantity(item.getQuantity());
            }

            // 获取货位编码
            if (pickItem.getLocationId() != null) {
                WarehouseLocation location = bqf.selectFrom(qWarehouseLocation)
                        .where(qWarehouseLocation.id.eq(pickItem.getLocationId()))
                        .fetchFirst();
                if (location != null) {
                    pickItem.setLocationCode(location.getCode());
                }
            }

            pickItem.setMerchantId(merchantId);
            pickItem.setAccountBookId(accountBookId);
            pickItems.add(pickItem);
        }

        if (CollUtil.isNotEmpty(pickItems)) {
            pickOrderItemRepository.saveAll(pickItems);
        }

        return pickOrder;
    }

    /**
     * 更新拣货状态
     */
    @Transactional
    public void updatePickStatus(Long id, Integer status, Long pickerId, Long merchantId, Long accountBookId) {
        PickOrder pickOrder = pickOrderRepository.findById(id)
                .orElseThrow(() -> new ServiceException("拣货单不存在"));

        pickOrder.setStatus(status);
        if (status == 2) { // 已完成
            pickOrder.setPickerId(pickerId);
            pickOrder.setPickTime(LocalDateTime.now());
        }
        pickOrderRepository.save(pickOrder);
    }

    /**
     * 更新实拣数量
     */
    @Transactional
    public void updateActualQuantity(Long itemId, BigDecimal actualQuantity, Long merchantId) {
        PickOrderItem item = pickOrderItemRepository.findById(itemId)
                .orElseThrow(() -> new ServiceException("拣货明细不存在"));
        item.setActualQuantity(actualQuantity);
        pickOrderItemRepository.save(item);
    }

    /**
     * 删除拣货单
     */
    @Transactional
    public void delete(Long id, Long merchantId, Long accountBookId) {
        PickOrder pickOrder = pickOrderRepository.findById(id)
                .orElseThrow(() -> new ServiceException("拣货单不存在"));
        if (pickOrder.getStatus() != 0) {
            throw new ServiceException("只能删除待拣货状态的拣货单");
        }
        pickOrderItemRepository.deleteByPickOrderId(id);
        pickOrderRepository.delete(pickOrder);
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qPickOrder.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qPickOrder.accountBookId, accountBookId);
        }

        public void setStatus(Integer status) {
            if (status != null) {
                builder.and(qPickOrder.status.eq(status));
            }
        }

        public void setSourceType(String sourceType) {
            if (StrUtil.isNotBlank(sourceType)) {
                builder.and(qPickOrder.sourceType.eq(sourceType));
            }
        }

        public void setKeyword(String keyword) {
            if (StrUtil.isNotBlank(keyword)) {
                builder.and(qPickOrder.orderNo.contains(keyword)
                        .or(qPickOrder.sourceNo.contains(keyword)));
            }
        }
    }
}
