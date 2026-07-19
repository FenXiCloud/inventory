<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <t-table
          row-key="rowKey"
          size="medium"
          bordered
          stripe
          hover
          height="360px"
          table-layout="fixed"
          :data="tableData"
          :columns="columns"
          :loading="loading"
          :selected-row-keys="selectedRowKeys"
          @select-change="onSelectChange"
      >
        <template #businessType="{ row }">
          <span v-if="params.sourceType == '预收'">收款</span>
          <span v-else-if="params.sourceType == '应收'">{{ row.businessType != 2 ? '普通销售' : '期初余额' }}</span>
          <span v-else-if="params.sourceType == '预付'">付款</span>
          <span v-else-if="params.sourceType == '应付'">{{ row.businessType != 2 ? '普通采购' : '期初余额' }}</span>
          <span v-else>{{ row.businessType || row.orderType || '-' }}</span>
        </template>
      </t-table>
      <div class="modal-pager">
        <t-pagination
            v-model:current="pagination.page"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :show-jumper="true"
            :show-page-size="true"
            :page-size-options="[5, 10, 20]"
            :popup-props="{ attach: 'body' }"
            @change="onPageChange"
        />
      </div>
    </div>
    <div class="modal-column-between">
      <t-button variant="outline" :loading="loading" @click="$emit('close')">取消</t-button>
      <t-button theme="primary" :loading="loading" @click="confirm">保存</t-button>
    </div>
  </div>
</template>

<script>
/**
 * @功能描述: 核销源单选择
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import { MessagePlugin } from 'tdesign-vue-next';
import OrderReceipt from '@js/api/fund/OrderReceipt';
import OrderPayment from '@js/api/fund/OrderPayment';

export default {
  name: 'sourceByVerfication',
  emits: { close: null, success: null },
  props: { params: Object },
  data() {
    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth() + 1;
    const day = now.getDate();
    const today = `${year}-${month < 10 ? '0' + month : month}-${day < 10 ? '0' + day : day}`;
    return {
      loading: false,
      dataList: [],
      selectedRowKeys: [],
      selectedRows: [],
      pagination: {
        page: 1,
        pageSize: 5,
        total: 0
      },
      tableJson: [],
      openingBalance: {
        salesOrderId: '-1',
        salesOrderNo: '期初余额',
        businessType: 2,
        businessDate: today,
        documentAmount: 0,
        verifiedAmount: 0,
        unverifiedAmount: 0
      }
    };
  },
  computed: {
    tableColumns() {
      return this.tableJson.filter((i) => !i.none);
    },
    columns() {
      const cols = [{ colKey: 'row-select', type: 'multiple', width: 46 }];
      this.tableColumns.forEach((item) => {
        const col = {
          colKey: item.field,
          title: item.title,
          width: item.width,
          ellipsis: true
        };
        if (item.field === 'businessType' || item.field === 'orderType') {
          col.colKey = 'businessType';
          col.cell = 'businessType';
        }
        cols.push(col);
      });
      return cols;
    },
    tableData() {
      return (this.dataList || []).map((row, index) => ({
        ...row,
        rowKey: `${row.id || row.salesOrderId || row.orderNo || row.salesOrderNo || 'row'}_${index}`
      }));
    }
  },
  methods: {
    onSelectChange(keys, { selectedRowData }) {
      this.selectedRowKeys = keys;
      this.selectedRows = selectedRowData || [];
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    loadList() {
      this.loading = true;
      this.selectedRowKeys = [];
      this.selectedRows = [];
      this.openingBalance.documentAmount = this.params.balance;
      this.openingBalance.unverifiedAmount = this.params.balance;
      if (this.params.sourceType == '预收') {
        this.getAdvanceReceipt();
      } else if (this.params.sourceType == '应收') {
        this.getAccountsReceivable();
      } else if (this.params.sourceType == '预付') {
        this.getAdvancePayment();
      } else if (this.params.sourceType == '应付') {
        this.getAccountsPayable();
      } else {
        this.loading = false;
      }
    },
    getAdvanceReceipt() {
      this.tableJson = [
        { title: '源单ID', field: 'id', toField: 'businessId', none: true },
        { title: '源单编号', field: 'orderNo', toField: 'businessNo', width: 150 },
        { title: '业务类别', field: 'orderType', toField: 'businessType', width: 100 },
        { title: '单据日期', field: 'orderDate', toField: 'businessDate', width: 130 },
        { title: '单据金额', field: 'shouldVerificationAmount', toField: 'documentAmount', width: 120 },
        { title: '已核销金额', field: 'hasVerificationAmount', toField: 'verifiedAmount', width: 120 },
        { title: '未核销金额', field: 'notVerificationAmount', toField: 'unverifiedAmount', width: 120 },
        { title: '备注', field: 'remarks', toField: 'businessRemarks' },
      ];
      OrderReceipt.list({
        writeOff: 1,
        customerId: this.params.personnelId,
        orderType: 2,
        page: this.pagination.page,
        pageSize: this.pagination.pageSize
      })
        .then(({ data: { results, total } }) => {
          this.dataList = results || [];
          this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
    },
    getAccountsReceivable() {
      this.tableJson = [
        { title: '源单ID', field: 'salesOrderId', toField: 'businessId', none: true },
        { title: '源单编号', field: 'salesOrderNo', toField: 'businessNo', width: 200 },
        { title: '业务类别', field: 'businessType', toField: 'businessType', width: 100 },
        { title: '单据日期', field: 'businessDate', toField: 'businessDate', width: 130 },
        { title: '单据金额', field: 'documentAmount', toField: 'documentAmount', width: 120 },
        { title: '已核销金额', field: 'verifiedAmount', toField: 'verifiedAmount', width: 120 },
        { title: '未核销金额', field: 'unverifiedAmount', toField: 'unverifiedAmount' },
      ];
      OrderReceipt.writeOffTheOrder({
        customerId: this.params.personnelId,
        orderType: this.params.type,
        page: this.pagination.page,
        pageSize: this.pagination.pageSize
      })
        .then(({ data: { results, total } }) => {
          this.dataList = [this.openingBalance, ...(results || [])];
          this.pagination.total = (total || 0) + 1;
        })
        .finally(() => (this.loading = false));
    },
    getAdvancePayment() {
      this.tableJson = [
        { title: '源单ID', field: 'id', toField: 'businessId', none: true },
        { title: '源单编号', field: 'orderNo', toField: 'businessNo', width: 150 },
        { title: '业务类别', field: 'orderType', toField: 'businessType', width: 100 },
        { title: '单据日期', field: 'orderDate', toField: 'businessDate', width: 130 },
        { title: '单据金额', field: 'shouldVerificationAmount', toField: 'documentAmount', width: 120 },
        { title: '已核销金额', field: 'hasVerificationAmount', toField: 'verifiedAmount', width: 120 },
        { title: '未核销金额', field: 'notVerificationAmount', toField: 'unverifiedAmount', width: 120 },
        { title: '备注', field: 'remarks', toField: 'businessRemarks' },
      ];
      OrderPayment.list({
        writeOff: 1,
        orderType: 2,
        supplierId: this.params.personnelId,
        page: this.pagination.page,
        pageSize: this.pagination.pageSize
      })
        .then(({ data: { results, total } }) => {
          this.dataList = results || [];
          this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
    },
    getAccountsPayable() {
      this.tableJson = [
        { title: '源单ID', field: 'salesOrderId', toField: 'businessId', none: true },
        { title: '源单编号', field: 'salesOrderNo', toField: 'businessNo', width: 200 },
        { title: '业务类别', field: 'businessType', toField: 'businessType', width: 100 },
        { title: '单据日期', field: 'businessDate', toField: 'businessDate', width: 130 },
        { title: '单据金额', field: 'documentAmount', toField: 'documentAmount', width: 120 },
        { title: '已核销金额', field: 'verifiedAmount', toField: 'verifiedAmount', width: 120 },
        { title: '未核销金额', field: 'unverifiedAmount', toField: 'unverifiedAmount' },
      ];
      OrderPayment.writeOffTheOrder({
        supplierId: this.params.personnelId,
        page: this.pagination.page,
        pageSize: this.pagination.pageSize
      })
        .then(({ data: { results, total } }) => {
          this.dataList = [this.openingBalance, ...(results || [])];
          this.pagination.total = (total || 0) + 1;
        })
        .finally(() => (this.loading = false));
    },
    confirm() {
      if (!this.selectedRows.length) {
        return MessagePlugin.warning('请至少选择一条源单');
      }
      const checkList = this.selectedRows.map((item) => {
        const row = { ...item };
        delete row.rowKey;
        return row;
      });
      this.$emit('success', checkList, this.tableJson);
    }
  },
  created() {
    this.loadList();
  }
};
</script>

<style scoped>
.modal-pager {
  padding: 10px 0 0;
  display: flex;
  justify-content: flex-end;
}
</style>
