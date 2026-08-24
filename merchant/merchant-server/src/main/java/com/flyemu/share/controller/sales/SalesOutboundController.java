package com.flyemu.share.controller.sales;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.hutool.poi.excel.style.StyleUtil;
import com.alibaba.fastjson.JSONObject;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.ImportVoUtil;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.dto.SalesOutboundImportVo;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.SalesOutboundForm;
import com.flyemu.share.service.sales.SalesOutboundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/salesOutbound")
@RequiredArgsConstructor
public class SalesOutboundController {

    private final SalesOutboundService salesOutboundService;

    @GetMapping
    public JsonResult list(Page page, SalesOutboundService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(salesOutboundService.query(page, query));
    }

    @GetMapping("/total")
    public JsonResult queryTotal(SalesOutboundService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(salesOutboundService.queryTotal(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid SalesOutboundForm salesOutboundForm,
            @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(salesOutboundForm.getSalesOutbound(), accountDto);
        salesOutboundForm.getSalesOutbound().setCreatedBy(adminId);
        salesOutboundForm.getSalesOutbound().setCreatedAt(LocalDateTime.now());
        salesOutboundForm.getSalesOutbound().setOrderStatus(OrderStatus.已保存);
        salesOutboundService.save(salesOutboundForm, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SalesOutboundForm salesOutboundForm, @SaAccountVal AccountDto accountDto) {
        salesOutboundService.save(salesOutboundForm, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @DeleteMapping("/{salesOutboundId}")
    public JsonResult delete(@PathVariable Long salesOutboundId, @SaAccountVal AccountDto accountDto) {
        salesOutboundService.delete(salesOutboundId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(salesOutboundService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/load/{orderId}")
    public JsonResult load(@PathVariable Long orderId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(salesOutboundService.load(accountDto.getMerchantId(), orderId));
    }

    @GetMapping("/prefillInvoice/{outboundId}")
    public JsonResult prefillInvoice(@PathVariable Long outboundId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(salesOutboundService.prefillInvoice(accountDto.getMerchantId(), outboundId));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        salesOutboundService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @PostMapping("/importData")
    public JsonResult importData(@RequestParam("file") MultipartFile multipartFile, @SaAccountVal AccountDto accountDto) {
        try {
            List<SalesOutboundImportVo> rows = ImportVoUtil.readImportFile(multipartFile, SalesOutboundImportVo.class);
            if (CollUtil.isEmpty(rows)) {
                throw new ServiceException("excel中未解析到可以导入的数据");
            }
            if (rows.size() > 1000) {
                throw new ServiceException("导入数据不能大于1000行");
            }
            salesOutboundService.importData(rows, accountDto.getMerchantId(), accountDto.getAccountBookId(), accountDto.getAdminId());
            return JsonResult.successful();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }

    // 导出
    @GetMapping("/exportToExcel")
    public ResponseEntity<byte[]> exportToExcel(SalesOutboundService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return toExcel(salesOutboundService.exportList(query));
    }

    private ResponseEntity<byte[]> toExcel(List<JSONObject> exportList) {
        ExcelWriter writer = ExcelUtil.getBigWriter("销售出库单");
        Workbook workbook = writer.getWorkbook();
        Font font = StyleUtil.createFont(workbook, Font.COLOR_RED, (short) 11, null);
        CellStyle cellStyle = StyleUtil.cloneCellStyle(workbook, writer.getStyleSet().getHeadCellStyle());
        cellStyle.setFont(font);
        List<String> strings = Arrays.asList("出库日期", "订单编号", "客户", "销售金额",
                "折扣金额", "折后金额", "数量", "制单人", "状态", "备注");
        writer.writeHeadRow(strings);
        writer.setCurrentRow(1).write(exportList);
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        writer.flush(byteOutputStream);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, ExcelUtil.XLSX_CONTENT_TYPE);
        httpHeaders.setContentDisposition(ContentDisposition.builder("attachment")
                .filename("销售出库单.xlsx", StandardCharsets.UTF_8).build());
        return new ResponseEntity<>(byteOutputStream.toByteArray(), httpHeaders, HttpStatus.OK);
    }

}
