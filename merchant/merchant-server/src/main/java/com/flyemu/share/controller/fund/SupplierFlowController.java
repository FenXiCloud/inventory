package com.flyemu.share.controller.fund;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.fund.SupplierFlow;
import com.flyemu.share.service.fund.SupplierFlowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RestController
@RequestMapping("/supplierFlow")
@RequiredArgsConstructor
public class SupplierFlowController {

    private final SupplierFlowService supplierFlowService;

    @GetMapping("/statement")
    public JsonResult statement(Page page,
            @RequestParam Long supplierId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endTime, @SaAccountVal AccountDto accountDto) {

        LocalDateTime startDateTime = startTime.atStartOfDay();
        LocalDateTime endDateTime = endTime.atTime(LocalTime.MAX);

        SupplierFlowService.QueryDTO queryDTO = new SupplierFlowService.QueryDTO();
        TenantScope.bind(queryDTO, accountDto);
        queryDTO.setSupplierId(supplierId);
        queryDTO.setStartTime(startDateTime);
        queryDTO.setEndTime(endDateTime);

        return JsonResult.successful(supplierFlowService.statement(page, queryDTO));
    }

    @GetMapping
    public JsonResult list(SupplierFlowService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(supplierFlowService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid SupplierFlow supplierFlow, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(supplierFlow, accountDto);
        supplierFlowService.save(supplierFlow);
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
