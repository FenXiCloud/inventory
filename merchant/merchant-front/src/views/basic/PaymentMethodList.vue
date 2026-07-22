<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="showPaymentMethodForm()">新 增</t-button>
        <t-input
            v-model="params.name"
            clearable
            placeholder="请输入名称"
            style="width: 240px; border-radius: 4px"
            @enter="searchPaymentMethod"
        >
          <template #suffixIcon>
            <t-icon name="search" style="cursor:pointer" @click="searchPaymentMethod"/>
          </template>
        </t-input>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="searchPaymentMethod">查询</t-button>
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
          :data="paymentMethodDataList"
          :columns="columns"
          :loading="loading"
      >
        <template #ops="{ row }">
          <t-space size="small">
            <t-link theme="primary" @click="showPaymentMethodForm(row)"><t-icon name="edit"/></t-link>
            <t-link theme="primary" @click="deletePaymentMethod(row)"><t-icon name="delete"/></t-link>
          </t-space>
        </template>
        <template #enabled="{ row }">
          <t-tag
              :theme="row.enabled ? 'primary' : 'danger'"
              variant="light"
              style="cursor:pointer"
              @click="trigger(row)"
          >
            {{ row.enabled ? '启用' : '禁用' }}
          </t-tag>
        </template>
      </t-table>
    </div>
  </div>
</template>

<script>
import PaymentMethod from '@js/api/basic/PaymentMethod';
import PaymentMethodForm from '@views/basic/PaymentMethodForm.vue';
import {MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import {openDialog, closeDialog} from '@common/dialog';
import {h} from 'vue';
export default {
  name: 'PaymentMethodList',
  data() {
    return {
      loading: false,
      paymentMethodDataList: [],
      params: {name: ''},
      columns: [
        {colKey: 'ops', title: '操作', width: 90, fixed: 'left', align: 'center'},
        {colKey: 'name', title: '名称', minWidth: 200, ellipsis: true},
        {colKey: 'enabled', title: '状态', width: 90, align: 'center', fixed: 'right'}
      ]
    };
  },
  methods: {
    showPaymentMethodForm(entity) {
      const dialogId = openDialog({
        header: '结算方式',
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '400px',
        body: h(PaymentMethodForm, {
          entity: entity || null,
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.searchPaymentMethod();
            closeDialog(dialogId);
          }
        })
      });
    },
    searchPaymentMethod() {
      this.loadPaymentMethod();
    },
    loadPaymentMethod() {
      this.loading = true;
      const query = {};
      if (this.params.name) query.name = this.params.name;
      PaymentMethod.list(query)
        .then(({data}) => {
          this.paymentMethodDataList = Array.isArray(data) ? data : [];
        })
        .finally(() => (this.loading = false));
    },
    deletePaymentMethod(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除：${row.name}?`,
        onConfirm: () => {
          PaymentMethod.delete(row.id).then(() => {
            MessagePlugin.success('删除成功~');
            this.loadPaymentMethod();
          });
        }
      });
    },
    trigger(row) {
      const enabled = !row.enabled;
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认要「${enabled ? '启用' : '禁用'}」结算方式：${row.name}?`,
        onConfirm: () => {
          PaymentMethod.save({id: row.id, enabled}).then(() => {
            MessagePlugin.success('操作成功~');
            this.loadPaymentMethod();
          });
        }
      });
    }
  },
  created() {
    this.loadPaymentMethod();
  }
};
</script>

