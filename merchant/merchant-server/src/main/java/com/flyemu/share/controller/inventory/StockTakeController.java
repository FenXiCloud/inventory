package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.inventory.StockTake;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.StockTakeForm;
import com.flyemu.share.service.inventory.StockTakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stockTake")
@RequiredArgsConstructor
public class StockTakeController {

    private final StockTakeService stockTakeService;

    @GetMapping
    public JsonResult list(Page page, StockTakeService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(stockTakeService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid StockTakeForm stockTakeForm, @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        StockTake stockTake = stockTakeForm.getStockTake();
        TenantScope.bind(stockTake, accountDto);
        stockTake.setOrderStatus(OrderStatus.已保存);
        stockTake.setCreatedBy(adminId);
        return JsonResult.successful(stockTakeService.save(stockTakeForm, accountDto.getMerchantId()));
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid StockTakeForm stockTakeForm, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(stockTakeService.save(stockTakeForm, accountDto.getMerchantId()));
    }

    @DeleteMapping("/{stockTakeId}")
    public JsonResult delete(@PathVariable Long stockTakeId, @SaAccountVal AccountDto accountDto) {
        stockTakeService.delete(stockTakeId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(stockTakeService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(stockTakeService.load(accountDto.getMerchantId(), id));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        stockTakeService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @GetMapping("/export/{id}")
    public JsonResult export(@PathVariable Long id) {
        return JsonResult.successful(stockTakeService.export(id));
    }
}
