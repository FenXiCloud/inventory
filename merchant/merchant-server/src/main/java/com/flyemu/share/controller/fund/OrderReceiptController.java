package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.service.fund.OrderReceiptService;
import com.flyemu.share.form.OrderReceiptForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        return JsonResult.successful(orderReceiptService.writeOffCandidates(page, query));
    }
}
