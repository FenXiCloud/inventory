<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <div v-if="selectedSourceId && selectedSourceType === 'SALES_OUTBOUND'" class="source-hint">
        本发票由销售出库单生成，开票成功后将自动回写该单据的开票状态。
      </div>
      <div class="outbound-select-bar">
        <label>选择销售出库单：</label>
        <t-select
            v-model="selectedOutboundId"
            :options="outboundList"
            :keys="{ value: 'id', label: 'orderNo' }"
            filterable
            clearable
            placeholder="选择已审核未开票的销售出库单自动带出明细"
            style="width: 440px"
            @change="onOutboundChange"
        />
      </div>
    <!-- 购买方 / 销售方 -->
    <div class="party-row">
      <div class="party-card">
        <div class="card-title"><span class="bar bar-buyer"/>购买方</div>
        <div class="card-body">
          <div class="buyer-type-row">
            <t-radio-group v-model="buyerType" variant="default-filled">
              <t-radio-button value="enterprise">企业</t-radio-button>
              <t-radio-button value="personal">个人/其他</t-radio-button>
            </t-radio-group>
          </div>
          <t-form label-width="80px" :colon="false">
            <t-form-item label="选择客户">
              <t-select
                  v-model="customerId"
                  :options="customerList"
                  :keys="{ value: 'id', label: 'name' }"
                  filterable
                  clearable
                  placeholder="选择客户自动带出开票信息"
                  @change="onCustomerChange"
              />
            </t-form-item>
            <t-form-item label="发票抬头">
              <t-input v-model="form.buyerName" placeholder="请输入购买方名称"/>
            </t-form-item>
            <t-form-item v-if="buyerType === 'enterprise'" label="企业税号">
              <t-input v-model="form.buyerTaxNo" placeholder="请输入企业税号" maxlength="18"/>
            </t-form-item>
            <t-form-item label="地址">
              <t-input v-model="form.buyerAddress"/>
            </t-form-item>
            <t-form-item label="电话">
              <t-input v-model="form.buyerPhone"/>
            </t-form-item>
            <t-form-item label="开户行">
              <t-input v-model="form.buyerBank"/>
            </t-form-item>
          </t-form>
        </div>
      </div>

      <div class="party-card">
        <div class="card-title"><span class="bar bar-seller"/>销售方</div>
        <div class="card-body">
          <div class="quota-row">
            <span>授信总额度：<strong>¥{{ fmt(stats.totalQuota) }}</strong></span>
            <span>可用总额度：<strong class="green">¥{{ fmt(stats.remainingQuota) }}</strong></span>
            <t-button size="small" variant="outline" :loading="statsLoading" @click="loadStats">更新额度</t-button>
          </div>
          <div class="quota-row">
            <span>已开票份数：<strong>{{ stats.invoiceCount || 0 }}</strong></span>
          </div>
          <div class="seller-hint">销售方信息由系统配置维护（名称 / 税号 / 开户行）。</div>
        </div>
      </div>
    </div>

    <!-- 开票项目明细 -->
    <div class="party-card items-card">
      <div class="card-title"><span class="bar bar-items"/>开票项目明细</div>
      <div class="card-body">
        <div class="items-toolbar">
          <t-space>
            <t-button size="small" theme="primary" @click="addItem">添加行</t-button>
          </t-space>
        </div>

        <t-table
            row-key="_rid"
            size="small"
            bordered
            :data="form.items"
            :columns="itemColumns"
        >
          <template #goodsName="{ row }">
            <t-input v-model="row.goodsName" size="small" placeholder="商品名称"/>
          </template>
          <template #spec="{ row }">
            <t-input v-model="row.spec" size="small" placeholder="规格型号"/>
          </template>
          <template #unit="{ row }">
            <t-input v-model="row.unit" size="small" placeholder="单位"/>
          </template>
          <template #quantity="{ row }">
            <t-input-number v-model="row.quantity" size="small" :min="0" :decimal-places="4" theme="normal" style="width:100%"/>
          </template>
          <template #unitPrice="{ row }">
            <t-input-number v-model="row.unitPrice" size="small" :min="0" :decimal-places="2" theme="normal" style="width:100%"/>
          </template>
          <template #taxRate="{ row }">
            <t-select v-model="row.taxRate" size="small" :options="taxRateOptions"/>
          </template>
          <template #amount="{ row }">
            <span class="amount-cell">¥{{ fmt(lineTotal(row)) }}</span>
          </template>
          <template #ops="{ row }">
            <t-link theme="danger" :disabled="form.items.length <= 1" @click="removeItem(row)">
              <t-icon name="delete"/>
            </t-link>
          </template>
        </t-table>

        <div class="total-bar">
          <span>合计（含税）：<strong class="total-price">¥{{ fmt(grandTotal) }}</strong></span>
          <span>合计税额：<strong>¥{{ fmt(totalTax) }}</strong></span>
        </div>
      </div>
    </div>

    <!-- 备注信息 -->
    <div class="party-card remark-card">
      <div class="card-body">
        <t-form label-width="80px" :colon="false">
          <div class="remark-grid">
            <t-form-item label="备注">
              <t-textarea v-model="form.remark" placeholder="请输入" :maxlength="200"/>
            </t-form-item>
            <t-form-item label="收款人">
              <t-input v-model="form.payee"/>
            </t-form-item>
            <t-form-item label="复核人">
              <t-input v-model="form.reviewer"/>
            </t-form-item>
          </div>
        </t-form>
      </div>
    </div>

    <!-- 底部操作 -->
    <div class="bottom-actions">
      <t-button @click="saveDraft">保存草稿</t-button>
      <t-button variant="outline" @click="previewVisible = true">预览发票</t-button>
      <t-button theme="primary" :loading="submitting" @click="handleIssue">开 票</t-button>
    </div>

    <!-- 预览 -->
    <t-dialog v-model:visible="previewVisible" header="发票预览" width="720px" :footer="false">
      <div class="preview-invoice">
        <div class="preview-head"><h3>电子发票（普通发票）</h3></div>
        <div class="preview-row"><span>购买方名称：</span><strong>{{ form.buyerName || '—' }}</strong></div>
        <div class="preview-row" v-if="form.buyerTaxNo"><span>购买方税号：</span><strong>{{ form.buyerTaxNo }}</strong></div>
        <table class="preview-table">
          <thead><tr><th>商品名称</th><th>规格</th><th>单位</th><th>数量</th><th>单价</th><th>税率</th><th>金额</th></tr></thead>
          <tbody>
            <tr v-for="(item, i) in validItems" :key="i">
              <td>{{ item.goodsName }}</td>
              <td>{{ item.spec || '—' }}</td>
              <td>{{ item.unit || '—' }}</td>
              <td>{{ item.quantity }}</td>
              <td>{{ fmt(item.unitPrice) }}</td>
              <td>{{ rateLabel(item.taxRate) }}</td>
              <td class="right">{{ fmt(lineTotal(item)) }}</td>
            </tr>
          </tbody>
        </table>
        <div class="preview-total">
          <div>价税合计：<strong>¥{{ fmt(grandTotal) }}</strong></div>
          <div>大写：<strong>{{ numberToChinese(grandTotal) }}</strong></div>
        </div>
        <div class="preview-footer">
          <span>收款人：{{ form.payee || '—' }}</span>
          <span>复核人：{{ form.reviewer || '—' }}</span>
          <span>开票人：系统</span>
        </div>
      </div>
      <div class="dialog-footer">
        <t-button @click="previewVisible = false">关闭</t-button>
        <t-button theme="primary" @click="previewVisible = false; handleIssue()">确认开票</t-button>
      </div>
    </t-dialog>

    <!-- 开票成功 -->
    <t-dialog v-model:visible="successVisible" header="开票成功" width="480px" :footer="false">
      <div class="success-detail">
        <div class="info-row" v-if="successResult.id"><span>发票ID</span><strong>{{ successResult.id }}</strong></div>
        <div class="info-row" v-if="successResult.thirdPartyNumber"><span>发票号码</span><strong>{{ successResult.thirdPartyNumber }}</strong></div>
        <div class="info-row" v-if="successResult.thirdPartyCode"><span>发票代码</span><strong>{{ successResult.thirdPartyCode }}</strong></div>
        <div class="info-row" v-if="successResult.totalAmount"><span>价税合计</span><strong class="price">¥{{ fmt(successResult.totalAmount) }}</strong></div>
      </div>
      <div class="dialog-footer">
        <t-button @click="successVisible = false">关闭</t-button>
        <t-button v-if="successResult.id" theme="primary" @click="downloadPdf">下载PDF</t-button>
      </div>
    </t-dialog>
    </div>
  </div>
</template>

<script>
import Invoice from '@js/api/invoice/Invoice';
import Auth from '@js/api/invoice/Auth';
import SalesOutbound from '@js/api/sales/SalesOutbound';
import Customer from '@js/api/basic/Customer';
import {MessagePlugin} from 'tdesign-vue-next';

const DRAFT_KEY = 'invoice_drafts_v2';
let ridSeed = 1;

export default {
  name: 'InvoiceIssue',
  props: {
    sourceType: { type: String, default: null },
    sourceId: { type: [Number, String], default: null }
  },
  data() {
    return {
      buyerType: 'enterprise',
      customerId: null,
      customerList: [],
      outboundList: [],
      selectedOutboundId: null,
      selectedSourceId: null,
      selectedSourceType: null,
      submitting: false,
      statsLoading: false,
      previewVisible: false,
      successVisible: false,
      successResult: {},
      stats: {},
      form: {
        buyerName: '',
        buyerTaxNo: '',
        buyerAddress: '',
        buyerPhone: '',
        buyerBank: '',
        remark: '',
        payee: '',
        reviewer: '',
        items: [this.makeItem()]
      },
      taxRateOptions: [
        {label: '0%', value: 0},
        {label: '1%', value: 0.01},
        {label: '3%', value: 0.03},
        {label: '6%', value: 0.06},
        {label: '9%', value: 0.09},
        {label: '13%', value: 0.13}
      ],
      itemColumns: [
        {colKey: 'goodsName', title: '商品名称', minWidth: 200},
        {colKey: 'spec', title: '规格型号', width: 130},
        {colKey: 'unit', title: '单位', width: 90},
        {colKey: 'quantity', title: '数量', width: 110},
        {colKey: 'unitPrice', title: '单价(含税)', width: 130},
        {colKey: 'taxRate', title: '税率', width: 110},
        {colKey: 'amount', title: '金额(含税)', width: 130, align: 'right'},
        {colKey: 'ops', title: '操作', width: 70, align: 'center'}
      ]
    };
  },
  computed: {
    validItems() {
      return this.form.items.filter((i) => i.goodsName && String(i.goodsName).trim());
    },
    grandTotal() {
      return this.validItems.reduce((s, i) => s + this.lineTotal(i), 0);
    },
    totalTax() {
      return this.validItems.reduce((s, i) => {
        const line = this.lineTotal(i);
        const r = Number(i.taxRate) || 0;
        return s + (line / (1 + r)) * r;
      }, 0);
    }
  },
  methods: {
    makeItem() {
      return {
        _rid: ridSeed++,
        goodsName: '',
        spec: '',
        unit: '',
        quantity: 1,
        unitPrice: 0,
        taxRate: 0.06
      };
    },
    onCustomerChange(value) {
      if (value == null || value === '') return;
      const c = (this.customerList || []).find((i) => String(i.id) === String(value));
      if (!c) return;
      this.form.buyerName = c.name || '';
      if (c.taxNo) {
        this.form.buyerTaxNo = c.taxNo;
        this.buyerType = 'enterprise';
      }
      this.form.buyerPhone = c.phone || '';
    },
    addItem() {
      this.form.items.push(this.makeItem());
    },
    removeItem(row) {
      if (this.form.items.length <= 1) return;
      const idx = this.form.items.findIndex((i) => i._rid === row._rid);
      if (idx >= 0) this.form.items.splice(idx, 1);
    },
    lineTotal(item) {
      return (Number(item.quantity) || 0) * (Number(item.unitPrice) || 0);
    },
    rateLabel(r) {
      return `${Math.round((Number(r) || 0) * 100)}%`;
    },
    fmt(v) {
      return v === undefined || v === null || v === '' ? '0.00' : Number(v).toFixed(2);
    },
    loadSource() {
      if (!this.selectedSourceId || this.selectedSourceType !== 'SALES_OUTBOUND') return;
      SalesOutbound.prefillInvoice(this.selectedSourceId)
        .then(({data}) => {
          if (!data) return;
          this.form.buyerName = data.buyerName || '';
          this.form.buyerTaxNo = data.buyerTaxNo || '';
          this.buyerType = data.buyerTaxNo ? 'enterprise' : 'personal';
          this.form.items = (data.items && data.items.length)
            ? data.items.map((i) => ({
                ...this.makeItem(),
                goodsName: i.goodsName || '',
                quantity: i.quantity,
                unitPrice: i.unitPrice,
                taxRate: i.taxRate != null ? i.taxRate : 0.06
              }))
            : [this.makeItem()];
        })
        .catch(() => {});
    },
    loadOutboundList() {
      SalesOutbound.list({ state: '已审核', page: 1, pageSize: 1000 })
        .then(({ data }) => {
          const list = data?.results || [];
          this.outboundList = list
            .filter((r) => r.invoiceStatus !== '已开票')
            .map((r) => ({
              id: r.id,
              orderNo: `${r.orderNo}${r.customerName ? ' - ' + r.customerName : ''}`
            }));
        })
        .catch(() => {});
    },
    onOutboundChange(value) {
      if (value == null || value === '') {
        this.selectedSourceId = null;
        this.selectedSourceType = null;
        return;
      }
      this.selectedSourceId = value;
      this.selectedSourceType = 'SALES_OUTBOUND';
      this.loadSource();
    },
    loadStats() {
      this.statsLoading = true;
      Invoice.stats()
        .then(({data}) => {
          this.stats = data || {};
        })
        .catch(() => {
          this.stats = {};
        })
        .finally(() => (this.statsLoading = false));
    },
    saveDraft() {
      const draft = {
        buyerName: this.form.buyerName,
        buyerTaxNo: this.form.buyerTaxNo,
        buyerAddress: this.form.buyerAddress,
        buyerPhone: this.form.buyerPhone,
        buyerBank: this.form.buyerBank,
        remark: this.form.remark,
        payee: this.form.payee,
        reviewer: this.form.reviewer,
        buyerType: this.buyerType,
        items: JSON.parse(JSON.stringify(this.form.items)),
        _grandTotal: this.grandTotal,
        _savedAt: new Date().toLocaleString()
      };
      const drafts = this.loadDrafts();
      drafts.unshift(draft);
      if (drafts.length > 20) drafts.length = 20;
      localStorage.setItem(DRAFT_KEY, JSON.stringify(drafts));
      MessagePlugin.success('草稿已保存');
    },
    loadDrafts() {
      try {
        return JSON.parse(localStorage.getItem(DRAFT_KEY) || '[]');
      } catch (e) {
        return [];
      }
    },
    loadDraftIndex() {
      const raw = localStorage.getItem('_loadDraftIndex');
      if (raw === null) return;
      localStorage.removeItem('_loadDraftIndex');
      const idx = Number(raw);
      const drafts = this.loadDrafts();
      const d = drafts[idx];
      if (!d) return;
      this.form.buyerName = d.buyerName || '';
      this.form.buyerTaxNo = d.buyerTaxNo || '';
      this.form.buyerAddress = d.buyerAddress || '';
      this.form.buyerPhone = d.buyerPhone || '';
      this.form.buyerBank = d.buyerBank || '';
      this.form.remark = d.remark || '';
      this.form.payee = d.payee || '';
      this.form.reviewer = d.reviewer || '';
      this.buyerType = d.buyerType || 'enterprise';
      this.form.items = (d.items && d.items.length)
        ? d.items.map((i) => ({...this.makeItem(), ...i}))
        : [this.makeItem()];
      MessagePlugin.success('草稿已加载');
    },
    async handleIssue() {
      if (!this.form.buyerName.trim()) {
        MessagePlugin.warning('请输入购买方名称');
        return;
      }
      if (this.buyerType === 'enterprise' && !this.form.buyerTaxNo.trim()) {
        MessagePlugin.warning('请输入企业税号');
        return;
      }
      if (this.buyerType === 'enterprise' && this.form.buyerTaxNo.trim() && !/^[0-9A-Z]{18}$/.test(this.form.buyerTaxNo.trim())) {
        MessagePlugin.warning('企业税号格式不正确（应为 18 位统一社会信用代码）');
        return;
      }
      if (!this.validItems.length) {
        MessagePlugin.warning('请至少填写一条商品明细');
        return;
      }

      // 校验是否已登录数电账户
      try {
        const {data: st} = await Auth.state();
        if (!st || !st.loggedIn) {
          this.saveDraft();
          MessagePlugin.warning('尚未完成登录认证，已保存草稿。请到「电子税务局账号」页面完成登录与扫码认证。');
          return;
        }
      } catch (e) {
        this.saveDraft();
        MessagePlugin.warning('无法确认登录状态，已保存草稿');
        return;
      }

      this.submitting = true;
      const payload = {
        sourceId: this.selectedSourceId ? Number(this.selectedSourceId) : undefined,
        sourceType: this.selectedSourceType || undefined,
        buyerName: this.form.buyerName.trim(),
        buyerTaxNo: this.buyerType === 'enterprise' ? this.form.buyerTaxNo.trim() : undefined,
        remark: this.form.remark ? this.form.remark.trim() : undefined,
        payee: this.form.payee ? this.form.payee.trim() : undefined,
        reviewer: this.form.reviewer ? this.form.reviewer.trim() : undefined,
        items: this.validItems.map((i) => ({
          goodsName: String(i.goodsName).trim(),
          quantity: Number(i.quantity),
          unitPrice: Number(i.unitPrice),
          taxRate: Number(i.taxRate)
        }))
      };
      Invoice.issue(payload)
        .then(({data}) => {
          this.successResult = data || {};
          this.successVisible = true;
          this.loadStats();
        })
        .finally(() => (this.submitting = false));
    },
    downloadPdf() {
      if (this.successResult.id) {
        window.open(Invoice.getPdfUrl(this.successResult.id), '_blank');
      }
    },
    numberToChinese(n) {
      const DIGITS = '零壹贰叁肆伍陆柒捌玖';
      const UNITS = ['', '拾', '佰', '仟', '万', '拾', '佰', '仟', '亿'];
      if (n === undefined || n === null || n === 0) return '零元整';
      const [intPart, decPart] = Number(n).toFixed(2).split('.');
      let result = '';
      const intStr = parseInt(intPart, 10).toString();
      if (intStr === '0') {
        result = '零';
      } else {
        let zero = false;
        for (let i = 0; i < intStr.length; i++) {
          const d = parseInt(intStr[i], 10);
          const u = intStr.length - 1 - i;
          const ui = u % 8;
          if (d === 0) {
            zero = true;
          } else {
            if (zero) {
              result += '零';
              zero = false;
            }
            result += DIGITS[d] + (UNITS[ui] || '');
          }
          if (ui === 4 && u >= 4) result += '万';
          if (ui === 0 && u >= 8) result += '亿';
        }
      }
      result += '元';
      const jiao = parseInt(decPart[0], 10) || 0;
      const fen = parseInt(decPart[1], 10) || 0;
      if (jiao === 0 && fen === 0) {
        result += '整';
      } else {
        if (jiao > 0) result += DIGITS[jiao] + '角';
        if (fen > 0) result += DIGITS[fen] + '分';
      }
      return result;
    }
  },
  created() {
    this.selectedSourceId = this.sourceId;
    this.selectedSourceType = this.sourceType;
    this.loadStats();
    this.loadOutboundList();
    Customer.select().then(({data}) => {
      this.customerList = data || [];
    }).catch(() => {});
    if (this.selectedSourceId && this.selectedSourceType === 'SALES_OUTBOUND') {
      this.loadSource();
    } else {
      this.loadDraftIndex();
    }
  }
};
</script>

<style scoped>
.source-hint {
  margin-bottom: 12px;
  padding: 8px 12px;
  border-radius: 4px;
  background: #e8f3ff;
  color: #0052d9;
  font-size: 13px;
  line-height: 1.6;
}

.outbound-select-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px 16px;
  background: #fff;
  border: 1px solid var(--td-component-border, #dcdcdc);
  border-radius: 6px;
  font-size: 13px;
  color: #333;
}

.party-row {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.party-card {
  flex: 1;
  background: #fff;
  border: 1px solid var(--td-component-border, #dcdcdc);
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 12px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  padding: 10px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.bar {
  width: 4px;
  height: 16px;
  border-radius: 2px;
}

.bar-buyer {
  background: #0052d9;
}

.bar-seller {
  background: #e37318;
}

.bar-items {
  background: #333;
}

.card-body {
  padding: 16px;
}

.buyer-type-row {
  margin-bottom: 8px;
}

.quota-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 13px;
  color: #666;
  margin-bottom: 10px;
}

.quota-row strong {
  font-size: 16px;
  color: #333;
}

.quota-row strong.green {
  color: #2ba471;
}

.seller-hint {
  font-size: 12px;
  color: #909399;
}

.items-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.amount-cell {
  font-family: monospace;
  font-weight: 600;
}

.total-bar {
  display: flex;
  justify-content: flex-end;
  gap: 24px;
  margin-top: 10px;
  font-size: 13px;
  color: #666;
}

.total-price {
  color: #e37318;
  font-size: 16px;
}

.remark-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 0 16px;
}

.bottom-actions {
  display: flex;
  justify-content: center;
  gap: 8px;
  padding: 12px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

/* 预览 */
.preview-invoice {
  background: #fff;
  padding: 16px;
  border: 1px solid #e0e0e0;
  font-size: 13px;
}

.preview-head {
  text-align: center;
  margin-bottom: 12px;
}

.preview-row {
  display: flex;
  margin-bottom: 4px;
}

.preview-row span {
  color: #666;
  width: 100px;
  flex-shrink: 0;
}

.preview-table {
  width: 100%;
  border-collapse: collapse;
  margin: 8px 0;
}

.preview-table th,
.preview-table td {
  border: 1px solid #e0e0e0;
  padding: 5px 8px;
  font-size: 12px;
}

.preview-table th {
  background: #f5f5f5;
}

.preview-table .right {
  text-align: right;
}

.preview-total {
  text-align: right;
  line-height: 1.8;
}

.preview-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid #e0e0e0;
  color: #999;
}

/* 成功 */
.success-detail {
  padding: 4px 0;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 4px 24px;
  font-size: 14px;
}

.info-row span {
  color: #909399;
}

.info-row .price {
  color: #e37318;
}
</style>
