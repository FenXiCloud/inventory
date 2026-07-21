<template>
  <div class="print-preview">
    <div class="print-preview__toolbar no-print">
      <t-space>
        <t-select
          v-if="templates.length > 1"
          v-model="templateId"
          :options="templateOptions"
          placeholder="选择模板"
          style="width: 220px"
          @change="applyTemplate"
        />
        <t-button theme="primary" @click="doPrint">打 印</t-button>
        <t-button variant="outline" @click="$emit('close')">关 闭</t-button>
      </t-space>
    </div>
    <div class="print-preview__body" ref="printArea">
      <h2 class="print-preview__title">{{ documentType }}</h2>
      <table class="print-preview__header" v-if="headerFields.length">
        <tbody>
          <tr v-for="row in headerRows" :key="row[0].key">
            <template v-for="f in row" :key="f.key">
              <td class="label">{{ f.label }}</td>
              <td class="value">{{ headerValue(f.key) }}</td>
            </template>
          </tr>
        </tbody>
      </table>
      <table class="print-preview__items" v-if="itemFields.length">
        <thead>
          <tr>
            <th style="width: 50px">序号</th>
            <th v-for="f in itemFields" :key="f.key">{{ f.label }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, idx) in items" :key="idx">
            <td>{{ idx + 1 }}</td>
            <td v-for="f in itemFields" :key="f.key">{{ itemValue(item, f.key) }}</td>
          </tr>
          <tr v-if="!items.length">
            <td :colspan="itemFields.length + 1" style="text-align:center">暂无明细</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import PrintTemplate from '@js/api/setting/PrintTemplate';
import { MessagePlugin } from 'tdesign-vue-next';

const HEADER_KEYS = new Set(['orderNo', 'orderDate', 'partner', 'amount', 'remarks']);
const DEFAULT_CONTENT = [
  {key: 'orderNo', label: '单据编号', enabled: true},
  {key: 'orderDate', label: '单据日期', enabled: true},
  {key: 'partner', label: '往来单位', enabled: true},
  {key: 'amount', label: '金额', enabled: true},
  {key: 'productName', label: '产品名称', enabled: true},
  {key: 'quantity', label: '数量', enabled: true},
  {key: 'price', label: '单价', enabled: true},
  {key: 'amount', label: '金额', enabled: true, item: true},
  {key: 'remarks', label: '备注', enabled: true},
];

export default {
  name: 'PrintPreview',
  emits: ['close'],
  props: {
    documentType: {type: String, required: true},
    data: {type: Object, default: () => ({})}
  },
  data() {
    return {
      templates: [],
      templateId: null,
      content: []
    };
  },
  computed: {
    templateOptions() {
      return this.templates.map(t => ({label: t.name, value: t.id}));
    },
    enabledFields() {
      const list = Array.isArray(this.content) && this.content.length ? this.content : DEFAULT_CONTENT;
      return list.filter(f => f.enabled !== false);
    },
    headerFields() {
      const hasSection = this.enabledFields.some(f => f.section);
      const out = [];
      const seen = new Set();
      for (const f of this.enabledFields) {
        const isHeader = hasSection ? f.section === 'header' : HEADER_KEYS.has(f.key);
        if (!isHeader) continue;
        if (seen.has(f.key)) continue;
        seen.add(f.key);
        out.push(f);
      }
      return out;
    },
    itemFields() {
      const itemKeys = ['productName', 'quantity', 'price', 'amount', 'remarks'];
      const hasSection = this.enabledFields.some(f => f.section);
      const out = [];
      const seen = new Set();
      for (const f of this.enabledFields) {
        const isItem = hasSection ? f.section === 'item' : itemKeys.includes(f.key);
        if (!isItem) continue;
        if (!itemKeys.includes(f.key) && hasSection) {
          // allow custom item keys
        } else if (!itemKeys.includes(f.key)) {
          continue;
        }
        if (seen.has(f.key)) continue;
        seen.add(f.key);
        out.push(f);
      }
      return out;
    },
    headerRows() {
      const rows = [];
      const fields = this.headerFields;
      for (let i = 0; i < fields.length; i += 2) {
        rows.push(fields.slice(i, i + 2));
      }
      return rows;
    },
    header() {
      return this.data?.header || this.data || {};
    },
    items() {
      return this.data?.items || this.data?.productData || this.data?.tableData || [];
    }
  },
  methods: {
    headerValue(key) {
      const h = this.header;
      const map = {
        orderNo: h.orderNo || h.documentNumber || h.businessNo || '',
        orderDate: h.orderDate || h.businessDate || h.date || '',
        partner: h.partner || h.customerName || h.supplierName || h.correspondentsName || '',
        amount: h.amount ?? h.finalAmount ?? h.totalAmount ?? h.collectionAmount ?? '',
        remarks: h.remarks || ''
      };
      return map[key] ?? h[key] ?? '';
    },
    itemValue(item, key) {
      const map = {
        productName: item.productName || item.name || '',
        quantity: item.quantity ?? item.secondaryQuantity ?? '',
        price: item.price ?? item.unitPrice ?? item.secondaryPrice ?? '',
        amount: item.amount ?? item.subtotal ?? item.finalAmount ?? '',
        remarks: item.remarks || ''
      };
      return map[key] ?? item[key] ?? '';
    },
    applyTemplate() {
      const t = this.templates.find(x => x.id === this.templateId);
      if (!t) return;
      let content = t.content;
      if (typeof content === 'string') {
        try { content = JSON.parse(content); } catch (e) { content = []; }
      }
      this.content = Array.isArray(content) ? content : [];
    },
    doPrint() {
      window.print();
    },
    loadTemplates() {
      const q = PrintTemplate.byType
        ? PrintTemplate.byType(this.documentType)
        : PrintTemplate.list({documentType: this.documentType});
      q.then(({data}) => {
        let list = data || [];
        if (list.results) list = list.results;
        this.templates = list;
        const def = list.find(t => t.systemDefault) || list[0];
        if (def) {
          this.templateId = def.id;
          this.applyTemplate();
        } else {
          this.content = DEFAULT_CONTENT;
        }
      }).catch(() => {
        this.content = DEFAULT_CONTENT;
        MessagePlugin.warning('未找到打印模板，使用默认字段');
      });
    }
  },
  created() {
    this.loadTemplates();
  }
};
</script>

<style scoped>
.print-preview { padding: 8px 12px 16px; }
.print-preview__toolbar { margin-bottom: 12px; }
.print-preview__title { text-align: center; margin: 0 0 16px; font-size: 20px; }
.print-preview__header { width: 100%; margin-bottom: 16px; border-collapse: collapse; }
.print-preview__header .label { width: 100px; color: #666; padding: 4px 8px; }
.print-preview__header .value { padding: 4px 8px; min-width: 140px; }
.print-preview__items { width: 100%; border-collapse: collapse; }
.print-preview__items th,
.print-preview__items td { border: 1px solid #ddd; padding: 6px 8px; font-size: 13px; }
.print-preview__items th { background: #f5f5f5; }
@media print {
  .no-print { display: none !important; }
  .print-preview { padding: 0; }
}
</style>
