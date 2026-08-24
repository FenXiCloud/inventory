package com.flyemu.share.service.fund;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.common.TenantAware;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.fund.*;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.SettlementForm;
import com.flyemu.share.repository.fund.SettlementItemRepository;
import com.flyemu.share.repository.fund.SettlementRepository;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.service.setting.CheckoutService;
import com.flyemu.share.service.setting.CodeRuleService;
import com.flyemu.share.way.CodeGenerator;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class SettlementService extends BaseService {

    private final CheckoutService checkoutService;
    private final static QSettlement qSettlement = QSettlement.settlement;
    private final static QSettlementItem qSettlementItem = QSettlementItem.settlementItem;

    private final SettlementRepository settlementRepository;
    private final SettlementItemRepository settlementItemRepository;
    private final CodeRuleService codeRuleService;

    @Transactional
    public Settlement save(SettlementForm dto) {
        java.time.LocalDate checkoutDate = dto.getOrder() == null || dto.getOrder().getOrderDate() == null
                ? null : dto.getOrder().getOrderDate().toLocalDate();
        if (dto.getOrder() != null) {
            checkoutService.assertEditable(dto.getOrder().getMerchantId(), dto.getOrder().getAccountBookId(), checkoutDate);
        }
        if (dto.getOrder() == null) {
            throw new ServiceException("结算单主表信息不能为空");
        }
        if (dto.getOrder().getOrderStatus() == null) {
            throw new ServiceException("状态为空");
        }
        if (dto.getOrder().getType() == null) {
            throw new ServiceException("类型为空");
        }
        if (OrderStatus.已审核.equals(dto.getOrder().getOrderStatus())) {
            if (dto.getOrder().getApprovedBy() == null) {
                throw new ServiceException("已审核状态,审核人必填");
            }
            dto.getOrder().setApprovedAt(LocalDateTime.now());
        }

        List<SettlementItem> items = dto.getItemList();
        if (items == null || items.isEmpty()) {
            throw new ServiceException("结算明细不能为空");
        }

        Settlement settlement = dto.getOrder();

        if (settlement.getId() != null) {
            Settlement original = settlementRepository.findById(settlement.getId())
                    .orElseThrow(() -> new ServiceException("结算单不存在"));
            if (!OrderStatus.已保存.equals(original.getOrderStatus())) {
                throw new ServiceException("该单据不是【已保存】状态，无法修改");
            }
            jqf.delete(qSettlementItem)
                    .where(qSettlementItem.settlementId.eq(settlement.getId()))
                    .execute();
        }

        if (settlement.getId() == null) {
            settlement.setCreatedAt(LocalDateTime.now());
            assignOrderNumber(settlement);
        } else {
            settlement.setUpdatedAt(LocalDateTime.now());
        }

        settlement = settlementRepository.save(settlement);

        // 保存明细
        for (SettlementItem item : items) {
            item.setSettlementId(settlement.getId());
            item.setMerchantId(settlement.getMerchantId());
            item.setAccountBookId(settlement.getAccountBookId());
            settlementItemRepository.save(item);
        }

        return settlement;
    }

    private void assignOrderNumber(Settlement settlement) {
        if (StrUtil.isNotBlank(settlement.getOrderNo())) {
            return;
        }
        CodeRule codeRule = codeRuleService.findByDocumentTypeAndMerchantIdAndAccountBookId(
                CodeRule.DocumentType.结算单,
                settlement.getMerchantId(),
                settlement.getAccountBookId());

        StringBuilder codeBuilder = new StringBuilder();
        if (codeRule != null) {
            if (StrUtil.isNotBlank(codeRule.getPrefix())) {
                codeBuilder.append(codeRule.getPrefix());
            }
            if (StrUtil.isNotBlank(codeRule.getFormat())) {
                String formattedDate = DateUtil.format(new Date(), codeRule.getFormat());
                codeBuilder.append(formattedDate);
            }
            Integer serialLength = codeRule.getSerialNumberLength();
            if (serialLength != null && serialLength > 0) {
                Long maxId = jqf.select(qSettlement.id.max())
                        .from(qSettlement)
                        .where(qSettlement.merchantId.eq(settlement.getMerchantId())
                                .and(qSettlement.accountBookId.eq(settlement.getAccountBookId())))
                        .fetchOne();
                Integer currentSerial = Math.toIntExact(maxId != null ? maxId + 1 : 1L);
                String serialStr = String.format("%0" + serialLength + "d", currentSerial);
                codeBuilder.append(serialStr);
            }
        } else {
            codeBuilder.append(CodeGenerator.generateCode());
        }
        settlement.setOrderNo(codeBuilder.toString());
    }

    public PageResults<Settlement> query(Page page, Query query) {
        PagedList<Settlement> fetchPage = bqf.selectFrom(qSettlement)
                .where(query.builder)
                .orderBy(qSettlement.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());
        return new PageResults<>(fetchPage, page, fetchPage.getTotalSize());
    }

    public BigDecimal queryTotal(Query query) {
        return bqf.selectFrom(qSettlementItem)
                .select(qSettlementItem.currentVerifyAmount.sum())
                .innerJoin(qSettlement).on(qSettlement.id.eq(qSettlementItem.settlementId))
                .where(query.builder).fetchFirst();
    }

    public Map<String, Object> load(Long id, Long merchantId) {
        if (id == null) {
            throw new ServiceException("ID不能为空");
        }
        QMerchantUser qCreatedByUser = new QMerchantUser("createdByUser");
        QMerchantUser qUpdatedByUser = new QMerchantUser("updatedByUser");
        QMerchantUser qApprovedByUser = new QMerchantUser("approvedByUser");

        Settlement order = jqf.select(qSettlement).from(qSettlement)
                .where(qSettlement.id.eq(id).and(qSettlement.merchantId.eq(merchantId)))
                .fetchOne();
        if (order == null) {
            throw new ServiceException("结算单不存在");
        }
        List<SettlementItem> items = jqf.select(qSettlementItem)
                .from(qSettlementItem)
                .where(qSettlementItem.settlementId.eq(id))
                .fetch();

        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("itemList", items);
        return result;
    }

    @Transactional
    public void approved(List<Long> ids, OrderStatus state, Long adminId, Long merchantId) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要操作的数据");
        }
        List<Settlement> settlements = jqf.select(qSettlement)
                .from(qSettlement)
                .where(qSettlement.id.in(ids).and(qSettlement.merchantId.eq(merchantId)))
                .fetch();

        if (settlements.isEmpty()) {
            throw new ServiceException("没有找到可操作的结算单");
        }
        for (Settlement s : settlements) {
            if (state == OrderStatus.已审核 && !OrderStatus.已保存.equals(s.getOrderStatus())) {
                throw new ServiceException("只能审核【已保存】状态的单据：" + s.getOrderNo());
            }
            if (state == OrderStatus.已保存 && !OrderStatus.已审核.equals(s.getOrderStatus())) {
                throw new ServiceException("只能反审核【已审核】状态的单据：" + s.getOrderNo());
            }
        }
        for (Settlement s : settlements) {
            s.setOrderStatus(state);
            s.setApprovedBy(adminId.intValue());
            s.setApprovedAt(LocalDateTime.now());
            settlementRepository.save(s);
        }
    }

    @Transactional
    public void delete(Long id, Long merchantId, Long accountBookId) {
        if (id == null) {
            throw new ServiceException("请选择要删除的数据");
        }
        Settlement settlement = jqf.select(qSettlement)
                .from(qSettlement)
                .where(qSettlement.id.eq(id)
                        .and(qSettlement.merchantId.eq(merchantId))
                        .and(qSettlement.accountBookId.eq(accountBookId)))
                .fetchOne();
        if (settlement == null) {
            throw new ServiceException("没有找到可删除的结算单");
        }
        if (!OrderStatus.已保存.equals(settlement.getOrderStatus())) {
            throw new ServiceException("只能删除【已保存】状态的结算单：" + settlement.getOrderNo());
        }
        jqf.delete(qSettlementItem)
                .where(qSettlementItem.settlementId.eq(id))
                .execute();
        jqf.delete(qSettlement)
                .where(qSettlement.id.eq(id)
                        .and(qSettlement.merchantId.eq(merchantId))
                        .and(qSettlement.accountBookId.eq(accountBookId)))
                .execute();
    }

    /**
     * 入库/出库审核时自动生成结算单
     */
    @Transactional
    public Settlement createFromOrder(Long merchantId, Long accountBookId, Integer type,
                                       Long personnelId, String personnelName,
                                       String businessNo, BigDecimal documentAmount,
                                       Long businessId, String businessType) {
        // 防重复：同一订单已存在结算单则跳过
        Long exists = jqf.select(qSettlementItem.id.count())
                .from(qSettlementItem)
                .innerJoin(qSettlement).on(qSettlement.id.eq(qSettlementItem.settlementId))
                .where(qSettlementItem.businessId.eq(businessId)
                        .and(qSettlementItem.businessCategory.eq("INVENTORY"))
                        .and(qSettlementItem.businessType.eq(businessType))
                        .and(qSettlement.merchantId.eq(merchantId)))
                .fetchOne();
        if (exists != null && exists > 0) {
            return null;
        }
        Settlement settlement = new Settlement();
        settlement.setMerchantId(merchantId);
        settlement.setAccountBookId(accountBookId);
        settlement.setType(type);
        settlement.setPersonnelId(personnelId);
        settlement.setPersonnelName(personnelName);
        settlement.setOrderDate(LocalDateTime.now());
        settlement.setOrderStatus(OrderStatus.未平账);
        settlement.setCreatedAt(LocalDateTime.now());
        settlement.setApprovedBy(-1);
        settlement.setApprovedAt(LocalDateTime.now());
        assignOrderNumber(settlement);
        settlement = settlementRepository.save(settlement);

        SettlementItem item = new SettlementItem();
        item.setSettlementId(settlement.getId());
        item.setMerchantId(merchantId);
        item.setAccountBookId(accountBookId);
        item.setBusinessId(businessId);
        item.setBusinessNo(businessNo);
        item.setBusinessCategory("INVENTORY");
        item.setBusinessType(businessType);
        item.setDocumentAmount(documentAmount);
        item.setCurrentVerifyAmount(BigDecimal.ZERO);
        item.setVerifiedAmount(BigDecimal.ZERO);
        item.setUnverifiedAmount(documentAmount);
        settlementItemRepository.save(item);

        return settlement;
    }

    /**
     * 查询未结算完的结算单（供收款/付款单选源单）
     */
    /**
     * 收款/付款单保存时更新关联结算单的已核销金额
     */
    @Transactional
    public void updateWriteOff(Long orderId, BigDecimal currentVerifyAmount, Long merchantId, Long accountBookId, String businessType) {
        if (orderId == null || currentVerifyAmount == null) return;
        SettlementItem item = jqf.selectFrom(qSettlementItem)
                .innerJoin(qSettlement).on(qSettlement.id.eq(qSettlementItem.settlementId))
                .where(qSettlementItem.businessId.eq(orderId)
                        .and(qSettlementItem.businessCategory.eq("INVENTORY"))
                        .and(qSettlementItem.businessType.eq(businessType))
                        .and(qSettlement.merchantId.eq(merchantId))
                        .and(qSettlement.accountBookId.eq(accountBookId)))
                .fetchFirst();
        if (item != null) {
            BigDecimal newVerified = (item.getVerifiedAmount() != null ? item.getVerifiedAmount() : BigDecimal.ZERO)
                    .add(currentVerifyAmount);
            BigDecimal unverified = (item.getDocumentAmount() != null ? item.getDocumentAmount() : BigDecimal.ZERO)
                    .subtract(newVerified);
            item.setVerifiedAmount(newVerified);
            item.setUnverifiedAmount(unverified);
            item.setCurrentVerifyAmount(currentVerifyAmount);
            settlementItemRepository.save(item);

            // 已结清 → 标记为已平账
            if (unverified.compareTo(BigDecimal.ZERO) <= 0) {
                jqf.update(qSettlement)
                        .set(qSettlement.orderStatus, OrderStatus.已平账)
                        .where(qSettlement.id.eq(item.getSettlementId()))
                        .execute();
            }
        }
    }

    public PageResults<Map<String, Object>> writeOffCandidates(Page page, WriteOffQuery query) {
        List<Tuple> tuples = bqf.selectFrom(qSettlementItem)
                .select(qSettlementItem, qSettlement.orderNo, qSettlement.type)
                .innerJoin(qSettlement).on(qSettlement.id.eq(qSettlementItem.settlementId))
                .where(query.builder)
                .where(qSettlementItem.unverifiedAmount.gt(BigDecimal.ZERO))
                .where(qSettlementItem.businessCategory.eq("INVENTORY"))
                .where(qSettlement.orderStatus.ne(OrderStatus.已平账))
                .orderBy(qSettlement.id.desc())
                .offset(page.getOffset())
                .limit(page.getPageSize())
                .fetch();
        Long total = bqf.selectFrom(qSettlementItem)
                .select(qSettlementItem.id.count())
                .innerJoin(qSettlement).on(qSettlement.id.eq(qSettlementItem.settlementId))
                .where(query.builder)
                .where(qSettlementItem.unverifiedAmount.gt(BigDecimal.ZERO))
                .where(qSettlementItem.businessCategory.eq("INVENTORY"))
                .fetchOne();
        List<Map<String, Object>> results = tuples.stream().map(t -> {
            SettlementItem item = t.get(qSettlementItem);
            String settlementNo = t.get(qSettlement.orderNo);
            Integer settlementType = t.get(qSettlement.type);
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("businessId", item.getBusinessId());
            map.put("businessNo", settlementNo); // 显示结算单号
            map.put("businessType", 1); // 1=出入库单
            map.put("businessTypeLabel", settlementType != null && settlementType == 2 ? "供应商结算单" : "客户结算单");
            map.put("businessDate", item.getBusinessDate());
            map.put("documentAmount", item.getDocumentAmount());
            map.put("verifiedAmount", item.getVerifiedAmount());
            map.put("unverifiedAmount", item.getUnverifiedAmount());
            map.put("currentVerifyAmount", item.getCurrentVerifyAmount());
            map.put("businessRemarks", item.getBusinessRemarks());
            map.put("businessCategory", item.getBusinessCategory());
            map.put("settlementNo", settlementNo);
            return map;
        }).collect(Collectors.toList());
        return new PageResults<>(results, page, total != null ? total : 0);
    }

    public static class WriteOffQuery implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qSettlement.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qSettlement.accountBookId, accountBookId);
        }

        public void setPersonnelId(Long personnelId) {
            if (personnelId != null) {
                builder.and(qSettlement.personnelId.eq(personnelId));
            }
        }

        public void setType(Integer type) {
            if (type != null) {
                builder.and(qSettlement.type.eq(type));
            }
        }
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qSettlement.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qSettlement.accountBookId, accountBookId);
        }

        public void setStartTime(String startTime) {
            if (StrUtil.isNotEmpty(startTime)) {
                builder.and(qSettlement.createdAt.goe(LocalDateTime.parse(startTime + "T00:00:00")));
            }
        }

        public void setEndTime(String endTime) {
            if (StrUtil.isNotEmpty(endTime)) {
                builder.and(qSettlement.createdAt.loe(LocalDateTime.parse(endTime + "T23:59:59")));
            }
        }

        public void setOrderStatus(OrderStatus orderStatus) {
            if (orderStatus != null) {
                builder.and(qSettlement.orderStatus.eq(orderStatus));
            }
        }

        public void setOrderType(Integer orderType) {
            if (orderType != null) {
                builder.and(qSettlement.type.eq(orderType));
            }
        }

        public void setOrderNo(String orderNo) {
            if (StrUtil.isNotEmpty(orderNo)) {
                builder.and(qSettlement.orderNo.like("%" + orderNo + "%"));
            }
        }
    }
}
