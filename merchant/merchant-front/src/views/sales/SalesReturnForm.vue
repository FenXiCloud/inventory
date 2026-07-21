<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <vxe-toolbar class-name="!size--mini">
        <template #buttons>
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
              :disable-date="{ before: accountBook.checkoutDate }"
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
        </template>
        <template #tools>
          <Stamp v-if="isAudited"/>
        </template>
      </vxe-toolbar>
      <vxe-table
          size="mini"
          ref="xTable"
          border
          show-overflow
          :row-config="{height: 40}"
          show-footer
          :footer-method="footerMethod"
          stripe
          :data="productData"
      >
        <vxe-column title="序号" type="seq" width="60" align="center" fixed="left"/>
        <vxe-column title="操作" field="seq" width="70" align="center" fixed="left">
          <template #default="{row,rowIndex}">
            <template v-if="!isAudited">
              <div class="fa fa-minus text-hover" v-if="isDeleting" @click="adjustRows('delete',rowIndex)"></div>
            </template>
          </template>
        </vxe-column>
        <vxe-column field="imgPath" title="产品图片" width="100">
          <template #default="{row}">
            <img
                :src="productImage(row)"
                alt=""
                class="product-img cursor-pointer"
                @click="previewImage(productImage(row))"
            >
          </template>
        </vxe-column>
        <vxe-column field="productCode" title="产品编码" width="240"/>
        <vxe-column title="产品信息" width="180" align="center">
          <template #default="{row,rowIndex}">
            <div class="input-group goodsSelect" v-if="row.isNew && !isAudited" @keyup.stop="void(0)">
              <t-select
                  ref="ms"
                  @change="selectProduct($event,rowIndex)"
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
        </vxe-column>
        <vxe-column title="规格型号" field="specification" align="center" width="100">
          <template #default="{row}">
            {{ productList.find(item => item.id === row.productId)?.specification || '-' }}
          </template>
        </vxe-column>
        <vxe-column title="产品类别" field="productCategoryName" align="center" width="100">
          <template #default="{row}">
            {{ productList.find(item => item.id === row.productId)?.productCategoryName || '-' }}
          </template>
        </vxe-column>
        <vxe-column title="仓库" field="warehouse" align="center" width="180">
          <template #default="{row}">
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
        </vxe-column>
        <vxe-column title="数量" field="quantity" width="90">
          <template #default="{row,rowIndex}">
            <vxe-input
                v-if="!row.isNew && !isAudited"
                :id="'r'+rowIndex+''+3"
                @blur="updateQuantity(row)"
                v-model.number="row.quantity"
                type="float"
                min="0"
                :max="row.returnQuantity"
                :controls="false"
            />
            <span v-else-if="!row.isNew">{{ row.quantity }}</span>
          </template>
        </vxe-column>
        <vxe-column title="单位" field="unitName" align="center" width="80"/>
        <vxe-column title="单价" field="unitPrice" width="100">
          <template #default="{row,rowIndex}">
            <template v-if="!row.isNew && !isAudited">
              <vxe-tooltip theme="light">
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
                <vxe-input
                    :id="'r'+rowIndex+''+4"
                    @blur="updatePrice(row)"
                    @focus="showPrice(row)"
                    v-model.number="row.unitPrice"
                    type="float"
                    min="0"
                    :controls="false"
                />
              </vxe-tooltip>
            </template>
            <span v-else-if="!row.isNew">{{ row.unitPrice }}</span>
          </template>
        </vxe-column>
        <vxe-column title="折扣率(%)" field="discountRate" width="100">
          <template #default="{row,rowIndex}">
            <vxe-input
                v-if="!row.isNew && !isAudited"
                :id="'r'+rowIndex+''+5"
                @blur="updateDiscount(row)"
                v-model.number="row.discountRate"
                type="float"
                min="0"
                :controls="false"
            />
            <span v-else-if="!row.isNew">{{ row.discountRate }}</span>
          </template>
        </vxe-column>
        <vxe-column title="折扣额" field="discountValue" width="100">
          <template #default="{row,rowIndex}">
            <vxe-input
                v-if="!row.isNew && !isAudited"
                :id="'r'+rowIndex+''+6"
                @blur="updateDiscountAmount(row)"
                v-model.number="row.discountValue"
                type="float"
                min="0"
                :controls="false"
            />
            <span v-else-if="!row.isNew">{{ row.discountValue }}</span>
          </template>
        </vxe-column>
        <vxe-column title="金额" field="subtotal" width="100">
          <template #default="{row,rowIndex}">
            <vxe-input
                v-if="!row.isNew && !isAudited"
                :id="'r'+rowIndex+''+7"
                @blur="updateFinalAmount(row)"
                v-model.number="row.subtotal"
                type="float"
                min="0"
                :controls="false"
            />
            <span v-else-if="!row.isNew">{{ row.subtotal }}</span>
          </template>
        </vxe-column>
        <vxe-column title="备注" field="remark" width="200">
          <template #default="{row,rowIndex}">
            <vxe-input
                v-if="!row.isNew && !isAudited"
                :id="'r'+rowIndex+''+8"
                v-model="row.remark"
                placeholder="输入备注"
                :controls="false"
            />
            <span v-else-if="!row.isNew">{{ row.remark }}</span>
          </template>
        </vxe-column>
        <vxe-column title="关联销售出库单号" field="salesOutboundNo" width="200" align="center"/>
      </vxe-table>
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
          <vxe-input
              v-model.number="form.discountRate"
              @blur="discountRateComputeFinalAmount"
              type="float"
              min="0"
              :controls="false"
              :disabled="isAudited"
          />
          <label class="ml-10px mr-16px w-80px">优惠金额：</label>
          <vxe-input
              v-model.number="form.discountAmount"
              @blur="discountAmountComputeFinalAmount"
              type="float"
              min="0"
              :controls="false"
              :disabled="isAudited"
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
      return p?.imgPath || '-';
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
      this.productData = this.applyDefaultWarehouse(itemList);
      this.selectSalesOutboundIdList = params.selectSalesOutboundIdList || [];
      this.$nextTick(() => this.$refs.xTable?.loadData(this.productData));
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
    footerMethod({columns, data}) {
      let quantity = 0;
      let discountValue = 0;
      let subtotal = 0;
      columns.forEach((column) => {
        if (column.property && ['quantity', 'discountValue', 'subtotal'].includes(column.property)) {
          data.forEach((row) => {
            if (column.property === 'quantity') {
              let rd = row[column.property];
              if (rd) quantity += Number(rd || 0);
            } else if (column.property === 'discountValue') {
              let rd = row[column.property];
              if (rd) discountValue += Number(rd || 0);
            } else if (column.property === 'subtotal') {
              let rd = row[column.property];
              if (rd) subtotal += Number(rd || 0);
            }
          });
        }
      });
      this.form.orderQuantity = quantity.toFixed(2);
      this.form.totalAmount = subtotal.toFixed(2);
      this.extracted();
      return [["", "", "", "", "", "", "", "", quantity.toFixed(2), "", "", "", discountValue.toFixed(2), subtotal.toFixed(2), ""]];
    },
    selectProduct(value, index) {
      const d = (this.productList || []).find((item) => String(item.id) === String(value));
      if (!d) return;
      const unitPrice = d.lastSalePrice || 0;
      let g = {
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
      };
      this.productData[index] = g;
      if (!this.productData[index + 1]) {
        this.productData.push({isNew: true});
      }
      this.$refs.xTable.loadData(this.productData).then(() => {
        this.$nextTick(() => {
          let str = index + '' + 3;
          let element = document.querySelector('#r' + str + ' input');
          setTimeout(() => {
            element?.focus();
            element?.select();
          }, 100);
        });
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
      let productData = this.productData.filter(c => !c.isNew && c.quantity > 0);
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
      this.productData = [{isNew: true}];
      this.customerId = null;
      this.selectSalesOutboundIdList = [];
    },
    adjustRows(type, index) {
      if (type === 'insert') {
        this.productData.splice(index + 1, 0, {isNew: true});
      } else {
        this.productData.splice(index, 1);
      }
    },
    changeCustomer(value) {
      if (value == null || value === '') {
        this.form.customerId = null;
        this.productData = [{isNew: true}];
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
              this.productData = [{isNew: true}];
              this.selectSalesOutboundIdList = [];
              this.form.customerId = e.id;
              return this.reloadProductList();
            }
          });
        } else {
          this.form.customerId = e.id;
          this.productData = [{isNew: true}];
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
      this.$refs.xTable.updateFooter();
    },
    updatePrice(item) {
      if (!item.productId) return;
      item.unitPrice = item.unitPrice || 0.00;
      item.discountValue = (item.unitPrice * item.quantity * item.discountRate / 100).toFixed(2);
      item.subtotal = (item.unitPrice * item.quantity - item.discountValue).toFixed(2);
      this.$refs.xTable.updateFooter();
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
      this.$refs.xTable.updateFooter();
    },
    updateDiscountAmount(item) {
      item.discountValue = item.discountValue || 0.00;
      item.discountRate = (((item.discountValue / (item.unitPrice * item.quantity)) * 100) || 0).toFixed(2);
      item.subtotal = (item.unitPrice * item.quantity - item.discountValue).toFixed(2);
      this.$refs.xTable.updateFooter();
    },
    updateFinalAmount(item) {
      item.subtotal = item.subtotal || 0;
      item.unitPrice = ((item.subtotal) / ((100 - item.discountRate)) * 100 / item.quantity).toFixed(2);
      item.discoutPrice = (item.unitPrice - item.subtotal).toFixed(2);
      this.$refs.xTable.updateFooter();
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
            this.productData = this.applyDefaultWarehouse(salesReturn.salesReturnItemList || []);
            this.reloadProductList();
          }
        });
      } else {
        this.productData = [{isNew: true}];
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
