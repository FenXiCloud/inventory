<template>
  <div class="frame-page" style="margin: 0">
    <div class="t-panel p-16px">
      <div class="toolbar">
        <div class="toolbar-left">
          <label for="name" class="mr-10px">用户名</label>
          <t-input id="name" v-model="params.username" class="flex-1" placeholder="请输入用户名"/>
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
        <vxe-column type="seq" width="90" title="序列"/>
        <vxe-column title="账号" field="username"/>
        <vxe-column title="姓名" field="name"/>
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
import UserForm from "./UserForm.vue";
import User from "@js/api/User";
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
  name: "UserList",
  components: {UserForm},
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
        body: h(UserForm, {
          entity,
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
      User.list(this.queryParams).then(({data}) => {
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
          User.remove(row.id).then(() => {
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
          User.resetPassword(row.id).then(() => {
            MessagePlugin.success("重置成功~");
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
