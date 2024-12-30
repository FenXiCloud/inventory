<template>
  <div class="app-header" style="position: absolute;bottom: 20px;left: 0;right: 0">
    <div class="app-header-info" v-if="user">
      <span class="h-icon-user mr-10px"></span>
      <DropdownMenu
          className="app-header-dropdown"
          trigger="hover"
          offset="5,5"
          :width="150"
          placement="bottom-end"
          :datas="infoMenu"
          @clickItem="trigger"
      >
        <span>{{ user.name }}</span>
      </DropdownMenu>
    </div>
  </div>
</template>

<script>
/**
 * @功能描述: header
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import {onMounted, onUnmounted} from 'vue';
import {loading, confirm} from "heyui.ext";
import {Logout} from "@js/api/App";

export default {
  name: "AppHead",
  data() {
    return {
      searchText: '',
      infoMenu: [
        {key: 'info', title: '个人信息', icon: 'h-icon-user'},
        {key: 'logout', title: '退出登录', icon: 'h-icon-outbox'}
      ]
    };
  },
  computed: {
    user() {
      return this.$store.state.user;
    },
    siderCollapsed: {
      get() {
        return this.$store.state.siderCollapsed;
      },
      set(value) {
        this.$store.commit('updateSiderCollapse', value);
      }
    }
  },
  setup(props, context) {
    let resizeEvent = null;
    onMounted(() => {
      let windowWidth = window.innerWidth;
      resizeEvent = window.addEventListener('resize', () => {
        if (windowWidth == window.innerWidth) {
          return;
        }
        if (context.siderCollapsed && window.innerWidth > 900) {
          context.siderCollapsed = false;
        } else if (!context.siderCollapsed && window.innerWidth < 900) {
          context.siderCollapsed = true;
        }
        windowWidth = window.innerWidth;
      });
      window.dispatchEvent(new Event('resize'));
    });
    onUnmounted(() => {
      window.removeEventListener('resize', resizeEvent);
    });
  },
  methods: {
    trigger(data) {
      if (data === 'logout') {
        confirm({
          title: "系统提示",
          content: '确认退出？',
          onConfirm: () => {
            loading("登出中....");
            Logout().then(() => {
              localStorage.removeItem("SYS_TABS");
              this.$router.replace({name: 'Login'});
            }).finally(() => loading.close())
          }
        });
      } else {
        this.$router.push({name: 'AccountBasic'});
      }
    }
  }
};
</script>

<style lang="less" scoped>
.app-header {
  color: #fff;
  margin-top: 20px;
  text-align: center;

}
</style>
