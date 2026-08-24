<template>
  <t-card :bordered="false" class="to-order-card">
    <!-- 筛选条件区 -->
    <div class="filter-bar">
      <t-space wrap>
        <t-select
          v-model="query.pendingStatus"
          placeholder="采购状态"
          clearable
          style="width: 130px"
          :options="statusOptions"
        />
        <t-select
          v-model="query.customerName"
          placeholder="客户名称（模糊搜索）"
          filterable
          clearable
          style="width: 180px"
        >
          <t-option v-for="c in customerList" :key="c.id" :label="c.name" :value="c.name" />
        </t-select>
        <t-date-range-picker
          v-model="dateRange"
          clearable
          style="width: 240px"
          placeholder="订单日期范围"
        />
        <t-input
          v-model="query.orderNo"
          placeholder="订单编号"
          clearable
          style="width: 180px"
          @enter="handleSearch"
        />
        <t-button theme="primary" @click="handleSearch">查询</t-button>
        <t-button theme="default" variant="outline" @click="handleReset">重置</t-button>
      </t-space>
    </div>

    <!-- 操作栏 -->
    <div class="op-bar">
      <t-space>
        <t-button
          theme="primary"
          :disabled="!selectedRowKeys.length"
          @click="openBatchTransfer"
        >
          生成采购入库单
        </t-button>
        <t-button
          theme="default"
          variant="outline"
          :disabled="!selectedRowKeys.length"
          @click="openBatchSupplier"
        >
          批量设置供应商
        </t-button>
        <t-button theme="default" variant="outline" @click="loadList">
          <template #icon><t-icon name="refresh" /></template>
          刷新
        </t-button>
      </t-space>
    </div>

    <!-- 看板列表 -->
    <div class="table-area">
    <t-table
      :data="dataList"
      :columns="columns"
      row-key="salesOrderId"
      :loading="loading"
      :selected-row-keys="selectedRowKeys"
      :expanded-row-keys="expandedRowKeys"
      :pagination="pagination"
      hover
      stripe
      @select-change="onSelectChange"
      @expand-change="onExpandChange"
      @page-change="onPageChange"
    >
      <template #empty>
        <div class="empty-tip">暂无待处理订单，所有已审核订单均已采购</div>
      </template>
      <template #salesOrderNo="{ row }">
        <t-link theme="primary" hover="color" @click="goSalesOrder(row)">
          {{ row.salesOrderNo }}
        </t-link>
      </template>
      <template #supplierNames="{ row }">
        <span v-if="row.supplierNames && row.supplierNames.length">{{ row.supplierNames.join('、') }}</span>
        <span v-else class="muted">未设置</span>
      </template>
      <template #purchaseStatusText="{ row }">
        <t-tag :theme="getStatusTheme(row.purchaseStatusText)" variant="light">
          {{ row.purchaseStatusText }}
        </t-tag>
      </template>
      <template #ops="{ row }">
        <t-button size="small" variant="outline" theme="primary" @click="openSingleTransfer(row)">转采购</t-button>
      </template>
      <template #expandedRow="{ row }">
        <div class="expanded-preview">
          <div class="expanded-preview__title">商品明细预览：</div>
          <t-table
            size="small"
            bordered
            row-key="productId"
            :data="row.items || []"
            :columns="detailColumns"
          >
            <template #pendingPurchaseQuantity="{ row: it }">
              <span class="strong">{{ fmt(it.pendingPurchaseQuantity) }}</span>
            </template>
            <template #supplierName="{ row: it }">
              {{ it.supplierName || '未设置' }}
            </template>
          </t-table>
        </div>
      </template>
    </t-table>
    </div>

    <!-- 底部汇总栏 -->
    <div v-if="selectedRowKeys.length" class="summary-bar">
      <t-space>
        <span>已选订单：<b>{{ selectedOrderCount }}</b> 张</span>
        <span>涉及商品种类：<b>{{ selectedProductCount }}</b> 种</span>
        <span>预计采购总金额：<b class="amount">¥{{ fmt(totalPurchaseAmount) }}</b></span>
        <t-button size="small" theme="primary" @click="openBatchTransfer">生成采购入库单</t-button>
      </t-space>
    </div>

    <!-- 单张转采购对话框 -->
    <t-dialog
      v-model:visible="transferVisible"
      header="生成采购入库单"
      :confirm-btn="{ content: '确认生成', loading: transferring, theme: 'primary' }"
      :cancel-btn="{ content: '取消' }"
      width="860px"
      @confirm="doSingleTransfer"
    >
      <div v-if="transferOrder" class="dialog-order-info">
        <span>来源订单：{{ transferOrder.salesOrderNo }}</span>
        <span>客户名称：{{ transferOrder.customerName }}</span>
      </div>
      <div class="dialog-form-row">
        <t-space>
          <div class="dialog-form-item">
            <label>预计到货日期：</label>
            <t-date-picker v-model="transferExpectedDate" clearable style="width: 160px" />
          </div>
          <div class="dialog-form-item">
            <label>生成方式：</label>
            <t-radio-group v-model="transferStatus">
              <t-radio-button value="草稿">草稿</t-radio-button>
              <t-radio-button value="已审核">已审核</t-radio-button>
            </t-radio-group>
          </div>
        </t-space>
      </div>
      <t-table
        size="small"
        bordered
        row-key="goodsId"
        :data="transferItems"
        :columns="singleTransferColumns"
      >
        <template #quantity="{ row }">
          <t-input-number
            v-model="row.quantity"
            theme="column"
            :max="Number(row.pendingPurchaseQuantity)"
            :min="0"
            :step="1"
            style="width: 110px"
          />
        </template>
        <template #supplier="{ row }">
          <t-select
            v-model="row.supplierId"
            filterable
            clearable
            placeholder="请选择供应商"
            style="width: 140px"
          >
            <t-option v-for="s in supplierList" :key="s.id" :label="s.name" :value="s.id" />
          </t-select>
        </template>
        <template #purchasePrice="{ row }">
          <t-input-number v-model="row.purchasePrice" theme="column" :min="0" :step="0.01" style="width: 110px" />
        </template>
      </t-table>
      <div class="dialog-form-row">
        <label>备注：</label>
        <t-textarea v-model="transferRemark" placeholder="备注信息" :autosize="{ minRows: 1, maxRows: 3 }" style="width: 100%" />
      </div>
    </t-dialog>

    <!-- 批量转采购对话框 -->
    <t-dialog
      v-model:visible="batchTransferVisible"
      header="批量生成采购入库单"
      :confirm-btn="{ content: '确认生成', loading: batchTransferring, theme: 'primary' }"
      :cancel-btn="{ content: '取消' }"
      width="980px"
      @confirm="doBatchTransfer"
    >
      <div class="dialog-form-row">
        <t-space>
          <div class="dialog-form-item">
            <label>预计到货日期：</label>
            <t-date-picker v-model="batchTransferExpectedDate" clearable style="width: 160px" />
          </div>
          <div class="dialog-form-item">
            <label>生成方式：</label>
            <t-radio-group v-model="batchTransferStatus">
              <t-radio-button value="草稿">草稿</t-radio-button>
              <t-radio-button value="已审核">已审核</t-radio-button>
            </t-radio-group>
          </div>
        </t-space>
      </div>
      <t-table
        size="small"
        bordered
        row-key="salesOrderId_goodsId"
        :data="batchTransferRows"
        :columns="batchTransferColumns"
      >
        <template #quantity="{ row }">
          <t-input-number
            v-model="row.quantity"
            theme="column"
            :max="Number(row.pendingPurchaseQuantity)"
            :min="0"
            :step="1"
            style="width: 100px"
          />
        </template>
        <template #supplier="{ row }">
          <t-select
            v-model="row.supplierId"
            filterable
            clearable
            placeholder="请选择供应商"
            style="width: 130px"
          >
            <t-option v-for="s in supplierList" :key="s.id" :label="s.name" :value="s.id" />
          </t-select>
        </template>
        <template #purchasePrice="{ row }">
          <t-input-number v-model="row.purchasePrice" theme="column" :min="0" :step="0.01" style="width: 100px" />
        </template>
      </t-table>
      <div class="dialog-form-row">
        <span class="muted">共 {{ batchTransferRows.length }} 个商品，涉及 {{ batchTransferOrderCount }} 张订单，系统将按供应商自动拆分生成采购入库单</span>
      </div>
    </t-dialog>

    <!-- 批量设置供应商对话框 -->
    <t-dialog
      v-model:visible="supplierDialogVisible"
      header="批量设置供应商"
      :confirm-btn="{ content: '确认设置', theme: 'primary' }"
      :cancel-btn="{ content: '取消' }"
      width="420px"
      @confirm="doBatchSupplier"
    >
      <div class="dialog-form-item">
        <label>将 {{ batchProductIds.length }} 个商品设置为：</label>
        <t-select
          v-model="batchSupplierId"
          filterable
          clearable
          placeholder="请选择供应商"
        >
          <t-option v-for="s in supplierList" :key="s.id" :label="s.name" :value="s.id" />
        </t-select>
      </div>
    </t-dialog>
  </t-card>
</template>

<script>
import {MessagePlugin} from 'tdesign-vue-next';
import {mapMutations} from 'vuex';
import SalesOrder from '@js/api/sales/SalesOrder';
import Customer from '@js/api/basic/Customer';
import Supplier from '@js/api/basic/Supplier';
import Warehouse from '@js/api/basic/Warehouse';

export default {
  name: 'SalesDrivenDashboard',
  props: {
    // AppFrame 传入：当前标签页是否为激活状态，用于切换回本页时自动刷新
    dataActive: {
      type: Boolean,
      default: true
    }
  },
  watch: {
    dataActive(val) {
      if (val) this.loadList();
    }
  },
  data() {
    return {
      loading: false,
      dataList: [],
      query: {pendingStatus: null, customerName: '', orderNo: ''},
      dateRange: [],
      pagination: {page: 1, pageSize: 50, total: 0},
      selectedRowKeys: [],
      selectedRowsCache: {},
      expandedRowKeys: [],
      statusOptions: [
        {label: '待处理', value: 0},
        {label: '部分采购', value: 1},
        {label: '已采购', value: 2},
      ],
      customerList: [],
      supplierList: [],
      warehouseList: [],
      toOrderParams: {toOrderDefaultStatus: '草稿', toOrderAutoAudit: false},
      columns: [
        {colKey: 'row-select', type: 'multiple', width: 46},
        {colKey: 'salesOrderNo', title: '订单编号', width: 150},
        {colKey: 'customerName', title: '客户名称', width: 160},
        {colKey: 'orderDate', title: '订单日期', width: 110},
        {colKey: 'productCount', title: '商品数', width: 90},
        {colKey: 'pendingPurchaseQuantity', title: '待采购数量', width: 110},
        {colKey: 'supplierNames', title: '建议供应商', width: 180},
        {colKey: 'purchaseStatusText', title: '采购状态', width: 100},
        {colKey: 'ops', title: '操作', width: 90, fixed: 'right'},
      ],
      detailColumns: [
        {colKey: 'productCode', title: '商品编码', width: 110},
        {colKey: 'productName', title: '商品名称', width: 200},
        {colKey: 'specification', title: '规格型号', width: 120},
        {colKey: 'unitName', title: '单位', width: 70},
        {colKey: 'orderQuantity', title: '订单数量', width: 100},
        {colKey: 'pendingPurchaseQuantity', title: '待采购数量', width: 100},
        {colKey: 'supplierName', title: '供应商', width: 140},
      ],
      singleTransferColumns: [
        {colKey: 'productName', title: '商品名称', width: 180},
        {colKey: 'specification', title: '规格型号', width: 110},
        {colKey: 'unitName', title: '单位', width: 60},
        {colKey: 'orderQuantity', title: '订单数量', width: 90},
        {colKey: 'pendingPurchaseQuantity', title: '待采购数量', width: 90},
        {colKey: 'quantity', title: '本次采购数量', width: 130},
        {colKey: 'supplier', title: '供应商', width: 150},
        {colKey: 'purchasePrice', title: '采购单价', width: 130},
      ],
      batchTransferColumns: [
        {colKey: 'salesOrderNo', title: '订单编号', width: 130},
        {colKey: 'productName', title: '商品名称', width: 150},
        {colKey: 'specification', title: '规格型号', width: 90},
        {colKey: 'unitName', title: '单位', width: 55},
        {colKey: 'pendingPurchaseQuantity', title: '待采购数量', width: 85},
        {colKey: 'quantity', title: '本次采购数量', width: 115},
        {colKey: 'supplier', title: '供应商', width: 140},
        {colKey: 'purchasePrice', title: '采购单价', width: 115},
      ],
      // 单张转采购
      transferVisible: false,
      transferOrder: null,
      transferItems: [],
      transferExpectedDate: '',
      transferRemark: '',
      transferStatus: '草稿',
      transferring: false,
      // 批量转采购
      batchTransferVisible: false,
      batchTransferRows: [],
      batchTransferExpectedDate: '',
      batchTransferRemark: '',
      batchTransferStatus: '草稿',
      batchTransferring: false,
      // 批量设置供应商
      supplierDialogVisible: false,
      batchSupplierId: null,
      batchProductIds: [],
    };
  },
  computed: {
    selectedRows() {
      return this.selectedRowKeys.map(k => this.selectedRowsCache[k]).filter(Boolean);
    },
    selectedOrderCount() {
      return this.selectedRowKeys.length;
    },
    selectedProductCount() {
      const set = new Set();
      this.selectedRows.forEach(r => (r.items || []).forEach(it => set.add(it.productId)));
      return set.size;
    },
    totalPurchaseAmount() {
      let sum = 0;
      this.selectedRows.forEach(r => (r.items || []).forEach(it => {
        sum += (Number(it.pendingPurchaseQuantity) || 0) * (Number(it.lastPurchasePrice) || 0);
      }));
      return sum;
    },
    batchTransferOrderCount() {
      const set = new Set();
      this.batchTransferRows.forEach(r => set.add(r.salesOrderId));
      return set.size;
    },
  },
  created() {
    this.loadCustomerList();
    this.loadSupplierList();
    this.loadWarehouseList();
    this.loadParams();
    this.loadList();
  },
  methods: {
    ...mapMutations(['pushTab']),
    fmt(v) {
      const n = Number(v);
      return isNaN(n) ? '0.00' : n.toFixed(2);
    },
    getStatusTheme(status) {
      if (status === '待处理') return 'warning';
      if (status === '部分采购') return 'primary';
      if (status === '已采购') return 'success';
      return 'default';
    },
    loadCustomerList() {
      Customer.select().then(({data}) => {
        this.customerList = data || [];
      });
    },
    loadSupplierList() {
      Supplier.select().then(({data}) => {
        this.supplierList = data || [];
      });
    },
    loadWarehouseList() {
      Warehouse.select().then(({data}) => {
        this.warehouseList = data || [];
      });
    },
    loadParams() {
      SalesOrder.toOrderParams().then(({data}) => {
        if (data) this.toOrderParams = data;
      });
    },
    handleSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    handleReset() {
      this.query = {pendingStatus: null, customerName: '', orderNo: ''};
      this.dateRange = [];
      this.pagination.page = 1;
      this.loadList();
    },
    loadList() {
      this.loading = true;
      const params = {
        ...this.query,
        startDate: this.dateRange && this.dateRange[0] ? this.dateRange[0] : null,
        endDate: this.dateRange && this.dateRange[1] ? this.dateRange[1] : null,
        pageNum: this.pagination.page,
        pageSize: this.pagination.pageSize,
      };
      SalesOrder.toOrderList(params).then(({data}) => {
        this.dataList = data?.results || [];
        this.pagination.total = data?.total || 0;
        // 刷新选中行缓存（跨页保持勾选）
        const cache = {};
        this.dataList.forEach(r => {
          const existed = this.selectedRowsCache[r.salesOrderId];
          cache[r.salesOrderId] = existed ? {...r, items: r.items || existed.items} : r;
        });
        Object.keys(this.selectedRowsCache).forEach(k => {
          if (!cache[k]) cache[k] = this.selectedRowsCache[k];
        });
        this.selectedRowsCache = cache;
      }).catch(() => {
        this.dataList = [];
      }).finally(() => {
        this.loading = false;
      });
    },
    onSelectChange(keys) {
      this.selectedRowKeys = keys;
      const cache = {...this.selectedRowsCache};
      (keys || []).forEach(k => {
        const id = Number(k);
        if (!cache[id]) {
          const row = this.dataList.find(r => r.salesOrderId === id);
          if (row) cache[id] = row;
        }
      });
      Object.keys(cache).forEach(k => {
        if (!(keys || []).map(Number).includes(Number(k))) delete cache[k];
      });
      this.selectedRowsCache = cache;
    },
    onExpandChange(keys) {
      this.expandedRowKeys = keys;
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      if (pageInfo.pageSize) this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    goSalesOrder(row) {
      this.$store.commit('SET_TAB_DATA', {type: 'edit', orderId: row.salesOrderId});
      this.pushTab({
        key: 'SalesOrderForm',
        title: '编辑销售订单'
      });
    },
    /* ---------------- 单张转采购 ---------------- */
    openSingleTransfer(row) {
      this.transferOrder = row;
      this.transferItems = (row.items || []).map(it => ({
        goodsId: it.productId,
        productName: it.productName,
        specification: it.specification,
        unitName: it.unitName,
        orderQuantity: it.orderQuantity,
        pendingPurchaseQuantity: it.pendingPurchaseQuantity,
        quantity: Number(it.pendingPurchaseQuantity) || 0,
        supplierId: it.supplierId,
        purchasePrice: Number(it.lastPurchasePrice) || 0,
        warehouseId: it.warehouseId,
        baseUnitId: it.baseUnitId,
        conversionRate: it.conversionRate || 1,
      }));
      this.transferExpectedDate = '';
      this.transferRemark = '';
      this.transferStatus = this.toOrderParams.toOrderAutoAudit
        ? '已审核'
        : (this.toOrderParams.toOrderDefaultStatus || '草稿');
      this.transferVisible = true;
    },
    validateTransferRows(rows) {
      for (const it of rows) {
        const qty = Number(it.quantity);
        const pending = Number(it.pendingPurchaseQuantity);
        if (!qty || qty <= 0) {
          MessagePlugin.warning(`请填写“${it.productName}”的本次采购数量`);
          return false;
        }
        if (qty > pending) {
          MessagePlugin.warning(`“${it.productName}”本次采购数量不能大于待采购数量`);
          return false;
        }
        if (!it.supplierId) {
          MessagePlugin.warning(`请为“${it.productName}”设置默认供应商或手动选择供应商`);
          return false;
        }
      }
      return true;
    },
    buildTransferPayload(saleOrderId, rows) {
      return {
        saleOrderId,
        expectedDeliveryDate: this.transferExpectedDate || null,
        remark: this.transferRemark,
        orderStatus: this.transferStatus,
        autoAudit: this.transferStatus === '已审核',
        items: rows.map(it => ({
          goodsId: it.goodsId,
          quantity: it.quantity,
          supplierId: it.supplierId,
          purchasePrice: it.purchasePrice,
          warehouseId: it.warehouseId,
          baseUnitId: it.baseUnitId,
          conversionRate: it.conversionRate,
        })),
      };
    },
    doSingleTransfer() {
      if (!this.validateTransferRows(this.transferItems)) return;
      this.transferring = true;
      const payload = this.buildTransferPayload(this.transferOrder.salesOrderId, this.transferItems);
      SalesOrder.toOrderCreatePurchaseIn(payload).then(({data: ids}) => {
        this.transferVisible = false;
        MessagePlugin.success(`采购入库单已生成，单号数量：${(ids || []).length}`);
        this.loadList();
        if (ids && ids.length === 1) {
          this.pushTab({
            key: 'PurchaseInboundDetail',
            title: '采购入库单',
            params: {orderId: ids[0]}
          });
        }
      }).finally(() => {
        this.transferring = false;
      });
    },
    /* ---------------- 批量转采购 ---------------- */
    openBatchTransfer() {
      if (!this.selectedRows.length) return;
      this.batchTransferRows = [];
      this.selectedRows.forEach(r => {
        (r.items || []).forEach(it => {
          this.batchTransferRows.push({
            salesOrderId_goodsId: r.salesOrderId + '_' + it.productId,
            salesOrderId: r.salesOrderId,
            salesOrderNo: r.salesOrderNo,
            customerName: r.customerName,
            goodsId: it.productId,
            productName: it.productName,
            specification: it.specification,
            unitName: it.unitName,
            orderQuantity: it.orderQuantity,
            pendingPurchaseQuantity: it.pendingPurchaseQuantity,
            quantity: Number(it.pendingPurchaseQuantity) || 0,
            supplierId: it.supplierId,
            purchasePrice: Number(it.lastPurchasePrice) || 0,
            warehouseId: it.warehouseId,
            baseUnitId: it.baseUnitId,
            conversionRate: it.conversionRate || 1,
          });
        });
      });
      this.batchTransferExpectedDate = '';
      this.batchTransferRemark = '';
      this.batchTransferStatus = this.toOrderParams.toOrderAutoAudit
        ? '已审核'
        : (this.toOrderParams.toOrderDefaultStatus || '草稿');
      this.batchTransferVisible = true;
    },
    async doBatchTransfer() {
      if (!this.validateTransferRows(this.batchTransferRows)) return;
      this.batchTransferring = true;
      try {
        // 按订单分组，逐订单调用（后端按供应商自动拆分）
        const grouped = {};
        this.batchTransferRows.forEach(r => {
          if (!grouped[r.salesOrderId]) grouped[r.salesOrderId] = [];
          grouped[r.salesOrderId].push(r);
        });
        let totalIds = [];
        for (const saleOrderId of Object.keys(grouped)) {
          const payload = this.buildTransferPayload(Number(saleOrderId), grouped[saleOrderId]);
          const {data: ids} = await SalesOrder.toOrderCreatePurchaseIn(payload);
          totalIds = totalIds.concat(ids || []);
        }
        this.batchTransferVisible = false;
        this.selectedRowKeys = [];
        this.selectedRowsCache = {};
        MessagePlugin.success(`已生成 ${totalIds.length} 张采购入库单`);
        this.loadList();
      } finally {
        this.batchTransferring = false;
      }
    },
    /* ---------------- 批量设置供应商 ---------------- */
    openBatchSupplier() {
      const set = new Set();
      this.selectedRows.forEach(r => (r.items || []).forEach(it => set.add(it.productId)));
      this.batchProductIds = [...set];
      this.batchSupplierId = null;
      this.supplierDialogVisible = true;
    },
    doBatchSupplier() {
      if (!this.batchSupplierId) {
        MessagePlugin.warning('请选择供应商');
        return;
      }
      if (!this.batchProductIds.length) {
        MessagePlugin.warning('没有可设置的商品');
        return;
      }
      SalesOrder.toOrderBatchSetSupplier(this.batchSupplierId, this.batchProductIds).then(() => {
        this.supplierDialogVisible = false;
        MessagePlugin.success('批量设置供应商成功');
        this.loadList();
      });
    },
  },
};
</script>

<style scoped>
/* 卡片占满页面高度，内部纵向布局，超长时表格区滚动 */
.to-order-card {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.to-order-card :deep(.t-card__body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 16px;
}
.table-area {
  flex: 1;
  min-height: 0;
  overflow: auto;
  margin-top: 12px;
}
.filter-bar {
  flex-shrink: 0;
  margin-bottom: 12px;
}
.op-bar {
  flex-shrink: 0;
  margin-bottom: 12px;
}
.muted {
  color: #999;
}
.strong {
  font-weight: 600;
}
.amount {
  color: #e34d59;
}
.empty-tip {
  padding: 24px 0;
  color: #999;
}
.summary-bar {
  flex-shrink: 0;
  margin-top: 12px;
  padding: 10px 16px;
  background: #f5f7fa;
  border-radius: 6px;
  display: inline-block;
}
.expanded-preview {
  padding: 8px 16px 16px 48px;
}
.expanded-preview__title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #333;
}
.dialog-order-info {
  display: flex;
  gap: 24px;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 13px;
}
.dialog-form-row {
  margin: 12px 0;
}
.dialog-form-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-right: 20px;
}
.dialog-form-item label {
  color: #333;
  white-space: nowrap;
}
</style>
