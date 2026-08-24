<template>
  <div class="app-menu" @click="onMenuRootClick">
    <AppLogo/>
    <t-menu
        :key="menuRenderKey"
        :value="activeMenuId"
        theme="dark"
        width="100%"
        expand-type="popup"
        @change="onMenuChange"
    >
      <template v-for="m in filteredMenus" :key="m.id">
        <!-- 一级无子级 -->
        <t-menu-item v-if="!m.children || !m.children.length" :value="m.id">
          <template #icon>
            <t-icon :name="resolveMenuIcon(m)"/>
          </template>
          {{ m.title }}
        </t-menu-item>

        <!-- 一级：悬浮/单击弹出；二级作标题；三级可点击 -->
        <t-submenu
            v-else
            :value="m.id"
            :title="m.title"
            :popup-props="popupProps"
        >
          <template #icon>
            <t-icon :name="resolveMenuIcon(m)"/>
          </template>

          <template v-for="c1 in m.children" :key="c1.id">
            <t-menu-group v-if="c1.children && c1.children.length" :title="c1.title">
              <t-menu-item
                  v-for="c2 in c1.children"
                  :key="c2.id"
                  :value="c2.id"
              >
                {{ c2.title }}
              </t-menu-item>
            </t-menu-group>
            <t-menu-item v-else :value="c1.id">
              {{ c1.title }}
            </t-menu-item>
          </template>
        </t-submenu>
      </template>
    </t-menu>
  </div>
</template>

<script>
import AppLogo from "@views/app/AppLogo";
import {mapState, mapMutations} from 'vuex';
import {pick} from '@common/utils';

/** 一级菜单 key → TDesign 图标名 */
const MENU_ICON_BY_KEY = {
  basic: 'root-list',
  Purchase: 'cart',
  Sales: 'shop',
  Inventory: 'layers',
  Fund: 'wallet',
  Invoice: 'bill',
  Setting: 'setting',
  DashboardMain: 'dashboard'
};
export default {
  name: "AppMenu",
  props: {
    theme: String
  },
  data() {
    return {
      menuRenderKey: 0,
      popupProps: {
        overlayClassName: 'app-menu-popup',
        placement: 'right-top',
        destroyOnClose: true,
        attach: 'body'
      }
    };
  },
  computed: {
    ...mapState(['siderCollapsed', 'menus', "currentTab", 'tabs']),
    /** 菜单按数据库 enabled 状态展示（启用/禁用由后端菜单配置控制） */
    filteredMenus() {
      return this.menus || [];
    },
    /** 当前激活的菜单 id（用 id 而非组件名，避免多个菜单复用同一组件时高亮错乱） */
    activeMenuId() {
      const val = this.currentTab;
      if (val === 'DashboardMain') return null;
      const tab = this.tabs.find(t => String(t.key) === String(val));
      if (tab && tab.menuId != null) return tab.menuId;
      // 兜底：无 menuId（头部下拉/表单页等）时按组件名反查菜单
      const menu = this.findMenu(this.filteredMenus, val);
      return menu ? menu.id : null;
    }
  },
  watch: {
    currentTab(val) {
      location.hash = val;
      localStorage.setItem("currentTab", JSON.stringify(pick(this.tabs.find(value => value.key === val), ['key', 'title'])))
    }
  },
  mounted() {
    document.addEventListener('click', this.onDocumentClick);
  },
  beforeUnmount() {
    document.removeEventListener('click', this.onDocumentClick);
  },
  methods: {
    ...mapMutations(['pushTab', 'updateTab']),
    resolveMenuIcon(menu) {
      const key = menu.key || menu.id;
      if (key && MENU_ICON_BY_KEY[key]) return MENU_ICON_BY_KEY[key];
      const icon = menu.icon;
      if (icon && typeof icon === 'string' && !icon.startsWith('fa')) return icon;
      return 'app';
    },
    /** 单击一级菜单标题：打开浮动层（与悬停共用 TDesign 内部 popupVisible） */
    onMenuRootClick(e) {
      const title = e.target.closest('.app-menu .t-submenu > .t-menu__item');
      if (!title) return;
      const li = title.closest('.t-submenu');
      if (!li) return;
      e.stopPropagation();
      if (li.classList.contains('t-is-opened')) {
        this.closeFloatingMenu();
        return;
      }
      li.dispatchEvent(new MouseEvent('mouseenter', {bubbles: false}));
    },
    /** 点击菜单/弹层外时收起（单击打开后可能不会走 mouseleave） */
    onDocumentClick(e) {
      if (e.target.closest('.app-menu .t-submenu > .t-menu__item')) return;
      if (e.target.closest('.app-menu')) return;
      if (e.target.closest('.app-menu-popup, .t-menu__popup, .t-menu__popup-wrapper')) return;
      if (!this.$el?.querySelector?.('.t-submenu.t-is-opened')) return;
      this.closeFloatingMenu();
    },
    /** 悬浮菜单为 hover 触发，点击后鼠标仍在弹层上时不会自动收起，需主动关闭 */
    closeFloatingMenu() {
      const nodes = document.querySelectorAll(
        'body > .t-popup.app-menu-popup, body > .t-popup.t-menu__popup, .t-menu__popup-wrapper'
      );
      nodes.forEach((node) => {
        const popup = node.classList.contains('t-popup') ? node : (node.closest('.t-popup') || node);
        if (!popup) return;
        popup.classList.remove('t-popup--visible');
        popup.style.display = 'none';
        popup.style.visibility = 'hidden';
        popup.style.pointerEvents = 'none';
      });
      // 重建菜单，清除内部 hover 状态，保证下次悬停/单击可再次打开
      this.menuRenderKey += 1;
    },
    onMenuChange(value) {
      const menu = this.findMenuById(this.filteredMenus, value);
      // 必须用前端组件名（menu.key）；缺 key 时勿回退到数字 id，否则 :is 渲染会失败
      const componentKey = menu && menu.key;
      if (menu && componentKey) {
        this.pushTab({
          keepAlive: false,
          key: componentKey,
          title: menu.title,
          icon: menu.icon,
          menuId: menu.id
        });
      } else if (menu) {
        console.warn('[AppMenu] 菜单缺少 component/key，无法打开：', menu.title, menu.id);
      }
      this.closeFloatingMenu();
    },
    findMenuById(list, id) {
      const match = (item) => String(item.id) === String(id);
      for (const item of list) {
        if (match(item)) return item;
        if (item.children) {
          for (const c1 of item.children) {
            if (match(c1)) return c1;
            if (c1.children) {
              const c2 = c1.children.find(match);
              if (c2) return c2;
            }
          }
        }
      }
      return null;
    },
    findMenu(list, key) {
      const match = (item) => String(item.key ?? item.id) === String(key);
      for (const item of list) {
        if (match(item)) return item;
        if (item.children) {
          for (const c1 of item.children) {
            if (match(c1)) return c1;
            if (c1.children) {
              const c2 = c1.children.find(match);
              if (c2) return c2;
            }
          }
        }
      }
      return null;
    }
  },
  components: {
    AppLogo
  }
}
</script>

<style scoped>
.app-menu {
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style>
