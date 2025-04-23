<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <Button @click="addForm()" color="primary">新 增</Button>
        <Button @click="batchDelete()" >批量删除</Button>
      </template>
      <template #tools>
        <div class="h-input-group">
          <span class="h-input-addon">仓库：</span>
          <Select class="w-178px" :multiple="true" v-model="params.warehouseIds"  keyName="id" titleName="name"
                  :datas="warehouseList" placeholder="请选择仓库"/>
        </div>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">商品：</span>
          <Select class="w-178px" :multiple="true" v-model="params.productIds"  keyName="id" titleName="name" :datas="productList" placeholder="请选择商品"/>
        </div>
        <Search v-model.trim="params.filter" search-button-theme="h-btn-default"
                show-search-button class="w-280px ml-8px"
                placeholder="请输入商品编码/名称" @search="doSearch">
          <i class="h-icon-search"/>
        </Search>
      </template>
    </vxe-toolbar>
    <div class="flex1">
      <vxe-table row-id="id"
                 ref="table"
                 :data="dataList"
                 highlight-hover-row
                 show-overflow
                 show-footer
                 :footer-method="footerMethod"
                 stripe
                 :row-config="{height: 48}"
                 :column-config="{resizable: true}"
                 :loading="loading">
        <vxe-column type="checkbox" width="40" align="center"/>
        <vxe-column type="seq" width="60" title="序号"/>
        <vxe-column title="产品编码" field="productCode" width="150"/>
        <vxe-column title="产品名称" field="productName"/>
        <vxe-column title="规格型号" field="specification"/>
        <vxe-column title="单位" field="unitName"/>
        <vxe-column title="仓库" field="warehouseName"/>
        <vxe-column title="期初库存" field="quantity"/>
        <vxe-column title="期初单位成本" field="unitPrice"/>
        <vxe-column title="期初总价" field="subtotal"/>
        <vxe-column title="操作" align="center" width="150">
          <template #default="{row}">
            <i class="primary-color h-icon-edit ml-10px text-hover" @click="addForm('edit',row.id)"></i>
            <i class="primary-color h-icon-trash ml-10px text-hover" @click="doRemove(row)"></i>
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
import {mapMutations} from "vuex";
import Warehouse from "@js/api/basic/Warehouse";
import Product from "@js/api/basic/Product";
import {loading, confirm,message} from "heyui.ext";

export default {
  name: "InventoryInitialList",
  data() {
    return {
      loading: false,
      dataList: [],
      params: {
        filter: null,
        operationType:'期初库存',
        productIds: [],
        warehouseIds: [],
      },
      pagination: {
        page: 1,
        size: 20,
        total: 0
      },
      warehouseList: [],
      productList: [],
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
    footerMethod({columns, data}) {
      let totalQuantity = 0;
      let totalAmount = 0;
      columns.forEach((column) => {
        if (column.property && ['quantity','subtotal'].includes(column.property)) {

          data.forEach((row) => {
            let rd = row[column.property];
            if (column.property === 'quantity') {
              if (rd) {
                totalQuantity += Number(rd || 0);
              }
            }else if (column.property === 'subtotal') {
              if (rd) {
                totalAmount += Number(rd || 0);
              }
            }
          });
        }
      })
      return [["", "", "", "", "", "", "", totalQuantity.toFixed(2),"",totalAmount.toFixed(2)]];
    },
    doSearch() {
      this.loadList();
    },
    loadList() {
      this.loading = true;
      const params = JSON.parse(JSON.stringify(this.queryParams));
      params.productIds = params.productIds.join(",");
      params.warehouseIds = params.warehouseIds.join(",");
      InventoryInitial.list(params).then(({data: {results, total}}) => {
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
    batchDelete(){
      const selectedRows = this.$refs.table.getCheckboxRecords();
      console.log(selectedRows);
      if (selectedRows.length === 0) {
        message.error("请选择至少一条数据");
        return;
      }
      confirm({
        content: `确定批量删除数据？`,
        onConfirm: () => {
          const ids = selectedRows.map(row => row.id);
          let params = {
            ids: ids,
          };
          InventoryInitial.batchDelete(params).then((success) => {
            if (success) {
              message.success("批量删除成功");
              this.loadList(); // Refresh the list
            }
          }).finally(() =>
              loading.close()
          );
        }
      })
    },
    doRemove(row) {
      confirm({
        title: "系统提示",
        content: `确认删除：${row.productName}?`,
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
    Promise.all([
      Warehouse.select(),
      Product.select(),
    ]).then((results) => {
      this.warehouseList = results[0].data || [];
      this.productList = results[1].data || [];

    }).finally(() => loading.close());
  }
}
</script>
