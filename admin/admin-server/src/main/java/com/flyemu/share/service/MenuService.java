package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.flyemu.share.entity.Menu;
import com.flyemu.share.entity.MerchantMenu;
import com.flyemu.share.entity.QMenu;
import com.flyemu.share.entity.QMerchantMenu;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.MenuRepository;
import com.flyemu.share.repository.MerchantMenuRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @功能描述: 菜单管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MenuService extends AbsService {

    private final static QMenu qMenu = QMenu.menu;

    private final MenuRepository menuRepository;

    private final MerchantMenuRepository merchantMenuRepository;

    private final QMerchantMenu qMerchantMenu = QMerchantMenu.merchantMenu;


    public List<Menu> query(Query query) {
        return bqf.selectFrom(qMenu)
                .where(query.builder)
                .orderBy(qMenu.menuModule.asc(),qMenu.menuGroup.asc(), qMenu.pos.asc()).fetch();
    }

    @Transactional
    public Menu save(Menu menu) {
        if (menu.getId() != null) {
            //更新
            Menu original = menuRepository.getById(menu.getId());
            //同步更新子节点状态
            if (menu.getEnabled() != null && menu.getEnabled() != original.getEnabled()) {
                List<Menu> parentMenu = bqf.selectFrom(qMenu).where(qMenu.parentId.eq(original.getId())).fetch();
                parentMenu.forEach(parent -> {
                    parent.setEnabled(menu.getEnabled());
                });
                if (CollUtil.isNotEmpty(parentMenu)) {
                    menuRepository.saveAll(parentMenu);
                }
            }
            BeanUtil.copyProperties(menu, original, CopyOptions.create().ignoreNullValue());
            return menuRepository.save(original);
        }
        menu.setEnabled(true);
        return menuRepository.save(menu);
    }

    @Transactional
    public void delete(Long menuId) {
        if (jqf.selectFrom(qMenu).where(qMenu.parentId.eq(menuId)).fetch().size() > 0) {
            throw new ServiceException("请删除所有下级~");
        }
        //同步删除授权的菜单
        jqf.delete(qMerchantMenu).where(qMerchantMenu.menuId.eq(menuId)).execute();
        menuRepository.deleteById(menuId);
    }

    @Transactional
    public void grantMerchant(MerchantMenuVo vo) {
        vo.merchants.forEach(merchantId -> {
            jqf.delete(qMerchantMenu)
                    .where(qMerchantMenu.merchantId.eq(merchantId))
                    .execute();
            List<MerchantMenu> menus = new ArrayList<>();
            vo.menus.forEach(menuId -> {
                MerchantMenu merchantMenu = new MerchantMenu();
                merchantMenu.setMenuId(menuId);
                merchantMenu.setMerchantId(merchantId);
                menus.add(merchantMenu);
            });
            if (CollUtil.isNotEmpty(menus)) {
                merchantMenuRepository.saveAll(menus);
            }
        });
    }

    public List<Long> queryGrantMenu(Long merchantId) {
        return bqf.selectFrom(qMerchantMenu).select(qMerchantMenu.menuId).innerJoin(qMenu).on(qMerchantMenu.menuId.eq(qMenu.id))
                .where(qMerchantMenu.merchantId.eq(merchantId).and(qMenu.enabled.isTrue())).fetch();
    }


    public List<Menu> queryMerchantMenu(Long merchantId) {
        return bqf.selectFrom(qMerchantMenu)
                .select(qMenu)
                .innerJoin(qMenu)
                .on(qMerchantMenu.menuId.eq(qMenu.id))
                .where(qMerchantMenu.merchantId.eq(merchantId).and(qMenu.enabled.isTrue()).and(qMenu.menuGroup.eq(Menu.MenuGroup.MERCHANT)))
                .orderBy(qMenu.menuGroup.asc(), qMenu.pos.asc())
                .fetch();
    }

    @Data
    public static class MerchantMenuVo {
        private List<Long> merchants;
        private List<Long> menus;
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setEnabled(Boolean enabled) {
            if (enabled != null) {
                builder.and(qMenu.enabled.eq(enabled));
            }
        }
        public void setName(String name) {
            if (StrUtil.isNotEmpty(name)) {
                builder.and(qMenu.name.contains(name));
            }
        }
        public void setMenuModule(String menuModule) {
            if (StrUtil.isNotEmpty(menuModule)) {
                builder.and(qMenu.menuModule.eq(Enum.valueOf(Menu.MenuModule.class,menuModule)));
            }
        }
    }
}
