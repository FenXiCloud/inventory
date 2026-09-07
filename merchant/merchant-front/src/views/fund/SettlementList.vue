<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-select
            v-model="params.orderType"
            :options="businessTypeList"
            :keys="{ value: 'type', label: 'name' }"
            clearable
            placeholder="业务类型"
            style="width: 160px; border-radius: 4px"
        />
        <t-date-range-picker
            v-model="dateRangeValue"
            clearable
            allow-input
            placeholder="单据日期"
            style="width: 260px; border-radius: 4px"
        />
        <t-input
            v-model="params.orderNo"
            clearable
            placeholder="请输入单据号"
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
      >
        <template #type="{ row }">
          {{ row.type == "1" ? "客户结算" : "供应商结算" }}
        </template>
        <template #sumDocumentAmount="{ row }">{{ money(row.sumDocumentAmount) }}</template>
        <template #sumVerifiedAmount="{ row }">{{ money(row.sumVerifiedAmount) }}</template>
        <template #sumUnverifiedAmount="{ row }">{{ money(row.sumUnverifiedAmount) }}</template>
        <template #orderStatus="{ row }">
          <t-tag
              v-if="row.statusMismatch"
              theme="danger"
              :title="'已平账但仍有剩余 ' + money(row.sumUnverifiedAmount) + '，请核对'"
          >已平账(异常)</t-tag>
          <t-tag
              v-else
              :theme="row.orderStatus === '已平账' ? 'success' : row.orderStatus === '未平账' ? 'warning' : 'default'"
          >{{ row.orderStatus }}</t-tag>
        </template>
      </t-table>
    </div>

    <div class="simple-page__pager">
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
import Settlement from "@js/api/fund/Settlement";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-DD");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-DD");
export default {
  name: "SettlementList",
  data() {
    return {
      businessTypeList: [
        {name: "客户结算", type: "1"},
        {name: "供应商结算", type: "2"},
      ],
      dataList: [],
      loading: false,
      pagination: { page: 1, pageSize: 20, total: 0 },
      params: { orderNo: null, orderType: null },
      dateRangeValue: [startTime, endTime],
      columns: [
        {colKey: 'orderDate', title: '单据日期', width: 120, align: 'center'},
        {colKey: 'orderNo', title: '单据编号', minWidth: 160, ellipsis: true},
        {colKey: 'sourceBusinessNo', title: '源单据号', minWidth: 160, ellipsis: true},
        {colKey: 'sourceBusinessType', title: '源单据类型', width: 110, align: 'center'},
        {colKey: 'sourceDate', title: '源单据日期', width: 120, align: 'center'},
        {colKey: 'sumDocumentAmount', title: '单据金额', width: 110, align: 'right'},
        {colKey: 'sumVerifiedAmount', title: '已核销/实收', width: 120, align: 'right'},
        {colKey: 'sumUnverifiedAmount', title: '剩余未结', width: 110, align: 'right'},
        {colKey: 'type', title: '业务类型', width: 120, align: 'center'},
        {colKey: 'personnelName', title: '客户/供应商', minWidth: 140, ellipsis: true},
        {colKey: 'orderStaffName', title: '业务员', width: 100, align: 'center'},
        {colKey: 'orderStatus', title: '状态', width: 100, align: 'center'},
        {colKey: 'remarks', title: '备注', minWidth: 120, ellipsis: true},
      ]
    };
  },
  computed: {
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        startTime: start || null,
        endTime: end || null,
      });
    },
  },
  methods: {
    money(v) {
      return v === null || v === undefined ? '' : Number(v).toFixed(2);
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
      Settlement.list(this.queryParams)
          .then(({data: {results, total}}) => {
            this.dataList = results || [];
            this.pagination.total = total;
          })
          .finally(() => (this.loading = false));
    },
  },
  created() {
    this.loadList();
  },
};
</script>
