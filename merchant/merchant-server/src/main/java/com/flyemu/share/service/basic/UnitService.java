package com.flyemu.share.service.basic;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.entity.basic.QUnit;
import com.flyemu.share.entity.basic.Unit;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.basic.UnitRepository;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.way.ProductExistenceChecker;
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
public class UnitService extends BaseService {

    private final static QUnit qUnit = QUnit.unit;

    private final UnitRepository unitRepository;

    private final ProductExistenceChecker existenceChecker;

    public List<Unit> query(Query query) {
        return bqf.selectFrom(qUnit)
                .where(query.builder)
                .orderBy(qUnit.id.desc())
                .fetch();
    }

    @Transactional
    public Unit save(Unit unit) {
        try {
            BooleanBuilder builder = new BooleanBuilder();
            builder.and(qUnit.merchantId.eq(unit.getMerchantId()))
                    .and(qUnit.accountBookId.eq(unit.getAccountBookId()))
                    .and(qUnit.name.eq(unit.getName()));

            if (unit.getId() != null) {
                builder.and(qUnit.id.ne(unit.getId()));
            }
            Long count = jqf.select(qUnit.id.count())
                    .from(qUnit)
                    .where(builder)
                    .fetchOne();

            if (count != null && count > 0) {
                throw new ServiceException("已存在同名单位：" + unit.getName());
            }
            if (unit.getId() != null) {
                Unit original = unitRepository.getById(unit.getId());
                BeanUtil.copyProperties(unit, original, CopyOptions.create().ignoreNullValue());
                return unitRepository.save(original);
            }
            return unitRepository.save(unit);
        } catch (Exception e) {
            log.error("保存单位失败", e);
            throw new ServiceException(e.getMessage());
        }
    }

    @Transactional
    public void delete(Long unitsId, Long merchantId, Long accountBookId) {
        if (existenceChecker.existsInPurchaseOrder(unitsId, 5)) {
            throw new ServiceException("该单位已存在采购单,不能删除");
        }
        if (existenceChecker.existsInPurchaseInbound(unitsId, 5)) {
            throw new ServiceException("该单位已存在采购入库单,不能删除");
        }
        if (existenceChecker.existsInPurchaseReturn(unitsId, 5)) {
            throw new ServiceException("该单位已存在采购退货单,不能删除");
        }
        if (existenceChecker.existsInSalesOrder(unitsId, 5)) {
            throw new ServiceException("该单位已存在销售单,不能删除");
        }
        if (existenceChecker.existsInSalesOutbound(unitsId, 5)) {
            throw new ServiceException("该单位已存在销售出库单,不能删除");
        }
        if (existenceChecker.existsInSalesReturn(unitsId, 5)) {
            throw new ServiceException("该单位已存在销售退货单,不能删除");
        }
        if (existenceChecker.existsInOtherInbound(unitsId, 5)) {
            throw new ServiceException("该单位已存在其他入库单,不能删除");
        }
        if (existenceChecker.existsInOtherOutbound(unitsId, 5)) {
            throw new ServiceException("该单位已存在其他出库单,不能删除");
        }
        if (existenceChecker.existsInCostAdjustment(unitsId, 5)) {
            throw new ServiceException("该单位已存在成本调整单,不能删除");
        }
        if (existenceChecker.checkOutTheProduct(unitsId, 5)) {
            throw new ServiceException("该单位已存在产品,不能删除");
        }
        jqf.delete(qUnit)
                .where(qUnit.id.eq(unitsId).and(qUnit.merchantId.eq(merchantId)).and(qUnit.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<Unit> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qUnit).where(qUnit.merchantId.eq(merchantId).and(qUnit.accountBookId.eq(accountBookId))).fetch();
    }

    public Unit selectByPrimaryKey(Long id) {
        return unitRepository.getReferenceById(id);
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setName(String name) {
            if (StrUtil.isNotBlank(name)) {
                builder.and(qUnit.name.contains(name));
            }
        }

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qUnit.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qUnit.accountBookId, accountBookId);
        }
    }
}
