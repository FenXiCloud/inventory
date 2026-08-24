package com.flyemu.share.service.inventory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.common.TenantAware;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.basic.Product;
import com.flyemu.share.entity.basic.QProduct;
import com.flyemu.share.entity.basic.QWarehouseLocation;
import com.flyemu.share.entity.basic.WarehouseLocation;
import com.flyemu.share.entity.inventory.*;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.inventory.LocationTransferRepository;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.service.setting.CheckoutService;
import com.flyemu.share.service.setting.CodeSeedService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LocationTransferService extends BaseService {

    private final static QLocationTransfer qLocationTransfer = QLocationTransfer.locationTransfer;
    private final static QProduct qProduct = QProduct.product;
    private final static QWarehouseLocation qWarehouseLocation = QWarehouseLocation.warehouseLocation;
    private final static QInventory qInventory = QInventory.inventory;

    private final LocationTransferRepository locationTransferRepository;
    private final CodeSeedService codeSeedService;
    private final CheckoutService checkoutService;
    private final InventoryService inventoryService;

    /**
     * 分页查询
     */
    public PageResults<LocationTransfer> query(Page page, Query query) {
        PagedList<LocationTransfer> fetchPage = bqf.selectFrom(qLocationTransfer)
                .where(query.builder)
                .orderBy(qLocationTransfer.createdAt.desc(), qLocationTransfer.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());
        return new PageResults<>(fetchPage, page, fetchPage.getTotalSize());
    }

    /**
     * 根据ID查询
     */
    public LocationTransfer getById(Long id, Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qLocationTransfer)
                .where(qLocationTransfer.id.eq(id)
                        .and(qLocationTransfer.merchantId.eq(merchantId))
                        .and(qLocationTransfer.accountBookId.eq(accountBookId)))
                .fetchFirst();
    }

    /**
     * 保存
     */
    @Transactional
    public LocationTransfer save(LocationTransfer transfer) {
        // 结账日期校验
        checkoutService.assertEditable(transfer.getMerchantId(), transfer.getAccountBookId(), transfer.getTransferDate());

        // 校验
        validateTransfer(transfer);

        if (transfer.getId() != null) {
            // 更新
            LocationTransfer original = locationTransferRepository.findById(transfer.getId())
                    .orElseThrow(() -> new ServiceException("调拨单不存在"));
            if (original.getOrderStatus() != OrderStatus.已保存) {
                throw new ServiceException("只能修改草稿状态的调拨单");
            }
            BeanUtil.copyProperties(transfer, original, CopyOptions.create().ignoreNullValue());
            return locationTransferRepository.save(original);
        }

        // 新增
        transfer.setOrderNo(codeSeedService.generateCode(transfer.getMerchantId(), transfer.getAccountBookId(), "货位调拨"));
        transfer.setOrderStatus(OrderStatus.已保存);
        return locationTransferRepository.save(transfer);
    }

    /**
     * 校验调拨单
     */
    private void validateTransfer(LocationTransfer transfer) {
        if (transfer.getFromWarehouseId() == null) {
            throw new ServiceException("请选择源仓库");
        }
        if (transfer.getFromLocationId() == null) {
            throw new ServiceException("请选择源货位");
        }
        if (transfer.getToWarehouseId() == null) {
            throw new ServiceException("请选择目标仓库");
        }
        if (transfer.getToLocationId() == null) {
            throw new ServiceException("请选择目标货位");
        }
        if (transfer.getProductId() == null) {
            throw new ServiceException("请选择商品");
        }
        if (transfer.getQuantity() == null || transfer.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("调拨数量必须大于0");
        }
        // 检查源货位和目标货位不能相同
        if (transfer.getFromLocationId().equals(transfer.getToLocationId())) {
            throw new ServiceException("源货位和目标货位不能相同");
        }
    }

    /**
     * 审核
     */
    @Transactional
    public void approve(Long id, Long adminId, Long merchantId, Long accountBookId) {
        LocationTransfer transfer = getById(id, merchantId, accountBookId);
        if (transfer == null) {
            throw new ServiceException("调拨单不存在");
        }
        if (transfer.getOrderStatus() != OrderStatus.已保存) {
            throw new ServiceException("只能审核草稿状态的调拨单");
        }

        // 结账日期校验
        checkoutService.assertEditable(merchantId, accountBookId, transfer.getTransferDate());

        // 检查源货位库存
        Inventory fromInventory = getInventory(transfer.getFromWarehouseId(), transfer.getFromLocationId(),
                transfer.getProductId(), merchantId, accountBookId);
        if (fromInventory == null || fromInventory.getCurrentQuantity().compareTo(transfer.getQuantity().intValue()) < 0) {
            throw new ServiceException("源货位库存不足");
        }

        // 执行库存调整
        // 减少源货位库存
        inventoryService.adjustQuantity(fromInventory, -transfer.getQuantity().intValue());

        // 增加目标货位库存
        Inventory toInventory = getInventory(transfer.getToWarehouseId(), transfer.getToLocationId(),
                transfer.getProductId(), merchantId, accountBookId);
        if (toInventory == null) {
            toInventory = new Inventory();
            toInventory.setWarehouseId(transfer.getToWarehouseId());
            toInventory.setLocationId(transfer.getToLocationId());
            toInventory.setProductId(transfer.getProductId());
            toInventory.setCurrentQuantity(0);
            toInventory.setTotalCost(BigDecimal.ZERO);
            toInventory.setAverageCost(BigDecimal.ZERO);
            toInventory.setMerchantId(merchantId);
            toInventory.setAccountBookId(accountBookId);
        }
        inventoryService.adjustQuantity(toInventory, transfer.getQuantity().intValue());

        // 更新调拨单状态
        transfer.setOrderStatus(OrderStatus.已审核);
        transfer.setApprovedBy(adminId);
        transfer.setApprovedAt(LocalDateTime.now());
        locationTransferRepository.save(transfer);

        log.info("货位调拨审核通过：单号={}, 商品={}, 数量={}", transfer.getOrderNo(), transfer.getProductId(), transfer.getQuantity());
    }

    /**
     * 获取库存
     */
    private Inventory getInventory(Long warehouseId, Long locationId, Long productId, Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qInventory)
                .where(qInventory.warehouseId.eq(warehouseId)
                        .and(qInventory.locationId.eq(locationId))
                        .and(qInventory.productId.eq(productId))
                        .and(qInventory.merchantId.eq(merchantId))
                        .and(qInventory.accountBookId.eq(accountBookId)))
                .fetchFirst();
    }

    /**
     * 删除
     */
    @Transactional
    public void delete(Long id, Long merchantId, Long accountBookId) {
        LocationTransfer transfer = getById(id, merchantId, accountBookId);
        if (transfer == null) {
            throw new ServiceException("调拨单不存在");
        }
        if (transfer.getOrderStatus() != OrderStatus.已保存) {
            throw new ServiceException("只能删除草稿状态的调拨单");
        }
        locationTransferRepository.delete(transfer);
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qLocationTransfer.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qLocationTransfer.accountBookId, accountBookId);
        }

        public void setOrderStatus(String orderStatus) {
            if (StrUtil.isNotBlank(orderStatus)) {
                builder.and(qLocationTransfer.orderStatus.eq(OrderStatus.valueOf(orderStatus)));
            }
        }

        public void setKeyword(String keyword) {
            if (StrUtil.isNotBlank(keyword)) {
                builder.and(qLocationTransfer.orderNo.contains(keyword));
            }
        }
    }
}
