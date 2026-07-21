<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <vxe-toolbar class-name="!size--mini">
        <template #buttons>
          <label class="mr-20px" style="font-size: 16px !important;">供货商：</label>
          <t-select
              class="w-300px"
              filterable
              :options="supplierList"
              :clearable="false"
              :disabled="isAudited"
              @change="changeSupplier($event)"
              v-model="supplierId"
              placeholder="请选择供货商"
                :keys="{ value: 'id', label: 'name' }"
              />
          <label class="mr-20px ml-16px" style="font-size: 16px !important;">入库日期：</label>
          <t-date-picker
              v-model="form.inboundDate"
              :disable-date="{ before: accountBook.checkoutDate }"
              :clearable="false"
              :disabled="isAudited"
          />
          <t-button
              v-if="type==='add' && !isAudited"
              @click="selectPurchaseOrder()"
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
              <div class="fa fa-plus text-hover mr-5px" @click="adjustRows('insert',rowIndex)"></div>
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
        <vxe-column title="产品信息" min-width="300">
          <template #default="{row,rowIndex}">
            <div class="input-group goodsSelect" @keyup.stop="void(0)" v-if="!isAudited">
              <t-select
                  ref="ms"
                  @change="selectProduct($event,rowIndex)"
                  v-model="row.productId"
                  :options="productList"
                  filterable
                  placeholder="输入编码/名称"
                :keys="{ value: 'productId', label: 'productName' }"
              />
            </div>
            <div v-else>{{ row.productCode }}--{{ row.productName }}</div>
          </template>
        </vxe-column>
        <vxe-column title="采购单位" field="secondaryUnitName" align="center" width="80">
          <template #default="{row}">
            <template v-if="!row.isNew && !isAudited">
              <t-select
                  v-if="row.auxiliaryUnitPrices"
                  :clearable="false"
                  @change="changeProductUnit($event,row)"
                  v-model="row.secondaryUnitId"
                  :options="row.auxiliaryUnitPrices"
                  filterable
                  placeholder="输入采购单位"
                :keys="{ value: 'unitId', label: 'unitName' }"
              />
              <span v-else>{{ row.secondaryUnitName }}</span>
            </template>
            <span v-else-if="!row.isNew">{{ row.secondaryUnitName }}</span>
          </template>
        </vxe-column>
        <vxe-column title="产品类别" field="categoryName" align="center" width="80"/>
        <vxe-column title="规格型号" field="spec" align="center" width="80"/>
        <vxe-column title="仓库" field="warehouse" align="center" width="120">
          <template #default="{row}">
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
        </vxe-column>
        <vxe-column title="数量" field="secondaryQuantity" width="90">
          <template #default="{row,rowIndex}">
            <template v-if="!row.isNew && !isAudited">
              <vxe-tooltip theme="light">
                <template #content>
                  <div>当前库存: {{ row.currentStockQuantity || 0 }}</div>
                  <div>总库存: {{ row.totalStockQuantity || 0 }}</div>
                </template>
                <vxe-input
                    :id="'r'+rowIndex+''+3"
                    @blur="updateQuantity(row)"
                    @focus="showStockQuantity(row)"
                    v-model.number="row.secondaryQuantity"
                    type="float"
                    min="0"
                    :controls="false"
                />
              </vxe-tooltip>
            </template>
            <span v-else-if="!row.isNew">{{ row.secondaryQuantity }}</span>
          </template>
        </vxe-column>
        <vxe-column title="基本单位" field="baseUnitName" align="center" width="80"/>
        <vxe-column title="基本数量" field="quantity" width="90"/>
        <vxe-column title="购货单价" field="secondaryPrice" width="100">
          <template #default="{row,rowIndex}">
            <template v-if="!row.isNew && !isAudited">
              <vxe-tooltip theme="light">
                <template #content>
                  <div class="recent-sales-table">
                    <table>
                      <thead>
                      <tr>
                        <th>最近采购时间</th>
                        <th>最近采购价</th>
                        <th>预计采购价</th>
                      </tr>
                      </thead>
                      <tbody>
                      <tr v-for="(item, index) in recentSales || []" :key="index">
                        <td>{{ item.orderDate || '-' }}</td>
                        <td>{{ item.unitPrice || '-' }}</td>
                        <td>{{ item.purchasePrice || '-' }}</td>
                      </tr>
                      </tbody>
                    </table>
                  </div>
                </template>
                <vxe-input
                    :id="'r'+rowIndex+''+4"
                    @keyup="handleEnter($event,rowIndex,4)"
                    @blur="updatePrice(row)"
                    @focus="showPrice(row.productId)"
                    v-model.number="row.secondaryPrice"
                    type="float"
                    min="0"
                    :controls="false"
                />
              </vxe-tooltip>
            </template>
            <span v-else-if="!row.isNew">{{ row.secondaryPrice }}</span>
          </template>
        </vxe-column>
        <vxe-column title="折扣率(%)" field="discountRate" width="100">
          <template #default="{row,rowIndex}">
            <vxe-input
                v-if="!row.isNew && !isAudited"
                :id="'r'+rowIndex+''+5"
                @keyup="handleEnter($event,rowIndex,5)"
                @blur="updateDiscount(row)"
                v-model.number="row.discountRate"
                type="float"
                min="0"
                :controls="false"
            />
            <span v-else-if="!row.isNew">{{ row.discountRate }}</span>
          </template>
        </vxe-column>
        <vxe-column title="折扣额" field="discountAmount" width="100">
          <template #default="{row,rowIndex}">
            <vxe-input
                v-if="!row.isNew && !isAudited"
                :id="'r'+rowIndex+''+6"
                @keyup="handleEnter($event,rowIndex,6)"
                @blur="updateDiscountAmount(row)"
                v-model.number="row.discountAmount"
                type="float"
                min="0"
                :controls="false"
            />
            <span v-else-if="!row.isNew">{{ row.discountAmount }}</span>
          </template>
        </vxe-column>
        <vxe-column title="购货金额" field="subtotal" width="100">
          <template #default="{row,rowIndex}">
            <vxe-input
                v-if="!row.isNew && !isAudited"
                :id="'r'+rowIndex+''+7"
                @keyup="handleEnter($event,rowIndex,7)"
                @blur="updateSubtotal(row)"
                v-model.number="row.subtotal"
                type="float"
                min="0"
                :controls="false"
            />
            <span v-else-if="!row.isNew">{{ row.subtotal }}</span>
          </template>
        </vxe-column>
        <vxe-column title="备注" field="remark" width="160">
          <template #default="{row,rowIndex}">
            <vxe-input
                v-if="!row.isNew && !isAudited"
                :id="'r'+rowIndex+''+8"
                @keyup="handleEnter($event,rowIndex,8)"
                v-model="row.remark"
                placeholder="输入备注"
                :controls="false"
            />
            <span v-else-if="!row.isNew">{{ row.remark }}</span>
          </template>
        </vxe-column>
      </vxe-table>
      <div class="mt-10px"></div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px w-80px">备注说明：</label>
          <t-input placeholder="请输入备注" maxlength="150" v-model="form.remarks" :disabled="isAudited"/>
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px w-80px">优惠率：</label>
          <t-input v-model="form.discountRate" @blur="changeDiscountRate" :disabled="isAudited"/>
          <label class="ml-10px mr-16px w-80px">优惠金额：</label>
          <t-input v-model="form.discountAmount" @blur="changeDiscountAmount" :disabled="isAudited"/>
          <label class="ml-16px mr-16px w-100px">优惠后金额：</label>
          <t-input v-model="form.finalAmount" @blur="changeFinalAmount" :disabled="isAudited"/>
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
import {CopyObj} from "@common/utils";
import Supplier from "@js/api/basic/Supplier";
import Warehouse from "@js/api/basic/Warehouse";
import {mapState} from "vuex";
import {openDrawer, closeDialog} from '@common/dialog';
import {h} from "vue";
import PurchaseOrderSelect from "@views/purchase/PurchaseOrderSelect.vue";
import PurchaseInbound from "@js/api/purchase/PurchaseInbound";
import PurchaseOrder from "@js/api/purchase/PurchaseOrder";
import PriceRecord from "@js/api/basic/PriceRecord";
import Inventory from "@js/api/inventory/Inventory";
import Stamp from "@views/common/Stamp.vue";

export default {
  name: "PurchaseInboundForm",
  components: {Stamp},
  props: {
    orderId: [String, Number],
    type: String,
  },
  computed: {
    ...mapState(['accountBook']),
    isAudited() {
      return this.form.orderStatus === '已审核';
    },
    isDeleting() {
      return this.productData.length > 1;
    }
  },
  data() {
    return {
      loading: false,
      productList: [],
      product: null,
      allFinalAmount: 0,
      warehouseList: [],
      supplierList: [],
      orderIds: [],
      supplierId: null,
      warehouseId: null,
      form: {
        id: null,
        inboundDate: manba().format("YYYY-MM-dd"),
        supplierId: null,
        discountAmount: 0.00,
        discountRate: 0.00,
        finalAmount: 0.00,
        remarks: null,
        orderStatus: null,
      },
      productData: [{isNew: true}],
      recentSales: [],
    }
  },
  watch: {
    allFinalAmount(val) {
      this.form.discountAmount = (val * this.form.discountRate * 0.01).toFixed(2)
      this.form.finalAmount = (val - this.form.discountAmount).toFixed(2)
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
          productName: r.productName || p.productName || p.name || '',
          quantity: r.quantity ?? r.secondaryQuantity,
          price: r.unitPrice ?? r.secondaryPrice ?? r.price,
          amount: r.subtotal ?? r.amount,
        };
      });
      openPrint('采购入库单', {
        header: {
          ...this.form,
          partner: (this.supplierList.find(s => s.id === this.supplierId) || {}).name || '',
          amount: this.form.finalAmount ?? this.form.totalAmount,
        },
        items,
      });
    },
    previewImage(url) {
      if (!url || url === '-') return;
      window.open(url, '_blank');
    },
    selectPurchaseOrder() {
      if (!this.form.supplierId) {
        MessagePlugin.error("请选择供货商~");
        return;
      }
      let dialogId = openDrawer({
        header: "请选择采购订单",
        closeOnOverlayClick: false,
        closeBtn: false,
        size: '1200px',
        body: h(PurchaseOrderSelect, {
          supplierId: this.supplierId,
          onClose: () => closeDialog(dialogId),
          onSuccess: (params) => {
            this.loadToInbound(params);
            closeDialog(dialogId);
          },
        }),
      });
    },
    loadToInbound(params) {
      this.orderIds = params.orderIds;
      PurchaseOrder.toInbound(this.form.supplierId, params.orderIds).then(({data}) => {
        const rows = this.applyDefaultWarehouse(data || []);
        this.productData = rows.concat([{isNew: true}]);
        this.$nextTick(() => this.$refs.xTable?.loadData(this.productData));
      });
    },
    handleEnter(e, index, num) {
      e.$event.stopPropagation();
      if (e.$event.keyCode === 13) {
        e.$input.blur();
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
      }
    },
    footerMethod({columns, data}) {
      let quantity = 0;
      let sums = [];
      data.forEach((row) => {
        if (row.quantity > 0) {
          quantity += Number(row.quantity || 0);
        }
      });
      columns.forEach((column, columnIndex) => {
        if (columnIndex === 0) {
          sums.push('合计');
        } else if (column.property && ['subtotal', 'discountAmount'].includes(column.property)) {
          let total = 0;
          data.forEach((row) => {
            let rd = row[column.property];
            if (rd) total += Number(rd || 0);
          });
          if (column.property !== 'quantity') {
            sums.push(total.toFixed(2));
          }
        }
      });
      this.allFinalAmount = sums[1];
      return [['', '', '', '', '', '', '', '', '', '', quantity.toFixed(0), '', ''].concat(sums)];
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
    selectProduct(value, index) {
      const d = (this.productList || []).find((item) => String(item.productId) === String(value));
      if (!d) return;
      const defaultWarehouseId = this.resolveDefaultWarehouseId();
      let g = {
        quantity: 1,
        secondaryQuantity: 1,
        secondaryPrice: d.price || 0,
        warehouseId: defaultWarehouseId,
        price: d.price || 0,
        discountAmount: 0.00,
        discountRate: 0.00,
        subtotal: d.price || 0,
        conversionRate: 1,
        secondaryUnitId: d.unitId,
        secondaryUnitName: d.unitName,
        baseUnitId: d.unitId,
        baseUnitName: d.unitName,
        categoryName: d.categoryName,
        spec: d.spec,
        remark: ""
      };
      this.productData[index] = Object.assign({}, d, g);
      if (!this.productData[index].warehouseId) {
        this.productData[index].warehouseId = defaultWarehouseId;
      }
      if (!this.productData[index + 1]) {
        this.productData.push({isNew: true});
      }
      this.$refs.xTable.loadData(this.productData).then(() => {
        this.$nextTick(() => {
          let element = document.querySelector('#r' + index + '3 input');
          setTimeout(() => {
            element?.focus();
            element?.select();
          }, 100);
        });
      });
      this.showPrice(d.productId);
      this.product = null;
    },
    showPrice(productId) {
      if (!productId) return;
      PriceRecord.purchasePrice({productId}).then(({data}) => {
        this.recentSales = data || [];
      });
    },
    saveOrder(type) {
      LoadingPlugin(true);
      if (!this.form.supplierId) {
        MessagePlugin.error("请选择供货商~");
        LoadingPlugin(false);
        return;
      }
      let productData = this.productData.filter(c => !c.isNew && c.quantity > 0);
      if (productData.length <= 0) {
        MessagePlugin.error("请选择产品~");
        LoadingPlugin(false);
        return;
      }
      if (productData.some(c => !c.warehouseId)) {
        MessagePlugin.error("请选择仓库~");
        LoadingPlugin(false);
        return;
      }
      PurchaseInbound.save({
        purchaseInbound: Object.assign(this.form, {totalAmount: this.allFinalAmount}),
        type: this.type,
        orderIds: this.orderIds,
        purchaseInboundItemList: productData
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
        inboundDate: manba().format("YYYY-MM-dd"),
        supplierId: null,
        remarks: null,
        discountAmount: 0.00,
        discountRate: 0.00,
        finalAmount: 0.00,
        orderStatus: null,
      };
      this.allFinalAmount = 0;
      this.productData = [{isNew: true}];
      this.supplierId = null;
      this.orderIds = [];
    },
    adjustRows(type, index) {
      if (type === 'insert') {
        this.productData.splice(index + 1, 0, {isNew: true});
      } else {
        this.productData.splice(index, 1);
      }
    },
    changeSupplier(value) {
      if (value == null || value === '') {
        this.form.supplierId = null;
        this.productData = [{ isNew: true }];
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
              this.productData = [{ isNew: true }];
              this.form.supplierId = e.id;
              this.loadProductsBySupplier();
            }
          });
        } else {
          this.form.supplierId = e.id;
          this.productData = [{ isNew: true }];
          this.loadProductsBySupplier();
        }
      }
    },
    loadProductsBySupplier() {
      if (!this.form.supplierId) return;
      Supplier.selectProduct(this.form.supplierId).then(({data}) => {
        this.productList = data || [];
        if (!this.form.id) {
          this.productData = [{isNew: true}];
        }
      }).finally(() => {
        this.$refs.xTable?.loadData(this.productData);
      });
    },
    changeDiscountRate() {
      this.form.discountRate = parseFloat(this.form.discountRate) || 0;
      this.form.discountAmount = (this.allFinalAmount * this.form.discountRate * 0.01).toFixed(2);
      this.form.finalAmount = (this.allFinalAmount - this.form.discountAmount).toFixed(2);
    },
    changeDiscountAmount() {
      this.form.discountAmount = parseFloat(this.form.discountAmount) || 0;
      this.form.finalAmount = (this.allFinalAmount - this.form.discountAmount).toFixed(2);
      this.form.discountRate = this.form.discountAmount === 0 ? 0 : ((this.form.discountAmount / this.allFinalAmount) * 100).toFixed(2);
    },
    changeFinalAmount() {
      this.form.finalAmount = parseFloat(this.form.finalAmount) || 0;
      this.form.discountAmount = (this.allFinalAmount - this.form.finalAmount).toFixed(2);
      this.form.discountRate = this.allFinalAmount === 0 ? 0 : ((this.form.discountAmount / this.allFinalAmount) * 100).toFixed(2);
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
      item.subtotal = ((item.secondaryQuantity * item.secondaryPrice * (100 - item.discountRate)) / 100).toFixed(2);
      item.discountAmount = (((item.secondaryQuantity * item.secondaryPrice) * item.discountRate) / 100).toFixed(2);
      item.quantity = (item.secondaryQuantity * (item.conversionRate || 1)).toFixed(2);
      this.$refs.xTable.updateFooter();
    },
    updatePrice(item) {
      item.secondaryPrice = item.secondaryPrice || 0.00;
      item.discountAmount = (item.secondaryPrice * item.secondaryQuantity * item.discountRate / 100).toFixed(2);
      item.subtotal = (item.secondaryPrice * item.secondaryQuantity - item.discountAmount).toFixed(2);
      this.$refs.xTable.updateFooter();
    },
    updateDiscount(item) {
      item.discountRate = item.discountRate || 0.00;
      item.subtotal = ((item.secondaryQuantity || 0) * item.secondaryPrice * (100 - item.discountRate || 0) / 100).toFixed(2);
      item.discountAmount = ((item.secondaryQuantity || 0) * item.secondaryPrice - item.subtotal).toFixed(2);
      this.$refs.xTable.updateFooter();
    },
    updateDiscountAmount(item) {
      item.discountAmount = item.discountAmount || 0.00;
      item.discountRate = (((item.discountAmount / (item.secondaryPrice * item.secondaryQuantity)) * 100) || 0).toFixed(2);
      item.subtotal = (item.secondaryPrice * item.secondaryQuantity - item.discountAmount).toFixed(2);
      this.$refs.xTable.updateFooter();
    },
    updateSubtotal(item) {
      item.subtotal = item.subtotal || 0;
      item.secondaryPrice = ((item.subtotal) / ((100 - item.discountRate)) * 100 / item.secondaryQuantity).toFixed(2);
      this.$refs.xTable.updateFooter();
    },
    approved() {
      DialogPlugin.confirm({
        header: "审核提示",
        body: `确认审核该订单?`,
        onConfirm: () => {
          return PurchaseInbound.approved('已审核', [this.form.id]).then(() => {
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
          return PurchaseInbound.approved('已保存', [this.form.id]).then(() => {
            MessagePlugin.success("操作成功~");
            this.closeWindow();
          });
        }
      });
    },
    closeWindow() {
      this.$store.commit('closeTabKey', this.$store.state.currentTab);
      this.$store.commit('newTab', "PurchaseInboundList");
      this.$nextTick(() => {
        this.$store.commit('SET_TAB_DATA', {refresh: true});
      });
    },
  },
  created() {
    LoadingPlugin(true);
    Promise.all([Supplier.select(), Warehouse.select()]).then((results) => {
      this.supplierList = results[0].data || [];
      this.warehouseList = results[1].data || [];
      this.warehouseId = this.warehouseList.find(val => val.systemDefault || val.isDefault)?.id;
      if (this.orderId) {
        PurchaseInbound.load(this.orderId).then(({data: {purchaseInbound, purchaseInboundItemList}}) => {
          if (purchaseInbound) {
            CopyObj(this.form, purchaseInbound);
            this.supplierId = purchaseInbound.supplierId;
            if ('copy' === this.type) {
              this.form.id = null;
              this.form.orderStatus = null;
            }
          }
          this.loadProductsBySupplier();
          this.productData = (purchaseInboundItemList || []).concat([{isNew: true}]);
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
</style>
