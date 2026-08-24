<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-select
            v-model="statusFilter"
            :options="statusOptions"
            clearable
            placeholder="效期状态"
            style="width: 160px; border-radius: 4px"
        />
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="loadList">刷新</t-button>
      </t-space>
    </div>

    <div class="simple-page__hint">
      在此维护采购入库明细的批次号、生产日期与有效期至；到期 30 天内或已过期的批次会被标记，便于及时处理。
    </div>

    <div class="simple-page__table">
      <t-table
          row-key="itemId"
          size="medium"
          bordered
          stripe
          hover
          height="100%"
          table-layout="fixed"
          :data="dataList"
          :columns="columns"
          :loading="loading"
      >
        <template #expiryStatus="{ row }">
          <t-tag :theme="statusTheme(row.expiryStatus)" variant="light">{{ row.expiryStatus || '未登记' }}</t-tag>
        </template>
        <template #ops="{ row }">
          <t-link theme="primary" @click="openEdit(row)">维护效期</t-link>
        </template>
      </t-table>
    </div>

    <t-dialog v-model:visible="editVisible" header="维护效期" width="480px" :confirm-btn="{ content: '保存', loading: saving }" @confirm="submitEdit">
      <t-form label-width="90px" :colon="false">
        <t-form-item label="产品">
          <span>{{ editForm.productName }}</span>
        </t-form-item>
        <t-form-item label="批次号">
          <t-input v-model="editForm.batchNumber" placeholder="批次号"/>
        </t-form-item>
        <t-form-item label="生产日期">
          <t-date-picker v-model="editForm.productionDate" :value-type="'YYYY-MM-DD'" clearable placeholder="生产日期" style="width: 100%"/>
        </t-form-item>
        <t-form-item label="有效期至">
          <t-date-picker v-model="editForm.expiryDate" :value-type="'YYYY-MM-DD'" clearable placeholder="有效期至" style="width: 100%"/>
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script>
import {MessagePlugin} from 'tdesign-vue-next';
import InventoryReport from '@js/api/inventory/InventoryReport';

export default {
  name: 'ShelfLifeList',
  data() {
    return {
      dataList: [],
      loading: false,
      saving: false,
      editVisible: false,
      statusFilter: null,
      statusOptions: [
        {label: '未登记', value: '未登记'},
        {label: '正常', value: '正常'},
        {label: '即将到期', value: '即将到期'},
        {label: '已过期', value: '已过期'},
      ],
      editForm: {
        itemId: null,
        productName: '',
        batchNumber: '',
        productionDate: null,
        expiryDate: null
      },
      columns: [
        {colKey: 'productCode', title: '产品编码', minWidth: 120, ellipsis: true},
        {colKey: 'productName', title: '产品名称', minWidth: 140, ellipsis: true},
        {colKey: 'productSpecification', title: '规格型号', width: 120, ellipsis: true},
        {colKey: 'warehouseName', title: '仓库', width: 110, ellipsis: true},
        {colKey: 'batchNumber', title: '批次号', width: 130, ellipsis: true},
        {colKey: 'productionDate', title: '生产日期', width: 110, align: 'center'},
        {colKey: 'expiryDate', title: '有效期至', width: 110, align: 'center'},
        {colKey: 'quantity', title: '数量', width: 90, align: 'right'},
        {colKey: 'daysToExpiry', title: '剩余天数', width: 90, align: 'right'},
        {colKey: 'expiryStatus', title: '状态', width: 100, align: 'center'},
        {colKey: 'ops', title: '操作', width: 90, align: 'center', fixed: 'right'},
      ]
    };
  },
  computed: {
    filteredList() {
      if (!this.statusFilter) return this.dataList;
      return this.dataList.filter(r => (r.expiryStatus || '未登记') === this.statusFilter);
    }
  },
  methods: {
    statusTheme(s) {
      if (s === '已过期') return 'danger';
      if (s === '即将到期') return 'warning';
      if (s === '正常') return 'success';
      return 'default';
    },
    openEdit(row) {
      this.editForm = {
        itemId: row.itemId,
        productName: row.productName,
        batchNumber: row.batchNumber || '',
        productionDate: row.productionDate,
        expiryDate: row.expiryDate
      };
      this.editVisible = true;
    },
    submitEdit() {
      this.saving = true;
      InventoryReport.updateShelfLife(this.editForm.itemId, {
        batchNumber: this.editForm.batchNumber,
        productionDate: this.editForm.productionDate,
        expiryDate: this.editForm.expiryDate
      }).then(() => {
        MessagePlugin.success('保存成功');
        this.editVisible = false;
        this.loadList();
      }).finally(() => (this.saving = false));
    },
    loadList() {
      this.loading = true;
      InventoryReport.shelfLife().then(({data}) => {
        this.dataList = data || [];
      }).finally(() => (this.loading = false));
    }
  },
  created() {
    this.loadList();
  }
};
</script>

<style scoped>
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
