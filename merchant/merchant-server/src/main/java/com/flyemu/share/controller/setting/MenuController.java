package com.flyemu.share.controller.setting;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.setting.Menu;
import com.flyemu.share.service.setting.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public JsonResult list(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(menuService.query(accountDto.getMerchantId()));
    }

    @GetMapping("/merchant")
    public JsonResult merchantMenu(Menu.MenuGroup menuGroup, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(menuService.merchantMenu(accountDto.getMerchantId(), menuGroup));
    }
}
