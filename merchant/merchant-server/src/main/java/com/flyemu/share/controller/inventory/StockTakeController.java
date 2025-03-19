package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.inventory.StockTake;
import com.flyemu.share.enums.ApproveType;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.StockTakeForm;
import com.flyemu.share.service.inventory.StockTakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

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
        stockTake.setOrderStatus(OrderStatus.未审核);
        stockTake.setCreatedBy(adminId);
        StockTake take = stockTakeService.save(stockTakeForm);
        return JsonResult.successful(take);
    }

    @DeleteMapping("/{stockTakeId}")
    public JsonResult delete(@PathVariable Long stockTakeId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        // 判断是否有关联的订单
        Boolean existOrder = stockTakeService.existOrder(stockTakeId);
        if (existOrder) {
            return JsonResult.failure("已生成对应盘点单据～");
        }
        stockTakeService.delete(stockTakeId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(stockTakeService.select(merchantId, accountBookId));
    }

    @GetMapping("approve")
    public JsonResult approve(@RequestParam("id") Long id, @RequestParam("type") ApproveType type, @SaAdminId Long adminId) {
        if (type.equals(ApproveType.ANTI_AUDIT)) {
            Boolean existOrder = stockTakeService.existOrder(id);
            if (existOrder) {
                return JsonResult.failure("已生成对应盘点单据～");
            }
        }
        stockTakeService.approve(id, type, adminId);
        return JsonResult.successful();
    }

    @GetMapping("approves")
    public JsonResult approves(@RequestParam("ids") String ids, @RequestParam("type") ApproveType type, @SaAdminId Long adminId) {
        if (type.equals(ApproveType.ANTI_AUDIT)) {
            Boolean existOrder = stockTakeService.existOrders(ids);
            if (existOrder) {
                return JsonResult.failure("审核数据中有已生成盘点单据的数据～");
            }
        }
        Arrays.stream(ids.split(",")).map(Long::parseLong).forEach(id -> {
            stockTakeService.approve(id, type, adminId);
        });
        return JsonResult.successful();
    }

    @GetMapping("load/{id}")
    public JsonResult load(@PathVariable Long id) {
        return JsonResult.successful(stockTakeService.load(id));
    }

    @GetMapping("/export/{id}")
    public JsonResult export(@PathVariable Long id) {
        return JsonResult.successful(stockTakeService.export(id));
    }
}
