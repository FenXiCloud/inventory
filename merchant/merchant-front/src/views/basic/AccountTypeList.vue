<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line align="center">
        <t-button v-auth="'accountType:edit'" theme="primary" style="border-radius: 4px" @click="showForm()">新 增</t-button>
        <t-radio-group
            v-model="params.costType"
            variant="default-filled"
            style="border-radius: 4px"
            @change="onCostTypeChange"
        >
          <t-radio-button value="支出">支出</t-radio-button>
          <t-radio-button value="收入">收入</t-radio-button>
        </t-radio-group>
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
      >
        <template #ops="{ row }">
          <t-space size="small">
            <t-link v-auth="'accountType:edit'" theme="primary" @click="showForm(null, row)">下级</t-link>
            <t-link v-auth="'accountType:edit'" theme="primary" @click="showForm(row)"><t-icon name="edit"/></t-link>
            <t-link v-auth="'accountType:delete'" theme="primary" @click="doRemove(row)"><t-icon name="delete"/></t-link>
          </t-space>
        </template>
        <template #name="{ row }">
          <div class="account-type-name" :style="{ paddingLeft: `${row._level * 22}px` }">
            <span
                v-if="row._hasChildren"
                class="account-type-name__icon"
                @click.stop="toggleExpand(row.id)"
            >
              <t-icon :name="isExpanded(row.id) ? 'caret-down-small' : 'caret-right-small'"/>
            </span>
            <span v-else class="account-type-name__spacer"/>
            <span class="account-type-name__text" :title="row.name">{{ row.name }}</span>
          </div>
        </template>
        <template #enabled="{ row }">
          <t-tag
              v-auth="'accountType:edit'"
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
import {toArrayTree} from '@common/utils';

export default {
  name: 'AccountTypeList',
  data() {
    return {
      loading: false,
      treeData: [],
      expandedIds: {},
      params: {
        name: '',
        costType: '支出'
      },
      columns: [
        {colKey: 'ops', title: '操作', width: 140, fixed: 'left', align: 'center'},
        {colKey: 'name', title: '名称', minWidth: 220, ellipsis: true},
        {colKey: 'costType', title: '收支类型', width: 100},
        {colKey: 'enabled', title: '状态', width: 90, align: 'center', fixed: 'right'}
      ]
    };
  },
  computed: {
    dataList() {
      return this.flattenTree(this.treeData);
    }
  },
  methods: {
    isExpanded(id) {
      return !!this.expandedIds[id];
    },
    toggleExpand(id) {
      this.expandedIds = {
        ...this.expandedIds,
        [id]: !this.expandedIds[id]
      };
    },
    expandAll(nodes, map = {}) {
      (nodes || []).forEach((node) => {
        if (node.children && node.children.length) {
          map[node.id] = true;
          this.expandAll(node.children, map);
        }
      });
      return map;
    },
    flattenTree(nodes, level = 0, result = []) {
      (nodes || []).forEach((node) => {
        const children = node.children || [];
        const hasChildren = children.length > 0;
        result.push({
          id: node.id,
          name: node.name,
          costType: node.costType,
          enabled: node.enabled,
          pid: node.pid,
          _level: level,
          _hasChildren: hasChildren,
          _childCount: children.length
        });
        if (hasChildren && this.isExpanded(node.id)) {
          this.flattenTree(children, level + 1, result);
        }
      });
      return result;
    },
    showForm(entity, parent) {
      const isChild = !!parent;
      const dialogId = openDialog({
        header: entity
          ? '编辑收支类别'
          : (isChild ? `新增下级（${parent.name}）` : `新增${this.params.costType}类别`),
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '420px',
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
    onCostTypeChange() {
      this.params.name = '';
      this.loadList();
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
          const tree = toArrayTree(list, {key: 'id', parentKey: 'pid', children: 'children'});
          this.treeData = tree;
          this.expandedIds = this.expandAll(tree);
        })
        .finally(() => (this.loading = false));
    },
    doRemove(row) {
      if (row._hasChildren || row._childCount > 0) {
        MessagePlugin.warning('请先删除下级类别');
        return;
      }
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
.account-type-name {
  display: flex;
  align-items: center;
  min-width: 0;
}

.account-type-name__icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  margin-right: 4px;
  cursor: pointer;
  color: var(--td-text-color-secondary, #666);
}

.account-type-name__spacer {
  display: inline-block;
  width: 16px;
  flex: 0 0 auto;
}

.account-type-name__text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
