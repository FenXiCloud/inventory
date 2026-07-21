package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.annotation.SaMerchantId;
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

/**
 * @功能描述: 成本调整单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/costAdjustment")
@RequiredArgsConstructor
public class CostAdjustmentController {

    private final CostAdjustmentService costAdjustmentService;

    @GetMapping
    public JsonResult list(Page page, CostAdjustmentService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(costAdjustmentService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid CostAdjustmentForm costAdjustmentForm, @SaMerchantId Long merchantId,
                           @SaAccountBookId Long accountBookId, @SaAdminId Long adminId) {
        CostAdjustment costAdjustment = costAdjustmentForm.getCostAdjustment();
        costAdjustment.setMerchantId(merchantId);
        costAdjustment.setAccountBookId(accountBookId);
        costAdjustment.setOrderStatus(OrderStatus.已保存);
        costAdjustment.setCreatedBy(adminId);
        CostAdjustment adjustment = costAdjustmentService.save(costAdjustmentForm, merchantId);
        return JsonResult.successful(adjustment);
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid CostAdjustmentForm costAdjustmentForm, @SaMerchantId Long merchantId) {
        CostAdjustment adjustment = costAdjustmentService.save(costAdjustmentForm, merchantId);
        return JsonResult.successful(adjustment);
    }

    @DeleteMapping("/{costAdjustmentId}")
    public JsonResult delete(@PathVariable Long costAdjustmentId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        costAdjustmentService.delete(costAdjustmentId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(costAdjustmentService.select(merchantId, accountBookId));
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaMerchantId Long merchantId) {
        return JsonResult.successful(costAdjustmentService.load(merchantId, id));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        costAdjustmentService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

}
