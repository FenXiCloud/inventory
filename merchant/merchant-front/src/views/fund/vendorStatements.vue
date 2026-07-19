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
          field="supplierFlowType"
          min-width="120"
        />
        <vxe-column
          align="center"
          title="采购金额"
          field="purchaseAmount"
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
          title="应付金额"
          field="copeWithAmount"
          min-width="120"
        />
        <vxe-column
          align="center"
          title="实付金额"
          field="actualPaymentAmount"
          min-width="120"
        />
        <vxe-column
          align="center"
          title="应付余额"
          field="balancePayable"
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
import Supplier from '@js/api/basic/Supplier';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-dd');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-dd');

export default {
  name: 'vendorStatements',
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
      SupplierDataList: [],
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
      if (!this.params.supplierId) {
        return MessagePlugin.error('请选择供应商进行查询~');
      }
      this.loadList();
    },

    loadList() {
      this.loading = true;
      AccountFlow.listBySupplier(this.queryParams)
        .then(({ data: { results, total } }) => {
          this.dataList = results || [];
          this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
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
    selectSupplier(e) {
      this.params.supplierId = e?.id || null;
    }
  },
  created() {
    this.loadSupplier();
  }
};
</script>
