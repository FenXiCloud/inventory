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
            :datas="{ 1: '客户', 2: '客户类型', 3: '销售员' }"
            placeholder="选择单据类型"
            @change="changeType"
          />
        </div>
        <div v-if="params.type == 1" class="h-input-group">
          <span class="h-input-addon ml-8px">客户</span>
          <Select
            v-model="paramsfilter.customerName"
            class="w-120px z-index-1"
            :datas="customerDataList"
            keyName="name"
            titleName="name"
            placeholder="选择客户"
            :filterable="true"
            @change="selectCustomer($event)"
          >
          </Select>
        </div>
        <div v-if="params.type == 2" class="h-input-group">
          <span class="h-input-addon ml-8px">客户类型</span>
          <Select
            ref="selectRef"
            style="z-index: 1"
            v-model="paramsfilter.customerTypeName"
            class="w-120px"
            :datas="CustomerCategoryList"
            keyName="name"
            titleName="name"
            placeholder="选择客户类型"
            :filterable="true"
            @change="selectCustomerType($event)"
          >
          </Select>
        </div>
        <div v-if="params.type == 3" class="h-input-group">
          <span class="h-input-addon ml-8px">销售员</span>
          <Select
            ref="selectRef"
            style="z-index: 1"
            v-model="paramsfilter.orderStaffName"
            class="w-120px"
            :datas="orderStaffList"
            keyName="name"
            titleName="name"
            placeholder="选择销售员"
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
            title="客户分类"
            field="customerCategory"
            align="center"
            width="130"
          />
          <vxe-column
            title="客户编码"
            align="center"
            field="customerCode"
            min-width="120"
          />
          <vxe-column
            title="客户名称"
            field="customerName"
            align="center"
            width="130"
          />
        </template>
        <template v-if="type == 2">
          <vxe-column
            title="客户分类"
            field="customerCategory"
            align="center"
            width="130"
          />
        </template>
        <template v-if="type == 3">
          <vxe-column
            title="销售员编号"
            field="customerCode"
            align="center"
            width="130"
          />
          <vxe-column
            title="销售员"
            field="customerName"
            align="center"
            width="130"
          />
        </template>
        <vxe-column
          title="期初余额"
          field="openingBalance"
          align="center"
          width="130"
        />
        <vxe-column
          title="本期应收"
          field="currentReceivable"
          align="center"
          width="200"
        />
        <vxe-column
          align="center"
          title="本期收款"
          field="currentReceipt"
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
import AccountFlow from '@js/api/fund/AccountFlow';
import OrderStaff from '@js/api/basic/OrderStaff';
import Customer from '@js/api/basic/Customer';
import CustomerCategory from '@js/api/basic/CustomerCategory';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-dd');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-dd');

export default {
  name: 'receivableSummary',
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
      CustomerCategoryList: [],
      customerDataList: [],
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
        this.totalCount.totalCurrentReceivable,
        this.totalCount.totalCurrentReceipt,
        this.totalCount.totalClosingBalance
      ];
      let totalCount2 = [
        '合计',
        this.totalCount.totalOpeningBalance,
        this.totalCount.totalCurrentReceivable,
        this.totalCount.totalCurrentReceipt,
        this.totalCount.totalClosingBalance
      ];
      let totalCount3 = [
        '合计',
        '',
        this.totalCount.totalOpeningBalance,
        this.totalCount.totalCurrentReceivable,
        this.totalCount.totalCurrentReceipt,
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
      this.params.customerTypeId = null;
      this.params.salesmanId = null;
      this.params.customerId = null;
      // this.loadList();
    },
    //加载销售员列表
    loadOrderStaff() {
      OrderStaff.orderStaffList()
        .then(({ data }) => {
          this.orderStaffList = data || [];
          // this.pagination.total = total;
        })
        .finally();
    },
    //加载客户分类
    loadCustomerCategory() {
      CustomerCategory.select()
        .then(({ data }) => {
          this.CustomerCategoryList = data || [];
          // this.pagination.total = total;
        })
        .finally();
    },
    //加载客户列表
    loadCustomer() {
      this.loading = true;
      Customer.select()
        .then(({ data }) => {
          this.customerDataList = data || [];
          // this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
    },
    loadList(type = true) {
      this.loading = true;
      AccountFlow.summaryReceivableDetails(this.queryParams)
        .then(
          ({
            data: {
              receivableDetailsList,
              receivableDetailsListTotal,
              totalOpeningBalance,
              totalCurrentReceivable,
              totalCurrentReceipt,
              totalClosingBalance
            }
          }) => {
            this.dataList = receivableDetailsList || [];
            this.pagination.total = receivableDetailsListTotal;
            this.totalCount = {
              totalOpeningBalance,
              totalCurrentReceivable,
              totalCurrentReceipt,
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
    selectCustomerType(e) {
      this.params.customerTypeId = e?.id || null;
    },
    selectCustomer(e) {
      this.params.customerId = e?.id || null;
    }
  },
  created() {
    this.loadList();
    this.loadOrderStaff();
    this.loadCustomer();
    this.loadCustomerCategory();
  }
};
</script>
