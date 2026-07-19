<template>
  <div class="frame-page" style="margin: 0">
    <div class="t-panel p-16px">
      <div class="toolbar">
        <div class="toolbar-left">
          <label for="username" class="mr-10px">账号</label>
          <t-input id="username" v-model="params.username" class="flex-1" placeholder="请输入账号"/>
          <label for="name" class="mr-10px">姓名</label>
          <t-input id="name" v-model="params.name" class="flex-1" placeholder="请输入姓名"/>
          <t-button theme="primary" :loading="loading" @click="doSearch">查询</t-button>
        </div>
        <div class="toolbar-right">
          <t-button @click="showForm()" theme="primary">添加</t-button>
        </div>
      </div>
      <vxe-table row-id="id"
                 ref="table"
                 :data="dataList"
                 highlight-hover-row
                 show-overflow
                 :row-config="{height: 48}"
                 :loading="loading">
        <vxe-column type="seq" width="60" title="序列"/>
        <vxe-column title="账号" field="username"/>
        <vxe-column title="姓名" field="name"/>
        <vxe-column title="角色" field="roleName"/>
        <vxe-column title="默认管理员" field="systemDefault">
          <template #default="{row:{systemDefault}}">
            <t-tag theme="primary" v-if="systemDefault">是</t-tag>
            <t-tag theme="warning" v-else>否</t-tag>
          </template>
        </vxe-column>
        <vxe-column title="状态" field="enabled">
          <template #default="{row:{enabled}}">
            <t-tag theme="primary" v-if="enabled">启用</t-tag>
            <t-tag theme="danger" v-else>禁用</t-tag>
          </template>
        </vxe-column>
        <vxe-column title="操作" align="center" width="200">
          <template #default="{row}">
            <div class="flex items-center justify-center">
              <span class="primary-color text-hover " @click="resetPassword(row)">重置密码</span>
              <span class=" primary-color text-hover ml-10px" @click="showForm(row)">编辑</span>
              <template v-if="!row.systemDefault">
                <span class="primary-color ml-10px text-hover" @click="doRemove(row)">删除</span>
                <t-dropdown trigger="hover" class="ml-8px">
                  <span class="primary-color text-hover cursor-pointer">更多</span>
                  <t-dropdown-menu slot="dropdown">
                    <t-dropdown-item value="enabled" @click="trigger('enabled', row)">启用</t-dropdown-item>
                    <t-dropdown-item value="disabled" @click="trigger('disabled', row)">禁用</t-dropdown-item>
                  </t-dropdown-menu>
                </t-dropdown>
              </template>
            </div>
          </template>
        </vxe-column>
      </vxe-table>
      <t-pagination class="mt-16px" v-model:current="pagination.page" :total="pagination.total" :page-size="pagination.size" @change="pageChange" size="small"/>
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
      checkedRows: [],
      dataList: [],
      pagination: {
        page: 1,
        size: 20,
        total: 0
      },
    }
  },
  computed: {
    queryParams() {
      return Object.assign(this.params, {
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
    pageChange() {
      this.loadList();
    },
    tableCheck() {
      this.checkedRows = this.$refs.table.getCheckboxRecords();
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
