package com.flyemu.share.service.invoice;

import com.flyemu.share.entity.invoice.InputInvoice;
import com.flyemu.share.repository.invoice.InputInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 进项发票（供应商开给本商户的采购发票）管理 + 进项归集。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InputInvoiceService {

    private final InputInvoiceRepository repository;

    @Transactional(readOnly = true)
    public Page<InputInvoice> list(Long merchantId, int page, int size) {
        return repository.findAllByMerchantIdOrderByIssueDateDescIdDesc(merchantId, PageRequest.of(page, size));
    }

    @Transactional
    public InputInvoice save(InputInvoice invoice, Long merchantId, Long accountBookId) {
        if (invoice.getId() != null) {
            InputInvoice original = repository.findById(invoice.getId())
                    .filter(i -> merchantId.equals(i.getMerchantId()))
                    .orElseThrow(() -> new RuntimeException("进项发票不存在: id=" + invoice.getId()));
            original.setSupplierName(invoice.getSupplierName());
            original.setSupplierTaxNo(invoice.getSupplierTaxNo());
            original.setInvoiceCode(invoice.getInvoiceCode());
            original.setInvoiceNo(invoice.getInvoiceNo());
            original.setIssueDate(invoice.getIssueDate());
            original.setAmount(invoice.getAmount());
            original.setTax(invoice.getTax());
            original.setTotalAmount(invoice.getTotalAmount());
            original.setRemark(invoice.getRemark());
            return repository.save(original);
        }
        invoice.setId(null);
        invoice.setMerchantId(merchantId);
        invoice.setAccountBookId(accountBookId);
        // 价税合计未填时，按金额+税额推算
        if (invoice.getTotalAmount() == null) {
            BigDecimal amount = nz(invoice.getAmount());
            BigDecimal tax = nz(invoice.getTax());
            invoice.setTotalAmount(amount.add(tax));
        }
        return repository.save(invoice);
    }

    @Transactional
    public void delete(Long merchantId, Long id) {
        repository.findById(id)
                .filter(i -> merchantId.equals(i.getMerchantId()))
                .ifPresent(repository::delete);
    }

    /**
     * 进项归集：按开票月份汇总进项发票（数量、金额、税额、价税合计）。
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> aggregation(Long merchantId) {
        List<InputInvoice> list = repository.findAllByMerchantId(merchantId);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, BigDecimal[]> byMonth = new TreeMap<>();
        for (InputInvoice inv : list) {
            String month = inv.getIssueDate() != null ? inv.getIssueDate().format(fmt) : "未登记";
            BigDecimal[] agg = byMonth.computeIfAbsent(month, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            agg[0] = agg[0].add(BigDecimal.ONE); // 张数
            agg[1] = agg[1].add(nz(inv.getAmount())); // 不含税金额
            agg[2] = agg[2].add(nz(inv.getTax())); // 税额
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> e : byMonth.entrySet()) {
            BigDecimal[] agg = e.getValue();
            Map<String, Object> m = new HashMap<>();
            m.put("month", e.getKey());
            m.put("invoiceCount", agg[0].intValue());
            m.put("amount", agg[1].setScale(2, RoundingMode.HALF_UP));
            m.put("tax", agg[2].setScale(2, RoundingMode.HALF_UP));
            m.put("totalAmount", agg[1].add(agg[2]).setScale(2, RoundingMode.HALF_UP));
            result.add(m);
        }
        return result;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
