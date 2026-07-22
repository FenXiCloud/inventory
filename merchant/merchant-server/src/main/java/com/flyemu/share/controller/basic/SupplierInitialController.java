package com.flyemu.share.controller.basic;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.fund.SupplierFlow;
import com.flyemu.share.form.SupplierInitialForm;
import com.flyemu.share.service.fund.SupplierFlowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/supplierInitial")
@RequiredArgsConstructor
public class SupplierInitialController {

    private final SupplierFlowService supplierFlowService;

    @GetMapping
    public JsonResult list(Page page, SupplierFlowService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(supplierFlowService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid SupplierFlow supplierFlow, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(supplierFlow, accountDto);
        supplierFlowService.save(supplierFlow);
        return JsonResult.successful();
    }

    @PostMapping("/batch")
    public JsonResult batch(@RequestBody @Valid SupplierInitialForm form,
                            @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(form, accountDto);
        form.setCreatedBy(adminId);
        supplierFlowService.batch(form);
        return JsonResult.successful();
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        SupplierFlow query = new SupplierFlow();
        TenantScope.bind(query, accountDto);
        query.setId(id);
        return JsonResult.successful(supplierFlowService.getById(query));
    }

    @PutMapping("/batchDelete")
    public JsonResult batchDelete(@RequestBody SupplierInitialForm form, @SaAccountVal AccountDto accountDto) {
        supplierFlowService.batchDelete(form.getIds(), accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SupplierFlow supplierFlow, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(supplierFlow, accountDto);
        supplierFlowService.save(supplierFlow);
        return JsonResult.successful();
    }

    @DeleteMapping("/{supplierFlowId}")
    public JsonResult delete(@PathVariable Long supplierFlowId, @SaAccountVal AccountDto accountDto) {
        supplierFlowService.delete(supplierFlowId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(supplierFlowService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

}
