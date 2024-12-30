<template>
  <div class="frame-page" style="margin: 0">
    <div class="h-panel">
      <div class="h-panel-body">
        <div class="table-toolbar">
          <div class="table-toolbar-left">
            <div class="h-input-group">
              <Input id="name" v-model="params.username" class="flex-1" placeholder="请输入用户名"/>
              <span class="h-input-addon" @click="doSearch" :loading="loading"><i class="h-icon-search"></i></span>
            </div>
          </div>
          <div class="table-toolbar-right">
            <Button @click="showForm()" color="primary">新 增</Button>
          </div>
        </div>
        <vxe-table row-id="id"
                   ref="table"
                   :data="dataList"
                   highlight-hover-row
                   show-overflow
                   :row-config="{height: 48}"
                   :column-config="{resizable: true}"
                   :loading="loading">
          <vxe-column type="seq" width="40" title="#"/>
          <vxe-column title="账号" field="username"/>
          <vxe-column title="姓名" field="name"/>
          <vxe-column title="电话" field="mobile"/>
          <vxe-column title="角色" field="roleName"/>
          <vxe-column title="默认用户" field="systemDefault" width="100" align="center">
            <template #default="{row:{systemDefault}}">
              <Tag color="primary" v-if="systemDefault">是</Tag>
              <Tag color="yellow" v-else>否</Tag>
            </template>
          </vxe-column>
          <vxe-column title="状态" field="enabled" width="100" align="center">
            <template #default="{row}">
              <template v-if="!row.systemDefault">
                <Tag color="primary" v-if="row.enabled" @click="trigger(row)">启用</Tag>
                <Tag color="red" v-else @click="trigger(row)">禁用</Tag>
              </template>
              <template v-else>
                <Tag color="primary" v-if="row.enabled">启用</Tag>
                <Tag color="red" v-else>禁用</Tag>
              </template>
            </template>
          </vxe-column>
          <vxe-column title="操作" align="center" width="200">
            <template #default="{row}">
              <div class="flex items-center justify-center">
                <i class="primary-color h-icon-lock ml-10px" @click="resetPassword(row)"></i>
                <i class="primary-color h-icon-edit ml-10px" @click="showForm(row)"></i>
                <template v-if="!row.systemDefault">
                  <i class="primary-color h-icon-trash ml-10px" @click="doRemove(row)"></i>
                </template>
              </div>
            </template>
          </vxe-column>
        </vxe-table>
      </div>
    </div>
  </div>
</template>

<script>
import AdminForm from "./AdminForm.vue";
import Admin from "@js/api/Admin";
import {confirm, message} from "heyui.ext";
import {layer} from "@layui/layer-vue";
import {h} from "vue";

/**
 * @功能描述: 用户管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "AdminList",
  components: {AdminForm},
  data() {
    return {
      loading: false,
      params: {
        name: null,
        username: null,
        phone: null,
      },
      checkedRows: [],
      dataList: [],
      pagination: {
        page: 1,
        size: 20,
        total: 0
      },
      param: [
        {title: '启用', key: 'enabled'},
        {title: '禁用', key: 'disabled'},
      ]
    }
  },
  computed: {
    queryParams() {
      return Object.assign(this.params, {})
    }
  },
  methods: {
    showForm(entity) {
      let layerId = layer.open({
        title: "用户信息",
        shadeClose: false,
        closeBtn: false,
        area: ['600px', '500px'],
        content: h(AdminForm, {
          entity,
          onClose: () => {
            layer.close(layerId);
          },
          onSuccess: () => {
            this.doSearch();
            layer.close(layerId);
          }
        })
      });
    },
    loadList() {
      this.loading = true;
      Admin.list(this.queryParams).then(({data}) => {
        this.dataList = data;
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
      confirm({
        title: "系统提示",
        content: `确认删除用户：${row.name}?`,
        onConfirm: () => {
          Admin.remove(row.id).then(() => {
            message("删除成功~");
            this.loadList();
          })
        }
      })
    },
    resetPassword(row) {
      confirm({
        title: "系统提示",
        content: `确认要重置【${row.name}】的登录密码?`,
        onConfirm: () => {
          Admin.resetPassword(row.id).then(() => {
            message("重置成功~");
          })
        }
      })
    },
    trigger(row) {
      let enabled = !row.enabled;
      confirm({
        title: "系统提示",
        content: `确认要「${enabled ? "启用" : "禁用"}」用户：${row.name}?`,
        onConfirm: () => {
          Admin.save({id: row.id, enabled}).then(() => {
            message("操作成功~");
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
