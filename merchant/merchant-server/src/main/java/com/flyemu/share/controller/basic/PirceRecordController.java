package com.flyemu.share.controller.basic;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.basic.PriceRecord;
import com.flyemu.share.service.basic.PriceRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 账户收支类别
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/priceRecord")
@RequiredArgsConstructor
public class PirceRecordController {

    private final PriceRecordService priceRecordService;

    @GetMapping
    public JsonResult list(PriceRecordService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(priceRecordService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid PriceRecord priceRecord, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        priceRecord.setMerchantId(merchantId);
        priceRecord.setAccountBookId(accountBookId);
        priceRecordService.save(priceRecord);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid PriceRecord priceRecord) {
        priceRecordService.save(priceRecord);
        return JsonResult.successful();
    }

    @DeleteMapping("/{priceRecordId}")
    public JsonResult delete(@PathVariable Long priceRecordId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        priceRecordService.delete(priceRecordId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(priceRecordService.select(merchantId, accountBookId));
    }

}
