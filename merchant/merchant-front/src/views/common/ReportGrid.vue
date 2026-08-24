<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <span class="report-grid__title">{{ title }}</span>
        <t-button theme="primary" style="border-radius: 4px" :loading="loading" @click="load">刷新</t-button>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" @click="excel">导 出</t-button>
      </t-space>
    </div>

    <div class="simple-page__hint">
      {{ hint }}
    </div>

    <div class="simple-page__table">
      <t-table
          row-key="rowKey"
          size="medium"
          bordered
          hover
          height="100%"
          table-layout="fixed"
          :data="dataList"
          :columns="columns"
          :loading="loading"
          :foot-data="footData"
      />
    </div>
  </div>
</template>

<script>
import InventoryReport from '@js/api/inventory/InventoryReport';
import { export_json_to_excel } from '@js/excel/export2Excel';
import { MessagePlugin } from 'tdesign-vue-next';

const CONFIG = {
  warning: {
    title: '库存预警',
    hint: '列出当前库存数量低于或等于预警库存的商品，便于及时补货。',
    columns: [
      { colKey: 'productCode', title: '产品编码', minWidth: 120, ellipsis: true },
      { colKey: 'productName', title: '产品名称', minWidth: 140, ellipsis: true },
      { colKey: 'productSpecification', title: '规格型号', width: 120, ellipsis: true },
      { colKey: 'brand', title: '品牌', width: 100, ellipsis: true },
      { colKey: 'productCategoryName', title: '产品类别', width: 120, ellipsis: true },
      { colKey: 'productUnitName', title: '单位', width: 80, align: 'center' },
      { colKey: 'warehouseName', title: '仓库', width: 120, ellipsis: true },
      { colKey: 'currentQuantity', title: '当前库存', width: 100, align: 'right' },
      { colKey: 'alertQuantity', title: '预警库存', width: 100, align: 'right' },
      { colKey: 'shortageQuantity', title: '缺货数量', width: 100, align: 'right' },
    ],
  },
  overstock: {
    title: '库存上限预警',
    hint: '列出当前库存数量达到或超过库存上限的商品，提示积压风险，便于及时调拨或促销消化。',
    columns: [
      { colKey: 'productCode', title: '产品编码', minWidth: 120, ellipsis: true },
      { colKey: 'productName', title: '产品名称', minWidth: 140, ellipsis: true },
      { colKey: 'productSpecification', title: '规格型号', width: 120, ellipsis: true },
      { colKey: 'brand', title: '品牌', width: 100, ellipsis: true },
      { colKey: 'productCategoryName', title: '产品类别', width: 120, ellipsis: true },
      { colKey: 'productUnitName', title: '单位', width: 80, align: 'center' },
      { colKey: 'warehouseName', title: '仓库', width: 120, ellipsis: true },
      { colKey: 'currentQuantity', title: '当前库存', width: 100, align: 'right' },
      { colKey: 'maxStockQuantity', title: '库存上限', width: 100, align: 'right' },
      { colKey: 'excessQuantity', title: '超出数量', width: 100, align: 'right' },
    ],
  },
  overview: {
    title: '库存状况总览',
    hint: '按商品汇总各仓库的库存数量与账面成本，展示平均成本。',
    columns: [
      { colKey: 'productCode', title: '产品编码', minWidth: 120, ellipsis: true },
      { colKey: 'productName', title: '产品名称', minWidth: 140, ellipsis: true },
      { colKey: 'productSpecification', title: '规格型号', width: 120, ellipsis: true },
      { colKey: 'brand', title: '品牌', width: 100, ellipsis: true },
      { colKey: 'productCategoryName', title: '产品类别', width: 120, ellipsis: true },
      { colKey: 'productUnitName', title: '单位', width: 80, align: 'center' },
      { colKey: 'quantity', title: '库存数量', width: 100, align: 'right' },
      { colKey: 'averageCost', title: '平均成本', width: 110, align: 'right' },
      { colKey: 'totalCost', title: '成本合计', width: 120, align: 'right' },
    ],
  },
  distribution: {
    title: '库存分布',
    hint: '按仓库汇总库存数量与成本，查看各仓库的库存分布。',
    columns: [
      { colKey: 'warehouseCode', title: '仓库编码', width: 120, ellipsis: true },
      { colKey: 'warehouseName', title: '仓库名称', minWidth: 160, ellipsis: true },
      { colKey: 'quantity', title: '库存数量', width: 120, align: 'right' },
      { colKey: 'totalCost', title: '成本合计', width: 140, align: 'right' },
      { colKey: 'productCount', title: '商品种类数', width: 120, align: 'right' },
    ],
  },
  virtualStock: {
    title: '虚拟库存状况表',
    hint: '可用库存 = 账面库存 + 采购在途 - 销售占用（已审核且未完成单据）。',
    columns: [
      { colKey: 'productCode', title: '产品编码', minWidth: 120, ellipsis: true },
      { colKey: 'productName', title: '产品名称', minWidth: 140, ellipsis: true },
      { colKey: 'productSpecification', title: '规格型号', width: 120, ellipsis: true },
      { colKey: 'productCategoryName', title: '产品类别', width: 120, ellipsis: true },
      { colKey: 'productUnitName', title: '单位', width: 80, align: 'center' },
      { colKey: 'currentQuantity', title: '账面库存', width: 100, align: 'right' },
      { colKey: 'onOrderQuantity', title: '采购在途', width: 100, align: 'right' },
      { colKey: 'reservedQuantity', title: '销售占用', width: 100, align: 'right' },
      { colKey: 'availableQuantity', title: '可用库存', width: 100, align: 'right' },
      { colKey: 'totalCost', title: '成本合计', width: 120, align: 'right' },
    ],
  },
  transfer: {
    title: '调拨统计报表',
    hint: '按商品汇总已审核调拨单的调拨数量。',
    columns: [
      { colKey: 'productCode', title: '产品编码', minWidth: 120, ellipsis: true },
      { colKey: 'productName', title: '产品名称', minWidth: 140, ellipsis: true },
      { colKey: 'productSpecification', title: '规格型号', width: 120, ellipsis: true },
      { colKey: 'productCategoryName', title: '产品类别', width: 120, ellipsis: true },
      { colKey: 'productUnitName', title: '单位', width: 80, align: 'center' },
      { colKey: 'quantity', title: '调拨数量', width: 120, align: 'right' },
    ],
  },
  batch: {
    title: '批次跟踪查询',
    hint: '按商品与批次号汇总库存流水明细（数量与当前库存）。',
    columns: [
      { colKey: 'productCode', title: '产品编码', minWidth: 120, ellipsis: true },
      { colKey: 'productName', title: '产品名称', minWidth: 140, ellipsis: true },
      { colKey: 'productSpecification', title: '规格型号', width: 120, ellipsis: true },
      { colKey: 'productCategoryName', title: '产品类别', width: 120, ellipsis: true },
      { colKey: 'productUnitName', title: '单位', width: 80, align: 'center' },
      { colKey: 'batchNumber', title: '批次号', width: 140, ellipsis: true },
      { colKey: 'quantity', title: '入库数量', width: 100, align: 'right' },
      { colKey: 'currentQuantity', title: '当前库存', width: 100, align: 'right' },
    ],
  },
  lossGain: {
    title: '报损报溢汇总表',
    hint: '按商品汇总已审核报损单（盘亏）与报溢单（盘盈）的数量和金额。',
    columns: [
      { colKey: 'productCode', title: '产品编码', minWidth: 120, ellipsis: true },
      { colKey: 'productName', title: '产品名称', minWidth: 140, ellipsis: true },
      { colKey: 'productSpecification', title: '规格型号', width: 120, ellipsis: true },
      { colKey: 'lossQuantity', title: '报损数量', width: 100, align: 'right' },
      { colKey: 'lossAmount', title: '报损金额', width: 110, align: 'right' },
      { colKey: 'gainQuantity', title: '报溢数量', width: 100, align: 'right' },
      { colKey: 'gainAmount', title: '报溢金额', width: 110, align: 'right' },
      { colKey: 'netQuantity', title: '净增减数量', width: 110, align: 'right' },
      { colKey: 'netAmount', title: '净增减金额', width: 120, align: 'right' },
    ],
  },
  expiry: {
    title: '保质期预警',
    hint: '列出采购入库明细中已登记有效期至的批次，按有效期升序；30 天内到期或已过期的批次高亮提示。',
    columns: [
      { colKey: 'productCode', title: '产品编码', minWidth: 120, ellipsis: true },
      { colKey: 'productName', title: '产品名称', minWidth: 140, ellipsis: true },
      { colKey: 'productSpecification', title: '规格型号', width: 120, ellipsis: true },
      { colKey: 'warehouseName', title: '仓库', width: 120, ellipsis: true },
      { colKey: 'batchNumber', title: '批次号', width: 140, ellipsis: true },
      { colKey: 'productionDate', title: '生产日期', width: 110, align: 'center' },
      { colKey: 'expiryDate', title: '有效期至', width: 110, align: 'center' },
      { colKey: 'quantity', title: '数量', width: 90, align: 'right' },
      { colKey: 'daysToExpiry', title: '剩余天数', width: 90, align: 'right' },
      { colKey: 'expiryStatus', title: '状态', width: 100, align: 'center' },
    ],
  },
};

export default {
  name: 'ReportGrid',
  props: {
    reportType: { type: String, required: true },
  },
  data() {
    return {
      dataList: [],
      loading: false,
    };
  },
  computed: {
    config() {
      return CONFIG[this.reportType] || {};
    },
    title() {
      return this.config.title || '报表';
    },
    hint() {
      return this.config.hint || '';
    },
    columns() {
      return this.config.columns || [];
    },
    footData() {
      const cols = this.columns || [];
      if (!cols.length) return [];
      const foot = { [cols[0].colKey]: '合计' };
      let hasNumeric = false;
      cols.forEach((c) => {
        if (c.align === 'right') {
          const sum = (this.dataList || []).reduce((acc, row) => {
            const v = Number(row[c.colKey]);
            return acc + (Number.isFinite(v) ? v : 0);
          }, 0);
          foot[c.colKey] = Number(sum.toFixed(2));
          hasNumeric = true;
        }
      });
      return hasNumeric ? [foot] : [];
    },
  },
  methods: {
    load() {
      this.loading = true;
      const fn = {
        warning: InventoryReport.warning,
        overstock: InventoryReport.overstock,
        overview: InventoryReport.overview,
        distribution: InventoryReport.distribution,
        virtualStock: InventoryReport.virtualStock,
        transfer: InventoryReport.transfer,
        batch: InventoryReport.batch,
        lossGain: InventoryReport.lossGain,
        expiry: InventoryReport.expiry,
      }[this.reportType];
      if (!fn) {
        this.loading = false;
        return;
      }
      fn().then(({ data }) => {
        this.dataList = data || [];
      }).catch(() => {
        this.dataList = [];
      }).finally(() => {
        this.loading = false;
      });
    },
    excel() {
      if (!this.dataList.length) {
        MessagePlugin.warning('暂无数据～');
        return;
      }
      const cols = this.columns;
      const header = cols.map((c) => c.title);
      const data = this.dataList.map((row) => cols.map((c) => row[c.colKey] ?? ''));
      export_json_to_excel({
        header,
        data,
        filename: this.title,
        autoWidth: true,
        bookType: 'xlsx',
      });
    },
  },
  created() {
    this.load();
  },
};
</script>

<style scoped>
.report-grid__title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-right: 8px;
}

.simple-page__hint {
  flex-shrink: 0;
  margin-bottom: 8px;
  padding: 8px 12px;
  border-radius: 4px;
  background: #f3f3f3;
  color: #555;
  font-size: 13px;
  line-height: 1.6;
}
</style>
