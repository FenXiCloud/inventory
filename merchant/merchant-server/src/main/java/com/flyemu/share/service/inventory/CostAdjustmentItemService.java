package com.flyemu.share.service.inventory;

import com.flyemu.share.entity.inventory.CostAdjustment;
import com.flyemu.share.entity.inventory.CostAdjustmentItem;
import com.flyemu.share.entity.inventory.QCostAdjustmentItem;
import com.flyemu.share.repository.inventory.CostAdjustmentItemRepository;
import com.flyemu.share.service.BaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CostAdjustmentItemService extends BaseService {

    private final static QCostAdjustmentItem qCostAdjustmentItem = QCostAdjustmentItem.costAdjustmentItem;

    private final CostAdjustmentItemRepository costAdjustmentItemRepository;

    public BigDecimal generateCostAdjustmentDetails(CostAdjustment costAdjustment,
                                                    List<CostAdjustmentItem> costAdjustmentItems) {
        Long costAdjustmentId = costAdjustment.getId();
        Long merchantId = costAdjustment.getMerchantId();
        Long accountBookId = costAdjustment.getAccountBookId();
        // 清除历史关联
        this.deleteByCostAdjustmentId(costAdjustmentId);
        final BigDecimal[] totalAmount = {BigDecimal.ZERO};
        costAdjustmentItems.forEach(costAdjustmentItem -> {
            Long productId = costAdjustmentItem.getProductId();
            costAdjustmentItem.setCostAdjustmentId(costAdjustmentId);
            costAdjustmentItem.setMerchantId(merchantId);
            costAdjustmentItem.setAccountBookId(accountBookId);
            costAdjustmentItem.setCreatedAt(costAdjustment.getCreatedAt());
            costAdjustmentItem.setCreatedBy(costAdjustment.getCreatedBy());
            totalAmount[0] = totalAmount[0].add(costAdjustmentItem.getAdjustmentAmount());
            costAdjustmentItem.setProductId(productId);
        });
        if (!costAdjustmentItems.isEmpty()) {
            costAdjustmentItemRepository.saveAll(costAdjustmentItems);
        }
        return totalAmount[0].setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * 根据成本调节单id删除数据
     *
     * @param costAdjustmentId 成本调节单id
     */
    public void deleteByCostAdjustmentId(Long costAdjustmentId) {
        jqf.delete(qCostAdjustmentItem).where(qCostAdjustmentItem.costAdjustmentId.eq(costAdjustmentId)).execute();
    }

    public List<CostAdjustmentItem> findByCostAdjustmentId(Long costAdjustmentId) {
        return jqf.selectFrom(qCostAdjustmentItem).where(qCostAdjustmentItem.costAdjustmentId.eq(costAdjustmentId)).fetch();
    }
}
