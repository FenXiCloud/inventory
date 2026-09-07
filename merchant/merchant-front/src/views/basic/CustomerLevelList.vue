<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button v-auth="'customerLevel:edit'" theme="primary" style="border-radius: 4px" @click="showCustomerLevelForm()">新 增</t-button>
        <t-input
            v-model="params.name"
            clearable
            placeholder="请输入名称"
            style="width: 240px; border-radius: 4px"
            @enter="searchCustomerLevel"
        >
          <template #suffixIcon>
            <t-icon name="search" style="cursor:pointer" @click="searchCustomerLevel"/>
          </template>
        </t-input>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="searchCustomerLevel">查询</t-button>
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
          :data="customerLevelDataList"
          :columns="columns"
          :loading="loading"
      >
        <template #ops="{ row }">
          <t-space size="small">
            <t-link v-auth="'customerLevel:edit'" theme="primary" @click="showCustomerLevelForm(row)"><t-icon name="edit"/></t-link>
            <t-link v-if="$can('customerLevel:delete') && !row.systemDefault" theme="primary" @click="deleteCustomerLevel(row)">
              <t-icon name="delete"/>
            </t-link>
          </t-space>
        </template>
      </t-table>
    </div>
  </div>
</template>

<script>
import CustomerLevel from '@js/api/basic/CustomerLevel';
import CustomerLevelForm from '@views/basic/CustomerLevelForm.vue';
import {MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import {openDialog, closeDialog} from '@common/dialog';
import {h} from 'vue';
export default {
  name: 'CustomerLevelList',
  data() {
    return {
      loading: false,
      customerLevelDataList: [],
      params: {name: ''},
      columns: [
        {colKey: 'ops', title: '操作', width: 90, fixed: 'left', align: 'center'},
        {colKey: 'name', title: '名称', minWidth: 200, ellipsis: true}
      ]
    };
  },
  methods: {
    showCustomerLevelForm(entity) {
      const dialogId = openDialog({
        header: '客户等级',
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '400px',
        body: h(CustomerLevelForm, {
          entity: entity || null,
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.searchCustomerLevel();
            closeDialog(dialogId);
          }
        })
      });
    },
    searchCustomerLevel() {
      this.loadCustomerLevel();
    },
    loadCustomerLevel() {
      this.loading = true;
      const query = {};
      if (this.params.name) query.name = this.params.name;
      CustomerLevel.list(query)
        .then(({data}) => {
          this.customerLevelDataList = Array.isArray(data) ? data : [];
        })
        .finally(() => (this.loading = false));
    },
    deleteCustomerLevel(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除：${row.name}?`,
        onConfirm: () => {
          CustomerLevel.delete(row.id).then(() => {
            MessagePlugin.success('删除成功~');
            this.loadCustomerLevel();
          });
        }
      });
    }
  },
  created() {
    this.loadCustomerLevel();
  }
};
</script>

