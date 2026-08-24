package com.flyemu.share.service.basic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.common.TenantAware;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.basic.QWarehouse;
import com.flyemu.share.entity.basic.QWarehouseLocation;
import com.flyemu.share.entity.basic.WarehouseLocation;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.basic.WarehouseLocationRepository;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WarehouseLocationService extends BaseService {

    private final static QWarehouseLocation qWarehouseLocation = QWarehouseLocation.warehouseLocation;
    private final static QWarehouse qWarehouse = QWarehouse.warehouse;

    private final WarehouseLocationRepository warehouseLocationRepository;

    /**
     * 分页查询
     */
    public PageResults<WarehouseLocation> query(Page page, Query query) {
        PagedList<WarehouseLocation> fetchPage = bqf.selectFrom(qWarehouseLocation)
                .where(query.builder)
                .orderBy(qWarehouseLocation.warehouseId.asc(), qWarehouseLocation.code.asc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());
        return new PageResults<>(fetchPage, page, fetchPage.getTotalSize());
    }

    /**
     * 列表查询（不分页）
     */
    public List<WarehouseLocation> list(Long merchantId, Long accountBookId) {
        return warehouseLocationRepository.findByMerchantIdAndAccountBookIdOrderByWarehouseIdAscCodeAsc(merchantId, accountBookId);
    }

    /**
     * 按仓库查询货位
     */
    public List<WarehouseLocation> listByWarehouse(Long merchantId, Long accountBookId, Long warehouseId) {
        return warehouseLocationRepository.findByMerchantIdAndAccountBookIdAndWarehouseIdOrderByCodeAsc(
                merchantId, accountBookId, warehouseId);
    }

    /**
     * 按类型查询货位
     */
    public List<WarehouseLocation> listByType(Long merchantId, Long accountBookId, String type) {
        return warehouseLocationRepository.findByMerchantIdAndAccountBookIdAndTypeOrderByCodeAsc(
                merchantId, accountBookId, type);
    }

    /**
     * 按仓库和类型查询货位
     */
    public List<WarehouseLocation> listByWarehouseAndType(Long merchantId, Long accountBookId, Long warehouseId, String type) {
        return warehouseLocationRepository.findByMerchantIdAndAccountBookIdAndWarehouseIdAndTypeOrderByCodeAsc(
                merchantId, accountBookId, warehouseId, type);
    }

    /**
     * 根据ID查询
     */
    public WarehouseLocation getById(Long id) {
        return warehouseLocationRepository.findById(id)
                .orElseThrow(() -> new ServiceException("货位不存在"));
    }

    /**
     * 保存/更新
     */
    @Transactional
    public WarehouseLocation save(WarehouseLocation location) {
        // 检查编码唯一性
        if (location.getId() == null) {
            // 新增
            if (warehouseLocationRepository.existsByMerchantIdAndAccountBookIdAndWarehouseIdAndCode(
                    location.getMerchantId(), location.getAccountBookId(),
                    location.getWarehouseId(), location.getCode())) {
                throw new ServiceException("货位编码已存在");
            }
        } else {
            // 更新
            WarehouseLocation original = warehouseLocationRepository.findById(location.getId())
                    .orElseThrow(() -> new ServiceException("货位不存在"));
            // 如果修改了编码，检查唯一性
            if (!original.getCode().equals(location.getCode())) {
                if (warehouseLocationRepository.existsByMerchantIdAndAccountBookIdAndWarehouseIdAndCode(
                        location.getMerchantId(), location.getAccountBookId(),
                        location.getWarehouseId(), location.getCode())) {
                    throw new ServiceException("货位编码已存在");
                }
            }
            BeanUtil.copyProperties(location, original, CopyOptions.create().ignoreNullValue());
            return warehouseLocationRepository.save(original);
        }
        return warehouseLocationRepository.save(location);
    }

    /**
     * 删除
     */
    @Transactional
    public void delete(Long id, Long merchantId, Long accountBookId) {
        WarehouseLocation location = warehouseLocationRepository.findById(id)
                .orElseThrow(() -> new ServiceException("货位不存在"));
        // TODO: 检查货位是否有库存，有库存不能删除
        warehouseLocationRepository.delete(location);
    }

    /**
     * 批量删除
     */
    @Transactional
    public void batchDelete(List<Long> ids, Long merchantId, Long accountBookId) {
        for (Long id : ids) {
            delete(id, merchantId, accountBookId);
        }
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        private Long warehouseId;
        private String type;
        private String keyword;

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qWarehouseLocation.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qWarehouseLocation.accountBookId, accountBookId);
        }

        public void setWarehouseId(Long warehouseId) {
            this.warehouseId = warehouseId;
            if (warehouseId != null) {
                builder.and(qWarehouseLocation.warehouseId.eq(warehouseId));
            }
        }

        public void setType(String type) {
            this.type = type;
            if (StrUtil.isNotBlank(type)) {
                builder.and(qWarehouseLocation.type.eq(type));
            }
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
            if (StrUtil.isNotBlank(keyword)) {
                builder.and(qWarehouseLocation.code.contains(keyword)
                        .or(qWarehouseLocation.name.contains(keyword)));
            }
        }
    }
}
