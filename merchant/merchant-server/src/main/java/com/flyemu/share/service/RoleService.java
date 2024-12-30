package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.RoleSimpleDto;
import com.flyemu.share.entity.*;
import com.flyemu.share.repository.MenuRoleRepository;
import com.flyemu.share.repository.RoleRepository;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @功能描述: 角色管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoleService extends AbsService {

    private final static QRole qRole = QRole.role;

    private final QMenuRole qMenuRole = QMenuRole.menuRole;

    private final RoleRepository roleRepository;


    private final MenuRoleRepository menuRoleRepository;

    private final QAdmin qAdmin = QAdmin.admin;


    public PageResults<Role> query(Page page, Query query) {
        PagedList<Role> fetchPage = bqf.selectFrom(qRole)
                .where(query.builder)
                .orderBy(qRole.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());
        return new PageResults<>(fetchPage, page);
    }


    @Transactional
    public Role save(Role role) {
        if (role.getId() != null) {
            //更新
            Role original = roleRepository.getById(role.getId());
            BeanUtil.copyProperties(role, original, CopyOptions.create().ignoreNullValue());
            return roleRepository.save(original);
        }

        return roleRepository.save(role);
    }


    @Transactional
    public void delete(Long roleId, Long merchantId) {
        long count = bqf.selectFrom(qAdmin).where(qAdmin.roleId.eq(roleId)).fetchCount();
        Assert.isTrue(count == 0, "已被使用不能删除~");
        jqf.delete(qMenuRole).where(qMenuRole.roleId.eq(roleId)).execute();
        jqf.delete(qRole)
                .where(qRole.id.eq(roleId).and(qRole.merchantId.eq(merchantId)))
                .execute();
    }


    public List<RoleSimpleDto> simpleList(Long merchantId) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(qRole.merchantId.eq(merchantId));
        List<RoleSimpleDto> dtos = new ArrayList<>();
        bqf.selectFrom(qRole)
                .select(qRole.id, qRole.name)
                .where(builder)
                .fetch().forEach(tuple -> {
                    RoleSimpleDto dto = new RoleSimpleDto();
                    dto.setId(tuple.get(qRole.id));
                    dto.setName(tuple.get(qRole.name));
                    dtos.add(dto);
                });
        return dtos;
    }


    public List<Long> getMenuRole(Long roleId) {
        return bqf.selectFrom(qMenuRole).select(qMenuRole.menuId).where(qMenuRole.roleId.eq(roleId)).fetch();
    }

    @Transactional
    public void grantMenuRole(Long roleId, List<Long> menus) {
        jqf.delete(qMenuRole)
                .where(qMenuRole.roleId.eq(roleId))
                .execute();
        List<MenuRole> menuRoles = new ArrayList<>();
        menus.forEach(menuId -> {
            MenuRole menuRole = new MenuRole();
            menuRole.setMenuId(menuId);
            menuRole.setRoleId(roleId);
            menuRoles.add(menuRole);
        });
        if (CollUtil.isNotEmpty(menuRoles)) {
            menuRoleRepository.saveAll(menuRoles);
        }
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qRole.merchantId.eq(merchantId));
            }
        }

    }
}
