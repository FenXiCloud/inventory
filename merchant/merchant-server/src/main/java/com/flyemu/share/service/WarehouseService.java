package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.*;
import com.flyemu.share.repository.WarehouseRepository;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WarehouseService extends AbsService {

    private static final QWarehouse qWarehouse = QWarehouse.warehouse;
    private final WarehouseRepository warehouseRepository;

    public PageResults<Warehouse> query(Page page, Query query) {
        PagedList<Warehouse> fetchPage = bqf.selectFrom(qWarehouse).where(query.builder).orderBy(qWarehouse.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());
        return new PageResults<>(fetchPage, page);
    }


    @Transactional
    public Warehouse save(Warehouse warehouse) {
        // 如果是默认
        if (warehouse.getSystemDefault()) {
            jqf.update(qWarehouse).set(qWarehouse.systemDefault, false).where(qWarehouse.merchantId.eq(warehouse.getMerchantId()).and(qWarehouse.accountBookId.eq(warehouse.getAccountBookId()))).execute();
        }
        if (warehouse.getId() != null) {
            //更新
            Warehouse original = warehouseRepository.getById(warehouse.getId());
            BeanUtil.copyProperties(warehouse, original, CopyOptions.create().ignoreNullValue());
            return warehouseRepository.save(original);
        }
        return warehouseRepository.save(warehouse);
    }


    @Transactional
    public void delete(Long warehousesId, Long merchantId, Long accountBookId) {

        jqf.delete(qWarehouse).where(qWarehouse.merchantId.eq(merchantId).and(qWarehouse.accountBookId.eq(accountBookId)).and(qWarehouse.id.eq(warehousesId))).execute();
    }

    public List<Warehouse> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qWarehouse).where(qWarehouse.merchantId.eq(merchantId).and(qWarehouse.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qWarehouse.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qWarehouse.accountBookId.eq(accountBookId));
            }
        }

        public void setFilter(String filter) {
            if (StrUtil.isNotBlank(filter)) {
                builder.and(qWarehouse.name.contains(filter));
            }
        }
    }
}
