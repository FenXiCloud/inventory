package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.fund.OrderStaff;
import com.flyemu.share.service.fund.OrderStaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 业务员
 */
@RestController
@RequestMapping("/orderStaff")
@RequiredArgsConstructor
public class OrderStaffController {

    private final OrderStaffService orderStaffService;

    @GetMapping
    public JsonResult list(Page page, OrderStaffService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(orderStaffService.query(page, query));
    }

    @GetMapping("select")
    public JsonResult select(OrderStaffService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(orderStaffService.select(query));
    }

    @GetMapping("load/{id}")
    public JsonResult load(@PathVariable Integer id, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        return JsonResult.successful(orderStaffService.load(id, merchantId, accountBookId));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid OrderStaff orderStaff, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        orderStaff.setMerchantId(merchantId);
        orderStaff.setAccountBookId(accountBookId);
        orderStaffService.save(orderStaff);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid OrderStaff orderStaff, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        orderStaff.setMerchantId(merchantId);
        orderStaff.setAccountBookId(accountBookId);
        orderStaffService.save(orderStaff);
        return JsonResult.successful();
    }

    @DeleteMapping("/{id}")
    public JsonResult delete(@PathVariable Integer id, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        orderStaffService.delete(id, merchantId, accountBookId);
        return JsonResult.successful();
    }
}
