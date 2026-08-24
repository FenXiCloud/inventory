package com.flyemu.share.controller.invoice;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.dto.invoice.PagedResponse;
import com.flyemu.share.entity.invoice.InputInvoice;
import com.flyemu.share.service.invoice.InputInvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/invoice/input")
@RequiredArgsConstructor
public class InputInvoiceController {

    private final InputInvoiceService inputInvoiceService;

    @GetMapping
    public JsonResult list(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           @SaAccountVal AccountDto accountDto) {
        Page<InputInvoice> result = inputInvoiceService.list(accountDto.getMerchantId(), page, size);
        return JsonResult.successful(PagedResponse.of(result.getContent(), result.getTotalElements(), PageRequest.of(page, size)));
    }

    @PostMapping
    public JsonResult save(@RequestBody InputInvoice invoice, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inputInvoiceService.save(invoice, accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @DeleteMapping("/{id}")
    public JsonResult delete(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        inputInvoiceService.delete(accountDto.getMerchantId(), id);
        return JsonResult.successful();
    }

    @GetMapping("/aggregation")
    public JsonResult aggregation(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inputInvoiceService.aggregation(accountDto.getMerchantId()));
    }
}
