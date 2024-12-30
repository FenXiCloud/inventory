package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.entity.Menu;
import com.flyemu.share.entity.QMenu;
import com.flyemu.share.entity.QMerchantMenu;
import com.flyemu.share.repository.MenuRepository;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final QMenu qMenu = QMenu.menu;

    private final QMerchantMenu qMerchantMenu = QMerchantMenu.merchantMenu;

    private final MenuRepository menuRepository;

    /**
     * 查询商户所有菜单
     *
     * @return
     */
    public List<Menu> query(Long merchantId) {
        return bqf.selectFrom(qMenu).innerJoin(qMerchantMenu).on(qMerchantMenu.menuId.eq(qMenu.id))
                .where(qMerchantMenu.merchantId.eq(merchantId).and(qMenu.menuModule.eq(Menu.MenuModule.MERCHANT)).and(qMenu.enabled.isTrue())).fetch();
    }

    @Transactional
    public Menu save(Menu menu) {
        if (menu.getId() != null) {
            //更新
            Menu original = menuRepository.getById(menu.getId());
            BeanUtil.copyProperties(menu, original, CopyOptions.create().ignoreNullValue());
            return menuRepository.save(original);
        }

        return menuRepository.save(menu);
    }

    @Transactional
    public void delete(Long menuId) {
        menuRepository.deleteById(menuId);
    }

    public List<Menu> merchantMenu(Long merchantId, Menu.MenuGroup menuGroup) {
        BooleanExpression expression = qMerchantMenu.merchantId.eq(merchantId).and(qMenu.enabled.isTrue());
        if (menuGroup != null) {
            expression = expression.and(qMenu.menuModule.eq(Menu.MenuModule.MERCHANT)).and(qMenu.menuGroup.eq(menuGroup));
        }
        return bqf.selectFrom(qMerchantMenu)
                .select(qMenu)
                .innerJoin(qMenu)
                .on(qMerchantMenu.menuId.eq(qMenu.id))
                .where(expression)
                .orderBy(qMenu.menuGroup.asc(), qMenu.pos.asc())
                .fetch();
    }
}
