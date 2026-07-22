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
          :foot-data="footData"
          @select-change="onSelectChange"
      />
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
import { MessagePlugin } from 'tdesign-vue-next';
import OrderReceipt from '@js/api/fund/OrderReceipt';
import OrderPayment from '@js/api/fund/OrderPayment';

export default {
  name: 'SourceForm',
  emits: { close: null, success: null },
  props: {
    params: Object,
    URL: {
      type: String,
      default: 'OrderReceipt'
    }
  },
  data() {
    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth() + 1;
    const day = now.getDate();
    const today = `${year}-${month < 10 ? '0' + month : month}-${day < 10 ? '0' + day : day}`;
    return {
      dataList: [],
      selectedRowKeys: [],
      selectedRows: [],
      openingBalance: {
        salesOrderId: '-1',
        salesOrderNo: '期初余额',
        businessType: 2,
        businessDate: today,
        documentAmount: 0,
        verifiedAmount: 0,
        unverifiedAmount: 0
      },
      loading: false,
      pagination: {
        page: 1,
        pageSize: 5,
        total: 0
      },
      columns: [
        { colKey: 'row-select', type: 'multiple', width: 46 },
        { colKey: 'salesOrderNo', title: '订单编号', width: 200, ellipsis: true },
        { colKey: 'businessTypeText', title: '业务类别', width: 120, align: 'center' },
        { colKey: 'businessDate', title: '单据日期', width: 130, align: 'center' },
        { colKey: 'documentAmount', title: '单据金额', width: 120, align: 'right' },
        { colKey: 'verifiedAmount', title: '已核销金额', width: 120, align: 'right' },
        { colKey: 'unverifiedAmount', title: '未核销金额', width: 120, align: 'right' },
      ]
    };
  },
  computed: {
    tableData() {
      return (this.dataList || []).map((row, index) => {
        let businessTypeText = '-';
        if (this.URL === 'OrderReceipt') {
          businessTypeText = row.businessType == 2 ? '期初余额' : '销售出库单';
        } else if (this.URL === 'OrderPayment') {
          businessTypeText = row.businessType == 2 ? '期初余额' : '采购入库单';
        }
        return {
          ...row,
          businessTypeText,
          rowKey: `${row.salesOrderNo || row.salesOrderId || 'row'}_${index}`
        };
      });
    },
    footData() {
      const sum = (key) => {
        const total = (this.selectedRows || []).reduce((acc, row) => acc + Number(row[key] || 0), 0);
        return total.toFixed(2);
      };
      return [{
        salesOrderNo: `已选 ${this.selectedRows.length} 条`,
        documentAmount: sum('documentAmount'),
        verifiedAmount: sum('verifiedAmount'),
        unverifiedAmount: sum('unverifiedAmount'),
      }];
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
      const apiMap = { OrderReceipt, OrderPayment };
      const apiModule = apiMap[this.URL];
      this.loading = true;
      this.selectedRowKeys = [];
      this.selectedRows = [];
      this.openingBalance.documentAmount = this.params.balance;
      this.openingBalance.unverifiedAmount = this.params.balance;

      const params = {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize
      };
      if (this.URL === 'OrderReceipt') {
        params.customerId = this.params.customerId;
      } else if (this.URL === 'OrderPayment') {
        params.supplierId = this.params.supplierId;
      }

      apiModule
        .writeOff(params)
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
        if (this.URL === 'OrderPayment') {
          row.businessId = row.salesOrderId;
          row.businessNo = row.salesOrderNo;
        }
        delete row.id;
        delete row.rowKey;
        delete row.businessTypeText;
        return row;
      });
      this.$emit('success', checkList);
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
