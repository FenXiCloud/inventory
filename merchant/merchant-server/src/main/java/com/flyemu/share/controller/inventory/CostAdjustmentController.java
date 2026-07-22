package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.inventory.CostAdjustment;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.CostAdjustmentForm;
import com.flyemu.share.service.inventory.CostAdjustmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/costAdjustment")
@RequiredArgsConstructor
public class CostAdjustmentController {

    private final CostAdjustmentService costAdjustmentService;

    @GetMapping
    public JsonResult list(Page page, CostAdjustmentService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(costAdjustmentService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid CostAdjustmentForm costAdjustmentForm, @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        CostAdjustment costAdjustment = costAdjustmentForm.getCostAdjustment();
        TenantScope.bind(costAdjustment, accountDto);
        costAdjustment.setOrderStatus(OrderStatus.已保存);
        costAdjustment.setCreatedBy(adminId);
        return JsonResult.successful(costAdjustmentService.save(costAdjustmentForm, accountDto.getMerchantId()));
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid CostAdjustmentForm costAdjustmentForm, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(costAdjustmentService.save(costAdjustmentForm, accountDto.getMerchantId()));
    }

    @DeleteMapping("/{costAdjustmentId}")
    public JsonResult delete(@PathVariable Long costAdjustmentId, @SaAccountVal AccountDto accountDto) {
        costAdjustmentService.delete(costAdjustmentId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(costAdjustmentService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(costAdjustmentService.load(accountDto.getMerchantId(), id));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        costAdjustmentService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

}
