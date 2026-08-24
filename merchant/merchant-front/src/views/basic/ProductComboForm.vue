<template>
  <div class="modal-column product-combo-form">
    <div class="modal-column-full-body">
      <t-form ref="form" :data="model" :rules="rules" layout="vertical" label-align="top">
        <t-row :gutter="16">
          <t-col :span="6">
            <t-form-item label="套餐名称" name="name">
              <t-input v-model="model.name" placeholder="请输入套餐名称" :maxlength="255"/>
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="套餐编码" name="code">
              <t-input v-model="model.code" placeholder="不填自动生成" :maxlength="255"/>
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="是否启用" name="enabled">
              <t-radio-group v-model="model.enabled">
                <t-radio :value="true">启用</t-radio>
                <t-radio :value="false">禁用</t-radio>
              </t-radio-group>
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="备注" name="remarks">
              <t-input v-model="model.remarks" placeholder="备注" :maxlength="150"/>
            </t-form-item>
          </t-col>
        </t-row>
      </t-form>

      <div class="product-combo-form__items-title">套餐组件（{{ items.filter(i => i.productId).length }} 个）</div>
      <t-table
          row-key="_rowKey"
          size="small"
          bordered
          :data="items"
          :columns="itemColumns"
      >
        <template #product="{ row }">
          <t-select
              v-model="row.productId"
              :options="productList"
              :keys="{ value: 'id', label: 'customName' }"
              filterable
              placeholder="输入编码/名称"
              @change="(v) => onSelectProduct(row, v)"
          />
        </template>
        <template #unitName="{ row }">
          <span>{{ productUnitName(row) }}</span>
        </template>
        <template #unitPrice="{ row }">
          <span>¥{{ fmtMoney(productUnitPrice(row)) }}</span>
        </template>
        <template #quantity="{ row }">
          <t-input-number v-model="row.quantity" theme="normal" :min="0" :decimal-places="2" style="width: 100%"/>
        </template>
        <template #amount="{ row }">
          <span>¥{{ fmtMoney(itemAmount(row)) }}</span>
        </template>
        <template #ops="{ row }">
          <t-link theme="danger" @click="removeItem(row)">删除</t-link>
        </template>
      </t-table>
      <div class="product-combo-form__add">
        <t-button size="small" variant="outline" @click="addItem">添加组件</t-button>
      </div>
    </div>

    <div class="modal-column-between">
      <t-button variant="outline" :loading="loading" @click="$emit('close')">取消</t-button>
      <t-button theme="primary" :loading="loading" @click="save">保存</t-button>
    </div>
  </div>
</template>

<script>
import {MessagePlugin} from 'tdesign-vue-next';
import ProductCombo from '@js/api/basic/ProductCombo';
import Product from '@js/api/basic/Product';

let rowSeq = 0;
function newItem(extra = {}) {
  return { _rowKey: `ci-${++rowSeq}`, productId: null, quantity: 1, baseUnitId: null, ...extra };
}

export default {
  name: 'ProductComboForm',
  emits: ['close', 'success'],
  props: {
    entity: Object
  },
  data() {
    return {
      loading: false,
      productList: [],
      model: {
        id: null,
        name: null,
        code: null,
        enabled: true,
        remarks: null
      },
      items: [newItem()],
      rules: {
        name: [{required: true, message: '请输入套餐名称', type: 'error', trigger: 'blur'}]
      },
      itemColumns: [
        {colKey: 'product', title: '组件商品', minWidth: 220},
        {colKey: 'unitName', title: '单位', width: 90, align: 'center'},
        {colKey: 'unitPrice', title: '单价', width: 110, align: 'right'},
        {colKey: 'quantity', title: '数量', width: 120},
        {colKey: 'amount', title: '金额', width: 120, align: 'right'},
        {colKey: 'ops', title: '操作', width: 70, align: 'center'}
      ]
    };
  },
  methods: {
    fmtMoney(v) {
      const n = Number(v) || 0;
      return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    },
    productOf(row) {
      return (this.productList || []).find((i) => String(i.id) === String(row.productId));
    },
    onSelectProduct(row, value) {
      const p = this.productOf(row);
      if (!p) return;
      const dup = (this.items || []).find((i) => i !== row && String(i.productId) === String(p.id));
      if (dup) {
        MessagePlugin.warning('该商品已添加为组件，请勿重复添加');
        row.productId = null;
        row.baseUnitId = null;
        return;
      }
      row.baseUnitId = p.unitId;
    },
    productUnitName(row) {
      return this.productOf(row)?.unitName || '-';
    },
    productUnitPrice(row) {
      return this.productOf(row)?.purchasePrice || 0;
    },
    itemAmount(row) {
      return (Number(row.quantity) || 0) * (Number(this.productOf(row)?.purchasePrice) || 0);
    },
    addItem() {
      this.items.push(newItem());
    },
    removeItem(row) {
      const idx = this.items.findIndex((i) => i._rowKey === row._rowKey);
      if (idx >= 0) this.items.splice(idx, 1);
      if (!this.items.length) this.items.push(newItem());
    },
    save() {
      this.$refs.form.validate().then((result) => {
        if (result !== true) return;
        const comboItemList = this.items
          .filter((i) => i.productId != null)
          .map(({ _rowKey, ...rest }) => rest);
        if (!comboItemList.length) {
          MessagePlugin.warning('请至少添加一个组件');
          return;
        }
        this.loading = true;
        ProductCombo.save({ combo: this.model, comboItemList })
          .then(() => {
            MessagePlugin.success('保存成功~');
            this.$emit('success');
          })
          .finally(() => (this.loading = false));
      }).catch(() => {});
    }
  },
  created() {
    Product.select().then(({data}) => {
      this.productList = (data || []).map((p) => ({
        ...p,
        customName: [p.name, p.code, p.specification].filter(Boolean).join(' | ')
      }));
    });
    if (this.entity && this.entity.id) {
      ProductCombo.load(this.entity.id).then(({data}) => {
        if (!data) return;
        if (data.combo) Object.assign(this.model, data.combo);
        const list = data.comboItemList || [];
        this.items = list.length ? list.map((i) => newItem({ ...i })) : [newItem()];
      });
    }
  }
};
</script>

<style scoped>
.product-combo-form {
  background: #fff;
}

.product-combo-form__items-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
  margin: 8px 0;
}

.product-combo-form__add {
  margin-top: 8px;
}
</style>
