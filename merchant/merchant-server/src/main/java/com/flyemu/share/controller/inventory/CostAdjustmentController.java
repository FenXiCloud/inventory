package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.inventory.CostAdjustment;
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
    public JsonResult save(@RequestBody @Valid CostAdjustment costAdjustment, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        costAdjustment.setMerchantId(merchantId);
        costAdjustment.setAccountBookId(accountBookId);
        costAdjustmentService.save(costAdjustment);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid CostAdjustment costAdjustment) {
        costAdjustmentService.save(costAdjustment);
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

}
