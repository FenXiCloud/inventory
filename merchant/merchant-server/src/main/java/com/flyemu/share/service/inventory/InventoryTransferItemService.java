package com.flyemu.share.service.inventory;

import com.flyemu.share.entity.inventory.InventoryTransfer;
import com.flyemu.share.entity.inventory.InventoryTransferItem;
import com.flyemu.share.entity.inventory.QInventoryTransferItem;
import com.flyemu.share.repository.inventory.InventoryTransferItemRepository;
import com.flyemu.share.service.BaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InventoryTransferItemService extends BaseService {

    private final static QInventoryTransferItem qInventoryTransferItem = QInventoryTransferItem.inventoryTransferItem;

    private final InventoryTransferItemRepository inventoryTransferItemRepository;

    /**
     * 处理调拨单明细
     *
     * @param inventoryTransfer      调拨单对象
     * @param inventoryTransferItems 明细对象
     */
    public void generateInventoryTransferDetails(InventoryTransfer inventoryTransfer,
                                                 List<InventoryTransferItem> inventoryTransferItems) {
        Long inventoryTransferId = inventoryTransfer.getId();
        Long merchantId = inventoryTransfer.getMerchantId();
        Long accountBookId = inventoryTransfer.getAccountBookId();
        // 清除历史关联
        this.deleteByInventoryTransferId(inventoryTransferId);
        inventoryTransferItems.forEach(inventoryTransferItem -> {
            Long productId = inventoryTransferItem.getProductId();
            inventoryTransferItem.setInventoryTransferId(inventoryTransferId);
            inventoryTransferItem.setAccountBookId(accountBookId);
            inventoryTransferItem.setMerchantId(merchantId);
            inventoryTransferItem.setCreatedAt(inventoryTransfer.getCreatedAt());
            inventoryTransferItem.setCreatedBy(inventoryTransfer.getCreatedBy());
            inventoryTransferItem.setProductId(productId);
        });
        if (!inventoryTransferItems.isEmpty()) {
            inventoryTransferItemRepository.saveAll(inventoryTransferItems);
        }
    }

    /**
     * 根据调拨单id删除明细数据
     *
     * @param inventoryTransferId 调拨单id
     */
    public void deleteByInventoryTransferId(Long inventoryTransferId) {
        jqf.delete(qInventoryTransferItem).where(qInventoryTransferItem.inventoryTransferId.eq(inventoryTransferId)).execute();
    }

    /**
     * 根据调拨单id获取明细数据
     *
     * @param id 调拨单id
     * @return list
     */
    public List<InventoryTransferItem> findByInventoryTransferId(Long id) {
        return jqf.selectFrom(qInventoryTransferItem).where(qInventoryTransferItem.inventoryTransferId.eq(id)).fetch();
    }
}
