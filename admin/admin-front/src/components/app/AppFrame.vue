<template>
  <Layout class="app-frame" v-if="!loading" :siderCollapsed="siderCollapsed" siderFixed>
    <Sider theme="dark">
      <AppMenu theme="dark"/>
    </Sider>
    <Layout headerFixed>
      <SysTabs homePage="DashboardMain"/>
      <Content>
        <div class="app-frame-content">
          <router-view/>
        </div>
        <HFooter>
          <AppFooter/>
        </HFooter>
      </Content>
    </Layout>
  </Layout>
</template>

<script>
import AppHead from "@components/app/AppHead";
import AppMenu from "@components/app/AppMenu";
import AppFooter from "@components/app/AppFooter";
import SysTabs from "@components/common/sys-tabs";
import {mapState} from "vuex";
import {fullMenuKeys, isAuthPage} from "@js/config/menu-config";
import {loading} from "heyui.ext";
import {Init} from "@js/api/App";

/**
 * @功能描述: appFrame
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "AppFrame",
  components: {SysTabs, AppFooter, AppMenu, AppHead},
  computed: {
    ...mapState(['siderCollapsed', 'menus'])
  },
  data() {
    return {
      loading: false
    }
  },
  methods: {
    init() {
      this.loading = true;
      loading('加载中');
      Init().then(({data}) => {
        this.$store.dispatch('updateAccount', data);
        this.initMenu();
        this.loading = false;
      }).catch(() => {
        this.$router.replace({name: 'Login'});
      }).finally(() => loading.close());
    },
    initMenu() {
      // 如果使用权限配置，配合后端获取请求的数据
      // Request.Account.menus().then(resp => {
      //   if (resp.ok) {
      //     this.menus = getMenus(resp.body);
      //     this.menuSelect();
      //   }
      // });
      let menuKeys = fullMenuKeys;
      this.$store.dispatch('updateMenuKeys', menuKeys);
      if (!isAuthPage(menuKeys, this.$route.name)) {
        this.$router.replace({name: 'PermissionError'});
      }
    }
  },
  created() {
    // 如果无后台数据，将此处屏蔽
    this.init();
  }
}
</script>
<style>

.app-frame-content{
  margin: 10px;
}
</style>
