package com.flyemu.share.service;

import com.flyemu.share.entity.basic.QProduct;
import com.flyemu.share.entity.basic.QProductCategory;
import com.flyemu.share.entity.fund.QOrderReceipt;
import com.flyemu.share.entity.fund.QOtherReceipt;
import com.flyemu.share.entity.invoice.Invoice;
import com.flyemu.share.entity.purchase.QPurchaseInbound;
import com.flyemu.share.entity.purchase.QPurchaseOrder;
import com.flyemu.share.entity.sales.QSalesOrder;
import com.flyemu.share.entity.sales.QSalesOutbound;
import com.flyemu.share.entity.sales.QSalesOutboundItem;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.repository.invoice.InvoiceRepository;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardService extends BaseService {

    private final static QSalesOutbound qOutbound = QSalesOutbound.salesOutbound;
    private final static QSalesOutboundItem qOutboundItem = QSalesOutboundItem.salesOutboundItem;
    private final static QSalesOrder qSalesOrder = QSalesOrder.salesOrder;
    private final static QPurchaseOrder qPurchaseOrder = QPurchaseOrder.purchaseOrder;
    private final static QPurchaseInbound qPurchaseInbound = QPurchaseInbound.purchaseInbound;
    private final static QOrderReceipt qReceipt = QOrderReceipt.orderReceipt;
    private final static QOtherReceipt qOtherReceipt = QOtherReceipt.otherReceipt;
    private final static QProduct qProduct = QProduct.product;
    private final static QProductCategory qCategory = QProductCategory.productCategory;

    private final InvoiceRepository invoiceRepository;

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static Long nz(Long v) {
        return v == null ? 0L : v;
    }

    private static BigDecimal scale(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    public Map<String, Object> overview(Long merchantId, Long accountBookId) {
        LocalDate today = LocalDate.now();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("metrics", metrics(merchantId, accountBookId, today));
        result.put("todo", todo(merchantId, accountBookId));
        result.put("trend", trend(merchantId, accountBookId, today));
        result.put("category", category(merchantId, accountBookId));
        return result;
    }

    private Map<String, Object> metrics(Long merchantId, Long accountBookId, LocalDate today) {
        BigDecimal todaySales = scale(jqf.select(qOutbound.finalAmount.sum())
                .from(qOutbound)
                .where(qOutbound.merchantId.eq(merchantId)
                        .and(qOutbound.accountBookId.eq(accountBookId))
                        .and(qOutbound.orderStatus.eq(OrderStatus.已审核))
                        .and(qOutbound.outboundDate.eq(today)))
                .fetchOne());

        BigDecimal todayCost = nz(jqf.select(qOutboundItem.costAmount.sum())
                .from(qOutboundItem)
                .leftJoin(qOutbound).on(qOutbound.id.eq(qOutboundItem.salesOutboundId))
                .where(qOutboundItem.merchantId.eq(merchantId)
                        .and(qOutboundItem.accountBookId.eq(accountBookId))
                        .and(qOutbound.orderStatus.eq(OrderStatus.已审核))
                        .and(qOutbound.outboundDate.eq(today)))
                .fetchOne());

        BigDecimal todayCollection = scale(jqf.select(qReceipt.collectionAmount.sum())
                .from(qReceipt)
                .where(qReceipt.merchantId.eq(merchantId)
                        .and(qReceipt.accountBookId.eq(accountBookId))
                        .and(qReceipt.orderStatus.eq(OrderStatus.已审核))
                        .and(qReceipt.orderDate.eq(today)))
                .fetchOne());

        BigDecimal todayOtherReceipt = scale(jqf.select(qOtherReceipt.collectionAmount.sum())
                .from(qOtherReceipt)
                .where(qOtherReceipt.merchantId.eq(merchantId)
                        .and(qOtherReceipt.accountBookId.eq(accountBookId))
                        .and(qOtherReceipt.orderStatus.eq(OrderStatus.已审核))
                        .and(qOtherReceipt.orderDate.eq(today)))
                .fetchOne());

        todayCollection = todayCollection.add(todayOtherReceipt);

        BigDecimal monthInvoice = monthInvoiceAmount(merchantId, today);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("todaySales", todaySales);
        m.put("todayGrossProfit", todaySales.subtract(scale(todayCost)));
        m.put("todayCollection", todayCollection);
        m.put("monthInvoiceAmount", monthInvoice);
        return m;
    }

    private BigDecimal monthInvoiceAmount(Long merchantId, LocalDate today) {
        YearMonth month = YearMonth.from(today);
        List<Invoice> invoices = invoiceRepository.findAllByMerchantIdOrderByIssueDateDesc(merchantId);
        BigDecimal sum = BigDecimal.ZERO;
        for (Invoice inv : invoices) {
            if (inv.getIssueDate() == null) continue;
            if (!YearMonth.from(inv.getIssueDate()).equals(month)) continue;
            if ("RED".equals(inv.getInvoiceType())) continue;
            sum = sum.add(nz(inv.getTotalAmount()));
        }
        return scale(sum);
    }

    private Map<String, Object> todo(Long merchantId, Long accountBookId) {
        long pendingAudit = nz(jqf.select(qSalesOrder.id.count()).from(qSalesOrder)
                .where(qSalesOrder.merchantId.eq(merchantId)
                        .and(qSalesOrder.accountBookId.eq(accountBookId))
                        .and(qSalesOrder.orderStatus.eq(OrderStatus.已保存))).fetchOne())
                + nz(jqf.select(qPurchaseOrder.id.count()).from(qPurchaseOrder)
                        .where(qPurchaseOrder.merchantId.eq(merchantId)
                                .and(qPurchaseOrder.accountBookId.eq(accountBookId))
                                .and(qPurchaseOrder.orderStatus.eq(OrderStatus.已保存))).fetchOne())
                + nz(jqf.select(qOutbound.id.count()).from(qOutbound)
                        .where(qOutbound.merchantId.eq(merchantId)
                                .and(qOutbound.accountBookId.eq(accountBookId))
                                .and(qOutbound.orderStatus.eq(OrderStatus.已保存))).fetchOne())
                + nz(jqf.select(qPurchaseInbound.id.count()).from(qPurchaseInbound)
                        .where(qPurchaseInbound.merchantId.eq(merchantId)
                                .and(qPurchaseInbound.accountBookId.eq(accountBookId))
                                .and(qPurchaseInbound.orderStatus.eq(OrderStatus.已保存))).fetchOne());

        List<Tuple> outbounds = jqf.select(qOutbound.finalAmount, qOutbound.collectionAmount)
                .from(qOutbound)
                .where(qOutbound.merchantId.eq(merchantId)
                        .and(qOutbound.accountBookId.eq(accountBookId))
                        .and(qOutbound.orderStatus.eq(OrderStatus.已审核)))
                .fetch();
        BigDecimal pendingCollection = BigDecimal.ZERO;
        for (Tuple t : outbounds) {
            BigDecimal finalAmount = nz(t.get(qOutbound.finalAmount));
            BigDecimal collection = nz(t.get(qOutbound.collectionAmount));
            BigDecimal diff = finalAmount.subtract(collection);
            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                pendingCollection = pendingCollection.add(diff);
            }
        }

        long pendingInvoice = nz(jqf.select(qOutbound.id.count()).from(qOutbound)
                .where(qOutbound.merchantId.eq(merchantId)
                        .and(qOutbound.accountBookId.eq(accountBookId))
                        .and(qOutbound.orderStatus.eq(OrderStatus.已审核))
                        .and(qOutbound.invoiceStatus.eq("未开票"))).fetchOne());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pendingAuditCount", pendingAudit);
        m.put("pendingCollection", scale(pendingCollection));
        m.put("pendingInvoice", pendingInvoice);
        return m;
    }

    private List<Map<String, Object>> trend(Long merchantId, Long accountBookId, LocalDate today) {
        LocalDate start = today.minusDays(6);
        List<Tuple> rows = jqf.select(qOutbound.outboundDate, qOutbound.finalAmount)
                .from(qOutbound)
                .where(qOutbound.merchantId.eq(merchantId)
                        .and(qOutbound.accountBookId.eq(accountBookId))
                        .and(qOutbound.orderStatus.eq(OrderStatus.已审核))
                        .and(qOutbound.outboundDate.goe(start))
                        .and(qOutbound.outboundDate.loe(today)))
                .fetch();

        Map<LocalDate, BigDecimal> byDay = new TreeMap<>();
        for (Tuple t : rows) {
            LocalDate d = t.get(qOutbound.outboundDate);
            if (d == null) continue;
            byDay.merge(d, nz(t.get(qOutbound.finalAmount)), BigDecimal::add);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", d.toString());
            m.put("sales", scale(byDay.getOrDefault(d, BigDecimal.ZERO)));
            result.add(m);
        }
        return result;
    }

    private List<Map<String, Object>> category(Long merchantId, Long accountBookId) {
        List<Tuple> rows = jqf.select(qCategory.name, qOutboundItem.subtotal.sum())
                .from(qOutboundItem)
                .leftJoin(qOutbound).on(qOutbound.id.eq(qOutboundItem.salesOutboundId))
                .leftJoin(qProduct).on(qProduct.id.eq(qOutboundItem.productId))
                .leftJoin(qCategory).on(qCategory.id.eq(qProduct.productCategoryId))
                .where(qOutboundItem.merchantId.eq(merchantId)
                        .and(qOutboundItem.accountBookId.eq(accountBookId))
                        .and(qOutbound.orderStatus.eq(OrderStatus.已审核)))
                .groupBy(qCategory.name)
                .fetch();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Tuple t : rows) {
            String name = t.get(qCategory.name);
            if (name == null) name = "未分类";
            BigDecimal value = nz(t.get(qOutboundItem.subtotal.sum()));
            if (value.compareTo(BigDecimal.ZERO) <= 0) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", name);
            m.put("value", scale(value));
            result.add(m);
        }
        result.sort((a, b) -> ((BigDecimal) b.get("value")).compareTo((BigDecimal) a.get("value")));
        return result;
    }
}
