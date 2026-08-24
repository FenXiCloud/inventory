<template>
  <div class="invoice-aggregation">
    <t-tabs v-model="activeTab" theme="normal">
      <t-tab-panel value="output" label="销项归集">
        <div class="tab-body">
          <div class="toolbar">
            <t-space break-line>
              <span class="page-title">销项发票按月归集</span>
              <t-button theme="primary" variant="outline" :loading="outputLoading" @click="loadOutput">刷新</t-button>
              <t-button theme="primary" variant="outline" @click="exportOutput">导 出</t-button>
            </t-space>
          </div>
          <div class="hint">蓝字发票计入正数，红字发票冲减对应月份的金额与税额。</div>
          <div class="table-wrap">
            <t-table
                row-key="month"
                size="medium"
                bordered
                hover
                height="100%"
                :data="outputList"
                :columns="outputColumns"
                :loading="outputLoading"
                :foot-data="outputFoot"
            />
          </div>
        </div>
      </t-tab-panel>

      <t-tab-panel value="input" label="进项归集">
        <div class="tab-body">
          <div class="toolbar">
            <t-space break-line>
              <span class="page-title">进项发票按月归集</span>
              <t-button theme="primary" :loading="inputLoading" @click="loadInput">刷新</t-button>
              <t-button theme="primary" variant="outline" @click="openForm()">录入进项发票</t-button>
              <t-button theme="primary" variant="outline" @click="exportInput">导 出</t-button>
            </t-space>
          </div>
          <div class="hint">统计供应商开给本商户的采购发票，按开票月份汇总。</div>
          <div class="agg-table">
            <t-table
                row-key="month"
                size="medium"
                bordered
                hover
                height="100%"
                :data="inputList"
                :columns="inputColumns"
                :loading="inputLoading"
                :foot-data="inputFoot"
            />
          </div>
          <div class="detail-title">进项发票明细</div>
          <div class="detail-table">
            <t-table
                row-key="id"
                size="medium"
                bordered
                stripe
                hover
                height="100%"
                :data="inputDetailList"
                :columns="detailColumns"
                :loading="inputLoading"
            >
              <template #issueDate="{ row }">{{ row.issueDate || '—' }}</template>
              <template #amount="{ row }">{{ fmt(row.amount) }}</template>
              <template #tax="{ row }">{{ fmt(row.tax) }}</template>
              <template #totalAmount="{ row }">{{ fmt(row.totalAmount) }}</template>
              <template #ops="{ row }">
                <t-space size="small">
                  <t-link theme="primary" @click="openForm(row)">编辑</t-link>
                  <t-link theme="danger" @click="doDelete(row)">删除</t-link>
                </t-space>
              </template>
            </t-table>
          </div>
        </div>
      </t-tab-panel>
    </t-tabs>

    <t-dialog v-model:visible="formVisible" :header="form.id ? '编辑进项发票' : '录入进项发票'" width="560px" :footer="false">
      <t-form label-width="110px" :colon="false">
        <t-form-item label="供应商名称">
          <t-input v-model="form.supplierName" placeholder="请输入供应商名称"/>
        </t-form-item>
        <t-form-item label="供应商税号">
          <t-input v-model="form.supplierTaxNo" placeholder="请输入供应商税号"/>
        </t-form-item>
        <t-form-item label="发票号码">
          <t-input v-model="form.invoiceNo" placeholder="请输入发票号码"/>
        </t-form-item>
        <t-form-item label="发票代码">
          <t-input v-model="form.invoiceCode" placeholder="请输入发票代码"/>
        </t-form-item>
        <t-form-item label="开票日期">
          <t-date-picker v-model="form.issueDate" mode="date" :clearable="false" style="width:100%"/>
        </t-form-item>
        <t-form-item label="金额（不含税）">
          <t-input-number v-model="form.amount" theme="normal" :min="0" :decimal-places="2" style="width:100%"/>
        </t-form-item>
        <t-form-item label="税额">
          <t-input-number v-model="form.tax" theme="normal" :min="0" :decimal-places="2" style="width:100%"/>
        </t-form-item>
        <t-form-item label="价税合计">
          <t-input-number v-model="form.totalAmount" theme="normal" :min="0" :decimal-places="2" style="width:100%"/>
        </t-form-item>
        <t-form-item label="备注">
          <t-textarea v-model="form.remark" placeholder="备注" :autosize="{minRows: 2, maxRows: 4}"/>
        </t-form-item>
      </t-form>
      <div class="dialog-footer">
        <t-button @click="formVisible = false">取消</t-button>
        <t-button theme="primary" :loading="formLoading" @click="saveForm">保存</t-button>
      </div>
    </t-dialog>
  </div>
</template>

<script>
import Invoice from '@js/api/invoice/Invoice';
import {MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import {export_json_to_excel} from '@js/excel/export2Excel';

export default {
  name: 'InvoiceAggregation',
  data() {
    return {
      activeTab: 'output',
      outputLoading: false,
      inputLoading: false,
      outputList: [],
      inputList: [],
      inputDetailList: [],
      outputColumns: [
        {colKey: 'month', title: '月份', width: 120, align: 'center'},
        {colKey: 'blueCount', title: '蓝字张数', width: 110, align: 'right'},
        {colKey: 'redCount', title: '红字张数', width: 110, align: 'right'},
        {colKey: 'amount', title: '金额（不含税）', width: 140, align: 'right'},
        {colKey: 'tax', title: '税额', width: 120, align: 'right'},
        {colKey: 'totalAmount', title: '价税合计', width: 140, align: 'right'}
      ],
      inputColumns: [
        {colKey: 'month', title: '月份', width: 120, align: 'center'},
        {colKey: 'invoiceCount', title: '发票张数', width: 110, align: 'right'},
        {colKey: 'amount', title: '金额（不含税）', width: 140, align: 'right'},
        {colKey: 'tax', title: '税额', width: 120, align: 'right'},
        {colKey: 'totalAmount', title: '价税合计', width: 140, align: 'right'}
      ],
      detailColumns: [
        {colKey: 'issueDate', title: '开票日期', width: 120},
        {colKey: 'supplierName', title: '供应商', minWidth: 160, ellipsis: true},
        {colKey: 'invoiceNo', title: '发票号码', width: 160},
        {colKey: 'amount', title: '金额', width: 110, align: 'right'},
        {colKey: 'tax', title: '税额', width: 100, align: 'right'},
        {colKey: 'totalAmount', title: '价税合计', width: 110, align: 'right'},
        {colKey: 'ops', title: '操作', width: 100, align: 'center', fixed: 'right'}
      ],
      formVisible: false,
      formLoading: false,
      form: this.emptyForm()
    };
  },
  computed: {
    outputFoot() {
      return this.footOf(this.outputList, ['blueCount', 'redCount', 'amount', 'tax', 'totalAmount']);
    },
    inputFoot() {
      return this.footOf(this.inputList, ['invoiceCount', 'amount', 'tax', 'totalAmount']);
    }
  },
  methods: {
    emptyForm() {
      return {
        id: null,
        supplierName: '',
        supplierTaxNo: '',
        invoiceNo: '',
        invoiceCode: '',
        issueDate: '',
        amount: 0,
        tax: 0,
        totalAmount: 0,
        remark: ''
      };
    },
    footOf(list, numericKeys) {
      if (!list || !list.length) return [];
      const foot = {month: '合计'};
      let has = false;
      numericKeys.forEach((k) => {
        const sum = list.reduce((acc, row) => acc + (Number(row[k]) || 0), 0);
        foot[k] = Number(sum.toFixed(2));
        has = true;
      });
      return has ? [foot] : [];
    },
    loadOutput() {
      this.outputLoading = true;
      Invoice.outputAggregation()
        .then(({data}) => { this.outputList = data || []; })
        .catch(() => { this.outputList = []; })
        .finally(() => (this.outputLoading = false));
    },
    loadInput() {
      this.inputLoading = true;
      Invoice.inputAggregation()
        .then(({data}) => { this.inputList = data || []; })
        .catch(() => { this.inputList = []; })
        .finally(() => (this.inputLoading = false));
      this.loadDetail();
    },
    loadDetail() {
      Invoice.inputList({page: 0, size: 500})
        .then(({data}) => { this.inputDetailList = (data && data.content) || []; })
        .catch(() => { this.inputDetailList = []; });
    },
    openForm(row) {
      this.form = row ? {...this.emptyForm(), ...row} : this.emptyForm();
      this.formVisible = true;
    },
    saveForm() {
      this.formLoading = true;
      Invoice.inputSave(this.form)
        .then(() => {
          MessagePlugin.success('已保存');
          this.formVisible = false;
          this.loadInput();
        })
        .finally(() => (this.formLoading = false));
    },
    doDelete(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: '确认删除该进项发票？',
        onConfirm: () => {
          Invoice.inputDelete(row.id)
            .then(() => {
              MessagePlugin.success('已删除');
              this.loadInput();
            });
        }
      });
    },
    exportOutput() {
      this.exportList(this.outputList, this.outputColumns, '销项归集');
    },
    exportInput() {
      this.exportList(this.inputList, this.inputColumns, '进项归集');
    },
    exportList(list, columns, filename) {
      if (!list.length) {
        MessagePlugin.warning('暂无数据～');
        return;
      }
      const header = columns.map((c) => c.title);
      const data = list.map((row) => columns.map((c) => row[c.colKey] ?? ''));
      export_json_to_excel({header, data, filename, autoWidth: true, bookType: 'xlsx'});
    },
    fmt(v) {
      return v === undefined || v === null || v === '' ? '0.00' : Number(v).toFixed(2);
    }
  },
  created() {
    this.loadOutput();
    this.loadInput();
  }
};
</script>

<style scoped>
.invoice-aggregation {
  height: 100%;
  min-height: 0;
  background: #fff;
  border-radius: 4px;
  padding: 0 12px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tab-body {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 160px);
  padding-top: 8px;
}

.toolbar {
  flex-shrink: 0;
  padding-bottom: 8px;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-right: 8px;
}

.hint {
  flex-shrink: 0;
  margin-bottom: 8px;
  padding: 6px 12px;
  border-radius: 4px;
  background: #f3f3f3;
  color: #555;
  font-size: 13px;
}

.table-wrap {
  flex: 1;
  height: 0;
  min-height: 0;
  overflow: auto;
}

.agg-table {
  height: 40%;
  min-height: 0;
  overflow: auto;
}

.detail-title {
  flex-shrink: 0;
  margin: 10px 0 6px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.detail-table {
  flex: 1;
  height: 0;
  min-height: 0;
  overflow: auto;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}
</style>
