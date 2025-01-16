<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>

      </template>
      <template #tools>
        <Input id="name" v-model="params.filter" class="flex-1" placeholder="请输入名称"/>
        <Button color="primary" :loading="loading" @click="doSearch">查询</Button>
      </template>
    </vxe-toolbar>
    <div class="flex1">
      <vxe-table row-id="id"
                 ref="table"
                 :data="dataList"
                 highlight-hover-row
                 show-overflow
                 stripe
                 :row-config="{height: 48}"
                 :column-config="{resizable: true}"
                 :loading="loading">
        <vxe-column type="seq" width="40" title="#"/>
        <vxe-column title="账户ID" field="accountId" width="150"/>
        <vxe-column title="业务单据" field="voucherId" width="150"/>
        <vxe-column title="创建人" field="createdBy" width="100"/>
        <vxe-column title="创建时间" field="createdAt" width="150"/>
        <vxe-column title="操作类型" field="accountFlowType" width="150"/>
        <vxe-column title="交易金额" field="amount" width="100"/>
        <vxe-column title="交易前余额" field="balanceBefore" width="100"/>
        <vxe-column title="交易后余额" field="balanceAfter" width="100"/>
        <vxe-column title="备注" field="remarks"/>
      </vxe-table>
    </div>
  </div>
</template>

<script>
import AccountFlow from "@js/api/fund/AccountFlow";


/**
 * @功能描述: 资金明细
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "AccountFlowList",
  data() {
    return {
      loading: false,
      dataList: [],
      params: {
        filter: null,
      },
    }
  },
  methods: {
    doSearch() {
      this.loadList();
    },
    loadList() {
      this.loading = true;
      AccountFlow.list(this.params).then(({data}) => {
        this.dataList = data;
      }).finally(() => this.loading = false);
    },
  },
  created() {
    this.loadList();
  }
}
</script>
