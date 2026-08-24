<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <!-- 流程引导 -->
      <flow-guide
        :current="flowCurrentStep"
        :steps="flowSteps"
        title="以销定购流程"
      />

      <!-- 状态标签 -->
      <div v-if="form.id" class="status-bar mb-12px">
        <t-tag :theme="statusTheme" variant="light" size="large">
          {{ form.orderStatusText || form.orderStatus }}
        </t-tag>
        <span v-if="form.statusText" class="ml-8px text-gray-500">{{ form.statusText }}</span>
        <span v-if="form.sourceSalesReservationNo" class="ml-8px text-gray-500">来源销售预订：{{ form.sourceSalesReservationNo }}</span>
      </div>

      <div class="form-toolbar">
        <div class="form-toolbar__left">
          <label class="mr-20px" style="font-size: 16px !important">供货商：</label>
          <t-select
            class="w-300px"
            filterable
            :options="supplierList"
            :clearable="false"
            :disabled="isReadOnly"
            @change="changeSupplier($event)"
            v-model="supplierId"
            placeholder="请选择供货商"
            :keys="{ value: 'id', label: 'name' }"
          />
          <label class="mr-20px ml-16px" style="font-size: 16px !important">单据日期：</label>
          <t-date-picker
            v-model="form.orderDate"
            :clearable="false"
            :disabled="isReadOnly"
          />
        </div>
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
        :data="productData"
        :foot-data="footData"
        :loading="loading"
      >
        <template #ops="{ rowIndex }">
          <div class="fa fa-plus text-hover mr-5px" @click="adjustRows('insert', rowIndex)"></div>
          <div
            class="fa fa-minus text-hover"
            v-if="isDeleting"
            @click="adjustRows('delete', rowIndex)"
          ></div>
        </template>
        <template #productInfo="{ row, rowIndex }">
          <div class="input-group goodsSelect" @keyup.stop="void 0">
            <t-select
              ref="ms"
              @change="selectProduct($event, rowIndex)"
              @create="createProduct($event, rowIndex)"
              v-model="row.productId"
              :options="productList"
              filterable
              creatable
              placeholder="输入编码/名称"
              :keys="{ value: 'productId', label: 'productName' }"
            />
          </div>
        </template>
        <template #secondaryUnitName="{ row }">
          <template v-if="!row.isNew">
            <t-select
              v-if="row.auxiliaryUnitPrices"
              :clearable="false"
              @change="changeProductUnit($event, row)"
              v-model="row.secondaryUnitId"
              :options="row.auxiliaryUnitPrices"
              filterable
              placeholder="输入单位"
              :keys="{ value: 'unitId', label: 'unitName' }"
            />
            <span v-else>{{ row.secondaryUnitName }}</span>
          </template>
        </template>
        <template #warehouse="{ row }">
          <template v-if="!row.isNew">
            <t-select
              :clearable="false"
              v-model="row.warehouseId"
              :options="warehouseList"
              filterable
              :keys="{ value: 'id', label: 'name' }"
            />
          </template>
        </template>
        <template #secondaryQuantity="{ row, rowIndex }">
          <template v-if="!row.isNew">
            <t-input-number
              :id="'r' + rowIndex + '' + 3"
              v-model="row.secondaryQuantity"
              theme="normal"
              :min="0"
              :decimal-places="2"
              style="width: 100%"
              @blur="updateQuantity(row)"
            />
          </template>
        </template>
        <template #secondaryPrice="{ row, rowIndex }">
          <template v-if="!row.isNew">
            <t-input-number
              :id="'r' + rowIndex + '' + 4"
              v-model="row.secondaryPrice"
              theme="normal"
              :min="0"
              :decimal-places="2"
              style="width: 100%"
              @keyup="handleEnter($event, rowIndex, 4)"
              @blur="updatePrice(row)"
            />
          </template>
        </template>
        <template #discountRate="{ row, rowIndex }">
          <t-input-number
            v-if="!row.isNew"
            :id="'r' + rowIndex + '' + 5"
            v-model="row.discountRate"
            theme="normal"
            :min="0"
            :decimal-places="2"
            style="width: 100%"
            @keyup="handleEnter($event, rowIndex, 5)"
            @blur="updateDiscount(row)"
          />
        </template>
        <template #discountValue="{ row, rowIndex }">
          <t-input-number
            v-if="!row.isNew"
            :id="'r' + rowIndex + '' + 6"
            v-model="row.discountValue"
            theme="normal"
            :min="0"
            :decimal-places="2"
            style="width: 100%"
            @keyup="handleEnter($event, rowIndex, 6)"
            @blur="updateDiscountValue(row)"
          />
        </template>
        <template #subtotal="{ row, rowIndex }">
          <t-input-number
            v-if="!row.isNew"
            :id="'r' + rowIndex + '' + 7"
            v-model="row.subtotal"
            theme="normal"
            :min="0"
            :decimal-places="2"
            style="width: 100%"
            @keyup="handleEnter($event, rowIndex, 7)"
            @blur="updateSubtotal(row)"
          />
        </template>
        <template #taxRate="{ row }">
          <span v-if="!row.isNew">{{ taxRateText(row.taxRate) }}</span>
        </template>
        <template #remark="{ row, rowIndex }">
          <t-input
            v-if="!row.isNew"
            :id="'r' + rowIndex + '' + 8"
            v-model="row.remark"
            placeholder="输入备注"
            @keyup="handleEnter($event, rowIndex, 8)"
          />
        </template>
      </t-table>
      <div class="mt-10px"></div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px w-80px">备注说明：</label>
          <t-input
            placeholder="请输入备注"
            maxlength="150"
            v-model="form.remarks"
          />
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px w-80px">优惠率：</label>
          <t-input v-model="form.discountRate" @blur="changeDiscountRate" />
          <label class="ml-10px mr-16px w-80px">优惠金额：</label>
          <t-input v-model="form.discountAmount" @blur="changeDiscountAmount" />
          <label class="ml-16px mr-16px w-100px">优惠后金额：</label>
          <t-input v-model="form.finalAmount" @blur="changeFinalAmount" />
          <label class="ml-16px mr-16px w-80px">税额：</label>
          <t-input-number :value="totalTaxAmount" theme="normal" :min="0" :decimal-places="2" disabled />
          <label class="ml-16px mr-16px w-100px">价税合计：</label>
          <t-input-number :value="totalWithTax" theme="normal" :min="0" :decimal-places="2" disabled />
        </div>
      </div>
    </div>
    <div class="page-column-footer modal-column-between bg-white-color border">
      <t-button @click="closeWindow" :loading="loading"> 取消 </t-button>
      <div>
        <!-- 待审核状态：保存、审核 -->
        <template v-if="!isReadOnly">
          <t-button theme="primary" @click="saveOrder('add')" :loading="loading">
            保存并新增
          </t-button>
          <t-button @click="saveOrder('save')" :loading="loading"> 保存 </t-button>
        </template>
        <t-button @click="approved()" :loading="loading" v-if="form.id && form.orderStatus === '已保存'">
          审核
        </t-button>
        <!-- 已审核状态：转采购入库单 -->
        <t-button theme="success" @click="convertToInbound()" :loading="loading"
          v-if="form.id && form.orderStatus === '已审核' && !isConvertedToInbound">
          转采购入库单
        </t-button>
        <!-- 反审核（仅已审核且未转时） -->
        <t-button @click="unApproved()" :loading="loading"
          v-if="form.id && form.orderStatus === '已审核' && !isConvertedToInbound">
          反审核
        </t-button>
      </div>
    </div>

    <!-- 选择仓库弹窗 -->
    <t-dialog
      v-model:visible="showWarehouseDialog"
      header="选择仓库"
      :on-confirm="confirmConvertToInbound"
      :on-close="() => showWarehouseDialog = false"
    >
      <div class="p-16px">
        <p class="mb-12px">将根据此进货预订生成采购入库单，请选择仓库：</p>
        <t-select
          v-model="selectedWarehouseId"
          :options="warehouseList"
          filterable
          placeholder="请选择仓库"
          :keys="{ value: 'id', label: 'name' }"
          style="width: 100%"
        />
      </div>
    </t-dialog>
  </div>
</template>
<script>
import {LoadingPlugin, MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import manba from 'manba';
import {CopyObj} from '@common/utils';
import PurchaseReservation from '@js/api/purchase/PurchaseReservation';
import Supplier from '@js/api/basic/Supplier';
import Product from '@js/api/basic/Product';
import Warehouse from '@js/api/basic/Warehouse';
import {mapState} from 'vuex';
import FlowGuide from '@/views/components/FlowGuide.vue';

let rowSeq = 0;
function newRow(extra = {}) {
  return { _rowKey: `r-${++rowSeq}`, productId: null, isNew: true, ...extra };
}

export default {
  name: 'PurchaseReservationForm',
  components: {FlowGuide},
  props: {
    orderId: [String, Number],
    type: String
  },
  computed: {
    ...mapState(['accountBook']),
    totalAmount() {
      let total = 0;
      this.productData.forEach((val) => {
        if (val.quantity > 0) {
          total += parseFloat(val.subtotal);
        }
      });
      return total.toFixed(2);
    },
    totalTaxAmount() {
      let tax = 0;
      (this.productData || []).forEach((row) => {
        tax += Number(row.subtotal || 0) * Number(row.taxRate || 0);
      });
      return tax.toFixed(2);
    },
    totalWithTax() {
      return (Number(this.form.finalAmount || 0) + Number(this.totalTaxAmount)).toFixed(2);
    },
    isDeleting() {
      return this.productData.length > 1;
    },
    isReadOnly() {
      return this.form.orderStatus === '已审核' || this.form.orderStatus === '已转进货' || this.form.orderStatus === '已完成';
    },
    statusTheme() {
      const statusMap = {
        '待审核': 'warning',
        '已审核': 'primary',
        '已转采购': 'success',
        '已完成': 'default'
      };
      return statusMap[this.form.orderStatusText] || 'default';
    },
    flowCurrentStep() {
      if (this.form.orderStatus === '已保存') return 1;
      if (this.form.orderStatus === '已审核' && !this.isConvertedToInbound) return 1;
      if (this.form.orderStatus === '已审核' && this.isConvertedToInbound) return 2;
      if (this.form.orderStatus === '已转进货') return 2;
      if (this.form.orderStatus === '已完成') return 3;
      return 1;
    },
    columns() {
      return [
        {
          colKey: 'seq',
          title: '序号',
          width: 60,
          align: 'center',
          fixed: 'left',
          cell: (h, { rowIndex }) => rowIndex + 1
        },
        {
          colKey: 'ops',
          title: '操作',
          width: 70,
          align: 'center',
          fixed: 'left',
          foot: () => '合计'
        },
        { colKey: 'productInfo', title: '产品信息', minWidth: 240 },
        { colKey: 'specification', title: '规格', width: 90 },
        { colKey: 'secondaryUnitName', title: '单位', align: 'center', width: 80 },
        { colKey: 'warehouse', title: '仓库', align: 'center', width: 120 },
        { colKey: 'secondaryQuantity', title: '数量', width: 90 },
        { colKey: 'secondaryPrice', title: '单价', width: 100 },
        { colKey: 'discountRate', title: '折扣率(%)', width: 90 },
        { colKey: 'discountValue', title: '折扣额', width: 90 },
        { colKey: 'subtotal', title: '金额', width: 100 },
        { colKey: 'taxRate', title: '税率', align: 'center', width: 80 },
        { colKey: 'remark', title: '备注', width: 140 }
      ];
    },
    footData() {
      let quantity = 0;
      let discountValue = 0;
      let subtotal = 0;
      (this.productData || []).forEach((row) => {
        if (row.quantity) {
          quantity += Number(row.quantity || 0);
        }
        discountValue += Number(row.discountValue || 0);
        subtotal += Number(row.subtotal || 0);
      });
      this.allFinalAmount = subtotal;
      return [{
        ops: '合计',
        quantity: quantity.toFixed(0),
        discountValue: discountValue.toFixed(2),
        subtotal: subtotal.toFixed(2)
      }];
    }
  },
  data() {
    return {
      flowSteps: [
        {title: '销售预订', content: '记录客户选购商品'},
        {title: '进货预订', content: '根据销售预订进货'},
        {title: '采购入库', content: '进货后入库'},
        {title: '销售出库', content: '发货给客户'}
      ],
      loading: false,
      productList: [],
      product: null,
      allFinalAmount: 0,
      warehouseList: [],
      supplierList: [],
      supplierId: null,
      warehouseId: null,
      showWarehouseDialog: false,
      selectedWarehouseId: null,
      isConvertedToInbound: false,
      form: {
        id: null,
        orderDate: manba().format('YYYY-MM-dd'),
        supplierId: null,
        discountAmount: 0.0,
        discountRate: 0.0,
        finalAmount: 0.0,
        totalAmount: 0.0,
        remarks: null,
        orderStatus: null,
        orderStatusText: null,
        statusText: null,
        status: 0,
        sourceSalesReservationNo: null
      },
      productData: []
    };
  },
  watch: {
    allFinalAmount(val) {
      this.form.discountAmount = (val * this.form.discountRate * 0.01).toFixed(2);
      this.form.finalAmount = (val - this.form.discountAmount).toFixed(2);
    }
  },
  methods: {
    taxRateText(rate) {
      const r = Number(rate || 0);
      return r ? (r * 100).toFixed(0) + '%' : '-';
    },
    handleEnter(e, index, num) {
      const event = e?.$event || e;
      if (!event) return;
      event.stopPropagation?.();
      const keyCode = event.keyCode || event.which;
      if (keyCode === 13) {
        if (num === 8) {
          if (index >= this.productData.length - 2) {
            this.$refs.ms?.$el?.querySelector('input')?.click();
            this.$refs.ms?.$el?.querySelector('input')?.select();
          } else {
            let str = 'r' + (index + 1) + '' + 3;
            document.getElementById(str)?.querySelector('input')?.focus();
            document.getElementById(str)?.querySelector('input')?.select();
          }
        } else {
          let str = 'r' + index + '' + (num + 1);
          document.getElementById(str)?.querySelector('input')?.focus();
          document.getElementById(str)?.querySelector('input')?.select();
        }
      } else if (keyCode === 38) {
        if (index > 0) {
          let str = 'r' + (index - 1) + '' + num;
          document.getElementById(str)?.querySelector('input')?.focus();
          document.getElementById(str)?.querySelector('input')?.select();
        }
      } else if (keyCode === 40) {
        if (index < this.productData.length - 2) {
          let str = 'r' + (index + 1) + '' + num;
          document.getElementById(str)?.querySelector('input')?.focus();
          document.getElementById(str)?.querySelector('input')?.select();
        } else {
          this.$refs.ms?.$el?.querySelector('input')?.click();
          this.$refs.ms?.$el?.querySelector('input')?.select();
        }
      }
    },

    createProduct(value, index) {
      const name = (typeof value === 'string' ? value : (value?.label || value?.productName || '')).trim();
      if (!name) return;
      Product.quickCreate({ name }).then(({ data }) => {
        if (data && data.productId) {
          this.productList.push(data);
          this.selectProduct(data.productId, index);
        }
      });
    },

    selectProduct(value, index) {
      const d = (this.productList || []).find((item) => String(item.productId) === String(value));
      if (d) {
        const defaultWarehouseId = this.resolveDefaultWarehouseId();
        let g = newRow({
          isNew: false,
          quantity: 1,
          secondaryQuantity: 1,
          secondaryPrice: d.price || 0,
          specification: d.spec,
          taxRate: d.taxRate,
          warehouseId: defaultWarehouseId,
          price: d.price || 0,
          discountValue: 0.0,
          discountRate: 0.0,
          subtotal: d.price || 0,
          conversionRate: 1,
          secondaryUnitId: d.unitId,
          secondaryUnitName: d.unitName,
          baseUnitId: d.unitId,
          baseUnitName: d.unitName,
          remark: ''
        });
        this.productData[index] = Object.assign({}, d, g, {
          productId: d.productId,
          isNew: false,
        });
        if (!this.productData[index].warehouseId) {
          this.productData[index].warehouseId = defaultWarehouseId;
        }
        if (!this.productData[index + 1]) {
          this.productData.push(newRow({ isNew: true }));
        }
        this.$nextTick(() => {
          let str = index + '' + 3;
          let element = document.querySelector('#r' + str + ' input');
          setTimeout(() => {
            element?.focus();
            element?.select();
          }, 100);
        });
      }
      this.product = null;
    },

    resolveDefaultWarehouseId() {
      if (this.warehouseId) {
        return this.warehouseId;
      }
      const list = this.warehouseList || [];
      const found = list.find((w) => w.systemDefault || w.isDefault);
      const id = found?.id || null;
      this.warehouseId = id;
      return id;
    },

    saveOrder(type) {
      LoadingPlugin(true);
      if (!this.form.supplierId) {
        MessagePlugin.error('请选择供货商~');
        LoadingPlugin(false);
        return;
      }
      let productData = this.productData
        .filter((c) => c.quantity > 0)
        .map(({ _rowKey, ...rest }) => ({ ...rest, unitPrice: rest.secondaryPrice }));
      if (productData.length <= 0) {
        MessagePlugin.error('请选择产品~');
        LoadingPlugin(false);
        return;
      }
      let warehouse = productData.filter((c) => c.warehouseId === null);
      if (warehouse.length > 0) {
        MessagePlugin.error('请选择仓库~');
        LoadingPlugin(false);
        return;
      }
      PurchaseReservation.save({
        purchaseReservation: Object.assign(this.form, {
          totalAmount: this.allFinalAmount
        }),
        purchaseReservationItemList: productData
      })
        .then((success) => {
          if (success) {
            MessagePlugin.success('保存成功~');
            this.clearForm();
            if (type === 'save') {
              this.closeWindow();
            }
          }
        })
        .finally(() => LoadingPlugin(false));
    },

    clearForm() {
      this.form = {
        id: null,
        orderDate: manba().format('YYYY-MM-dd'),
        supplierId: null,
        remarks: null,
        discountAmount: 0.0,
        discountRate: 0.0,
        finalAmount: 0.0
      };
      this.allFinalAmount = 0;
      this.productData = [];
      this.supplierId = null;
    },

    adjustRows(type, index) {
      if (type === 'insert') {
        this.productData.splice(index + 1, 0, newRow({ isNew: true }));
      } else {
        this.productData.splice(index, 1);
      }
    },

    changeSupplier(value) {
      if (value == null || value === '') {
        this.form.supplierId = null;
        this.productData = [newRow({ isNew: true })];
        return;
      }
      const e = (this.supplierList || []).find((item) => String(item.id) === String(value));
      if (!e) return;
      if (e.id !== this.form.supplierId) {
        if (this.productData.length > 1) {
          DialogPlugin.confirm({
            header: '系统提示',
            body: `修改供货商后，将清除已选择的产品数据，确定修改？`,
            onConfirm: () => {
              this.productData = [newRow({ isNew: true })];
              this.form.supplierId = e.id;
              this.loadProductsBySupplier();
            }
          });
        } else {
          this.form.supplierId = e.id;
          this.productData = [newRow({ isNew: true })];
          this.loadProductsBySupplier();
        }
      }
    },

    loadProductsBySupplier() {
      if (this.form.supplierId) {
        Supplier.selectProduct(this.form.supplierId)
          .then(({ data }) => {
            this.productList = data || [];
            if (!this.form.id) {
              this.productData = [newRow({ isNew: true })];
            }
          })
          .finally(() => {
            this.$nextTick(() => {
              this.$refs.ms?.$el?.querySelector('input')?.click();
              this.$refs.ms?.$el?.querySelector('input')?.select();
            });
          });
      }
    },

    changeDiscountRate() {
      this.form.discountRate = parseFloat(this.form.discountRate) || 0;
      this.form.discountAmount = (
        this.allFinalAmount *
        this.form.discountRate *
        0.01
      ).toFixed(2);
      this.form.finalAmount = (
        this.allFinalAmount - this.form.discountAmount
      ).toFixed(2);
    },
    changeDiscountAmount() {
      this.form.discountAmount = parseFloat(this.form.discountAmount) || 0;
      this.form.finalAmount = (
        this.allFinalAmount - this.form.discountAmount
      ).toFixed(2);
      this.form.discountRate =
        this.form.discountAmount === 0
          ? 0
          : ((this.form.discountAmount / this.allFinalAmount) * 100).toFixed(2);
    },
    changeFinalAmount() {
      this.form.finalAmount = parseFloat(this.form.finalAmount) || 0;
      this.form.discountAmount = (
        this.allFinalAmount - this.form.finalAmount
      ).toFixed(2);
      this.form.discountRate =
        this.form.finalAmount === 0
          ? 0
          : ((this.form.finalAmount / this.allFinalAmount) * 100).toFixed(2);
    },
    changeProductUnit(value, row) {
      const item = (row.auxiliaryUnitPrices || []).find((u) => String(u.unitId) === String(value));
      if (!item) return;
      row.secondaryUnitName = item.unitName;
      row.secondaryPrice = (item.unitPrice || item.price || 0).toFixed(2) || 0;
      row.conversionRate = item.conversionRate || 1;
      row.quantity = (row.secondaryQuantity * row.conversionRate).toFixed(2);
      row.subtotal = (row.secondaryQuantity * row.secondaryPrice).toFixed(2);
    },

    updateQuantity(item) {
      item.secondaryQuantity = item.secondaryQuantity || 1;
      item.subtotal = (
        (item.secondaryQuantity *
          item.secondaryPrice *
          (100 - item.discountRate)) /
        100
      ).toFixed(2);
      item.discountValue = (
        (item.secondaryQuantity * item.secondaryPrice * item.discountRate) /
        100
      ).toFixed(2);
      item.quantity = (
        item.secondaryQuantity * (item.conversionRate || 1)
      ).toFixed(2);
    },

    updatePrice(item) {
      item.secondaryPrice = item.secondaryPrice || 0.0;
      item.discountValue = (
        (item.secondaryPrice * item.secondaryQuantity * item.discountRate) /
        100
      ).toFixed(2);
      item.subtotal = (
        item.secondaryPrice * item.secondaryQuantity -
        item.discountValue
      ).toFixed(2);
    },

    updateDiscount(item) {
      item.discountRate = item.discountRate || 0.0;
      item.subtotal = (
        ((item.secondaryQuantity || 0) *
          item.secondaryPrice *
          (100 - item.discountRate || 0)) /
        100
      ).toFixed(2);
      item.discountValue = (
        (item.secondaryQuantity || 0) * item.secondaryPrice -
        item.subtotal
      ).toFixed(2);
    },

    updateDiscountValue(item) {
      item.discountValue = item.discountValue || 0.0;
      item.discountRate = (
        (item.discountValue / (item.secondaryPrice * item.secondaryQuantity)) *
          100 || 0
      ).toFixed(2);
      item.subtotal = (
        item.secondaryPrice * item.secondaryQuantity -
        item.discountValue
      ).toFixed(2);
    },

    updateSubtotal(item) {
      item.subtotal = item.subtotal || 0;
      item.secondaryPrice = (
        ((item.subtotal / (100 - item.discountRate)) * 100) /
        item.secondaryQuantity
      ).toFixed(2);
      item.discoutPrice = (item.secondaryPrice - item.subtotal).toFixed(2);
    },

    approved() {
      let ids = [this.form.id];
      DialogPlugin.confirm({
        header: '审核提示',
        body: `确认审核该订单?`,
        onConfirm: () => {
          PurchaseReservation.approved(ids, '已审核').then(() => {
            MessagePlugin.success('操作成功~');
            this.loadFormData();
          });
        }
      });
    },

    unApproved() {
      let ids = [this.form.id];
      DialogPlugin.confirm({
        header: '反审核提示',
        body: `确认反审核该订单?`,
        onConfirm: () => {
          PurchaseReservation.approved(ids, '已保存').then(() => {
            MessagePlugin.success('操作成功~');
            this.loadFormData();
          });
        }
      });
    },

    convertToInbound() {
      // 打开仓库选择弹窗
      this.showWarehouseDialog = true;
      this.selectedWarehouseId = this.warehouseId;
    },

    confirmConvertToInbound() {
      if (!this.selectedWarehouseId) {
        MessagePlugin.warning('请选择仓库');
        return;
      }
      this.showWarehouseDialog = false;
      LoadingPlugin(true);
      PurchaseReservation.transferToPurchaseInbound(this.form.id, this.selectedWarehouseId).then(({data}) => {
        MessagePlugin.success('已生成采购入库单');
        this.$store.commit('pushTab', {
          key: 'PurchaseInboundForm',
          title: '采购入库单详情',
          params: { orderId: data }
        });
      }).finally(() => LoadingPlugin(false));
    },

    loadFormData() {
      if (this.orderId) {
        PurchaseReservation.load(this.orderId).then(({data}) => {
          if (data) {
            CopyObj(this.form, data);
            this.supplierId = data.supplierId;
            this.isConvertedToInbound = data.status > 0;
            if ('copy' === this.type) {
              this.form.id = null;
            }
          }
          this.loadProductsBySupplier();
          this.productData = (data.purchaseReservationItemList || []).map((row) =>
            newRow({ ...row, secondaryPrice: row.unitPrice, isNew: false })
          );
          this.productData.push(newRow({ isNew: true }));
        });
      }
    },

    closeWindow() {
      this.$store.commit('closeTabKey', this.$store.state.currentTab);
      this.$store.commit('newTab', 'PurchaseReservationList');
      this.$nextTick(() => {
        this.$store.commit('SET_TAB_DATA', { refresh: true });
      });
    }
  },
  created() {
    LoadingPlugin(true);
    Promise.all([Supplier.select(), Warehouse.select()])
      .then((results) => {
        this.supplierList = results[0].data || [];
        this.warehouseList = results[1].data || [];
        if (this.warehouseList != null) {
          this.warehouseId = this.warehouseList.find(
            (val) => val.systemDefault || val.isDefault
          )?.id;
        }
        if (this.orderId) {
          this.loadFormData();
        }
      })
      .finally(() => LoadingPlugin(false));
  }
};
</script>
<style scoped>
.product-img {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
}
</style>
