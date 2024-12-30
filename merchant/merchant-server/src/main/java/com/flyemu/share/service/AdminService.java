package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.dto.AdminDto;
import com.flyemu.share.dto.MenuDto;
import com.flyemu.share.entity.setting.*;
import com.flyemu.share.repository.AdminRepository;
import com.flyemu.share.repository.RoleRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @功能描述: 管理员
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminService extends AbsService {
    private final RoleRepository roleRepository;

    private final static QAdmin qAdmin = QAdmin.admin;

    private final static QRole qRole = QRole.role;

    private final static QMenuRole qMenuRole = QMenuRole.menuRole;

    private final static QMenu qMenu = QMenu.menu;

    private final static QMerchant qMerchant = QMerchant.merchant;

    private final static QAccountBook qAccountBook = QAccountBook.accountBook;

    private final static QMerchantMenu qMerchantMenu = QMerchantMenu.merchantMenu;

    private final AdminRepository adminRepository;


    public List<AdminDto> query(Long merchantId, Query query) {
        List<Tuple> fetchPage = bqf.selectFrom(qAdmin)
                .join(qRole).on(qAdmin.roleId.eq(qRole.id))
                .select(qAdmin, qRole.name)
                .where(query.builder.and(qAdmin.merchantId.eq(merchantId)))
                .orderBy(qAdmin.id.desc())
                .fetchJoin()
                .fetch();
        List<AdminDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            AdminDto adminDto = BeanUtil.toBean(tuple.get(qAdmin), AdminDto.class);
            adminDto.setRoleName(tuple.get(qRole.name));
            dtos.add(adminDto);
        });
        dtos.sort(Comparator.comparing(AdminDto::getId).reversed());
        return dtos;
    }

    @Transactional
    public Admin save(Admin admin) {
        if (admin.getId() != null) {
            //更新
            Admin original = adminRepository.getById(admin.getId());
            //判断是否更新了手机号码，更新手机号需要检查重复
            if (StrUtil.isNotEmpty(admin.getMobile()) && !original.getMobile().equalsIgnoreCase(admin.getMobile())) {
                long count = bqf.selectFrom(qAdmin)
                        .where(qAdmin.mobile.eq(admin.getMobile())
                                .and(qAdmin.merchantId.eq(admin.getMerchantId()))
                                .and(qAdmin.id.ne(admin.getId()))
                        ).fetchCount();
                Assert.isTrue(count == 0, "手机号已被其他管理员使用~");
            }
            BeanUtil.copyProperties(admin, original, CopyOptions.create().ignoreNullValue());
            return adminRepository.save(original);
        }
        admin.setSystemDefault(false);
        admin.setEnabled(true);
        long count = bqf.selectFrom(qAdmin)
                .where(qAdmin.mobile.eq(admin.getMobile())
                        .and(qAdmin.merchantId.eq(admin.getMerchantId()))
                ).fetchCount();
        Assert.isTrue(count == 0, "手机号已被其他管理员使用~");
        if (StrUtil.isNotEmpty(admin.getUsername())) {
            //手动设置了账号，需要检查重复
            count = bqf.selectFrom(qAdmin)
                    .where(qAdmin.username.eq(admin.getUsername()))
                    .fetchCount();
            Assert.isTrue(count == 0, admin.getUsername() + "账号已存在~");
        }
        //没有设置密码，默认使用手机号后6位
        if (StrUtil.isEmpty(admin.getPassword())) {
            admin.setPassword(DigestUtil.bcrypt(admin.getMobile().substring(5)));
        } else {
            admin.setPassword(DigestUtil.bcrypt(admin.getPassword()));
        }

        return adminRepository.save(admin);
    }

    @Transactional
    public void delete(Long adminId, Long merchantId) {
        jqf.delete(qAdmin)
                .where(qAdmin.id.eq(adminId).and(qAdmin.systemDefault.isFalse()).and(qAdmin.merchantId.eq(merchantId)))
                .execute();
    }

    /**
     * 登录
     *
     * @param username
     * @param password
     */
    public AccountDto login(String username, String password) {
        Admin admin = jqf.selectFrom(qAdmin)
                .where(qAdmin.username.eq(username)).fetchFirst();
        Assert.notNull(admin, "账号错误~");
        Assert.isTrue(DigestUtil.bcryptCheck(password, admin.getPassword()), "密码错误~");

        Merchant merchant = jqf.selectFrom(qMerchant).where(qMerchant.id.eq(admin.getMerchantId())).fetchFirst();

        Role role = bqf.selectFrom(qRole).where(qRole.id.eq(admin.getRoleId())).fetchFirst();

        AccountBook accountBook = bqf.selectFrom(qAccountBook).where(qAccountBook.merchantId.eq(admin.getMerchantId()).and(qAccountBook.current.isTrue())).fetchFirst();

        return new AccountDto(admin, merchant, role);


    }

    @Transactional
    public void updatePassword(Long adminId, String oldPassword, String newPassword, Long merchantId) {
        Admin admin = jqf.selectFrom(qAdmin).where(qAdmin.id.eq(adminId).and(qAdmin.merchantId.eq(merchantId))).fetchFirst();
        Assert.isTrue(DigestUtil.bcryptCheck(oldPassword, admin.getPassword()), "原密码错误~");
        admin.setPassword(DigestUtil.bcrypt(newPassword));
        adminRepository.save(admin);
    }

    @Transactional
    public void resetPassword(Long adminId, Long merchantId) {
        Admin admin = jqf.selectFrom(qAdmin).where(qAdmin.id.eq(adminId).and(qAdmin.merchantId.eq(merchantId))).fetchFirst();
        admin.setPassword(DigestUtil.bcrypt(admin.getMobile().substring(5)));
        adminRepository.save(admin);
    }

    public List<MenuDto> loadMenu(Long merchantId, Role role) {
        List<Tuple> tuples;
        //如果是系统默认角色，则获取所有菜单权限
        if (role.getSystemDefault()) {
            tuples = jqf.selectFrom(qMerchantMenu)
                    .join(qMenu).on(qMerchantMenu.menuId.eq(qMenu.id))
                    .select(qMenu.id, qMenu.name, qMenu.component, qMenu.iconCls, qMenu.parentId, qMenu.menuGroup)
                    .where(qMerchantMenu.merchantId.eq(merchantId).and(qMenu.enabled.isTrue()).and(qMenu.menuModule.eq(Menu.MenuModule.MERCHANT)).and(qMenu.menuType.eq(Menu.MenuType.MENU)))
                    .orderBy(qMenu.menuGroup.asc(), qMenu.pos.asc())
                    .fetch();
        } else {
            tuples = bqf.selectFrom(qMenuRole)
                    .join(qMenu).on(qMenuRole.menuId.eq(qMenu.id))
                    .select(qMenu.id, qMenu.name, qMenu.component, qMenu.iconCls, qMenu.parentId, qMenu.menuGroup)
                    .where(qMenuRole.roleId.eq(role.getId())
                            .and(qMenu.menuModule.eq(Menu.MenuModule.MERCHANT))
                            .and(qMenu.enabled.isTrue()).and(qMenu.menuType.eq(Menu.MenuType.MENU))
                            .and(qMenuRole.menuId.in(jqf.selectFrom(qMerchantMenu).select(qMerchantMenu.menuId).where(qMerchantMenu.merchantId.eq(merchantId)).fetch()))
                    ).orderBy(qMenu.menuGroup.asc(), qMenu.pos.asc()).fetch();
        }
        return tuples.stream().collect(ArrayList::new, (list, tuple) -> {
            MenuDto menuDto = new MenuDto();
            menuDto.setTitle(tuple.get(qMenu.name));
            menuDto.setKey(tuple.get(qMenu.component));
            menuDto.setIcon(tuple.get(qMenu.iconCls));
            menuDto.setGroup(tuple.get(qMenu.menuGroup).name());
            menuDto.setId(tuple.get(qMenu.id));
            menuDto.setParentId(tuple.get(qMenu.parentId));
            list.add(menuDto);
        }, List::addAll);

    }

    public List<String> loadFunction(Long merchantId, Role role) {
        return new ArrayList<>(bqf.selectFrom(qMenuRole)
                .join(qMenu).on(qMenuRole.menuId.eq(qMenu.id))
                .select(qMenu.component)
                .where(qMenuRole.roleId.eq(role.getId())
                        .and(qMenu.menuModule.eq(Menu.MenuModule.MERCHANT))
                        .and(qMenu.enabled.isTrue()).and(qMenu.menuType.eq(Menu.MenuType.FUNCTION))
                        .and(qMenuRole.menuId.in(jqf.selectFrom(qMerchantMenu).select(qMerchantMenu.menuId).where(qMerchantMenu.merchantId.eq(merchantId)).fetch()))
                ).orderBy(qMenu.menuGroup.asc(), qMenu.pos.asc()).fetch());
    }

    public Admin getDefaultAdmin(Long merchantId) {
        return bqf.selectFrom(qAdmin)
                .where(qAdmin.merchantId.eq(merchantId).and(qAdmin.systemDefault.isTrue()))
                .fetchFirst();
    }


    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setName(String name) {
            if (StrUtil.isNotEmpty(name)) {
                builder.and(qAdmin.name.contains(name));
            }
        }

        public void setUsername(String username) {
            if (StrUtil.isNotEmpty(username)) {
                builder.and(qAdmin.username.contains(username));
            }
        }

        public void setPhone(String mobile) {
            if (StrUtil.isNotEmpty(mobile)) {
                builder.and(qAdmin.mobile.contains(mobile));
            }
        }
    }
}
