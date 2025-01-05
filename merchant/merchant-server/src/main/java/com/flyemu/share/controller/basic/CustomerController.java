package com.flyemu.share.controller.basic;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.basic.Customer;
import com.flyemu.share.service.basic.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 客户管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public JsonResult list(Page page, CustomerService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(customerService.query(page,query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid Customer customer, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId, @SaAccountVal AccountDto accountDto) {
        customer.setMerchantId(merchantId);
        customer.setAccountBookId(accountBookId);
        customerService.save(customer,accountDto.getMerchant().getCode());
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid Customer customer, @SaAccountVal AccountDto accountDto) {
        customerService.save(customer,accountDto.getMerchant().getCode());
        return JsonResult.successful();
    }

    @DeleteMapping("/{customerId}")
    public JsonResult delete(@PathVariable Long customerId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        customerService.delete(customerId,merchantId,accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId,@SaAccountBookId Long accountBookId) {
        return JsonResult.successful(customerService.select(merchantId,accountBookId));
    }

}
