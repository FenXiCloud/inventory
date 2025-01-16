<template>
  <Layout class="app-frame" :siderCollapsed="siderCollapsed" siderFixed>
    <Sider theme="dark" style="overflow: unset;!important;">
      <AppMenu theme="dark"/>
    </Sider>
    <Layout headerFixed>
      <HHeader theme="white">
        <AppHead/>
      </HHeader>
      <SysTabs :homePage="currentTab"/>
      <Content>
        <div class="app-frame-content h-full p-20px pt-20px">
          <Suspense>
            <component is="DashboardMain" v-show="'DashboardMain'===currentTab"/>
            <template #fallback>
              <div class="bg-white-color h-full flex justify-center items-center flex-column">
                <div class="mb-16px">
                  <div class="loading" data-v-79c34abf="">
                    <div data-v-79c34abf=""></div>
                    <div data-v-79c34abf=""></div>
                    <div data-v-79c34abf=""></div>
                    <div data-v-79c34abf=""></div>
                    <div data-v-79c34abf=""></div>
                  </div>
                </div>
                <div>页面加载中,请稍后...</div>
              </div>
            </template>
          </Suspense>
          <template v-for="tab in tabs">
            <Suspense>
              <component class="h-full bg-white-color" v-show="tab.key===currentTab" :is="tab.key" :pro="tab.params"/>
              <!-- 加载中状态 -->
              <template #fallback>
                <div class="bg-white-color h-full flex justify-center items-center flex-column">
                  <div class="mb-16px">
                    <div class="loading" data-v-79c34abf="">
                      <div data-v-79c34abf=""></div>
                      <div data-v-79c34abf=""></div>
                      <div data-v-79c34abf=""></div>
                      <div data-v-79c34abf=""></div>
                      <div data-v-79c34abf=""></div>
                    </div>
                  </div>
                  <div>页面加载中,请稍后...</div>
                </div>
              </template>
            </Suspense>
          </template>
        </div>
        <HFooter>
          <AppFooter/>
        </HFooter>
      </Content>
    </Layout>
  </Layout>
</template>

<script>
import AppHead from "@views/app/AppHead";
import AppMenu from "@views/app/AppMenu";
import AppFooter from "@views/app/AppFooter";
import SysTabs from "@views/common/sys-tabs";
import {mapState} from "vuex";
import {message} from "heyui.ext";

/**
 * @功能描述: FRAME
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "AppFrame",
  components: {SysTabs, AppFooter, AppMenu, AppHead},
  computed: {
    ...mapState(['siderCollapsed', 'currentTab', 'tabs'])
  },
  errorCaptured(args) {
    if (args.name === 'ChunkLoadError') {
      alert('系统已更新，即将刷新页面...');
      window.location.reload();
      return false;
    } else {
      message.error(args.message);
      return true;
    }
  }
}
</script>
