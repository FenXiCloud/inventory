package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.fund.AccountFlow;
import com.flyemu.share.service.fund.AccountFlowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 资金明细
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/accountFlow")
@RequiredArgsConstructor
public class AccountFlowController {

    private final AccountFlowService accountFlowService;

    @GetMapping
    public JsonResult list(AccountFlowService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(accountFlowService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid AccountFlow accountFlow, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        accountFlow.setMerchantId(merchantId);
        accountFlow.setAccountBookId(accountBookId);
        accountFlowService.save(accountFlow);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid AccountFlow accountFlow) {
        accountFlowService.save(accountFlow);
        return JsonResult.successful();
    }

    @DeleteMapping("/{accountFlowId}")
    public JsonResult delete(@PathVariable Long accountFlowId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        accountFlowService.delete(accountFlowId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(accountFlowService.select(merchantId, accountBookId));
    }

}
