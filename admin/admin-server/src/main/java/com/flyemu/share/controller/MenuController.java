package com.flyemu.share.controller;

import cn.hutool.core.lang.Assert;
import com.flyemu.share.entity.Menu;
import com.flyemu.share.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
/**
 * @功能描述: 菜单管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
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
