<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button v-auth="'role:edit'" theme="primary" style="border-radius: 4px" @click="showForm()">新 增</t-button>
        <t-input
            v-model="params.name"
            clearable
            placeholder="请输入角色名称"
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
            <t-link theme="primary" @click="showGrantMenu(row)">可用菜单</t-link>
            <t-link v-auth="'role:edit'" theme="primary" @click="showForm(row)"><t-icon name="edit"/></t-link>
            <t-link v-if="$can('role:delete') && !row.systemDefault" theme="primary" @click="doRemove(row)"><t-icon name="delete"/></t-link>
          </t-space>
        </template>
        <template #systemDefault="{ row }">
          <t-tag :theme="row.systemDefault ? 'primary' : 'warning'" variant="light">
            {{ row.systemDefault ? '是' : '否' }}
          </t-tag>
        </template>
      </t-table>
    </div>

    <div class="simple-page__pager">
      <t-pagination
          v-model:current="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :show-jumper="true"
          :show-page-size="true"
          :popup-props="{ attach: 'body' }"
          @change="onPageChange"
      />
    </div>
  </div>
</template>

<script>
import Role from '@js/api/setting/Role';
import RoleForm from './RoleForm.vue';
import GrantMenu from './GrantMenu.vue';
import {MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import {openDialog, openDrawer, closeDialog} from '@common/dialog';
import {h} from 'vue';
export default {
  name: 'RoleList',
  data() {
    return {
      loading: false,
      params: {
        type: 0,
        name: ''
      },
      dataList: [],
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      columns: [
        {colKey: 'ops', title: '操作', width: 180, fixed: 'left', align: 'center'},
        {colKey: 'name', title: '名称', minWidth: 160, ellipsis: true},
        {colKey: 'systemDefault', title: '是否默认', width: 100, align: 'center'}
      ]
    };
  },
  computed: {
    queryParams() {
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize
      });
    }
  },
  methods: {
    showForm(entity) {
      let type = 0;
      let dialogId = openDialog({
        header: '角色信息',
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '400px',
        body: h(RoleForm, {
          entity,
          type,
          onClose: () => {
            closeDialog(dialogId);
          },
          onSuccess: () => {
            this.doSearch();
            closeDialog(dialogId);
          }
        })
      });
    },
    showGrantMenu(entity) {
      let dialogId = openDrawer({
        header: entity.name + '-可用菜单',
        size: '40vw',
        body: h(GrantMenu, {
          entity,
          onClose: () => {
            closeDialog(dialogId);
          },
          onSuccess: () => {
            closeDialog(dialogId);
          }
        })
      });
    },
    loadList() {
      this.loading = true;
      Role.list(this.queryParams)
        .then(({data}) => {
          this.dataList = data.results;
          this.pagination.total = data.total;
        })
        .finally(() => (this.loading = false));
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    doRemove(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除角色：${row.name}?`,
        onConfirm: () => {
          Role.remove(row.id).then(() => {
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

