package com.flyemu.share.service.fund;

import com.flyemu.share.entity.fund.QOtherExpense;
import com.flyemu.share.entity.fund.QOtherReceipt;
import com.flyemu.share.entity.sales.QSalesOutbound;
import com.flyemu.share.entity.sales.QSalesOutboundItem;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FundReportService extends BaseService {

    private final static QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;
    private final static QSalesOutboundItem qSalesOutboundItem = QSalesOutboundItem.salesOutboundItem;
    private final static QOtherReceipt qOtherReceipt = QOtherReceipt.otherReceipt;
    private final static QOtherExpense qOtherExpense = QOtherExpense.otherExpense;

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * 利润表：销售收入 - 销售成本 + 其他收入 - 其他支出
     */
    public Map<String, Object> profit(Long merchantId, Long accountBookId, LocalDate start, LocalDate end) {
        BigDecimal salesRevenue = salesRevenue(merchantId, accountBookId, start, end);
        BigDecimal salesCost = salesCost(merchantId, accountBookId, start, end);
        BigDecimal otherIncome = otherIncome(merchantId, accountBookId, start, end);
        BigDecimal otherExpense = otherExpense(merchantId, accountBookId, start, end);

        BigDecimal grossProfit = salesRevenue.subtract(salesCost);
        BigDecimal netProfit = grossProfit.add(otherIncome).subtract(otherExpense);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("salesRevenue", salesRevenue.setScale(2, RoundingMode.HALF_UP));
        m.put("salesCost", salesCost.setScale(2, RoundingMode.HALF_UP));
        m.put("grossProfit", grossProfit.setScale(2, RoundingMode.HALF_UP));
        m.put("otherIncome", otherIncome.setScale(2, RoundingMode.HALF_UP));
        m.put("otherExpense", otherExpense.setScale(2, RoundingMode.HALF_UP));
        m.put("netProfit", netProfit.setScale(2, RoundingMode.HALF_UP));
        return m;
    }

    /**
     * 利润表按月分组：每个月一行，列出销售收入/销售成本/毛利/其他收入/其他支出/净利润。
     * 采用按 (日期, 金额) 原始行拉取后在内存按月聚合，避免数据库函数差异。
     */
    public List<Map<String, Object>> profitMonthly(Long merchantId, Long accountBookId, LocalDate start, LocalDate end) {
        // [0]=销售收入 [1]=销售成本 [2]=其他收入 [3]=其他支出
        Map<String, BigDecimal[]> monthMap = new TreeMap<>();
        addMonthly(monthMap, 0, monthlySalesRevenue(merchantId, accountBookId, start, end));
        addMonthly(monthMap, 1, monthlySalesCost(merchantId, accountBookId, start, end));
        addMonthly(monthMap, 2, monthlyOtherIncome(merchantId, accountBookId, start, end));
        addMonthly(monthMap, 3, monthlyOtherExpense(merchantId, accountBookId, start, end));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> e : monthMap.entrySet()) {
            BigDecimal[] v = e.getValue();
            BigDecimal grossProfit = v[0].subtract(v[1]);
            BigDecimal netProfit = grossProfit.add(v[2]).subtract(v[3]);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", e.getKey());
            row.put("salesRevenue", v[0].setScale(2, RoundingMode.HALF_UP));
            row.put("salesCost", v[1].setScale(2, RoundingMode.HALF_UP));
            row.put("grossProfit", grossProfit.setScale(2, RoundingMode.HALF_UP));
            row.put("otherIncome", v[2].setScale(2, RoundingMode.HALF_UP));
            row.put("otherExpense", v[3].setScale(2, RoundingMode.HALF_UP));
            row.put("netProfit", netProfit.setScale(2, RoundingMode.HALF_UP));
            result.add(row);
        }
        return result;
    }

    private void addMonthly(Map<String, BigDecimal[]> monthMap, int idx, List<Object[]> rows) {
        for (Object[] r : rows) {
            LocalDate d = (LocalDate) r[0];
            if (d == null) continue;
            String key = d.toString().substring(0, 7);
            BigDecimal v = nz((BigDecimal) r[1]);
            BigDecimal[] arr = monthMap.computeIfAbsent(key,
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            arr[idx] = arr[idx].add(v);
        }
    }

    private List<Object[]> monthlySalesRevenue(Long merchantId, Long accountBookId, LocalDate start, LocalDate end) {
        BooleanBuilder b = new BooleanBuilder();
        b.and(qSalesOutbound.merchantId.eq(merchantId));
        b.and(qSalesOutbound.accountBookId.eq(accountBookId));
        b.and(qSalesOutbound.orderStatus.eq(OrderStatus.已审核));
        if (start != null) b.and(qSalesOutbound.outboundDate.goe(start));
        if (end != null) b.and(qSalesOutbound.outboundDate.loe(end));
        return jqf.select(qSalesOutbound.outboundDate, qSalesOutbound.finalAmount)
                .from(qSalesOutbound).where(b).fetch()
                .stream().map(t -> new Object[]{t.get(qSalesOutbound.outboundDate), t.get(qSalesOutbound.finalAmount)})
                .toList();
    }

    private List<Object[]> monthlySalesCost(Long merchantId, Long accountBookId, LocalDate start, LocalDate end) {
        BooleanBuilder b = new BooleanBuilder();
        b.and(qSalesOutboundItem.merchantId.eq(merchantId));
        b.and(qSalesOutboundItem.accountBookId.eq(accountBookId));
        b.and(qSalesOutbound.orderStatus.eq(OrderStatus.已审核));
        if (start != null) b.and(qSalesOutbound.outboundDate.goe(start));
        if (end != null) b.and(qSalesOutbound.outboundDate.loe(end));
        return jqf.select(qSalesOutbound.outboundDate, qSalesOutboundItem.costAmount)
                .from(qSalesOutboundItem)
                .leftJoin(qSalesOutbound).on(qSalesOutbound.id.eq(qSalesOutboundItem.salesOutboundId))
                .where(b).fetch()
                .stream().map(t -> new Object[]{t.get(qSalesOutbound.outboundDate), t.get(qSalesOutboundItem.costAmount)})
                .toList();
    }

    private List<Object[]> monthlyOtherIncome(Long merchantId, Long accountBookId, LocalDate start, LocalDate end) {
        BooleanBuilder b = new BooleanBuilder();
        b.and(qOtherReceipt.merchantId.eq(merchantId));
        b.and(qOtherReceipt.accountBookId.eq(accountBookId));
        b.and(qOtherReceipt.orderStatus.eq(OrderStatus.已审核));
        if (start != null) b.and(qOtherReceipt.orderDate.goe(start));
        if (end != null) b.and(qOtherReceipt.orderDate.loe(end));
        return jqf.select(qOtherReceipt.orderDate, qOtherReceipt.collectionAmount)
                .from(qOtherReceipt).where(b).fetch()
                .stream().map(t -> new Object[]{t.get(qOtherReceipt.orderDate), t.get(qOtherReceipt.collectionAmount)})
                .toList();
    }

    private List<Object[]> monthlyOtherExpense(Long merchantId, Long accountBookId, LocalDate start, LocalDate end) {
        BooleanBuilder b = new BooleanBuilder();
        b.and(qOtherExpense.merchantId.eq(merchantId));
        b.and(qOtherExpense.accountBookId.eq(accountBookId));
        b.and(qOtherExpense.orderStatus.eq(OrderStatus.已审核));
        if (start != null) b.and(qOtherExpense.orderDate.goe(start));
        if (end != null) b.and(qOtherExpense.orderDate.loe(end));
        return jqf.select(qOtherExpense.orderDate, qOtherExpense.collectionAmount)
                .from(qOtherExpense).where(b).fetch()
                .stream().map(t -> new Object[]{t.get(qOtherExpense.orderDate), t.get(qOtherExpense.collectionAmount)})
                .toList();
    }

    private BigDecimal salesRevenue(Long merchantId, Long accountBookId, LocalDate start, LocalDate end) {
        BooleanBuilder b = new BooleanBuilder();
        b.and(qSalesOutbound.merchantId.eq(merchantId));
        b.and(qSalesOutbound.accountBookId.eq(accountBookId));
        b.and(qSalesOutbound.orderStatus.eq(OrderStatus.已审核));
        if (start != null) b.and(qSalesOutbound.outboundDate.goe(start));
        if (end != null) b.and(qSalesOutbound.outboundDate.loe(end));
        return nz(jqf.select(qSalesOutbound.finalAmount.sum()).from(qSalesOutbound).where(b).fetchOne());
    }

    private BigDecimal salesCost(Long merchantId, Long accountBookId, LocalDate start, LocalDate end) {
        BooleanBuilder b = new BooleanBuilder();
        b.and(qSalesOutboundItem.merchantId.eq(merchantId));
        b.and(qSalesOutboundItem.accountBookId.eq(accountBookId));
        b.and(qSalesOutbound.orderStatus.eq(OrderStatus.已审核));
        if (start != null) b.and(qSalesOutbound.outboundDate.goe(start));
        if (end != null) b.and(qSalesOutbound.outboundDate.loe(end));
        return nz(jqf.select(qSalesOutboundItem.costAmount.sum())
                .from(qSalesOutboundItem)
                .leftJoin(qSalesOutbound).on(qSalesOutbound.id.eq(qSalesOutboundItem.salesOutboundId))
                .where(b)
                .fetchOne());
    }

    private BigDecimal otherIncome(Long merchantId, Long accountBookId, LocalDate start, LocalDate end) {
        BooleanBuilder b = new BooleanBuilder();
        b.and(qOtherReceipt.merchantId.eq(merchantId));
        b.and(qOtherReceipt.accountBookId.eq(accountBookId));
        b.and(qOtherReceipt.orderStatus.eq(OrderStatus.已审核));
        if (start != null) b.and(qOtherReceipt.orderDate.goe(start));
        if (end != null) b.and(qOtherReceipt.orderDate.loe(end));
        return nz(jqf.select(qOtherReceipt.collectionAmount.sum()).from(qOtherReceipt).where(b).fetchOne());
    }

    private BigDecimal otherExpense(Long merchantId, Long accountBookId, LocalDate start, LocalDate end) {
        BooleanBuilder b = new BooleanBuilder();
        b.and(qOtherExpense.merchantId.eq(merchantId));
        b.and(qOtherExpense.accountBookId.eq(accountBookId));
        b.and(qOtherExpense.orderStatus.eq(OrderStatus.已审核));
        if (start != null) b.and(qOtherExpense.orderDate.goe(start));
        if (end != null) b.and(qOtherExpense.orderDate.loe(end));
        return nz(jqf.select(qOtherExpense.collectionAmount.sum()).from(qOtherExpense).where(b).fetchOne());
    }
}
