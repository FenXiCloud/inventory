<template>
  <t-layout class="app-frame">
    <t-aside width="150px">
      <AppMenu theme="dark"/>
    </t-aside>
    <t-layout>
      <t-header height="60px">
        <AppHead/>
      </t-header>
      <SysTabs :homePage="currentTab"/>
      <t-content class="app-main-content">
        <div class="app-page-pane" v-show="'DashboardMain'===currentTab">
          <Suspense>
            <component is="DashboardMain"/>
            <template #fallback>
              <t-loading text="页面加载中,请稍后..."/>
            </template>
          </Suspense>
        </div>
        <div
            v-for="(tab,index) in tabs"
            :key="tab.key"
            class="app-page-pane"
            v-show="tab.key === currentTab"
        >
          <Suspense>
            <component
                v-if="tab.keepAlive === false ? tab.key === currentTab : true"
                :is="tab.key"
                v-bind="tab.params"
                :index="index"
                :pro="tab.params"
            />
            <template #fallback>
              <t-loading text="页面加载中,请稍后..."/>
            </template>
          </Suspense>
        </div>
      </t-content>
    </t-layout>
  </t-layout>
</template>

<script>
import AppHead from "@views/app/AppHead";
import AppMenu from "@views/app/AppMenu";
import SysTabs from "@views/common/sys-tabs";
import {mapState} from "vuex";
import {MessagePlugin} from "tdesign-vue-next";

/**
 * @功能描述: FRAME
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "AppFrame",
  components: {SysTabs, AppMenu, AppHead},
  computed: {
    ...mapState(['siderCollapsed', 'currentTab', 'tabs'])
  },
  errorCaptured(args) {
    if (args.name === 'ChunkLoadError') {
      alert('系统已更新，即将刷新页面...');
      window.location.reload();
      return false;
    } else {
      MessagePlugin.error(args.message);
      return true;
    }
  }
}
</script>
