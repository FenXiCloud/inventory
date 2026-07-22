package com.flyemu.share.controller.setting;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
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

    @GetMapping("/load/{accountBookId}")
    public JsonResult load(@PathVariable Integer accountBookId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(accountBookParametersService.load(accountDto.getMerchantId(), accountBookId));
    }

    @GetMapping
    public JsonResult getCurrent(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(accountBookParametersService.list(Math.toIntExact(accountDto.getAccountBookId())));
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid AccountBookParameters accountBookParameters, @SaAccountVal AccountDto accountDto) {
        accountBookParameters.setAccountBookId(Math.toIntExact(accountDto.getAccountBookId()));
        accountBookParametersService.update(accountBookParameters);
        return JsonResult.successful();
    }

}