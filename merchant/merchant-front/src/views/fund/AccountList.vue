<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="showForm()">新 增</t-button>
        <t-input
            v-model="params.filter"
            clearable
            placeholder="请输入名称"
            style="width: 240px; border-radius: 4px"
            @enter="doSearch"
        >
          <template #suffixIcon>
            <t-icon name="search" style="cursor:pointer" @click="doSearch"/>
          </template>
        </t-input>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="doSearch">查询</t-button>
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
          table-layout="auto"
          :data="dataList"
          :columns="columns"
          :loading="loading"
      >
        <template #ops="{ row }">
          <t-space size="small">
            <t-link theme="primary" @click="showForm(row)"><t-icon name="edit"/></t-link>
            <t-link theme="primary" @click="doRemove(row)"><t-icon name="delete"/></t-link>
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
import Account from '@js/api/fund/Account';
import AccountForm from './AccountForm.vue';
import {DialogPlugin, MessagePlugin} from 'tdesign-vue-next';
import {openDialog, closeDialog} from '@common/dialog';
import {h} from 'vue';

/**
 * @功能描述: 账户管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: 'AccountList',
  data() {
    return {
      loading: false,
      dataList: [],
      params: {filter: null},
      columns: [
        {colKey: 'ops', title: '操作', width: 90, fixed: 'left', align: 'center'},
        {colKey: 'accountType', title: '账户类型', width: 100},
        {colKey: 'accountTypeItem', title: '账户类型名称', width: 120},
        {colKey: 'name', title: '名称', minWidth: 140, ellipsis: true},
        {colKey: 'currency', title: '币别', width: 80},
        {colKey: 'balance', title: '账户余额', width: 120},
        {colKey: 'enabled', title: '状态', width: 90, align: 'center', fixed: 'right'}
      ]
    };
  },
  methods: {
    trigger(row) {
      const enabled = !row.enabled;
      DialogPlugin.confirm({
        title: '系统提示',
        content: `确认要「${enabled ? '启用' : '禁用'}」名称：${row.name}?`,
        onConfirm: () => {
          Account.save({...row, enabled}).then(() => {
            MessagePlugin.success('操作成功~');
            this.loadList();
          });
        }
      });
    },
    showForm(entity) {
      const dialogId = openDialog({
        header: '账户信息',
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '520px',
        body: h(AccountForm, {
          entity,
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.doSearch();
            closeDialog(dialogId);
          }
        })
      });
    },
    doSearch() {
      this.loadList();
    },
    loadList() {
      this.loading = true;
      Account.list(this.params)
        .then(({data}) => {
          this.dataList = data || [];
        })
        .finally(() => (this.loading = false));
    },
    doRemove(row) {
      DialogPlugin.confirm({
        title: '系统提示',
        content: `确认删除：${row.name}?`,
        onConfirm: () => {
          Account.remove(row.id).then(() => {
            MessagePlugin.success('删除成功~');
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
.simple-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
  padding: 0 12px;
  box-sizing: border-box;
  overflow: hidden;
}

.simple-page__toolbar {
  flex-shrink: 0;
  padding: 8px 0;
}

.simple-page__table {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
</style>
