package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.service.inventory.InventoryCostBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventoryCostBatch")
@RequiredArgsConstructor
public class InventoryCostBatchController {

    private final InventoryCostBatchService inventoryCostBatchService;

    @GetMapping
    public JsonResult list(Page page, InventoryCostBatchService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(inventoryCostBatchService.query(page, query));
    }
}
