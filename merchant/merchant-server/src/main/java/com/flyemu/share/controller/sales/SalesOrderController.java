package com.flyemu.share.controller.sales;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.ImportVoUtil;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.dto.SalesOrderImportVo;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.SalesOrderForm;
import com.flyemu.share.form.TransferToPurchaseForm;
import com.flyemu.share.service.sales.SalesOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/salesOrder")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    @GetMapping
    public JsonResult list(Page page, SalesOrderService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(salesOrderService.query(page, query));
    }

    @GetMapping("/total")
    public JsonResult queryTotal(SalesOrderService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(salesOrderService.queryTotal(query));
    }

    @GetMapping("/toOutBound")
    public JsonResult listToOutBound(Page page, SalesOrderService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(salesOrderService.queryToOutBound(page, query));
    }

    @PostMapping("/toOutbound/{customerId}")
    public JsonResult toOutbound(@RequestBody List<Long> orderIds, @PathVariable Long customerId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(salesOrderService.loadToOutbound(orderIds, accountDto.getMerchantId(), customerId));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid SalesOrderForm salesOrderForm,
            @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(salesOrderForm.getSalesOrder(), accountDto);
        salesOrderForm.getSalesOrder().setCreatedBy(adminId);
        salesOrderForm.getSalesOrder().setCreatedAt(LocalDateTime.now());
        salesOrderForm.getSalesOrder().setOrderStatus(OrderStatus.已保存);
        salesOrderService.save(salesOrderForm, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SalesOrderForm salesOrderForm, @SaAccountVal AccountDto accountDto) {
        salesOrderService.save(salesOrderForm, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @DeleteMapping("/{salesOrderId}")
    public JsonResult delete(@PathVariable Long salesOrderId, @SaAccountVal AccountDto accountDto) {
        salesOrderService.delete(salesOrderId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(salesOrderService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/load/{orderId}")
    public JsonResult load(@PathVariable Long orderId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(salesOrderService.load(accountDto.getMerchantId(), orderId));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        salesOrderService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @PostMapping("/transferToPurchaseInbound/{salesOrderId}")
    public JsonResult transferToPurchaseInbound(@PathVariable Long salesOrderId,
                                                 @RequestBody TransferToPurchaseForm form,
                                                 @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        Long inboundId = salesOrderService.transferToPurchaseInbound(
                salesOrderId, form.getSupplierId(), form.getItems(),
                accountDto.getMerchantId(), adminId, accountDto.getAccountBookId());
        return JsonResult.successful(inboundId);
    }

    /** 以销定购看板 - 待采购列表 */
    @GetMapping("/pendingPurchase")
    public JsonResult pendingPurchase(Page page, SalesOrderService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(salesOrderService.queryPendingPurchase(page, query));
    }

    /** 批量转采购入库单 */
    @PostMapping("/batchTransferToPurchaseInbound")
    public JsonResult batchTransfer(@RequestBody List<TransferToPurchaseForm> forms,
                                     @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        List<Long> inboundIds = salesOrderService.batchTransferToPurchaseInbound(
                forms, accountDto.getMerchantId(), adminId, accountDto.getAccountBookId());
        return JsonResult.successful(inboundIds);
    }

    /** 批量设置供应商 */
    @PostMapping("/batchSetSupplier")
    public JsonResult batchSetSupplier(@RequestParam Long supplierId,
                                        @RequestBody List<Long> productIds,
                                        @SaAccountVal AccountDto accountDto) {
        salesOrderService.batchSetSupplier(productIds, supplierId, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    /** 订单追踪：查看销售订单的采购-入库-出库全链路 */
    @GetMapping("/track/{salesOrderId}")
    public JsonResult track(@PathVariable Long salesOrderId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(salesOrderService.trackOrder(salesOrderId, accountDto.getMerchantId()));
    }

    @PostMapping("/importData")
    public JsonResult importData(@RequestParam("file") MultipartFile multipartFile, @SaAccountVal AccountDto accountDto) {
        try {
            List<SalesOrderImportVo> rows = ImportVoUtil.readImportFile(multipartFile, SalesOrderImportVo.class);
            if (CollUtil.isEmpty(rows)) throw new ServiceException("excel中未解析到可以导入的数据");
            if (rows.size() > 1000) throw new ServiceException("导入数据不能大于1000行");
            salesOrderService.importData(rows, accountDto.getMerchantId(), accountDto.getAccountBookId(), accountDto.getAdminId());
            return JsonResult.successful();
        } catch (Exception e) { log.error(e.getMessage(), e); throw new ServiceException(e.getMessage()); }
    }

}
