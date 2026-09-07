<template>
  <div class="page-column stock-take-form">
    <div class="page-column-full-body">
      <div class="form-toolbar">
        <div class="form-toolbar__left">
          <label class="stock-take-form__label">盘点日期</label>
          <t-date-picker
              v-model="form.checkDate"
              mode="date"
              format="YYYY-MM-DD"
              value-type="YYYY-MM-DD"
              :disabled="isLocked"
              style="width: 160px"
          />
          <label class="stock-take-form__label">仓库</label>
          <t-select
              v-model="form.warehouseIds"
              class="stock-take-form__warehouse"
              filterable
              multiple
              clearable
              :options="warehouseList"
              :keys="{ value: 'id', label: 'name' }"
              placeholder="请选择仓库"
              :disabled="isLocked || !!form.id"
          />
          <t-button
              v-if="!form.id && !isLocked"
              theme="primary"
              :disabled="!canLoadInventory"
              :loading="loading"
              @click="confirmLoadInventory"
          >
            载入库存
          </t-button>
          <template v-if="allStockTakeData.length">
            <t-button
                v-if="!isLocked"
                variant="outline"
                :loading="loading"
                @click="fillEmptyBySystem"
            >
              未填按账面填写
            </t-button>
            <t-radio-group v-model="viewMode" variant="default-filled" size="small">
              <t-radio-button value="all">全部</t-radio-button>
              <t-radio-button value="unfilled">未填({{ unfilledCount }})</t-radio-button>
              <t-radio-button value="diff">有盈亏({{ diffCount }})</t-radio-button>
            </t-radio-group>
            <t-select
                v-model="filterProductId"
                class="stock-take-form__product"
                filterable
                clearable
                :options="productFilterOptions"
                :keys="{ value: 'id', label: 'customName' }"
                placeholder="产品"
            />
            <t-input
                v-model.trim="filterKeyword"
                clearable
                class="stock-take-form__search"
                placeholder="产品编号/名称"
            >
              <template #suffixIcon>
                <t-icon name="search"/>
              </template>
            </t-input>
            <span v-if="!isLocked && unfilledCount > 0" class="stock-take-form__hint">
              还有 {{ unfilledCount }} 行未填 · 回车跳下一行
            </span>
          </template>
        </div>
        <div class="form-toolbar__right">
          <Stamp v-if="isAudited"/>
        </div>
      </div>

      <div v-if="allStockTakeData.length" class="stock-take-form__summary">
        <span>共 {{ allStockTakeData.length }} 行</span>
        <span>未填 <b>{{ unfilledCount }}</b></span>
        <span class="stock-take-form__summary-gain">盘盈 {{ gainSummary.count }} 项 / {{ gainSummary.qty }}</span>
        <span class="stock-take-form__summary-loss">盘亏 {{ lossSummary.count }} 项 / {{ lossSummary.qty }}</span>
      </div>

      <t-table
          ref="xTable"
          row-key="_rowKey"
          size="small"
          bordered
          stripe
          hover
          table-layout="fixed"
          :columns="columns"
          :data="displayList"
          :foot-data="footData"
          :loading="loading"
          :empty="emptyText"
          :row-class-name="rowClassName"
      >
        <template #actualQuantity="{ row }">
          <div v-if="!isLocked" :id="`qty-${row._rowKey}`">
            <!-- 盘点数量为整件口径（结余列为整数），恒0位、不随账套参数 -->
            <t-input-number
                v-model="row.actualQuantity"
                theme="normal"
                :min="0"
                :decimal-places="0"
                placeholder="请输入"
                style="width: 100%"
                @change="() => quantityInput(row)"
                @keydown="(e) => onQtyKeydown(e, row)"
            />
          </div>
          <span v-else>{{ isEmpty(row.actualQuantity) ? '-' : row.actualQuantity }}</span>
        </template>
        <template #deficient="{ row }">
          <span v-if="isEmpty(row.actualQuantity)" class="deficient deficient--empty">-</span>
          <span v-else-if="row.deficient > 0" class="deficient deficient--gain">+{{ row.deficient }}</span>
          <span v-else-if="row.deficient < 0" class="deficient deficient--loss">{{ row.deficient }}</span>
          <span v-else class="deficient deficient--flat">0</span>
        </template>
        <template #differenceReason="{ row }">
          <t-input
              v-if="!isLocked"
              v-model="row.differenceReason"
              placeholder="备注"
              :maxlength="100"
          />
          <span v-else>{{ row.differenceReason || '-' }}</span>
        </template>
      </t-table>

      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px w-80px">备注说明</label>
          <t-input
              v-model="form.remarks"
              :disabled="isLocked"
              placeholder="请输入备注"
              maxlength="150"
              style="width: 80%"
          />
          <label class="ml-16px w-180px">制单人：{{ form.adminName }}</label>
        </div>
      </div>
    </div>

    <div class="page-column-footer modal-column-between bg-white-color border">
      <t-button :loading="loading" @click="closeWindow">取消</t-button>
      <div class="stock-take-form__actions">
        <template v-if="!isAudited && !looked">
          <t-button v-auth="'stockTake:edit'" theme="primary" :loading="loading" @click="saveOrder('save')">保存</t-button>
          <t-button v-auth="'stockTake:edit'" theme="primary" variant="outline" :loading="loading" @click="saveAndApprove">
            保存并审核
          </t-button>
          <t-button v-if="!form.id" v-auth="'stockTake:edit'" variant="outline" :loading="loading" @click="saveOrder('add')">
            保存并新增
          </t-button>
          <t-button v-if="$can('stockTake:audit') && form.id" :loading="loading" @click="approved">审核</t-button>
          <t-button v-if="form.id" variant="outline" :loading="loading" @click="doPrint">打印</t-button>
        </template>
        <template v-else-if="isAudited">
          <t-tooltip :content="inboundBtnTip">
            <span class="stock-take-form__tip-wrap">
              <t-button
                  v-if="canGenerateInbound"
                  theme="primary"
                  :loading="loading"
                  @click="generatedInbounds"
              >
                生成盘盈单（{{ inbounds.length }}）
              </t-button>
              <t-tag v-else-if="inboundGenerated" theme="success" variant="light">盘盈已生成</t-tag>
              <t-tag v-else-if="needInbound === false" theme="default" variant="light">无盘盈</t-tag>
            </span>
          </t-tooltip>
          <t-tooltip :content="outboundBtnTip">
            <span class="stock-take-form__tip-wrap">
              <t-button
                  v-if="canGenerateOutbound"
                  theme="danger"
                  variant="outline"
                  :loading="loading"
                  @click="generatedOutbounds"
              >
                生成盘亏单（{{ outbounds.length }}）
              </t-button>
              <t-tag v-else-if="outboundGenerated" theme="success" variant="light">盘亏已生成</t-tag>
              <t-tag v-else-if="needOutbound === false" theme="default" variant="light">无盘亏</t-tag>
            </span>
          </t-tooltip>
          <t-button variant="outline" :loading="loading" @click="doPrint">打印</t-button>
          <t-button v-if="$can('stockTake:audit') && !looked" :loading="loading" @click="backApproved">反审核</t-button>
        </template>
        <template v-else>
          <t-button variant="outline" :loading="loading" @click="doPrint">打印</t-button>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import {LoadingPlugin, MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import {openPrint} from '@common/print';
import manba from 'manba';
import Product from '@js/api/basic/Product';
import Warehouse from '@js/api/basic/Warehouse';
import Inventory from '@js/api/inventory/Inventory';
import {mapMutations, mapState} from 'vuex';
import StockTake from '@js/api/inventory/StockTake';
import Stamp from '../common/Stamp.vue';

let rowSeq = 0;
function newRow(extra = {}) {
  return {_rowKey: `r-${++rowSeq}`, ...extra};
}

export default {
  name: 'StockTakeForm',
  components: {Stamp},
  props: {
    stockTakeId: [String, Number],
    type: String,
    index: Number,
    status: String,
    /** true | 'inbound' | 'outbound' */
    autoGenerate: {type: [Boolean, String], default: false}
  },
  data() {
    return {
      loading: false,
      productList: [],
      warehouseList: [],
      filterProductId: null,
      filterKeyword: '',
      viewMode: 'all',
      form: {
        id: null,
        checkDate: manba().format('YYYY-MM-DD'),
        remarks: null,
        warehouseId: null,
        warehouseIds: [],
        adminName: '',
        orderStatus: '已保存'
      },
      allStockTakeData: [],
      outbounds: [],
      inbounds: [],
      needInbound: null,
      needOutbound: null,
      inboundGenerated: false,
      outboundGenerated: false
    };
  },
  computed: {
    ...mapState(['user', 'accountBook']),
    isAudited() {
      return this.form.orderStatus === '已审核';
    },
    looked() {
      return this.type === 'look';
    },
    isLocked() {
      return this.isAudited || this.looked;
    },
    canLoadInventory() {
      return (this.form.warehouseIds || []).length > 0;
    },
    canGenerateInbound() {
      return this.isAudited && this.inbounds.length > 0;
    },
    canGenerateOutbound() {
      return this.isAudited && this.outbounds.length > 0;
    },
    inboundBtnTip() {
      if (this.canGenerateInbound) return '生成其他入库单（盘盈入库），保存并审核后生效';
      if (this.inboundGenerated) return '盘盈单已生成';
      return '无盘盈明细';
    },
    outboundBtnTip() {
      if (this.canGenerateOutbound) return '生成其他出库单（盘亏出库），保存并审核后生效';
      if (this.outboundGenerated) return '盘亏单已生成';
      return '无盘亏明细';
    },
    unfilledCount() {
      return (this.allStockTakeData || []).filter((row) => this.isEmpty(row.actualQuantity)).length;
    },
    diffCount() {
      return (this.allStockTakeData || []).filter(
        (row) => !this.isEmpty(row.actualQuantity) && Number(row.deficient) !== 0
      ).length;
    },
    gainSummary() {
      let count = 0;
      let qty = 0;
      (this.allStockTakeData || []).forEach((row) => {
        const d = Number(row.deficient);
        if (!this.isEmpty(row.actualQuantity) && d > 0) {
          count += 1;
          qty += d;
        }
      });
      return {count, qty};
    },
    lossSummary() {
      let count = 0;
      let qty = 0;
      (this.allStockTakeData || []).forEach((row) => {
        const d = Number(row.deficient);
        if (!this.isEmpty(row.actualQuantity) && d < 0) {
          count += 1;
          qty += Math.abs(d);
        }
      });
      return {count, qty};
    },
    emptyText() {
      if (this.form.id || this.allStockTakeData.length) {
        return '没有符合筛选条件的明细';
      }
      return '请选择仓库并点击「载入库存」';
    },
    productFilterOptions() {
      const map = new Map();
      (this.allStockTakeData || []).forEach((row) => {
        if (row.productId && !map.has(row.productId)) {
          map.set(row.productId, {
            id: row.productId,
            customName: `${row.productCode || ''}--${row.productName || ''}`
          });
        }
      });
      return Array.from(map.values());
    },
    displayList() {
      const keyword = (this.filterKeyword || '').trim().toLowerCase();
      const productId = this.filterProductId;
      const mode = this.viewMode;
      return (this.allStockTakeData || []).filter((row) => {
        if (mode === 'unfilled' && !this.isEmpty(row.actualQuantity)) return false;
        if (mode === 'diff') {
          if (this.isEmpty(row.actualQuantity) || Number(row.deficient) === 0) return false;
        }
        if (productId && row.productId !== productId && String(row.productId) !== String(productId)) {
          return false;
        }
        if (!keyword) return true;
        const code = String(row.productCode || '').toLowerCase();
        const name = String(row.productName || '').toLowerCase();
        return code.includes(keyword) || name.includes(keyword);
      });
    },
    columns() {
      return [
        {
          colKey: 'seq',
          title: '序号',
          width: 60,
          align: 'center',
          fixed: 'left',
          cell: (h, {rowIndex}) => rowIndex + 1,
          foot: () => '合计'
        },
        {colKey: 'warehouseName', title: '仓库', width: 110},
        {colKey: 'productCode', title: '产品编码', width: 130},
        {colKey: 'productName', title: '产品名称', minWidth: 160, ellipsis: true},
        {colKey: 'productSpecification', title: '规格', width: 90, ellipsis: true},
        {colKey: 'productCategoryName', title: '类别', width: 90, ellipsis: true},
        {colKey: 'productUnitName', title: '单位', width: 70, align: 'center'},
        {colKey: 'systemQuantity', title: '系统库存', width: 100, align: 'right'},
        {colKey: 'actualQuantity', title: '盘点库存', width: 120, align: 'right'},
        {colKey: 'deficient', title: '盘点盈亏', width: 100, align: 'right'},
        {colKey: 'differenceReason', title: '备注', minWidth: 140}
      ];
    },
    footData() {
      let systemTotal = 0;
      let actualTotal = 0;
      let deficientTotal = 0;
      let hasActual = false;
      (this.displayList || []).forEach((row) => {
        systemTotal += Number(row.systemQuantity) || 0;
        if (!this.isEmpty(row.actualQuantity)) {
          hasActual = true;
          actualTotal += Number(row.actualQuantity) || 0;
          deficientTotal += Number(row.deficient) || 0;
        }
      });
      return [{
        seq: '合计',
        systemQuantity: systemTotal,
        actualQuantity: hasActual ? actualTotal : '',
        deficient: hasActual ? deficientTotal : ''
      }];
    },
    hasFilledActual() {
      return (this.allStockTakeData || []).some((row) => !this.isEmpty(row.actualQuantity));
    }
  },
  methods: {
    ...mapMutations(['closeSelfTab', 'pushTab']),
    isEmpty(value) {
      return value !== 0 && (value === null || value === undefined || value === '');
    },
    rowClassName({row}) {
      if (!this.isLocked && this.isEmpty(row.actualQuantity)) {
        return 'stock-take-form__row--unfilled';
      }
      return '';
    },
    doPrint() {
      const items = (this.allStockTakeData || [])
        .filter((r) => r && r.productId)
        .map((r) => ({
          ...r,
          productName: r.productName || '',
          quantity: r.actualQuantity ?? r.quantity,
          price: r.unitPrice ?? r.price,
          amount: r.subtotal ?? r.amount
        }));
      openPrint('盘点单', {
        header: {
          ...this.form,
          partner: '',
          amount: this.form.totalAmount
        },
        items
      });
    },
    confirmLoadInventory() {
      if (!this.canLoadInventory) {
        MessagePlugin.warning('请先选择仓库');
        return;
      }
      if (this.hasFilledActual) {
        DialogPlugin.confirm({
          header: '重新载入',
          body: '重新载入将覆盖当前未保存的盘点录入，是否继续？',
          onConfirm: () => this.loadInventory()
        });
        return;
      }
      this.loadInventory();
    },
    loadInventory() {
      this.loading = true;
      Inventory.products({
        warehouseIds: (this.form.warehouseIds || []).join(','),
        productId: null,
        filter: null
      })
        .then((res) => {
          const list = (res?.data || []).map((row) =>
            newRow({
              ...row,
              actualQuantity: null,
              deficient: null,
              differenceReason: row.differenceReason || ''
            })
          );
          this.allStockTakeData = list;
          this.filterProductId = null;
          this.filterKeyword = '';
          this.viewMode = 'all';
          MessagePlugin.success(`已载入 ${list.length} 条，请填写盘点库存`);
        })
        .finally(() => {
          this.loading = false;
        });
    },
    fillEmptyBySystem() {
      let filled = 0;
      (this.allStockTakeData || []).forEach((row) => {
        if (this.isEmpty(row.actualQuantity)) {
          row.actualQuantity = Number(row.systemQuantity) || 0;
          this.quantityInput(row);
          filled += 1;
        }
      });
      if (filled === 0) {
        MessagePlugin.info('没有未填行');
        return;
      }
      MessagePlugin.success(`已按账面填写 ${filled} 行，请核对有差异的产品`);
      this.viewMode = 'diff';
    },
    quantityInput(row) {
      if (!row) return;
      if (this.isEmpty(row.actualQuantity)) {
        row.deficient = null;
        return;
      }
      let actualQuantity = Number(row.actualQuantity);
      const systemQuantity = Number(row.systemQuantity) || 0;
      if (Number.isNaN(actualQuantity) || actualQuantity < 0) {
        row.actualQuantity = 0;
        actualQuantity = 0;
      }
      row.deficient = actualQuantity - systemQuantity;
    },
    onQtyKeydown(e, row) {
      if (!e || (e.key !== 'Enter' && e.keyCode !== 13)) return;
      e.preventDefault?.();
      e.stopPropagation?.();
      this.quantityInput(row);
      const list = this.displayList || [];
      const idx = list.findIndex((r) => r._rowKey === row._rowKey);
      if (idx < 0 || idx >= list.length - 1) return;
      const next = list[idx + 1];
      this.$nextTick(() => {
        const wrap = this.$el?.querySelector?.(`#qty-${next._rowKey}`);
        const input = wrap?.querySelector?.('input');
        if (input) {
          input.focus();
          input.select?.();
        }
      });
    },
    saveOrder(type) {
      try {
        const rows = (this.allStockTakeData || []).filter(
          (item) =>
            !this.isEmpty(item.productId) ||
            !this.isEmpty(item.warehouseId) ||
            !this.isEmpty(item.actualQuantity) ||
            !this.isEmpty(item.differenceReason)
        );
        this.validatorsForm(rows);
        const params = this.getSaveOrderParams(rows);
        StockTake.save(params)
          .then(({success, data}) => {
            if (success) {
              MessagePlugin.success(type === 'add' ? '保存成功，已新建空白单' : '保存成功，可继续审核');
              if (type === 'add') {
                this.clearForm();
                this.initIncreaseForm();
                return;
              }
              const id = data?.id || this.form.id;
              if (id) {
                this.loadEditForm(id);
              }
            }
          })
          .finally(() => LoadingPlugin(false));
      } catch (e) {
        LoadingPlugin(false);
        MessagePlugin.error(e?.message || '保存失败~');
      }
    },
    saveAndApprove() {
      try {
        const rows = (this.allStockTakeData || []).filter(
          (item) =>
            !this.isEmpty(item.productId) ||
            !this.isEmpty(item.warehouseId) ||
            !this.isEmpty(item.actualQuantity) ||
            !this.isEmpty(item.differenceReason)
        );
        this.validatorsForm(rows);
        const params = this.getSaveOrderParams(rows);
        StockTake.save(params)
          .then(({success, data}) => {
            if (!success) return;
            const id = data?.id || this.form.id;
            if (!id) {
              MessagePlugin.error('保存成功但未取得单据号');
              return;
            }
            return StockTake.approved('已审核', [id]).then(() => {
              MessagePlugin.success('已保存并审核，请生成盘盈/盘亏单');
              this.loadEditForm(id, true);
            });
          })
          .finally(() => LoadingPlugin(false));
      } catch (e) {
        LoadingPlugin(false);
        MessagePlugin.error(e?.message || '操作失败~');
      }
    },
    generatedInbounds() {
      if (!this.canGenerateInbound) {
        MessagePlugin.warning(this.inboundBtnTip);
        return;
      }
      this.pushTab({
        key: 'OtherInboundForm',
        title: '新增其他入库单',
        params: {
          type: 'add',
          otherInboundId: null,
          stockTakeId: this.form.id,
          importInbound: this.inbounds
        }
      });
    },
    generatedOutbounds() {
      if (!this.canGenerateOutbound) {
        MessagePlugin.warning(this.outboundBtnTip);
        return;
      }
      this.pushTab({
        key: 'OtherOutboundForm',
        title: '新增其他出库单',
        params: {
          type: 'add',
          otherOutboundId: null,
          stockTakeId: this.form.id,
          importOutbound: this.outbounds
        }
      });
    },
    validatorsForm(rows) {
      if (!(this.form.warehouseIds && this.form.warehouseIds.length > 0)) {
        throw new Error('请选择仓库');
      }
      if (!rows || rows.length === 0) {
        throw new Error('请先载入库存并填写盘点数量');
      }
      LoadingPlugin(true);
      const emptyQty = rows.filter((c) => this.isEmpty(c.actualQuantity));
      if (emptyQty.length > 0) {
        LoadingPlugin(false);
        this.viewMode = 'unfilled';
        throw new Error(`还有 ${emptyQty.length} 行未填写盘点库存`);
      }
    },
    getSaveOrderParams(rows) {
      return {
        stockTake: {
          id: this.form.id,
          checkDate: this.form.checkDate,
          remarks: this.form.remarks,
          warehouseId: this.form.warehouseId,
          warehouseIds: (this.form.warehouseIds || []).join(',')
        },
        stockTakeItems: rows.map((item) => ({
          actualQuantity: item.actualQuantity,
          systemQuantity: item.systemQuantity,
          productId: item.productId,
          warehouseId: item.warehouseId,
          differenceReason: item.differenceReason
        }))
      };
    },
    clearForm() {
      this.form = {
        id: null,
        checkDate: manba().format('YYYY-MM-DD'),
        remarks: null,
        warehouseId: null,
        warehouseIds: [],
        adminName: this.user?.admin?.name || '',
        orderStatus: '已保存'
      };
      this.allStockTakeData = [];
      this.filterProductId = null;
      this.filterKeyword = '';
      this.viewMode = 'all';
      this.inbounds = [];
      this.outbounds = [];
      this.needInbound = null;
      this.needOutbound = null;
      this.inboundGenerated = false;
      this.outboundGenerated = false;
    },
    loadEditForm(id, openGenerate = false) {
      const loadId = id || this.form.id || this.stockTakeId;
      if (!loadId) return;
      this.allStockTakeData = [];
      StockTake.load(loadId).then(({data}) => {
        if (!data || !data.length) return;
        this.form.id = data[0].id;
        this.form.warehouseId = data[0].mainWarehouseId;
        if (data[0].mainWarehouseIds) {
          this.form.warehouseIds = String(data[0].mainWarehouseIds)
            .split(',')
            .filter(Boolean)
            .map((v) => (Number.isNaN(Number(v)) ? v : Number(v)));
        }
        this.form.remarks = data[0].remarks;
        this.form.checkDate = data[0].checkDate;
        this.form.adminName = data[0].adminName;
        this.form.orderStatus = data[0].orderStatus;
        this.allStockTakeData = data.map((item) =>
          newRow({
            productCode: item.productCode,
            productName: item.productName,
            productId: item.productId,
            productSpecification: item.productSpecification,
            productCategoryName: item.productCategoryName,
            productUnitId: item.productUnitId,
            productUnitName: item.productUnitName,
            systemQuantity: item.systemQuantity,
            actualQuantity: item.actualQuantity,
            warehouseName: item.warehouseName,
            warehouseId: item.warehouseId,
            deficient: item.deficient,
            differenceReason: item.differenceReason
          })
        );
        this.filterProductId = null;
        this.filterKeyword = '';
        if (this.isAudited) {
          this.viewMode = 'diff';
          this.getInventoryList(openGenerate || this.autoGenerate);
        } else {
          this.viewMode = this.unfilledCount > 0 ? 'unfilled' : 'all';
        }
      });
    },
    loadDict(callback) {
      LoadingPlugin(true);
      Promise.all([Product.select(), Warehouse.select()])
        .then((results) => {
          this.productList = results[0]?.data || [];
          this.productList.forEach((item) => {
            item.customName = `${item.code}--${item.name}`;
          });
          this.warehouseList = results[1]?.data || [];
          if (callback) callback();
        })
        .finally(() => LoadingPlugin(false));
    },
    initIncreaseForm() {
      this.form.adminName = this.user?.admin?.name || '';
      this.form.id = null;
      this.form.orderStatus = '已保存';
      this.allStockTakeData = [];
      this.filterProductId = null;
      this.filterKeyword = '';
      this.viewMode = 'all';
    },
    approved() {
      if (!this.form.id) {
        MessagePlugin.warning('请先保存单据');
        return;
      }
      DialogPlugin.confirm({
        header: '审核提示',
        body: '确认审核该盘点单？审核后可直接生成盘盈/盘亏单。',
        onConfirm: () =>
          StockTake.approved('已审核', [this.form.id]).then(() => {
            MessagePlugin.success('审核成功');
            this.loadEditForm(this.form.id, true);
          })
      });
    },
    backApproved() {
      if (!this.form.id) {
        MessagePlugin.warning('请先保存单据');
        return;
      }
      DialogPlugin.confirm({
        header: '反审核提示',
        body: '确认反审核该盘点单？',
        onConfirm: () =>
          StockTake.approved('已保存', [this.form.id]).then(() => {
            MessagePlugin.success('操作成功~');
            this.inbounds = [];
            this.outbounds = [];
            this.needInbound = null;
            this.needOutbound = null;
            this.inboundGenerated = false;
            this.outboundGenerated = false;
            this.loadEditForm(this.form.id);
          })
      });
    },
    closeWindow() {
      this.closeSelfTab(this.index);
      this.pushTab({
        keepAlive: false,
        key: 'StockTakeList',
        title: '盘点单'
      });
    },
    applyGenerateAction(action) {
      if (action === 'inbound') {
        if (this.canGenerateInbound) {
          this.generatedInbounds();
        } else {
          MessagePlugin.info(this.inboundBtnTip);
        }
        return;
      }
      if (action === 'outbound') {
        if (this.canGenerateOutbound) {
          this.generatedOutbounds();
        } else {
          MessagePlugin.info(this.outboundBtnTip);
        }
        return;
      }
      if (action) {
        const tips = [];
        if (this.canGenerateInbound) tips.push(`盘盈 ${this.inbounds.length} 项`);
        if (this.canGenerateOutbound) tips.push(`盘亏 ${this.outbounds.length} 项`);
        if (tips.length) {
          MessagePlugin.info(`可生成：${tips.join('，')}，请点击底栏按钮`);
        } else if (this.inboundGenerated || this.outboundGenerated) {
          MessagePlugin.info('盘盈/盘亏单已生成完毕');
        } else {
          MessagePlugin.info('账实相符，无需生成调整单');
        }
      }
    },
    getInventoryList(openGenerate = false) {
      const id = this.stockTakeId || this.form.id;
      if (!id) return;
      StockTake.export(id).then((res) => {
        const data = res?.data || {};
        this.outbounds = data.outbounds || [];
        this.inbounds = data.inbounds || [];
        this.needInbound = !!data.needInbound;
        this.needOutbound = !!data.needOutbound;
        this.inboundGenerated = !!data.inboundGenerated;
        this.outboundGenerated = !!data.outboundGenerated;
        this.$nextTick(() => this.applyGenerateAction(openGenerate));
      });
    }
  },
  activated() {
    if (this.isAudited && (this.stockTakeId || this.form.id)) {
      this.getInventoryList(false);
    }
  },
  created() {
    this.loadDict(() => {
      if (this.stockTakeId) {
        this.loadEditForm();
        return;
      }
      this.initIncreaseForm();
    });
  }
};
</script>

<style lang="less" scoped>
.stock-take-form {
  .form-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 8px 0 12px;
    flex-wrap: wrap;
  }

  .form-toolbar__left {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }

  &__label {
    margin-left: 4px;
    color: var(--td-text-color-secondary, #666);
    white-space: nowrap;
  }

  &__warehouse {
    min-width: 220px;
    max-width: 320px;
  }

  &__product {
    width: 200px;
  }

  &__search {
    width: 200px;
  }

  &__hint {
    color: #d54941;
    font-size: 13px;
    white-space: nowrap;
  }

  &__summary {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    margin: 0 0 10px;
    padding: 8px 12px;
    background: var(--td-bg-color-secondarycontainer, #f3f3f3);
    border-radius: 4px;
    font-size: 13px;
    color: var(--td-text-color-secondary, #666);

    b {
      color: var(--td-text-color-primary, #333);
      font-weight: 600;
    }
  }

  &__summary-gain {
    color: #2ba471;
  }

  &__summary-loss {
    color: #d54941;
  }

  &__actions {
    display: inline-flex;
    gap: 8px;
    align-items: center;
    flex-wrap: wrap;
  }

  &__tip-wrap {
    display: inline-flex;
    align-items: center;
  }

  :deep(.stock-take-form__row--unfilled) {
    background: rgba(213, 73, 65, 0.04);
  }
}

.deficient--empty {
  color: var(--td-text-color-placeholder, #999);
}

.deficient--gain {
  color: #2ba471;
  font-weight: 500;
}

.deficient--loss {
  color: #d54941;
  font-weight: 500;
}

.deficient--flat {
  color: var(--td-text-color-secondary, #666);
}

.filler-panel {
  margin-top: 10px;
}

.filler-item {
  display: flex;
  align-items: center;
}
</style>
