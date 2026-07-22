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
          <label class="mr-20px ml-16px" style="font-size: 16px !important;">单据日期：</label>
          <t-date-picker
              v-model="form.orderDate"
              :disable-date="{ before: accountBook.checkoutDate }"
              :clearable="false"
              :disabled="isAudited"
          />
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
            <div class="fa fa-plus text-hover mr-5px" @click="adjustRows('insert', rowIndex)"></div>
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
          <div class="input-group goodsSelect" @keyup.stop="void(0)" v-if="!isAudited">
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
          <div v-else>{{ row.productCode }}--{{ row.productName }}</div>
        </template>
        <template #specification="{ row }">
          {{ productSpec(row) }}
        </template>
        <template #productCategoryName="{ row }">
          {{ productCategory(row) }}
        </template>
        <template #warehouse="{ row }">
          <template v-if="!row.isNew && !isAudited">
            <t-select
                :clearable="false"
                v-model="row.warehouseId"
                :options="warehouseList"
                filterable
                @change="handleWarehouseChange(row, $event)"
                :keys="{ value: 'id', label: 'name' }"
            />
          </template>
          <span v-else-if="!row.isNew">{{ warehouseName(row.warehouseId) }}</span>
        </template>
        <template #quantity="{ row, rowIndex }">
          <template v-if="!row.isNew && !isAudited">
            <t-tooltip theme="light">
              <template #content>
                <div>当前库存: {{ row.currentStockQuantity || 0 }}</div>
                <div>总库存: {{ row.totalStockQuantity || 0 }}</div>
              </template>
              <t-input-number
                  :id="'r'+rowIndex+''+3"
                  v-model="row.quantity"
                  theme="normal"
                  :min="0"
                  :decimal-places="2"
                  style="width: 100%"
                  @blur="updateQuantity(row)"
                  @focus="showStockQuantity(row)"
              />
            </t-tooltip>
          </template>
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
                      <th>{{ customerLevel }}</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr v-for="(item, index) in recentSales || []" :key="index">
                      <td>{{ item.orderDate || '-' }}</td>
                      <td>{{ item.unitPrice || '-' }}</td>
                      <td>{{ item.customerPrice || '-' }}</td>
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
              disabled
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
          <label class="ml-16px mr-16px w-100px">优惠后金额：</label>
          <t-input-number
              v-model="form.finalAmount"
              theme="normal"
              :min="0"
              :decimal-places="2"
              disabled
          />
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px w-100px">备注说明：</label>
          <t-input placeholder="请输入备注" maxlength="150" v-model="form.remarks" :disabled="isAudited"/>
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
    <div v-if="previewVisible" class="image-preview-modal" @click="previewVisible = false">
      <div class="image-preview-container">
        <img :src="previewImageUrl" class="preview-image" alt="产品图片预览">
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
import {mapMutations, mapState} from "vuex";
import SalesOrder from "@js/api/sales/SalesOrder";
import Product from "@js/api/basic/Product";
import Inventory from "@js/api/inventory/Inventory";
import Stamp from "@views/common/Stamp.vue";
import PriceRecord from "@js/api/basic/PriceRecord";

let rowSeq = 0;
function newRow(extra = {}) {
  return { _rowKey: `r-${++rowSeq}`, productId: null, isNew: true, ...extra };
}

export default {
  name: "SalesOrderForm",
  components: {Stamp},
  computed: {
    ...mapState(['accountBook']),
    isAudited() {
      return this.form.orderStatus === '已审核';
    },
    isDeleting() {
      return this.productData.length > 1;
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
        { colKey: 'productCode', title: '产品编码', width: 120 },
        { colKey: 'productInfo', title: '产品信息', minWidth: 300 },
        { colKey: 'specification', title: '规格型号', align: 'center', width: 100 },
        { colKey: 'productCategoryName', title: '产品类别', align: 'center', width: 100 },
        { colKey: 'warehouse', title: '仓库', align: 'center', width: 120 },
        { colKey: 'quantity', title: '数量', width: 90 },
        { colKey: 'unitName', title: '单位', align: 'center', width: 80 },
        { colKey: 'unitPrice', title: '单价', width: 100 },
        { colKey: 'discountRate', title: '折扣率(%)', width: 100 },
        { colKey: 'discountValue', title: '折扣额', width: 100 },
        { colKey: 'subtotal', title: '金额', width: 100 },
        { colKey: 'remark', title: '备注', width: 160 },
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
      if (this.form.totalAmount > 0) {
        if (this.form.discountRate > 0) {
          this.discountRateComputeFinalAmount();
        } else if (this.form.discountAmount > 0) {
          this.discountAmountComputeFinalAmount();
        } else {
          this.form.finalAmount = this.form.totalAmount;
        }
      }
      return [{
        ops: '合计',
        quantity: quantity.toFixed(2),
        discountValue: discountValue,
        subtotal: subtotal
      }];
    }
  },
  data() {
    return {
      loading: false,
      productList: [],
      product: null,
      warehouseList: [],
      customerList: [],
      customerId: null,
      warehouseId: null,
      form: {
        id: null,
        orderDate: manba().format("YYYY-MM-dd"),
        customerId: null,
        discountAmount: 0.00,
        discountRate: 0.00,
        finalAmount: 0.00,
        remarks: null,
        orderStatus: null,
      },
      productData: [newRow({ isNew: true })],
      orderId: null,
      type: null,
      previewVisible: false,
      previewImageUrl: '',
      recentSales: [],
      customerLevel: null,
      customerLevelId: null,
      customerPrice: null,
    };
  },
  methods: {
    ...mapMutations(['newTab']),
    productImage(row) {
      const p = (this.productList || []).find(item => (item.productId || item.id) === row.productId);
      return p?.imgPath || '-';
    },
    productSpec(row) {
      const p = (this.productList || []).find(item => (item.productId || item.id) === row.productId);
      return p?.specification || '-';
    },
    productCategory(row) {
      const p = (this.productList || []).find(item => (item.productId || item.id) === row.productId);
      return p?.productCategoryName || '-';
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
    doPrint() {
      const items = (this.productData || []).filter(r => r && !r.isNew && r.productId).map(r => {
        const p = (this.productList || []).find(x => (x.productId || x.id) === r.productId) || {};
        return {
          ...r,
          productName: r.productName || p.name || '',
          quantity: r.quantity ?? r.secondaryQuantity,
          price: r.unitPrice ?? r.secondaryPrice ?? r.price,
          amount: r.subtotal ?? r.amount,
        };
      });
      openPrint('销售订单', {
        header: {
          ...this.form,
          partner: (this.customerList.find(s => s.id === this.customerId) || {}).name || '',
          amount: this.form.finalAmount ?? this.form.totalAmount,
        },
        items,
      });
    },
    selectProduct(value, index) {
      const d = (this.productList || []).find((item) => String(item.id) === String(value));
      if (!d) return;
      let unitPrice = d.lastSalePrice || 0;
      const defaultWarehouseId = this.resolveDefaultWarehouseId();
      let g = newRow({
        isNew: false,
        quantity: 1,
        unitPrice: unitPrice,
        warehouseId: defaultWarehouseId,
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
      if (!g.warehouseId) {
        g.warehouseId = defaultWarehouseId;
      }
      this.productData[index] = g;
      if (!this.productData[index + 1]) {
        this.productData.push(newRow({ isNew: true }));
      }
      this.$nextTick(() => {
        let element = document.querySelector('#r' + index + '3 input');
        setTimeout(() => {
          element?.focus();
          element?.select();
        }, 100);
      });
      this.showStockQuantity(g);
      this.showPrice(g);
    },
    checkHttp() {
      if (this.productData.length === 0) {
        MessagePlugin.error("请选择产品~");
        LoadingPlugin(false);
        return false;
      }
      if (this.productData.length === 1) {
        let item = this.productData[0];
        if (item.isNew) {
          MessagePlugin.error("请选择产品~");
          LoadingPlugin(false);
          return false;
        }
      }
      let quantityFlag = false;
      let unitPriceFlag = false;
      let subtotalFlag = false;
      let warehouseFlag = false;
      this.productData.forEach(item => {
        if (item.isNew) return;
        if (item.quantity === 0 || !item.quantity) {
          quantityFlag = true;
          LoadingPlugin(false);
        }
        if (item.unitPrice === 0 || !item.unitPrice) {
          unitPriceFlag = true;
          LoadingPlugin(false);
        }
        if (item.subtotal === 0 || !item.subtotal) {
          subtotalFlag = true;
          LoadingPlugin(false);
        }
        if (!item.warehouseId) {
          warehouseFlag = true;
          LoadingPlugin(false);
        }
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
    saveOrder(type) {
      LoadingPlugin(true);
      if (!this.form.customerId) {
        MessagePlugin.error("请选择客户~");
        LoadingPlugin(false);
        return;
      }
      if (!this.checkHttp()) {
        return;
      }
      let productData = this.productData
        .filter(c => !c.isNew && c.quantity > 0)
        .map(({ _rowKey, ...rest }) => rest);
      SalesOrder.save({
        salesOrder: Object.assign(this.form),
        salesOrderItemList: productData
      }).then((success) => {
        if (success) {
          MessagePlugin.success("保存成功~");
          this.clearForm();
          if (type === 'save') {
            this.closeWindow();
          }
        }
      }).finally(() => LoadingPlugin(false));
    },
    clearForm() {
      this.form = {
        id: null,
        orderDate: manba().format("YYYY-MM-dd"),
        customerId: null,
        remarks: null,
        discountAmount: 0.00,
        discountRate: 0.00,
        finalAmount: 0.00,
        orderStatus: null,
      };
      this.productData = [newRow({ isNew: true })];
      this.customerId = null;
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
        this.customerLevelId = null;
        this.reloadProductList();
        return;
      }
      const e = (this.customerList || []).find((item) => String(item.id) === String(value));
      if (!e) return;
      if (e.customerLevelId === 1) {
        this.customerLevel = "零售价";
      } else {
        this.customerLevel = "会员价";
      }
      this.customerLevelId = e.customerLevelId;
      if (e.id !== this.form.customerId) {
        if (this.productData.length > 1) {
          DialogPlugin.confirm({
            header: "系统提示",
            body: `修改客户后，将清除已选择的产品数据，确定修改？`,
            onConfirm: () => {
              this.productData = [newRow({ isNew: true })];
              this.form.customerId = e.id;
              return this.reloadProductList();
            }
          });
        } else {
          this.form.customerId = e.id;
          this.productData = [newRow({ isNew: true })];
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
    handleWarehouseChange(row) {
      this.showStockQuantity(row);
    },
    showStockQuantity(row) {
      let productId = row.productId;
      let warehouseId = row.warehouseId;
      if (!productId) return;
      Inventory.list({productId, page: 1, pageSize: 1000}).then(res => {
        const {data} = res;
        if (data && data.results) {
          let totalQuantity = 0;
          let quantity = 0;
          data.results.forEach(item => {
            totalQuantity += Number(item.currentQuantity);
            if (Number(item.warehouseId) === Number(warehouseId)) {
              quantity = item.currentQuantity;
            }
          });
          row.currentStockQuantity = quantity;
          row.totalStockQuantity = totalQuantity;
        }
      });
    },
    changeProductUnit(value, row) {
      const item = (row.auxiliaryUnitPrices || []).find((u) => String(u.unitId) === String(value));
      if (!item) return;
      row.orderUnitName = item.unitName;
      row.unitPrice = (item.price || 0).toFixed(2) || 0;
      row.num = item.num || 1;
      row.sysQuantity = (row.quantity * row.num).toFixed(2);
      row.subtotal = (row.quantity * row.unitPrice).toFixed(2);
    },
    updateQuantity(item) {
      if (!item.productId) return;
      item.quantity = item.quantity || 1;
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
        this.recentSales.forEach(item => {
          item.customerPrice = this.customerPrice;
        });
      }).finally(() => this.loading = false);
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
    approved() {
      DialogPlugin.confirm({
        header: "审核提示",
        body: `确认审核该订单?`,
        onConfirm: () => {
          return SalesOrder.approved('已审核', [this.form.id]).then(() => {
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
          return SalesOrder.approved('已保存', [this.form.id]).then(() => {
            MessagePlugin.success("操作成功~");
            this.closeWindow();
          });
        }
      });
    },
    closeWindow() {
      this.$store.commit('closeTabKey', this.$store.state.currentTab);
      this.$store.commit('newTab', "SalesOrderList");
      this.$nextTick(() => {
        this.$store.commit('SET_TAB_DATA', {refresh: true});
      });
    },
    discountRateComputeFinalAmount() {
      this.form.finalAmount = (this.form.totalAmount * (100 - this.form.discountRate) / 100).toFixed(2);
      this.form.discountAmount = (this.form.totalAmount - this.form.finalAmount).toFixed(2);
    },
    discountAmountComputeFinalAmount() {
      this.form.finalAmount = (this.form.totalAmount - this.form.discountAmount).toFixed(2);
      this.form.discountRate = ((this.form.totalAmount - this.form.finalAmount) / this.form.totalAmount * 100).toFixed(2);
    },
    previewImage(url) {
      if (!url || url === '-') return;
      this.previewImageUrl = url;
      this.previewVisible = true;
    },
  },
  created() {
    LoadingPlugin(true);
    Promise.all([
      Customer.select(),
      Warehouse.select(),
      Product.select()
    ]).then((results) => {
      this.customerList = results[0].data || [];
      this.warehouseList = results[1].data || [];
      this.productList = results[2].data || [];
      this.productList.forEach(item => {
        item.customName = `${item.code}--${item.name}`;
      });
      this.warehouseId = this.warehouseList.find(val => val.systemDefault || val.isDefault)?.id;

      const tabData = this.$store.state.currentTabData;
      this.$store.commit('SET_TAB_DATA', null);
      this.type = tabData?.type;
      this.orderId = tabData?.orderId;
      if (this.orderId) {
        SalesOrder.load(this.orderId).then(response => {
          let salesOrder = response.data;
          this.form = salesOrder;
          this.customerId = salesOrder.customerId;
          if ('copy' === this.type) {
            this.form.id = null;
            this.form.orderStatus = null;
          }
          const customer = this.customerList.find(c => c.id === this.customerId);
          if (customer) {
            this.customerLevelId = customer.customerLevelId;
            this.customerLevel = customer.customerLevelId === 1 ? "零售价" : "会员价";
          }
          this.productData = (salesOrder.salesOrderItemList || []).map((row) => newRow({ ...row, isNew: false }))
            .concat([newRow({ isNew: true })]);
          this.reloadProductList();
        });
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

.image-preview-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-preview-container {
  max-width: 90%;
  max-height: 90%;
  background: #fff;
  padding: 10px;
  border-radius: 5px;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
</style>
