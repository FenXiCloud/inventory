package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.service.fund.AccountTransferService;
import com.flyemu.share.form.AccountTransferForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @功能描述: 转账单管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/accountTransfer")
@RequiredArgsConstructor
public class AccountTransferController {

    private final AccountTransferService accountTransferService;

    @GetMapping
    public JsonResult list(Page page, AccountTransferService.Query query,
                           @SaAccountBookId Long accountBookId,
                           @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(accountTransferService.query(page, query));
    }

    @GetMapping("/total")
    public JsonResult queryTotal(AccountTransferService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(accountTransferService.queryTotal(query));
    }



    @PostMapping
    public JsonResult save(@RequestBody @Valid AccountTransferForm dto,
                           @SaAccountBookId Long accountBookId,
                           @SaMerchantId Long merchantId) {
        dto.getOrder().setMerchantId(merchantId);
        dto.getOrder().setAccountBookId(accountBookId);
        accountTransferService.save(dto);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid AccountTransferForm dto,
                             @SaAccountBookId Long accountBookId,
                             @SaMerchantId Long merchantId) {
        dto.getOrder().setMerchantId(merchantId);
        dto.getOrder().setAccountBookId(accountBookId);
        accountTransferService.save(dto);
        return JsonResult.successful();
    }

    @DeleteMapping("/{accountTransferId}")
    public JsonResult delete(@PathVariable Long accountTransferId,
                             @SaAccountBookId Long accountBookId,
                             @SaMerchantId Long merchantId) {
        accountTransferService.delete(String.valueOf(accountTransferId), merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("load/{id}")
    public JsonResult load(@SaMerchantId Long merchantId, @PathVariable Long id) {
        return JsonResult.successful(accountTransferService.load(merchantId, id));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        accountTransferService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }
}
