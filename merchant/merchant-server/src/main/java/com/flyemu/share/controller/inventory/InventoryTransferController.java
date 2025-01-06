package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.inventory.InventoryTransfer;
import com.flyemu.share.service.inventory.InventoryTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 调拨单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/inventoryTransfer")
@RequiredArgsConstructor
public class InventoryTransferController {

    private final InventoryTransferService inventoryTransferService;

    @GetMapping
    public JsonResult list(Page page, InventoryTransferService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(inventoryTransferService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid InventoryTransfer inventoryTransfer, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        inventoryTransfer.setMerchantId(merchantId);
        inventoryTransfer.setAccountBookId(accountBookId);
        inventoryTransferService.save(inventoryTransfer);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid InventoryTransfer inventoryTransfer) {
        inventoryTransferService.save(inventoryTransfer);
        return JsonResult.successful();
    }

    @DeleteMapping("/{inventoryTransferId}")
    public JsonResult delete(@PathVariable Long inventoryTransferId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        inventoryTransferService.delete(inventoryTransferId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(inventoryTransferService.select(merchantId, accountBookId));
    }

}
