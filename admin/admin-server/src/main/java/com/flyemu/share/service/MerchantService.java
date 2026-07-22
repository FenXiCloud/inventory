package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.setting.*;
import com.flyemu.share.repository.setting.AccountBookRepository;
import com.flyemu.share.repository.setting.AdminRepository;
import com.flyemu.share.repository.setting.MerchantRepository;
import com.flyemu.share.repository.setting.RoleRepository;
import com.querydsl.core.BooleanBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MerchantService extends BaseService {

    private static final QMerchant qMerchant = QMerchant.merchant;

    private final MerchantRepository merchantRepository;

    private final AdminRepository adminRepository;

    private final RoleRepository roleRepository;

    private final AccountBookRepository accountBookRepository;

    @PostConstruct
    public void initDefaultUser() {
        Long count = jqf.selectFrom(qMerchant).select(qMerchant.count()).fetchFirst();
        if (count == 0) {
            Merchant merchant = new Merchant();
            merchant.setCode(UUID.randomUUID().toString());
            merchant.setName("纷析云");
            merchant.setAddress("杭州");
            merchant.setCreatedAt(LocalDateTime.now());
            merchant.setContact("李泽龙");
            merchant.setMobile("13944878765");
            merchant.setEnabled(true);
            LocalDate now = LocalDate.now();
            LocalDate firstDay = now.with(TemporalAdjusters.firstDayOfMonth());
            this.save(merchant);

            AccountBook accountBook = new AccountBook();
            accountBook.setMerchantId(merchant.getId());
            accountBook.setCurrent(true);
            accountBook.setName("纷析云");
            accountBook.setEnabled(true);
            accountBookRepository.save(accountBook);

            log.info("测试商户账号：13944878765，密码：878765");
        }
    }

    public PageResults<Merchant> query(Page page, Query query) {
        PagedList<Merchant> fetchPage = bqf.selectFrom(qMerchant)
                .where(query.builder)
                .orderBy(qMerchant.enabled.desc(), qMerchant.id.desc())
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
        } else {
            merchant.setEnabled(true);
            merchant.setCreatedAt(LocalDateTime.now());
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
            admin.setUsername(merchant.getMobile());
            admin.setPassword(DigestUtil.bcrypt(merchant.getMobile().substring(5)));
            admin.setEnabled(true);
            admin.setMerchantId(merchant.getId());
            admin.setSystemDefault(true);
            admin.setRoleId(role.getId());
            adminRepository.save(admin);
        }
        return merchantRepository.save(merchant);
    }

    @Transactional
    public void delete(Long merchantId) {
        merchantRepository.deleteById(merchantId);
    }

    public List<Merchant> select() {
        return bqf.selectFrom(qMerchant).orderBy(qMerchant.code.asc()).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setName(String name) {
            if (StrUtil.isNotEmpty(name)) {
                builder.and(qMerchant.name.contains(name));
            }
        }
    }
}
