package com.flyemu.share.service.inventory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.inventory.Inventory;
import com.flyemu.share.entity.inventory.QInventory;
import com.flyemu.share.repository.InventoryRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @功能描述: 库存余额表
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InventoryService extends AbsService {

    private final static QInventory qInventory = QInventory.inventory;

    private final InventoryRepository inventoryRepository;

    public PageResults<Inventory> query(Page page, Query query) {
        PagedList<Inventory> fetchPage = bqf.selectFrom(qInventory).where(query.builder).orderBy(qInventory.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<Inventory> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            Inventory inventory1 = tuple;
            Inventory inventory = BeanUtil.toBean(inventory1, Inventory.class);
            dtos.add(inventory);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public Inventory save(Inventory inventory) {
        if (inventory.getId() != null) {
            //更新
            Inventory original = inventoryRepository.getById(inventory.getId());
            BeanUtil.copyProperties(inventory, original, CopyOptions.create().ignoreNullValue());
            return inventoryRepository.save(original);
        }
        return inventoryRepository.save(inventory);
    }

    @Transactional
    public void delete(Long inventoryId, Long merchantId, Long accountBookId) {
        jqf.delete(qInventory)
                .where(qInventory.id.eq(inventoryId).and(qInventory.merchantId.eq(merchantId)).and(qInventory.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<Inventory> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qInventory).where(qInventory.merchantId.eq(merchantId).and(qInventory.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qInventory.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qInventory.accountBookId.eq(accountBookId));
            }
        }
    }
}
