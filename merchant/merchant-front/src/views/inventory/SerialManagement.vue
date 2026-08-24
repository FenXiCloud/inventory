<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="openRegister">登记序列号</t-button>
        <t-button style="border-radius: 4px" @click="openOutbound">出 库</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="doScrap">报 废</t-button>
        <t-select
            v-model="params.status"
            :options="statusOptions"
            clearable
            placeholder="状态"
            style="width: 140px; border-radius: 4px"
        />
        <t-input
            v-model="params.keyword"
            clearable
            placeholder="序列号"
            style="width: 200px; background: #fff; border-radius: 4px"
            @enter="doSearch"
        >
          <template #suffixIcon>
            <t-icon name="search" style="cursor:pointer" @click="doSearch"/>
          </template>
        </t-input>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="doSearch">查询</t-button>
      </t-space>
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
        <template #status="{ row }">
          <t-tag :theme="statusTheme(row.status)" variant="light">{{ row.status }}</t-tag>
        </template>
        <template #ops="{ row }">
          <t-link theme="danger" @click="doRemove(row)">删除</t-link>
        </template>
      </t-table>
    </div>

    <!-- 登记序列号 -->
    <t-dialog v-model:visible="registerVisible" header="登记序列号" width="560px" :confirm-btn="{ content: '保存', loading: saving }" @confirm="submitRegister">
      <t-form label-width="90px" :colon="false">
        <t-form-item label="产品">
          <t-select v-model="registerForm.productId" :options="productList" :keys="{ value: 'id', label: 'name' }" filterable clearable placeholder="请选择产品"/>
        </t-form-item>
        <t-form-item label="仓库">
          <t-select v-model="registerForm.warehouseId" :options="warehouseList" :keys="{ value: 'id', label: 'name' }" filterable clearable placeholder="请选择仓库"/>
        </t-form-item>
        <t-form-item label="批次号">
          <t-input v-model="registerForm.batchNumber" placeholder="可选"/>
        </t-form-item>
        <t-form-item label="入库日期">
          <t-date-picker v-model="registerForm.inboundDate" :value-type="'YYYY-MM-DD'" clearable placeholder="入库日期" style="width: 100%"/>
        </t-form-item>
        <t-form-item label="序列号">
          <t-textarea v-model="registerForm.serialNumbers" placeholder="多个序列号用逗号或换行分隔" :autosize="{ minRows: 3 }"/>
        </t-form-item>
        <t-form-item label="备注">
          <t-input v-model="registerForm.remark" placeholder="可选"/>
        </t-form-item>
      </t-form>
    </t-dialog>

    <!-- 出库核销 -->
    <t-dialog v-model:visible="outboundVisible" header="序列号出库" width="480px" :confirm-btn="{ content: '确认出库', loading: saving }" @confirm="submitOutbound">
      <t-form label-width="90px" :colon="false">
        <t-form-item label="出库日期">
          <t-date-picker v-model="outboundForm.outboundDate" :value-type="'YYYY-MM-DD'" clearable placeholder="出库日期" style="width: 100%"/>
        </t-form-item>
        <t-form-item label="出库单号">
          <t-input v-model="outboundForm.outboundOrderId" placeholder="可选，关联销售出库单ID"/>
        </t-form-item>
        <t-form-item label="已选序列号">
          <span>{{ selectedRows.length }} 个（仅对「在库」序列号生效）</span>
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script>
import {MessagePlugin, DialogPlugin} from 'tdesign-vue-next';
import Product from '@js/api/basic/Product';
import Warehouse from '@js/api/basic/Warehouse';
import Serial from '@js/api/inventory/Serial';

export default {
  name: 'SerialManagement',
  data() {
    return {
      dataList: [],
      productList: [],
      warehouseList: [],
      selectedRowKeys: [],
      selectedRows: [],
      loading: false,
      saving: false,
      registerVisible: false,
      outboundVisible: false,
      params: {
        status: null,
        keyword: null
      },
      statusOptions: [
        {label: '在库', value: '在库'},
        {label: '已出库', value: '已出库'},
        {label: '报废', value: '报废'},
      ],
      registerForm: {
        productId: null,
        warehouseId: null,
        batchNumber: '',
        serialNumbers: '',
        inboundDate: null,
        remark: ''
      },
      outboundForm: {
        outboundDate: null,
        outboundOrderId: ''
      },
      columns: [
        {colKey: 'row-select', type: 'multiple', width: 46},
        {colKey: 'serialNumber', title: '序列号', minWidth: 160, ellipsis: true},
        {colKey: 'productCode', title: '产品编码', width: 120, ellipsis: true},
        {colKey: 'productName', title: '产品名称', minWidth: 140, ellipsis: true},
        {colKey: 'productSpecification', title: '规格型号', width: 120, ellipsis: true},
        {colKey: 'warehouseName', title: '仓库', width: 110, ellipsis: true},
        {colKey: 'batchNumber', title: '批次号', width: 120, ellipsis: true},
        {colKey: 'status', title: '状态', width: 90, align: 'center'},
        {colKey: 'inboundDate', title: '入库日期', width: 110, align: 'center'},
        {colKey: 'outboundDate', title: '出库日期', width: 110, align: 'center'},
        {colKey: 'remark', title: '备注', width: 120, ellipsis: true},
        {colKey: 'ops', title: '操作', width: 70, align: 'center', fixed: 'right'},
      ]
    };
  },
  methods: {
    statusTheme(s) {
      if (s === '在库') return 'success';
      if (s === '报废') return 'danger';
      return 'warning';
    },
    onSelectChange(keys, {selectedRowData}) {
      this.selectedRowKeys = keys;
      this.selectedRows = selectedRowData || [];
    },
    openRegister() {
      this.registerForm = {productId: null, warehouseId: null, batchNumber: '', serialNumbers: '', inboundDate: null, remark: ''};
      this.registerVisible = true;
    },
    submitRegister() {
      if (!this.registerForm.productId) return MessagePlugin.warning('请选择产品');
      if (!this.registerForm.serialNumbers.trim()) return MessagePlugin.warning('请输入序列号');
      this.saving = true;
      Serial.register(this.registerForm)
        .then(({data}) => {
          MessagePlugin.success(`成功登记 ${data} 个序列号`);
          this.registerVisible = false;
          this.loadList();
        })
        .finally(() => (this.saving = false));
    },
    openOutbound() {
      if (!this.selectedRows.length) return MessagePlugin.warning('请先选择要出库的序列号');
      this.outboundForm = {outboundDate: null, outboundOrderId: ''};
      this.outboundVisible = true;
    },
    submitOutbound() {
      this.saving = true;
      const payload = {
        ids: this.selectedRows.map(r => r.id),
        outboundDate: this.outboundForm.outboundDate,
        outboundOrderId: this.outboundForm.outboundOrderId ? Number(this.outboundForm.outboundOrderId) : null
      };
      Serial.outbound(payload)
        .then(({data}) => {
          MessagePlugin.success(`成功出库 ${data} 个序列号`);
          this.outboundVisible = false;
          this.clearSelection();
          this.loadList();
        })
        .finally(() => (this.saving = false));
    },
    doScrap() {
      if (!this.selectedRows.length) return MessagePlugin.warning('请先选择要报废的序列号');
      const ids = this.selectedRows.map(r => r.id);
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认报废选中的 ${ids.length} 个序列号？`,
        onConfirm: () => {
          return Serial.scrap(ids).then(({data}) => {
            MessagePlugin.success(`成功报废 ${data} 个序列号`);
            this.clearSelection();
            this.loadList();
          });
        }
      });
    },
    doRemove(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除序列号：${row.serialNumber}?`,
        onConfirm: () => {
          return Serial.remove(row.id).then(() => {
            MessagePlugin.success('删除成功~');
            this.loadList();
          });
        }
      });
    },
    clearSelection() {
      this.selectedRowKeys = [];
      this.selectedRows = [];
    },
    doSearch() {
      this.clearSelection();
      this.loadList();
    },
    loadList() {
      this.loading = true;
      Serial.list(this.params).then(({data}) => {
        this.dataList = data || [];
      }).finally(() => (this.loading = false));
    },
    loadProducts() {
      Product.select().then(({data}) => { this.productList = data || []; });
    },
    loadWarehouses() {
      Warehouse.select().then(({data}) => { this.warehouseList = data || []; });
    }
  },
  created() {
    this.loadProducts();
    this.loadWarehouses();
    this.loadList();
  }
};
</script>
