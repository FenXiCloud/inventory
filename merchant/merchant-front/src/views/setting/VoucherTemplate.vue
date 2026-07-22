<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="showForm()">新 增</t-button>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="loadList">刷新</t-button>
      </t-space>
    </div>

    <div class="simple-page__body">
      <div v-if="!loading && !dataList.length" class="simple-page__empty">暂无凭证模板，请点击「新增」</div>
      <div v-for="item in dataList" :key="item.id" class="tpl-card">
        <div class="tpl-card__header">
          <div class="tpl-card__meta">
            <span>模板名称：{{ item.title }}</span>
            <span>模板类型：{{ item.type }}</span>
            <span>凭证：{{ item.word }}</span>
          </div>
          <t-space size="small">
            <t-link theme="primary" @click="showForm(item.id)">编辑</t-link>
            <t-link theme="danger" @click="doRemove(item.id)">删除</t-link>
          </t-space>
        </div>
        <t-table
            row-key="subjectId"
            size="small"
            bordered
            stripe
            hover
            table-layout="fixed"
            :data="item.details || []"
            :columns="detailColumns"
            empty="暂无科目明细"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { openDialog, closeDialog } from '@common/dialog';
import { h } from 'vue';
import TemplateConfigForm from './TemplateConfigForm.vue';
import FinanceVoucherTemplate from '@js/api/setting/FinanceVoucherTemplate';
import { MessagePlugin } from 'tdesign-vue-next';
import { DialogPlugin } from '@common/dialog-plugin';

export default {
  name: 'VoucherTemplate',
  data() {
    return {
      loading: false,
      dataList: [],
      detailColumns: [
        { colKey: 'subjectName', title: '会计科目', minWidth: 180, ellipsis: true },
        { colKey: 'balanceDirection', title: '借贷方向', width: 100, align: 'center' }
      ]
    };
  },
  methods: {
    showForm(id) {
      const dialogId = openDialog({
        header: '凭证模板',
        closeOnOverlayClick: false,
        width: '800px',
        body: h(TemplateConfigForm, {
          id,
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
      FinanceVoucherTemplate.list({})
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
        body: '是否删除当前数据?',
        onConfirm: () => {
          FinanceVoucherTemplate.delete(id).then(() => {
            MessagePlugin.success('操作成功～');
            this.loadList();
          });
        }
      });
    }
  },
  created() {
    this.loadList();
  }
};
</script>

<style scoped>

.simple-page__body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-bottom: 12px;
}

.simple-page__empty {
  padding: 48px 0;
  text-align: center;
  color: #8f959e;
  font-size: 14px;
}

.tpl-card {
  border: 1px solid var(--td-component-border, #e7e7e7);
  border-radius: 4px;
  padding: 12px;
  margin-bottom: 12px;
}

.tpl-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.tpl-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 14px;
  color: #333;
  min-width: 0;
}
</style>
