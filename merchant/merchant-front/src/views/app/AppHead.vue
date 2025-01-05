<template>
  <div class="app-header">
    <div class="flex">
      <div class="account">
        <Select v-model="selectAccountBookId" :datas="accountBooks"  @change="changeCurrent" :deletable="false"></Select>
      </div>
    </div>
    <div class="flex app-header-info flex items-center" v-if="user.admin">
      <span class="h-icon-users mr-10px"></span>
      <DropdownMenu
          className="app-header-dropdown"
          trigger="hover"
          offset="5,5"
          :width="120"
          placement="bottom-end"
          :datas="infoMenu"
          @clickItem="trigger">
        <span>{{ user.admin.name }}</span>
      </DropdownMenu>
    </div>
  </div>
</template>

<script>
/**
 * @功能描述: 头部
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import {onMounted, onUnmounted} from 'vue';
import {mapMutations, mapState} from 'vuex';
import {confirm, loading, message} from 'heyui.ext';
import {Logout} from "@js/api/App";
import AccountBook from "@js/api/AccountBook";

export default {
  name: "AppHead",
  data() {
    return {
      selectAccountBookId:null,
      searchText: '',
      infoMenu: [
        {key: 'info', title: '个人信息', icon: 'h-icon-user'},
        {key: 'logout', title: '退出登录', icon: 'h-icon-outbox'}
      ]
    };
  },
  computed: {
    ...mapState(['user','checkout','accountBooks','accountBook']),
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
    ...mapMutations(['pushTab']),
    changeCurrent(){
      loading("正在切换，请稍后...");
      AccountBook.changeCurrentAccountBook(this.selectAccountBookId).then(() => {
        message('切换成功！');
        localStorage.removeItem("currentTab");
        window.location.replace("/")
      });
    },
    trigger(data) {
      if (data === 'logout') {
        confirm({
          title: "系统提示",
          content: '确认退出？',
          onConfirm: () => {
            loading("登出中....");
            Logout().then(() => {
              localStorage.removeItem("SYS_TABS");
              window.location.replace("/")
            }).finally(() => loading.close())
          }
        });
      } else {
        this.pushTab({key:'AccountBasic',title:'个人信息'})
      }
    },
  },
  created() {
    if (this.accountBooks) {
      this.selectAccountBookId = this.accountBook.key;
    }
  }
};
</script>

<style lang="less" scoped>
.app-header {
  color: rgba(49, 58, 70, 0.8);
  overflow: hidden;
  display: flex;
  justify-content: space-between;
  .account{
    width: 250px;
    margin-top: 5px;
    margin-left: 10px;
  }


  .h-autocomplete {
    line-height: 1.5;
    float: left;
    margin-top: 15px;
    margin-right: 20px;
    width: 120px;

    &-show,
    &-show:hover,
    &-show.focusing {
      outline: none;
      box-shadow: none;
      border-color: transparent;
      border-radius: 0;
    }

    &-show.focusing {
      border-bottom: 1px solid #eee;
    }
  }

  &-info &-icon-item {
    cursor: pointer;
    display: inline-block;
    float: left;
    padding: 0 15px;
    height: @layout-header-height;
    line-height: @layout-header-height;
    margin-right: 10px;

    &:hover {
      background: @hover-background-color;
    }

    i {
      font-size: 18px;
    }

    a {
      color: inherit;
    }

    .h-badge {
      margin: 20px 0;
      display: block;
    }
  }

  .h-dropdownmenu {
    float: left;
  }

  &-dropdown {
    float: right;
    margin-left: 10px;
    padding: 0 20px 0 15px;

    .h-icon-down {
      right: 20px;
    }

    cursor: pointer;

    &:hover,
    &.h-pop-trigger {
      background: @hover-background-color;
    }

    &-dropdown {
      padding: 5px 0;

      .h-dropdownmenu-item {
        padding: 8px 20px;
      }
    }
  }

  &-menus {
    display: inline-block;
    vertical-align: top;

    > div {
      display: inline-block;
      font-size: 15px;
      padding: 0 25px;
      color: @dark-color;

      &:hover {
        color: @primary-color;
      }

      + div {
        margin-left: 5px;
      }

      &.h-tab-selected {
        color: @white-color;
        background-color: @primary-color;
      }
    }
  }
}

.account {
  min-width: 150px;

  li, dt {
    cursor: pointer;
    padding: 5px 10px;

    &:hover {
      background-color: @gray3-color;
      color: @primary-color;
    }
  }

  dt {
    padding-left: 30px;
  }
}
</style>

