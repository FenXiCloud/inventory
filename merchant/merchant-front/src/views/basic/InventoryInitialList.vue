<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <Button @click="addForm()" color="primary">新 增</Button>
      </template>
    </vxe-toolbar>
    <div class="flex1">
      <vxe-table row-id="id"
                 ref="table"
                 :data="dataList"
                 highlight-hover-row
                 show-overflow
                 stripe
                 :row-config="{height: 48}"
                 :column-config="{resizable: true}"
                 :loading="loading">
        <vxe-column type="seq" width="40" title="#"/>
        <vxe-column title="产品编码" field="code" width="150"/>
        <vxe-column title="产品名称" field="name"/>
        <vxe-column title="规格型号" field="specification"/>
        <vxe-column title="规格型号" field="specification" align="center" width="100"></vxe-column>
        <vxe-column title="单位" field="unitName"/>
        <vxe-column title="仓库" field="warehouseName"/>
        <vxe-column title="期初库存" field="amount"/>
        <vxe-column title="期初单位成本" field="amount"/>
        <vxe-column title="期初总价" field="amount"/>
        <vxe-column title="操作" align="center" width="150">
          <template #default="{row}">
            <i class="primary-color h-icon-edit ml-10px" @click="addForm(row)"></i>
            <i class="primary-color h-icon-trash ml-10px" @click="doRemove(row)"></i>
          </template>
        </vxe-column>
      </vxe-table>
      <vxe-pager perfect @page-change="loadList(false)"
                 v-model:current-page="pagination.page"
                 v-model:page-size="pagination.pageSize"
                 :total="pagination.total"
                 :layouts="[ 'PrevPage', 'Number', 'NextPage', 'Sizes', 'Total']">
        <template #left>
          <vxe-button @click="loadList(false)" type="text" size="mini" icon="h-icon-refresh"
                      :loading="loading"></vxe-button>
        </template>
      </vxe-pager>
    </div>
  </div>
</template>

<script>
import InventoryInitial from "@js/api/basic/InventoryInitial";
import {confirm, message} from "heyui.ext";
import {mapMutations} from "vuex";
import Customer from "@js/api/basic/Customer";

export default {
  name: "InventoryInitialList",
  data() {
    return {
      loading: false,
      dataList: [],
      params: {
        filter: null,
        operationType:'期初库存'
      },
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
        pageSize: this.pagination.pageSize,
      })
    }
  },
  methods: {
    ...mapMutations(['pushTab']),
    doSearch() {
      this.loadList();
    },
    loadList() {
      this.loading = true;
      InventoryInitial.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },

    addForm(type = 'add', inventoryInitialId = null) {
      console.log(type, inventoryInitialId);
      this.pushTab({
        key: 'InventoryInitialForm',
        title: type === 'edit' ? '编辑库存初期' : '新增库存初期',
        params: {type: type, inventoryInitialId: inventoryInitialId}
      });
    },
    doRemove(row) {
      confirm({
        title: "系统提示",
        content: `确认删除：${row.name}?`,
        onConfirm: () => {
          InventoryInitial.remove(row.id).then(() => {
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
