package com.flyemu.share.controller.basic;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.basic.ProductAttribute;
import com.flyemu.share.service.basic.ProductAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 辅助属性字典（颜色/尺码等多规格属性）
 */
@RestController
@RequestMapping("/productAttribute")
@RequiredArgsConstructor
public class ProductAttributeController {

    private final ProductAttributeService productAttributeService;

    @GetMapping
    public JsonResult list(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(productAttributeService.list(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @PostMapping
    public JsonResult save(@RequestBody ProductAttribute attribute, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(productAttributeService.save(attribute, accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @PutMapping
    public JsonResult update(@RequestBody ProductAttribute attribute, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(productAttributeService.save(attribute, accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @DeleteMapping("/{id}")
    public JsonResult remove(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        productAttributeService.remove(id, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }
}
