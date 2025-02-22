<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <Button @click="exportData" color="primary">导 出</Button>
        <Button @click="printEvent">打 印</Button>
      </template>
      <template #tools>
        <Select v-model="params.salesType" class="w-80px" :datas="{all:'全部',out:'销货',return:'退货'}" placeholder="业务类别："/>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">订单日期：</span>
          <DateRangePicker v-model="dateRange" ></DateRangePicker>
        </div>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">客户：</span>
          <Select class="w-120px" filterable :datas="customerList" keyName="id" titleName="name"
                  v-model="params.customerId" placeholder="请选择客户"  />
        </div>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">仓库：</span>
          <Select v-model="params.warehouseId" class="w-100px" keyName="id" titleName="name" :datas="warehouseList" placeholder="请选择仓库"/>
        </div>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">商品：</span>
          <Select v-model="params.productId" class="w-100px" keyName="id" titleName="name" :datas="productList" placeholder="请选择商品"/>
        </div>
        <Search v-model.trim="params.filter" search-button-theme="h-btn-default"
                show-search-button class="w-180px ml-8px"
                placeholder="请输入订单号" @search="doSearch">
          <i class="h-icon-search"/>
        </Search>
      </template>
    </vxe-toolbar>
    <div class="flex1">
      <vxe-table row-id="id"
                 ref="tableRef"
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
<!--        <vxe-column type="checkbox" width="40" align="center"/>-->
        <vxe-column title="销售日期" field="orderDate" align="center" width="130"/>
        <vxe-column title="订单编号" field="orderNo" width="200"/>
        <vxe-column title="业务类别" field="salesType" width="200" :formatter="formatOrderType"/>
        <vxe-column title="客户" field="customerName" min-width="120"/>
        <vxe-column title="商品编码" field="productCode" width="100"/>
        <vxe-column title="商品名称" field="productName" width="100"/>
        <vxe-column title="销售单位" field="unitName" width="100"/>
        <vxe-column title="仓库名称" field="warehouseName" width="100"/>
        <vxe-column title="数量" field="quantity" width="100"/>
        <vxe-column title="单价" field="unitPrice" width="100"/>
        <!--        <vxe-column title="折扣金额" field="discountValue" width="120"/>-->
        <vxe-column title="销售收入" field="subtotal" width="120"/>
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
import {mapMutations} from "vuex";
import SalesReport from "@js/api/sales/SalesReport";
import Customer from "@js/api/basic/Customer";
import {loading, message} from "heyui.ext";
import * as XLSX from 'xlsx';
import Product from "@js/api/basic/Product";
import Warehouse from "@js/api/basic/Warehouse";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-dd");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-dd");

export default {
  name: "SalesItemReport",
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
        salesType: 'out'
      },
      customerList: [],
      warehouseList: [],
      productList: [],
      dateRange: {
        start: manba(startTime).format("YYYY-MM-dd"),
        end: manba(endTime).format("YYYY-MM-dd")
      },
      quantityTotal:null,
      subtotalTotal:null
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
    formatOrderType({ cellValue }) {
      if (cellValue === 'return') {
        return '退货';
      }
      if (cellValue === 'out') {
        return '销货';
      }
      return cellValue || '销货'; // 如果 cellValue 为空，则返回默认值 '销货'
    },
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
      return [["", "", "", "", "", "", "", "", quantityTotal.toFixed(2), "", subtotalTotal.toFixed(2)]];
    },

    printEvent () {
      const $table = this.$refs.tableRef
      if ($table) {
        $table.print()
      }
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
          '销售日期': item.orderDate,
          '订单编号': item.orderNo,
          '业务类别': this.params.salesType === 'return' ? '退货' : '销货',
          '客户': item.customerName,
          '商品编码': item.productCode,
          '商品名称': item.productName,
          '销售单位': item.unitName,
          '仓库名称': item.warehouseName,
          '数量': item.quantity,
          '单价': item.unitPrice,
          '销售收入': item.subtotal
        }));

        // 如果有合计行，添加到导出数据中
        exportData.push({
          '销售日期': '合计',
          '数量': this.quantityTotal,
          '销售收入': this.subtotalTotal
        });

        // 创建工作簿和工作表
        const ws = XLSX.utils.json_to_sheet(exportData);
        const wb = XLSX.utils.book_new();

        // 设置标题行样式
        ws['!cols'] = [
          { wch: 12 }, // 销售日期
          { wch: 30 }, // 订单编号
          { wch: 10 }, // 业务类别
          { wch: 15 }, // 客户
          { wch: 12 }, // 商品编码
          { wch: 20 }, // 商品名称
          { wch: 10 }, // 销售单位
          { wch: 12 }, // 仓库名称
          { wch: 10 }, // 数量
          { wch: 10 }, // 单价
          { wch: 12 }  // 销售收入
        ];
        XLSX.utils.book_append_sheet(wb, ws, '销售明细');

        // 导出文件
        const fileName = `销售明细报表_${manba().format('YYYY-MM-DD')}.xlsx`;
        XLSX.writeFile(wb, fileName);

        message.success('导出成功');
      } catch (error) {
        console.error('导出错误:', error);
        message.error('导出失败');
      } finally {
        loading.close();
      }
    },
    doSearch() {
      this.pagination.page = 1;
      if(!this.params.salesType){
        message.error("请选择业务类型~");
        return
      }
      this.loadList();
    },
    loadList(type = true) {
      this.loading = true;
      SalesReport.salesItem(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);

      Promise.all([
        Customer.select(),
        Warehouse.select(),
        Product.select(),
      ]).then((results) => {
        this.customerList = results[0].data || [];
        this.warehouseList = results[1].data || [];
        this.productList = results[2].data || [];

      }).finally(() => loading.close());
    },
  },
  created() {
    this.loadList();
  }
}
</script>
