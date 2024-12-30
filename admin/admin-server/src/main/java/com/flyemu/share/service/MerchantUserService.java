package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.*;
import com.flyemu.share.repository.MerchantUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;

/**
 * @功能描述: 用户管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MerchantUserService extends AbsService {

    private final MerchantUserRepository merchantUserRepository;

    private final QMerchantUser qMerchantUser = QMerchantUser.merchantUser;

    private final static QRole qRole = QRole.role;

    private final static QMerchant qMerchant = QMerchant.merchant;

    /**
     * 初始化系统默认账号
     */
    @PostConstruct
    public void initDefaultUser() {
        Long count = jqf.selectFrom(qMerchantUser).select(qMerchantUser.count()).fetchFirst();
        if (count == 0) {
            MerchantUser merchantUser = new MerchantUser();
            merchantUser.setName("李泽龙");
            merchantUser.setUsername("13944878765");
            merchantUser.setPassword(DigestUtil.bcrypt("878765"));
            merchantUser.setEnabled(true);
            merchantUser.setSystemDefault(true);
            merchantUserRepository.save(merchantUser);

            log.info("默认管理账号：13944878765，密码：878765");
        }
    }

    @Transactional
    public MerchantUser login(String username, String password) {
        MerchantUser merchantUser = jqf.selectFrom(qMerchantUser)
                .where(qMerchantUser.username.eq(username)).fetchFirst();
        Assert.notNull(merchantUser, "账号错误~");
        Assert.isTrue(DigestUtil.bcryptCheck(password, merchantUser.getPassword()), "密码错误~");

        merchantUser.setLastLoginDate(LocalDateTime.now());
        merchantUserRepository.save(merchantUser);
        return merchantUser;
    }

    public PageResults<MerchantUser> query(Page page) {
        PagedList<MerchantUser> fetchPage = bqf.selectFrom(qMerchantUser)
                .orderBy(qMerchantUser.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());
        return new PageResults<>(fetchPage, page);
    }


    @Transactional
    public MerchantUser save(MerchantUser merchantUser) {
        if (merchantUser.getId() != null) {
            //更新
            MerchantUser original = merchantUserRepository.getById(merchantUser.getId());
            BeanUtil.copyProperties(merchantUser, original, CopyOptions.create().ignoreNullValue());
            return merchantUserRepository.save(original);
        }
        merchantUser.setSystemDefault(false);
        merchantUser.setEnabled(true);
        //手动设置了账号，需要检查重复
        long count = bqf.selectFrom(qMerchantUser)
                .where(qMerchantUser.username.eq(merchantUser.getUsername()))
                .fetchCount();
        Assert.isTrue(count == 0, merchantUser.getUsername() + "账号已存在~");

        //没有设置密码，默认使用手机号后6位
        if (StrUtil.isEmpty(merchantUser.getPassword())) {
            merchantUser.setPassword(DigestUtil.bcrypt(merchantUser.getUsername().substring(5)));
        } else {
            merchantUser.setPassword(DigestUtil.bcrypt(merchantUser.getPassword()));
        }

        return merchantUserRepository.save(merchantUser);
    }


    @Transactional
    public void delete(Long userId, Long merchantId) {
        jqf.delete(qMerchantUser)
                .where(qMerchantUser.id.eq(userId).and(qMerchantUser.systemDefault.isFalse()))
                .execute();
    }

    @Transactional
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        MerchantUser merchantUser = jqf.selectFrom(qMerchantUser).where(qMerchantUser.id.eq(userId)).fetchFirst();
        Assert.isTrue(DigestUtil.bcryptCheck(oldPassword, merchantUser.getPassword()), "原密码错误~");
        merchantUser.setPassword(DigestUtil.bcrypt(newPassword));
        merchantUserRepository.save(merchantUser);
    }

    /**
     * 重置密码为手机号后6位
     *
     * @param userId 管理员ID
     */
    @Transactional
    public void resetPassword(Long userId, Long merchantId) {
        MerchantUser merchantUser = jqf.selectFrom(qMerchantUser).where(qMerchantUser.id.eq(userId)).fetchFirst();
        merchantUser.setPassword(DigestUtil.bcrypt(merchantUser.getUsername().substring(5)));
        merchantUserRepository.save(merchantUser);
    }
}
