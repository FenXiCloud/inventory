package com.flyemu.share.service.inventory;

import com.flyemu.share.entity.inventory.QStockTakeItem;
import com.flyemu.share.entity.inventory.StockTake;
import com.flyemu.share.entity.inventory.StockTakeItem;
import com.flyemu.share.repository.inventory.StockTakeItemRepository;
import com.flyemu.share.service.BaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StockTakeItemService extends BaseService {

    private final static QStockTakeItem qStockTakeItem = QStockTakeItem.stockTakeItem;

    private final StockTakeItemRepository stockTakeItemRepository;

    /**
     * 处理盘点单明细
     *
     * @param stockTake      盘点单对象
     * @param stockTakeItems 盘点单明细对象
     */
    public void generateStockTakeDetails(StockTake stockTake, List<StockTakeItem> stockTakeItems) {
        Long stockTakeId = stockTake.getId();
        Long merchantId = stockTake.getMerchantId();
        Long accountBookId = stockTake.getAccountBookId();
        // 清除历史关联
        this.deleteByStockTakeId(stockTakeId);
        stockTakeItems.forEach(stockTakeItem -> {
            stockTakeItem.setStockTakeId(stockTakeId);
            stockTakeItem.setMerchantId(merchantId);
            stockTakeItem.setAccountBookId(accountBookId);
            stockTakeItem.setCreatedAt(stockTake.getCreatedAt());
        });
        if (!stockTakeItems.isEmpty()) {
            stockTakeItemRepository.saveAll(stockTakeItems);
        }
    }

    /**
     * 根据盘点单id删除明细数据
     *
     * @param stockTakeId 盘点单id
     */
    public void deleteByStockTakeId(Long stockTakeId) {
        jqf.delete(qStockTakeItem).where(qStockTakeItem.StockTakeId.eq(stockTakeId)).execute();
    }

    /**
     * 根据盘点单id获取盘点明细
     *
     * @param stockTakeId 盘点单id
     * @return list
     */
    public List<StockTakeItem> findByStockTakeId(Long stockTakeId) {
        return jqf.selectFrom(qStockTakeItem).where(qStockTakeItem.StockTakeId.eq(stockTakeId)).fetch();
    }
}
