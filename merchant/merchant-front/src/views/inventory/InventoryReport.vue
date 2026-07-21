<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="excel">导 出</t-button>
        <t-select
            v-model="params.warehouseIds"
            :options="warehouseList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            multiple
            placeholder="仓库"
            style="width: 160px; border-radius: 4px"
        />
        <t-select
            v-model="params.productIds"
            :options="productList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            multiple
            placeholder="产品"
            style="width: 160px; border-radius: 4px"
        />
        <t-select
            v-model="params.productCategoryIds"
            :options="productCategoryList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            multiple
            placeholder="产品类别"
            style="width: 160px; border-radius: 4px"
        />
        <t-input
            v-model="params.filter"
            clearable
            placeholder="请输入产品编号/名称/类别/规格"
            style="width: 260px; background: #fff; border-radius: 4px"
            @enter="doSearch"
        >
          <template #suffixIcon>
            <t-icon name="search" style="cursor:pointer" @click="doSearch"/>
          </template>
        </t-input>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="doSearch">查询</t-button>
      </t-space>
    </div>

    <div class="simple-page__hint">
      显示当前库存余额（按仓库分列）。成本取库存账面成本。
    </div>

    <div class="simple-page__table">
      <vxe-table
          row-id="productId"
          ref="table"
          height="auto"
          :data="dataList"
          highlight-hover-row
          show-overflow
          show-footer
          :footer-method="footerMethod"
          :row-config="{height: 48}"
          :column-config="{resizable: true}"
          :loading="loading"
      >
        <vxe-column title="产品编码" field="productCode" min-width="120"/>
        <vxe-column title="产品名称" field="productName" min-width="140"/>
        <vxe-column title="产品类别" field="productCategoryName" min-width="120"/>
        <vxe-column title="规格型号" field="productSpecification" width="120"/>
        <vxe-column title="单位" field="productUnitName" width="80" align="center"/>
        <vxe-colgroup align="center" title="全部仓库">
          <vxe-column title="单位数量" field="all_quantity" align="right" width="110"/>
          <vxe-column title="单位成本" field="all_averageCost" align="right" width="110"/>
          <vxe-column title="成本小计" field="all_totalCost" align="right" width="110"/>
        </vxe-colgroup>
        <vxe-colgroup
            v-for="item in displayWarehouses"
            :key="item.id"
            align="center"
            :title="(item.code || '') + '-' + item.name"
        >
          <vxe-column title="单位数量" :field="warehouseField(item, 'quantity')" align="right" width="110"/>
          <vxe-column title="单位成本" :field="warehouseField(item, 'averageCost')" align="right" width="110"/>
          <vxe-column title="成本小计" :field="warehouseField(item, 'totalCost')" align="right" width="110"/>
        </vxe-colgroup>
      </vxe-table>
    </div>

    <div class="simple-page__pager">
      <span class="simple-page__total">总成本：{{ amountTotal }}元</span>
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
import manba from 'manba';
import Inventory from '@js/api/inventory/Inventory';
import Product from '@js/api/basic/Product';
import ProductCategory from '@js/api/basic/ProductCategory';
import Warehouse from '@js/api/basic/Warehouse';
import { LoadingPlugin, MessagePlugin } from 'tdesign-vue-next';
import { exportExcelHeader } from '@js/excel';

export default {
  name: 'InventoryReport',
  data() {
    return {
      dataList: [],
      loading: false,
      amountTotal: '0.00',
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      params: {
        productCategoryIds: [],
        productIds: [],
        warehouseIds: [],
        filter: null
      },
      warehouseList: [],
      productList: [],
      productCategoryList: [],
      balanceTotalList: []
    };
  },
  computed: {
    displayWarehouses() {
      if (!this.params.warehouseIds || !this.params.warehouseIds.length) {
        return this.warehouseList;
      }
      const ids = new Set(this.params.warehouseIds);
      return this.warehouseList.filter((item) => ids.has(item.id));
    },
    queryParams() {
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize
      });
    }
  },
  methods: {
    warehouseField(warehouse, suffix) {
      return `${warehouse.code || warehouse.id}_${suffix}`;
    },
    buildRequestParams(extra = {}) {
      const params = Object.assign({}, this.queryParams, extra);
      params.productCategoryIds = (params.productCategoryIds || []).join(',');
      params.productIds = (params.productIds || []).join(',');
      params.warehouseIds = (params.warehouseIds || []).join(',');
      return params;
    },
    fillRowInventory(rows, inventoryList) {
      const warehouses = this.displayWarehouses;
      let amountTotal = 0;
      rows.forEach((item) => {
        let allQuantity = 0;
        let allTotalCost = 0;
        warehouses.forEach((warehouse) => {
          const report = inventoryList.find(
            (row) => row.warehouseId === warehouse.id && row.productId === item.productId
          );
          const quantity = Number(report?.currentQuantity || 0);
          const averageCost = Number(report?.averageCost || 0);
          const totalCost = Number(report?.totalCost || 0);
          item[this.warehouseField(warehouse, 'quantity')] = quantity;
          item[this.warehouseField(warehouse, 'averageCost')] = averageCost;
          item[this.warehouseField(warehouse, 'totalCost')] = totalCost;
          allQuantity += quantity;
          allTotalCost += totalCost;
          amountTotal += totalCost;
        });
        item.all_quantity = allQuantity;
        item.all_totalCost = Number(allTotalCost.toFixed(2));
        item.all_averageCost = allQuantity
          ? Number((allTotalCost / allQuantity).toFixed(2))
          : 0;
      });
      this.amountTotal = amountTotal.toFixed(2);
      return rows;
    },
    footerMethod({ columns, data }) {
      const sums = [];
      columns.forEach((column, index) => {
        if (index === 0) {
          sums.push('合计');
          return;
        }
        const property = column.property;
        if (!property) {
          sums.push('');
          return;
        }
        if (property.endsWith('_quantity') || property === 'all_quantity') {
          const total = data.reduce((acc, row) => acc + Number(row[property] || 0), 0);
          sums.push(total);
          return;
        }
        if (property.endsWith('_totalCost') || property === 'all_totalCost') {
          const total = data.reduce((acc, row) => acc + Number(row[property] || 0), 0);
          sums.push(total.toFixed(2));
          return;
        }
        sums.push('');
      });
      return [sums];
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    loadList() {
      this.loading = true;
      const params = this.buildRequestParams();
      Promise.all([Inventory.balance(params), Inventory.balanceTotal(params)])
        .then(([reportRes, inventoryRes]) => {
          const rows = reportRes?.data?.results || [];
          this.pagination.total = reportRes?.data?.total || 0;
          this.balanceTotalList = inventoryRes?.data || [];
          this.dataList = this.fillRowInventory(rows, this.balanceTotalList);
        })
        .catch(() => {
          this.dataList = [];
          this.pagination.total = 0;
          this.amountTotal = '0.00';
        })
        .finally(() => {
          this.loading = false;
        });
    },
    loadDict(callback) {
      LoadingPlugin(true);
      Promise.all([Product.select(), Warehouse.select(), ProductCategory.select()])
        .then((results) => {
          this.productList = results[0].data || [];
          this.warehouseList = results[1].data || [];
          this.productCategoryList = results[2].data || [];
          callback && callback();
        })
        .finally(() => LoadingPlugin(false));
    },
    excel() {
      const params = this.buildRequestParams({ page: 1, pageSize: 999999 });
      this.loading = true;
      Promise.all([Inventory.balance(params), Inventory.balanceTotal(params)])
        .then(([reportRes, inventoryRes]) => {
          const rows = this.fillRowInventory(reportRes?.data?.results || [], inventoryRes?.data || []);
          this.callExcel(rows);
        })
        .finally(() => {
          this.loading = false;
        });
    },
    callExcel(dataList) {
      if (!dataList.length) {
        MessagePlugin.warning('暂无数据～');
        return;
      }
      const headList = [
        { label: '产品编码', key: 'productCode' },
        { label: '产品名称', key: 'productName' },
        { label: '产品类别', key: 'productCategoryName' },
        { label: '规格型号', key: 'productSpecification' },
        { label: '单位', key: 'productUnitName' },
        { label: '单位数量', key: 'all_quantity' },
        { label: '单位成本', key: 'all_averageCost' },
        { label: '成本小计', key: 'all_totalCost' }
      ];
      const tHeader = ['产品编码', '产品名称', '产品类别', '规格型号', '单位', '全部仓库', null, null];
      const secondHeader = [null, null, null, null, null, '单位数量', '单位成本', '成本小计'];
      const merges = [
        { s: { r: 0, c: 0 }, e: { r: 1, c: 0 } },
        { s: { r: 0, c: 1 }, e: { r: 1, c: 1 } },
        { s: { r: 0, c: 2 }, e: { r: 1, c: 2 } },
        { s: { r: 0, c: 3 }, e: { r: 1, c: 3 } },
        { s: { r: 0, c: 4 }, e: { r: 1, c: 4 } },
        { s: { r: 0, c: 5 }, e: { r: 0, c: 7 } }
      ];
      let start = 5;
      let end = 7;
      this.displayWarehouses.forEach((warehouse) => {
        start += 3;
        end += 3;
        headList.push({ label: '单位数量', key: this.warehouseField(warehouse, 'quantity') });
        headList.push({ label: '单位成本', key: this.warehouseField(warehouse, 'averageCost') });
        headList.push({ label: '成本小计', key: this.warehouseField(warehouse, 'totalCost') });
        tHeader.push(`${warehouse.code || ''}-${warehouse.name}`);
        tHeader.push(null);
        tHeader.push(null);
        secondHeader.push('单位数量');
        secondHeader.push('单位成本');
        secondHeader.push('成本小计');
        merges.push({ s: { r: 0, c: start }, e: { r: 0, c: end } });
      });
      exportExcelHeader(
        dataList,
        tHeader,
        headList,
        merges,
        [secondHeader],
        manba(new Date()).format('YYYYMMDDHHmmss') + '_库存余额'
      );
    }
  },
  created() {
    this.loadDict(() => this.loadList());
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

.simple-page__hint {
  flex-shrink: 0;
  margin-bottom: 8px;
  padding: 8px 12px;
  border-radius: 4px;
  background: #f3f3f3;
  color: #555;
  font-size: 13px;
  line-height: 1.6;
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
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid var(--td-component-border, #dcdcdc);
  background: #fff;
}

.simple-page__total {
  font-size: 14px;
  color: #333639;
  flex-shrink: 0;
}
</style>
