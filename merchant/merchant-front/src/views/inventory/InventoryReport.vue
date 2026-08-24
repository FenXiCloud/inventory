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
        <t-date-range-picker
            v-model="dateRangeValue"
            :presets="datePresets"
            clearable
            allow-input
            placeholder="单据日期"
            style="width: 260px; border-radius: 4px"
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
      <t-table
          row-key="productId"
          size="medium"
          bordered
          hover
          height="100%"
          table-layout="fixed"
          :data="dataList"
          :columns="columns"
          :loading="loading"
          :foot-data="footData"
      />
    </div>

    <div class="simple-page__pager">
      <span class="simple-page__total">总成本：{{ amountTotal }}元 &nbsp; 总利润：{{ profitTotal }}元</span>
      <t-pagination
          v-model:current="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-size-options="pagination.pageSizeOptions"
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
import CustomerLevel from '@js/api/basic/CustomerLevel';
import { LoadingPlugin, MessagePlugin } from 'tdesign-vue-next';
import { exportExcelHeader } from '@js/excel';

export default {
  name: 'InventoryReport',
  data() {
    return {
      dataList: [],
      loading: false,
      amountTotal: '0.00',
      profitTotal: '0.00',
      pagination: {
        page: 1,
        pageSize: 20,
        pageSizeOptions: [
          {label: "50条/页", value: 50},
          {label: "100条/页", value: 100},
          {label: "200条/页", value: 200},
          {label: "500条/页", value: 500},
        ],
        total: 0
      },
      params: {
        productCategoryIds: [],
        productIds: [],
        warehouseIds: [],
        filter: null
      },
      dateRangeValue: [],
      warehouseList: [],
      productList: [],
      productCategoryList: [],
      customerLevelList: [],
      balanceTotalList: []
    };
  },
  computed: {
    datePresets() {
      return {
        '近期': () => [manba().format('YYYY-MM-dd'), manba().format('YYYY-MM-dd')],
        '昨天': () => [manba().add(-1, 'day').format('YYYY-MM-dd'), manba().add(-1, 'day').format('YYYY-MM-dd')],
        '今天': () => [manba().format('YYYY-MM-dd'), manba().format('YYYY-MM-dd')],
        '本月': () => [manba().startOf(manba.MONTH).format('YYYY-MM-dd'), manba().endOf(manba.MONTH).format('YYYY-MM-dd')],
        '上月': () => [manba().add(-1, 'month').startOf(manba.MONTH).format('YYYY-MM-dd'), manba().add(-1, 'month').endOf(manba.MONTH).format('YYYY-MM-dd')],
        '本年': () => [manba().startOf(manba.YEAR).format('YYYY-MM-dd'), manba().endOf(manba.YEAR).format('YYYY-MM-dd')],
        '上年': () => [manba().add(-1, 'year').startOf(manba.YEAR).format('YYYY-MM-dd'), manba().add(-1, 'year').endOf(manba.YEAR).format('YYYY-MM-dd')],
      };
    },
    displayWarehouses() {
      if (!this.params.warehouseIds || !this.params.warehouseIds.length) {
        return this.warehouseList;
      }
      const ids = new Set(this.params.warehouseIds);
      return this.warehouseList.filter((item) => ids.has(item.id));
    },
    columns() {
      const cols = [
        { colKey: 'productCode', title: '产品编码', minWidth: 120, ellipsis: true },
        { colKey: 'productName', title: '产品名称', minWidth: 140, ellipsis: true },
        { colKey: 'productCategoryName', title: '产品类别', minWidth: 120, ellipsis: true },
        { colKey: 'productSpecification', title: '规格型号', width: 120, ellipsis: true },
        { colKey: 'productUnitName', title: '单位', width: 80, align: 'center' },
        {
          colKey: 'allWarehouse',
          title: '全部仓库',
          align: 'center',
          children: [
            { colKey: 'all_quantity', title: '单位数量', width: 100, align: 'right' },
            { colKey: 'all_averageCost', title: '单位成本', width: 100, align: 'right' },
            { colKey: 'all_totalCost', title: '成本小计', width: 100, align: 'right' },
            { colKey: 'all_unitPrice', title: '单位售价', width: 100, align: 'right' },
            { colKey: 'all_unitProfit', title: '单位利润', width: 100, align: 'right' },
            { colKey: 'all_profitTotal', title: '利润小计', width: 100, align: 'right' },
          ],
        },
      ];
      (this.displayWarehouses || []).forEach((item) => {
        cols.push({
          colKey: `wh_${item.id}`,
          title: `${item.code || ''}-${item.name}`,
          align: 'center',
          children: [
            { colKey: this.warehouseField(item, 'quantity'), title: '单位数量', width: 100, align: 'right' },
            { colKey: this.warehouseField(item, 'averageCost'), title: '单位成本', width: 100, align: 'right' },
            { colKey: this.warehouseField(item, 'totalCost'), title: '成本小计', width: 100, align: 'right' },
            { colKey: this.warehouseField(item, 'unitPrice'), title: '单位售价', width: 100, align: 'right' },
            { colKey: this.warehouseField(item, 'unitProfit'), title: '单位利润', width: 100, align: 'right' },
            { colKey: this.warehouseField(item, 'profitTotal'), title: '利润小计', width: 100, align: 'right' },
          ],
        });
      });
      return cols;
    },
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        start: start || null,
        end: end || null,
      });
    },
    footData() {
      const foot = { productCode: '合计' };
      const data = this.dataList || [];
      const sumField = (property) => {
        if (property.endsWith('_quantity') || property === 'all_quantity') {
          return data.reduce((acc, row) => acc + Number(row[property] || 0), 0);
        }
        if (property.endsWith('_totalCost') || property === 'all_totalCost'
            || property.endsWith('_profitTotal') || property === 'all_profitTotal') {
          return data.reduce((acc, row) => acc + Number(row[property] || 0), 0).toFixed(2);
        }
        return '';
      };
      foot.all_quantity = sumField('all_quantity');
      foot.all_totalCost = sumField('all_totalCost');
      foot.all_profitTotal = sumField('all_profitTotal');
      (this.displayWarehouses || []).forEach((warehouse) => {
        const qtyKey = this.warehouseField(warehouse, 'quantity');
        const costKey = this.warehouseField(warehouse, 'totalCost');
        const profitKey = this.warehouseField(warehouse, 'profitTotal');
        foot[qtyKey] = sumField(qtyKey);
        foot[costKey] = sumField(costKey);
        foot[profitKey] = sumField(profitKey);
      });
      return [foot];
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
      // 找到客户等级中名称含"零售"的，否则取第一个
      const levels = this.customerLevelList || [];
      let targetLevelId = null;
      const retailLevel = levels.find(l => l.name && l.name.includes('零售'));
      if (retailLevel) {
        targetLevelId = retailLevel.id;
      } else if (levels.length > 0) {
        targetLevelId = levels[0].id;
      }
      // 从产品列表的客户级别定价构建售价 map
      const priceMap = {};
      (this.productList || []).forEach(p => {
        let price = 0;
        if (targetLevelId && p.customerLevelPriceList && p.customerLevelPriceList.length) {
          const levelPrice = p.customerLevelPriceList.find(lp => lp.customerLevelId === targetLevelId);
          if (levelPrice) {
            price = Number(levelPrice.price || 0);
          }
        }
        if (!price) {
          price = Number(p.retailCustomerPrice || p.purchasePrice || 0);
        }
        priceMap[p.id] = price;
      });
      let amountTotal = 0;
      let profitTotal = 0;
      rows.forEach((item) => {
        let allQuantity = 0;
        let allTotalCost = 0;
        let allProfitTotal = 0;
        const unitPrice = priceMap[item.productId] || 0;
        item.all_unitPrice = unitPrice;
        warehouses.forEach((warehouse) => {
          const report = inventoryList.find(
            (row) => row.warehouseId === warehouse.id && row.productId === item.productId
          );
          const quantity = Number(report?.currentQuantity || 0);
          const averageCost = Number(report?.averageCost || 0);
          const totalCost = Number(report?.totalCost || 0);
          const whUnitProfit = unitPrice - averageCost;
          const whProfitTotal = Number((whUnitProfit * quantity).toFixed(2));
          item[this.warehouseField(warehouse, 'quantity')] = quantity;
          item[this.warehouseField(warehouse, 'averageCost')] = averageCost;
          item[this.warehouseField(warehouse, 'totalCost')] = totalCost;
          item[this.warehouseField(warehouse, 'unitPrice')] = unitPrice;
          item[this.warehouseField(warehouse, 'unitProfit')] = whUnitProfit;
          item[this.warehouseField(warehouse, 'profitTotal')] = whProfitTotal;
          allQuantity += quantity;
          allTotalCost += totalCost;
          allProfitTotal += whProfitTotal;
          amountTotal += totalCost;
          profitTotal += whProfitTotal;
        });
        item.all_quantity = allQuantity;
        item.all_totalCost = Number(allTotalCost.toFixed(2));
        item.all_averageCost = allQuantity
          ? Number((allTotalCost / allQuantity).toFixed(2))
          : 0;
        item.all_unitProfit = unitPrice - item.all_averageCost;
        item.all_profitTotal = Number(allProfitTotal.toFixed(2));
      });
      this.amountTotal = amountTotal.toFixed(2);
      this.profitTotal = profitTotal.toFixed(2);
      return rows;
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
          this.profitTotal = '0.00';
        })
        .finally(() => {
          this.loading = false;
        });
    },
    loadDict(callback) {
      LoadingPlugin(true);
      Promise.all([Product.select(), Warehouse.select(), ProductCategory.select(), CustomerLevel.select()])
        .then((results) => {
          this.productList = results[0].data || [];
          this.warehouseList = results[1].data || [];
          this.productCategoryList = results[2].data || [];
          this.customerLevelList = results[3].data || [];
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
        { label: '成本小计', key: 'all_totalCost' },
        { label: '单位售价', key: 'all_unitPrice' },
        { label: '单位利润', key: 'all_unitProfit' },
        { label: '利润小计', key: 'all_profitTotal' }
      ];
      const tHeader = ['产品编码', '产品名称', '产品类别', '规格型号', '单位', '全部仓库', null, null, null, null, null];
      const secondHeader = [null, null, null, null, null, '单位数量', '单位成本', '成本小计', '单位售价', '单位利润', '利润小计'];
      const merges = [
        { s: { r: 0, c: 0 }, e: { r: 1, c: 0 } },
        { s: { r: 0, c: 1 }, e: { r: 1, c: 1 } },
        { s: { r: 0, c: 2 }, e: { r: 1, c: 2 } },
        { s: { r: 0, c: 3 }, e: { r: 1, c: 3 } },
        { s: { r: 0, c: 4 }, e: { r: 1, c: 4 } },
        { s: { r: 0, c: 5 }, e: { r: 0, c: 10 } }
      ];
      let start = 5;
      let end = 10;
      this.displayWarehouses.forEach((warehouse) => {
        start += 6;
        end += 6;
        headList.push({ label: '单位数量', key: this.warehouseField(warehouse, 'quantity') });
        headList.push({ label: '单位成本', key: this.warehouseField(warehouse, 'averageCost') });
        headList.push({ label: '成本小计', key: this.warehouseField(warehouse, 'totalCost') });
        headList.push({ label: '单位售价', key: this.warehouseField(warehouse, 'unitPrice') });
        headList.push({ label: '单位利润', key: this.warehouseField(warehouse, 'unitProfit') });
        headList.push({ label: '利润小计', key: this.warehouseField(warehouse, 'profitTotal') });
        tHeader.push(`${warehouse.code || ''}-${warehouse.name}`);
        tHeader.push(null);
        tHeader.push(null);
        tHeader.push(null);
        tHeader.push(null);
        tHeader.push(null);
        secondHeader.push('单位数量');
        secondHeader.push('单位成本');
        secondHeader.push('成本小计');
        secondHeader.push('单位售价');
        secondHeader.push('单位利润');
        secondHeader.push('利润小计');
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

</style>
