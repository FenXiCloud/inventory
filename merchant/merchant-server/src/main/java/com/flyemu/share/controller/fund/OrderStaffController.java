package com.flyemu.share.controller.fund;


import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.fund.OrderStaff;
import com.flyemu.share.service.fund.OrderReceiptService;
import com.flyemu.share.service.fund.OrderStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * 控制层
 *
 * @author shuaiqi
 * @since 2025-05-20 11:53:48
 */
@RestController
@RequestMapping("/orderStaff")
@RequiredArgsConstructor
public class OrderStaffController {
    @Autowired
    private OrderStaffService orderStaffService;

    /**
     * 获取列表(分页)
     */
    @GetMapping("/page")
    public JsonResult page(Page page, OrderStaffService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(orderStaffService.query(page, query));
    }

    @GetMapping("/list")
    public JsonResult list(OrderStaffService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(orderStaffService.list(query));
    }

    /**
     * 添加
     */
    @PostMapping("/add")
    public JsonResult add(@RequestBody OrderStaff jxcOrderStaff, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        orderStaffService.add(jxcOrderStaff, merchantId, accountBookId);
        return JsonResult.successful();
    }
//
//
//	/**
//	 * 修改
//	 */
//	@PostMapping("/update")
//	public void update(@RequestBody JxcOrderStaff jxcOrderStaff) {
//		orderStaffService.save(jxcOrderStaff);
//	}
//
//	/**
//	 * 删除
//	 */
//	@PostMapping("/delete")
//	public void delete(Integer id) {
//		orderStaffService.deleteById(id);
//	}

}

