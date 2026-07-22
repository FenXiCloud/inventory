package com.flyemu.share.controller.setting;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.core.lang.Assert;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.setting.Role;
import com.flyemu.share.service.setting.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public JsonResult list(Page page, RoleService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bindMerchant(accountDto, query::setMerchantId);
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
        TenantScope.bindMerchant(accountDto, role::setMerchantId);
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
        TenantScope.bindMerchant(accountDto, role::setMerchantId);
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
    public JsonResult delete(@PathVariable Long roleId, @SaAccountVal AccountDto accountDto) {
        roleService.delete(roleId, accountDto.getMerchantId());
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
