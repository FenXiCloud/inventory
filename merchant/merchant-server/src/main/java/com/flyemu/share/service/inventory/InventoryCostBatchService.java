package com.flyemu.share.service.inventory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.common.TenantAware;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.InventoryCostBatchDto;
import com.flyemu.share.entity.basic.QProduct;
import com.flyemu.share.entity.basic.QSupplier;
import com.flyemu.share.entity.basic.QWarehouse;
import com.flyemu.share.entity.inventory.InventoryCostBatch;
import com.flyemu.share.entity.inventory.QInventoryCostBatch;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InventoryCostBatchService extends BaseService {

    private static final QInventoryCostBatch qBatch = QInventoryCostBatch.inventoryCostBatch;
    private static final QProduct qProduct = QProduct.product;
    private static final QWarehouse qWarehouse = QWarehouse.warehouse;
    private static final QSupplier qSupplier = QSupplier.supplier;

    public PageResults<InventoryCostBatchDto> query(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qBatch)
                .select(qBatch, qProduct.code, qProduct.name, qWarehouse.name, qSupplier.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qBatch.productId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qBatch.warehouseId))
                .leftJoin(qSupplier).on(qSupplier.id.eq(qBatch.supplierId))
                .where(query.builder)
                .orderBy(qBatch.inboundDate.desc(), qBatch.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());

        List<InventoryCostBatchDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            InventoryCostBatchDto dto = BeanUtil.toBean(tuple.get(qBatch), InventoryCostBatchDto.class);
            dto.setProductCode(tuple.get(qProduct.code));
            dto.setProductName(tuple.get(qProduct.name));
            dto.setWarehouseName(tuple.get(qWarehouse.name));
            dto.setSupplierName(tuple.get(qSupplier.name));
            dtos.add(dto);
        });
        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Data
    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qBatch.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qBatch.accountBookId, accountBookId);
        }

        public void setWarehouseId(Long warehouseId) {
            if (warehouseId != null) {
                builder.and(qBatch.warehouseId.eq(warehouseId));
            }
        }

        public void setProductId(Long productId) {
            if (productId != null) {
                builder.and(qBatch.productId.eq(productId));
            }
        }

        public void setClosed(Boolean closed) {
            if (closed != null) {
                builder.and(qBatch.closed.eq(closed));
            }
        }

        public void setInboundOrderType(OperationType inboundOrderType) {
            if (inboundOrderType != null) {
                builder.and(qBatch.inboundOrderType.eq(inboundOrderType));
            }
        }

        public void setStart(LocalDate start) {
            if (start != null) {
                builder.and(qBatch.inboundDate.goe(start));
            }
        }

        public void setEnd(LocalDate end) {
            if (end != null) {
                builder.and(qBatch.inboundDate.loe(end));
            }
        }

        public void setFilter(String filter) {
            if (StrUtil.isNotBlank(filter)) {
                String f = filter.trim();
                builder.and(qBatch.batchNo.contains(f)
                        .or(qProduct.code.contains(f))
                        .or(qProduct.name.contains(f)));
            }
        }
    }
}
