<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="showForm()">新 增</t-button>
        <span>类型：</span>
        <t-select
            v-model="params.costType"
            :options="costTypeOptions"
            :keys="{ value: 'key', label: 'title' }"
            :clearable="false"
            style="width: 120px; border-radius: 4px"
            @change="loadList"
        />
        <t-input
            v-model="params.name"
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
          table-layout="fixed"
          :data="dataList"
          :columns="columns"
          :loading="loading"
          :tree="treeConfig"
      >
        <template #ops="{ row }">
          <t-space size="small">
            <t-link theme="primary" @click="showForm(null, row)">下级</t-link>
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
import AccountType from '@js/api/basic/AccountType';
import AccountTypeForm from '@views/basic/AccountTypeForm.vue';
import {MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import {openDialog, closeDialog} from '@common/dialog';
import {h} from 'vue';
import {toArrayTree} from 'xe-utils';
import {costTypes} from '@common/dict';

/**
 * @功能描述: 账户收支类别
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: 'AccountTypeList',
  data() {
    return {
      loading: false,
      dataList: [],
      params: {
        name: '',
        costType: '支出'
      },
      costTypeOptions: costTypes,
      treeConfig: {
        childrenKey: 'children',
        treeNodeColumnIndex: 1,
        defaultExpandAll: true,
        indent: 24
      },
      columns: [
        {colKey: 'ops', title: '操作', width: 140, fixed: 'left', align: 'center'},
        {colKey: 'name', title: '名称', minWidth: 200, ellipsis: true},
        {colKey: 'costType', title: '收支类型', width: 100},
        {colKey: 'enabled', title: '状态', width: 90, align: 'center', fixed: 'right'}
      ]
    };
  },
  methods: {
    showForm(entity, parent) {
      const dialogId = openDialog({
        header: '收支类别信息',
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '400px',
        body: h(AccountTypeForm, {
          entity: entity || null,
          parent: parent || null,
          defaultCostType: this.params.costType,
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
      const query = {costType: this.params.costType};
      if (this.params.name) query.name = this.params.name;
      AccountType.list(query)
        .then(({data}) => {
          const list = Array.isArray(data) ? data : [];
          this.dataList = toArrayTree(list, {key: 'id', parentKey: 'pid', children: 'children'});
        })
        .finally(() => (this.loading = false));
    },
    doRemove(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除：${row.name}?`,
        onConfirm: () => {
          AccountType.remove(row.id).then(() => {
            MessagePlugin.success('删除成功~');
            this.loadList();
          });
        }
      });
    },
    trigger(row) {
      const enabled = !row.enabled;
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认要「${enabled ? '启用' : '禁用'}」名称：${row.name}?`,
        onConfirm: () => {
          AccountType.save({id: row.id, enabled}).then(() => {
            MessagePlugin.success('操作成功~');
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
  flex: 1 1 0;
  height: 0;
  min-height: 0;
  overflow: hidden;
}
</style>
