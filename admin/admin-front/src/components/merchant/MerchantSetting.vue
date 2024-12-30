<template>
  <div class="flex" style="flex-direction: column;height: 100%;overflow-x: hidden">
    <div class="flex-1 h-0px">
      <component :is="setting" :merchant="merchant" class="h-full flex flex-column" style="background: #f3f6f8;"/>
    </div>
    <Tabs :datas="settings" v-model="setting" class-name="h-tabs-custom"></Tabs>
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
import MerchantModuleGrant from "@components/merchant/MerchantModuleGrant";
import AdminList from "@components/merchant/admin/AdminList";
import RoleList from "@components/merchant/role/RoleList";
import AccountBookList from "@components/merchant/accountBook/AccountBookList.vue";

export default {
  name: "MerchantSetting",
  components: {MerchantModuleGrant, AdminList, RoleList,AccountBookList},
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
      },{
        key: "MerchantModuleGrant",
        title: "模块授权"
      }]
    }
  }
}
</script>
<style lang="less">
.h-tabs-custom {
  background-color: @gray2-color;
  border-top: @border;

  > .h-tabs-item {
    padding: 12px 16px;
    line-height: 1;
    font-size: 15px;

    &:hover {
      color: @primary-color;
    }

    &.h-tabs-selected {
      color: @white-color;
      background-color: #3d74ff;
    }
  }
}
</style>
