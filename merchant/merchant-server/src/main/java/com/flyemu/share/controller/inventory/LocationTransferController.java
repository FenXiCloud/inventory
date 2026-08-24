package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.inventory.LocationTransfer;
import com.flyemu.share.service.inventory.LocationTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/locationTransfer")
@RequiredArgsConstructor
public class LocationTransferController {

    private final LocationTransferService locationTransferService;

    /**
     * 分页查询
     */
    @GetMapping
    public JsonResult list(Page page, LocationTransferService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(locationTransferService.query(page, query));
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public JsonResult getById(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(locationTransferService.getById(id, accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    /**
     * 保存
     */
    @PostMapping
    public JsonResult save(@RequestBody @Valid LocationTransfer locationTransfer, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(locationTransfer, accountDto);
        locationTransfer.setCreatedBy(accountDto.getAdminId());
        return JsonResult.successful(locationTransferService.save(locationTransfer));
    }

    /**
     * 审核
     */
    @PutMapping("/approve/{id}")
    public JsonResult approve(@PathVariable Long id, @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        locationTransferService.approve(id, adminId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    public JsonResult delete(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        locationTransferService.delete(id, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }
}
