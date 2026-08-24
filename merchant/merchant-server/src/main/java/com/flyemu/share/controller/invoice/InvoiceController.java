package com.flyemu.share.controller.invoice;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.dto.invoice.InvoiceRequest;
import com.flyemu.share.dto.invoice.PagedResponse;
import com.flyemu.share.entity.invoice.Invoice;
import com.flyemu.share.service.invoice.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/invoice")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    // ========== 发票列表（分页） ==========

    @GetMapping
    public JsonResult list(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(invoiceService.listInvoices(accountDto.getMerchantId()));
    }

    @GetMapping("/paged")
    public JsonResult listPaged(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size,
                                @SaAccountVal AccountDto accountDto) {
        Page<Invoice> result = invoiceService.listInvoicesPaged(accountDto.getMerchantId(), page, size);
        return JsonResult.successful(PagedResponse.of(result.getContent(), result.getTotalElements(), PageRequest.of(page, size)));
    }

    @GetMapping("/blue-paged")
    public JsonResult listBluePaged(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @SaAccountVal AccountDto accountDto) {
        Page<Invoice> result = invoiceService.listBlueInvoicesPaged(accountDto.getMerchantId(), page, size);
        return JsonResult.successful(PagedResponse.of(result.getContent(), result.getTotalElements(), PageRequest.of(page, size)));
    }

    @GetMapping("/red-paged")
    public JsonResult listRedPaged(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @SaAccountVal AccountDto accountDto) {
        Page<Invoice> result = invoiceService.listRedInvoicesPaged(accountDto.getMerchantId(), page, size);
        return JsonResult.successful(PagedResponse.of(result.getContent(), result.getTotalElements(), PageRequest.of(page, size)));
    }

    @GetMapping("/search")
    public JsonResult search(@RequestParam(required = false) String buyerName,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String invoiceType,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size,
                             @SaAccountVal AccountDto accountDto) {
        Page<Invoice> result = invoiceService.searchInvoices(accountDto.getMerchantId(), buyerName, status, invoiceType, page, size);
        return JsonResult.successful(PagedResponse.of(result.getContent(), result.getTotalElements(), PageRequest.of(page, size)));
    }

    @PostMapping("/red/sync")
    public JsonResult syncRed(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(invoiceService.syncRedInvoices(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @PostMapping("/red/manual")
    public JsonResult manualRed(@RequestParam String redInvoiceNo,
                                @RequestParam String redInvoiceCode,
                                @RequestParam String originalInvoiceNo,
                                @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(invoiceService.manualRedInvoice(accountDto.getMerchantId(), accountDto.getAccountBookId(), redInvoiceNo, redInvoiceCode, originalInvoiceNo));
    }

    // ========== 品名查询 ==========

    @GetMapping("/goods-tax")
    public JsonResult searchGoodsTax(@RequestParam String name, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(invoiceService.searchGoodsTax(accountDto.getMerchantId(), name));
    }

    // ========== 蓝字发票 ==========

    @PostMapping("/issue")
    public JsonResult issue(@RequestBody InvoiceRequest request, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(invoiceService.issueInvoice(accountDto.getMerchantId(), accountDto.getAccountBookId(), request));
    }

    @PostMapping("/batch-issue")
    public JsonResult batchIssue(@RequestBody List<Long> salesOutboundIds, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(invoiceService.batchIssue(accountDto.getMerchantId(), accountDto.getAccountBookId(), salesOutboundIds));
    }

    @GetMapping("/{id}")
    public JsonResult get(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(invoiceService.queryInvoice(accountDto.getMerchantId(), id));
    }

    // ========== PDF下载 ==========

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        byte[] pdf = invoiceService.downloadPdf(accountDto.getMerchantId(), id);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "inline; filename=invoice-" + id + ".pdf")
                .body(pdf);
    }

    // ========== 红字发票 ==========

    @GetMapping("/lookup")
    public JsonResult lookup(@RequestParam String invoiceNo, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(invoiceService.lookupByInvoiceNo(accountDto.getMerchantId(), invoiceNo));
    }

    @PostMapping("/red/apply")
    public JsonResult applyRed(@RequestParam(required = false) String originalInvoiceCode,
                               @RequestParam String originalInvoiceNo,
                               @RequestParam(required = false) String originalInvoiceDate,
                               @RequestParam(defaultValue = "01") String redReason,
                               @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(invoiceService.applyRedInvoice(accountDto.getMerchantId(), originalInvoiceCode, originalInvoiceNo, originalInvoiceDate, redReason));
    }

    @PostMapping("/red/issue")
    public JsonResult issueRed(@RequestParam String redInfoNo,
                               @RequestParam(required = false) String originalInvoiceNo,
                               @RequestParam(required = false) String originalInvoiceDate,
                               @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(invoiceService.issueRedInvoice(accountDto.getMerchantId(), accountDto.getAccountBookId(), redInfoNo, originalInvoiceNo, originalInvoiceDate));
    }

    // ========== 统计 ==========

    @GetMapping("/stats")
    public JsonResult stats(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(invoiceService.getStats(accountDto.getMerchantId()));
    }

    @GetMapping("/output-aggregation")
    public JsonResult outputAggregation(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(invoiceService.outputAggregation(accountDto.getMerchantId()));
    }
}
