package com.flyemu.share.controller.basic;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.hutool.poi.excel.style.StyleUtil;
import com.alibaba.fastjson.JSONObject;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.common.ImportVoUtil;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.dto.CustomerImportVo;
import com.flyemu.share.entity.basic.Customer;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.service.basic.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);
    private final CustomerService customerService;

    @GetMapping
    public JsonResult list(Page page, CustomerService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(customerService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid Customer customer, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(customer, accountDto);
        customerService.save(customer, accountDto.getMerchant().getCode());
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid Customer customer, @SaAccountVal AccountDto accountDto) {
        customerService.save(customer, accountDto.getMerchant().getCode());
        return JsonResult.successful();
    }

    @PutMapping("/credit-limit")
    public JsonResult updateCreditLimit(@RequestBody Customer customer, @SaAccountVal AccountDto accountDto) {
        customerService.updateCreditLimit(customer.getId(), customer.getCreditLimit(), accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @DeleteMapping("/{customerId}")
    public JsonResult delete(@PathVariable Long customerId, @SaAccountVal AccountDto accountDto) {
        customerService.delete(customerId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(customerService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/product/select/{customerId}")
    public JsonResult selectProduct(@PathVariable Long customerId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(customerService.selectProducts(customerId, accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    // 导入
    @PostMapping("/importData")
    public JsonResult importData(@RequestParam("file") MultipartFile multipartFile, @SaAccountVal AccountDto accountDto) {
        try {
            System.setProperty("javax.xml.parsers.SAXParserFactory", "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");

            ExcelReader reader = ExcelUtil.getReader(multipartFile.getInputStream());
            ImportVoUtil.setHeaderAlias(CustomerImportVo.class, reader);
            List<CustomerImportVo> rows = reader.readAll(CustomerImportVo.class);
            Assert.isFalse(CollUtil.isEmpty(rows), "excel中未解析到可以导入的数据");
            if (CollUtil.isEmpty(rows)) {
                throw new ServiceException("excel中未解析到可以导入的数据");
            }
            if (rows.size() > 1000) {
                throw new ServiceException("导入数据不能大于1000行");
            }
            customerService.importData(rows, accountDto.getMerchantId(), accountDto.getAccountBookId());
            return JsonResult.successful();
        } catch (IllegalArgumentException ae) {
            log.error(ae.getMessage());
            throw new ServiceException(ae.getMessage());
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }

    // 导出
    @GetMapping("/exportToExcel")
    public ResponseEntity<byte[]> exportToExcel(@SaAccountVal AccountDto accountDto) {
        return toExcel(customerService.exportList(accountDto.getMerchantId(), null, null));
    }

    private ResponseEntity<byte[]> toExcel(List<JSONObject> exportList) {
        ExcelWriter writer = ExcelUtil.getBigWriter("客户档案");
        Workbook workbook = writer.getWorkbook();
        Font font = StyleUtil.createFont(workbook, Font.COLOR_RED, (short) 11, null);
        CellStyle cellStyle = StyleUtil.cloneCellStyle(workbook, writer.getStyleSet().getHeadCellStyle());
        cellStyle.setFont(font);
        List<String> strings = Arrays.asList("分类名称", "客户编码", "客户名称", "客户联系人",
                "联系电话", "备注", "客户等级", "应收账款", "客户状态");
        writer.writeHeadRow(strings);

        writer.setCurrentRow(1).write(exportList);
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        writer.flush(byteOutputStream);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, ExcelUtil.XLSX_CONTENT_TYPE);
        httpHeaders.setContentDisposition(ContentDisposition.builder("attachment")
                .filename("客户档案.xlsx", StandardCharsets.UTF_8).build());
        return new ResponseEntity<>(byteOutputStream.toByteArray(),
                httpHeaders,
                HttpStatus.OK);
    }

}
