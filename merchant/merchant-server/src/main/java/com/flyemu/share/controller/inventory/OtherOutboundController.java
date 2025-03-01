package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.inventory.OtherOutbound;
import com.flyemu.share.enums.ApproveType;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.OtherOutboundForm;
import com.flyemu.share.service.inventory.OtherOutboundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 其他出库单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/otherOutbound")
@RequiredArgsConstructor
public class OtherOutboundController {

    private final OtherOutboundService otherOutboundService;

    @GetMapping
    public JsonResult list(Page page, OtherOutboundService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(otherOutboundService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid OtherOutboundForm otherOutboundForm, @SaAccountBookId Long accountBookId,
                           @SaMerchantId Long merchantId, @SaAdminId Long adminId) {
        OtherOutbound otherOutbound = otherOutboundForm.getOtherOutbound();
        otherOutbound.setMerchantId(merchantId);
        otherOutbound.setAccountBookId(accountBookId);
        otherOutbound.setOrderStatus(OrderStatus.已保存);
        otherOutbound.setCreatedBy(adminId);
        otherOutboundService.save(otherOutboundForm);
        return JsonResult.successful();
    }

    @GetMapping("approve")
    public JsonResult approve(@RequestParam("id") Long id, @RequestParam("type") ApproveType type, @SaAdminId Long adminId) {
        otherOutboundService.approve(id, type, adminId);
        return JsonResult.successful();
    }

    @DeleteMapping("/{otherOutboundId}")
    public JsonResult delete(@PathVariable Long otherOutboundId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        otherOutboundService.delete(otherOutboundId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(otherOutboundService.select(merchantId, accountBookId));
    }

    @DeleteMapping("/delete/{id}")
    public JsonResult delete(@PathVariable Long id) {
        otherOutboundService.delete(id);
        return JsonResult.successful();
    }

    @GetMapping("load/{id}")
    public JsonResult load(@PathVariable Long id) {
        return JsonResult.successful(otherOutboundService.load(id));
    }
}
