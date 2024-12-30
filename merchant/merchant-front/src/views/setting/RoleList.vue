<template>
  <div class="frame-page" style="margin: 0">
    <div class="h-panel">
      <div class="h-panel-body">
        <div class="table-toolbar">
          <div class="table-toolbar-left">
            <div class="h-input-group">
              <Input id="name" v-model="params.name" class="flex-1" placeholder="请输入角色名称"/>
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
          <vxe-column title="名称" field="name"/>
          <vxe-column title="是否默认" field="systemDefault" width="100" align="center">
            <template #default="{row:{systemDefault}}">
              <Tag color="primary" v-if="systemDefault">是</Tag>
              <Tag color="gray" v-else>否</Tag>
            </template>
          </vxe-column>
          <vxe-column title="操作" align="center" width="300">
            <template #default="{row}">
              <div class="flex items-center justify-center" v-if="!row.systemDefault">
                <span class="primary-color text-hover" @click="showGrantMenu(row)">可用菜单</span>
                <i class="primary-color h-icon-edit ml-10px" @click="showForm(row)"></i>
                <i class="primary-color h-icon-trash ml-10px" @click="doRemove(row)"></i>
              </div>
            </template>
          </vxe-column>
        </vxe-table>
        <!--        <Pagination align="right" class="mt-16px" v-model="pagination" @change="pageChange" small/>-->
      </div>
    </div>
  </div>
</template>

<script>
import Role from "@js/api/Role";
import RoleForm from "./RoleForm.vue";
import GrantMenu from "./GrantMenu.vue";
import {confirm, message} from "heyui.ext";
import {layer} from "@layui/layer-vue";
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
  data() {
    return {
      loading: false,
      params: {
        type: 0
      },
      checkedRows: [],
      dataList: [],
      pagination: {
        page: 1,
        size: 20,
        total: 0
      }
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
      let type = 0;
      let layerId = layer.open({
        title: "角色信息",
        shadeClose: false,
        closeBtn: false,
        area: ['400px', '230px'],
        content: h(RoleForm, {
          entity, type,
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
    showGrantMenu(entity) {
      let layerId = layer.drawer({
        title: entity.name + "-可用菜单",
        area: ['40vw', '100vh'],
        content: h(GrantMenu, {
          entity,
          onClose: () => {
            layer.close(layerId);
          },
          onSuccess: () => {
            layer.close(layerId);
          }
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
        content: `确认删除角色：${row.name}?`,
        onConfirm: () => {
          Role.remove(row.id).then(() => {
            message("删除成功~");
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
