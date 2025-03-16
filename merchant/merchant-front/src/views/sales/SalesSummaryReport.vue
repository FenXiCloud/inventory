<template>
  <div class="frame-page flex flex-column">
<!--    <vxe-toolbar>-->
<!--      <template #buttons>-->
<!--        <Button @click="exportData" color="primary">导 出</Button>-->
<!--        <Button @click="printEvent">打 印</Button>-->
<!--      </template>-->
<!--    </vxe-toolbar>-->

    <div class="filter-row flex flex-wrap gap-8px p-10px bg-white-color border-b">
      <div class="h-input-group">
        <Button @click="exportData" color="primary">导 出</Button>
        <Button @click="printEvent" class="ml-8px">打 印</Button>
      </div>

      <div class="h-input-group">
        <span class="h-input-addon">汇总条件：</span>
        <Select v-model="params.salesGroup" class="w-120px"
                :datas="{
                   PRODUCT:'商品',
                   PRODUCT_WAREHOUSE:'商品+仓库',
                   CUSTOMER_PRODUCT:'客户+商品',
                   CUSTOMER_PRODUCT_WAREHOUSE:'客户+商品+仓库'
                }"/>
      </div>
      <div class="h-input-group">
        <span class="h-input-addon">日期：</span>
        <DateRangePicker v-model="dateRange"></DateRangePicker>
      </div>
      <div class="h-input-group">
        <span class="h-input-addon">客户：</span>
        <Select class="w-150px" filterable :datas="customerList" keyName="id" titleName="name"
                v-model="params.customerId" placeholder="请选择客户"/>
      </div>
      <div class="h-input-group">
        <span class="h-input-addon">仓库：</span>
        <Select v-model="params.warehouseId" class="w-150px" keyName="id" titleName="name"
                :datas="warehouseList" placeholder="请选择仓库"/>
      </div>
      <div class="h-input-group">
        <span class="h-input-addon">商品：</span>
        <Select v-model="params.productId" class="w-150px" keyName="id" titleName="name"
                :datas="productList" placeholder="请选择商品"/>
      </div>
      <div class="h-input-group">
        <span class="h-input-addon">商品类别：</span>
        <Select class="w-150px" filterable :datas="productCategoryList" keyName="id" titleName="name"
                v-model="params.productCategoryId" placeholder="请选择类别"/>
      </div>
      <div class="h-input-group">
        <span class="h-input-addon">订单号：</span>
        <Search v-model.trim="params.filter" 
                search-button-theme="h-btn-default"
                show-search-button 
                class="w-180px"
                placeholder="请输入订单号" 
                @search="doSearch">
          <i class="h-icon-search"/>
        </Search>
      </div>
    </div>
    <div class="flex1">
      <vxe-table row-id="id"
                 ref="table"
                 height="auto"
                 :data="dataList"
                 highlight-hover-row
                 show-overflow
                 show-footer
                 :footer-method="footerMethod"
                 :row-config="{height: 48}"
                 :column-config="{resizable: true}"
                 :sort-config="{remote:true}"
                 :loading="loading">


        <template
            v-if="this.params.salesGroupSearch === 'CUSTOMER_PRODUCT' || this.params.salesGroupSearch === 'CUSTOMER_PRODUCT_WAREHOUSE'">
          <vxe-column title="客户编码" field="customerCode"/>
          <vxe-column title="客户名称" field="customerName"/>
        </template>
        <vxe-column title="商品编码" field="productCode" />
        <vxe-column title="商品名称" field="productName" />
        <vxe-column title="销售单位" field="unitName" />
        <template
            v-if="this.params.salesGroupSearch === 'PRODUCT_WAREHOUSE' || this.params.salesGroupSearch === 'CUSTOMER_PRODUCT_WAREHOUSE'">
          <vxe-column title="仓库名称" field="warehouseName"/>
        </template>
        <vxe-column title="单价" field="unitPrice" />
        <vxe-column title="数量" field="quantity" />
        <!--        <vxe-column title="折扣金额" field="discountValue" width="120"/>-->
        <vxe-column title="销售收入" field="subtotal"/>

      </vxe-table>
    </div>
    <div class="flex justify-between items-center pt-5px">
      <vxe-pager perfect @page-change="loadList(false)"
                 v-model:current-page="pagination.page"
                 v-model:page-size="pagination.pageSize"
                 :total="pagination.total"
                 :layouts="['PrevJump', 'PrevPage', 'Number', 'NextPage', 'NextJump', 'Sizes', 'Total']">
        <template #left>
<!--          <span class="mr-12px text-16px">总金额：{{ amountTotal }}元</span>-->
          <vxe-button @click="loadList(false)" type="text" size="mini" icon="h-icon-refresh"
                      :loading="loading"></vxe-button>
        </template>
      </vxe-pager>
    </div>
  </div>
</template>
<script>
import manba from "manba";
import SalesOutbound from "@js/api/sales/SalesOutbound";
import {mapMutations} from "vuex";
import SalesReport from "@js/api/sales/SalesReport";
import Customer from "@js/api/basic/Customer";
import {loading, message} from "heyui.ext";
import Product from "@js/api/basic/Product";
import Warehouse from "@js/api/basic/Warehouse";
import * as XLSX from "xlsx";
import ProductCategory from "@js/api/basic/ProductCategory";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-dd");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-dd");

export default {
  name: "SalesSummaryReport",
  data() {
    return {
      dataList: [],
      loading: false,
      amountTotal: 0,
      totalParams: {},
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      params: {
        filter: null,
        state: null,
        sortCol: null,
        sort: null,
        salesGroup: 'PRODUCT',
        salesGroupSearch: 'PRODUCT'
      },
      dateRange: {
        start: manba(startTime).format("YYYY-MM-dd"),
        end: manba(endTime).format("YYYY-MM-dd")
      },
      customerList: [],
      warehouseList: [],
      productList: [],
      productCategoryList:[],
    }
  },
  computed: {
    queryParams() {
      return Object.assign(this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        start: this.dateRange.start,
        end: this.dateRange.end,
      })
    },
  },
  methods: {
    ...mapMutations(['pushTab']),
    footerMethod({columns, data}) {
      let quantityTotal = 0;
      let subtotalTotal = 0;
      columns.forEach((column) => {
        if (column.property && ['quantity', 'subtotal'].includes(column.property)) {

          data.forEach((row) => {
            let rd = row[column.property];
            if (column.property === 'quantity') {
              if (rd) {
                quantityTotal += Number(rd || 0);
              }
            } else if (column.property === 'subtotal') {
              if (rd) {
                subtotalTotal += Number(rd || 0);
              }
            }
          });
        }
      })
      this.quantityTotal = quantityTotal.toFixed(2);
      this.subtotalTotal = subtotalTotal.toFixed(2);

      // 创建与列数相同长度的数组，默认填充空字符串
      const footerRow = new Array(columns.length).fill('');

      // 找到quantity和subtotal列的索引位置
      columns.forEach((column, index) => {
        if (column.property === 'quantity') {
          footerRow[index] = quantityTotal.toFixed(2);
        } else if (column.property === 'subtotal') {
          footerRow[index] = subtotalTotal.toFixed(2);
        }
      });

      return [footerRow];

      // let newVar = ["", "", "", "", quantityTotal.toFixed(2), subtotalTotal.toFixed(2)];
      // return [newVar];
    },
    doSearch() {
      this.pagination.page = 1;
      if(!this.params.salesGroup){
        message.error("请选择汇总条件~");
        return
      }
      this.params.salesGroupSearch = this.params.salesGroup;
      this.loadList();
    },
    exportData() {
      if (this.dataList.length === 0) {
        message.warn('没有可导出的数据');
        return;
      }

      try {
        loading.open('正在导出...');
        // 准备导出数据
        const exportData = this.dataList.map(item => ({
          '客户编码': item.customerCode,
          '客户名称': item.customerName,
          '商品编码': item.productCode,
          '商品名称': item.productName,
          '销售单位': item.unitName,
          '仓库名称': item.warehouseName,
          '单价': item.unitPrice,
          '数量': item.quantity,
          '销售收入': item.subtotal
        }));

        // 如果有合计行，添加到导出数据中
        exportData.push({
          '单价': '合计',
          '数量': this.quantityTotal,
          '销售收入': this.subtotalTotal
        });

        // 创建工作簿和工作表
        const ws = XLSX.utils.json_to_sheet(exportData);
        const wb = XLSX.utils.book_new();

        // 设置标题行样式
        ws['!cols'] = [
          { wch: 10 }, // 客户编码
          { wch: 15 }, // 客户名称
          { wch: 12 }, // 商品编码
          { wch: 20 }, // 商品名称
          { wch: 10 }, // 销售单位
          { wch: 12 }, // 仓库名称
          { wch: 10 }, // 单价
          { wch: 10 }, // 数量
          { wch: 12 }  // 销售收入
        ];
        XLSX.utils.book_append_sheet(wb, ws, '销售明细');

        // 导出文件
        const fileName = `销售汇总报表_${manba().format('YYYY-MM-DD')}.xlsx`;
        XLSX.writeFile(wb, fileName);

        message.success('导出成功');
      } catch (error) {
        console.error('导出错误:', error);
        message.error('导出失败');
      } finally {
        loading.close();
      }
    },
    loadList(type = true) {
      this.loading = true;
      SalesReport.salesSummary(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);

      Promise.all([
        Customer.select(),
        Warehouse.select(),
        Product.select(),
        ProductCategory.select(),
      ]).then((results) => {
        this.customerList = results[0].data || [];
        this.warehouseList = results[1].data || [];
        this.productList = results[2].data || [];
        this.productCategoryList = results[3].data || [];
      }).finally(() => loading.close());
    },
  },
  created() {
    this.loadList();
  }
}
</script>

<style scoped>
.filter-row {
  border-top: 1px solid #eee;
  background-color: #fff;
}

.h-input-group {
  display: flex;
  align-items: center;
  margin: 4px 0;
}

.h-input-addon {
  white-space: nowrap;
  padding: 0 8px;
  color: #666;
}
</style>
