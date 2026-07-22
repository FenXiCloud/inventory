package com.flyemu.share.controller.basic;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.basic.PriceRecord;
import com.flyemu.share.form.ProductForm;
import com.flyemu.share.form.ProductPriceCellForm;
import com.flyemu.share.service.basic.PriceRecordService;
import com.flyemu.share.service.basic.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/priceRecord")
@RequiredArgsConstructor
public class PriceRecordController {

    private final PriceRecordService priceRecordService;

    @GetMapping
    public JsonResult list(Page page, PriceRecordService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(priceRecordService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid PriceRecord priceRecord, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(priceRecord, accountDto);
        priceRecordService.save(priceRecord);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid PriceRecord priceRecord, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(priceRecord, accountDto);
        priceRecordService.save(priceRecord);
        return JsonResult.successful();
    }

    @DeleteMapping("/{priceRecordId}")
    public JsonResult delete(@PathVariable Long priceRecordId, @SaAccountVal AccountDto accountDto) {
        priceRecordService.delete(priceRecordId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(priceRecordService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/product")
    public JsonResult productList(Page page,
                                  ProductService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(priceRecordService.productList(page, query));
    }

    @PostMapping("/product")
    public JsonResult productSave(@RequestBody ProductForm productForm, @SaAccountVal AccountDto accountDto) {
        priceRecordService.productSave(productForm, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @PostMapping("/product/cell")
    public JsonResult productCellSave(@RequestBody @Valid ProductPriceCellForm form, @SaAccountVal AccountDto accountDto) {
        priceRecordService.productCellSave(form, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/price")
    public JsonResult price(Page page, PriceRecordService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(priceRecordService.showPrice(page, query));
    }

    @GetMapping("/purchasePrice")
    public JsonResult purchasePrice(PriceRecordService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(priceRecordService.showPurchasePrice(query));
    }

}
