<template>
  <div class="app-header">
    <div class="flex">
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
  width: 250px;
}

.app-header-info {
  cursor: pointer;
  padding: 0 15px;
}

.app-header-info:hover {
  background: #f8f8f8;
}
</style>
