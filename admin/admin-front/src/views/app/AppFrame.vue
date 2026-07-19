<template>
  <t-layout class="app-frame" v-if="!loading" style="height: 100vh">
    <t-aside width="232px">
      <AppMenu theme="dark"/>
    </t-aside>
    <t-layout>
      <t-header>
        <AppHead/>
      </t-header>
      <t-content class="app-frame-content">
        <router-view/>
      </t-content>
    </t-layout>
  </t-layout>
</template>

<script>
import AppMenu from "@/views/app/AppMenu";
import AppHead from "@/views/app/AppHead";
import {fullMenuKeys, isAuthPage} from "@js/config/menu-config";
import {LoadingPlugin} from "tdesign-vue-next";
import {Init} from "@js/api/App";

export default {
  name: "AppFrame",
  components: {AppHead, AppMenu},
  data() {
    return {
      loading: false
    }
  },
  methods: {
    init() {
      this.loading = true;
      LoadingPlugin(true);
      Init().then(({data}) => {
        this.$store.dispatch('updateAccount', data);
        this.initMenu();
        this.loading = false;
      }).catch(() => {
        this.$router.replace({name: 'Login'});
      }).finally(() => LoadingPlugin(false));
    },
    initMenu() {
      let menuKeys = fullMenuKeys;
      this.$store.dispatch('updateMenuKeys', menuKeys);
      if (!isAuthPage(menuKeys, this.$route.name)) {
        this.$router.replace({name: 'PermissionError'});
      }
    }
  },
  created() {
    this.init();
  }
}
</script>
