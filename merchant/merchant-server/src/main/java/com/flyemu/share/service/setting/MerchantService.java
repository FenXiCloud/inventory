package com.flyemu.share.service.setting;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.crypto.digest.DigestUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.setting.Admin;
import com.flyemu.share.entity.setting.Merchant;
import com.flyemu.share.entity.setting.MerchantMenu;
import com.flyemu.share.entity.setting.QMenu;
import com.flyemu.share.entity.setting.QMerchant;
import com.flyemu.share.entity.setting.Role;
import com.flyemu.share.repository.setting.AdminRepository;
import com.flyemu.share.repository.setting.MerchantMenuRepository;
import com.flyemu.share.repository.setting.MerchantRepository;
import com.flyemu.share.repository.setting.RoleRepository;
import com.flyemu.share.service.BaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MerchantService extends BaseService {

    private final QMerchant qMerchant = QMerchant.merchant;

    private static final QMenu qMenu = QMenu.menu;

    private final MerchantRepository merchantRepository;

    private final AdminRepository adminRepository;

    private final RoleRepository roleRepository;

    private final MerchantMenuRepository merchantMenuRepository;

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
        return merchantRepository.save(merchant);
    }

    @Transactional
    public Admin create(Merchant merchant) {
        merchant.setEnabled(true);
        merchant.setCreatedAt(LocalDateTime.now());
        merchant.setCode(LocalDateTime.now().toString());
        merchantRepository.save(merchant);

        //创建默认角色和管理员账号
        Role role = new Role();
        role.setName("商户管理员");
        role.setSystemDefault(true);
        role.setMerchantId(merchant.getId());
        roleRepository.save(role);

        Admin admin = new Admin();
        admin.setName(merchant.getContact());
        admin.setMobile(merchant.getMobile());
        admin.setUsername(merchant.getCode() + LocalDateTime.now().toString());
        admin.setPassword(DigestUtil.bcrypt(merchant.getMobile().substring(5)));
        admin.setEnabled(true);
        admin.setMerchantId(merchant.getId());
        admin.setSystemDefault(true);
        admin.setRoleId(role.getId());
        adminRepository.save(admin);

        // 为新商户关联所有菜单
        associateMerchantMenus(merchant.getId());

        return admin;
    }

    /**
     * 为新商户关联所有菜单
     */
    private void associateMerchantMenus(Long merchantId) {
        List<Long> menuIds = jqf.selectFrom(qMenu).select(qMenu.id).fetch();
        if (menuIds.isEmpty()) {
            return;
        }
        List<MerchantMenu> merchantMenus = menuIds.stream()
                .map(menuId -> {
                    MerchantMenu mm = new MerchantMenu();
                    mm.setMenuId(menuId);
                    mm.setMerchantId(merchantId);
                    return mm;
                })
                .collect(Collectors.toList());
        merchantMenuRepository.saveAll(merchantMenus);
    }

    @Transactional
    public void delete(Long merchantId) {
        merchantRepository.deleteById(merchantId);
    }

}
