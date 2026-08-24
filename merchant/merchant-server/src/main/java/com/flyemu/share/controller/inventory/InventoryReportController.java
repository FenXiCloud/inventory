package com.flyemu.share.controller.inventory;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.service.inventory.InventoryReportService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/inventory/report")
@RequiredArgsConstructor
public class InventoryReportController {

    private final InventoryReportService inventoryReportService;

    @GetMapping("/warning")
    public JsonResult warning(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryReportService.warning(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/overstock")
    public JsonResult overstockWarning(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryReportService.overstockWarning(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/warning-summary")
    public JsonResult warningSummary(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryReportService.warningSummary(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/overview")
    public JsonResult overview(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryReportService.overview(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/distribution")
    public JsonResult distribution(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryReportService.distribution(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/virtual-stock")
    public JsonResult virtualStock(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryReportService.virtualStock(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/transfer")
    public JsonResult transferReport(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryReportService.transferReport(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/batch")
    public JsonResult batchReport(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryReportService.batchReport(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/loss-gain")
    public JsonResult lossGainReport(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryReportService.lossGainReport(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/expiry")
    public JsonResult expiryWarning(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryReportService.expiryWarning(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/shelf-life")
    public JsonResult shelfLifeList(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryReportService.shelfLifeList(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/batch-available")
    public JsonResult batchAvailable(@RequestParam Long productId, @RequestParam Long warehouseId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryReportService.batchAvailable(
                accountDto.getMerchantId(), accountDto.getAccountBookId(), productId, warehouseId));
    }

    @PutMapping("/shelf-life/{itemId}")
    public JsonResult updateShelfLife(@PathVariable Long itemId, @RequestBody ShelfLifeForm form, @SaAccountVal AccountDto accountDto) {
        inventoryReportService.updateShelfLife(accountDto.getMerchantId(), accountDto.getAccountBookId(),
                itemId, form.getBatchNumber(), form.getProductionDate(), form.getExpiryDate());
        return JsonResult.successful();
    }

    @Data
    public static class ShelfLifeForm {
        private String batchNumber;
        private LocalDate productionDate;
        private LocalDate expiryDate;
    }
}
