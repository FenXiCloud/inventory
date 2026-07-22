<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-tabs v-model="selected" style="flex: 1" @change="onTabChange">
        <t-tab-panel value="销售价格取数" label="销售价格取数"/>
        <t-tab-panel value="采购价格取数" label="采购价格取数"/>
      </t-tabs>
      <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="doSearch">刷新</t-button>
    </div>

    <div class="simple-page__table">
      <t-table
          row-key="id"
          size="medium"
          bordered
          stripe
          hover
          height="100%"
          table-layout="fixed"
          :data="dataList"
          :columns="columns"
          :loading="loading"
      >
        <template #ops="{ row }">
          <t-space size="small">
            <t-link theme="primary" @click="moveUp(row)">上移</t-link>
            <t-link theme="primary" @click="moveDown(row)">下移</t-link>
          </t-space>
        </template>
        <template #enabled="{ row }">
          <t-switch v-model="row.enabled" @change="() => toggleStatus(row)"/>
        </template>
      </t-table>
    </div>

    <div class="simple-page__footer">
      <t-button theme="primary" style="border-radius: 4px" :loading="loading" @click="saveOrder">保存</t-button>
    </div>
  </div>
</template>

<script>
import PricingPolicy from '@js/api/basic/PricingPolicy';
import {MessagePlugin} from 'tdesign-vue-next';
export default {
  name: 'PricingPolicyList',
  data() {
    return {
      loading: false,
      dataList: [],
      selected: '销售价格取数',
      params: {policyType: '销售价格取数'},
      columns: [
        {colKey: 'ops', title: '操作', width: 120, fixed: 'left', align: 'center'},
        {colKey: 'policySource', title: '取数来源', width: 160},
        {colKey: 'remarks', title: '应用说明', minWidth: 200, ellipsis: true},
        {colKey: 'enabled', title: '状态', width: 100, align: 'center', fixed: 'right'}
      ]
    };
  },
  methods: {
    onTabChange(value) {
      this.params.policyType = value;
      this.loadList();
    },
    doSearch() {
      this.loadList();
    },
    loadList() {
      this.loading = true;
      PricingPolicy.list(this.params)
        .then(({data}) => {
          this.dataList = Array.isArray(data) ? data : [];
        })
        .finally(() => (this.loading = false));
    },
    moveUp(row) {
      const index = this.dataList.findIndex((item) => item.id === row.id);
      if (index <= 0) {
        MessagePlugin.warning('已经是第一条数据了');
        return;
      }
      const newList = [...this.dataList];
      [newList[index - 1], newList[index]] = [newList[index], newList[index - 1]];
      this.dataList = newList;
    },
    moveDown(row) {
      const index = this.dataList.findIndex((item) => item.id === row.id);
      if (index >= this.dataList.length - 1) {
        MessagePlugin.warning('已经是最后一条数据了');
        return;
      }
      const newList = [...this.dataList];
      [newList[index], newList[index + 1]] = [newList[index + 1], newList[index]];
      this.dataList = newList;
    },
    saveOrder() {
      PricingPolicy.sort({dataList: this.dataList}).then(() => {
        MessagePlugin.success('保存成功');
      });
    },
    toggleStatus() {
      // 与原逻辑一致：仅本地切换，随排序一并保存
    }
  },
  created() {
    this.loadList();
  }
};
</script>

<style scoped>

.simple-page__footer {
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  padding: 10px 0;
  border-top: 1px solid var(--td-component-border, #dcdcdc);
}
</style>
