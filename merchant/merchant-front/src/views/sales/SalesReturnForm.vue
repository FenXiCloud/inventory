<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <div class="form-toolbar">
        <div class="form-toolbar__left">
          <label class="mr-20px" style="font-size: 16px !important;">客户：</label>
          <t-select
              class="w-300px"
              filterable
              :options="customerList"
              :clearable="false"
              :disabled="isAudited"
              @change="changeCustomer($event)"
              v-model="customerId"
              placeholder="请选择客户"
              :keys="{ value: 'id', label: 'name' }"
          />
          <label class="mr-20px ml-16px" style="font-size: 16px !important;">退货日期：</label>
          <t-date-picker
              v-model="form.returnDate"
              :clearable="false"
              :disabled="isAudited"
          />
          <t-button
              v-if="type==='add' && !isAudited"
              @click="selectOutboundOrder()"
              theme="primary"
              style="margin-left: 20px"
          >
            选择源单
          </t-button>
        </div>
        <Stamp v-if="isAudited"/>
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
          <template v-if="!isAudited">
            <div class="fa fa-minus text-hover" v-if="isDeleting" @click="adjustRows('delete', rowIndex)"></div>
          </template>
        </template>
        <template #imgPath="{ row }">
          <img
              :src="productImage(row)"
              alt=""
              class="product-img cursor-pointer"
              @click="previewImage(productImage(row))"
          >
        </template>
        <template #productInfo="{ row, rowIndex }">
          <div class="input-group goodsSelect" v-if="row.isNew && !isAudited" @keyup.stop="void(0)">
            <t-select
                ref="ms"
                @change="selectProduct($event, rowIndex)"
                :options="productList"
                v-model="row.productId"
                filterable
                placeholder="输入编码/名称"
                :clearable="false"
                :keys="{ value: 'id', label: 'customName' }"
            />
          </div>
          <div v-else-if="!row.isNew" class="flex">
            <div class="flex1 ml-8px">
              <div>{{ row.productCode }}--{{ row.productName }}</div>
            </div>
          </div>
        </template>
        <template #specification="{ row }">
          {{ productList.find(item => item.id === row.productId)?.specification || '-' }}
        </template>
        <template #productCategoryName="{ row }">
          {{ productList.find(item => item.id === row.productId)?.productCategoryName || '-' }}
        </template>
        <template #warehouse="{ row }">
          <template v-if="!row.isNew && !isAudited">
            <t-select
                :clearable="false"
                v-model="row.warehouseId"
                :options="warehouseList"
                filterable
                :keys="{ value: 'id', label: 'name' }"
            />
          </template>
          <span v-else-if="!row.isNew">{{ warehouseName(row.warehouseId) }}</span>
        </template>
        <template #quantity="{ row, rowIndex }">
          <t-input-number
              v-if="!row.isNew && !isAudited"
              :id="'r'+rowIndex+''+3"
              v-model="row.quantity"
              theme="normal"
              :min="0"
              :max="row.returnQuantity"
              :decimal-places="2"
              style="width: 100%"
              @blur="updateQuantity(row)"
          />
          <span v-else-if="!row.isNew">{{ row.quantity }}</span>
        </template>
        <template #unitPrice="{ row, rowIndex }">
          <template v-if="!row.isNew && !isAudited">
            <t-tooltip theme="light">
              <template #content>
                <div class="recent-sales-table">
                  <table>
                    <thead>
                    <tr>
                      <th>最近销售时间</th>
                      <th>最近销售价</th>
                      <th>零售客户</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr v-for="(item, index) in recentSales || []" :key="index">
                      <td>{{ item.orderDate || '-' }}</td>
                      <td>{{ item.unitPrice || '-' }}</td>
                      <td>{{ item.customer || '-' }}</td>
                    </tr>
                    </tbody>
                  </table>
                </div>
              </template>
              <t-input-number
                  :id="'r'+rowIndex+''+4"
                  v-model="row.unitPrice"
                  theme="normal"
                  :min="0"
                  :decimal-places="2"
                  style="width: 100%"
                  @blur="updatePrice(row)"
                  @focus="showPrice(row)"
              />
            </t-tooltip>
          </template>
          <span v-else-if="!row.isNew">{{ row.unitPrice }}</span>
        </template>
        <template #discountRate="{ row, rowIndex }">
          <t-input-number
              v-if="!row.isNew && !isAudited"
              :id="'r'+rowIndex+''+5"
              v-model="row.discountRate"
              theme="normal"
              :min="0"
              :decimal-places="2"
              style="width: 100%"
              @blur="updateDiscount(row)"
          />
          <span v-else-if="!row.isNew">{{ row.discountRate }}</span>
        </template>
        <template #discountValue="{ row, rowIndex }">
          <t-input-number
              v-if="!row.isNew && !isAudited"
              :id="'r'+rowIndex+''+6"
              v-model="row.discountValue"
              theme="normal"
              :min="0"
              :decimal-places="2"
              style="width: 100%"
              @blur="updateDiscountAmount(row)"
          />
          <span v-else-if="!row.isNew">{{ row.discountValue }}</span>
        </template>
        <template #subtotal="{ row, rowIndex }">
          <t-input-number
              v-if="!row.isNew && !isAudited"
              :id="'r'+rowIndex+''+7"
              v-model="row.subtotal"
              theme="normal"
              :min="0"
              :decimal-places="2"
              style="width: 100%"
              @blur="updateFinalAmount(row)"
          />
          <span v-else-if="!row.isNew">{{ row.subtotal }}</span>
        </template>
        <template #remark="{ row, rowIndex }">
          <t-input
              v-if="!row.isNew && !isAudited"
              :id="'r'+rowIndex+''+8"
              v-model="row.remark"
              placeholder="输入备注"
          />
          <span v-else-if="!row.isNew">{{ row.remark }}</span>
        </template>
      </t-table>
      <div class="mt-10px"></div>
      <div class="filler-panel" v-if="type==='edit'">
        <div class="filler-item">
          <label class="mr-16px w-100px">单据编号：</label>
          <t-input v-model="form.orderNo" readonly/>
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px w-100px">退货原因：</label>
          <t-input placeholder="请输入退货原因" maxlength="150" v-model="form.returnReason" :disabled="isAudited"/>
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px w-100px">备注说明：</label>
          <t-input placeholder="请输入备注" maxlength="150" v-model="form.remarks" :disabled="isAudited"/>
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px w-100px">优惠率(%)：</label>
          <t-input-number
              v-model="form.discountRate"
              theme="normal"
              :min="0"
              :decimal-places="2"
              :disabled="isAudited"
              @blur="discountRateComputeFinalAmount"
          />
          <label class="ml-10px mr-16px w-80px">优惠金额：</label>
          <t-input-number
              v-model="form.discountAmount"
              theme="normal"
              :min="0"
              :decimal-places="2"
              :disabled="isAudited"
              @blur="discountAmountComputeFinalAmount"
          />
          <label class="ml-16px mr-16px w-100px">客户承担：</label>
          <t-input v-model="form.customerAmount" type="number" @blur="updateCustomerAmount" :disabled="isAudited"/>
          <label class="ml-16px mr-16px w-100px">本次退款：</label>
          <t-input v-model="form.refundAmount" type="number" readonly :disabled="isAudited"/>
        </div>
      </div>
    </div>
    <div class="page-column-footer modal-column-between bg-white-color border">
      <t-button @click="closeWindow" :loading="loading">取消</t-button>
      <div>
        <t-button theme="primary" v-if="!isAudited" @click="saveOrder('add')" :loading="loading">保存并新增</t-button>
        <t-button v-if="!isAudited" @click="saveOrder('save')" :loading="loading">保存</t-button>
        <t-button @click="doPrint" :loading="loading">打印</t-button>
        <t-button v-if="form.id && !isAudited" @click="approved()" :loading="loading">审核</t-button>
        <t-button v-if="isAudited" @click="backApproved()" :loading="loading">反审核</t-button>
      </div>
    </div>
  </div>
</template>
<script>
import {LoadingPlugin, MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';
import {openPrint} from '@common/print';
import manba from "manba";
import Customer from "@js/api/basic/Customer";
import Warehouse from "@js/api/basic/Warehouse";
import {mapState} from "vuex";
import Product from "@js/api/basic/Product";
import {openDrawer, closeDialog} from '@common/dialog';
import {h} from "vue";
import Unit from "@js/api/basic/Unit";
import SalesOutboundSelect from "@views/sales/SalesOutboundSelect.vue";
import SalesReturn from "@js/api/sales/SalesReturn";
import Stamp from "@views/common/Stamp.vue";
import PriceRecord from "@js/api/basic/PriceRecord";

let rowSeq = 0;
function newRow(extra = {}) {
  return { _rowKey: `r-${++rowSeq}`, productId: null, ...extra };
}

export default {
  name: "SalesReturnForm",
  components: {Stamp},
  computed: {
    ...mapState(['accountBook']),
    isAudited() {
      return this.form.orderStatus === '已审核';
    },
    isDeleting() {
      return this.productData.some(r => !r.isNew);
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
        { colKey: 'imgPath', title: '产品图片', width: 100 },
        { colKey: 'productCode', title: '产品编码', width: 240 },
        { colKey: 'productInfo', title: '产品信息', width: 180, align: 'center' },
        { colKey: 'specification', title: '规格型号', align: 'center', width: 100 },
        { colKey: 'productCategoryName', title: '产品类别', align: 'center', width: 100 },
        { colKey: 'warehouse', title: '仓库', align: 'center', width: 180 },
        { colKey: 'quantity', title: '数量', width: 90 },
        { colKey: 'unitName', title: '单位', align: 'center', width: 80 },
        { colKey: 'unitPrice', title: '单价', width: 100 },
        { colKey: 'discountRate', title: '折扣率(%)', width: 100 },
        { colKey: 'discountValue', title: '折扣额', width: 100 },
        { colKey: 'subtotal', title: '金额', width: 100 },
        { colKey: 'remark', title: '备注', width: 200 },
        { colKey: 'salesOutboundNo', title: '关联销售出库单号', width: 200, align: 'center' },
      ];
    },
    footData() {
      let quantity = 0;
      let discountValue = 0;
      let subtotal = 0;
      (this.productData || []).forEach((row) => {
        quantity += Number(row.quantity || 0);
        discountValue += Number(row.discountValue || 0);
        subtotal += Number(row.subtotal || 0);
      });
      this.form.orderQuantity = quantity.toFixed(2);
      this.form.totalAmount = subtotal.toFixed(2);
      this.extracted();
      return [{
        ops: '合计',
        quantity: quantity.toFixed(2),
        discountValue: discountValue.toFixed(2),
        subtotal: subtotal.toFixed(2)
      }];
    }
  },
  data() {
    return {
      loading: false,
      productList: [],
      unitList: [],
      product: null,
      warehouseList: [],
      customerList: [],
      customerId: null,
      warehouseId: null,
      form: {
        id: null,
        returnDate: manba().format("YYYY-MM-dd"),
        customerId: null,
        discountAmount: 0.00,
        discountRate: 0.00,
        finalAmount: 0.00,
        customerAmount: 0.00,
        refundAmount: 0.00,
        returnReason: null,
        remarks: null,
        orderNo: null,
        orderStatus: null,
        totalAmount: 0.00,
        orderQuantity: 0.00,
      },
      productData: [],
      selectSalesOutboundIdList: [],
      orderId: null,
      type: null,
      recentSales: [],
    }
  },
  methods: {
    productImage(row) {
      const p = (this.productList || []).find(item => (item.productId || item.id) === row.productId);
      return p?.imgPath || '';
    },
    warehouseName(id) {
      return (this.warehouseList || []).find(w => w.id === id)?.name || '';
    },
    resolveDefaultWarehouseId() {
      if (this.warehouseId) return this.warehouseId;
      const found = (this.warehouseList || []).find(w => w.systemDefault || w.isDefault);
      this.warehouseId = found?.id || null;
      return this.warehouseId;
    },
    applyDefaultWarehouse(rows) {
      const defaultId = this.resolveDefaultWarehouseId();
      (rows || []).forEach(row => {
        if (row && !row.isNew && !row.warehouseId) {
          row.warehouseId = defaultId;
        }
      });
      return rows;
    },
    doPrint() {
      const items = (this.productData || []).filter(r => r && !r.isNew && r.productId).map(r => {
        const p = (this.productList || []).find(x => (x.productId || x.id) === r.productId) || {};
        return {
          ...r,
          productName: r.productName || p.productName || p.name || p.customName || '',
          quantity: r.quantity ?? r.secondaryQuantity,
          price: r.unitPrice ?? r.secondaryPrice ?? r.price,
          amount: r.subtotal ?? r.amount,
        };
      });
      openPrint('销售退货单', {
        header: {
          ...this.form,
          partner: (this.customerList.find(s => s.id === this.customerId) || {}).name || '',
          amount: this.form.refundAmount ?? this.form.finalAmount ?? this.form.totalAmount,
        },
        items,
      });
    },
    previewImage(url) {
      if (!url || url === '-') return;
      window.open(url, '_blank');
    },
    selectOutboundOrder() {
      if (!this.form.customerId) {
        MessagePlugin.error("请选择客户~");
        return;
      }
      let dialogId = openDrawer({
        header: "请选择销售出库单",
        closeOnOverlayClick: false,
        closeBtn: false,
        size: '1200px',
        body: h(SalesOutboundSelect, {
          customerId: this.customerId,
          onClose: () => closeDialog(dialogId),
          onSuccess: (params) => {
            this.handleSelectedOrders(params);
            closeDialog(dialogId);
          },
        }),
      });
    },
    handleSelectedOrders(params) {
      let itemList = params.itemList || [];
      const productMap = new Map(this.productList.map(product => [product.id, product]));
      const unitMap = new Map(this.unitList.map(unit => [unit.id, unit]));
      itemList.forEach(row => {
        const product = productMap.get(row.productId);
        if (product) {
          row.productName = product.name;
          row.productCode = product.code;
        }
        const unit = unitMap.get(row.baseUnitId);
        if (unit) {
          row.unitName = unit.name;
        }
        row.outItemId = row.id;
        row.id = null;
        if (row.returnQuantity == null) {
          row.returnQuantity = row.quantity;
        }
      });
      this.productData = this.applyDefaultWarehouse(itemList).map((row) => newRow({ ...row, isNew: false }));
      this.selectSalesOutboundIdList = params.selectSalesOutboundIdList || [];
    },
    extracted() {
      if (this.form.totalAmount > 0) {
        if (this.form.discountRate > 0) {
          this.discountRateComputeFinalAmount();
        } else if (this.form.discountAmount > 0) {
          this.discountAmountComputeFinalAmount();
        } else {
          this.form.finalAmount = this.form.totalAmount;
          this.updateCustomerAmount();
        }
      }
    },
    selectProduct(value, index) {
      const d = (this.productList || []).find((item) => String(item.id) === String(value));
      if (!d) return;
      const unitPrice = d.lastSalePrice || 0;
      let g = newRow({
        isNew: false,
        quantity: 1,
        unitPrice: unitPrice,
        warehouseId: this.resolveDefaultWarehouseId(),
        discountValue: 0.00,
        discountRate: 0.00,
        subtotal: unitPrice,
        baseUnitId: d.unitId,
        unitName: d.unitName,
        productId: d.id,
        productCode: d.code,
        productName: d.name,
        remark: "",
      });
      this.productData[index] = g;
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
      this.product = null;
    },
    checkHttp() {
      let productData = this.productData.filter(c => !c.isNew && c.productId);
      if (productData.length === 0) {
        MessagePlugin.error("请选择产品~");
        return false;
      }
      if (!this.form.returnDate) {
        MessagePlugin.error("请选择日期~");
        return false;
      }
      let quantityFlag = false;
      let unitPriceFlag = false;
      let subtotalFlag = false;
      let warehouseFlag = false;
      productData.forEach(item => {
        if (item.quantity === 0 || !item.quantity) quantityFlag = true;
        if (item.unitPrice === 0 || !item.unitPrice) unitPriceFlag = true;
        if (item.subtotal === 0 || !item.subtotal) subtotalFlag = true;
        if (!item.warehouseId) warehouseFlag = true;
      });
      if (quantityFlag) {
        MessagePlugin.error("请填写数量~");
        return false;
      }
      if (unitPriceFlag) {
        MessagePlugin.error("请填写单价~");
        return false;
      }
      if (subtotalFlag) {
        MessagePlugin.error("金额不能为空~");
        return false;
      }
      if (warehouseFlag) {
        MessagePlugin.error("请选择仓库~");
        return false;
      }
      return true;
    },
    saveOrder(saveType) {
      LoadingPlugin(true);
      if (!this.form.customerId) {
        MessagePlugin.error("请选择客户~");
        LoadingPlugin(false);
        return;
      }
      if (!this.checkHttp()) {
        LoadingPlugin(false);
        return;
      }
      let productData = this.productData
        .filter(c => !c.isNew && c.quantity > 0)
        .map(({ _rowKey, ...rest }) => rest);
      SalesReturn.save({
        salesReturn: Object.assign(this.form),
        salesReturnItemList: productData,
        selectSalesOutboundIdList: this.selectSalesOutboundIdList
      }).then((success) => {
        if (success) {
          MessagePlugin.success("保存成功~");
          this.clearForm();
          if (saveType === 'save') {
            this.closeWindow();
          }
        }
      }).finally(() => LoadingPlugin(false));
    },
    clearForm() {
      this.form = {
        id: null,
        returnDate: manba().format("YYYY-MM-dd"),
        customerId: null,
        discountAmount: 0.00,
        discountRate: 0.00,
        finalAmount: 0.00,
        customerAmount: 0.00,
        refundAmount: 0.00,
        returnReason: null,
        remarks: null,
        orderNo: null,
        orderStatus: null,
        totalAmount: 0.00,
        orderQuantity: 0.00,
      };
      this.productData = [newRow({ isNew: true })];
      this.customerId = null;
      this.selectSalesOutboundIdList = [];
    },
    adjustRows(type, index) {
      if (type === 'insert') {
        this.productData.splice(index + 1, 0, newRow({ isNew: true }));
      } else {
        this.productData.splice(index, 1);
      }
    },
    changeCustomer(value) {
      if (value == null || value === '') {
        this.form.customerId = null;
        this.productData = [newRow({ isNew: true })];
        this.selectSalesOutboundIdList = [];
        this.reloadProductList();
        return;
      }
      const e = (this.customerList || []).find((item) => String(item.id) === String(value));
      if (!e) return;
      if (e.id !== this.form.customerId) {
        const hasLines = this.productData.some(r => r && !r.isNew);
        if (hasLines) {
          DialogPlugin.confirm({
            header: "系统提示",
            body: `修改客户后，将清除已选择的产品数据，确定修改？`,
            onConfirm: () => {
              this.productData = [newRow({ isNew: true })];
              this.selectSalesOutboundIdList = [];
              this.form.customerId = e.id;
              return this.reloadProductList();
            }
          });
        } else {
          this.form.customerId = e.id;
          this.productData = [newRow({ isNew: true })];
          this.selectSalesOutboundIdList = [];
          this.reloadProductList();
        }
      }
    },
    reloadProductList() {
      return Product.select({customerId: this.form.customerId || this.customerId}).then(res => {
        this.productList = res.data || [];
        this.productList.forEach(item => {
          item.customName = `${item.code}--${item.name}`;
        });
      });
    },
    updateQuantity(item) {
      if (!item.productId) return;
      item.quantity = item.quantity || 1;
      if (item.returnQuantity != null && Number(item.quantity) > Number(item.returnQuantity)) {
        item.quantity = item.returnQuantity;
      }
      item.subtotal = ((item.quantity * item.unitPrice * (100 - item.discountRate)) / 100).toFixed(2);
      item.discountValue = (((item.quantity * item.unitPrice) * item.discountRate) / 100).toFixed(2);
    },
    updatePrice(item) {
      if (!item.productId) return;
      item.unitPrice = item.unitPrice || 0.00;
      item.discountValue = (item.unitPrice * item.quantity * item.discountRate / 100).toFixed(2);
      item.subtotal = (item.unitPrice * item.quantity - item.discountValue).toFixed(2);
    },
    showPrice(row) {
      let productId = row.productId;
      if (!productId) return;
      PriceRecord.price({
        productId,
        priceSource: '最近销售价格',
        priceType: '最近销售价格',
        page: 1,
        pageSize: 5
      }).then(({data: {results}}) => {
        this.recentSales = results || [];
      });
    },
    updateDiscount(item) {
      item.discountRate = item.discountRate || 0.00;
      item.subtotal = ((item.quantity || 0) * item.unitPrice * (100 - item.discountRate || 0) / 100).toFixed(2);
      item.discountValue = ((item.quantity || 0) * item.unitPrice - item.subtotal).toFixed(2);
    },
    updateDiscountAmount(item) {
      item.discountValue = item.discountValue || 0.00;
      item.discountRate = (((item.discountValue / (item.unitPrice * item.quantity)) * 100) || 0).toFixed(2);
      item.subtotal = (item.unitPrice * item.quantity - item.discountValue).toFixed(2);
    },
    updateFinalAmount(item) {
      item.subtotal = item.subtotal || 0;
      item.unitPrice = ((item.subtotal) / ((100 - item.discountRate)) * 100 / item.quantity).toFixed(2);
      item.discoutPrice = (item.unitPrice - item.subtotal).toFixed(2);
    },
    updateCustomerAmount() {
      this.form.refundAmount = (this.form.finalAmount - this.form.customerAmount).toFixed(2);
    },
    discountRateComputeFinalAmount() {
      this.form.finalAmount = (this.form.totalAmount * (100 - this.form.discountRate) / 100).toFixed(2);
      this.form.discountAmount = (this.form.totalAmount - this.form.finalAmount).toFixed(2);
      this.updateCustomerAmount();
    },
    discountAmountComputeFinalAmount() {
      this.form.finalAmount = (this.form.totalAmount - this.form.discountAmount).toFixed(2);
      this.form.discountRate = this.form.totalAmount === 0
          ? 0
          : ((this.form.totalAmount - this.form.finalAmount) / this.form.totalAmount * 100).toFixed(2);
      this.updateCustomerAmount();
    },
    approved() {
      DialogPlugin.confirm({
        header: "审核提示",
        body: `确认审核该订单?`,
        onConfirm: () => {
          return SalesReturn.approved('已审核', [this.form.id]).then(() => {
            MessagePlugin.success("操作成功~");
            this.closeWindow();
          });
        }
      });
    },
    backApproved() {
      DialogPlugin.confirm({
        header: "反审核提示",
        body: `确认反审核该订单?`,
        onConfirm: () => {
          return SalesReturn.approved('已保存', [this.form.id]).then(() => {
            MessagePlugin.success("操作成功~");
            this.closeWindow();
          });
        }
      });
    },
    closeWindow() {
      this.$store.commit('closeTabKey', this.$store.state.currentTab);
      this.$store.commit('newTab', "SalesReturnList");
      this.$nextTick(() => {
        this.$store.commit('SET_TAB_DATA_RETURN', {refresh: true});
      });
    },
  },
  created() {
    LoadingPlugin(true);
    Promise.all([
      Customer.select(),
      Warehouse.select(),
      Product.select(),
      Unit.select()
    ]).then((results) => {
      this.customerList = results[0].data || [];
      this.warehouseList = results[1].data || [];
      this.productList = results[2].data || [];
      this.unitList = results[3].data || [];
      this.productList.forEach(item => {
        item.customName = `${item.code}--${item.name}`;
      });
      this.warehouseId = this.warehouseList.find(val => val.systemDefault || val.isDefault)?.id;
      const tabData = this.$store.state.currentTabDataReturn;
      this.$store.commit('SET_TAB_DATA_RETURN', null);
      this.type = tabData?.type;
      this.orderId = tabData?.orderId;
      if (this.orderId) {
        SalesReturn.load(this.orderId).then(response => {
          let salesReturn = response.data;
          if (salesReturn) {
            this.form = Object.assign(this.form, salesReturn);
            this.customerId = salesReturn.customerId;
            if (this.type === 'copy') {
              this.form.id = null;
              this.form.orderStatus = null;
              this.form.orderNo = null;
            }
            this.productData = this.applyDefaultWarehouse(salesReturn.salesReturnItemList || [])
              .map((row) => newRow({ ...row, isNew: false }));
            this.reloadProductList();
          }
        });
      } else {
        this.productData = [newRow({ isNew: true })];
      }
    }).finally(() => LoadingPlugin(false));
  },
}
</script>
<style scoped>

.product-img {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
}

.recent-sales-table {
  min-width: 300px;
}

.recent-sales-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  border: 1px solid #dfe6ec;
}

.recent-sales-table th,
.recent-sales-table td {
  padding: 8px 12px;
  text-align: left;
  border: 1px solid #dfe6ec;
}

.recent-sales-table th {
  background-color: #f5f7fa;
  font-weight: bold;
  color: #606266;
}

.recent-sales-table tbody tr:hover {
  background-color: #f5f7fa;
}
</style>
