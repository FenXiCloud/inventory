<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-input
            v-model="params.filter"
            clearable
            placeholder="请输入编码、名称"
            style="width: 240px; border-radius: 4px"
            @enter="doSearch"
        >
          <template #suffixIcon>
            <t-icon name="search" style="cursor:pointer" @click="doSearch"/>
          </template>
        </t-input>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="doSearch">查询</t-button>
        <span class="simple-page__tip">提示：单击价格单元格可直接编辑</span>
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
      >
        <template #purchasePrice="{ row }">
          <t-input-number
              v-if="isEditing(row.id, 'purchasePrice')"
              :ref="el => setEditRef(el, row)"
              v-model="editValue"
              theme="normal"
              :min="0"
              :decimal-places="2"
              style="width: 100%"
              @blur="commitEdit(row)"
          />
          <span v-else class="price-cell" @click.stop="startEdit(row, 'purchasePrice')">{{ formatPrice(row.purchasePrice) }}</span>
        </template>
        <template
            v-for="level in customerLevels"
            :key="'slot-' + level.id"
            #[`level_${level.id}`]="{ row }"
        >
          <t-input-number
              v-if="isEditing(row.id, `level_${level.id}`)"
              :ref="el => setEditRef(el, row)"
              v-model="editValue"
              theme="normal"
              :min="0"
              :decimal-places="2"
              style="width: 100%"
              @blur="commitEdit(row)"
          />
          <span v-else class="price-cell" @click.stop="startEdit(row, `level_${level.id}`)">{{ formatPrice(row[`level_${level.id}`]) }}</span>
        </template>
      </t-table>
    </div>

    <div class="simple-page__pager">
      <t-pagination
          v-model:current="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :show-jumper="true"
          :show-page-size="true"
          :popup-props="{ attach: 'body' }"
          @change="onPageChange"
      />
    </div>
  </div>
</template>

<script>
import {MessagePlugin} from 'tdesign-vue-next';
import PriceRecord from '@js/api/basic/PriceRecord';
import CustomerLevel from '@js/api/basic/CustomerLevel';

export default {
  name: 'ProductPriceList',
  data() {
    return {
      loading: false,
      dataList: [],
      customerLevels: [],
      params: {filter: ''},
      pagination: {page: 1, pageSize: 10, total: 0},
      editing: null, // {rowId, colKey}
      editValue: 0,
      editRef: null,
      saving: false,
    };
  },
  computed: {
    columns() {
      const cols = [
        {colKey: 'code', title: '编码', width: 110, fixed: 'left', ellipsis: true},
        {colKey: 'name', title: '名称', minWidth: 140, fixed: 'left', ellipsis: true},
        {colKey: 'productCategoryName', title: '产品类别', minWidth: 110, ellipsis: true},
        {colKey: 'specification', title: '规格', width: 100, ellipsis: true},
        {colKey: 'unitName', title: '单位', width: 70, align: 'center'},
        {
          colKey: 'purchasePrice',
          title: '默认采购价',
          width: 120,
          align: 'right',
          className: 'price-col',
        },
      ];
      (this.customerLevels || []).forEach((level) => {
        cols.push({
          colKey: `level_${level.id}`,
          title: level.name,
          width: 120,
          align: 'right',
          className: 'price-col',
        });
      });
      return cols;
    },
  },
  methods: {
    formatPrice(val) {
      const n = Number(val);
      if (Number.isNaN(n)) return '0.00';
      return n.toFixed(2);
    },
    isEditing(rowId, colKey) {
      return this.editing && this.editing.rowId === rowId && this.editing.colKey === colKey;
    },
    setEditRef(el, row) {
      this.editRef = el;
      if (!el) return;
      this.$nextTick(() => {
        const input = el?.$el?.querySelector?.('input') || el?.querySelector?.('input');
        if (!input) return;
        input.focus?.();
        input.select?.();
        if (input._priceKeyHandler) {
          input.removeEventListener('keydown', input._priceKeyHandler);
        }
        input._priceKeyHandler = (e) => {
          if (e.key === 'Enter') {
            e.preventDefault();
            this.commitEdit(row);
          } else if (e.key === 'Escape') {
            e.preventDefault();
            this.cancelEdit();
          }
        };
        input.addEventListener('keydown', input._priceKeyHandler);
      });
    },
    onCellDblclick({row, col}) {
      const colKey = col?.colKey;
      if (!colKey || !row?.id) return;
      this.startEdit(row, colKey);
    },
    startEdit(row, colKey) {
      if (!colKey || !row?.id) return;
      if (colKey !== 'purchasePrice' && !colKey.startsWith('level_')) return;
      if (this.saving) return;
      this.editing = {rowId: row.id, colKey};
      this.editValue = Number(row[colKey] ?? 0);
    },
    cancelEdit() {
      this.editing = null;
      this.editValue = 0;
    },
    commitEdit(row) {
      if (!this.editing || this.saving) return;
      const {colKey} = this.editing;
      const next = Number(this.editValue);
      if (Number.isNaN(next) || next < 0) {
        MessagePlugin.warning('请输入有效价格');
        return;
      }
      const prev = Number(row[colKey] ?? 0);
      if (Number(prev.toFixed(2)) === Number(next.toFixed(2))) {
        this.cancelEdit();
        return;
      }

      const payload = {
        productId: row.id,
        price: next,
      };
      if (colKey === 'purchasePrice') {
        payload.field = 'purchasePrice';
      } else if (colKey.startsWith('level_')) {
        payload.field = 'levelPrice';
        payload.customerLevelId = Number(colKey.replace('level_', ''));
      } else {
        this.cancelEdit();
        return;
      }

      this.saving = true;
      PriceRecord.productCellSave(payload)
          .then(() => {
            row[colKey] = next;
            if (colKey === 'purchasePrice') {
              row.purchasePrice = next;
            } else if (!row.levelPrices) {
              row.levelPrices = {};
            }
            if (payload.customerLevelId != null) {
              row.levelPrices[payload.customerLevelId] = next;
            }
            MessagePlugin.success('已保存');
            this.cancelEdit();
          })
          .catch(() => {
            // 失败时保持编辑态，便于重试
          })
          .finally(() => {
            this.saving = false;
          });
    },
    flattenRow(row) {
      const item = {...row};
      const map = row.levelPrices || {};
      (this.customerLevels || []).forEach((level) => {
        const v = map[level.id] ?? map[String(level.id)];
        item[`level_${level.id}`] = v != null ? Number(v) : 0;
      });
      item.purchasePrice = item.purchasePrice != null ? Number(item.purchasePrice) : 0;
      return item;
    },
    loadLevels() {
      return CustomerLevel.select().then(({data}) => {
        this.customerLevels = data || [];
      });
    },
    loadList() {
      this.loading = true;
      const query = {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
      };
      if (this.params.filter) query.filter = this.params.filter;
      PriceRecord.productList(query)
          .then(({data: {results, total}}) => {
            this.dataList = (results || []).map((r) => this.flattenRow(r));
            this.pagination.total = total || 0;
          })
          .finally(() => (this.loading = false));
    },
    doSearch() {
      this.pagination.page = 1;
      this.cancelEdit();
      this.loadList();
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.cancelEdit();
      this.loadList();
    },
  },
  created() {
    this.loadLevels().then(() => this.loadList());
  },
};
</script>

<style scoped>
.simple-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
  padding: 0 12px;
  box-sizing: border-box;
  overflow: hidden;
}

.simple-page__toolbar {
  flex-shrink: 0;
  padding: 8px 0;
}

.simple-page__tip {
  color: var(--td-text-color-secondary, #8b8b8b);
  font-size: 13px;
}

.simple-page__table {
  flex: 1 1 0;
  height: 0;
  min-height: 0;
  overflow: hidden;
}

.simple-page__pager {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid var(--td-component-border, #dcdcdc);
}

.price-cell {
  display: inline-block;
  width: 100%;
  text-align: right;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 2px;
}

.price-cell:hover {
  background: var(--td-bg-color-container-hover, #f3f3f3);
}
</style>
