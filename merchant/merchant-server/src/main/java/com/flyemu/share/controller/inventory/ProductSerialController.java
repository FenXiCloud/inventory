package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.service.inventory.ProductSerialService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/productSerial")
@RequiredArgsConstructor
public class ProductSerialController {

    private final ProductSerialService productSerialService;

    @PostMapping
    public JsonResult register(@RequestBody RegisterForm form, @SaAccountVal AccountDto accountDto) {
        int count = productSerialService.register(accountDto.getMerchantId(), accountDto.getAccountBookId(),
                form.getProductId(), form.getWarehouseId(), form.getBatchNumber(),
                form.getSerialNumbers(), form.getInboundDate(), form.getRemark());
        return JsonResult.successful(count);
    }

    @GetMapping
    public JsonResult list(@RequestParam(required = false) String status,
                           @RequestParam(required = false) Long productId,
                           @RequestParam(required = false) String keyword,
                           @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(productSerialService.list(accountDto.getMerchantId(), accountDto.getAccountBookId(),
                status, productId, keyword));
    }

    @PutMapping("/outbound")
    public JsonResult outbound(@RequestBody OutboundForm form, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(productSerialService.outbound(accountDto.getMerchantId(), form.getIds(),
                form.getOutboundOrderId(), form.getOutboundDate()));
    }

    @PutMapping("/scrap")
    public JsonResult scrap(@RequestBody List<Long> ids, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(productSerialService.scrap(accountDto.getMerchantId(), ids));
    }

    @DeleteMapping("/{id}")
    public JsonResult delete(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        productSerialService.delete(accountDto.getMerchantId(), id);
        return JsonResult.successful();
    }

    @Data
    public static class RegisterForm {
        private Long productId;
        private Long warehouseId;
        private String batchNumber;
        private String serialNumbers;
        private LocalDate inboundDate;
        private String remark;
    }

    @Data
    public static class OutboundForm {
        private List<Long> ids;
        private Long outboundOrderId;
        private LocalDate outboundDate;
    }
}
