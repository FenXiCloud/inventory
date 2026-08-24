<template>
  <div class="scan-order">
    <div class="scan-order__head">
      <t-form label-width="70px" :colon="false" class="scan-order__form">
        <t-form-item label="客户">
          <t-select
              v-model="customerId"
              :options="customerList"
              :keys="{ value: 'id', label: 'name' }"
              filterable
              clearable
              placeholder="请选择客户"
              style="width: 320px"
          />
        </t-form-item>
      </t-form>
      <div class="scan-order__barcode">
        <t-input
            ref="barcodeInput"
            v-model="barcode"
            placeholder="扫描商品条码后回车，自动加入明细"
            clearable
            style="width: 360px"
            @enter="onScan"
        >
          <template #prefixIcon><t-icon name="barcode"/></template>
        </t-input>
        <t-button theme="primary" :loading="scanning" @click="onScan">加入</t-button>
        <span class="scan-order__hint">扫码枪回车后光标自动回到此处</span>
      </div>
    </div>

    <div class="scan-order__table">
      <t-table
          row-key="_rowKey"
          size="small"
          bordered
          :data="items"
          :columns="columns"
          :foot-data="footData"
      >
        <template #productName="{ row }">
          <span>{{ row.productName || '—' }}</span>
        </template>
        <template #quantity="{ row }">
          <t-input-number v-model="row.quantity" theme="normal" :min="0" :decimal-places="2" style="width:100%" @change="calcSubtotal(row)"/>
        </template>
        <template #unitPrice="{ row }">
          <t-input-number v-model="row.unitPrice" theme="normal" :min="0" :decimal-places="2" style="width:100%" @change="calcSubtotal(row)"/>
        </template>
        <template #subtotal="{ row }">
          <span>¥{{ fmt(row.subtotal) }}</span>
        </template>
        <template #ops="{ row }">
          <t-link theme="danger" :disabled="items.length <= 1" @click="removeItem(row)">删除</t-link>
        </template>
      </t-table>
    </div>

    <div class="scan-order__footer">
      <div class="scan-order__total">合计：<strong>¥{{ fmt(grandTotal) }}</strong></div>
      <div>
        <t-button variant="outline" @click="clearItems">清空</t-button>
        <t-button theme="primary" :loading="saving" @click="save" style="margin-left: 8px">保存销售单</t-button>
      </div>
    </div>
  </div>
</template>

<script>
import {MessagePlugin} from 'tdesign-vue-next';
import manba from 'manba';
import Customer from '@js/api/basic/Customer';
import Product from '@js/api/basic/Product';
import Warehouse from '@js/api/basic/Warehouse';
import SalesOrder from '@js/api/sales/SalesOrder';

let rowSeq = 0;
function newRow() {
  return {
    _rowKey: `so-${++rowSeq}`,
    productId: null,
    productCode: '',
    productName: '',
    unitId: null,
    taxRate: null,
    warehouseId: null,
    quantity: 1,
    unitPrice: 0,
    subtotal: 0
  };
}

export default {
  name: 'ScanOrder',
  data() {
    return {
      customerId: null,
      customerList: [],
      productList: [],
      warehouseList: [],
      defaultWarehouseId: null,
      barcode: '',
      scanning: false,
      saving: false,
      items: [newRow()],
      columns: [
        {colKey: 'productCode', title: '编码', width: 140},
        {colKey: 'productName', title: '商品', minWidth: 200},
        {colKey: 'quantity', title: '数量', width: 130},
        {colKey: 'unitPrice', title: '单价', width: 130},
        {colKey: 'subtotal', title: '金额', width: 130, align: 'right'},
        {colKey: 'ops', title: '操作', width: 70, align: 'center'}
      ]
    };
  },
  computed: {
    validItems() {
      return this.items.filter((i) => i.productId != null);
    },
    grandTotal() {
      return this.validItems.reduce((s, i) => s + (Number(i.subtotal) || 0), 0);
    },
    footData() {
      const quantity = this.validItems.reduce((s, i) => s + (Number(i.quantity) || 0), 0);
      const subtotal = this.validItems.reduce((s, i) => s + (Number(i.subtotal) || 0), 0);
      return [{productCode: '合计', quantity: quantity.toFixed(2), subtotal: subtotal.toFixed(2)}];
    }
  },
  methods: {
    fmt(v) {
      return v === undefined || v === null || v === '' ? '0.00' : Number(v).toFixed(2);
    },
    calcSubtotal(row) {
      row.subtotal = (Number(row.quantity) || 0) * (Number(row.unitPrice) || 0);
    },
    onBarcodeKeydown() {
      // 保持焦点，扫码枪不会因其它元素抢焦而丢失
    },
    onScan() {
      const code = (this.barcode || '').trim();
      if (!code) {
        MessagePlugin.warning('请输入或扫描条码');
        return;
      }
      this.scanning = true;
      const p = (this.productList || []).find((i) =>
        String(i.barcode) === code || String(i.code) === code
      );
      if (!p) {
        MessagePlugin.warning(`未找到条码【${code}】对应的商品，请先在商品档案维护条码`);
        this.barcode = '';
        this.scanning = false;
        this.$nextTick(() => this.$refs.barcodeInput && this.$refs.barcodeInput.focus());
        return;
      }
      this.addProduct(p);
      this.barcode = '';
      this.scanning = false;
      this.$nextTick(() => this.$refs.barcodeInput && this.$refs.barcodeInput.focus());
    },
    addProduct(p) {
      // 已存在该商品行则数量 +1
      const exist = this.items.find((i) => i.productId != null && String(i.productId) === String(p.id));
      if (exist) {
        exist.quantity = (Number(exist.quantity) || 0) + 1;
        this.calcSubtotal(exist);
        return;
      }
      const empty = this.items.find((i) => i.productId == null);
      const row = empty || newRow();
      row.productId = p.id;
      row.productCode = p.code;
      row.productName = p.name;
      row.unitId = p.unitId;
      row.taxRate = p.taxRate != null ? p.taxRate : null;
      row.warehouseId = this.defaultWarehouseId;
      row.unitPrice = p.lastSalePrice || 0;
      row.quantity = 1;
      this.calcSubtotal(row);
      if (!empty) this.items.push(row);
      if (!this.items.some((i) => i.productId == null)) this.items.push(newRow());
    },
    removeItem(row) {
      const idx = this.items.findIndex((i) => i._rowKey === row._rowKey);
      if (idx >= 0) this.items.splice(idx, 1);
      if (!this.items.length) this.items.push(newRow());
    },
    clearItems() {
      this.items = [newRow()];
      this.barcode = '';
    },
    buildPayload() {
      const orderDate = manba().format('YYYY-MM-dd');
      const salesOrder = {
        customerId: this.customerId,
        orderDate,
        remarks: null,
        discountAmount: 0,
        discountRate: 0,
        finalAmount: this.grandTotal,
        totalAmount: this.grandTotal
      };
      const salesOrderItemList = this.validItems.map((i) => ({
        productId: i.productId,
        baseUnitId: i.unitId,
        secondaryUnitId: i.unitId,
        secondaryQuantity: i.quantity,
        conversionRate: 1,
        quantity: i.quantity,
        unitPrice: i.unitPrice,
        discountRate: 0,
        discountValue: 0,
        subtotal: i.subtotal,
        warehouseId: i.warehouseId,
        remark: ''
      }));
      return {salesOrder, salesOrderItemList};
    },
    save() {
      if (!this.validItems.length) {
        MessagePlugin.warning('请至少扫描一个商品');
        return;
      }
      if (this.customerId == null) {
        MessagePlugin.warning('请先选择客户');
        return;
      }
      this.saving = true;
      SalesOrder.save(this.buildPayload())
        .then(() => {
          MessagePlugin.success('销售单保存成功~');
          this.clearItems();
        })
        .finally(() => (this.saving = false));
    }
  },
  mounted() {
    this.$nextTick(() => this.$refs.barcodeInput && this.$refs.barcodeInput.focus());
  },
  created() {
    Promise.all([Customer.select(), Product.select(), Warehouse.select()]).then(([c, p, w]) => {
      this.customerList = c.data || [];
      this.productList = p.data || [];
      this.warehouseList = w.data || [];
      this.defaultWarehouseId = (this.warehouseList.find((v) => v.systemDefault || v.isDefault))?.id || null;
    });
  }
};
</script>

<style scoped>
.scan-order {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
  padding: 16px;
  box-sizing: border-box;
  overflow: hidden;
}

.scan-order__head {
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.scan-order__form {
  flex-shrink: 0;
}

.scan-order__barcode {
  display: flex;
  align-items: center;
  gap: 8px;
}

.scan-order__hint {
  font-size: 12px;
  color: #8f959e;
}

.scan-order__table {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.scan-order__footer {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.scan-order__total {
  color: #646a73;
}

.scan-order__total strong {
  color: #e37318;
  font-size: 20px;
}
</style>
