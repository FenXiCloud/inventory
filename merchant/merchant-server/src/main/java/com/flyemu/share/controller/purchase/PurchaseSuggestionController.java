package com.flyemu.share.controller.purchase;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.service.purchase.PurchaseSuggestionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 采购建议：以销定购看板 + 智能补货 + 一键生成采购单。
 */
@RestController
@RequestMapping("/purchaseSuggestion")
@RequiredArgsConstructor
public class PurchaseSuggestionController {

    private final PurchaseSuggestionService purchaseSuggestionService;

    @GetMapping("/sales-driven")
    public JsonResult salesDriven(@RequestParam(required = false) Integer days, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(purchaseSuggestionService.salesDriven(
                accountDto.getMerchantId(), accountDto.getAccountBookId(), days));
    }

    @GetMapping("/replenishment")
    public JsonResult replenishment(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(purchaseSuggestionService.replenishment(
                accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @PostMapping("/generate")
    public JsonResult generate(@RequestBody GenerateForm form, @SaAccountVal AccountDto accountDto) {
        String orderNo = purchaseSuggestionService.generatePurchaseOrder(
                accountDto.getMerchantId(), accountDto.getAccountBookId(), accountDto.getAdminId(),
                form.getSupplierId(), form.getWarehouseId(), form.getItems());
        return JsonResult.successful(orderNo);
    }

    @Data
    public static class GenerateForm {
        private Long supplierId;
        private Long warehouseId;
        private List<PurchaseSuggestionService.Item> items;
    }
}
