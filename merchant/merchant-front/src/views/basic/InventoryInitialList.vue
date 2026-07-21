<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="addForm()">新 增</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="batchDelete">批量删除</t-button>
        <t-select
            v-model="params.warehouseIds"
            :options="warehouseList"
            :keys="{ value: 'id', label: 'name' }"
            multiple
            filterable
            clearable
            placeholder="请选择仓库"
            style="width: 180px; border-radius: 4px"
        />
        <t-select
            v-model="params.productIds"
            :options="productList"
            :keys="{ value: 'id', label: 'name' }"
            multiple
            filterable
            clearable
            placeholder="请选择产品"
            style="width: 180px; border-radius: 4px"
        />
        <t-input
            v-model="params.filter"
            clearable
            placeholder="请输入产品编码/名称"
            style="width: 220px; border-radius: 4px"
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
          :selected-row-keys="selectedRowKeys"
          :foot-data="footData"
          @select-change="onSelectChange"
      >
        <template #ops="{ row }">
          <t-space size="small">
            <t-link theme="primary" @click="addForm('edit', row.id)"><t-icon name="edit"/></t-link>
            <t-link theme="primary" @click="doRemove(row)"><t-icon name="delete"/></t-link>
          </t-space>
        </template>
      </t-table>
    </div>

    <div class="simple-page__pager">
      <t-pagination
          v-model:current="pagination.page"
          v-model:page-size="pagination.pageSize"
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
import InventoryInitial from '@js/api/basic/InventoryInitial';
import {mapMutations} from 'vuex';
import Warehouse from '@js/api/basic/Warehouse';
import Product from '@js/api/basic/Product';
import {MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';

export default {
  name: 'InventoryInitialList',
  data() {
    return {
      loading: false,
      dataList: [],
      selectedRowKeys: [],
      params: {
        filter: '',
        operationType: '期初库存',
        productIds: [],
        warehouseIds: []
      },
      pagination: {page: 1, pageSize: 10, total: 0},
      warehouseList: [],
      productList: [],
      columns: [
        {colKey: 'row-select', type: 'multiple', width: 46},
        {colKey: 'ops', title: '操作', width: 90, fixed: 'left', align: 'center'},
        {colKey: 'productCode', title: '产品编码', width: 130},
        {colKey: 'productName', title: '产品名称', minWidth: 140, ellipsis: true},
        {colKey: 'specification', title: '规格型号', width: 100, ellipsis: true},
        {colKey: 'unitName', title: '单位', width: 80},
        {colKey: 'warehouseName', title: '仓库', width: 120, ellipsis: true},
        {colKey: 'quantity', title: '期初库存', width: 100},
        {colKey: 'unitPrice', title: '期初单位成本', width: 120},
        {colKey: 'subtotal', title: '期初总价', width: 110}
      ]
    };
  },
  computed: {
    footData() {
      const sum = (key) =>
        this.dataList.reduce((s, r) => s + Number(r[key] || 0), 0).toFixed(2);
      return [{
        productCode: '合计',
        quantity: sum('quantity'),
        subtotal: sum('subtotal')
      }];
    }
  },
  methods: {
    ...mapMutations(['pushTab']),
    onSelectChange(keys) {
      this.selectedRowKeys = keys;
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    loadList() {
      this.loading = true;
      const query = {
        operationType: this.params.operationType,
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        productIds: (this.params.productIds || []).join(','),
        warehouseIds: (this.params.warehouseIds || []).join(',')
      };
      if (this.params.filter) query.filter = this.params.filter;
      InventoryInitial.list(query)
        .then(({data: {results, total}}) => {
          this.dataList = results || [];
          this.pagination.total = total || 0;
          this.selectedRowKeys = [];
        })
        .finally(() => (this.loading = false));
    },
    addForm(type = 'add', inventoryInitialId = null) {
      this.pushTab({
        keepAlive: false,
        key: 'InventoryInitialForm',
        title: type === 'edit' ? '编辑库存初期' : '新增库存初期',
        params: {type, inventoryInitialId}
      });
    },
    batchDelete() {
      if (!this.selectedRowKeys.length) {
        MessagePlugin.error('请选择至少一条数据');
        return;
      }
      DialogPlugin.confirm({
        header: '系统提示',
        body: '确定批量删除数据？',
        onConfirm: () => {
          InventoryInitial.batchDelete({ids: this.selectedRowKeys}).then(() => {
            MessagePlugin.success('批量删除成功');
            this.loadList();
          });
        }
      });
    },
    doRemove(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除：${row.productName}?`,
        onConfirm: () => {
          InventoryInitial.remove(row.id).then(() => {
            MessagePlugin.success('删除成功~');
            this.loadList();
          });
        }
      });
    }
  },
  created() {
    this.loadList();
    Promise.all([Warehouse.select(), Product.select()]).then((results) => {
      this.warehouseList = results[0].data || [];
      this.productList = results[1].data || [];
    });
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
}
</style>
