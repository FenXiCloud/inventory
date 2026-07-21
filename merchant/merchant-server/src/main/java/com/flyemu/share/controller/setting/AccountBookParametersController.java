package com.flyemu.share.controller.setting;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.setting.AccountBookParameters;
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

    @GetMapping("load/{accountBookId}")
    public JsonResult load(@SaMerchantId Long merchantId, @PathVariable Integer accountBookId) {
        return JsonResult.successful(accountBookParametersService.load(merchantId, accountBookId));
    }

    @GetMapping
    public JsonResult getCurrent(@SaAccountBookId Long accountBookId) {
        return JsonResult.successful(accountBookParametersService.list(Math.toIntExact(accountBookId)));
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid AccountBookParameters accountBookParameters, @SaAccountBookId Long accountBookId) {
        accountBookParameters.setAccountBookId(Math.toIntExact(accountBookId));
        accountBookParametersService.update(accountBookParameters);
        return JsonResult.successful();
    }

}