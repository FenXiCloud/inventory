package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.fund.OrderPayment;
import com.flyemu.share.service.fund.OrderPaymentService;
import com.flyemu.share.service.fund.dto.OrderPaymentSaveDTO;
import com.flyemu.share.service.fund.dto.OrderPaymentUpdateDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 付款单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/orderPayment")
@RequiredArgsConstructor
public class OrderPaymentController {

    private final OrderPaymentService orderPaymentService;


    @GetMapping("list")
    public JsonResult list(Page page, OrderPaymentService.Query query,
                           @SaAccountBookId Long accountBookId,
                           @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(orderPaymentService.query(query, page));
    }


    @PostMapping("save")
    public JsonResult save(@RequestBody @Valid OrderPaymentSaveDTO orderPaymentSaveDTO,
                           @SaAccountBookId Long accountBookId,
                           @SaMerchantId Long merchantId) {
        OrderPayment orderPayment = orderPaymentSaveDTO.getOrderPayment();
        orderPayment.setMerchantId(merchantId);
        orderPayment.setAccountBookId(accountBookId);
        orderPaymentService.save(orderPaymentSaveDTO);
        return JsonResult.successful();
    }


    @PostMapping("updateStatus")
    public JsonResult updateStatus(@RequestBody OrderPaymentUpdateDTO orderPayment) {
        orderPaymentService.updateStatus(orderPayment);
        return JsonResult.successful();
    }


    @PostMapping("delete")
    public JsonResult delete(@RequestBody OrderPaymentUpdateDTO orderPayment,
                             @SaAccountBookId Long accountBookId,
                             @SaMerchantId Long merchantId) {
        orderPaymentService.delete(orderPayment.getId(), merchantId, accountBookId);
        return JsonResult.successful();
    }


    @GetMapping("selectById")
    public JsonResult selectById(Long id) {
        return JsonResult.successful(orderPaymentService.selectById(id));
    }


    @GetMapping("writeOffTheOrder")
    public JsonResult aListSalesOrders(Page page, OrderPaymentService.SupplerQuery query) {
        return JsonResult.successful(orderPaymentService.aListSalesOrders(page, query));
    }
}
