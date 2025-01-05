package com.flyemu.share.controller.basic;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.basic.CustomerLevel;
import com.flyemu.share.service.basic.CustomerLevelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 客户级别管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/customerLevel")
@RequiredArgsConstructor
@SaCheckLogin
public class CustomerLevelController {

    private final CustomerLevelService customerLevelService;

    @GetMapping
    public JsonResult list(@SaAccountVal AccountDto accountDto, CustomerLevelService.Query query) {
        query.setMerchantId(accountDto.getMerchantId());
        query.setAccountBookId(accountDto.getAccountBookId());
        return JsonResult.successful(customerLevelService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid CustomerLevel customerLevel, @SaAccountVal AccountDto accountDto) {
        customerLevel.setMerchantId(accountDto.getMerchantId());
        customerLevel.setAccountBookId(accountDto.getAccountBookId());
        customerLevelService.save(customerLevel);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid CustomerLevel customerLevel, @SaAccountVal AccountDto accountDto) {
        customerLevel.setMerchantId(accountDto.getMerchantId());
        customerLevel.setAccountBookId(accountDto.getAccountBookId());
        customerLevelService.save(customerLevel);
        return JsonResult.successful();
    }

    @DeleteMapping("/{customerLevelId}")
    public JsonResult delete(@PathVariable Long customerLevelId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        customerLevelService.delete(customerLevelId, merchantId,accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(customerLevelService.select(merchantId,accountBookId));
    }
}
