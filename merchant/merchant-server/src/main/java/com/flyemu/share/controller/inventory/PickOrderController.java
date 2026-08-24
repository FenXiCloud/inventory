package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.service.inventory.PickOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/pickOrder")
@RequiredArgsConstructor
public class PickOrderController {

    private final PickOrderService pickOrderService;

    /**
     * 分页查询
     */
    @GetMapping
    public JsonResult list(Page page, PickOrderService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(pickOrderService.query(page, query));
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public JsonResult getById(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(pickOrderService.getById(id, accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    /**
     * 根据销售出库单生成拣货单
     */
    @PostMapping("/generate/{salesOutboundId}")
    public JsonResult generateFromSalesOutbound(@PathVariable Long salesOutboundId,
                                                 @SaAdminId Long adminId,
                                                 @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(pickOrderService.generateFromSalesOutbound(
                salesOutboundId, accountDto.getMerchantId(), accountDto.getAccountBookId(), adminId));
    }

    /**
     * 更新拣货状态
     */
    @PutMapping("/status/{id}")
    public JsonResult updateStatus(@PathVariable Long id, @RequestParam Integer status,
                                    @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        pickOrderService.updatePickStatus(id, status, adminId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    /**
     * 更新实拣数量
     */
    @PutMapping("/item/{itemId}")
    public JsonResult updateActualQuantity(@PathVariable Long itemId, @RequestParam BigDecimal actualQuantity,
                                            @SaAccountVal AccountDto accountDto) {
        pickOrderService.updateActualQuantity(itemId, actualQuantity, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    public JsonResult delete(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        pickOrderService.delete(id, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }
}
