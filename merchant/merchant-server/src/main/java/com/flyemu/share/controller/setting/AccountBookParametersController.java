package com.flyemu.share.controller.setting;

import cn.hutool.core.lang.Dict;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.setting.AccountBookParameters;
import com.flyemu.share.service.inventory.CostingMethodSwitchService;
import com.flyemu.share.service.setting.AccountBookParametersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 账套参数设置
 */
@RestController
@RequestMapping("/accountBookParameters")
@RequiredArgsConstructor
public class AccountBookParametersController {

    private final AccountBookParametersService accountBookParametersService;
    private final CostingMethodSwitchService costingMethodSwitchService;

    @GetMapping("/load/{accountBookId}")
    public JsonResult load(@PathVariable Integer accountBookId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(accountBookParametersService.load(accountDto.getMerchantId(), accountBookId));
    }

    @GetMapping
    public JsonResult getCurrent(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(accountBookParametersService.list(Math.toIntExact(accountDto.getAccountBookId())));
    }

    /**
     * 成本法切换预览：返回期间起始日与本期间已审核出入库流水数，供保存前二次确认文案分级
     */
    @GetMapping("/costSwitchPreview")
    public JsonResult costSwitchPreview(@RequestParam Integer accountBookId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(costingMethodSwitchService.preview(accountDto.getMerchantId(), accountBookId.longValue()));
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid AccountBookParameters accountBookParameters, @SaAccountVal AccountDto accountDto) {
        // 不再强制改写 accountBookId：以参数行自身归属为准（服务内校验商户归属），列表行修改非当前账套参数才不会落错目标
        Dict result = accountBookParametersService.update(accountBookParameters,
                accountDto.getMerchantId(), accountDto.getAdminId());
        return JsonResult.successful(result);
    }

}
