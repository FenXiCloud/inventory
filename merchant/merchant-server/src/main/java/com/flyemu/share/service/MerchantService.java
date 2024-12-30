package com.flyemu.share.service;

import java.time.LocalDateTime;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.crypto.digest.DigestUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.*;
import com.flyemu.share.repository.AdminRepository;
import com.flyemu.share.repository.MerchantRepository;
import com.flyemu.share.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @功能描述: 商户管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MerchantService extends AbsService {

    private final QMerchant qMerchant = QMerchant.merchant;

    private final MerchantRepository merchantRepository;

    private final AdminRepository adminRepository;

    private final RoleRepository roleRepository;

    public PageResults<Merchant> query(Page page) {
        PagedList<Merchant> fetchPage = bqf.selectFrom(qMerchant)
                .orderBy(qMerchant.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());
        return new PageResults<>(fetchPage, page);
    }
    
    @Transactional
    public Merchant save(Merchant merchant) {
        if (merchant.getId() != null) {
            //更新
            Merchant original = merchantRepository.getById(merchant.getId());
            BeanUtil.copyProperties(merchant, original, CopyOptions.create().ignoreNullValue());
            return merchantRepository.save(original);
        }
        Merchant m = merchantRepository.save(merchant);

        return m;
    }

    @Transactional
    public Admin create(Merchant merchant) {
        merchant.setEnabled(true);
        merchant.setCreateDate(LocalDateTime.now());
        merchant.setCode(LocalDateTime.now().toString());
        merchantRepository.save(merchant);

        //创建默认角色和管理员账号
        Role role = new Role();
        role.setName("商户管理员");
        role.setSystemDefault(true);
        role.setMerchantId(merchant.getId());
        roleRepository.save(role);

        Admin admin = new Admin();
        admin.setName(merchant.getLinkman());
        admin.setMobile(merchant.getMobile());
        admin.setUsername(merchant.getCode() + LocalDateTime.now().toString());
        admin.setPassword(DigestUtil.bcrypt(merchant.getMobile().substring(5)));
        admin.setEnabled(true);
        admin.setMerchantId(merchant.getId());
        admin.setSystemDefault(true);
        admin.setRoleId(role.getId());
        adminRepository.save(admin);

        return admin;
    }

    @Transactional
    public void delete(Long merchantId) {
        merchantRepository.deleteById(merchantId);
    }

}
