<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-tabs v-model="selected" style="flex: 1" @change="onTabChange">
        <t-tab-panel value="销售价格取数" label="销售价格取数"/>
        <t-tab-panel value="采购价格取数" label="采购价格取数"/>
      </t-tabs>
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
            <t-link theme="primary" :disabled="saving" @click="moveUp(row)">上移</t-link>
            <t-link theme="primary" :disabled="saving" @click="moveDown(row)">下移</t-link>
          </t-space>
        </template>
        <template #enabled="{ row }">
          <t-switch v-model="row.enabled" :disabled="saving" @change="() => persist()"/>
        </template>
      </t-table>
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
      saving: false,
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
    loadList() {
      this.loading = true;
      PricingPolicy.list(this.params)
        .then(({data}) => {
          this.dataList = Array.isArray(data) ? data : [];
        })
        .finally(() => (this.loading = false));
    },
    persist() {
      if (this.saving) return Promise.resolve();
      this.saving = true;
      return PricingPolicy.sort({dataList: this.dataList})
        .then(() => {
          MessagePlugin.success('已保存');
        })
        .catch(() => {
          this.loadList();
        })
        .finally(() => {
          this.saving = false;
        });
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
      this.persist();
    },
    moveDown(row) {
      const index = this.dataList.findIndex((item) => item.id === row.id);
      if (index < 0 || index >= this.dataList.length - 1) {
        MessagePlugin.warning('已经是最后一条数据了');
        return;
      }
      const newList = [...this.dataList];
      [newList[index], newList[index + 1]] = [newList[index + 1], newList[index]];
      this.dataList = newList;
      this.persist();
    }
  },
  created() {
    this.loadList();
  }
};
</script>
