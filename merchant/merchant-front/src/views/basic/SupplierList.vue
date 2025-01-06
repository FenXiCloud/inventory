<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <Search
            v-model.trim="params.filter"
            search-button-theme="h-btn-default"
            show-search-button
            class="w-360px"
            placeholder="请输入名称/编码/联系人/电话"
            @search="doSearch">
          <i class="h-icon-search"/>
        </Search>
      </template>
      <template #tools>
        <Button @click="showForm()" color="primary">新 增</Button>
        <Button @click="addCategoryForm()" >新增分类</Button>
      </template>
    </vxe-toolbar>
    <div class="flex-1">
      <vxe-table row-id="id"
                 ref="table"
                 height="auto"
                 :data="dataList"
                 highlight-hover-row
                 show-overflow
                 :row-config="{height: 48}"
                 :column-config="{resizable: true}"
                 :loading="loading">
        <vxe-column title="序号" type="seq" width="60" align="center" />
        <vxe-column title="分类" field="categoryName" width="100"/>
        <vxe-column title="编码" field="code" width="80"/>
        <vxe-column title="名称" field="name" min-width="150"/>
        <vxe-column title="联系人" field="linkman" width="100"/>
        <vxe-column title="电话" field="phone" width="150"/>
        <vxe-column title="地址" field="address" min-width="100"/>
        <vxe-column title="状态" field="enabled" width="60">
          <template #default="{row}">
            <Tag color="primary" v-if="row.enabled">启用</Tag>
            <Tag color="red" v-else>禁用</Tag>
          </template>
        </vxe-column>
        <vxe-column title="操作" align="center" width="150" fixed="right">
          <template #default="{row}">
            <span class="primary-color h-icon-edit text-hover ml-10px" @click="showForm(row)"></span>
            <span class="primary-color h-icon-trash text-hover ml-10px" @click="doRemove(row)"></span>
          </template>
        </vxe-column>
      </vxe-table>
    </div>
    <vxe-pager perfect @page-change="loadList"
               v-model:current-page="pagination.page"
               v-model:page-size="pagination.pageSize"
               :total="pagination.total"
               :layouts="['PrevJump', 'PrevPage', 'Number', 'NextPage', 'NextJump', 'Sizes', 'FullJump', 'Total']">
      <template #left>
        <vxe-button @click="loadList()" type="text" size="mini" icon="fa fa-refresh"
                    :loading="loading"></vxe-button>
      </template>
    </vxe-pager>
  </div>
</template>

<script>
import Supplier from "@js/api/basic/Supplier";
import SupplierForm from "./SupplierForm.vue";
import {confirm, message} from "heyui.ext";
import {layer} from "@layui/layer-vue";
import {h} from "vue";
import SupplierCategoryForm from "@views/basic/SupplierCategoryForm.vue";

export default {
  name: "SupplierList",
  data() {
    return {
      loading: false,
      dataList: [],
      params: {
        filter: null,
      },
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
    }
  },
  computed: {
    queryParams() {
      return Object.assign(this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
      })
    }
  },
  methods: {
    addCategoryForm(entity) {
      let type = 0;
      let layerId = layer.open({
        title: "货商分类信息",
        shadeClose: false,
        closeBtn: false,
        area: ['400px', '330px'],
        content: h(SupplierCategoryForm, {
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
    showForm(entity) {
      let layerId = layer.open({
        title: "货商信息",
        shadeClose: false,
        area: ['800px', 'auto'],
        content: h(SupplierForm, {
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
    doSearch() {
      this.loadList();
    },
    loadList() {
      this.loading = true;
      Supplier.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results;
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
    doRemove(row) {
      confirm({
        title: "系统提示",
        content: `确认删除：${row.name}?`,
        onConfirm: () => {
          Supplier.remove(row.id).then(() => {
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
