package com.flyemu.share.controller.basic;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.form.ProductComboForm;
import com.flyemu.share.service.basic.ProductComboService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/productCombo")
@RequiredArgsConstructor
public class ProductComboController {

    private final ProductComboService productComboService;

    @GetMapping
    public JsonResult list(Page page,
                           @RequestParam(required = false) String filter,
                           @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(productComboService.list(page, filter, accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(productComboService.load(id, accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @PostMapping
    public JsonResult save(@RequestBody ProductComboForm form, @SaAccountVal AccountDto accountDto) {
        productComboService.save(form, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @DeleteMapping("/{id}")
    public JsonResult delete(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        productComboService.remove(id, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }
}
