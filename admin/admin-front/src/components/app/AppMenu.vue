<template>
  <div class="app-menu">
    <AppLogo/>
    <Menu :datas="menus" :mode="menuMode" @clickItem="trigger" ref="menu" :className="`h-menu-${theme}`"></Menu>
    <!--    <div class="app-menu-mask" @click="hideMenu"></div>-->
  </div>
  <div>
    <AppHead/>
  </div>
</template>

<script>
import AppLogo from "@components/app/AppLogo";
import AppHead from "@components/app/AppHead";
import {mapState} from 'vuex';

/**
 * @功能描述: 菜单
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
    return {};
  },
  watch: {
    $route() {
      this.menuSelect();
    },
    menus() {
      this.menuSelect();
    }
  },
  mounted() {
    this.init();
  },
  computed: {
    ...mapState(['siderCollapsed', 'menus']),
    menuMode() {
      return this.siderCollapsed ? 'collapse' : 'vertical';
    }
  },
  methods: {
    init() {
      this.menuSelect();
    },
    menuSelect() {
      if (this.$route.name) {
        this.$refs.menu.select(this.$route.name);
      }
    },
    trigger(data) {
      if (data.children.length > 0) return;
      this.$router.push({name: data.key});
    },
    hideMenu() {
      this.$store.commit('updateSiderCollapse', true);
    }
  },
  components: {
    AppLogo,
    AppHead
  }
}
</script>
<style lang="less" scoped>
.app-menu {
  .h-menu {
    font-size: 14px;

    .h-menu-li-selected {
      .h-menu-show:after {
        width: 4px;
      }
    }

    > li > .h-menu-show {
      font-size: 15px;

      .h-menu-show-icon {
        font-size: 20px;
      }

      .h-menu-show-desc {
        transition: opacity 0.1s cubic-bezier(0.645, 0.045, 0.355, 1), width 0.1s cubic-bezier(0.645, 0.045, 0.355, 1);
      }
    }
  }

  .h-menu.h-menu-size-collapse > .h-menu-li > .h-menu-show {
    padding-left: 24px;

    .h-menu-show-icon {
      font-size: 20px;
    }
  }

  .h-menu.h-menu-white {
    color: rgb(49, 58, 70);
  }
}
</style>
