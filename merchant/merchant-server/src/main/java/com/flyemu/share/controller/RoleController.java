package com.flyemu.share.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.core.lang.Assert;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.Role;
import com.flyemu.share.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * @功能描述: 角色
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
@SaCheckLogin
public class RoleController {

    private final RoleService roleService;

    /**
     * 分页查询
     *
     * @param page
     * @return
     */
    @GetMapping
    public JsonResult list(Page page, @SaAccountVal AccountDto accountDto, RoleService.Query query) {
        query.setMerchantId(accountDto.getMerchantId());
        return JsonResult.successful(roleService.query(page, query));
    }

    /**
     * 简单的列表
     *
     * @return
     */
    @GetMapping("/simple")
    public JsonResult simpleList(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(roleService.simpleList(accountDto.getMerchantId()));
    }

    /**
     * 新增角色
     *
     * @param role
     * @return
     */
    @PostMapping
    public JsonResult save(@RequestBody @Valid Role role, @SaAccountVal AccountDto accountDto) {
        Assert.isNull(role.getId(), "新增角色Id必须为空~");
        role.setMerchantId(accountDto.getMerchantId());
        roleService.save(role);
        return JsonResult.successful();
    }

    /**
     * 更新角色
     *
     * @param role
     * @return
     */
    @PutMapping
    public JsonResult update(@RequestBody @Valid Role role, @SaAccountVal AccountDto accountDto) {
        Assert.notNull(role.getId(), "更新角色Id不允许为空~");
        role.setMerchantId(accountDto.getMerchantId());
        roleService.save(role);
        return JsonResult.successful();
    }

    /**
     * 删除角色
     *
     * @param roleId
     * @return
     */
    @DeleteMapping("/{roleId}")
    public JsonResult delete(@PathVariable Long roleId, @SaMerchantId Long merchantId) {
        roleService.delete(roleId, merchantId);
        return JsonResult.successful();
    }

    @GetMapping("/grant/menu/{roleId}")
    public JsonResult getMenuRole(@PathVariable Long roleId) {
        return JsonResult.successful(roleService.getMenuRole(roleId));
    }

    @PostMapping("/grant/{roleId}")
    public JsonResult grantMenuRole(@PathVariable Long roleId, @RequestBody List<Long> menus) {
        roleService.grantMenuRole(roleId, menus);
        return JsonResult.successful();
    }

}
