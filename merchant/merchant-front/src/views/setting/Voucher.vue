<template>
  <div class="simple-page">
    <div class="simple-page__split">
      <div class="simple-page__side">
        <t-table
            row-key="id"
            size="medium"
            bordered
            hover
            height="100%"
            table-layout="auto"
            :data="documentTypeDataList"
            :columns="documentTypeColumns"
            :selected-row-keys="selectedDocumentTypeKeys"
            :active-row-keys="selectedDocumentTypeKeys"
            @row-click="onDocumentTypeRowClick"
            @select-change="onDocumentTypeSelect"
        />
      </div>

      <div class="simple-page__main">
        <div class="simple-page__toolbar">
          <t-space break-line>
            <t-date-range-picker
                v-model="dateRangeValue"
                clearable
                allow-input
                placeholder="单据日期"
                style="width: 260px; border-radius: 4px"
            />
            <t-checkbox v-model="excludeVouchered">仅未生成凭证</t-checkbox>
            <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="doSearch">查询</t-button>
            <t-button theme="primary" style="border-radius: 4px" :loading="generating" :disabled="!canGenerate.length" @click="generateSelected">生成凭证</t-button>
          </t-space>
        </div>

        <div class="simple-page__hint">
          从已审核业务单据生成云财务凭证。请先完成「关联财务」「凭证模板」「辅助项映射」配置。
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
              :selected-row-keys="selectedRowKeys"
              @select-change="onSelectChange"
          >
            <template #ops="{ row }">
              <t-space size="small">
                <t-link v-if="!row.voucherCode" theme="primary" @click="generateOne(row)">推送</t-link>
                <t-link v-else theme="primary" @click="viewVoucher(row)">查看</t-link>
              </t-space>
            </template>
            <template #amount="{ row }">
              {{ formatMoney(row.amount) }}
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
    </div>
  </div>
</template>

<script>
import manba from 'manba';
import { MessagePlugin } from 'tdesign-vue-next';
import { openDialog, closeDialog } from '@common/dialog';
import { h } from 'vue';
import FinanceVoucher from '@js/api/setting/FinanceVoucher';
import VoucherForm from './VoucherForm.vue';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-DD');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-DD');

/** 库存/进销存类单据（与凭证模板 type 对齐） */
const DOCUMENT_TYPES = [
  '采购入库单', '销售退货单', '其他入库单', '调拨单',
  '采购退货单', '销售出库单', '其他出库单', '盘点单', '成本调整单'
];

export default {
  name: 'Voucher',
  data() {
    return {
      documentTypeDataList: DOCUMENT_TYPES.map((documentType, index) => ({
        id: index + 1,
        documentType
      })),
      selectedDocumentTypeKeys: [1],
      selectedRowKeys: [],
      selectedRows: [],
      loading: false,
      generating: false,
      excludeVouchered: true,
      dateRangeValue: [startTime, endTime],
      params: {
        documentType: DOCUMENT_TYPES[0]
      },
      dataList: [],
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      documentTypeColumns: [
        { colKey: 'row-select', type: 'single', width: 46 },
        { colKey: 'documentType', title: '单据类型', minWidth: 120, ellipsis: true }
      ],
      columns: [
        { colKey: 'row-select', type: 'multiple', width: 46 },
        { colKey: 'ops', title: '操作', width: 120, align: 'center', fixed: 'left' },
        { colKey: 'orderDate', title: '日期', width: 120, align: 'center' },
        { colKey: 'orderNo', title: '单据编号', minWidth: 160, ellipsis: true },
        { colKey: 'documentType', title: '类型', width: 120, align: 'center' },
        { colKey: 'supplierName', title: '供应商', minWidth: 120, ellipsis: true },
        { colKey: 'customerName', title: '客户', minWidth: 120, ellipsis: true },
        { colKey: 'amount', title: '金额', width: 120, align: 'right' },
        { colKey: 'createName', title: '制单人', width: 100, align: 'center' },
        { colKey: 'voucherCode', title: '凭证号', width: 120, align: 'center' }
      ]
    };
  },
  computed: {
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        documentType: this.params.documentType,
        excludeVouchered: this.excludeVouchered,
        startDate: start || null,
        endDate: end || null
      };
    },
    canGenerate() {
      return (this.selectedRows || []).filter((row) => !row.voucherCode);
    }
  },
  methods: {
    formatMoney(val) {
      if (val == null || val === '') return '-';
      return Number(val).toFixed(2);
    },
    onDocumentTypeSelect(keys) {
      if (!keys || !keys.length) return;
      this.selectedDocumentTypeKeys = keys.slice(0, 1);
      const row = this.documentTypeDataList.find((item) => item.id === keys[0]);
      if (row) {
        this.params.documentType = row.documentType;
        this.doSearch();
      }
    },
    onDocumentTypeRowClick({ row }) {
      this.onDocumentTypeSelect([row.id]);
    },
    onSelectChange(keys, { selectedRowData }) {
      this.selectedRowKeys = keys;
      this.selectedRows = selectedRowData || [];
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
      this.selectedRowKeys = [];
      this.selectedRows = [];
      FinanceVoucher.candidates(this.queryParams)
        .then(({ data }) => {
          this.dataList = data?.results || [];
          this.pagination.total = data?.total || 0;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    toForm(row) {
      return {
        orderId: row.orderId || row.id,
        orderName: row.orderName || row.orderNo,
        amount: row.amount,
        type: row.documentType || this.params.documentType,
        orderTime: row.orderDate,
        customerId: row.customerId,
        supplierId: row.supplierId,
        productId: row.productId,
        remark: row.orderNo
      };
    },
    generateOne(row) {
      this.generating = true;
      FinanceVoucher.save(this.toForm(row))
        .then(() => {
          MessagePlugin.success('推送成功');
          this.loadList();
        })
        .finally(() => {
          this.generating = false;
        });
    },
    generateSelected() {
      const rows = this.canGenerate;
      if (!rows.length) {
        return MessagePlugin.warning('请选择尚未生成凭证的单据');
      }
      this.generating = true;
      FinanceVoucher.batch(rows.map((row) => this.toForm(row)))
        .then(() => {
          MessagePlugin.success('批量推送成功');
          this.loadList();
        })
        .finally(() => {
          this.generating = false;
        });
    },
    viewVoucher(row) {
      const dialogId = openDialog({
        header: '查看凭证',
        closeOnOverlayClick: false,
        width: '90%',
        body: h(VoucherForm, {
          voucherId: row.voucherId,
          type: 'look',
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.loadList();
            closeDialog(dialogId);
          }
        })
      });
    }
  },
  created() {
    this.loadList();
  }
};
</script>

<style scoped>
.simple-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
  padding: 0 12px;
  box-sizing: border-box;
  overflow: hidden;
}

.simple-page__split {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 12px;
  overflow: hidden;
}

.simple-page__side {
  width: 200px;
  flex-shrink: 0;
  min-height: 0;
  overflow: hidden;
  padding: 8px 0;
}

.simple-page__main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.simple-page__toolbar {
  flex-shrink: 0;
  padding: 8px 0;
}

.simple-page__hint {
  flex-shrink: 0;
  font-size: 13px;
  color: #8f959e;
  padding-bottom: 8px;
}

.simple-page__table {
  flex: 1 1 0;
  height: 0;
  min-height: 0;
  overflow: hidden;
}

.simple-page__pager {
  flex-shrink: 0;
  padding: 8px 0;
}
</style>
