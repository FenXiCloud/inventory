package com.flyemu.share.service.scheduled;

import com.flyemu.share.entity.inventory.CostAdjustment;
import com.flyemu.share.entity.inventory.CostAdjustmentItem;
import com.flyemu.share.entity.inventory.Inventory;
import com.flyemu.share.entity.setting.AccountBook;
import com.flyemu.share.entity.setting.Merchant;
import com.flyemu.share.enums.AdjustmentType;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.repository.inventory.CostAdjustmentItemRepository;
import com.flyemu.share.repository.inventory.CostAdjustmentRepository;
import com.flyemu.share.repository.setting.MerchantRepository;
import com.flyemu.share.service.inventory.CostAdjustmentService;
import com.flyemu.share.service.inventory.InventoryService;
import com.flyemu.share.service.setting.AccountBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 尾差自动调整定时任务
 * 每天凌晨2点执行，自动处理数量为0但金额不为0的库存记录
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TailDifferenceScheduledTask {

    private final InventoryService inventoryService;
    private final CostAdjustmentService costAdjustmentService;
    private final CostAdjustmentRepository costAdjustmentRepository;
    private final CostAdjustmentItemRepository costAdjustmentItemRepository;
    private final MerchantRepository merchantRepository;
    private final AccountBookService accountBookService;

    /**
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void autoAdjustTailDifference() {
        log.info("开始执行尾差自动调整任务...");

        try {
            // 获取所有商户
            List<Merchant> merchants = merchantRepository.findAll();

            if (merchants.isEmpty()) {
                log.info("没有发现需要处理的商户");
                return;
            }

            int totalSuccess = 0;
            int totalFail = 0;

            for (Merchant merchant : merchants) {
                try {
                    // 获取商户的所有账簿
                    List<AccountBook> accountBooks = accountBookService.select(merchant.getId());

                    for (AccountBook accountBook : accountBooks) {
                        try {
                            int count = processMerchantTailDifference(merchant.getId(), accountBook.getId());
                            totalSuccess += count;
                            if (count > 0) {
                                log.info("商户 {} 账簿 {} 尾差调整成功，处理 {} 条记录",
                                    merchant.getId(), accountBook.getId(), count);
                            }
                        } catch (Exception e) {
                            totalFail++;
                            log.error("商户 {} 账簿 {} 尾差调整失败: {}",
                                merchant.getId(), accountBook.getId(), e.getMessage(), e);
                        }
                    }
                } catch (Exception e) {
                    totalFail++;
                    log.error("处理商户 {} 失败: {}", merchant.getId(), e.getMessage(), e);
                }
            }

            log.info("尾差自动调整任务完成：共处理 {} 条记录，失败 {} 次", totalSuccess, totalFail);

        } catch (Exception e) {
            log.error("尾差自动调整任务执行失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理单个商户的尾差
     * @return 处理的记录数
     */
    private int processMerchantTailDifference(Long merchantId, Long accountBookId) {
        // 查询尾差记录
        List<Map<String, Object>> tailRecords = inventoryService.tailDifference(merchantId, accountBookId);

        if (tailRecords.isEmpty()) {
            return 0;
        }

        // 创建成本调整单
        CostAdjustment costAdjustment = new CostAdjustment();
        costAdjustment.setMerchantId(merchantId);
        costAdjustment.setAccountBookId(accountBookId);
        costAdjustment.setOrderNo(generateOrderNo());
        costAdjustment.setDjustmentDate(new Date());
        costAdjustment.setAdjustmentType(AdjustmentType.出库调整);
        costAdjustment.setOrderStatus(OrderStatus.已保存);
        costAdjustment.setRemarks("系统自动尾差调整");
        costAdjustment.setCreatedAt(LocalDateTime.now());
        costAdjustment.setCreatedBy(0L); // 系统用户

        // 计算总调整金额
        BigDecimal totalAdjustmentAmount = BigDecimal.ZERO;

        // 保存调整单
        costAdjustment = costAdjustmentRepository.save(costAdjustment);

        // 创建明细
        List<CostAdjustmentItem> items = new ArrayList<>();
        for (Map<String, Object> record : tailRecords) {
            Long productId = (Long) record.get("productId");
            Long warehouseId = (Long) record.get("warehouseId");
            BigDecimal totalCost = (BigDecimal) record.get("totalCost");

            // 获取库存记录以获取baseUnitId
            Inventory inventory = inventoryService.findByWarehouseIdAndProductId(warehouseId, productId);
            if (inventory == null) {
                log.warn("库存记录不存在：productId={}, warehouseId={}", productId, warehouseId);
                continue;
            }

            CostAdjustmentItem item = new CostAdjustmentItem();
            item.setCostAdjustmentId(costAdjustment.getId());
            item.setProductId(productId);
            item.setWarehouseId(warehouseId);
            item.setBaseUnitId(inventory.getBaseUnitId());
            item.setAdjustmentAmount(totalCost.negate()); // 负数，清零
            item.setTotalCost(totalCost);
            item.setRemarks("自动调整尾差");
            item.setMerchantId(merchantId);
            item.setAccountBookId(accountBookId);
            item.setCreatedAt(LocalDateTime.now());
            item.setCreatedBy(0L);
            items.add(item);

            totalAdjustmentAmount = totalAdjustmentAmount.add(totalCost.negate());
        }

        if (items.isEmpty()) {
            return 0;
        }

        // 批量保存明细
        costAdjustmentItemRepository.saveAll(items);

        // 更新总金额
        costAdjustment.setAdjustmentAmount(totalAdjustmentAmount.setScale(2, RoundingMode.HALF_EVEN));
        costAdjustmentRepository.save(costAdjustment);

        // 自动审核
        costAdjustmentService.approved(
            Collections.singletonList(costAdjustment.getId()),
            OrderStatus.已审核,
            0L, // 系统用户
            merchantId
        );

        return tailRecords.size();
    }

    /**
     * 生成单据编号
     */
    private String generateOrderNo() {
        return "TD" + System.currentTimeMillis();
    }
}
