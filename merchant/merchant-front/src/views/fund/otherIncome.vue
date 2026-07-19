<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">单据日期:</span>
          <DateRangePicker v-model="dateRange"></DateRangePicker>
        </div>

        <div class="h-input-group">
          <span class="h-input-addon ml-8px">业务员</span>
          <Select
            ref="selectRef"
            style="z-index: 1"
            v-model="params.orderStaffName"
            class="w-120px"
            :datas="orderStaffList"
            keyName="name"
            titleName="name"
            placeholder="选择业务员"
            :filterable="true"
            @change="selectOrderStaff($event)"
          >
            <!-- <template #bottom>
              <Button no-border icon="add" @click="addOrderStaff()"
                >新建</Button
              >
            </template> -->
          </Select>
        </div>

        <div class="h-input-group">
          <span class="h-input-addon ml-8px">类型</span>
          <Select
            v-model="params.type"
            class="w-180px"
            :datas="{ 1: '其他收入', 2: '其他支出' }"
            placeholder="选择单据类型"
          />
        </div>
      </template>
      <template #tools>
        <Button color="primary" @click="doSearch">查询</Button>
      </template>
    </vxe-toolbar>
    <div class="flex1">
      <vxe-table
        row-id="id"
        ref="table"
        height="auto"
        :data="dataList"
        highlight-hover-row
        show-overflow
        show-footer
        :footer-method="footerMethod"
        :row-config="{ height: 48 }"
        :column-config="{ resizable: true }"
        :sort-config="{ remote: true }"
        :loading="loading"
      >
        <!-- <vxe-column type="checkbox" width="40" align="center" /> -->
        <vxe-column title="单据编号" field="documentNumber" align="center">
        </vxe-column>
        <vxe-column title="日期" field="date" align="center"></vxe-column>
        <!-- <vxe-column title="往来单位" field="businessPartner" align="center" /> -->
        <vxe-column
          :title="amountName"
          field="amount"
          align="center"
          width="130"
        >
        </vxe-column>
        <vxe-column title="收支类别" field="accountType" align="center" />
        <vxe-column title="业务员" field="staffName" align="center" />
      </vxe-table>
    </div>
    <div class="justify-between items-center pt-5px">
      <vxe-pager
        perfect
        @page-change="loadList(false)"
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :layouts="[
          'PrevJump',
          'PrevPage',
          'Number',
          'NextPage',
          'NextJump',
          'Sizes',
          'Total'
        ]"
      >
        <template #left>
          <vxe-button
            @click="loadList(false)"
            type="text"
            size="mini"
            icon="vxe-icon-refresh"
            :loading="loading"
          ></vxe-button>
        </template>
      </vxe-pager>
    </div>
  </div>
</template>
<script>
import manba from 'manba';
import { mapMutations } from 'vuex';
import OrderStaff from '@js/api/basic/OrderStaff';
import AccountFlow from '@js/api/fund/AccountFlow';
import Supplier from '@js/api/basic/Supplier';
import Warehouse from '@js/api/basic/Warehouse';
import { LoadingPlugin } from 'tdesign-vue-next';
import Product from '@js/api/basic/Product';
import ProductCategory from '@js/api/basic/ProductCategory';
import SupplierCategory from '@js/api/basic/SupplierCategory';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-dd');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-dd');

export default {
  name: 'otherIncome',
  data() {
    return {
      dataList: [],
      orderStaffList: [],
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      loading: false,
      amountName: '收入金额',
      params: {
        type: 1
      },
      dateRange: {
        start: manba(startTime).format('YYYY-MM-dd'),
        end: manba(endTime).format('YYYY-MM-dd')
      }
    };
  },
  computed: {
    queryParams() {
      return Object.assign(this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        startTime: this.dateRange.start,
        endTime: this.dateRange.end
      });
    }
  },
  methods: {
    ...mapMutations(['pushTab']),
    footerMethodFormat({ columns, data }, list, totalName) {
      // 初始化合计行，默认所有列为空字符串
      const footerRow = new Array(columns.length).fill('');

      // 设置第一列为“合计”
      footerRow[0] = '合计';

      // 遍历列，仅对需要合计的字段进行计算
      columns.forEach((column, index) => {
        if (list.includes(column.property)) {
          let total = 0;
          data.forEach((row) => {
            const value = parseFloat(row[column.property]);
            if (!isNaN(value)) {
              total += value;
            }
          });
          footerRow[index] = total.toFixed(2); // 将合计值放入对应位置

          this[totalName] = total;
        }
      });

      // this.form.collectionAmount = this.calcCollectionAmount();

      return [footerRow]; // 返回二维数组用于渲染 footer
    },
    footerMethod({ columns, data }) {
      return [[]];
      // return this.footerMethodFormat({ columns, data }, ['amount'], 'totalTb1');
    },
    doSearch() {
      if (this.params.type == 1) {
        this.amountName = '收入金额';
      } else if (this.params.type == 2) {
        this.amountName = '支出金额';
      }
      this.pagination.page = 1;
      this.loadList();
    },

    loadList(type = true) {
      this.loading = true;
      AccountFlow.otherFundDetails(this.queryParams)
        .then(({ data: { results, total } }) => {
          this.dataList = results || [];
          this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
    },
    //加载业务员列表
    loadOrderStaff() {
      OrderStaff.orderStaffList()
        .then(({ data }) => {
          this.orderStaffList = data || [];
          // this.pagination.total = total;
        })
        .finally();
    },
    selectOrderStaff(e) {
      this.params.orderStaffId = e?.id || null;
    }
  },
  created() {
    this.loadList();
    this.loadOrderStaff();
  }
};
</script>
