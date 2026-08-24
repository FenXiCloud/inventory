package com.flyemu.share.service.fund;

import com.flyemu.share.entity.basic.Customer;
import com.flyemu.share.entity.basic.QCustomer;
import com.flyemu.share.entity.basic.QSupplier;
import com.flyemu.share.entity.basic.Supplier;
import com.flyemu.share.service.BaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 预收/预付余额查询：客户 balance 为负表示预收，供应商 balance 为负表示预付。
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdvanceBalanceService extends BaseService {

    private final static QCustomer qCustomer = QCustomer.customer;
    private final static QSupplier qSupplier = QSupplier.supplier;

    public Map<String, Object> advanceBalance(Long merchantId, Long accountBookId) {
        List<Map<String, Object>> customerList = new ArrayList<>();
        BigDecimal customerTotal = BigDecimal.ZERO;
        List<Customer> customers = jqf.selectFrom(qCustomer)
                .where(qCustomer.merchantId.eq(merchantId)
                        .and(qCustomer.accountBookId.eq(accountBookId))
                        .and(qCustomer.balance.lt(BigDecimal.ZERO)))
                .orderBy(qCustomer.balance.asc())
                .fetch();
        for (Customer c : customers) {
            BigDecimal adv = c.getBalance().negate();
            customerTotal = customerTotal.add(adv);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", c.getId());
            row.put("code", c.getCode());
            row.put("name", c.getName());
            row.put("advanceBalance", adv.setScale(2, RoundingMode.HALF_UP));
            customerList.add(row);
        }

        List<Map<String, Object>> supplierList = new ArrayList<>();
        BigDecimal supplierTotal = BigDecimal.ZERO;
        List<Supplier> suppliers = jqf.selectFrom(qSupplier)
                .where(qSupplier.merchantId.eq(merchantId)
                        .and(qSupplier.accountBookId.eq(accountBookId))
                        .and(qSupplier.balance.lt(BigDecimal.ZERO)))
                .orderBy(qSupplier.balance.asc())
                .fetch();
        for (Supplier s : suppliers) {
            BigDecimal adv = s.getBalance().negate();
            supplierTotal = supplierTotal.add(adv);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", s.getId());
            row.put("code", s.getCode());
            row.put("name", s.getName());
            row.put("advanceBalance", adv.setScale(2, RoundingMode.HALF_UP));
            supplierList.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customerAdvance", customerList);
        result.put("customerTotal", customerTotal.setScale(2, RoundingMode.HALF_UP));
        result.put("supplierAdvance", supplierList);
        result.put("supplierTotal", supplierTotal.setScale(2, RoundingMode.HALF_UP));
        return result;
    }
}
