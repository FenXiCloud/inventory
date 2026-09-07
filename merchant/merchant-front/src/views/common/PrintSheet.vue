<template>
  <div class="print-sheet">
    <div class="print-sheet__titleblock">
      <h2 class="print-sheet__title">{{ documentType }}</h2>
      <div class="print-sheet__rule"></div>
    </div>
    <table class="print-sheet__header" v-if="headerFields.length">
      <tbody>
        <tr v-for="row in headerRows" :key="row[0].key">
          <template v-for="f in row" :key="f.key">
            <td class="label">{{ f.label }}</td>
            <td class="value">{{ headerValue(f.key) }}</td>
          </template>
        </tr>
      </tbody>
    </table>
    <table class="print-sheet__items" v-if="itemFields.length">
      <thead>
        <tr>
          <th class="seq">序号</th>
          <th v-for="f in itemFields" :key="f.key" :class="colClass(f.key)">{{ f.label }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, idx) in items" :key="idx">
          <td class="seq">{{ idx + 1 }}</td>
          <td v-for="f in itemFields" :key="f.key" :class="colClass(f.key)">{{ itemValue(item, f.key) }}</td>
        </tr>
        <tr v-if="!items.length">
          <td :colspan="itemFields.length + 1" style="text-align:center">暂无明细</td>
        </tr>
      </tbody>
      <tfoot v-if="items.length && labelColspan">
        <tr class="total-row">
          <td :colspan="labelColspan">合计</td>
          <td v-for="f in totalCells" :key="f.key" :class="colClass(f.key)">{{ columnTotal(f.key) }}</td>
        </tr>
      </tfoot>
    </table>
  </div>
</template>

<script>
/**
 * 打印单据渲染组件（纯展示）。
 * 实际打印（PrintPreview）与模板编辑/列表的效果预览共用本组件，保证预览所见即打印所得。
 * content 为空时回退到与后端预置模板一致的默认字段。
 */
import { fmtQty, fmtPrice, fmtMoney } from '@common/number';

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
  name: 'PrintSheet',
  props: {
    documentType: {type: String, default: ''},
    content: {type: Array, default: () => []},
    data: {type: Object, default: () => ({})}
  },
  computed: {
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
    // 合计行：从第一个可汇总列（数量/金额）开始，之前的列并入「合计」标签单元格
    labelColspan() {
      const idx = this.itemFields.findIndex(f => f.key === 'quantity' || f.key === 'amount');
      return idx < 0 ? 0 : idx + 1;
    },
    totalCells() {
      const idx = this.itemFields.findIndex(f => f.key === 'quantity' || f.key === 'amount');
      return idx < 0 ? [] : this.itemFields.slice(idx);
    },
    header() {
      return this.data?.header || this.data || {};
    },
    items() {
      return this.data?.items || this.data?.productData || this.data?.tableData || [];
    }
  },
  methods: {
    colClass(key) {
      return ['quantity', 'price', 'amount'].includes(key) ? 'num' : '';
    },
    columnTotal(key) {
      // 单价不做汇总；数量/金额按可见明细行求和
      if (key !== 'quantity' && key !== 'amount') return '';
      let sum = 0;
      let any = false;
      for (const item of this.items) {
        const raw = this.itemValue(item, key);
        const n = Number(String(raw).replace(/,/g, ''));
        if (raw !== '' && raw != null && !Number.isNaN(n)) {
          sum += n;
          any = true;
        }
      }
      if (!any) return '';
      // 合计与明细同口径：数量随 qtyDp、金额恒2位
      return key === 'quantity' ? fmtQty(sum) : fmtMoney(sum);
    },
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
        // 打印口径与账套参数一致：数量随 qtyDp、单价随 priceDp、金额恒2位（空值原样空串）
        quantity: fmtQty(item.quantity ?? item.secondaryQuantity ?? ''),
        price: fmtPrice(item.price ?? item.unitPrice ?? item.secondaryPrice ?? ''),
        amount: fmtMoney(item.amount ?? item.subtotal ?? item.finalAmount ?? ''),
        remarks: item.remarks || ''
      };
      return map[key] ?? item[key] ?? '';
    }
  }
};
</script>

<style scoped>
.print-sheet { color: #303133; font-size: 13px; }

.print-sheet__titleblock { margin: 0 0 18px; }
.print-sheet__title {
  text-align: center;
  margin: 0 0 10px;
  font-size: 21px;
  font-weight: 600;
  letter-spacing: 3px;
}
.print-sheet__rule { height: 2px; background: #303133; }

.print-sheet__header { width: 100%; margin-bottom: 18px; border-collapse: collapse; }
.print-sheet__header td { padding: 6px 8px; border-bottom: 1px dashed #e4e7ed; }
.print-sheet__header tr:last-child td { border-bottom: none; }
.print-sheet__header .label { width: 88px; color: #909399; }
.print-sheet__header .value { padding-right: 24px; font-weight: 500; }

.print-sheet__items { width: 100%; border-collapse: collapse; }
.print-sheet__items th,
.print-sheet__items td { border: 1px solid #dcdfe6; padding: 6px 10px; font-size: 13px; }
.print-sheet__items th { background: #fafafa; color: #606266; font-weight: 600; }
.print-sheet__items .seq { width: 50px; text-align: center; }
.print-sheet__items .num {
  text-align: right;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.print-sheet__items .total-row td {
  background: #fafafa;
  font-weight: 600;
  text-align: right;
}
</style>
