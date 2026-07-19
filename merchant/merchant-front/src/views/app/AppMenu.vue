<template>
  <div class="app-menu">
    <AppLogo/>
    <t-menu
        :value="currentTab"
        theme="dark"
        width="100%"
        expand-type="popup"
        @change="onMenuChange"
    >
      <template v-for="m in menus" :key="m.key || m.id">
        <!-- 一级无子级 -->
        <t-menu-item v-if="!m.children || !m.children.length" :value="m.key || m.id">
          <template #icon>
            <t-icon :name="resolveMenuIcon(m)"/>
          </template>
          {{ m.title }}
        </t-menu-item>

        <!-- 一级：悬浮弹出；二级作标题；三级可点击 -->
        <t-submenu
            v-else
            :value="m.key || m.id"
            :title="m.title"
            :popup-props="popupProps"
        >
          <template #icon>
            <t-icon :name="resolveMenuIcon(m)"/>
          </template>

          <template v-for="c1 in m.children" :key="c1.key || c1.id">
            <t-menu-group v-if="c1.children && c1.children.length" :title="c1.title">
              <t-menu-item
                  v-for="c2 in c1.children"
                  :key="c2.key || c2.id"
                  :value="c2.key || c2.id"
              >
                {{ c2.title }}
              </t-menu-item>
            </t-menu-group>
            <t-menu-item v-else :value="c1.key || c1.id">
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
import {pick} from "xe-utils";

/** 一级菜单 key / 旧 h-icon → TDesign 图标名 */
const MENU_ICON_BY_KEY = {
  basic: 'root-list',
  Purchase: 'cart',
  Sales: 'shop',
  Inventory: 'layers',
  Fund: 'wallet',
  Setting: 'setting',
  DashboardMain: 'dashboard'
};

const HEYUI_ICON_MAP = {
  'h-icon-task': 'root-list',
  'h-icon-plus': 'cart',
  'h-icon-complete': 'shop',
  'h-icon-check': 'layers',
  'h-icon-bell': 'wallet',
  'h-icon-setting': 'setting',
  'h-icon-home': 'home',
  'h-icon-user': 'user',
  'h-icon-menu': 'view-list'
};

/**
 * @功能描述: 菜单（一级侧栏 + 二/三级悬浮面板）
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "AppMenu",
  props: {
    theme: String
  },
  data() {
    return {
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
  },
  watch: {
    currentTab(val) {
      location.hash = val;
      localStorage.setItem("currentTab", JSON.stringify(pick(this.tabs.find(value => value.key === val), ['key', 'title'])))
    }
  },
  methods: {
    ...mapMutations(['pushTab', 'updateTab']),
    resolveMenuIcon(menu) {
      const key = menu.key || menu.id;
      if (key && MENU_ICON_BY_KEY[key]) return MENU_ICON_BY_KEY[key];
      const icon = menu.icon;
      if (!icon) return 'app';
      if (HEYUI_ICON_MAP[icon]) return HEYUI_ICON_MAP[icon];
      if (!String(icon).startsWith('h-icon-') && !String(icon).startsWith('fa')) {
        return icon;
      }
      return 'app';
    },
    onMenuChange(value) {
      if (value === 'DashboardMain') {
        this.updateTab('DashboardMain');
        return;
      }
      const menu = this.findMenu(this.menus, value);
      if (menu) {
        this.pushTab({
          keepAlive: false,
          key: menu.key || menu.id,
          title: menu.title,
          icon: menu.icon
        });
      }
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
