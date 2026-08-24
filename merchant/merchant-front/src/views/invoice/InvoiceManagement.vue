<template>
  <div class="invoice-management">
    <t-tabs v-model="activeTab" theme="normal">
      <t-tab-panel value="sales" label="开票历史记录">
        <div class="tab-body">
          <div class="toolbar">
            <t-space break-line>
              <t-input
                  v-model="buyerName"
                  clearable
                  placeholder="请输入购方名称"
                  style="width:240px;border-radius:4px"
                  @enter="search"
              >
                <template #suffixIcon><t-icon name="search" style="cursor:pointer" @click="search"/></template>
              </t-input>
              <t-button theme="primary" variant="outline" style="border-radius:4px" :loading="loading" @click="search">查询</t-button>
              <t-button style="border-radius:4px" @click="loadSales">刷新</t-button>
            </t-space>
          </div>

          <div class="table-wrap">
            <t-table
                row-key="id"
                size="medium"
                bordered
                stripe
                hover
                height="100%"
                :data="list"
                :columns="columns"
                :loading="loading"
            >
              <template #status="{ row }">
                <t-tag :theme="row.invoiceType === 'RED' ? 'danger' : 'success'" variant="light">
                  {{ row.invoiceType === 'RED' ? '红冲' : '正常' }}
                </t-tag>
              </template>
              <template #issueDate="{ row }">{{ fmtDate(row.issueDate) }}</template>
              <template #buyerTaxNo="{ row }">{{ row.buyerTaxNo || '—' }}</template>
              <template #amount="{ row }">
                <span :style="row.invoiceType === 'RED' ? 'color:#e34d59' : ''">
                  {{ fmt(sub(row.totalAmount, row.totalTax)) }}
                </span>
              </template>
              <template #tax="{ row }">
                <span :style="row.invoiceType === 'RED' ? 'color:#e34d59' : ''">{{ fmt(row.totalTax) }}</span>
              </template>
              <template #totalAmount="{ row }">
                <span :style="row.invoiceType === 'RED' ? 'color:#e34d59' : ''">{{ fmt(row.totalAmount) }}</span>
              </template>
              <template #invoiceNo="{ row }">
                <span style="font-family:monospace">{{ row.thirdPartyNumber || row.invoiceNo }}</span>
              </template>
              <template #originalInvoiceNo="{ row }">
                <span v-if="row.originalInvoiceNo" style="font-family:monospace">{{ row.originalInvoiceNo }}</span>
                <span v-else>—</span>
              </template>
              <template #ops="{ row }">
                <t-space size="small">
                  <t-link v-if="row.invoiceType !== 'RED'" theme="warning" @click="doRed(row)">红冲</t-link>
                  <t-link theme="primary" @click="showDetail(row)">详情</t-link>
                  <t-link v-if="row.id" theme="primary" @click="openPdf(row)">下载</t-link>
                </t-space>
              </template>
            </t-table>
          </div>

          <div class="pager">
            <t-pagination
                v-model:current="page"
                v-model:page-size="pageSize"
                :total="total"
                :show-jumper="true"
                :show-page-size="true"
                @change="loadSales"
            />
          </div>
        </div>
      </t-tab-panel>

      <t-tab-panel value="draft" label="发票草稿">
        <div class="tab-body">
          <div class="table-wrap">
            <t-table row-key="_savedAt" size="medium" bordered stripe hover height="100%" :data="drafts" :columns="draftColumns">
              <template #totalAmount="{ row }">
                <span>¥{{ fmt(row._grandTotal) }}</span>
              </template>
              <template #status="{ row }">
                <t-tag theme="warning" variant="light">草稿</t-tag>
              </template>
              <template #ops="{ row }">
                <t-space size="small">
                  <t-link theme="primary" @click="editDraft(row)">编辑</t-link>
                  <t-link theme="danger" @click="removeDraft(row)">删除</t-link>
                </t-space>
              </template>
            </t-table>
          </div>
        </div>
      </t-tab-panel>
    </t-tabs>

    <!-- 详情 -->
    <t-dialog v-model:visible="detailVisible" header="发票详情" width="620px" :footer="false">
      <t-descriptions v-if="detailRow" :column="2" bordered :items="detailItems"/>
      <div class="dialog-footer">
        <t-button @click="detailVisible = false">关闭</t-button>
      </div>
    </t-dialog>

    <!-- 红冲 -->
    <t-dialog v-model:visible="redDialog.visible" header="红冲操作" width="520px" :footer="false" :close-on-overlay-click="false">
      <t-steps :current="redDialog.step" theme="default" class="red-steps">
        <t-step-item title="确认发票"/>
        <t-step-item title="申请红字信息表"/>
        <t-step-item title="开具红字发票"/>
      </t-steps>
      <div v-if="redDialog.row" class="red-info">
        <t-descriptions :column="2" bordered :items="redSummaryItems"/>
      </div>
      <t-form label-width="100px" :colon="false">
        <t-form-item label="红冲原因">
          <t-select v-model="redDialog.reason" :options="redReasonOptions"/>
        </t-form-item>
      </t-form>
      <div v-if="redDialog.infoNo" class="red-info-no">
        红字信息表编号：<strong>{{ redDialog.infoNo }}</strong>
      </div>
      <div class="dialog-footer">
        <t-button @click="redDialog.visible = false">取消</t-button>
        <t-button
            v-if="redDialog.step < 2"
            theme="warning"
            :loading="redDialog.loading"
            @click="doApplyRed"
        >申请红字信息表</t-button>
        <t-button
            v-if="redDialog.step >= 1"
            theme="danger"
            :loading="redDialog.loading"
            :disabled="!redDialog.infoNo"
            @click="doIssueRed"
        >确认开具红字发票</t-button>
      </div>
    </t-dialog>
  </div>
</template>

<script>
import Invoice from '@js/api/invoice/Invoice';
import {MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';

const DRAFT_KEY = 'invoice_drafts_v2';

export default {
  name: 'InvoiceManagement',
  data() {
    return {
      activeTab: 'sales',
      loading: false,
      buyerName: '',
      page: 1,
      pageSize: 20,
      total: 0,
      list: [],
      columns: [
        {colKey: 'status', title: '发票状态', width: 90, align: 'center', fixed: 'left'},
        {colKey: 'issueDate', title: '开票日期', width: 140},
        {colKey: 'buyerName', title: '购方名称', minWidth: 180, ellipsis: true},
        {colKey: 'buyerTaxNo', title: '购方税号', width: 150, ellipsis: true},
        {colKey: 'amount', title: '金额', width: 110, align: 'right'},
        {colKey: 'tax', title: '税额', width: 100, align: 'right'},
        {colKey: 'totalAmount', title: '价税合计', width: 110, align: 'right'},
        {colKey: 'invoiceNo', title: '发票号', width: 170},
        {colKey: 'originalInvoiceNo', title: '对应蓝字发票号码', width: 170, ellipsis: true},
        {colKey: 'ops', title: '操作', width: 140, align: 'center', fixed: 'right'}
      ],
      draftColumns: [
        {colKey: 'buyerName', title: '购方名称', minWidth: 200, ellipsis: true},
        {colKey: 'totalAmount', title: '价税合计', width: 140, align: 'right'},
        {colKey: 'status', title: '状态', width: 90, align: 'center'},
        {colKey: '_savedAt', title: '保存时间', width: 170},
        {colKey: 'ops', title: '操作', width: 120, align: 'center', fixed: 'right'}
      ],
      drafts: [],
      detailVisible: false,
      detailRow: null,
      redDialog: {
        visible: false,
        loading: false,
        step: 0,
        row: null,
        reason: '01',
        infoNo: ''
      },
      redReasonOptions: [
        {label: '开票有误', value: '01'},
        {label: '销货退回', value: '02'},
        {label: '服务中止', value: '03'},
        {label: '折让', value: '04'}
      ]
    };
  },
  computed: {
    detailItems() {
      const r = this.detailRow;
      if (!r) return [];
      return [
        {label: '发票ID', content: r.id},
        {label: '类型', content: r.invoiceType === 'RED' ? '红字发票' : '蓝字发票'},
        {label: '发票号码', content: r.thirdPartyNumber || r.invoiceNo},
        {label: '发票代码', content: r.thirdPartyCode || '—'},
        {label: '购方名称', content: r.buyerName},
        {label: '购方税号', content: r.buyerTaxNo || '—'},
        {label: '金额', content: `¥${this.fmt(this.sub(r.totalAmount, r.totalTax))}`},
        {label: '税额', content: `¥${this.fmt(r.totalTax)}`},
        {label: '状态', content: r.status},
        {label: '开票日期', content: this.fmtDate(r.issueDate)}
      ];
    },
    redSummaryItems() {
      const r = this.redDialog.row;
      if (!r) return [];
      return [
        {label: '发票号码', content: r.thirdPartyNumber || r.invoiceNo},
        {label: '购方', content: r.buyerName},
        {label: '金额', content: `¥${this.fmt(r.totalAmount)}`},
        {label: '税额', content: `¥${this.fmt(r.totalTax)}`}
      ];
    }
  },
  methods: {
    search() {
      this.page = 1;
      this.loadSales();
    },
    loadSales() {
      this.loading = true;
      Invoice.search({
        buyerName: this.buyerName || null,
        page: this.page - 1,
        size: this.pageSize
      })
        .then(({data}) => {
          this.list = (data && data.content) || [];
          this.total = (data && data.total) || 0;
        })
        .catch(() => {
          this.list = [];
          this.total = 0;
        })
        .finally(() => (this.loading = false));
    },
    loadDrafts() {
      try {
        this.drafts = JSON.parse(localStorage.getItem(DRAFT_KEY) || '[]');
      } catch (e) {
        this.drafts = [];
      }
    },
    showDetail(row) {
      this.detailRow = row;
      this.detailVisible = true;
    },
    openPdf(row) {
      if (row.id) window.open(Invoice.getPdfUrl(row.id), '_blank');
    },
    editDraft(row) {
      const drafts = this.drafts;
      const idx = drafts.findIndex((d) => d._savedAt === row._savedAt);
      if (idx >= 0) {
        localStorage.setItem('_loadDraftIndex', String(idx));
        this.$store.commit('pushTab', {key: 'InvoiceIssue', title: '蓝字发票开具'});
      }
    },
    removeDraft(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: '确认删除该草稿？',
        onConfirm: () => {
          const idx = this.drafts.findIndex((d) => d._savedAt === row._savedAt);
          if (idx >= 0) {
            this.drafts.splice(idx, 1);
            localStorage.setItem(DRAFT_KEY, JSON.stringify(this.drafts));
            MessagePlugin.success('已删除');
          }
        }
      });
    },
    doRed(row) {
      this.redDialog.row = row;
      this.redDialog.step = 0;
      this.redDialog.reason = '01';
      this.redDialog.infoNo = '';
      this.redDialog.loading = false;
      this.redDialog.visible = true;
    },
    doApplyRed() {
      const row = this.redDialog.row;
      if (!row) return;
      this.redDialog.loading = true;
      Invoice.applyRed({
        originalInvoiceNo: row.thirdPartyNumber || row.invoiceNo,
        originalInvoiceDate: this.toFullDate(row.issueDate),
        redReason: this.redDialog.reason
      })
        .then(({data}) => {
          this.redDialog.infoNo = data && data.redInfoNo;
          this.redDialog.step = 1;
          MessagePlugin.success('红字信息表已生成: ' + (this.redDialog.infoNo || ''));
        })
        .finally(() => (this.redDialog.loading = false));
    },
    doIssueRed() {
      const row = this.redDialog.row;
      if (!row || !this.redDialog.infoNo) return;
      this.redDialog.loading = true;
      Invoice.issueRed({
        redInfoNo: this.redDialog.infoNo,
        originalInvoiceNo: row.thirdPartyNumber || row.invoiceNo,
        originalInvoiceDate: this.toFullDate(row.issueDate)
      })
        .then(() => {
          MessagePlugin.success('红字发票开具成功');
          this.redDialog.visible = false;
          this.loadSales();
        })
        .finally(() => (this.redDialog.loading = false));
    },
    fmt(v) {
      return v === undefined || v === null || v === '' ? '0.00' : Number(v).toFixed(2);
    },
    sub(a, b) {
      return (Number(a) || 0) - (Number(b) || 0);
    },
    fmtDate(v) {
      if (!v) return '—';
      const s = String(v).replace('T', ' ');
      return s.length > 16 ? s.substring(0, 16) : s;
    },
    toFullDate(v) {
      if (!v) {
        const d = new Date();
        const p = (n) => String(n).padStart(2, '0');
        return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
      }
      let s = String(v).replace('T', ' ');
      if (s.length === 16) s += ':00';
      if (s.length > 19) s = s.substring(0, 19);
      return s;
    }
  },
  created() {
    this.loadSales();
    this.loadDrafts();
  }
};
</script>

<style scoped>
.invoice-management {
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

.table-wrap {
  flex: 1;
  height: 0;
  min-height: 0;
  overflow: auto;
}

.pager {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding: 10px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

.red-steps {
  margin-bottom: 16px;
}

.red-info {
  margin-bottom: 12px;
}

.red-info-no {
  padding: 8px 12px;
  background: #f0f9eb;
  border-radius: 6px;
  font-size: 13px;
  margin-bottom: 4px;
}
</style>
