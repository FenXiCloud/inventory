package com.flyemu.share.controller.fund;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.fund.CustomerFlow;
import com.flyemu.share.service.fund.CustomerFlowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RestController
@RequestMapping("/customerFlow")
@RequiredArgsConstructor
public class CustomerFlowController {

    private final CustomerFlowService customerFlowService;

    @GetMapping("/statement")
    public JsonResult statement(Page page,
            @RequestParam Long customerId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endTime, @SaAccountVal AccountDto accountDto) {
        LocalDateTime startDateTime = startTime.atStartOfDay();
        LocalDateTime endDateTime = endTime.atTime(LocalTime.MAX);
        CustomerFlowService.CustomerBillQueryDTO queryDTO = new CustomerFlowService.CustomerBillQueryDTO();
        TenantScope.bind(queryDTO, accountDto);
        queryDTO.setCustomerId(customerId);
        queryDTO.setStartTime(startDateTime);
        queryDTO.setEndTime(endDateTime);
        return JsonResult.successful(customerFlowService.statement(page, queryDTO));

    }

    @GetMapping("/statementSummary")
    public JsonResult statementSummary(
            @RequestParam Long customerId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endTime, @SaAccountVal AccountDto accountDto) {
        LocalDateTime startDateTime = startTime.atStartOfDay();
        LocalDateTime endDateTime = endTime.atTime(LocalTime.MAX);
        return JsonResult.successful(customerFlowService.statementSummary(customerId, startDateTime, endDateTime, accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    /**
     * 调试接口：查看指定客户的所有 CustomerFlow 记录
     */
    @GetMapping("/debug/list")
    public JsonResult debugList(@RequestParam Long customerId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(customerFlowService.debugList(customerId, accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping
    public JsonResult list(CustomerFlowService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(customerFlowService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid CustomerFlow customerFlow, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(customerFlow, accountDto);
        customerFlowService.save(customerFlow);
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
