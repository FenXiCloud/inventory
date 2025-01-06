package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.inventory.OtherInbound;
import com.flyemu.share.service.inventory.OtherInboundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 其他入库单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/otherInbound")
@RequiredArgsConstructor
public class OtherInboundController {

    private final OtherInboundService otherInboundService;

    @GetMapping
    public JsonResult list(Page page, OtherInboundService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(otherInboundService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid OtherInbound otherInbound, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        otherInbound.setMerchantId(merchantId);
        otherInbound.setAccountBookId(accountBookId);
        otherInboundService.save(otherInbound);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid OtherInbound otherInbound) {
        otherInboundService.save(otherInbound);
        return JsonResult.successful();
    }

    @DeleteMapping("/{otherInboundId}")
    public JsonResult delete(@PathVariable Long otherInboundId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        otherInboundService.delete(otherInboundId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(otherInboundService.select(merchantId, accountBookId));
    }

}
