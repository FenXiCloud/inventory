<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <t-table
          row-key="_rowKey"
          size="small"
          bordered
          stripe
          hover
          table-layout="fixed"
          :data="templateData"
          :columns="columns"
          max-height="420"
      >
        <template #ops="{ rowIndex }">
          <t-space size="small">
            <t-link theme="primary" @click="adjustRows('insert', rowIndex)">增加</t-link>
            <t-link
                v-if="templateData.length > 1"
                theme="danger"
                @click="adjustRows('delete', rowIndex)"
            >删除</t-link>
          </t-space>
        </template>
        <template #financeId="{ row, rowIndex }">
          <t-select
              v-model="row.financeId"
              :options="financeItemMappings"
              :keys="{ value: 'id', label: 'titleName' }"
              filterable
              :clearable="false"
              placeholder="请选择财务软件辅助项"
              style="width: 100%"
              @change="() => changeMappings(rowIndex, 'financeId')"
          />
        </template>
        <template #inventoryId="{ row, rowIndex }">
          <t-select
              v-model="row.inventoryId"
              :options="itemMappings"
              :keys="{ value: 'id', label: 'titleName' }"
              filterable
              :clearable="false"
              placeholder="请选择进销存辅助项"
              style="width: 100%"
              @change="() => changeMappings(rowIndex, 'inventoryId')"
          />
        </template>
      </t-table>
    </div>
    <div class="modal-column-between">
      <t-button variant="outline" :loading="loading" @click="$emit('close')">取消</t-button>
      <t-button theme="primary" :loading="loading" @click="confirm">保存</t-button>
    </div>
  </div>
</template>

<script>
import FinanceAccountLink from '@js/api/setting/FinanceAccountLink';
import FinanceItemMap from '@js/api/setting/FinanceItemMap';
import Customer from '@js/api/basic/Customer';
import Product from '@js/api/basic/Product';
import Supplier from '@js/api/basic/Supplier';
import { MessagePlugin } from 'tdesign-vue-next';
import { ObjectUtil } from '@common/utils';

let rowSeq = 0;

function newRow(extra = {}) {
  return { _rowKey: `r-${++rowSeq}`, inventoryId: null, financeId: null, ...extra };
}

export default {
  name: 'FinanceItemMapBatchForm',
  props: {
    id: [Number, String],
    categoryId: [Number, String]
  },
  emits: ['close', 'success'],
  data() {
    return {
      loading: false,
      model: {
        id: null,
        categoryId: null,
        inventoryId: null,
        inventoryCode: null,
        inventoryName: null,
        financeId: null,
        financeCode: null,
        financeName: null
      },
      itemMappings: [],
      financeItemMappings: [],
      templateData: [],
      columns: [
        { colKey: 'ops', title: '操作', width: 110, align: 'center' },
        { colKey: 'financeId', title: '财务软件辅助项', minWidth: 200 },
        { colKey: 'inventoryId', title: '进销存辅助项', minWidth: 200 }
      ]
    };
  },
  methods: {
    confirm() {
      if (!this.templateData.length) {
        MessagePlugin.info('请添加数据~');
        return;
      }
      const filter = this.templateData.filter(
        (item) => !ObjectUtil.isEmpty(item.financeId) || !ObjectUtil.isEmpty(item.inventoryId)
      );
      let incomplete = false;
      filter.forEach((item) => {
        if (ObjectUtil.isEmpty(item.financeId) || ObjectUtil.isEmpty(item.inventoryId)) {
          incomplete = true;
        }
        item.categoryType = this.model.categoryType;
        item.categoryId = this.model.categoryId;
        item.categoryName = this.model.categoryName;
      });
      if (incomplete) {
        MessagePlugin.warning('请选择辅助项～');
        return;
      }
      this.loading = true;
      FinanceItemMap.batch(filter)
        .then(() => {
          MessagePlugin.success('保存成功~');
          this.$emit('success');
        })
        .finally(() => (this.loading = false));
    },
    init() {
      const categoryId = this.categoryId;
      FinanceAccountLink.accountingCategory({ ids: this.categoryId }).then(({ data }) => {
        const category = data.data[0];
        this.model.categoryType = category.categoryType;
        this.model.categoryId = category.id;
        this.model.categoryName = category.name;
        this.financeItemMappings = (category.details || []).map((item) => ({
          ...item,
          titleName: `${item.code} - ${item.name}`
        }));
      });
      switch (Number(categoryId)) {
        case 0:
          Customer.list().then(({ data }) => {
            this.itemMappings = (data.results || []).map((item) => ({
              ...item,
              titleName: `${item.name}`
            }));
          });
          break;
        case 1:
          Supplier.list().then(({ data }) => {
            this.itemMappings = (data.results || []).map((item) => ({
              ...item,
              titleName: `${item.code} - ${item.name}`
            }));
          });
          break;
        case 6:
          Product.list().then(({ data }) => {
            this.itemMappings = (data.results || []).map((item) => ({
              ...item,
              titleName: `${item.code} - ${item.name}`
            }));
          });
          break;
      }
    },
    changeMappings(rowIndex, type) {
      const row = this.templateData[rowIndex];
      if (!row) return;
      if (type === 'financeId') {
        const item = this.financeItemMappings.find((x) => x.id === row.financeId);
        if (!item) return;
        row.financeName = item.name;
        row.financeCode = item.code;
      } else if (type === 'inventoryId') {
        const item = this.itemMappings.find((x) => x.id === row.inventoryId);
        if (!item) return;
        row.inventoryName = item.name;
        row.inventoryCode = item.code;
      }
    },
    load() {
      FinanceItemMap.load(this.id).then(({ data }) => {
        this.model = data;
      });
    },
    adjustRows(type, index) {
      if (type === 'insert') {
        this.templateData.splice(index + 1, 0, newRow());
      } else {
        this.templateData.splice(index, 1);
      }
    }
  },
  created() {
    this.init();
    if (this.id) {
      this.load();
    } else {
      for (let i = 0; i < 5; i++) {
        this.templateData.push(newRow());
      }
    }
  }
};
</script>
