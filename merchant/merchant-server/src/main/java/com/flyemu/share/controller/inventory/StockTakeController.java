package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.inventory.StockTake;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.StockTakeForm;
import com.flyemu.share.service.inventory.StockTakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public JsonResult save(@RequestBody @Valid StockTakeForm stockTakeForm, @SaAccountBookId Long accountBookId,
                           @SaMerchantId Long merchantId, @SaAdminId Long adminId) {
        StockTake stockTake = stockTakeForm.getStockTake();
        stockTake.setMerchantId(merchantId);
        stockTake.setAccountBookId(accountBookId);
        stockTake.setOrderStatus(OrderStatus.已保存);
        stockTake.setCreatedBy(adminId);
        StockTake take = stockTakeService.save(stockTakeForm, merchantId);
        return JsonResult.successful(take);
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid StockTakeForm stockTakeForm, @SaMerchantId Long merchantId) {
        StockTake take = stockTakeService.save(stockTakeForm, merchantId);
        return JsonResult.successful(take);
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

    @GetMapping("load/{id}")
    public JsonResult load(@SaMerchantId Long merchantId, @PathVariable Long id) {
        return JsonResult.successful(stockTakeService.load(merchantId, id));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        stockTakeService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @GetMapping("/export/{id}")
    public JsonResult export(@PathVariable Long id) {
        return JsonResult.successful(stockTakeService.export(id));
    }
}
