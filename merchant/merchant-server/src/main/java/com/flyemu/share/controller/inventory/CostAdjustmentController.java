package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.inventory.CostAdjustment;
import com.flyemu.share.entity.inventory.OtherOutbound;
import com.flyemu.share.enums.ApproveType;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.CostAdjustmentForm;
import com.flyemu.share.service.inventory.CostAdjustmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public JsonResult list(Page page, CostAdjustmentService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(costAdjustmentService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid CostAdjustmentForm costAdjustmentForm, @SaAccountBookId Long accountBookId,
                           @SaMerchantId Long merchantId, @SaAdminId Long adminId) {
        CostAdjustment costAdjustment = costAdjustmentForm.getCostAdjustment();
        costAdjustment.setMerchantId(merchantId);
        costAdjustment.setAccountBookId(accountBookId);
        costAdjustment.setOrderStatus(OrderStatus.已保存);
        costAdjustment.setCreatedBy(adminId);
        costAdjustmentService.save(costAdjustmentForm);
        return JsonResult.successful();
    }

    @DeleteMapping("/{costAdjustmentId}")
    public JsonResult delete(@PathVariable Long costAdjustmentId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        costAdjustmentService.delete(costAdjustmentId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(costAdjustmentService.select(merchantId, accountBookId));
    }

    @GetMapping("approve")
    public JsonResult approve(@RequestParam("id") Long id, @RequestParam("type") ApproveType type, @SaAdminId Long adminId) {
        costAdjustmentService.approve(id, type, adminId);
        return JsonResult.successful();
    }


    @GetMapping("load/{id}")
    public JsonResult load(@PathVariable Long id) {
        return JsonResult.successful(costAdjustmentService.load(id));
    }

}
