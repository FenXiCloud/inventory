package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.fund.AccountTransfer;
import com.flyemu.share.service.fund.AccountTransferService;
import com.flyemu.share.service.fund.dto.AccountTransferDTO;
import com.flyemu.share.service.fund.dto.OrderPaymentUpdateDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("list")
    public JsonResult list(Page page, AccountTransferService.Query query,
                           @SaAccountBookId Long accountBookId,
                           @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(accountTransferService.query(page, query));
    }


    @PostMapping("save")
    public JsonResult save(@RequestBody @Valid AccountTransferDTO dto,
                           @SaAccountBookId Long accountBookId,
                           @SaMerchantId Long merchantId) {
        dto.getOrder().setMerchantId(merchantId);
        dto.getOrder().setAccountBookId(accountBookId);
        accountTransferService.save(dto);
        return JsonResult.successful();
    }


    @DeleteMapping("delete")
    public JsonResult delete(@RequestBody OrderPaymentUpdateDTO dto,
                             @SaAccountBookId Long accountBookId,
                             @SaMerchantId Long merchantId) {
        accountTransferService.delete(dto.getId(), merchantId, accountBookId);
        return JsonResult.successful();
    }


    @PostMapping("updateStatus")
    public JsonResult updateStatus(@RequestBody OrderPaymentUpdateDTO dto) {
        accountTransferService.updateStatus(dto);
        return JsonResult.successful();
    }

    @GetMapping("selectById")
    public JsonResult selectById(Long id) {
        return JsonResult.successful(accountTransferService.selectById(id));
    }
}
