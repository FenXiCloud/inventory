package com.flyemu.share.controller.setting;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.setting.FinanceRel;
import com.flyemu.share.service.setting.FinanceRelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 关联云财务
 * @创建时间: 2025年03月11日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/financeRel")
@RequiredArgsConstructor
@SaCheckLogin
public class FinanceRelController {

    private final FinanceRelService financeRelService;

    @GetMapping
    public JsonResult list(@SaAccountVal AccountDto accountDto, FinanceRelService.Query query) {
        query.setMerchantId(accountDto.getMerchantId());
        query.setAccountBookId(accountDto.getAccountBookId());
        return JsonResult.successful(financeRelService.query(query));
    }


    @PutMapping
    public JsonResult update(@RequestBody @Valid FinanceRel financeRel, @SaAccountVal AccountDto accountDto) {
        financeRel.setMerchantId(accountDto.getMerchantId());
        financeRel.setAccountBookId(accountDto.getAccountBookId());
        financeRelService.save(financeRel);
        return JsonResult.successful();
    }


}
