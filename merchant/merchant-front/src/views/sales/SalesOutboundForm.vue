<template>
  <div class="sales-outbound-wrapper">
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
            <label class="mr-20px ml-16px" style="font-size: 16px !important;">出库日期：</label>
            <t-date-picker
                v-model="form.outboundDate"
                :clearable="false"
                :disabled="isAudited"
            />
            <t-button
                v-if="type==='add' && !isAudited"
                @click="selectSalesOrder()"
                theme="primary"
                style="margin-left: 20px"
            >
              选择源单
            </t-button>
            <t-button
                v-if="!isAudited"
                @click="openComboSelect"
                variant="outline"
                style="margin-left: 12px"
            >
              选套餐
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
                  @change="handleWarehouseChange(row, $event)"
                  :keys="{ value: 'id', label: 'name' }"
              />
            </template>
            <span v-else-if="!row.isNew">{{ warehouseName(row.warehouseId) }}</span>
          </template>
          <template #batchNumber="{ row }">
            <template v-if="!row.isNew && !isAudited">
              <t-link v-if="isBatchProduct(row)" theme="primary" hover="color" @click="openBatchSelect(row)">
                {{ row.batchNumber || '选批次' }}
              </t-link>
              <span v-else>-</span>
            </template>
            <span v-else-if="!row.isNew">{{ row.batchNumber || '-' }}</span>
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
          <template #taxRate="{ row }">
            {{ taxRateText(row.taxRate) }}
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
          <template #salesOrderNo="{ row, rowIndex }">
            <t-input
                v-if="!row.isNew"
                :id="'r'+rowIndex+''+9"
                v-model="row.salesOrderNo"
                placeholder="关联销售单号"
                readonly
                disabled
            />
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
            <label class="ml-16px mr-16px w-80px">税额：</label>
            <t-input-number
                v-model="form.taxAmount"
                theme="normal"
                :min="0"
                :decimal-places="2"
                disabled
            />
            <label class="ml-16px mr-16px w-100px">价税合计：</label>
            <t-input-number
                v-model="form.totalWithTax"
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
          <t-button v-if="form.id && isAudited" theme="warning" @click="generatePickOrder()" :loading="loading">生成拣货单</t-button>
          <t-button v-if="form.id && !isAudited" @click="approved()" :loading="loading">审核</t-button>
          <t-button v-if="isAudited" @click="backApproved()" :loading="loading">反审核</t-button>
        </div>
      </div>
    </div>
    <div v-if="previewVisible" class="image-preview-modal" @click="previewVisible = false">
      <div class="image-preview-container">
        <img :src="previewImageUrl" class="preview-image" alt="产品图片预览">
      </div>
    </div>

    <t-dialog v-model:visible="batchVisible" header="选择出库批次" width="560px" :footer="false">
      <div class="batch-hint">选择本行商品的出库批次（按有效期先后排序，先到先出）：</div>
      <t-table
          row-key="batchNumber"
          size="small"
          bordered
          hover
          height="320px"
          :data="batchList"
          :columns="batchColumns"
          :loading="batchLoading"
          empty="该商品仓库暂无可用批次"
      >
        <template #ops="{ row }">
          <t-link theme="primary" @click="pickBatch(row)">选用</t-link>
        </template>
      </t-table>
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
          <t-input-number v-model="comboQuantity" :min="1" :decimal-places="0" style="width: 100%"/>
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
import manba from "manba";
import Customer from "@js/api/basic/Customer";
import Warehouse from "@js/api/basic/Warehouse";
import {mapState} from "vuex";
import Product from "@js/api/basic/Product";
import ProductCombo from "@js/api/basic/ProductCombo";
import {openDrawer, closeDialog} from '@common/dialog';
import {h} from "vue";
import SalesOrderSelect from "@views/sales/SalesOrderSelect.vue";
import Unit from "@js/api/basic/Unit";
import SalesOrder from "@js/api/sales/SalesOrder";
import SalesOutbound from "@js/api/sales/SalesOutbound";
import Inventory from "@js/api/inventory/Inventory";
import InventoryReport from "@js/api/inventory/InventoryReport";
import PickOrder from "@js/api/inventory/PickOrder";
import Stamp from "@views/common/Stamp.vue";
import PriceRecord from "@js/api/basic/PriceRecord";

let rowSeq = 0;
function newRow(extra = {}) {
  return { _rowKey: `r-${++rowSeq}`, productId: null, isNew: true, ...extra };
}

export default {
  name: "SalesOutboundForm",
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
        { colKey: 'batchNumber', title: '批次号', align: 'center', width: 100 },
        { colKey: 'quantity', title: '数量', width: 90 },
        { colKey: 'unitName', title: '单位', align: 'center', width: 80 },
        { colKey: 'unitPrice', title: '单价', width: 100 },
        { colKey: 'discountRate', title: '折扣率(%)', width: 100 },
        { colKey: 'discountValue', title: '折扣额', width: 100 },
        { colKey: 'subtotal', title: '金额', width: 100 },
        { colKey: 'taxRate', title: '税率', align: 'center', width: 80 },
        { colKey: 'remark', title: '备注', width: 100 },
        { colKey: 'salesOrderNo', title: '关联销售单号', width: 200 },
      ];
    },
    footData() {
      let quantity = 0;
      let discountValue = 0;
      let subtotal = 0;
      let taxAmount = 0;
      (this.productData || []).forEach((row) => {
        quantity += Number(row.quantity || 0);
        discountValue += Number(row.discountValue || 0);
        subtotal += Number(row.subtotal || 0);
        taxAmount += Number(row.subtotal || 0) * Number(row.taxRate || 0);
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
      this.form.taxAmount = taxAmount.toFixed(2);
      this.form.totalWithTax = (Number(this.form.finalAmount || 0) + taxAmount).toFixed(2);
      return [{
        ops: '合计',
        quantity: quantity.toFixed(2),
        discountValue: discountValue,
        subtotal: subtotal,
        taxAmount: taxAmount.toFixed(2)
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
        outboundDate: manba().format("YYYY-MM-dd"),
        customerId: null,
        discountAmount: 0.00,
        discountRate: 0.00,
        finalAmount: 0.00,
        taxAmount: 0.00,
        totalWithTax: 0.00,
        remarks: null,
        orderStatus: null,
      },
      productData: [newRow({ isNew: true })],
      selectSalesOrderIdList: [],
      previewVisible: false,
      previewImageUrl: '',
      batchVisible: false,
      batchLoading: false,
      batchList: [],
      currentBatchRow: null,
      batchColumns: [
        { colKey: 'batchNumber', title: '批次号', width: 160, ellipsis: true },
        { colKey: 'productionDate', title: '生产日期', width: 100, align: 'center' },
        { colKey: 'expiryDate', title: '有效期至', width: 100, align: 'center' },
        { colKey: 'availableQuantity', title: '可用数量', width: 100, align: 'right' },
        { colKey: 'ops', title: '操作', width: 70, align: 'center' },
      ],
      recentSales: [],
      customerLevel: null,
      customerLevelId: null,
      customerPrice: null,
      comboVisible: false,
      comboList: [],
      selectedComboId: null,
      comboQuantity: 1,
    }
  },
  methods: {
    productImage(row) {
      const p = (this.productList || []).find(item => item.id === row.productId);
      return p?.imgPath || '';
    },
    taxRateText(rate) {
      const r = Number(rate || 0);
      return r ? (r * 100).toFixed(0) + '%' : '-';
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
        const p = (this.productList || []).find(x => x.id === r.productId) || {};
        return {
          ...r,
          productName: r.productName || p.name || '',
          quantity: r.quantity ?? r.secondaryQuantity,
          price: r.unitPrice ?? r.secondaryPrice ?? r.price,
          amount: r.subtotal ?? r.amount,
        };
      });
      openPrint('销售出库单', {
        header: {
          ...this.form,
          partner: (this.customerList.find(s => s.id === this.customerId) || {}).name || '',
          amount: this.form.finalAmount ?? this.form.totalAmount,
        },
        items,
      });
    },
    selectSalesOrder() {
      if (!this.form.customerId) {
        MessagePlugin.error("请选择客户~");
        return;
      }
      let dialogId = openDrawer({
        header: "请选择销售订单",
        closeOnOverlayClick: false,
        closeBtn: false,
        size: '1200px',
        body: h(SalesOrderSelect, {
          customerId: this.customerId,
          onClose: () => closeDialog(dialogId),
          onSuccess: (params) => {
            this.loadToOutbound(params);
            closeDialog(dialogId);
          },
        }),
      });
    },
    loadToOutbound(params) {
      this.selectSalesOrderIdList = params.orderIds || [];
      SalesOrder.toOutbound(this.form.customerId, params.orderIds).then(({data}) => {
        if (!data || !data.length) {
          MessagePlugin.warning("所选订单没有可出库商品~");
          return;
        }
        const rows = this.applyDefaultWarehouse(data || []).map((row) => newRow({ ...row, isNew: false }));
        this.productData = rows.concat([newRow({ isNew: true })]);
      });
    },
    selectProduct(value, index) {
      const d = (this.productList || []).find((item) => String(item.id) === String(value));
      if (!d) return;
      const defaultWarehouseId = this.resolveDefaultWarehouseId();
      let unitPrice = d.lastSalePrice || 0;
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
        taxRate: d.taxRate,
        remark: "",
      });
      this.productData[index] = g;
      if (!this.productData[index].warehouseId) {
        this.productData[index].warehouseId = defaultWarehouseId;
      }
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
        const row = newRow({
          isNew: false,
          quantity: (Number(ci.quantity) || 1) * qty,
          unitPrice: p.lastSalePrice || 0,
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
        row.subtotal = (row.quantity * row.unitPrice).toFixed(2);
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
    handleWarehouseChange(row) {
      this.showStockQuantity(row);
    },
    isBatchProduct(row) {
      const p = (this.productList || []).find(x => String(x.id) === String(row.productId));
      return !!p && !!p.enableBatch;
    },
    openBatchSelect(row) {
      if (!row.productId || !row.warehouseId) {
        MessagePlugin.warning('请先选择产品和仓库~');
        return;
      }
      this.currentBatchRow = row;
      this.batchVisible = true;
      this.loadBatches(row);
    },
    loadBatches(row) {
      this.batchLoading = true;
      InventoryReport.batchAvailable({ productId: row.productId, warehouseId: row.warehouseId })
        .then(({ data }) => {
          this.batchList = (data || []).map((b) => ({...b}));
        })
        .finally(() => (this.batchLoading = false));
    },
    pickBatch(batch) {
      if (!this.currentBatchRow) return;
      this.currentBatchRow.batchNumber = batch.batchNumber;
      this.currentBatchRow.productionDate = batch.productionDate;
      this.currentBatchRow.expiryDate = batch.expiryDate;
      this.batchVisible = false;
      MessagePlugin.success('已选择批次：' + batch.batchNumber);
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
      SalesOutbound.save({
        salesOutbound: Object.assign(this.form),
        salesOutboundItemList: productData,
        selectSalesOrderIdList: this.selectSalesOrderIdList
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
        outboundDate: manba().format("YYYY-MM-dd"),
        customerId: null,
        discountAmount: 0.00,
        discountRate: 0.00,
        finalAmount: 0.00,
        taxAmount: 0.00,
        totalWithTax: 0.00,
        remarks: null,
        orderStatus: null,
      };
      this.productData = [newRow({ isNew: true })];
      this.customerId = null;
      this.selectSalesOrderIdList = [];
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
    approved() {
      DialogPlugin.confirm({
        header: "审核提示",
        body: `确认审核该订单?`,
        onConfirm: () => {
          return SalesOutbound.approved('已审核', [this.form.id]).then(() => {
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
          return SalesOutbound.approved('已保存', [this.form.id]).then(() => {
            MessagePlugin.success("操作成功~");
            this.closeWindow();
          });
        }
      });
    },
    generatePickOrder() {
      if (!this.form.id) {
        MessagePlugin.warning("请先保存出库单");
        return;
      }
      DialogPlugin.confirm({
        header: "生成拣货单",
        body: `确认根据当前出库单生成拣货单？系统将按库区（整货/零货）自动拆分。`,
        onConfirm: () => {
          this.loading = true;
          PickOrder.generate(this.form.id).then(({data}) => {
            MessagePlugin.success(`成功生成 ${data.length} 张拣货单`);
            // 跳转到拣货单列表
            this.$store.commit('newTab', "PickOrderList");
          }).finally(() => {
            this.loading = false;
          });
        }
      });
    },
    closeWindow() {
      this.$store.commit('closeTabKey', this.$store.state.currentTab);
      this.$store.commit('newTab', "SalesOutboundList");
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
    }
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
      this.warehouseId = this.warehouseList.find(val => val.systemDefault || val.isDefault)?.id;
      this.productList.forEach(item => {
        item.customName = `${item.code}--${item.name}`;
      });
      if (this.orderId) {
        SalesOutbound.load(this.orderId).then(response => {
          let salesOutbound = response.data;
          this.form = salesOutbound;
          this.customerId = salesOutbound.customerId;
          const rows = this.applyDefaultWarehouse(salesOutbound.salesOutboundItemList || [])
            .map((row) => newRow({ ...row, isNew: false }));
          this.productData = rows.concat([newRow({ isNew: true })]);
          this.reloadProductList();
        });
      }
      // 检查是否从销售订单转入
      const tabData = this.$store.state.currentTabData;
      if (tabData && tabData.fromSalesOrder && tabData.orderIds && tabData.orderIds.length > 0) {
        this.customerId = tabData.customerId;
        this.form.customerId = tabData.customerId;
        this.loadToOutbound({ orderIds: tabData.orderIds });
        this.$store.commit('SET_TAB_DATA', null);
      }
    }).finally(() => LoadingPlugin(false));
  },
}
</script>
<style scoped>
.sales-outbound-wrapper {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.batch-hint {
  margin-bottom: 8px;
  padding: 8px 12px;
  border-radius: 4px;
  background: #f3f3f3;
  color: #555;
  font-size: 13px;
  line-height: 1.6;
}

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
