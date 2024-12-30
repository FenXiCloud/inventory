package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.dto.AdminDto;
import com.flyemu.share.entity.setting.*;
import com.flyemu.share.repository.AdminRepository;
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
 * @功能描述: 管理员管理
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

    private final static QAdmin qAdmin = QAdmin.admin;

    private final static QRole qRole = QRole.role;

    private final static QMerchant qMerchant = QMerchant.merchant;

    private final AdminRepository adminRepository;

    public PageResults<AdminDto> query(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qAdmin)
                .join(qRole).on(qAdmin.roleId.eq(qRole.id))
                .select(qAdmin, qRole.name)
                .where(query.builder)
                .orderBy(qAdmin.id.desc())
                .fetchJoin()
                .fetchPage(page.getOffset(), page.getOffsetEnd());
        List<AdminDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            AdminDto adminDto = BeanUtil.toBean(tuple.get(qAdmin), AdminDto.class);
            adminDto.setRoleName(tuple.get(qRole.name));
            dtos.add(adminDto);
        });
        dtos.sort(Comparator.comparing(AdminDto::getId).reversed());
        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
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


    public AccountDto login(String username, String password) {
        Admin admin = jqf.selectFrom(qAdmin)
                .where(qAdmin.username.eq(username)).fetchFirst();
        Assert.notNull(admin, "账号错误~");
        Assert.isTrue(DigestUtil.bcryptCheck(password, admin.getPassword()), "密码错误~");

        Merchant merchant = jqf.selectFrom(qMerchant).where(qMerchant.id.eq(admin.getMerchantId())).fetchFirst();

        Role role = bqf.selectFrom(qRole).where(qRole.id.eq(admin.getRoleId())).fetchFirst();

        return new AccountDto(admin, merchant, role);
    }

    @Transactional
    public void updatePassword(Long adminId, String oldPassword, String newPassword, Long merchantId) {
        Admin admin = jqf.selectFrom(qAdmin).where(qAdmin.id.eq(adminId).and(qAdmin.merchantId.eq(merchantId))).fetchFirst();
        Assert.isTrue(DigestUtil.bcryptCheck(oldPassword, admin.getPassword()), "原密码错误~");
        admin.setPassword(DigestUtil.bcrypt(newPassword));
        adminRepository.save(admin);
    }

    /**
     * 重置密码为手机号后6位
     *
     * @param adminId 管理员ID
     */
    @Transactional
    public void resetPassword(Long adminId, Long merchantId) {
        Admin admin = jqf.selectFrom(qAdmin).where(qAdmin.id.eq(adminId)).fetchFirst();
        admin.setPassword(DigestUtil.bcrypt(admin.getMobile().substring(5)));
        adminRepository.save(admin);
    }


    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qAdmin.merchantId.eq(merchantId));
            }
        }

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
