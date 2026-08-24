package com.flyemu.share.controller.invoice;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.invoice.InvoiceSellerConfig;
import com.flyemu.share.service.invoice.InvoiceSellerConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/invoice/seller-config")
@RequiredArgsConstructor
public class InvoiceSellerConfigController {

    private final InvoiceSellerConfigService service;

    @GetMapping
    public JsonResult get(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(service.get(accountDto.getMerchantId()));
    }

    @PostMapping
    public JsonResult save(@RequestBody InvoiceSellerConfig config, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(service.save(config, accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }
}
