package com.flyemu.share.service.inventory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.entity.inventory.InventoryItem;
import com.flyemu.share.entity.inventory.QInventoryItem;
import com.flyemu.share.repository.InventoryItemRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @功能描述: 库存明细交易流水
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InventoryItemService extends AbsService {

    private final static QInventoryItem qInventoryItem = QInventoryItem.inventoryItem;

    private final InventoryItemRepository inventoryItemRepository;

    public List<InventoryItem> query(Query query) {
        List<InventoryItem> inventoryItems = bqf.selectFrom(qInventoryItem)
                .where(query.builder)
                .orderBy(qInventoryItem.id.desc())
                .fetch();
        return inventoryItems;
    }

    @Transactional
    public InventoryItem save(InventoryItem inventoryItem) {
        if (inventoryItem.getId() != null) {
            //更新
            InventoryItem original = inventoryItemRepository.getById(inventoryItem.getId());
            BeanUtil.copyProperties(inventoryItem, original, CopyOptions.create().ignoreNullValue());
            return inventoryItemRepository.save(original);
        }
        return inventoryItemRepository.save(inventoryItem);
    }

    @Transactional
    public void delete(Long supplierFlowId, Long merchantId, Long accountBookId) {
        jqf.delete(qInventoryItem)
                .where(qInventoryItem.id.eq(supplierFlowId).and(qInventoryItem.merchantId.eq(merchantId)).and(qInventoryItem.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<InventoryItem> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qInventoryItem).where(qInventoryItem.merchantId.eq(merchantId).and(qInventoryItem.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qInventoryItem.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qInventoryItem.accountBookId.eq(accountBookId));
            }
        }

    }
}
