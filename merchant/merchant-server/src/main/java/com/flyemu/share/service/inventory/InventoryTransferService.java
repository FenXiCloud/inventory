package com.flyemu.share.service.inventory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.inventory.InventoryTransfer;
import com.flyemu.share.entity.inventory.QInventoryTransfer;
import com.flyemu.share.repository.InventoryTransferRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @功能描述: 调拨单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InventoryTransferService extends AbsService {

    private final static QInventoryTransfer qInventoryTransfer = QInventoryTransfer.inventoryTransfer;

    private final InventoryTransferRepository inventoryTransferRepository;

    public PageResults<InventoryTransfer> query(Page page, InventoryTransferService.Query query) {
        PagedList<InventoryTransfer> fetchPage = bqf.selectFrom(qInventoryTransfer).where(query.builder).orderBy(qInventoryTransfer.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<InventoryTransfer> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            InventoryTransfer inventoryTransfer1 = tuple;
            InventoryTransfer inventoryTransfer = BeanUtil.toBean(inventoryTransfer1, InventoryTransfer.class);
            dtos.add(inventoryTransfer);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public InventoryTransfer save(InventoryTransfer inventoryTransfer) {
        if (inventoryTransfer.getId() != null) {
            //更新
            InventoryTransfer original = inventoryTransferRepository.getById(inventoryTransfer.getId());
            BeanUtil.copyProperties(inventoryTransfer, original, CopyOptions.create().ignoreNullValue());
            return inventoryTransferRepository.save(original);
        }
        return inventoryTransferRepository.save(inventoryTransfer);
    }

    @Transactional
    public void delete(Long supplierFlowId, Long merchantId, Long accountBookId) {
        jqf.delete(qInventoryTransfer)
                .where(qInventoryTransfer.id.eq(supplierFlowId).and(qInventoryTransfer.merchantId.eq(merchantId)).and(qInventoryTransfer.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<InventoryTransfer> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qInventoryTransfer).where(qInventoryTransfer.merchantId.eq(merchantId).and(qInventoryTransfer.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qInventoryTransfer.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qInventoryTransfer.accountBookId.eq(accountBookId));
            }
        }

    }
}
