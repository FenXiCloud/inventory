<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="showForm()">新 增</t-button>
        <t-input
            v-model="params.username"
            clearable
            placeholder="请输入账号"
            style="width: 180px; border-radius: 4px"
            @enter="doSearch"
        >
          <template #suffixIcon>
            <t-icon name="search" style="cursor:pointer" @click="doSearch"/>
          </template>
        </t-input>
        <t-input
            v-model="params.name"
            clearable
            placeholder="请输入姓名"
            style="width: 180px; border-radius: 4px"
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
            <t-link theme="primary" @click="resetPassword(row)">重置密码</t-link>
            <t-link theme="primary" @click="showForm(row)"><t-icon name="edit"/></t-link>
            <template v-if="!row.systemDefault">
              <t-link theme="primary" @click="doRemove(row)"><t-icon name="delete"/></t-link>
              <t-dropdown
                  trigger="hover"
                  :options="moreOptions"
                  @click="(data) => trigger(data.value, row)"
              >
                <t-link theme="primary">更多</t-link>
              </t-dropdown>
            </template>
          </t-space>
        </template>
        <template #systemDefault="{ row }">
          <t-tag :theme="row.systemDefault ? 'primary' : 'warning'" variant="light">
            {{ row.systemDefault ? '是' : '否' }}
          </t-tag>
        </template>
        <template #enabled="{ row }">
          <t-tag :theme="row.enabled ? 'primary' : 'danger'" variant="light">
            {{ row.enabled ? '启用' : '禁用' }}
          </t-tag>
        </template>
      </t-table>
    </div>

    <div class="simple-page__pager">
      <t-pagination
          v-model:current="pagination.page"
          v-model:page-size="pagination.size"
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
import AdminForm from "./AdminForm.vue";
import Admin from "@js/api/Admin";
import {DialogPlugin, MessagePlugin} from "tdesign-vue-next";
import {h} from "vue";

/**
 * @功能描述: 管理员列表
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "AdminList",
  components: {AdminForm},
  props: {
    merchant: Object,
  },
  data() {
    return {
      loading: false,
      params: {
        name: null,
        username: null,
      },
      dataList: [],
      pagination: {
        page: 1,
        size: 20,
        total: 0
      },
      moreOptions: [
        {content: '启用', value: 'enabled'},
        {content: '禁用', value: 'disabled'}
      ],
      columns: [
        {colKey: 'ops', title: '操作', width: 200, fixed: 'left', align: 'center'},
        {colKey: 'username', title: '账号', minWidth: 120},
        {colKey: 'name', title: '姓名', minWidth: 120},
        {colKey: 'roleName', title: '角色', minWidth: 120},
        {colKey: 'systemDefault', title: '默认管理员', width: 110, align: 'center'},
        {colKey: 'enabled', title: '状态', width: 90, align: 'center', fixed: 'right'}
      ]
    }
  },
  computed: {
    queryParams() {
      return Object.assign({}, this.params, {
        merchantId: this.merchant.id,
        page: this.pagination.page,
        pageSize: this.pagination.size
      })
    }
  },
  methods: {
    showForm(entity) {
      const dialog = DialogPlugin({
        header: "管理员信息",
        closeOnOverlayClick: false,
        closeBtn: false,
        footer: false,
        width: '500px',
        body: h(AdminForm, {
          entity, merchant: this.merchant,
          onClose: () => dialog.hide(),
          onSuccess: () => {
            this.doSearch();
            dialog.hide();
          }
        })
      });
    },
    loadList() {
      this.loading = true;
      Admin.list(this.queryParams).then(({data}) => {
        this.dataList = data.results;
        this.pagination.total = data.total;
      }).finally(() => this.loading = false);
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.size = pageInfo.pageSize;
      this.loadList();
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    doRemove(row) {
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认删除管理员：${row.name}?`,
        onConfirm: () => {
          Admin.remove(row.id).then(() => {
            MessagePlugin.success("删除成功~");
            this.loadList();
          })
        }
      })
    },
    resetPassword(row) {
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认要重置【${row.name}】的登录密码?`,
        onConfirm: () => {
          Admin.resetPassword(row.id).then(() => {
            MessagePlugin.success("重置成功~");
          })
        }
      })
    },
    trigger(code, row) {
      let enabled = code === 'enabled';
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认要「${enabled ? "启用" : "禁用"}」管理员：${row.name}?`,
        onConfirm: () => {
          Admin.save({id: row.id, enabled}).then(() => {
            MessagePlugin.success("操作成功~");
            this.loadList();
          })
        }
      })
    }
  },
  created() {
    this.loadList();
  }
}
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

.simple-page__pager {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid var(--td-component-border, #dcdcdc);
  background: #fff;
}
</style>
