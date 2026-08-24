<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-radio-group v-model="strategy" variant="default-filled" @change="onStrategyChange">
          <t-radio-button value="safety">按安全库存</t-radio-button>
          <t-radio-button value="trend">按销售趋势</t-radio-button>
        </t-radio-group>
        <t-select
            v-if="strategy === 'trend'"
            v-model="days"
            :options="daysOptions"
            style="width: 140px; border-radius: 4px"
        />
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="loadList">刷新</t-button>
        <t-button theme="primary" style="border-radius: 4px" :disabled="!selectedRows.length" :loading="generating" @click="openGenerate">一键生成采购单</t-button>
      </t-space>
    </div>

    <div class="simple-page__hint">
      <template v-if="strategy === 'safety'">当「当前库存 + 在途」低于安全库存时建议补货至库存上限（缺省用安全库存）；勾选商品后可一键生成采购订单草稿。</template>
      <template v-else>建议采购量 = 近{{ days }}天销量 − 当前库存 − 采购在途（向下取整为 0）；勾选商品后可一键生成采购订单草稿。</template>
    </div>

    <div class="simple-page__table">
      <t-table
          row-key="productId"
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
        <template #recentSalesQuantity="{ row }"><span class="num">{{ fmt(row.recentSalesQuantity) }}</span></template>
        <template #currentQuantity="{ row }"><span class="num">{{ row.currentQuantity }}</span></template>
        <template #onOrderQuantity="{ row }"><span class="num">{{ fmt(row.onOrderQuantity) }}</span></template>
        <template #alertQuantity="{ row }"><span class="num">{{ row.alertQuantity }}</span></template>
        <template #maxStockQuantity="{ row }">
          <span class="num">{{ row.maxStockQuantity > 0 ? row.maxStockQuantity : '—' }}</span>
        </template>
        <template #suggestedQuantity="{ row }">
          <t-tag v-if="row.suggestedQuantity > 0" theme="warning" variant="light">{{ row.suggestedQuantity }}</t-tag>
          <span v-else class="muted">—</span>
        </template>
      </t-table>
    </div>

    <t-dialog
        v-model:visible="dialogVisible"
        header="一键生成采购单"
        :confirm-loading="generating"
        @confirm="doGenerate"
    >
      <t-form label-width="90px" :colon="false">
        <t-form-item label="供应商">
          <t-select
              v-model="form.supplierId"
              :options="supplierList"
              :keys="{ value: 'id', label: 'name' }"
              filterable
              placeholder="请选择供应商"
              style="width: 100%"
          />
        </t-form-item>
        <t-form-item label="入库仓库">
          <t-select
              v-model="form.warehouseId"
              :options="warehouseList"
              :keys="{ value: 'id', label: 'name' }"
              filterable
              clearable
              placeholder="默认仓库（可选）"
              style="width: 100%"
          />
        </t-form-item>
        <t-form-item label="生成明细">
          <div class="generate-tip">共 {{ selectedRows.length }} 件商品，按建议补货量生成草稿。</div>
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script>
import PurchaseSuggestion from "@js/api/purchase/PurchaseSuggestion";
import Supplier from "@js/api/basic/Supplier";
import Warehouse from "@js/api/basic/Warehouse";
import {MessagePlugin} from "tdesign-vue-next";

const SAFETY_COLUMNS = [
  {colKey: 'row-select', type: 'multiple', width: 46},
  {colKey: 'productCode', title: '商品编码', width: 140, ellipsis: true},
  {colKey: 'productName', title: '商品名称', minWidth: 140, ellipsis: true},
  {colKey: 'productSpecification', title: '规格', width: 110, ellipsis: true},
  {colKey: 'productCategoryName', title: '分类', width: 120, ellipsis: true},
  {colKey: 'productUnitName', title: '单位', width: 70, align: 'center'},
  {colKey: 'currentQuantity', title: '当前库存', width: 100, align: 'right'},
  {colKey: 'onOrderQuantity', title: '在途', width: 90, align: 'right'},
  {colKey: 'alertQuantity', title: '安全库存', width: 100, align: 'right'},
  {colKey: 'maxStockQuantity', title: '库存上限', width: 100, align: 'right'},
  {colKey: 'suggestedQuantity', title: '建议补货量', width: 110, align: 'center'},
];

const TREND_COLUMNS = [
  {colKey: 'row-select', type: 'multiple', width: 46},
  {colKey: 'productCode', title: '商品编码', width: 140, ellipsis: true},
  {colKey: 'productName', title: '商品名称', minWidth: 140, ellipsis: true},
  {colKey: 'productSpecification', title: '规格', width: 110, ellipsis: true},
  {colKey: 'productCategoryName', title: '分类', width: 120, ellipsis: true},
  {colKey: 'productUnitName', title: '单位', width: 70, align: 'center'},
  {colKey: 'recentSalesQuantity', title: '销量', width: 100, align: 'right'},
  {colKey: 'currentQuantity', title: '当前库存', width: 100, align: 'right'},
  {colKey: 'onOrderQuantity', title: '在途', width: 90, align: 'right'},
  {colKey: 'suggestedQuantity', title: '建议采购量', width: 110, align: 'center'},
];

export default {
  name: "SmartReplenishment",
  data() {
    return {
      strategy: 'safety',
      loading: false,
      generating: false,
      dataList: [],
      selectedRowKeys: [],
      selectedRows: [],
      days: 30,
      daysOptions: [
        {label: '近15天', value: 15},
        {label: '近30天', value: 30},
        {label: '近60天', value: 60},
        {label: '近90天', value: 90},
      ],
      supplierList: [],
      warehouseList: [],
      dialogVisible: false,
      form: {supplierId: null, warehouseId: null},
      columns: SAFETY_COLUMNS,
    };
  },
  methods: {
    fmt(v) {
      const n = Number(v) || 0;
      return n.toLocaleString('zh-CN', {maximumFractionDigits: 2});
    },
    onStrategyChange(val) {
      this.columns = val === 'safety' ? SAFETY_COLUMNS : TREND_COLUMNS;
      this.dataList = [];
      this.selectedRowKeys = [];
      this.selectedRows = [];
      this.loadList();
    },
    onSelectChange(value, {selectedRowData}) {
      this.selectedRowKeys = value;
      this.selectedRows = selectedRowData || [];
    },
    loadSelect() {
      Supplier.select().then(({data}) => (this.supplierList = data || []));
      Warehouse.select().then(({data}) => (this.warehouseList = data || []));
    },
    loadList() {
      this.loading = true;
      const api = this.strategy === 'safety'
        ? PurchaseSuggestion.replenishment()
        : PurchaseSuggestion.salesDriven({days: this.days});
      api
        .then(({data}) => {
          this.dataList = data || [];
          this.selectedRowKeys = [];
          this.selectedRows = [];
        })
        .finally(() => (this.loading = false));
    },
    openGenerate() {
      if (!this.selectedRows.length) {
        MessagePlugin.warning('请先勾选要补货的商品');
        return;
      }
      this.form.supplierId = null;
      this.form.warehouseId = null;
      this.dialogVisible = true;
    },
    doGenerate() {
      if (!this.form.supplierId) {
        MessagePlugin.warning('请选择供应商');
        return;
      }
      const items = this.selectedRows.map((row) => ({
        productId: row.productId,
        quantity: row.suggestedQuantity > 0 ? row.suggestedQuantity : 1,
      }));
      this.generating = true;
      PurchaseSuggestion.generate({
        supplierId: this.form.supplierId,
        warehouseId: this.form.warehouseId || null,
        items,
      })
        .then(({data}) => {
          MessagePlugin.success(`已生成采购单：${data}`);
          this.dialogVisible = false;
          this.loadList();
        })
        .finally(() => (this.generating = false));
    },
  },
  created() {
    this.loadSelect();
    this.loadList();
  }
};
</script>

<style scoped>
.num {
  font-variant-numeric: tabular-nums;
}
.muted {
  color: #8f959e;
}
.generate-tip {
  color: var(--td-text-color-secondary);
  font-size: 13px;
}
</style>
