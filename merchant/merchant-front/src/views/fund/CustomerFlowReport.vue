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
        <vxe-column
          title="客户"
          field="customerName"
          align="center"
          width="130"
        />
        <vxe-column
          title="销售人员"
          field="staffName"
          align="center"
          width="130"
        />
        <vxe-column
          title="单据日期"
          field="orderDate"
          align="center"
          width="130"
        />
        <vxe-column
          title="单据编号"
          field="orderNo"
          align="center"
          width="200"
        />
        <vxe-column title="业务类型" field="businessType" min-width="120" />
        <vxe-column
          title="增加应收款金额"
          field="receivableAmount"
          min-width="120"
        />
        <vxe-column
          title="增加预收款金额"
          field="prepaymentAmount"
          min-width="120"
        />
        <vxe-column title="应收款余额" field="balance" min-width="120" />
        <vxe-column title="备注" field="remarks" width="120" />
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
const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-dd');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-dd');

export default {
  name: 'CustomerFlowReport',
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
      // return this.footerMethodFormat(
      //   { columns, data },
      //   ['receivableAmount', 'prepaymentAmount', 'balance'],
      //   'totalTb1'
      // );
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },

    loadList(type = true) {
      this.loading = true;
      AccountFlow.getReceivableDetailReport(this.queryParams)
        .then(({ data: { results, total } }) => {
          this.dataList = results || [];
          this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
    }
  },
  created() {
    this.loadList();
  }
};
</script>
