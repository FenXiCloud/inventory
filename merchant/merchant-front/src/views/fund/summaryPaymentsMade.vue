<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">单据日期:</span>
          <DateRangePicker
            v-model="dateRange"
            @confirm="doSearch"
          ></DateRangePicker>
        </div>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">汇总依据</span>
          <Select
            v-model="params.type"
            class="w-180px"
            :datas="{ 1: '供应商', 2: '供应商类型', 3: '业务员' }"
            placeholder="选择单据类型"
            @change="changeType"
          />
        </div>
        <div v-if="params.type == 1" class="h-input-group">
          <span class="h-input-addon ml-8px">供应商</span>
          <Select
            v-model="paramsfilter.SupplierName"
            class="w-120px z-index-1"
            :datas="SupplierDataList"
            keyName="name"
            titleName="name"
            placeholder="选择供应商"
            :filterable="true"
            @change="selectSupplier($event)"
          >
          </Select>
        </div>
        <div v-if="params.type == 2" class="h-input-group">
          <span class="h-input-addon ml-8px">供应商类型</span>
          <Select
            ref="selectRef"
            style="z-index: 1"
            v-model="paramsfilter.SupplierTypeName"
            class="w-120px"
            :datas="SupplierCategoryList"
            keyName="name"
            titleName="name"
            placeholder="选择供应商类型"
            :filterable="true"
            @change="selectSupplierType($event)"
          >
          </Select>
        </div>
        <div v-if="params.type == 3" class="h-input-group">
          <span class="h-input-addon ml-8px">业务员</span>
          <Select
            ref="selectRef"
            style="z-index: 1"
            v-model="paramsfilter.orderStaffName"
            class="w-120px"
            :datas="orderStaffList"
            keyName="name"
            titleName="name"
            placeholder="选择业务员"
            :filterable="true"
            @change="selectOrderStaff($event)"
          >
          </Select>
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
        <!-- <vxe-column title="id" field="id"> </vxe-column> -->

        <template v-if="type == 1">
          <vxe-column
            title="供应商分类"
            field="supplierCategory"
            align="center"
            width="130"
          />
          <vxe-column
            title="供应商编码"
            align="center"
            field="supplierCode"
            min-width="120"
          />
          <vxe-column
            title="供应商名称"
            field="supplierName"
            align="center"
            width="130"
          />
        </template>
        <template v-if="type == 2">
          <vxe-column
            title="供应商分类"
            field="supplierCategory"
            align="center"
            width="130"
          />
        </template>
        <template v-if="type == 3">
          <vxe-column
            title="业务员编号"
            field="supplierCode"
            align="center"
            width="130"
          />
          <vxe-column
            title="业务员"
            field="supplierName"
            align="center"
            width="130"
          />
        </template>
        <vxe-column
          title="期初余额"
          field="openingBalance"
          align="center"
          min-width="120"
        />
        <vxe-column
          title="本期应付"
          field="currentPayable"
          align="center"
          min-width="120"
        />
        <vxe-column
          align="center"
          title="本期付款"
          field="currentPayment"
          min-width="120"
        />
        <vxe-column
          align="center"
          title="期末余额"
          field="closingBalance"
          min-width="120"
        />
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
            icon="h-icon-refresh"
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
import AccountFlow from '@js/api/fund/AccountFlow';
import OrderStaff from '@js/api/basic/OrderStaff';
import Supplier from '@js/api/basic/Supplier';
import SupplierCategory from '@js/api/basic/SupplierCategory';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-dd');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-dd');

export default {
  name: 'summaryPaymentsMade',
  data() {
    return {
      dataList: [],
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      loading: false,
      params: {
        type: '1'
      },
      paramsfilter: {},
      SupplierCategoryList: [],
      SupplierDataList: [],
      orderStaffList: [],
      totalCount: {},
      dateRange: {
        start: manba(startTime).format('YYYY-MM-dd'),
        end: manba(endTime).format('YYYY-MM-dd')
      },
      totalCountFooter: [],
      type: 1
    };
  },
  computed: {
    queryParams() {
      return Object.assign(this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        startDate: this.dateRange.start,
        endDate: this.dateRange.end
      });
    }
  },
  methods: {
    ...mapMutations(['pushTab']),

    footerMethod({ columns, data }) {
      return [this.totalCountFooter];
    },
    calcFoot() {
      let totalCount1 = [
        '合计',
        '',
        '',
        this.totalCount.totalOpeningBalance,
        this.totalCount.totalCurrentPayable,
        this.totalCount.totalCurrentPayment,
        this.totalCount.totalClosingBalance
      ];
      let totalCount2 = [
        '合计',
        this.totalCount.totalOpeningBalance,
        this.totalCount.totalCurrentPayable,
        this.totalCount.totalCurrentPayment,
        this.totalCount.totalClosingBalance
      ];
      let totalCount3 = [
        '合计',
        '',
        this.totalCount.totalOpeningBalance,
        this.totalCount.totalCurrentPayable,
        this.totalCount.totalCurrentPayment,
        this.totalCount.totalClosingBalance
      ];

      if (this.type == 1) {
        this.totalCountFooter = totalCount1;
      } else if (this.type == 2) {
        this.totalCountFooter = totalCount2;
      } else if (this.type == 3) {
        this.totalCountFooter = totalCount3;
      }
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
      this.type = this.params.type;
    },
    changeType() {
      this.params.SupplierTypeId = null;
      this.params.salesmanId = null;
      this.params.SupplierId = null;
      // this.loadList();
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
    //加载供应商分类
    loadSupplierCategory() {
      SupplierCategory.select()
        .then(({ data }) => {
          this.SupplierCategoryList = data || [];
          // this.pagination.total = total;
        })
        .finally();
    },
    //加载供应商列表
    loadSupplier() {
      this.loading = true;
      Supplier.select()
        .then(({ data }) => {
          this.SupplierDataList = data || [];
          // this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
    },
    loadList(type = true) {
      this.loading = true;
      AccountFlow.summaryPayableDetails(this.queryParams)
        .then(
          ({
            data: {
              payableDetailsList,
              receivableDetailsListTotal,
              totalOpeningBalance,
              totalCurrentPayable,
              totalCurrentPayment,
              totalClosingBalance
            }
          }) => {
            this.dataList = payableDetailsList || [];
            this.pagination.total = receivableDetailsListTotal;
            this.totalCount = {
              totalOpeningBalance,
              totalCurrentPayable,
              totalCurrentPayment,
              totalClosingBalance
            };

            this.calcFoot();
          }
        )
        .finally(() => (this.loading = false));
    },
    selectOrderStaff(e) {
      this.params.salesmanId = e?.id || null;
    },
    selectSupplierType(e) {
      this.params.SupplierTypeId = e?.id || null;
    },
    selectSupplier(e) {
      this.params.SupplierId = e?.id || null;
    }
  },
  created() {
    this.loadList();
    this.loadOrderStaff();
    this.loadSupplier();
    this.loadSupplierCategory();
  }
};
</script>
