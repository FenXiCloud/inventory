<template>
  <div class="flex" style="flex-direction: column;height: 100%;overflow-x: hidden">
    <div class="flex-1">
      <component :is="setting" :merchant="merchant" class="h-full flex flex-column" style="background: #f3f6f8;"/>
    </div>
    <t-tabs v-model="setting" class="tabs-custom">
      <t-tab-panel v-for="item in settings" :key="item.key" :value="item.key" :label="item.title" />
    </t-tabs>
  </div>
</template>

<script>
/**
 * @功能描述: 商户配置
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import MerchantModuleGrant from "@/views/merchant/MerchantModuleGrant";
import AdminList from "@/views/merchant/admin/AdminList";
import RoleList from "@/views/merchant/role/RoleList";
import AccountBookList from "@/views/merchant/accountBook/AccountBookList.vue";

export default {
  name: "MerchantSetting",
  components: {MerchantModuleGrant, AdminList, RoleList, AccountBookList},
  props: {
    merchant: Object
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
      setting: 'AccountBookList',
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
.tabs-custom {
  background-color: #f5f5f5;
  border-top: 1px solid #d3d3d3;
}

.tabs-custom .t-tabs__nav-item {
  padding: 12px 16px;
  line-height: 1;
  font-size: 15px;
}

.tabs-custom .t-tabs__nav-item:hover {
  color: #3d74ff;
}

.tabs-custom .t-tabs__nav-item.t-is-active {
  color: #ffffff;
  background-color: #3d74ff;
}
</style>
