<template>
  <div class="app-header">
    <div class="app-header-info" v-if="user">
      <t-dropdown trigger="hover" :min-column-width="150" placement="bottom-right">
        <span class="cursor-pointer" style="color: #fff">{{ user.name }}</span>
        <template #dropdown>
          <t-dropdown-menu>
            <t-dropdown-item @click="trigger('info')">个人信息</t-dropdown-item>
            <t-dropdown-item @click="trigger('logout')">退出登录</t-dropdown-item>
          </t-dropdown-menu>
        </template>
      </t-dropdown>
    </div>
  </div>
</template>

<script>
import {DialogPlugin, LoadingPlugin} from "tdesign-vue-next";
import {Logout} from "@js/api/App";

export default {
  name: "AppHead",
  computed: {
    user() {
      return this.$store.state.user;
    }
  },
  methods: {
    trigger(data) {
      if (data === 'logout') {
        DialogPlugin.confirm({
          header: "系统提示",
          body: '确认退出？',
          onConfirm: () => {
            LoadingPlugin(true);
            Logout().then(() => {
              localStorage.removeItem("SYS_TABS");
              this.$router.replace({name: 'Login'});
            }).finally(() => LoadingPlugin(false))
          }
        });
      } else {
        this.$router.push({name: 'AccountBasic'});
      }
    }
  }
};
</script>

<style scoped>
.app-header {
  color: #fff;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 24px;
  box-sizing: border-box;
}
</style>
