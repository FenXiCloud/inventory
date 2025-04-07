package com.flyemu.share.controller.setting;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.setting.FinanceItemMap;
import com.flyemu.share.service.setting.FinanceItemMapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 财务软件进销存辅助核算映射
 * @创建时间: 2025年03月11日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/financeItemMap")
@RequiredArgsConstructor
@SaCheckLogin
public class FinanceItemMapController {

    private final FinanceItemMapService financeItemMapService;

    @GetMapping
    public JsonResult list(@SaAccountVal AccountDto accountDto, FinanceItemMapService.Query query) {
        query.setMerchantId(accountDto.getMerchantId());
        query.setAccountBookId(accountDto.getAccountBookId());
        return JsonResult.successful(financeItemMapService.query(query));
    }

    @PostMapping("save")
    public JsonResult save(@RequestBody @Valid FinanceItemMap financeItemMap, @SaAccountVal AccountDto accountDto) {
        financeItemMap.setMerchantId(accountDto.getMerchantId());
        financeItemMap.setAccountBookId(accountDto.getAccountBookId());
        financeItemMapService.save(financeItemMap, accountDto);
        return JsonResult.successful();
    }

    @DeleteMapping("/{id}")
    public JsonResult delete(@PathVariable Long id, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        financeItemMapService.delete(id, merchantId, accountBookId);
        return JsonResult.successful();
    }


    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id) {
        return JsonResult.successful(financeItemMapService.load(id));
    }

}
