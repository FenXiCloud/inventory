<template>
  <div class="quick-order">
    <t-steps :current="currentStep" class="quick-order__steps">
      <t-step-item title="选择客户"/>
      <t-step-item title="选择商品"/>
      <t-step-item title="提交打印"/>
    </t-steps>

    <div class="quick-order__body">
      <!-- 步骤1：选客户 -->
      <div v-show="currentStep === 0" class="quick-order__panel">
        <t-form label-width="90px" :colon="false">
          <t-form-item label="客户" required>
            <t-select
                v-model="customerId"
                :options="customerList"
                :keys="{ value: 'id', label: 'name' }"
                filterable
                clearable
                placeholder="请选择客户"
                @change="onCustomerChange"
            />
          </t-form-item>
          <t-form-item v-if="currentCustomer" label="税号">
            <span :class="{'quick-order__muted': !currentCustomer.taxNo}">{{ currentCustomer.taxNo || '未填写' }}</span>
          </t-form-item>
          <t-form-item v-if="currentCustomer" label="电话">
            <span :class="{'quick-order__muted': !currentCustomer.phone}">{{ currentCustomer.phone || '未填写' }}</span>
          </t-form-item>
        </t-form>
      </div>

      <!-- 步骤2：选商品（AI 识别 + 手动添加） -->
      <div v-show="currentStep === 1" class="quick-order__panel">
        <div class="quick-order__ai">
          <t-textarea
              v-model="aiText"
              placeholder="粘贴订单文本，如：&#10;苹果 5个 单价3.5&#10;香蕉 2斤 单价4"
              :autosize="{ minRows: 3, maxRows: 5 }"
          />
          <div class="quick-order__ai-actions">
            <t-button theme="primary" variant="outline" :loading="aiLoading" @click="aiRecognize">AI 识别</t-button>
            <t-button variant="outline" @click="startVoice">{{ listening ? '停止录音' : '语音输入' }}</t-button>
            <span class="quick-order__ai-hint">支持按编码/名称/拼音匹配商品</span>
          </div>
        </div>

        <t-table
            row-key="_rowKey"
            size="small"
            bordered
            :data="items"
            :columns="itemColumns"
            :foot-data="footData"
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
          <template #taxRate="{ row }">
            <span v-if="row.taxRate != null">{{ rateLabel(row.taxRate) }}</span>
            <span v-else class="quick-order__muted">—</span>
          </template>
          <template #quantity="{ row }">
            <t-input-number v-model="row.quantity" theme="normal" :min="0" :decimal-places="qtyDp" style="width:100%" @change="calcSubtotal(row)"/>
          </template>
          <template #unitPrice="{ row }">
            <t-input-number v-model="row.unitPrice" theme="normal" :min="0" :decimal-places="priceDp" style="width:100%" @change="calcSubtotal(row)"/>
          </template>
          <template #subtotal="{ row }">
            <span>{{ fmt(row.subtotal) }}</span>
          </template>
          <template #ops="{ row }">
            <t-link theme="danger" :disabled="items.length <= 1" @click="removeItem(row)">删除</t-link>
          </template>
        </t-table>
        <div class="quick-order__add">
          <t-button size="small" variant="outline" @click="addItem">添加商品行</t-button>
          <t-button size="small" variant="outline" @click="openComboSelect" style="margin-left: 8px">选套餐</t-button>
        </div>
        <div class="quick-order__actions">
          <t-button size="small" variant="outline" @click="clearItems">清空</t-button>
          <t-button size="small" variant="outline" @click="doPrint">打印</t-button>
          <t-button size="small" theme="primary" variant="outline" :loading="saving" @click="submitOrder('save')">保存</t-button>
          <t-button size="small" theme="primary" :loading="saving" @click="submitOrder('invoice')">开票</t-button>
        </div>
      </div>

      <!-- 步骤3：提交 + 打印 -->
      <div v-show="currentStep === 2" class="quick-order__panel">
        <div class="quick-order__summary">
          <div class="quick-order__summary-row">
            <span>客户</span><strong>{{ currentCustomer?.name || '—' }}</strong>
          </div>
          <div class="quick-order__summary-row">
            <span>商品明细</span><strong>{{ validItems.length }} 行</strong>
          </div>
          <div class="quick-order__summary-row">
            <span>合计金额</span><strong class="quick-order__price">¥{{ fmt(grandTotal) }}</strong>
          </div>
          <div class="quick-order__summary-row">
            <span>备注</span>
            <t-input v-model="remarks" placeholder="备注说明（可选）" style="width: 260px"/>
          </div>
        </div>
      </div>
    </div>

    <t-dialog v-model:visible="comboVisible" header="选择商品套餐" :footer="false" width="460px">
      <t-form label-width="90px">
        <t-form-item label="套餐">
          <t-select
              v-model="selectedComboId"
              :options="comboList"
              :keys="{ value: 'id', label: 'name' }"
              filterable
              placeholder="请选择套餐"
              style="width: 100%"
          />
        </t-form-item>
        <t-form-item label="套餐数量">
          <t-input-number v-model="comboQuantity" :min="1" :decimal-places="0" style="width: 100%"/> <!-- 套餐数量按整件，恒0位 -->
        </t-form-item>
      </t-form>
      <div style="text-align: right">
        <t-button variant="outline" @click="comboVisible = false">取消</t-button>
        <t-button theme="primary" style="margin-left: 8px" @click="applyCombo">确定</t-button>
      </div>
    </t-dialog>

    <div class="quick-order__footer">
      <t-button @click="closeWindow">取消</t-button>
      <div>
        <t-button v-if="currentStep > 0" @click="currentStep--">上一步</t-button>
        <t-button v-if="currentStep < 2" theme="primary" @click="nextStep">下一步</t-button>
        <t-button v-if="currentStep === 2" theme="primary" :loading="saving" @click="submitOrder()">保存并打印</t-button>
      </div>
    </div>
  </div>
</template>

<script>
import {MessagePlugin} from 'tdesign-vue-next';
import manba from 'manba';
import {mapState} from 'vuex';
import Customer from '@js/api/basic/Customer';
import Product from '@js/api/basic/Product';
import Warehouse from '@js/api/basic/Warehouse';
import SalesOrder from '@js/api/sales/SalesOrder';
import Ai from '@js/api/Ai';
import ProductCombo from '@js/api/basic/ProductCombo';
import {openPrint} from '@common/print';

let rowSeq = 0;
function newRow() {
  return {
    _rowKey: `qr-${++rowSeq}`,
    productId: null,
    productCode: '',
    productName: '',
    unitId: null,
    unitName: '',
    taxRate: null,
    warehouseId: null,
    quantity: 1,
    unitPrice: 0,
    subtotal: 0
  };
}

export default {
  name: 'QuickOrder',
  computed: {
    ...mapState(['accountBook']),
    currentCustomer() {
      if (this.customerId == null) return null;
      return (this.customerList || []).find((c) => String(c.id) === String(this.customerId)) || null;
    },
    validItems() {
      return this.items.filter((i) => i.productId != null);
    },
    grandTotal() {
      return this.validItems.reduce((s, i) => s + (Number(i.subtotal) || 0), 0);
    },
    footData() {
      const quantity = this.validItems.reduce((s, i) => s + (Number(i.quantity) || 0), 0);
      const subtotal = this.validItems.reduce((s, i) => s + (Number(i.subtotal) || 0), 0);
      return [{
        product: '合计',
        quantity: quantity.toFixed(2),
        subtotal: subtotal.toFixed(2)
      }];
    },
    itemColumns() {
      return [
        {colKey: 'product', title: '商品', minWidth: 260, foot: () => '合计'},
        {colKey: 'taxRate', title: '税率', width: 80, align: 'center'},
        {colKey: 'quantity', title: '数量', width: 120},
        {colKey: 'unitPrice', title: '单价', width: 120},
        {colKey: 'subtotal', title: '金额', width: 120, align: 'right'},
        {colKey: 'ops', title: '操作', width: 70, align: 'center'}
      ];
    }
  },
  data() {
    return {
      currentStep: 0,
      customerId: null,
      customerList: [],
      productList: [],
      warehouseList: [],
      defaultWarehouseId: null,
      items: [newRow()],
      aiText: '',
      aiLoading: false,
      saving: false,
      remarks: '',
      listening: false,
      recognition: null,
      comboVisible: false,
      comboList: [],
      selectedComboId: null,
      comboQuantity: 1
    };
  },
  methods: {
    rateLabel(r) {
      return `${Math.round((Number(r) || 0) * 100)}%`;
    },
    fmt(v) {
      return v === undefined || v === null || v === '' ? '0.00' : Number(v).toFixed(2);
    },
    calcSubtotal(row) {
      row.subtotal = (Number(row.quantity) || 0) * (Number(row.unitPrice) || 0);
    },
    onCustomerChange() {
      // 客户仅用于关联；商品价带出依赖 customerId
      this.reloadProductList();
    },
    reloadProductList() {
      return Product.select({customerId: this.customerId || undefined}).then(({data}) => {
        this.productList = (data || []).map((p) => ({...p, customName: `${p.code}--${p.name}`}));
      });
    },
    onSelectProduct(row, value) {
      const p = (this.productList || []).find((i) => String(i.id) === String(value));
      if (!p) return;
      row.productId = p.id;
      row.productCode = p.code;
      row.productName = p.name;
      row.unitId = p.unitId;
      row.unitName = p.unitName;
      row.taxRate = p.taxRate != null ? p.taxRate : null;
      row.warehouseId = this.defaultWarehouseId;
      row.unitPrice = p.lastSalePrice || 0;
      row.quantity = 1;
      this.calcSubtotal(row);
    },
    addItem() {
      this.items.push(newRow());
    },
    removeItem(row) {
      const idx = this.items.findIndex((i) => i._rowKey === row._rowKey);
      if (idx >= 0) this.items.splice(idx, 1);
    },
    aiRecognize() {
      if (!this.aiText.trim()) {
        MessagePlugin.warning('请先粘贴订单文本');
        return;
      }
      this.aiLoading = true;
      Ai.recognizeOrder(this.aiText.trim())
        .then(({data}) => {
          const rows = data || [];
          if (!rows.length) {
            MessagePlugin.warning('未能识别出商品，请手动添加');
            return;
          }
          this.applyRecognized(rows);
          MessagePlugin.success(`已识别 ${rows.length} 个商品`);
        })
        .catch(() => {
          MessagePlugin.error('识别失败，请手动添加商品');
        })
        .finally(() => (this.aiLoading = false));
    },
    applyRecognized(rows) {
      // 覆盖现有空白行，剩余追加
      const firstEmpty = this.items.findIndex((i) => i.productId == null);
      let cursor = firstEmpty >= 0 ? firstEmpty : this.items.length;
      rows.forEach((r) => {
        const p = (this.productList || []).find((i) => String(i.id) === String(r.productId));
        const row = newRow();
        row.productId = r.productId;
        row.productCode = r.code;
        row.productName = r.name || p?.name || '';
        row.unitId = p?.unitId;
        row.unitName = p?.unitName || '';
        row.taxRate = r.taxRate != null ? r.taxRate : (p?.taxRate != null ? p.taxRate : null);
        row.warehouseId = this.defaultWarehouseId;
        row.quantity = Number(r.quantity) || 1;
        row.unitPrice = Number(r.unitPrice) || Number(p?.lastSalePrice) || 0;
        this.calcSubtotal(row);
        this.items.splice(cursor++, 0, row);
      });
      // 清理多余的空白行，仅保留一行用于后续添加
      this.items = this.items.filter((i, idx) => i.productId != null || idx === this.items.length - 1);
      if (!this.items.some((i) => i.productId == null)) {
        this.items.push(newRow());
      }
    },
    clearItems() {
      this.items = [newRow()];
      this.aiText = '';
      this.remarks = '';
    },
    startVoice() {
      const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
      if (!SR) {
        MessagePlugin.warning('当前浏览器不支持语音识别，请使用 Chrome/Edge');
        return;
      }
      if (this.listening) {
        this.stopVoice();
        return;
      }
      this.recognition = new SR();
      this.recognition.lang = 'zh-CN';
      this.recognition.interimResults = false;
      this.recognition.continuous = false;
      this.recognition.onresult = (e) => {
        const text = Array.from(e.results || []).map((r) => r[0].transcript).join('');
        if (text) {
          this.aiText = this.aiText ? `${this.aiText}\n${text}` : text;
        }
      };
      this.recognition.onerror = (e) => {
        MessagePlugin.error('语音识别出错：' + (e.error || '未知错误'));
        this.listening = false;
      };
      this.recognition.onend = () => {
        this.listening = false;
      };
      this.recognition.start();
      this.listening = true;
    },
    stopVoice() {
      if (this.recognition) {
        this.recognition.stop();
      }
      this.listening = false;
    },
    openComboSelect() {
      if (this.customerId == null) {
        MessagePlugin.warning('请先选择客户');
        return;
      }
      this.loadCombos();
      this.comboVisible = true;
    },
    loadCombos() {
      ProductCombo.list({ page: 1, pageSize: 1000 }).then(({ data }) => {
        this.comboList = (data?.results || []).filter((c) => c.enabled !== false);
      });
    },
    applyCombo() {
      if (!this.selectedComboId) {
        MessagePlugin.warning('请选择套餐');
        return;
      }
      ProductCombo.load(this.selectedComboId).then(({ data }) => {
        const items = data?.comboItemList || [];
        if (!items.length) {
          MessagePlugin.warning('该套餐没有组件');
          return;
        }
        const qty = Number(this.comboQuantity) || 1;
        this.expandComboItems(items, qty);
        this.comboVisible = false;
        this.selectedComboId = null;
        this.comboQuantity = 1;
      });
    },
    expandComboItems(comboItems, qty) {
      const newRows = [];
      comboItems.forEach((ci) => {
        const p = (this.productList || []).find((i) => String(i.id) === String(ci.productId));
        if (!p) return;
        const row = newRow();
        row.productId = p.id;
        row.productCode = p.code;
        row.productName = p.name;
        row.unitId = p.unitId;
        row.unitName = p.unitName;
        row.taxRate = p.taxRate != null ? p.taxRate : null;
        row.warehouseId = this.defaultWarehouseId;
        row.unitPrice = p.lastSalePrice || 0;
        row.quantity = (Number(ci.quantity) || 1) * qty;
        this.calcSubtotal(row);
        newRows.push(row);
      });
      if (!newRows.length) {
        MessagePlugin.warning('套餐组件在商品库中不存在');
        return;
      }
      const firstEmpty = this.items.findIndex((i) => i.productId == null);
      if (firstEmpty >= 0) {
        this.items.splice(firstEmpty, 1, ...newRows);
      } else {
        this.items.push(...newRows);
      }
      if (!this.items.some((i) => i.productId == null)) {
        this.items.push(newRow());
      }
      MessagePlugin.success(`已展开套餐 ${newRows.length} 个组件`);
    },
    nextStep() {
      if (this.currentStep === 0) {
        if (this.customerId == null) {
          MessagePlugin.warning('请选择客户');
          return;
        }
      }
      if (this.currentStep === 1) {
        if (!this.validItems.length) {
          MessagePlugin.warning('请至少添加一个商品');
          return;
        }
      }
      this.currentStep++;
    },
    buildPayload() {
      const orderDate = manba().format('YYYY-MM-dd');
      const salesOrder = {
        customerId: this.customerId,
        orderDate,
        remarks: this.remarks || null,
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
      return { salesOrder, salesOrderItemList };
    },
    submitOrder(mode = 'saveAndPrint') {
      if (!this.validItems.length) {
        MessagePlugin.warning('请至少添加一个商品');
        return;
      }
      if (this.customerId == null) {
        MessagePlugin.warning('请先选择客户');
        return;
      }
      this.saving = true;
      SalesOrder.save(this.buildPayload())
        .then(() => {
          MessagePlugin.success('开单成功~');
          if (mode === 'saveAndPrint') {
            this.doPrint();
            this.closeWindow();
          } else if (mode === 'invoice') {
            this.openInvoice();
          } else {
            this.clearItems();
          }
        })
        .finally(() => (this.saving = false));
    },
    openInvoice() {
      this.$store.commit('pushTab', {
        key: 'InvoiceIssue',
        title: '蓝字发票',
        params: { sourceType: 'SALES_ORDER' },
      });
    },
    doPrint() {
      openPrint('销售订单', {
        header: {
          customerId: this.customerId,
          partner: this.currentCustomer?.name || '',
          amount: this.grandTotal
        },
        items: this.validItems.map((i) => ({
          productName: i.productName,
          quantity: i.quantity,
          price: i.unitPrice,
          amount: i.subtotal
        }))
      });
    },
    closeWindow() {
      this.$store.commit('closeTabKey', this.$store.state.currentTab);
      this.$store.commit('newTab', 'DashboardMain');
    }
  },
  created() {
    Promise.all([Customer.select(), Product.select(), Warehouse.select()]).then(([c, p, w]) => {
      this.customerList = c.data || [];
      this.productList = (p.data || []).map((x) => ({...x, customName: `${x.code}--${x.name}`}));
      this.warehouseList = w.data || [];
      this.defaultWarehouseId = (this.warehouseList.find((v) => v.systemDefault || v.isDefault))?.id || null;
    });
  }
};
</script>

<style scoped>
.quick-order {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
  padding: 16px;
  box-sizing: border-box;
  overflow: hidden;
}

.quick-order__steps {
  flex-shrink: 0;
  margin-bottom: 16px;
}

.quick-order__body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.quick-order__panel {
  padding: 8px 0;
}

.quick-order__ai {
  margin-bottom: 12px;
  padding: 12px;
  border: 1px dashed var(--td-component-border, #dcdcdc);
  border-radius: 6px;
  background: #fafafa;
}

.quick-order__ai-actions {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.quick-order__ai-hint {
  font-size: 12px;
  color: #8f959e;
}

.quick-order__add {
  margin-top: 8px;
}

.quick-order__actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

.quick-order__summary {
  max-width: 560px;
}

.quick-order__summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.quick-order__summary-row span {
  color: #646a73;
}

.quick-order__price {
  color: #e37318;
  font-size: 20px;
}

.quick-order__muted {
  color: #bbb;
}

.quick-order__footer {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
</style>
