package com.flyemu.share.service.inventory;

import com.flyemu.share.entity.inventory.InventoryTransfer;
import com.flyemu.share.entity.inventory.InventoryTransferItem;
import com.flyemu.share.entity.inventory.QInventoryTransferItem;
import com.flyemu.share.repository.InventoryTransferItemRepository;
import com.flyemu.share.service.AbsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * @功能描述: 调拨单明细
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InventoryTransferItemService extends AbsService {

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
