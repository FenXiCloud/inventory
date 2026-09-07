package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.service.fund.OrderReceiptService;
import com.flyemu.share.form.OrderReceiptForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orderReceipt")
@RequiredArgsConstructor
public class OrderReceiptController {

    private final OrderReceiptService orderReceiptService;

    @GetMapping
    public JsonResult list(Page page, OrderReceiptService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(orderReceiptService.query(query, page));
    }

    @GetMapping("/total")
    public JsonResult queryTotal(OrderReceiptService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(orderReceiptService.queryTotal(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid OrderReceiptForm orderReceipt, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(orderReceipt.getOrderReceipt(), accountDto);
        orderReceiptService.save(orderReceipt);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid OrderReceiptForm orderReceipt, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(orderReceipt.getOrderReceipt(), accountDto);
        orderReceiptService.save(orderReceipt);
        return JsonResult.successful();
    }

    @DeleteMapping("/{orderReceiptId}")
    public JsonResult delete(@PathVariable Long orderReceiptId, @SaAccountVal AccountDto accountDto) {
        orderReceiptService.delete(String.valueOf(orderReceiptId), accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(orderReceiptService.load(accountDto.getMerchantId(), id));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        orderReceiptService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @GetMapping("/writeOff")
    public JsonResult writeOff(Page page, OrderReceiptService.SalesQuery query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        PageResults<OrderReceiptService.SalesOrderWithVerification> results = orderReceiptService.writeOffCandidates(page, query);
        Map<String, Object> map = new HashMap<>();
        map.put("results", results.getResults());
        map.put("total", results.getTotal());
        // 期初应收的未收额，供收款单「选择源单」展示期初余额行（未录期初的客户为 0，不展示）
        map.put("opening", orderReceiptService.openingOutstanding(query.getCustomerId(), accountDto.getMerchantId(), accountDto.getAccountBookId()));
        return JsonResult.successful(map);
    }
}
