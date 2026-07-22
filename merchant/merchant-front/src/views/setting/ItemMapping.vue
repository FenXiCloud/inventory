<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-tabs v-model="selected" @change="changeTab">
          <t-tab-panel value="0" label="客户" />
          <t-tab-panel value="1" label="供应商" />
          <t-tab-panel value="6" label="产品" />
        </t-tabs>
        <t-button theme="primary" style="border-radius: 4px" @click="showForm()">新 增</t-button>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" @click="batchShowForm()">批量新增</t-button>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="loadList">刷新</t-button>
      </t-space>
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
          empty="暂无映射，请点击「新增」"
      >
        <template #ops="{ row }">
          <t-space size="small">
            <t-link theme="primary" @click="showForm(row.id)">编辑</t-link>
            <t-link theme="danger" @click="doRemove(row.id)">删除</t-link>
          </t-space>
        </template>
      </t-table>
    </div>
  </div>
</template>

<script>
import { openDialog, closeDialog } from '@common/dialog';
import { h } from 'vue';
import FinanceItemMap from '@js/api/setting/FinanceItemMap';
import {MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import FinanceItemMapForm from './FinanceItemMapForm.vue';
import FinanceItemMapBatchForm from './FinanceItemMapBatchForm.vue';

const CATEGORY_LABEL = { 0: '客户', 1: '供应商', 6: '产品' };

export default {
  name: 'ItemMapping',
  data() {
    return {
      loading: false,
      dataList: [],
      selected: '0',
      columns: [
        { colKey: 'financeName', title: '财务软件名称', minWidth: 140, ellipsis: true },
        { colKey: 'inventoryName', title: '进销存名称', minWidth: 140, ellipsis: true },
        { colKey: 'financeCode', title: '财务 code', minWidth: 120, ellipsis: true },
        { colKey: 'inventoryCode', title: '进销存 code', minWidth: 120, ellipsis: true },
        { colKey: 'ops', title: '操作', width: 120, align: 'center', fixed: 'right' }
      ]
    };
  },
  methods: {
    showForm(id) {
      const label = CATEGORY_LABEL[this.selected] || '辅助项';
      const dialogId = openDialog({
        header: `${label}-辅助项映射`,
        closeOnOverlayClick: false,
        width: '800px',
        body: h(FinanceItemMapForm, {
          id,
          categoryId: this.selected,
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.loadList();
            closeDialog(dialogId);
          }
        })
      });
    },
    batchShowForm() {
      const label = CATEGORY_LABEL[this.selected] || '辅助项';
      const dialogId = openDialog({
        header: `${label}-批量映射`,
        closeOnOverlayClick: false,
        width: '800px',
        body: h(FinanceItemMapBatchForm, {
          categoryId: this.selected,
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.loadList();
            closeDialog(dialogId);
          }
        })
      });
    },
    loadList() {
      this.loading = true;
      FinanceItemMap.list({ categoryId: this.selected })
        .then(({ data }) => {
          this.dataList = data || [];
        })
        .finally(() => {
          this.loading = false;
        });
    },
    doRemove(id) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: '是否删除当前映射?',
        onConfirm: () => {
          FinanceItemMap.delete(id).then(() => {
            MessagePlugin.success('删除成功');
            this.loadList();
          });
        }
      });
    },
    changeTab() {
      this.loadList();
    }
  },
  created() {
    this.loadList();
  }
};
</script>

