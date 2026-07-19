<template>
  <div class="app-menu">
    <AppLogo class="app-logo"/>
    <t-menu :value="activeMenu" :collapsed="false" theme="dark" width="232px" @change="onMenuChange">
      <template v-for="menu in menus" :key="menu.key">
        <t-menu-item v-if="!menu.children || !menu.children.length" :value="menu.key">
          <template #icon><t-icon v-if="menu.icon" :name="menu.icon"/></template>
          {{ menu.title }}
        </t-menu-item>
        <t-submenu v-else :value="menu.key" :title="menu.title">
          <template #icon><t-icon v-if="menu.icon" :name="menu.icon"/></template>
          <t-menu-item v-for="child in menu.children" :key="child.key" :value="child.key">
            {{ child.title }}
          </t-menu-item>
        </t-submenu>
      </template>
    </t-menu>
  </div>
</template>

<script>
import AppLogo from "@/views/app/AppLogo";
import {mapState} from 'vuex';

export default {
  name: "AppMenu",
  props: {
    theme: String
  },
  data() {
    return {
      activeMenu: null,
    };
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
    ...mapState(['menus']),
  },
  methods: {
    init() {
      this.menuSelect();
    },
    menuSelect() {
      if (this.$route.name) {
        this.activeMenu = this.$route.name;
      }
    },
    onMenuChange(value) {
      this.$router.push({name: value});
    }
  },
  components: {
    AppLogo
  }
}
</script>
<style scoped>
.app-menu {
  font-size: 14px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.app-menu .app-logo {
  flex-shrink: 0;
}

.app-menu .t-menu {
  flex: 1;
  overflow-y: auto;
  border-right: none;
}

.app-menu .t-menu::-webkit-scrollbar {
  width: 4px;
}

.app-menu .t-menu::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
}
</style>
