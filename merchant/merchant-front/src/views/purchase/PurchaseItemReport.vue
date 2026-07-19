<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-select
            v-model="params.orderType"
            :options="orderTypeOptions"
            placeholder="订单类型"
            style="width: 120px; border-radius: 4px"
        />
        <t-date-range-picker
            v-model="dateRangeValue"
            clearable
            allow-input
            placeholder="订单日期"
            style="width: 260px; border-radius: 4px"
        />
        <t-select
            v-model="warehouseIds"
            :options="warehouseList"
            :keys="{ value: 'id', label: 'name' }"
            multiple
            filterable
            clearable
            placeholder="请选择仓库"
            style="width: 180px; border-radius: 4px"
        />
        <t-select
            v-model="supplierIds"
            :options="supplierList"
            :keys="{ value: 'id', label: 'name' }"
            multiple
            filterable
            clearable
            placeholder="请选择供货商"
            style="width: 180px; border-radius: 4px"
        />
        <t-select
            v-model="supplierCategoryIds"
            :options="supplierCategoryList"
            :keys="{ value: 'id', label: 'name' }"
            multiple
            filterable
            clearable
            placeholder="供货商类别"
            style="width: 160px; border-radius: 4px"
        />
        <t-select
            v-model="productIds"
            :options="productList"
            :keys="{ value: 'id', label: 'name' }"
            multiple
            filterable
            clearable
            placeholder="请选择产品"
            style="width: 180px; border-radius: 4px"
        />
        <t-select
            v-model="productCategoryIds"
            :options="productCategoryList"
            :keys="{ value: 'id', label: 'name' }"
            multiple
            filterable
            clearable
            placeholder="产品类别"
            style="width: 160px; border-radius: 4px"
        />
        <t-input
            v-model="params.filter"
            clearable
            placeholder="订单号/供货商名称"
            style="width: 220px; background: #fff; border-radius: 4px"
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
          table-layout="fixed"
          :data="dataList"
          :columns="columns"
          :loading="loading"
          :foot-data="footData"
      >
        <template #productInfo="{ row }">
          {{ row.productCode }}--{{ row.productName }}
        </template>
      </t-table>
    </div>

    <div class="simple-page__pager">
      <span class="simple-page__total"></span>
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
import manba from "manba";
import PurchaseReport from "@js/api/purchase/PurchaseReport";
import Supplier from "@js/api/basic/Supplier";
import Warehouse from "@js/api/basic/Warehouse";
import Product from "@js/api/basic/Product";
import ProductCategory from "@js/api/basic/ProductCategory";
import SupplierCategory from "@js/api/basic/SupplierCategory";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-DD");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-DD");

/**
 * @功能描述: 采购明细表
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "PurchaseItemReport",
  data() {
    return {
      dataList: [],
      productList: [],
      warehouseList: [],
      productCategoryList: [],
      supplierCategoryList: [],
      supplierCategoryIds: [],
      productCategoryIds: [],
      supplierList: [],
      supplierIds: [],
      productIds: [],
      warehouseIds: [],
      loading: false,
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      params: {
        filter: null,
        orderType: "in",
      },
      dateRangeValue: [startTime, endTime],
      orderTypeOptions: [
        {label: '入库单', value: 'in'},
        {label: '退货单', value: 'out'},
        {label: '全部', value: 'all'},
      ],
      columns: [
        {colKey: 'productInfo', title: '产品信息', minWidth: 220, ellipsis: true},
        {colKey: 'spec', title: '规格型号', width: 110, align: 'center'},
        {colKey: 'categoryName', title: '产品类别', width: 110, align: 'center'},
        {colKey: 'orderDate', title: '订单日期', width: 120, align: 'center'},
        {colKey: 'orderNo', title: '订单编号', minWidth: 150, ellipsis: true},
        {colKey: 'supplierName', title: '供货商', minWidth: 120, ellipsis: true},
        {colKey: 'supplierCode', title: '供货商编码', minWidth: 110, ellipsis: true},
        {colKey: 'supplierCategoryName', title: '供货商类别', minWidth: 110, ellipsis: true},
        {colKey: 'warehouseName', title: '仓库名称', minWidth: 110, ellipsis: true},
        {colKey: 'orderType', title: '订单类型', width: 90, align: 'center'},
        {colKey: 'secondaryUnitName', title: '采购单位', width: 90, align: 'center'},
        {colKey: 'secondaryQuantity', title: '采购数量', width: 100, align: 'right'},
        {colKey: 'secondaryPrice', title: '采购单价', width: 100, align: 'right'},
        {colKey: 'subtotal', title: '采购金额', width: 110, align: 'right'},
      ]
    }
  },
  computed: {
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return Object.assign({}, this.params, {
        productCategoryIds: (this.productCategoryIds || []).toString(),
        supplierCategoryIds: (this.supplierCategoryIds || []).toString(),
        supplierIds: (this.supplierIds || []).toString(),
        productIds: (this.productIds || []).toString(),
        warehouseIds: (this.warehouseIds || []).toString(),
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        start: start || null,
        end: end || null,
      })
    },
    footData() {
      const sum = (key) => {
        const total = (this.dataList || []).reduce((acc, row) => acc + Number(row[key] || 0), 0);
        return total.toFixed(2);
      };
      return [{
        productInfo: '合计',
        secondaryQuantity: sum('secondaryQuantity'),
        secondaryPrice: sum('secondaryPrice'),
        subtotal: sum('subtotal'),
      }];
    }
  },
  methods: {
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    loadSelect() {
      Promise.all([
        Supplier.select(),
        Warehouse.select(),
        Product.select(),
        ProductCategory.select(),
        SupplierCategory.select(),
      ]).then((results) => {
        this.supplierList = results[0].data || [];
        this.warehouseList = results[1].data || [];
        this.productList = results[2].data || [];
        this.productCategoryList = results[3].data || [];
        this.supplierCategoryList = results[4].data || [];
      });
    },
    loadList() {
      this.loading = true;
      PurchaseReport.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
  },
  created() {
    this.loadList();
    this.loadSelect();
  }
}
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
