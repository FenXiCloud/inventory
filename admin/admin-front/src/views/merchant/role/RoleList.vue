<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="showForm()">新 增</t-button>
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
          <t-space v-if="!row.systemDefault" size="small">
            <t-link theme="primary" @click="showGrantMenu(row)">可用功能</t-link>
            <t-link theme="primary" @click="showForm(row)"><t-icon name="edit"/></t-link>
            <t-link theme="primary" @click="doRemove(row)"><t-icon name="delete"/></t-link>
          </t-space>
        </template>
        <template #systemDefault="{ row }">
          <t-tag :theme="row.systemDefault ? 'primary' : 'danger'" variant="light">
            {{ row.systemDefault ? '是' : '否' }}
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
import Role from "@js/api/Role";
import RoleForm from "./RoleForm";
import GrantMenu from "./GrantMenu";
import {DialogPlugin, MessagePlugin, DrawerPlugin} from "tdesign-vue-next";
import {h} from "vue";

/**
 * @功能描述: 角色列表
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "RoleList",
  props: {
    merchant: Object,
  },
  data() {
    return {
      loading: false,
      params: {
        type: 0
      },
      dataList: [],
      pagination: {
        page: 1,
        size: 20,
        total: 0
      },
      columns: [
        {colKey: 'ops', title: '操作', width: 200, fixed: 'left', align: 'center'},
        {colKey: 'name', title: '名称', minWidth: 160, ellipsis: true},
        {colKey: 'systemDefault', title: '是否默认', width: 100, align: 'center'}
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
      let type = 0;
      const dialog = DialogPlugin({
        header: "角色信息",
        closeOnOverlayClick: false,
        closeBtn: false,
        footer: false,
        width: '400px',
        body: h(RoleForm, {
          entity, type,
          merchant: this.merchant,
          onClose: () => dialog.hide(),
          onSuccess: () => {
            this.doSearch();
            dialog.hide();
          }
        })
      });
    },
    showGrantMenu(entity) {
      DrawerPlugin({
        header: entity.name + "-可用功能",
        size: '40vw',
        body: h(GrantMenu, {
          entity,
          merchant: this.merchant,
          onClose: () => {},
          onSuccess: () => {},
        })
      });
    },
    loadList() {
      this.loading = true;
      Role.list(this.queryParams).then(({data}) => {
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
        body: `确认删除角色：${row.name}?`,
        onConfirm: () => {
          Role.remove(row.id).then(() => {
            MessagePlugin.success("删除成功~");
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
