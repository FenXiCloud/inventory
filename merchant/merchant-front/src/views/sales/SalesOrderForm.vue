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
              :clearable="false"
              :disabled="isAudited"
          />
          <t-button
              v-if="!isAudited"
              @click="openComboSelect"
              variant="outline"
              style="margin-left: 20px"
          >
            选套餐
          </t-button>
        </div>
        <Stamp v-if="isAudited"/>
        <t-tag v-if="form.purchaseStatusText" :theme="purchaseStatusTheme" variant="light" size="large" class="ml-12px">
          {{ form.purchaseStatusText }}
        </t-tag>
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
          <!-- 新商品：显示商品名称输入框 -->
          <div v-if="row.isNewProduct && !isAudited" class="input-group">
            <t-input
              v-model="row.newProductName"
              placeholder="输入商品名称"
              size="small"
            />
          </div>
          <!-- 已有商品选择 -->
          <div class="input-group goodsSelect" @keyup.stop="void(0)" v-else-if="!isAudited">
            <t-select
                ref="ms"
                @change="selectProduct($event, rowIndex)"
                @create="createProduct($event, rowIndex)"
                :options="productList"
                v-model="row.productId"
                filterable
                creatable
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
          <!-- 新商品：显示商品类别选择 -->
          <t-select
            v-if="row.isNewProduct && !isAudited"
            v-model="row.newProductCategoryId"
            :options="productCategoryList"
            placeholder="选择类别"
            size="small"
            filterable
            creatable
            :keys="{ value: 'id', label: 'name' }"
            @create="createProductCategory($event, row)"
          />
          <span v-else>{{ productCategory(row) }}</span>
        </template>
        <template #secondaryUnitName="{ row }">
          <!-- 新商品：显示单位输入框 -->
          <t-input
            v-if="row.isNewProduct && !isAudited"
            v-model="row.unitName"
            placeholder="输入单位"
            size="small"
          />
          <!-- 多单位商品：销售单位下拉，可切换 -->
          <template v-else-if="!row.isNew && !row.isNewProduct && !isAudited && (row.auxiliaryUnitPrices || []).length">
            <t-select
                :clearable="false"
                v-model="row.secondaryUnitId"
                :options="row.auxiliaryUnitPrices"
                filterable
                @change="onUnitChange(row, $event)"
                :keys="{ value: 'unitId', label: 'unitName' }"
            />
          </template>
          <span v-else>{{ row.secondaryUnitName || row.unitName }}</span>
        </template>
        <template #baseUnitName="{ row }">
          <span v-if="!row.isNew && !row.isNewProduct">{{ row.unitName }}</span>
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
        <template #secondaryQuantity="{ row, rowIndex }">
          <template v-if="!row.isNew && !isAudited">
            <t-tooltip theme="light">
              <template #content>
                <div>当前库存: {{ row.currentStockQuantity || 0 }}</div>
                <div>总库存: {{ row.totalStockQuantity || 0 }}</div>
              </template>
              <t-input-number
                  :id="'r'+rowIndex+''+3"
                  v-model="row.secondaryQuantity"
                  theme="normal"
                  :min="0"
                  :decimal-places="qtyDp"
                  style="width: 100%"
                  @blur="updateQuantity(row)"
                  @focus="showStockQuantity(row)"
              />
            </t-tooltip>
          </template>
          <span v-else-if="!row.isNew">{{ row.secondaryQuantity }}</span>
        </template>
        <template #secondaryPrice="{ row, rowIndex }">
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
                  v-model="row.secondaryPrice"
                  theme="normal"
                  :min="0"
                  :decimal-places="priceDp"
                  style="width: 100%"
                  @blur="updatePrice(row)"
                  @focus="showPrice(row)"
              />
            </t-tooltip>
          </template>
          <span v-else-if="!row.isNew">{{ row.secondaryPrice }}</span>
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
        <t-button theme="primary" v-if="!isAudited" v-auth="'salesOrder:edit'" @click="saveOrder('add')" :loading="loading">保存并新增</t-button>
        <t-button v-if="!isAudited" v-auth="'salesOrder:edit'" @click="saveOrder('save')" :loading="loading">保存</t-button>
        <t-button @click="doPrint" :loading="loading">打印</t-button>
        <t-button theme="success" v-if="form.id && isAudited && form.purchaseStatus !== 2" @click="openTransferDialog()" :loading="loading">转采购</t-button>
        <t-button v-if="$can('salesOrder:audit') && form.id && !isAudited" @click="approved()" :loading="loading">审核</t-button>
        <t-button v-if="$can('salesOrder:audit') && isAudited" @click="backApproved()" :loading="loading">反审核</t-button>
      </div>
    </div>
    <div v-if="previewVisible" class="image-preview-modal" @click="previewVisible = false">
      <div class="image-preview-container">
        <img :src="previewImageUrl" class="preview-image" alt="产品图片预览">
      </div>
    </div>

    <!-- 以销定购对话框 -->
    <t-dialog
      v-model:visible="showTransferDialog"
      header="以销定购 - 转采购入库"
      width="1000px"
      :confirm-loading="transferLoading"
      @confirm="doTransfer"
    >
      <div class="p-16px">
        <div class="mb-16px flex items-center gap-16px">
          <div>
            <label class="mr-8px" style="font-weight: 600;">统一供应商：</label>
            <t-select
              v-model="transferForm.supplierId"
              :options="supplierList"
              filterable
              clearable
              placeholder="可统一设置（可选）"
              :keys="{ value: 'id', label: 'name' }"
              style="width: 260px"
              @change="onTransferSupplierChange"
            />
          </div>
          <div>
            <label class="mr-8px" style="font-weight: 600;">预计到货日：</label>
            <t-date-picker v-model="transferForm.expectedDate" clearable style="width: 180px" />
          </div>
        </div>
        <t-table
          :data="transferItems"
          :columns="transferColumns"
          size="small"
          bordered
          stripe
          table-layout="fixed"
          row-key="_rowKey"
          :selected-row-keys="transferSelectedKeys"
          @select-change="onTransferSelectChange"
        >
          <template #supplierId="{ row }">
            <t-select
              v-model="row.supplierId"
              :options="supplierList"
              filterable
              placeholder="选择供应商"
              :keys="{ value: 'id', label: 'name' }"
              size="small"
              style="width: 140px"
            />
          </template>
          <template #purchaseQuantity="{ row }">
            <t-input-number
              v-model="row.purchaseQuantity"
              theme="normal"
              :min="0"
              :decimal-places="qtyDp"
              style="width: 100%"
            />
          </template>
          <template #purchasePrice="{ row }">
            <t-input-number
              v-model="row.purchasePrice"
              theme="normal"
              :min="0"
              :decimal-places="priceDp"
              style="width: 100%"
            />
          </template>
          <template #purchaseSubtotal="{ row }">
            {{ (row.purchaseQuantity * row.purchasePrice).toFixed(2) }}
          </template>
        </t-table>
      </div>
    </t-dialog>

    <t-dialog v-model:visible="comboVisible" header="选择商品套餐" :footer="false" width="460px">
      <t-form label-width="90px">
        <t-form-item label="套餐">
          <t-select
              v-model="selectedComboId"
              :options="comboList"
              :keys="{ value: 'id', label: 'customName' }"
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
  </div>
</template>
<script>
import {LoadingPlugin, MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';
import {openPrint} from '@common/print';
import {switchUnit} from '@common/unit';
import manba from "manba";
import Customer from "@js/api/basic/Customer";
import Warehouse from "@js/api/basic/Warehouse";
import {mapMutations, mapState} from "vuex";
import SalesOrder from "@js/api/sales/SalesOrder";
import Product from "@js/api/basic/Product";
import ProductCombo from "@js/api/basic/ProductCombo";
import ProductCategory from "@js/api/basic/ProductCategory";
import Supplier from "@js/api/basic/Supplier";
import Inventory from "@js/api/inventory/Inventory";
import Stamp from "@views/common/Stamp.vue";
import PriceRecord from "@js/api/basic/PriceRecord";

let rowSeq = 0;
function newRow(extra = {}) {
  return {
    _rowKey: `r-${++rowSeq}`,
    productId: null,
    isNew: true,
    isNewProduct: false,
    newProductName: '',
    newProductCategoryId: null,
    ...extra
  };
}

export default {
  name: "SalesOrderForm",
  components: {Stamp},
  computed: {
    ...mapState(['accountBook']),
    isAudited() {
      return this.form.orderStatus === '已审核';
    },
    purchaseStatusTheme() {
      const s = this.form.purchaseStatus;
      if (s === 2) return 'success';
      if (s === 1) return 'warning';
      return 'default';
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
        { colKey: 'productInfo', title: '产品信息', minWidth: 300 },
        { colKey: 'secondaryUnitName', title: '销售单位', align: 'center', width: 80 },
        { colKey: 'productCategoryName', title: '产品类别', align: 'center', width: 80 },
        { colKey: 'specification', title: '规格型号', align: 'center', width: 80 },
        { colKey: 'warehouse', title: '仓库', align: 'center', width: 120 },
        { colKey: 'secondaryQuantity', title: '数量', width: 90 },
        { colKey: 'baseUnitName', title: '基本单位', align: 'center', width: 80 },
        { colKey: 'quantity', title: '基本数量', width: 90 },
        { colKey: 'secondaryPrice', title: '销售单价', width: 100 },
        { colKey: 'discountRate', title: '折扣率(%)', width: 100 },
        { colKey: 'discountValue', title: '折扣额', width: 100 },
        { colKey: 'subtotal', title: '销售金额', width: 100 },
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
    },
    transferColumns() {
      return [
        { colKey: 'row-select', type: 'multiple', width: 46 },
        { colKey: 'productCode', title: '商品编码', width: 100 },
        { colKey: 'productName', title: '商品名称', minWidth: 120 },
        { colKey: 'unitName', title: '单位', width: 60, align: 'center' },
        { colKey: 'orderQuantity', title: '订单数量', width: 80, align: 'right' },
        { colKey: 'supplierId', title: '供应商', width: 160 },
        { colKey: 'purchaseQuantity', title: '采购数量', width: 110 },
        { colKey: 'purchasePrice', title: '采购单价', width: 110 },
        { colKey: 'purchaseSubtotal', title: '采购金额', width: 90, align: 'right' },
      ];
    }
  },
  data() {
    return {
      loading: false,
      productList: [],
      productCategoryList: [],
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
      showTransferDialog: false,
      transferLoading: false,
      transferForm: { supplierId: null, expectedDate: null },
      transferItems: [],
      transferSelectedKeys: [],
      supplierList: [],
      recentSales: [],
      customerLevel: null,
      customerLevelId: null,
      customerPrice: null,
      comboVisible: false,
      comboList: [],
      selectedComboId: null,
      comboQuantity: 1,
    };
  },
  methods: {
    ...mapMutations(['newTab']),
    productImage(row) {
      const p = (this.productList || []).find(item => (item.productId || item.id) === row.productId);
      return p?.imgPath || '';
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
          quantity: r.secondaryQuantity ?? r.quantity,
          price: r.secondaryPrice ?? r.unitPrice ?? r.price,
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
    createProduct(value, index) {
      const name = (typeof value === 'string' ? value : (value?.label || value?.productName || '')).trim();
      if (!name) return;
      // 标记为新商品，不立即创建
      this.productData[index] = {
        ...this.productData[index],
        isNewProduct: true,
        newProductName: name,
        productId: null,
        isNew: false,
        quantity: 1,
        secondaryQuantity: 1,
        conversionRate: 1,
        unitPrice: 0,
        secondaryPrice: 0,
        secondaryUnitName: '个',
        discountRate: 0,
        discountValue: 0,
        subtotal: 0,
        unitName: '个', // 默认单位
      };
    },
    createProductCategory(value, row) {
      const name = (typeof value === 'string' ? value : (value?.label || value?.name || '')).trim();
      if (!name) return;
      // 先清空，避免把输入的名称字符串留在 newProductCategoryId 上
      row.newProductCategoryId = null;
      ProductCategory.save({ name }).then(({ data }) => {
        if (data && data.id) {
          this.productCategoryList.push(data);
          row.newProductCategoryId = data.id;
        }
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
        secondaryQuantity: 1,
        unitPrice: unitPrice,
        secondaryPrice: unitPrice,
        conversionRate: 1,
        secondaryUnitId: d.unitId,
        secondaryUnitName: d.unitName,
        auxiliaryUnitPrices: d.auxiliaryUnitPrices || null,
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
    openComboSelect() {
      if (!this.form.customerId && !this.customerId) {
        MessagePlugin.warning('请先选择客户');
        return;
      }
      this.loadCombos();
      this.comboVisible = true;
    },
    loadCombos() {
      ProductCombo.list({ page: 1, pageSize: 1000 }).then(({ data }) => {
        this.comboList = (data?.results || [])
          .filter((c) => c.enabled !== false)
          .map((c) => ({ ...c, customName: `${c.code || ''}--${c.name || ''}` }));
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
      const defaultWarehouseId = this.resolveDefaultWarehouseId();
      const newRows = [];
      comboItems.forEach((ci) => {
        const p = (this.productList || []).find((i) => String(i.id) === String(ci.productId));
        if (!p) return;
        const comboQty = (Number(ci.quantity) || 1) * qty;
        const comboPrice = p.lastSalePrice || 0;
        const row = newRow({
          isNew: false,
          quantity: comboQty,
          secondaryQuantity: comboQty,
          unitPrice: comboPrice,
          secondaryPrice: comboPrice,
          conversionRate: 1,
          secondaryUnitId: p.unitId,
          secondaryUnitName: p.unitName,
          auxiliaryUnitPrices: p.auxiliaryUnitPrices || null,
          warehouseId: defaultWarehouseId,
          discountValue: 0.00,
          discountRate: 0.00,
          baseUnitId: p.unitId,
          unitName: p.unitName,
          taxRate: p.taxRate != null ? p.taxRate : null,
          productId: p.id,
          productCode: p.code,
          productName: p.name,
          remark: "",
        });
        row.subtotal = (comboQty * comboPrice).toFixed(2);
        newRows.push(row);
      });
      if (!newRows.length) {
        MessagePlugin.warning('套餐组件在商品库中不存在');
        return;
      }
      const firstEmpty = this.productData.findIndex((i) => i.isNew && !i.productId);
      if (firstEmpty >= 0) {
        this.productData.splice(firstEmpty, 1, ...newRows);
      } else {
        this.productData.push(...newRows);
      }
      if (!this.productData.some((i) => i.isNew && !i.productId)) {
        this.productData.push(newRow({ isNew: true }));
      }
      newRows.forEach((r) => {
        this.showStockQuantity(r);
        this.showPrice(r);
      });
      MessagePlugin.success(`已展开套餐 ${newRows.length} 个组件`);
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
        LoadingPlugin(false);
        return;
      }

      // 收集新商品
      const newProducts = this.productData
        .filter(c => c.isNewProduct && c.newProductName && c.quantity > 0)
        .map(c => ({
          name: c.newProductName,
          productCategoryId: c.newProductCategoryId,
          unitName: c.unitName
        }));

      // 创建新商品的函数
      const createNewProducts = async () => {
        const createdProducts = [];
        for (const newProduct of newProducts) {
          const { data } = await Product.quickCreate(newProduct);
          if (data && data.productId) {
            createdProducts.push({...data, inputName: newProduct.name});
            this.productList.push(data);
          }
        }
        return createdProducts;
      };

      // 保存订单的函数
      const saveOrderData = (createdProducts) => {
        // 更新新商品行的完整信息
        this.productData.forEach(row => {
          if (row.isNewProduct && row.newProductName) {
            const createdProduct = createdProducts.find(p => p.inputName === row.newProductName);
            if (createdProduct) {
              row.productId = createdProduct.productId;
              row.productCode = createdProduct.productCode || '';
              row.productName = createdProduct.productName || row.newProductName;
              row.baseUnitId = createdProduct.unitId;
              row.unitName = createdProduct.unitName || row.unitName || '个';
            }
          }
        });

        let productData = this.productData
          .filter(c => !c.isNew && c.quantity > 0 && c.productId)
          .map(({ _rowKey, isNewProduct, newProductName, newProductCategoryId, ...rest }) => rest);

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
      };

      // 如果有新商品，先创建新商品，再保存订单
      if (newProducts.length > 0) {
        createNewProducts().then((createdProducts) => {
          saveOrderData(createdProducts);
        }).catch((error) => {
          MessagePlugin.error("创建商品失败~");
          LoadingPlugin(false);
        });
      } else {
        saveOrderData([]);
      }
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
    onUnitChange(row, value) {
      const item = (row.auxiliaryUnitPrices || []).find((u) => String(u.unitId) === String(value));
      if (item) {
        // 按基本单价等比换算：基本数量/基本单价/业务价/小计就地更新
        switchUnit(row, item);
        // switchUnit 写的是采购口径 discountAmount，销售明细用 discountValue
        row.discountValue = Number(row.discountAmount) || 0;
        delete row.discountAmount;
        // 按业务量×业务价重算折扣金额与小计（保持一致）
        const sq = Number(row.secondaryQuantity) || 0;
        const sp = Number(row.secondaryPrice) || 0;
        const dr = Number(row.discountRate) || 0;
        row.discountValue = Number((sq * sp * dr / 100).toFixed(2));
        row.subtotal = Number((sq * sp * (100 - dr) / 100).toFixed(2));
      }
    },
    updateQuantity(item) {
      // 新商品也需要计算
      if (!item.productId && !item.isNewProduct) return;
      item.secondaryQuantity = item.secondaryQuantity || 1;
      this.recalcRow(item);
    },
    updatePrice(item) {
      // 新商品也需要计算
      if (!item.productId && !item.isNewProduct) return;
      item.secondaryPrice = item.secondaryPrice || 0.00;
      this.recalcRow(item);
    },
    recalcRow(item) {
      const sq = Number(item.secondaryQuantity) || 0;
      const sp = Number(item.secondaryPrice) || 0;
      const rate = Number(item.conversionRate) || 1;
      const dr = Number(item.discountRate) || 0;
      item.quantity = Number((sq * rate).toFixed(2));
      if (rate) item.unitPrice = Number((sp / rate).toFixed(2));
      item.discountValue = Number((sq * sp * dr / 100).toFixed(2));
      item.subtotal = Number((sq * sp * (100 - dr) / 100).toFixed(2));
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
      const sq = Number(item.secondaryQuantity) || 0;
      const sp = Number(item.secondaryPrice) || 0;
      item.subtotal = Number((sq * sp * (100 - item.discountRate) / 100).toFixed(2));
      item.discountValue = Number((sq * sp - item.subtotal).toFixed(2));
      this.recalcRow(item);
    },
    updateDiscountAmount(item) {
      item.discountValue = item.discountValue || 0.00;
      const sq = Number(item.secondaryQuantity) || 0;
      const sp = Number(item.secondaryPrice) || 0;
      item.discountRate = (((item.discountValue / (sp * sq)) * 100) || 0);
      this.recalcRow(item);
    },
    updateFinalAmount(item) {
      // 小计（折后金额）反推业务单价：折前业务金额 = 小计 ÷ (100-折扣率)%
      item.subtotal = Number(item.subtotal) || 0;
      const dr = Number(item.discountRate) || 0;
      const sq = Number(item.secondaryQuantity) || 1;
      const pre = dr < 100 ? item.subtotal * 100 / (100 - dr) : item.subtotal;
      item.secondaryPrice = Number((pre / sq).toFixed(2));
      this.recalcRow(item);
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
    onTransferSelectChange(selectedRowKeys, { selectedRowData }) {
      this.transferSelectedKeys = selectedRowKeys;
    },
    openTransferDialog() {
      // 准备转采购数据：带入销售订单的商品明细
      this.transferForm.supplierId = null;
      this.transferForm.expectedDate = null;
      this.transferItems = (this.productData || [])
        .filter(r => !r.isNew && r.productId && r.quantity > 0)
        .map(r => {
          // 查找商品的默认供应商和最近采购价
          const product = (this.productList || []).find(p => (p.productId || p.id) === r.productId) || {};
          return {
            _rowKey: r._rowKey,
            productId: r.productId,
            productCode: r.productCode || product.productCode || '',
            productName: r.productName || product.productName || '',
            unitName: r.unitName || product.unitName || '',
            baseUnitId: r.baseUnitId || product.unitId,
            warehouseId: r.warehouseId,
            orderQuantity: r.quantity,
            purchaseQuantity: r.quantity,
            purchasePrice: product.lastPurchasePrice || r.unitPrice || 0,
            supplierId: product.defaultSupplierId || null,
            secondaryUnitId: r.baseUnitId || product.unitId,
            conversionRate: 1,
          };
        });
      if (!this.transferItems.length) {
        MessagePlugin.warning('订单中没有商品明细');
        return;
      }
      // 默认选中所有行
      this.transferSelectedKeys = this.transferItems.map(r => r._rowKey);
      this.showTransferDialog = true;
    },
    onTransferSupplierChange(val) {
      // 统一设置所有行的供应商
      if (val) {
        this.transferItems.forEach(r => { r.supplierId = val; });
      }
    },
    doTransfer() {
      // 只处理选中的行
      const selectedItems = this.transferItems.filter(r => this.transferSelectedKeys.includes(r._rowKey));
      if (!selectedItems.length) {
        MessagePlugin.warning('请至少选择一个商品');
        return;
      }
      // 校验：每行必须有供应商
      const noSupplier = selectedItems.find(r => r.purchaseQuantity > 0 && !r.supplierId);
      if (noSupplier) {
        MessagePlugin.warning(`商品「${noSupplier.productName}」未选择供应商`);
        return;
      }
      const validItems = selectedItems.filter(r => r.purchaseQuantity > 0);
      if (!validItems.length) {
        MessagePlugin.warning('请填写采购数量');
        return;
      }

      // 按供应商分组，不同供应商需要分别生成采购入库单
      const bySupplier = new Map();
      for (const r of validItems) {
        const key = r.supplierId;
        if (!bySupplier.has(key)) bySupplier.set(key, []);
        bySupplier.get(key).push(r);
      }

      this.transferLoading = true;
      const promises = [];
      for (const [supplierId, items] of bySupplier) {
        const payload = {
          supplierId,
          items: items.map(r => ({
            productId: r.productId,
            warehouseId: r.warehouseId,
            secondaryQuantity: r.purchaseQuantity,
            secondaryPrice: r.purchasePrice,
            secondaryUnitId: r.secondaryUnitId || r.baseUnitId,
            baseUnitId: r.baseUnitId,
            conversionRate: r.conversionRate || 1,
          })),
        };
        promises.push(SalesOrder.transferToPurchaseInbound(this.form.id, payload));
      }

      Promise.all(promises).then(results => {
        const ids = results.map(r => r.data).filter(Boolean);
        MessagePlugin.success(`已生成 ${ids.length} 张采购入库单`);
        this.showTransferDialog = false;
        // 跳转到最后一个采购入库单详情
        const lastId = ids[ids.length - 1];
        if (lastId) {
          this.$store.commit('pushTab', {
            key: 'PurchaseInboundForm',
            title: '采购入库单详情',
            params: { orderId: lastId, type: 'edit' }
          });
        }
      }).finally(() => this.transferLoading = false);
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
  watch: {
    'productData': {
      handler(newVal) {
        // 监听业务数量/业务单价变化：自动同步基本数量/基本单价并计算小计、折扣额
        newVal.forEach(row => {
          if (!row.isNew || row.isNewProduct) {
            const sq = Number(row.secondaryQuantity) || 0;
            const sp = Number(row.secondaryPrice) || 0;
            const rate = Number(row.conversionRate) || 1;
            const dr = Number(row.discountRate) || 0;
            row.quantity = Number((sq * rate).toFixed(2));
            if (rate) row.unitPrice = Number((sp / rate).toFixed(2));
            row.subtotal = Number((sq * sp * (100 - dr) / 100).toFixed(2));
            row.discountValue = Number((sq * sp * dr / 100).toFixed(2));
          }
        });
      },
      deep: true
    }
  },
  created() {
    LoadingPlugin(true);
    Promise.all([
      Customer.select(),
      Warehouse.select(),
      Product.select(),
      Supplier.select(),
      ProductCategory.select()
    ]).then((results) => {
      this.customerList = results[0].data || [];
      this.warehouseList = results[1].data || [];
      this.productList = results[2].data || [];
      this.supplierList = results[3].data || [];
      this.productCategoryList = results[4].data || [];
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
            this.form.purchaseStatus = 0;
            this.form.purchaseStatusText = null;
            this.form.purchaseInIds = null;
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
