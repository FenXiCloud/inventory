package com.flyemu.share.controller.basic;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.basic.Unit;
import com.flyemu.share.service.basic.UnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 单位
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/unit")
@RequiredArgsConstructor
@SaCheckLogin
public class UnitController {

    private final UnitService unitService;

    @GetMapping
    public JsonResult list(@SaAccountVal AccountDto accountDto, UnitService.Query query) {
        query.setMerchantId(accountDto.getMerchantId());
        query.setAccountBookId(accountDto.getAccountBookId());
        return JsonResult.successful(unitService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid Unit unit, @SaAccountVal AccountDto accountDto) {
        unit.setMerchantId(accountDto.getMerchantId());
        unit.setAccountBookId(accountDto.getAccountBookId());
        unitService.save(unit);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid Unit unit, @SaAccountVal AccountDto accountDto) {
        unit.setMerchantId(accountDto.getMerchantId());
        unit.setAccountBookId(accountDto.getAccountBookId());
        unitService.save(unit);
        return JsonResult.successful();
    }

    @DeleteMapping("/{unitId}")
    public JsonResult delete(@PathVariable Long unitId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        unitService.delete(unitId, merchantId,accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(unitService.select(merchantId,accountBookId));
    }
}
