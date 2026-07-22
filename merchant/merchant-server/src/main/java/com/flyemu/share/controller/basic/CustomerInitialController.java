package com.flyemu.share.controller.basic;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.fund.CustomerFlow;
import com.flyemu.share.entity.inventory.InventoryItem;
import com.flyemu.share.form.CustomerInitialForm;
import com.flyemu.share.service.fund.CustomerFlowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customerInitial")
@RequiredArgsConstructor
public class CustomerInitialController {

    private final CustomerFlowService customerFlowService;

    @GetMapping
    public JsonResult list(Page page, CustomerFlowService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(customerFlowService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid CustomerFlow customerFlow, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(customerFlow, accountDto);
        customerFlowService.save(customerFlow);
        return JsonResult.successful();
    }

    @PostMapping("/batch")
    public JsonResult batch(@RequestBody @Valid CustomerInitialForm form,
                            @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(form, accountDto);
        form.setCreatedBy(adminId);
        customerFlowService.batch(form);
        return JsonResult.successful();
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        InventoryItem query = new InventoryItem();
        TenantScope.bind(query, accountDto);
        query.setId(id);
        return JsonResult.successful(customerFlowService.getById(query));
    }

    @PutMapping("/batchDelete")
    public JsonResult batchDelete(@RequestBody CustomerInitialForm form, @SaAccountVal AccountDto accountDto) {
        customerFlowService.batchDelete(form.getIds(), accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid CustomerFlow customerFlow, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(customerFlow, accountDto);
        customerFlowService.save(customerFlow);
        return JsonResult.successful();
    }

    @DeleteMapping("/{customerFlowId}")
    public JsonResult delete(@PathVariable Long customerFlowId, @SaAccountVal AccountDto accountDto) {
        customerFlowService.delete(customerFlowId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(customerFlowService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

}
