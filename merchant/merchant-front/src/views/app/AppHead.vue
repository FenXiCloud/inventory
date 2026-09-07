<template>
  <div class="app-header">
    <div class="flex items-center">
      <div class="account">
        <t-select
            v-model="selectAccountBookId"
            :options="accountBooks"
            :keys="{ value: 'key', label: 'title' }"
            :clearable="false"
            placeholder="请选择账套"
            @change="changeCurrent"
        />
      </div>
      <t-icon name="setting" class="setting-icon" @click="goToAccountBook" />
      <t-button class="checkout-btn" theme="default" variant="outline" size="small" @click="goToCheckout" v-if="accountBook">
        账套日期：{{ displayDate }}
      </t-button>
    </div>
    <div class="flex app-header-info flex items-center" v-if="user.admin">
      <t-icon name="user" class="mr-10px"></t-icon>
      <t-dropdown trigger="hover" :minColumnWidth="120" placement="bottom-right" @click="trigger">
        <span>{{ user.admin.name }}</span>
        <template #dropdown>
          <t-dropdown-menu>
            <t-dropdown-item value="merchantInfo">企业信息</t-dropdown-item>
            <t-dropdown-item value="info">个人信息</t-dropdown-item>
            <t-dropdown-item value="logout">退出登录</t-dropdown-item>
          </t-dropdown-menu>
        </template>
      </t-dropdown>
    </div>
  </div>
</template>

<script>
import {mapMutations, mapState} from 'vuex';
import {LoadingPlugin, MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import {Logout} from "@js/api/App";
import AccountBook from "@js/api/setting/AccountBook";

export default {
  name: "AppHead",
  data() {
    return {
      selectAccountBookId: null,
    };
  },
  computed: {
    ...mapState(['user', 'checkout', 'accountBooks', 'accountBook']),
    isCheckout() {
      if (!this.accountBook || !this.accountBook.checkoutDate) return false;
      // 如果结账日期不等于启用日期，说明真正结账过
      return this.accountBook.checkoutDate !== this.accountBook.startDate;
    },
    displayDate() {
      if (!this.accountBook) return '';
      // 如果真正结账过，显示结账日期，否则显示启用日期
      const date = this.isCheckout ? this.accountBook.checkoutDate : this.accountBook.startDate;
      if (!date) return '未设置';
      // 格式化日期为 xxxx年xx月
      const year = date.substring(0, 4);
      const month = date.substring(5, 7);
      return `${year}年${month}月`;
    }
  },
  methods: {
    ...mapMutations(['pushTab']),
    changeCurrent() {
      LoadingPlugin(true);
      AccountBook.changeCurrentAccountBook(this.selectAccountBookId).then(() => {
        MessagePlugin.success('切换成功！');
        localStorage.removeItem("currentTab");
        window.location.replace("/")
      });
    },
    goToAccountBook() {
      this.pushTab({key: 'AccountBookList', title: '账套管理'});
    },
    goToCheckout() {
      this.pushTab({key: 'CheckoutList', title: '结账/反结账'});
    },
    trigger(data) {
      if (data.value === 'logout') {
        DialogPlugin.confirm({
          header: "系统提示",
          body: '确认退出？',
          onConfirm: () => {
            LoadingPlugin(true);
            Logout().then(() => {
              localStorage.removeItem("SYS_TABS");
              window.location.replace("/")
            }).finally(() => LoadingPlugin(false))
          }
        });
      } else if (data.value === 'merchantInfo') {
        this.pushTab({key: 'MerchantInfo', title: '企业信息'})
      } else if (data.value === 'info') {
        this.pushTab({key: 'AccountBasic', title: '个人信息'})
      }
    },
  },
  created() {
    if (this.accountBook?.key != null) {
      this.selectAccountBookId = this.accountBook.key;
    }
  }
};
</script>

<style scoped>
.app-header {
  color: rgba(49, 58, 70, 0.8);
  overflow: hidden;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
  padding: 0 16px;
  background: #fff;
}

.account {
  width: 200px;
}

.checkout-btn {
  margin-left: 12px;
  width: 180px;
  height: 32px;
  font-size: 13px;
  color: #666;
  border: 1px solid #d9d9d9;
  box-sizing: border-box;
}

.checkout-btn:hover {
  color: #1890ff;
  border-color: #1890ff;
}

.setting-icon {
  margin-left: 4px;
  cursor: pointer;
  color: #666;
  font-size: 18px;
  transition: color 0.2s;
}

.setting-icon:hover {
  color: #1890ff;
}

.app-header-info {
  cursor: pointer;
  padding: 0 15px;
}

.app-header-info:hover {
  background: #f8f8f8;
}
</style>
