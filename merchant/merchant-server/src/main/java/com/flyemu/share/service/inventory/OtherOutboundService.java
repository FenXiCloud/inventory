package com.flyemu.share.service.inventory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.lang.generator.SnowflakeGenerator;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.OtherOutboundDto;
import com.flyemu.share.entity.basic.Customer;
import com.flyemu.share.entity.inventory.*;
import com.flyemu.share.entity.setting.Admin;
import com.flyemu.share.enums.ApproveType;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.enums.OutboundType;
import com.flyemu.share.form.OtherOutboundForm;
import com.flyemu.share.repository.OtherOutboundRepository;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.service.basic.CustomerService;
import com.flyemu.share.service.setting.AdminService;
import com.querydsl.core.BooleanBuilder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @功能描述: 其他出库单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OtherOutboundService extends AbsService {

    private final static QOtherOutbound qOtherOutbound = QOtherOutbound.otherOutbound;

    private final OtherOutboundRepository otherOutboundRepository;

    private final OtherOutboundItemService otherOutboundItemService;

    private final InventoryService inventoryService;

    private final CustomerService customerService;

    private final AdminService adminService;

    public PageResults<OtherOutboundDto> query(Page page, Query query) {
        PagedList<OtherOutbound> fetchPage = bqf.selectFrom(qOtherOutbound).where(query.builder)
                .where(query.builders()).orderBy(qOtherOutbound.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<OtherOutboundDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            OtherOutboundDto dto = BeanUtil.toBean(tuple, OtherOutboundDto.class);
            // todo 获取额外参数(待优化)
            List<OtherOutboundItem> otherOutboundItems = otherOutboundItemService.findByOtherOutboundId(dto.getId());
            AtomicReference<Integer> quantity = new AtomicReference<>(0);
            if (!otherOutboundItems.isEmpty()) {
                otherOutboundItems.forEach(otherOutboundItem -> {
                    double parsed = Double.parseDouble(otherOutboundItem.getQuantity().toString());
                    quantity.updateAndGet(v -> v + (int) parsed);
                });
            }
            if (dto.getCustomerId() != null) {
                Customer customer = customerService.selectByPrimaryKey(dto.getCustomerId());
                if (customer != null) {
                    dto.setCustomerName(customer.getName());
                    dto.setCustomerCode(customer.getCode());
                }
            }
            if (dto.getCreatedBy() != null) {
                Admin admin = adminService.selectByPrimaryKey(dto.getCreatedBy());
                if (admin != null) {
                    dto.setCreatedByName(admin.getName());
                }
            }
            dto.setQuantity(quantity.get());
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public OtherOutbound save(OtherOutboundForm otherOutboundForm) {
        OtherOutbound result;
        SnowflakeGenerator snowflakeGenerator = new SnowflakeGenerator();
        OtherOutbound otherOutbound = otherOutboundForm.getOtherOutbound();
        if (otherOutbound.getId() != null) {
            //更新
            OtherOutbound original = otherOutboundRepository.getById(otherOutbound.getId());
            BeanUtil.copyProperties(otherOutbound, original, CopyOptions.create().ignoreNullValue());
            result = otherOutboundRepository.save(original);
        } else {
            otherOutbound.setCreatedAt(LocalDateTime.now());
            otherOutbound.setOrderNo(snowflakeGenerator.next().toString());
            result = otherOutboundRepository.save(otherOutbound);
        }
        // 出库明细
        BigDecimal totalAmount = otherOutboundItemService.generateOutboundDetails(result, otherOutboundForm.getOtherOutboundItems());
        result.setTotalAmount(totalAmount);
        otherOutboundRepository.save(result);
        return result;
    }

    @Transactional
    public void delete(Long otherOutboundId, Long merchantId, Long accountBookId) {
        jqf.delete(qOtherOutbound)
                .where(qOtherOutbound.id.eq(otherOutboundId).and(qOtherOutbound.merchantId.eq(merchantId)).and(qOtherOutbound.accountBookId.eq(accountBookId)))
                .execute();
    }

    @Transactional
    public void delete(Long otherOutboundId) {
        jqf.delete(qOtherOutbound).where(qOtherOutbound.id.eq(otherOutboundId)).execute();
        otherOutboundItemService.deleteByOtherOutboundId(otherOutboundId);
    }

    public List<OtherOutbound> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qOtherOutbound).where(qOtherOutbound.merchantId.eq(merchantId).and(qOtherOutbound.accountBookId.eq(accountBookId))).fetch();
    }

    @Transactional
    public void approve(Long id, ApproveType type, Long adminId) {
        OtherOutbound otherOutbound = jqf.selectFrom(qOtherOutbound).where(qOtherOutbound.id.eq(id)).fetchOne();
        List<OtherOutboundItem> otherOutboundItems = otherOutboundItemService.findByOtherOutboundId(id);
        List<Inventory> inventories = new ArrayList<>();
        List<InventoryItem> inventoryItems = new ArrayList<>();
        switch (type) {
            case AUDITS -> {
                //处理库存
                this.getComputedInventory(otherOutboundItems, inventories, inventoryItems);
                inventories.forEach(item -> {
                    // 减库存
                    inventoryService.computedInventory(item, false, id, inventoryItems);
                });
                otherOutbound.setOrderStatus(OrderStatus.已审核);
                otherOutbound.setApprovedBy(adminId);
                otherOutbound.setApprovedAt(LocalDateTime.now());
                otherOutboundRepository.save(otherOutbound);
            }
            case ANTI_AUDIT -> {
                //处理库存
                this.getComputedInventory(otherOutboundItems, inventories, inventoryItems);
                inventories.forEach(item -> {
                    // 加库存
                    inventoryService.computedInventory(item, true, id, null);
                });
                jqf.delete(qOtherOutbound).where(qOtherOutbound.id.eq(id)).execute();
                otherOutboundItemService.deleteByOtherOutboundId(id);
            }
            default -> {

            }
        }
    }

    /**
     * 统计操作的库存信息
     *
     * @param otherOutboundItems 库存明细
     * @param inventories        操作库存
     * @param inventoryItems     操作库存明细
     */
    private void getComputedInventory(List<OtherOutboundItem> otherOutboundItems,
                                      List<Inventory> inventories,
                                      List<InventoryItem> inventoryItems) {
        AtomicReference<Inventory> inventoryAtomicReference = new AtomicReference<>();
        AtomicReference<InventoryItem> inventoryItemAtomicReference = new AtomicReference<>();
        otherOutboundItems.forEach(otherOutboundItem -> {
            Double quantity = otherOutboundItem.getQuantity();
            BigDecimal subtotal = otherOutboundItem.getSubtotal();
            inventories.stream()
                    .filter(item -> item.getProductId().equals(otherOutboundItem.getProductId())
                            && item.getWarehouseId().equals(otherOutboundItem.getWarehouseId()))
                    .findFirst()
                    .ifPresentOrElse(
                            item -> {
                                Integer currentQuantity = item.getCurrentQuantity();
                                BigDecimal totalCost = item.getTotalCost();
                                BigDecimal added = totalCost.add(subtotal)
                                        .setScale(2, RoundingMode.DOWN);
                                double parsed = Double.parseDouble(quantity.toString());
                                currentQuantity += (int) parsed;
                                item.setCurrentQuantity(currentQuantity);
                                item.setTotalCost(added);
                            }, () -> {
                                Inventory inventory = new Inventory();
                                inventory.setProductId(otherOutboundItem.getProductId());
                                inventory.setWarehouseId(otherOutboundItem.getWarehouseId());
                                double parsed = Double.parseDouble(otherOutboundItem.getQuantity().toString());
                                inventory.setCurrentQuantity((int) parsed);
                                inventory.setTotalCost(otherOutboundItem.getSubtotal());
                                inventory.setMerchantId(otherOutboundItem.getMerchantId());
                                inventory.setBaseUnitId(otherOutboundItem.getBaseUnitId());
                                inventory.setAccountBookId(otherOutboundItem.getAccountBookId());
                                inventoryAtomicReference.set(inventory);
                                inventories.add(inventoryAtomicReference.get());
                            });
            InventoryItem inventoryItem = getInventoryItem(otherOutboundItem);
            inventoryItemAtomicReference.set(inventoryItem);
            inventoryItems.add(inventoryItemAtomicReference.get());
        });
    }

    /**
     * 获取库存明细列表
     *
     * @param otherOutboundItem 出库明细
     * @return inventoryItem
     */
    private InventoryItem getInventoryItem(OtherOutboundItem otherOutboundItem) {
        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setProductId(otherOutboundItem.getProductId());
        inventoryItem.setWarehouseId(otherOutboundItem.getWarehouseId());
        double parsed = Double.parseDouble(otherOutboundItem.getQuantity().toString());
        inventoryItem.setQuantity((int) parsed);
        inventoryItem.setBaseUnitId(otherOutboundItem.getBaseUnitId());
        inventoryItem.setOperationType(OperationType.出库);
        inventoryItem.setBaseUnitId(otherOutboundItem.getBaseUnitId());
        inventoryItem.setOrderId(otherOutboundItem.getOtherOutboundId());
        inventoryItem.setBatchNumber(otherOutboundItem.getBatchNumber());
        inventoryItem.setMerchantId(otherOutboundItem.getMerchantId());
        inventoryItem.setAccountBookId(otherOutboundItem.getAccountBookId());
        inventoryItem.setCreatedAt(LocalDateTime.now());
        inventoryItem.setCreatedBy(otherOutboundItem.getCreatedBy());
        return inventoryItem;
    }

    public List<Map<String, Object>> load(Long id) {
        //todo 获取出库信息待优化
        return otherOutboundRepository.findOtherOutboundById(id);
    }

    private static Date addTimeOfFinalMoment(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR, 23);
        calendar.add(Calendar.MINUTE, 59);
        calendar.add(Calendar.SECOND, 59);
        return calendar.getTime();
    }

    public List<OtherOutbound> findByStockTakeId(Long stockTakeId) {
        return jqf.selectFrom(qOtherOutbound).where(qOtherOutbound.stockTakeId.eq(stockTakeId)).fetch();
    }

    @Data
    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        private Date start;

        private Date end;

        private OutboundType outboundType;

        private OrderStatus state;

        private String filter;

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qOtherOutbound.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qOtherOutbound.accountBookId.eq(accountBookId));
            }
        }

        public BooleanBuilder builders() {
            if (start != null && end != null) {
                builder.and(qOtherOutbound.inboundDate.loe(addTimeOfFinalMoment(end)));
                builder.and(qOtherOutbound.inboundDate.goe(start));
            }
            if (outboundType != null) {
                builder.and(qOtherOutbound.outboundType.eq(outboundType));
            }
            if (state != null) {
                builder.and(qOtherOutbound.orderStatus.eq(state));
            }
            if (StrUtil.isNotBlank(filter) && StrUtil.isNotBlank(filter.trim())) {
                builder.and(qOtherOutbound.orderNo.contains(filter));
            }
            return builder;
        }
    }
}
