<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="synchronization()">同步钉钉用户</t-button>
        <t-button theme="primary" style="border-radius: 4px" @click="showForm()">新 增</t-button>
        <t-input
            v-model="params.username"
            clearable
            placeholder="请输入用户名"
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
            <t-link theme="primary" @click="resetPassword(row)"><t-icon name="lock-on"/></t-link>
            <t-link theme="primary" @click="showForm(row)"><t-icon name="edit"/></t-link>
            <t-link v-if="!row.systemDefault" theme="primary" @click="doRemove(row)"><t-icon name="delete"/></t-link>
          </t-space>
        </template>
        <template #systemDefault="{ row }">
          <t-tag :theme="row.systemDefault ? 'primary' : 'warning'" variant="light">
            {{ row.systemDefault ? '是' : '否' }}
          </t-tag>
        </template>
        <template #enabled="{ row }">
          <t-tag
              v-if="!row.systemDefault"
              :theme="row.enabled ? 'primary' : 'danger'"
              variant="light"
              style="cursor: pointer"
              @click="trigger(row)"
          >
            {{ row.enabled ? '启用' : '禁用' }}
          </t-tag>
          <t-tag v-else :theme="row.enabled ? 'primary' : 'danger'" variant="light">
            {{ row.enabled ? '启用' : '禁用' }}
          </t-tag>
        </template>
      </t-table>
    </div>
  </div>
</template>

<script>
import AdminForm from './AdminForm.vue';
import Admin from '@js/api/setting/Admin';
import {DialogPlugin, MessagePlugin} from 'tdesign-vue-next';
import {openDialog, closeDialog} from '@common/dialog';
import {h} from 'vue';

/**
 * @功能描述: 用户管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: 'AdminList',
  data() {
    return {
      loading: false,
      params: {
        name: null,
        username: null,
        phone: null
      },
      dataList: [],
      columns: [
        {colKey: 'ops', title: '操作', width: 120, fixed: 'left', align: 'center'},
        {colKey: 'username', title: '账号', minWidth: 120},
        {colKey: 'name', title: '姓名', minWidth: 120},
        {colKey: 'mobile', title: '电话', minWidth: 120},
        {colKey: 'roleName', title: '角色', minWidth: 120},
        {colKey: 'systemDefault', title: '默认用户', width: 100, align: 'center'},
        {colKey: 'enabled', title: '状态', width: 90, align: 'center', fixed: 'right'}
      ]
    };
  },
  computed: {
    queryParams() {
      return Object.assign({}, this.params);
    }
  },
  methods: {
    synchronization() {
      this.loading = true;
      Admin.addUserByDingDing()
        .then(({data}) => {
          // this.dataList = data;
        })
        .finally(() => (this.loading = false));
    },
    showForm(entity) {
      let dialogId = openDialog({
        header: '用户信息',
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '600px',
        body: h(AdminForm, {
          entity,
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
    loadList() {
      this.loading = true;
      Admin.list(this.queryParams)
        .then(({data}) => {
          this.dataList = data;
        })
        .finally(() => (this.loading = false));
    },
    doSearch() {
      this.loadList();
    },
    doRemove(row) {
      DialogPlugin.confirm({
        title: '系统提示',
        content: `确认删除用户：${row.name}?`,
        onConfirm: () => {
          Admin.remove(row.id).then(() => {
            MessagePlugin.success('删除成功~');
            this.loadList();
          });
        }
      });
    },
    resetPassword(row) {
      DialogPlugin.confirm({
        title: '系统提示',
        content: `确认要重置【${row.name}】的登录密码?`,
        onConfirm: () => {
          Admin.resetPassword(row.id).then(() => {
            MessagePlugin.success('重置成功~');
          });
        }
      });
    },
    trigger(row) {
      let enabled = !row.enabled;
      DialogPlugin.confirm({
        title: '系统提示',
        content: `确认要「${enabled ? '启用' : '禁用'}」用户：${row.name}?`,
        onConfirm: () => {
          Admin.save({id: row.id, enabled}).then(() => {
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
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
</style>
