package com.flyemu.share.controller;

import cn.hutool.core.lang.Assert;
import com.flyemu.share.entity.setting.Menu;
import com.flyemu.share.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public JsonResult list(MenuService.Query query) {
        return JsonResult.successful(menuService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid Menu menu) {
        Assert.isNull(menu.getId(), "新增菜单Id必须为空~");
        menuService.save(menu);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid Menu menu) {
        Assert.notNull(menu.getId(), "更新菜单Id不允许为空~");
        menuService.save(menu);
        return JsonResult.successful();
    }

    @DeleteMapping("/{menuId}")
    public JsonResult delete(@PathVariable Long menuId) {
        menuService.delete(menuId);
        return JsonResult.successful();
    }

    @PostMapping("/grant")
    public JsonResult grantMerchant(@RequestBody MenuService.MerchantMenuVo vo) {
        menuService.grantMerchant(vo);
        return JsonResult.successful();
    }

    @GetMapping("/query/grant/{merchantId}")
    public JsonResult queryGrantMenu(@PathVariable Long merchantId) {
        return JsonResult.successful(menuService.queryGrantMenu(merchantId));
    }

    @GetMapping("/query/merchant/{merchantId}")
    public JsonResult merchantMenu(@PathVariable Long merchantId) {
        return JsonResult.successful(menuService.queryMerchantMenu(merchantId));
    }
}
