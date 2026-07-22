<template>
  <div class="merchant-setting">
    <div class="merchant-setting__body">
      <component :is="setting" :merchant="merchant" class="merchant-setting__page"/>
    </div>
    <t-tabs v-model="setting" class="tabs-custom">
      <t-tab-panel v-for="item in settings" :key="item.key" :value="item.key" :label="item.title" />
    </t-tabs>
  </div>
</template>

<script>
import MerchantModuleGrant from "@/views/merchant/MerchantModuleGrant";
import AdminList from "@/views/merchant/admin/AdminList";
import RoleList from "@/views/merchant/role/RoleList";
import AccountBookList from "@/views/merchant/accountBook/AccountBookList.vue";

export default {
  name: "MerchantSetting",
  components: {MerchantModuleGrant, AdminList, RoleList, AccountBookList},
  props: {
    merchant: Object,
    defaultSetting: {
      type: String,
      default: 'AdminList'
    }
  },
  emits: {
    close: null,
  },
  watch: {
    opened(val) {
      if (!val) {
        this.$emit('close');
      }
    }
  },
  data() {
    return {
      setting: this.defaultSetting || 'AdminList',
      opened: true,
      settings: [{
        key: "AccountBookList",
        title: "账套管理"
      }, {
        key: "AdminList",
        title: "商户用户"
      }, {
        key: "RoleList",
        title: "商户角色"
      }, {
        key: "MerchantModuleGrant",
        title: "模块授权"
      }]
    }
  }
}
</script>
<style scoped>
.merchant-setting {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 70vh;
  min-height: 520px;
  overflow: hidden;
  box-sizing: border-box;
}

.merchant-setting__body {
  flex: 1;
  width: 100%;
  min-width: 0;
  overflow: auto;
  box-sizing: border-box;
}

.merchant-setting__page {
  display: block !important;
  width: 100% !important;
  min-width: 100%;
  height: 100%;
  background: #fff;
  box-sizing: border-box;
}

.merchant-setting__page :deep(.simple-page) {
  width: 100% !important;
  max-width: none !important;
  height: 100%;
  box-sizing: border-box;
}

.merchant-setting__page :deep(.simple-page__table) {
  min-height: 280px;
}

.tabs-custom {
  flex-shrink: 0;
  width: 100%;
  background-color: #f5f5f5;
  border-top: 1px solid #d3d3d3;
}

.tabs-custom :deep(.t-tabs__nav-item) {
  padding: 12px 16px;
  line-height: 1;
  font-size: 15px;
}

.tabs-custom :deep(.t-tabs__nav-item:hover) {
  color: #3d74ff;
}

.tabs-custom :deep(.t-tabs__nav-item.t-is-active) {
  color: #ffffff;
  background-color: #3d74ff;
}
</style>
