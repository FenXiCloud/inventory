package com.flyemu.share.controller.setting;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.setting.AccountBookParameters;
import com.flyemu.share.service.setting.AccountBookParametersService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;


/**
 * 账套参数设置控制层
 *
 * @author shuaiqi
 * @since 2025-05-13 15:35:33
 */
@RestController
@RequestMapping("/accountBookParameters")
@AllArgsConstructor
public class AccountBookParametersController {

    private AccountBookParametersService accountBookParametersService;


    @GetMapping("/getByAccountBookId")
    public JsonResult getByAccountBookId(Integer id) {
        return JsonResult.successful(accountBookParametersService.list(id));
    }


    @PostMapping("update")
    public JsonResult update(@RequestBody AccountBookParameters accountBookParameters) {
        accountBookParametersService.update(accountBookParameters);
        return JsonResult.successful();
    }

}

