package com.flyemu.share.service.fund;

import cn.hutool.core.bean.BeanUtil;
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
import com.flyemu.share.service.setting.CodeSeedService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final CodeSeedService codeSeedService;

    /** 平账判断误差阈值(金额两位小数),剩余未结超过该值视为未平账 */
    private static final BigDecimal SETTLE_EPSILON = new BigDecimal("0.005");

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
        // 统一走 code_seed 单调取号（与采购/销售订单一致）：按账套+重置周期递增，删除/反审核单据不复用已发号段。
        settlement.setOrderNo(codeSeedService.generateCode(
                settlement.getMerchantId(), settlement.getAccountBookId(), "结算单"));
    }

    public PageResults<Settlement> query(Page page, Query query) {
        PagedList<Settlement> fetchPage = bqf.selectFrom(qSettlement)
                .where(query.builder)
                .orderBy(qSettlement.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());
        enrichListRows(fetchPage);
        return new PageResults<>(fetchPage, page, fetchPage.getTotalSize());
    }

    /**
     * 列表行富化：按结算单批量取 INVENTORY 明细，填充可核对列
     * (源单据号/源单据类型/源单据日期、单据金额/已核销实收/剩余未结、已平账但仍有剩余的异常标)。
     * 仅计 INVENTORY 明细(FUND / business_category 为空的历史手工行忽略)；纯手工 FUND 结算单保持 null、不标红。
     */
    private void enrichListRows(List<Settlement> settlements) {
        if (settlements == null || settlements.isEmpty()) {
            return;
        }
        List<Long> settlementIds = settlements.stream().map(Settlement::getId).collect(Collectors.toList());
        Map<Long, List<SettlementItem>> itemsBySettlement = jqf.selectFrom(qSettlementItem)
                .where(qSettlementItem.settlementId.in(settlementIds))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(SettlementItem::getSettlementId));
        for (Settlement s : settlements) {
            List<SettlementItem> inv = itemsBySettlement.getOrDefault(s.getId(), Collections.emptyList()).stream()
                    .filter(i -> "INVENTORY".equals(i.getBusinessCategory()))
                    .collect(Collectors.toList());
            if (inv.isEmpty()) {
                continue;
            }
            BigDecimal doc = inv.stream().map(SettlementItem::getDocumentAmount).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal verified = inv.stream().map(SettlementItem::getVerifiedAmount).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal unverified = inv.stream().map(SettlementItem::getUnverifiedAmount).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            s.setSumDocumentAmount(doc);
            s.setSumVerifiedAmount(verified);
            s.setSumUnverifiedAmount(unverified);
            String sourceNo = inv.stream().map(SettlementItem::getBusinessNo).filter(Objects::nonNull)
                    .filter(n -> !n.isBlank()).distinct().collect(Collectors.joining("、"));
            if (!sourceNo.isEmpty()) {
                s.setSourceBusinessNo(sourceNo);
            }
            String sourceType = inv.stream().map(SettlementItem::getBusinessType).filter(Objects::nonNull)
                    .distinct().collect(Collectors.joining("、"));
            if (!sourceType.isEmpty()) {
                s.setSourceBusinessType(sourceType);
            }
            Date firstDate = inv.stream().map(SettlementItem::getBusinessDate).filter(Objects::nonNull)
                    .min(Date::compareTo).orElse(null);
            if (firstDate != null) {
                s.setSourceDate(new java.sql.Date(firstDate.getTime()).toLocalDate());
            }
            s.setStatusMismatch(OrderStatus.已平账.equals(s.getOrderStatus())
                    && unverified.abs().compareTo(SETTLE_EPSILON) > 0);
        }
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
        // 平账/未平账是核销自动计算的派生状态，禁止通过审核接口直接写入（防止绕过余额校验）
        if (state != OrderStatus.已保存 && state != OrderStatus.已审核) {
            throw new ServiceException("平账/未平账由核销自动计算，仅支持【已审核/已保存】的审核与反审核操作");
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
                                       Long businessId, String businessType,
                                       LocalDate businessDate) {
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
        // 记录源单据业务日期(列表核对列展示用)
        if (businessDate != null) {
            item.setBusinessDate(java.sql.Date.valueOf(businessDate));
        }
        item.setCurrentVerifyAmount(BigDecimal.ZERO);
        item.setVerifiedAmount(BigDecimal.ZERO);
        item.setUnverifiedAmount(documentAmount);
        settlementItemRepository.save(item);

        return settlement;
    }

    /**
     * 反审核出入库单时，删除其自动生成的未平账结算单（明细 + 无残留明细的主表一并删除）。
     * 明细已被核销（verifiedAmount>0）时不删除并抛异常，调用方应先拦截付款/核销场景。
     */
    @Transactional
    public void removeAutoSettlement(Long merchantId, Long accountBookId, Long businessId, String businessType) {
        if (businessId == null) {
            return;
        }
        List<Tuple> rows = jqf.select(qSettlementItem.id, qSettlementItem.settlementId, qSettlementItem.verifiedAmount)
                .from(qSettlementItem)
                .innerJoin(qSettlement).on(qSettlement.id.eq(qSettlementItem.settlementId))
                .where(qSettlementItem.businessId.eq(businessId)
                        .and(qSettlementItem.businessCategory.eq("INVENTORY"))
                        .and(qSettlementItem.businessType.eq(businessType))
                        .and(qSettlement.merchantId.eq(merchantId))
                        .and(qSettlement.accountBookId.eq(accountBookId)))
                .fetch();
        if (rows.isEmpty()) {
            return;
        }
        Set<Long> settlementIds = new HashSet<>();
        List<Long> itemIds = new ArrayList<>();
        for (Tuple row : rows) {
            BigDecimal verified = row.get(qSettlementItem.verifiedAmount);
            if (verified != null && verified.signum() > 0) {
                throw new ServiceException("该单据的结算单已核销，无法随单据反审核删除，请先处理相关收付款/核销~");
            }
            itemIds.add(row.get(qSettlementItem.id));
            settlementIds.add(row.get(qSettlementItem.settlementId));
        }
        jqf.delete(qSettlementItem).where(qSettlementItem.id.in(itemIds)).execute();
        // 主表已无任何明细 → 一并删除，避免残留"未平账"空单
        for (Long sid : settlementIds) {
            long remain = jqf.selectFrom(qSettlementItem)
                    .where(qSettlementItem.settlementId.eq(sid)).fetchCount();
            if (remain == 0) {
                jqf.delete(qSettlement).where(qSettlement.id.eq(sid)).execute();
            }
        }
    }

    /**
     * 一次性清理历史遗留的"未平账空单"：主表为未平账且没有任何结算明细则视为空单删除。
     * 修复"反审核未删结算单主表"bug 之前产生的脏数据，通过管理接口调用（可按账套过滤）。
     *
     * @return 清理的空结算单数量
     */
    @Transactional
    public int cleanOrphanSettlements(Long merchantId, Long accountBookId) {
        BooleanBuilder cond = new BooleanBuilder(qSettlement.orderStatus.eq(OrderStatus.未平账));
        if (merchantId != null) {
            cond.and(qSettlement.merchantId.eq(merchantId));
        }
        if (accountBookId != null) {
            cond.and(qSettlement.accountBookId.eq(accountBookId));
        }
        List<Long> orphanIds = jqf.select(qSettlement.id).from(qSettlement)
                .where(cond, qSettlement.id.notIn(
                        JPAExpressions.select(qSettlementItem.settlementId).from(qSettlementItem)))
                .fetch();
        if (orphanIds.isEmpty()) {
            return 0;
        }
        // 兜底清掉可能残留的空明细（正常为空单不会有明细）
        jqf.delete(qSettlementItem)
                .where(qSettlementItem.settlementId.in(orphanIds)).execute();
        jqf.delete(qSettlement).where(qSettlement.id.in(orphanIds)).execute();
        return orphanIds.size();
    }

    /**
     * 一次性迁移：结算单取号从旧的 max(id)+1 切到 code_seed 后，把各账套"当前归零桶"的 code_seed
     * 计数器回填到历史最大流水号（从既有单号右端截取），使新单接续（如 ...0072 之后到 ...0073）且不重号。
     * 已删除单据的历史高位无法复原，回填取现存单据最大流水，保证后续不再复用现存单号即可。
     *
     * @return 实际新建/抬升的计数条数
     */
    @Transactional
    public int migrateSettlementSerialSeed() {
        int seeded = 0;
        List<Tuple> pairs = jqf.select(qSettlement.merchantId, qSettlement.accountBookId)
                .from(qSettlement).distinct().fetch();
        for (Tuple pair : pairs) {
            Long merchantId = pair.get(qSettlement.merchantId);
            Long accountBookId = pair.get(qSettlement.accountBookId);
            if (merchantId == null || accountBookId == null) {
                continue;
            }
            CodeRule rule = codeRuleService.findByDocumentTypeAndMerchantIdAndAccountBookId(
                    CodeRule.DocumentType.结算单, merchantId, accountBookId);
            if (rule == null || rule.getSerialNumberLength() == null) {
                continue;
            }
            int serialLen = rule.getSerialNumberLength();
            List<Settlement> list = jqf.selectFrom(qSettlement)
                    .where(qSettlement.merchantId.eq(merchantId)
                            .and(qSettlement.accountBookId.eq(accountBookId))
                            .and(qSettlement.orderNo.isNotNull()))
                    .fetch();
            for (Settlement s : list) {
                String no = s.getOrderNo();
                if (no == null || no.length() < serialLen) {
                    continue;
                }
                String tail = no.substring(no.length() - serialLen);
                if (!tail.chars().allMatch(Character::isDigit)) {
                    continue;
                }
                try {
                    Integer serial = Integer.valueOf(tail);
                    seeded += codeSeedService.raiseSeed(merchantId, accountBookId, "结算单", s.getCreatedAt(), serial);
                } catch (NumberFormatException ignore) {
                    // 历史随机单号（非规则流水）跳过，不影响新号
                }
            }
        }
        return seeded;
    }

    /**
     * 审核核销：在 INVENTORY 结算明细上累加本次核销金额（钳制与状态重算见 applyWriteOff）。
     */
    @Transactional
    public void updateWriteOff(Long orderId, BigDecimal currentVerifyAmount, Long merchantId, Long accountBookId, String businessType) {
        if (orderId == null || currentVerifyAmount == null || currentVerifyAmount.signum() == 0) return;
        applyWriteOff(orderId, currentVerifyAmount, merchantId, accountBookId, businessType, false);
    }

    /**
     * 反审核核销：对称扣减该笔贡献（收款/付款/核销单反审核时调用，防止重复累加/残留已平账）。
     * 未找到匹配的 INVENTORY 结算明细（期初行、结算单已被删除等正常缺省）时不抛异常，仅告警。
     */
    @Transactional
    public void reverseWriteOff(Long orderId, BigDecimal currentVerifyAmount, Long merchantId, Long accountBookId, String businessType) {
        if (orderId == null || currentVerifyAmount == null || currentVerifyAmount.signum() == 0) return;
        applyWriteOff(orderId, currentVerifyAmount.negate(), merchantId, accountBookId, businessType, true);
    }

    /**
     * 有符号核销核心：delta&gt;0 审核累加，delta&lt;0 反审核扣减。
     * 两端钳制（0 ≤ verified ≤ document），并统一走 recalcSettlementStatus 重算主表平账状态（增/减两方向一致）。
     */
    private void applyWriteOff(Long orderId, BigDecimal delta, Long merchantId, Long accountBookId,
                               String businessType, boolean warnIfMissing) {
        SettlementItem item = jqf.selectFrom(qSettlementItem)
                .innerJoin(qSettlement).on(qSettlement.id.eq(qSettlementItem.settlementId))
                .where(qSettlementItem.businessId.eq(orderId)
                        .and(qSettlementItem.businessCategory.eq("INVENTORY"))
                        .and(qSettlementItem.businessType.eq(businessType))
                        .and(qSettlement.merchantId.eq(merchantId))
                        .and(qSettlement.accountBookId.eq(accountBookId)))
                .fetchFirst();
        if (item == null) {
            if (warnIfMissing) {
                log.warn("reverseWriteOff: 未找到可扣减的 INVENTORY 结算明细, businessType={}, businessId={}, delta={}",
                        businessType, orderId, delta);
            }
            return;
        }
        BigDecimal document = item.getDocumentAmount() != null ? item.getDocumentAmount() : BigDecimal.ZERO;
        BigDecimal verified = item.getVerifiedAmount() != null ? item.getVerifiedAmount() : BigDecimal.ZERO;
        BigDecimal newVerified = verified.add(delta);
        if (newVerified.signum() < 0) {
            log.warn("reverseWriteOff: 扣减超出已核销, 已钳制为0, businessId={}, delta={}, verified={}", orderId, delta, verified);
            newVerified = BigDecimal.ZERO;
        }
        if (newVerified.compareTo(document) > 0) {
            log.warn("updateWriteOff: 核销超出单据金额, 已钳制为单据金额, businessId={}, document={}, newVerified={}",
                    orderId, document, newVerified);
            newVerified = document;
        }
        item.setVerifiedAmount(newVerified);
        item.setUnverifiedAmount(document.subtract(newVerified));
        if (delta.signum() > 0) {
            item.setCurrentVerifyAmount(delta);
        } else if (newVerified.signum() == 0) {
            item.setCurrentVerifyAmount(BigDecimal.ZERO);
        }
        settlementItemRepository.save(item);
        recalcSettlementStatus(item.getSettlementId());
    }

    /**
     * 重算结算单主表平账状态：只要存在任一 INVENTORY 明细剩余未结&gt;0.005 即未平账，否则已平账。
     * FUND / business_category 为空的历史手工行忽略（不属于自动结算口径）。
     * 仅对当前状态为 未平账/已平账 的自动结算单生效，不覆盖手工单的 已保存/已审核 状态。
     */
    private void recalcSettlementStatus(Long settlementId) {
        if (settlementId == null) return;
        Settlement settlement = settlementRepository.findById(settlementId).orElse(null);
        if (settlement == null) return;
        if (settlement.getOrderStatus() != OrderStatus.未平账 && settlement.getOrderStatus() != OrderStatus.已平账) {
            log.warn("recalcSettlementStatus: 跳过非自动结算单状态 orderStatus={}, settlementId={}",
                    settlement.getOrderStatus(), settlementId);
            return;
        }
        List<SettlementItem> invItems = jqf.selectFrom(qSettlementItem)
                .where(qSettlementItem.settlementId.eq(settlementId)
                        .and(qSettlementItem.businessCategory.eq("INVENTORY")))
                .fetch();
        boolean open = invItems.stream().anyMatch(i ->
                i.getUnverifiedAmount() == null || i.getUnverifiedAmount().compareTo(SETTLE_EPSILON) > 0);
        jqf.update(qSettlement)
                .set(qSettlement.orderStatus, open ? OrderStatus.未平账 : OrderStatus.已平账)
                .where(qSettlement.id.eq(settlementId))
                .execute();
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
