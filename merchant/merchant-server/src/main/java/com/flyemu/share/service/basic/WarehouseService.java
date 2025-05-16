package com.flyemu.share.service.basic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.flyemu.share.entity.basic.QWarehouse;
import com.flyemu.share.entity.basic.Warehouse;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.WarehouseRepository;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.service.setting.CodeRuleService;
import com.flyemu.share.way.CodeGenerator;
import com.flyemu.share.way.ProductExistenceChecker;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WarehouseService extends AbsService {

    private static final QWarehouse qWarehouse = QWarehouse.warehouse;
    private final WarehouseRepository warehouseRepository;
    private final CodeRuleService codeRuleService;

    public List<Warehouse> query(Query query) {
        return bqf.selectFrom(qWarehouse).where(query.builder).where(query.builders()).orderBy(qWarehouse.id.desc()).fetch();
    }

    @Transactional
    public Warehouse save(Warehouse warehouse) {
        // 如果是默认
        if (warehouse.getSystemDefault()) {
            jqf.update(qWarehouse).set(qWarehouse.systemDefault, false)
                    .where(qWarehouse.merchantId.eq(warehouse.getMerchantId())
                            .and(qWarehouse.accountBookId.eq(warehouse.getAccountBookId())))
                    .execute();
        }

        if (warehouse.getId() != null) {
            // 更新
            Warehouse original = warehouseRepository.getById(warehouse.getId());
            BeanUtil.copyProperties(warehouse, original, CopyOptions.create().ignoreNullValue());
            return warehouseRepository.save(original);
        }

        if (io.micrometer.common.util.StringUtils.isEmpty(warehouse.getCode())) {
            // 查询系统默认编码规则（仓库）
            CodeRule codeRule = codeRuleService.findByDocumentTypeAndMerchantIdAndAccountBookId(
                    CodeRule.DocumentType.仓库,
                    warehouse.getMerchantId(),
                    warehouse.getAccountBookId()
            );

            if (codeRule != null) {
                StringBuilder codeBuilder = new StringBuilder();
                if (StrUtil.isNotBlank(codeRule.getPrefix())) {
                    codeBuilder.append(codeRule.getPrefix());
                }
                if (StrUtil.isNotBlank(codeRule.getFormat())) {
                    String formattedDate = DateUtil.format(LocalDateTime.now(), codeRule.getFormat());
                    codeBuilder.append(formattedDate);
                }
                Integer serialLength = codeRule.getSerialNumberLength();
                if (serialLength != null && serialLength > 0) {
                    JPAQuery<Long> query = jqf.select(qWarehouse.id.count())
                            .from(qWarehouse)
                            .where(
                                    qWarehouse.merchantId.eq(warehouse.getMerchantId())
                                            .and(qWarehouse.accountBookId.eq(warehouse.getAccountBookId()))
                            );
                    Long count = query.fetchOne();
                    Integer currentSerial = Math.toIntExact(count != null ? count + 1 : 1L);
                    String serialStr = String.format("%0" + serialLength + "d", currentSerial);
                    codeBuilder.append(serialStr);
                }
                warehouse.setCode(codeBuilder.toString());
            } else {
                warehouse.setCode(CodeGenerator.generateCode());
            }
        }
        Long merchantId = warehouse.getMerchantId();
        Long accountBookId = warehouse.getAccountBookId();
        Boolean existsOtherWarehouse = jqf.select(qWarehouse.id.isNotNull())
                .from(qWarehouse)
                .where(qWarehouse.merchantId.eq(merchantId)
                        .and(qWarehouse.accountBookId.eq(accountBookId)))
                .limit(1)
                .fetchFirst();

        if (!warehouse.getSystemDefault() && !existsOtherWarehouse) {
            warehouse.setSystemDefault(true);
        }

        return warehouseRepository.save(warehouse);
    }

    private final ProductExistenceChecker existenceChecker;

    @Transactional
    public void delete(Long warehousesId, Long merchantId, Long accountBookId) {
        if (existenceChecker.existsInPurchaseOrder(warehousesId, 4)) {
            throw new ServiceException("该仓库已存在采购单,不能删除");
        }
        if (existenceChecker.existsInPurchaseInbound(warehousesId, 4)) {
            throw new ServiceException("该仓库已存在采购入库单,不能删除");
        }
        if (existenceChecker.existsInPurchaseReturn(warehousesId, 4)) {
            throw new ServiceException("该仓库已存在采购退货单,不能删除");
        }
        if (existenceChecker.existsInSalesOrder(warehousesId, 4)) {
            throw new ServiceException("该仓库已存在销售单,不能删除");
        }
        if (existenceChecker.existsInSalesOutbound(warehousesId, 4)) {
            throw new ServiceException("该仓库已存在销售出库单,不能删除");
        }
        if (existenceChecker.existsInSalesReturn(warehousesId, 4)) {
            throw new ServiceException("该仓库已存在销售退货单,不能删除");
        }
        if (existenceChecker.existsInStockTake(warehousesId, 4)) {
            throw new ServiceException("该仓库已存在库存盘点单,不能删除");
        }
        if (existenceChecker.existsInOtherInbound(warehousesId, 4)) {
            throw new ServiceException("该仓库已存在其他入库单,不能删除");
        }
        if (existenceChecker.existsInOtherOutbound(warehousesId, 4)) {
            throw new ServiceException("该仓库已存在其他出库单,不能删除");
        }
        if (existenceChecker.existsInCostAdjustment(warehousesId, 4)) {
            throw new ServiceException("该仓库已存在成本调整单,不能删除");
        }
        if (existenceChecker.existsInInventoryTransfer(warehousesId, 4)) {
            throw new ServiceException("该仓库已存在库存调拨单,不能删除");
        }

        Warehouse toDelete = warehouseRepository.findById(warehousesId).orElseThrow(() -> new ServiceException("仓库不存在"));
        if (toDelete.getSystemDefault()) {
            List<Warehouse> otherWarehouses = jqf.selectFrom(qWarehouse)
                    .where(qWarehouse.merchantId.eq(merchantId)
                            .and(qWarehouse.accountBookId.eq(accountBookId))
                            .and(qWarehouse.id.ne(warehousesId)))
                    .orderBy(qWarehouse.id.desc())
                    .fetch();

            if (otherWarehouses.isEmpty()) {
                throw new ServiceException("至少保留一个仓库");
            } else {
                Warehouse nextDefault = otherWarehouses.get(0);
                nextDefault.setSystemDefault(true);
                warehouseRepository.save(nextDefault);
            }
        }

        jqf.delete(qWarehouse)
                .where(qWarehouse.merchantId.eq(merchantId)
                        .and(qWarehouse.accountBookId.eq(accountBookId))
                        .and(qWarehouse.id.eq(warehousesId)))
                .execute();
    }

    public List<Warehouse> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qWarehouse).where(qWarehouse.merchantId.eq(merchantId).and(qWarehouse.accountBookId.eq(accountBookId))).fetch();
    }

    /**
     * 根据id查询仓库信息
     *
     * @param id 仓库id
     */
    public Warehouse selectByPrimaryKey(Long id) {
        return jqf.selectFrom(qWarehouse).where(qWarehouse.id.eq(id)).fetchOne();
    }

    @Data
    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        private Long id;

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

        public BooleanBuilder builders() {
            if (id != null) {
                builder.and(qWarehouse.id.eq(id));
            }
            return builder;
        }
    }
}
