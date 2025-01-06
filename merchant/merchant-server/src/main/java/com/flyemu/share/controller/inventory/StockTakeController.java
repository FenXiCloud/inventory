package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.inventory.StockTake;
import com.flyemu.share.service.inventory.StockTakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 盘点单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/stockTake")
@RequiredArgsConstructor
public class StockTakeController {

    private final StockTakeService stockTakeService;

    @GetMapping
    public JsonResult list(Page page, StockTakeService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(stockTakeService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid StockTake stockTake, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        stockTake.setMerchantId(merchantId);
        stockTake.setAccountBookId(accountBookId);
        stockTakeService.save(stockTake);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid StockTake stockTake) {
        stockTakeService.save(stockTake);
        return JsonResult.successful();
    }

    @DeleteMapping("/{stockTakeId}")
    public JsonResult delete(@PathVariable Long stockTakeId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        stockTakeService.delete(stockTakeId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(stockTakeService.select(merchantId, accountBookId));
    }

}
