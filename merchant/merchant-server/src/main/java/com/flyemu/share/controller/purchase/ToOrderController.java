package com.flyemu.share.controller.purchase;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.setting.AccountBookParameters;
import com.flyemu.share.form.CreatePurchaseInboundForm;
import com.flyemu.share.form.ToOrderListQuery;
import com.flyemu.share.service.sales.SalesOrderService;
import com.flyemu.share.service.setting.AccountBookParametersService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 以销定购工作台
 * 菜单路径：采购管理 → 采购单据 → 以销定购看板
 */
@RestController
@RequestMapping("/purchase/to-order")
@RequiredArgsConstructor
public class ToOrderController {

    private final SalesOrderService salesOrderService;
    private final AccountBookParametersService accountBookParametersService;

    /**
     * 获取以销定购建议列表（订单级，一行一订单，展开查看商品明细）
     * 请求参数：pendingStatus（0待处理/1部分采购/2已采购）、startDate、endDate、customerName、orderNo
     */
    @PostMapping("/list")
    public JsonResult list(@RequestBody ToOrderListQuery body, @SaAccountVal AccountDto accountDto) {
        Page page = new Page();
        if (body.getPageNum() != null) page.setPage(body.getPageNum());
        if (body.getPageSize() != null) page.setPageSize(body.getPageSize());
        SalesOrderService.Query query = new SalesOrderService.Query();
        query.setFilter(body.getOrderNo());
        if (body.getStartDate() != null && !body.getStartDate().isEmpty()) {
            query.setStart(LocalDate.parse(body.getStartDate()));
        }
        if (body.getEndDate() != null && !body.getEndDate().isEmpty()) {
            query.setEnd(LocalDate.parse(body.getEndDate()));
        }
        query.setCustomerName(body.getCustomerName());
        return JsonResult.successful(salesOrderService.queryPendingPurchaseOrders(page, query, body.getPendingStatus()));
    }

    /**
     * 生成采购入库单（单订单，系统自动按供应商拆分，返回生成的采购入库单ID列表）
     */
    @PostMapping("/create-purchase-in")
    public JsonResult createPurchaseIn(@RequestBody CreatePurchaseInboundForm form,
                                       @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        List<Long> inboundIds = salesOrderService.createPurchaseInboundBySupplier(
                form, accountDto.getMerchantId(), adminId, accountDto.getAccountBookId());
        return JsonResult.successful(inboundIds);
    }

    /**
     * 获取订单关联的采购状态与完整追踪链
     */
    @GetMapping("/track/{saleOrderId}")
    public JsonResult track(@PathVariable Long saleOrderId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(salesOrderService.trackOrder(saleOrderId, accountDto.getMerchantId()));
    }

    /**
     * 批量设置供应商（更新选中订单商品的默认供应商）
     */
    @PostMapping("/batch-set-supplier")
    public JsonResult batchSetSupplier(@RequestParam Long supplierId,
                                       @RequestBody List<Long> productIds,
                                       @SaAccountVal AccountDto accountDto) {
        salesOrderService.batchSetSupplier(productIds, supplierId, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    /**
     * 以销定购系统参数（前端对话框默认值）
     */
    @GetMapping("/params")
    public JsonResult params(@SaAccountVal AccountDto accountDto) {
        AccountBookParameters p = accountBookParametersService.list(Math.toIntExact(accountDto.getAccountBookId()));
        Map<String, Object> map = new HashMap<>();
        map.put("toOrderDefaultStatus", p != null && p.getToOrderDefaultStatus() != null ? p.getToOrderDefaultStatus() : "待审核");
        map.put("toOrderAutoAudit", p != null && Boolean.TRUE.equals(p.getToOrderAutoAudit()));
        map.put("toOrderAllowPartial", p == null || !Boolean.FALSE.equals(p.getToOrderAllowPartial()));
        return JsonResult.successful(map);
    }
}
