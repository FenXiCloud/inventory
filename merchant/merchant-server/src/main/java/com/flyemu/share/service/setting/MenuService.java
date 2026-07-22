package com.flyemu.share.service.setting;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.entity.setting.Menu;
import com.flyemu.share.entity.setting.QMenu;
import com.flyemu.share.entity.setting.QMerchantMenu;
import com.flyemu.share.repository.setting.MenuRepository;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MenuService extends BaseService {

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
