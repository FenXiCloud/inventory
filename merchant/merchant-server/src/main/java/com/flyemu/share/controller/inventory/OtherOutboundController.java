package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.inventory.OtherOutbound;
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
    public JsonResult save(@RequestBody @Valid OtherOutbound otherOutbound, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        otherOutbound.setMerchantId(merchantId);
        otherOutbound.setAccountBookId(accountBookId);
        otherOutboundService.save(otherOutbound);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid OtherOutbound otherOutbound) {
        otherOutboundService.save(otherOutbound);
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

}
