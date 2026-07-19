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
        <vxe-column
          title="单据Id"
          field="businessId"
          align="center"
          width="130"
        />
        <vxe-column
          title="单据编号"
          field="businessNo"
          align="center"
          width="200"
        />
        <vxe-column
          align="center"
          title="单据日期"
          field="businessDate"
          min-width="120"
        />
        <vxe-column
          align="center"
          title="操作类型"
          field="customerFlowType"
          min-width="120"
        />
        <vxe-column
          align="center"
          title="销售金额"
          field="salesAmount"
          min-width="120"
        />
        <vxe-column
          align="center"
          title="优惠金额"
          field="preferentialAmount"
          min-width="120"
        />
        <vxe-column
          align="center"
          title="应收金额"
          field="receivableAmount"
          min-width="120"
        />
        <vxe-column
          align="center"
          title="实收金额"
          field="paidUpAmount"
          min-width="120"
        />
        <vxe-column
          align="center"
          title="应收款余额"
          field="balanceReceivables"
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
import { DialogPlugin, LoadingPlugin, MessagePlugin } from 'tdesign-vue-next';
import AccountFlow from '@js/api/fund/AccountFlow';
import Customer from '@js/api/basic/Customer';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-dd');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-dd');

export default {
  name: 'customerStatements',
  data() {
    return {
      dataList: [],
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      loading: false,
      params: {},
      paramsfilter: {},
      customerDataList: [],
      totalCount: {},
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

    footerMethod({ columns, data }) {
      return [];
    },

    doSearch() {
      this.pagination.page = 1;
      if (!this.params.customerId) {
        return MessagePlugin.error('请选择客户进行查询~');
      }
      this.loadList();
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
    loadList() {
      this.loading = true;
      AccountFlow.getCustomerBillFlows(this.queryParams)
        .then(({ data: { results, total } }) => {
          this.dataList = results || [];
          this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
    },

    selectCustomer(e) {
      this.params.customerId = e?.id || null;
    }
  },
  created() {
    this.loadCustomer();
  }
};
</script>
