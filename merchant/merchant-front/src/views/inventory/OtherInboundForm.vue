<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <div class="form-toolbar">
        <div class="form-toolbar__left">
          <label class="mr-20px ml-16px" style="font-size: 16px !important">单据日期：</label>
          <t-date-picker v-model="form.orderDate" :disabled="isLocked"
                         :disable-date="{ before: accountBook.checkoutDate }"
                         :clearable="false"/>
          <label class="mr-20px ml-20px" style="font-size: 16px !important">供应商：</label>
          <t-select class="w-178px" filterable :options="supplierList"
                    :keys="{ value: 'id', label: 'name' }"
                    v-model="form.supplierId" placeholder="请选择供应商" :disabled="isLocked" clearable/>
          <label class="mr-20px ml-20px" style="font-size: 16px !important">客户：</label>
          <t-select class="w-178px" filterable :options="customerList"
                    :keys="{ value: 'id', label: 'name' }"
                    v-model="form.customerId" placeholder="请选择客户" :disabled="isLocked" clearable/>
          <label class="mr-20px ml-16px" style="font-size: 16px !important">业务类型：</label>
          <t-select class="w-178px" filterable :options="inboundTypeList"
                    :keys="{ value: 'id', label: 'name' }"
                    :clearable="false" v-model="form.inboundType" :disabled="isLocked"
                    placeholder="请选择业务类型"/>
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
          :data="otherInboundData"
          :foot-data="footData"
          :loading="loading"
      >
        <template #ops="{ rowIndex }">
          <template v-if="!isLocked">
            <div class="fa fa-plus text-hover mr-5px" @click="adjustRows('insert', rowIndex)"></div>
            <div v-if="otherInboundData.length !== 1" class="fa fa-minus text-hover-danger"
                 @click="adjustRows('delete', rowIndex)"></div>
          </template>
          <template v-else>{{ rowIndex + 1 }}</template>
        </template>
        <template #productUrl="{ row }">
          <img v-if="row.productUrl" :src="row.productUrl" alt="" class="product-img"/>
        </template>
        <template #productName="{ row, rowIndex }">
          <div class="input-group goodsSelect" v-if="!isLocked">
            <t-select :clearable="false" ref="ms" v-model="row.productId" :options="productList" filterable
                      placeholder="输入编码/名称" :keys="{ value: 'id', label: 'customName' }"
                      @change="(e) => changeRow({ rowIndex }, 'product', e)"/>
          </div>
          <div v-else class="flex">
            <div class="flex1 ml-8px">
              <div>{{ row.productCode }}--{{ row.productName }}</div>
            </div>
          </div>
        </template>
        <template #warehouseName="{ row, rowIndex }">
          <div class="input-group goodsSelect" v-if="!isLocked">
            <t-select :clearable="false" ref="ms" v-model="row.warehouseId" :options="warehouseList" filterable
                      placeholder="请选择仓库" :keys="{ value: 'id', label: 'name' }"
                      @change="(e) => changeRow({ rowIndex }, 'warehouse', e)"/>
          </div>
          <div v-else class="flex">
            <div class="flex1 ml-8px">
              <div>{{ row.warehouseName }}</div>
            </div>
          </div>
        </template>
        <template #quantity="{ row, rowIndex }">
          <t-tooltip v-if="!isLocked" theme="light">
            <template #content>
              <div style="white-space: pre-line">{{ row.quantityTips }}</div>
            </template>
            <t-input-number
                v-model="row.quantity"
                theme="normal"
                :min="0"
                :decimal-places="0"
                style="width: 100%"
                @focus="quantityFocus({ rowIndex })"
                @blur="quantityBlur('quantity', { rowIndex })"
            />
          </t-tooltip>
          <div v-else class="flex">
            <div class="flex1 ml-8px">
              <div>{{ row.quantity }}</div>
            </div>
          </div>
        </template>
        <template #unitPrice="{ row, rowIndex }">
          <t-input-number
              v-if="!isLocked"
              v-model="row.unitPrice"
              theme="normal"
              :min="0"
              :decimal-places="2"
              style="width: 100%"
              @focus="quantityFocus({ rowIndex })"
              @blur="quantityBlur('unitPrice', { rowIndex })"
          />
          <div v-else class="flex">
            <div class="flex1 ml-8px">
              <div>{{ row.unitPrice }}</div>
            </div>
          </div>
        </template>
        <template #subtotal="{ row, rowIndex }">
          <t-input-number
              v-if="!isLocked"
              v-model="row.subtotal"
              theme="normal"
              :min="0"
              :decimal-places="2"
              style="width: 100%"
              @focus="quantityFocus({ rowIndex })"
              @blur="quantityBlur('subtotal', { rowIndex })"
          />
          <div v-else class="flex">
            <div class="flex1 ml-8px">
              <div>{{ row.subtotal }}</div>
            </div>
          </div>
        </template>
      </t-table>
      <div class="mt-10px"></div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px w-80px">备注说明：</label>
          <t-input :disabled="isLocked" placeholder="请输入备注" maxlength="150"
                   style="width: 80%"
                   v-model="form.remarks"/>
          <label class="ml-16px w-180px">制单人：{{ form.adminName }}</label>
        </div>
      </div>
    </div>
    <div class="page-column-footer modal-column-between bg-white-color border">
      <t-button @click="closeWindow" :loading="loading"> 取消</t-button>
      <div>
        <t-button theme="primary" v-if="!isLocked" @click="saveOrder('add')"
                  :loading="loading">
          保存并新增
        </t-button>
        <t-button @click="saveOrder('save')" v-if="!isLocked"
                  :loading="loading"> 保存
        </t-button>
        <t-button @click="doPrint" :loading="loading"> 打印 </t-button>
        <t-button v-if="form.id && !isLocked" @click="approved()" :loading="loading"> 审核</t-button>
        <t-button v-if="isAudited && !looked" @click="backApproved()" :loading="loading"> 反审核</t-button>
      </div>
    </div>
  </div>
</template>
<script>
import {LoadingPlugin, MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';
import {openPrint} from '@common/print';
import manba from "manba";
import Product from "@js/api/basic/Product";
import Warehouse from "@js/api/basic/Warehouse";
import Customer from "@js/api/basic/Customer";
import Supplier from "@js/api/basic/Supplier";
import OtherInbound from "@js/api/inventory/OtherInbound";
import Inventory from "@js/api/inventory/Inventory";
import {mapMutations, mapState} from "vuex";
import Stamp from "../common/Stamp.vue";

let rowSeq = 0;
function newRow(extra = {}) {
  return { _rowKey: `r-${++rowSeq}`, productId: null, warehouseId: null, quantity: null, ...extra };
}

export default {
  name: "OtherInboundForm",
  components: {Stamp},
  props: {
    otherInboundId: [String, Number],
    stockTakeId: [String, Number],
    importInbound: Array,
    type: String,
    index: Number
  },
  computed: {
    ...mapState(["user", "accountBook"]),
    isAudited() {
      return this.form.orderStatus === '已审核';
    },
    looked() {
      return this.type === 'look';
    },
    isLocked() {
      return this.isAudited || this.looked;
    },
    columns() {
      return [
        {
          colKey: 'ops',
          title: '操作',
          width: 70,
          align: 'center',
          fixed: 'left',
          foot: () => '合计'
        },
        { colKey: 'productUrl', title: '产品图片', width: 100 },
        { colKey: 'productCode', title: '产品编码', width: 100 },
        { colKey: 'productName', title: '产品名称', minWidth: 300 },
        { colKey: 'productSpecification', title: '规格型号', align: 'center', width: 80 },
        { colKey: 'productCategoryName', title: '产品类别', align: 'center', width: 120 },
        { colKey: 'productUnitName', title: '单位', width: 90 },
        { colKey: 'warehouseName', title: '仓库', width: 300 },
        { colKey: 'quantity', title: '数量', width: 100 },
        { colKey: 'unitPrice', title: '入库单价', width: 125 },
        { colKey: 'subtotal', title: '入库金额', width: 125 },
      ];
    },
    footData() {
      let totalQuantity = 0;
      let totalAmount = 0;
      (this.otherInboundData || []).forEach((row) => {
        totalQuantity += Number(row.quantity || 0);
        totalAmount += Number(row.subtotal || 0);
      });
      return [{ ops: '合计', quantity: totalQuantity, subtotal: totalAmount.toFixed(2) }];
    }
  },
  data() {
    return {
      refresh: false,
      loading: false,
      productList: [],
      product: null,
      warehouseList: [],
      warehouseId: null,
      form: {
        id: null,
        stockTakeId: null,
        orderDate: manba().format("YYYY-MM-dd"),
        remarks: null,
        customerId: null,
        supplierId: null,
        inboundType: '其他入库',
        totalAmount: 0.00,
        totalQuantity: 0,
        adminName: '',
        orderStatus: '已保存'
      },
      productData: [],
      otherInboundData: [],
      selectRowIndex: null,
      increase: true,
      inboundTypeList: [{id: '其他入库', name: '其他入库'}, {id: '盘盈入库', name: '盘盈入库'}],
      customerList: [],
      supplierList: [],
    };
  },
  methods: {
    doPrint() {
      const items = (this.otherInboundData || []).filter(r => r && !r.isNew && r.productId).map(r => {
        const p = (this.productList || []).find(x => (x.productId || x.id) === r.productId) || {};
        return {
          ...r,
          productName: r.productName || p.productName || p.name || p.customName || '',
          quantity: r.quantity ?? r.secondaryQuantity,
          price: r.unitPrice ?? r.secondaryPrice ?? r.price,
          amount: r.subtotal ?? r.amount,
        };
      });
      openPrint('其他入库单', {
        header: {
          ...this.form,
          partner: (this.supplierList.find(s => s.id === this.form.supplierId) || this.customerList.find(s => s.id === this.form.customerId) || {}).name || '',
          amount: this.form.finalAmount ?? this.form.totalAmount,
        },
        items,
      });
    },

    ...mapMutations(['closeSelfTab', 'pushTab']),
    changeRow: function ({rowIndex}, type, selected) {
      switch (type) {
        case 'product': {
          let value = (selected && typeof selected === 'object')
              ? (selected.id ?? selected.productId)
              : (selected != null && typeof selected !== 'object'
                  ? selected
                  : this.otherInboundData[rowIndex].productId);
          if (value && typeof value === 'object') {
            value = value.id ?? value.productId;
          }
          this.otherInboundData[rowIndex].productId = value;
          if (this.isEmpty(value)) {
            return;
          }
          const selectedProduct = (selected && typeof selected === 'object' && selected.name)
              ? selected
              : (this.productList || []).find(p => p.id === value || p.productId === value);
          const applyProduct = (item) => {
            if (!item) return;
            this.otherInboundData[rowIndex].productName = item.name;
            this.otherInboundData[rowIndex].productId = item.id;
            this.otherInboundData[rowIndex].productCode = item.code;
            this.otherInboundData[rowIndex].productSpecification = item.specification;
            this.otherInboundData[rowIndex].productCategoryName = item.productCategoryName;
            this.otherInboundData[rowIndex].productUnitName = item.unitName;
            this.otherInboundData[rowIndex].productUnitId = item.unitId;
            if (!this.otherInboundData[rowIndex].warehouseId) {
              const defaultId = this.resolveDefaultWarehouseId();
              const find = (this.warehouseList || []).find(w => w.id === defaultId);
              if (find) {
                this.otherInboundData[rowIndex].warehouseId = find.id;
                this.otherInboundData[rowIndex].warehouseName = find.name;
              }
            }
            this.$forceUpdate();
          };
          if (selectedProduct) {
            applyProduct(selectedProduct);
          } else {
            Product.list({id: value}).then(res => {
              const {success, data} = res;
              if (success) {
                applyProduct(data.results?.[0]);
              }
            });
          }
          break;
        }
        case 'warehouse': {
          let value = (selected && typeof selected === 'object')
              ? selected.id
              : (selected != null && typeof selected !== 'object'
                  ? selected
                  : this.otherInboundData[rowIndex].warehouseId);
          if (value && typeof value === 'object') {
            value = value.id;
          }
          this.otherInboundData[rowIndex].warehouseId = value;
          if (this.isEmpty(value)) {
            return;
          }
          const selectedWarehouse = (selected && typeof selected === 'object' && selected.name)
              ? selected
              : (this.warehouseList || []).find(w => w.id === value);
          if (selectedWarehouse) {
            this.otherInboundData[rowIndex].warehouseName = selectedWarehouse.name;
            this.otherInboundData[rowIndex].warehouseId = selectedWarehouse.id;
            this.$forceUpdate();
          } else {
            Warehouse.list({id: value}).then(res => {
              const {success, data} = res;
              if (success) {
                const item = data?.[0];
                if (!item) return;
                this.otherInboundData[rowIndex].warehouseName = item.name;
                this.otherInboundData[rowIndex].warehouseId = item.id;
                this.$forceUpdate();
              }
            });
          }
          break;
        }
        default:
          break;
      }
    },
    resolveDefaultWarehouseId() {
      if (this.warehouseId) return this.warehouseId;
      const found = (this.warehouseList || []).find(w => w.systemDefault || w.isDefault);
      this.warehouseId = found?.id || null;
      return this.warehouseId;
    },
    isEmpty(value) {
      return (value !== 0 && !value) || value === '';
    },
    saveOrder(type) {
      const filterOtherInboundData = this.otherInboundData.filter(item => !this.isEmpty(item.productId) || !this.isEmpty(item.warehouseId) || !this.isEmpty(item.quantity) || !this.isEmpty(item.remarks));
      if (!this.validatorsForm(filterOtherInboundData)) {
        return;
      }
      const params = this.getSaveOrderParams(filterOtherInboundData, type);
      LoadingPlugin(true);
      OtherInbound.save(params)
          .then(({success}) => {
            if (success) {
              MessagePlugin.success("保存成功~");
              this.clearForm();
              if (type === 'save') {
                this.closeWindow();
              }
            }
          })
          .finally(() => LoadingPlugin(false));
    },
    validatorsForm(filterOtherInboundData) {
      if (filterOtherInboundData.length === 0) {
        MessagePlugin.error("请填写操作数据~");
        return false;
      }
      if (filterOtherInboundData.some((c) => this.isEmpty(c.productId))) {
        MessagePlugin.error("请选择产品~");
        return false;
      }
      if (filterOtherInboundData.some((c) => this.isEmpty(c.warehouseId))) {
        MessagePlugin.error("请选择仓库~");
        return false;
      }
      if (filterOtherInboundData.some((c) => this.isEmpty(c.quantity) || Number(c.quantity) === 0)) {
        MessagePlugin.error("请填写数量~");
        return false;
      }
      if (filterOtherInboundData.some((c) => this.isEmpty(c.unitPrice))) {
        MessagePlugin.error("请填写入库单价~");
        return false;
      }
      if (filterOtherInboundData.some((c) => this.isEmpty(c.subtotal))) {
        MessagePlugin.error("请填写入库金额~");
        return false;
      }
      return true;
    },
    getSaveOrderParams(filterOtherInboundData) {
      const otherInboundItems = [];
      const otherInbound = {
        customerId: this.form.customerId,
        supplierId: this.form.supplierId,
        inboundType: this.form.inboundType,
        inboundDate: this.form.orderDate,
        remarks: this.form.remarks
      };
      otherInbound.id = this.form.id;
      if (this.form.stockTakeId !== null) {
        otherInbound.stockTakeId = this.form.stockTakeId;
      } else {
        otherInbound.stockTakeId = this.stockTakeId;
      }
      filterOtherInboundData.forEach(item => {
        otherInboundItems.push({
          productId: item.productId,
          baseUnitId: item.productUnitId,
          quantity: item.quantity,
          warehouseId: item.warehouseId,
          unitPrice: item.unitPrice,
          subtotal: item.subtotal,
        });
      });
      return {
        otherInbound: otherInbound,
        otherInboundItems: otherInboundItems
      };
    },
    clearForm() {
      this.form = {
        id: null,
        stockTakeId: null,
        orderDate: manba().format("YYYY-MM-dd"),
        remarks: null,
        customerId: null,
        supplierId: null,
        inboundType: '其他入库',
        totalAmount: 0.00,
        totalQuantity: 0,
        adminName: this.user?.admin?.name || '',
        orderStatus: '已保存',
        quantityTips: ''
      };
      this.otherInboundData = [];
      this.newOtherInboundData();
    },

    adjustRows(type, index) {
      if (type === "insert") {
        this.otherInboundData.splice(index + 1, 0, newRow({isNew: true}));
      } else {
        this.otherInboundData.splice(index, 1);
      }
    },
    newOtherInboundData() {
      for (let index = 0; index < 5; index++) {
        this.otherInboundData.push(newRow());
      }
    },
    quantityFocus({rowIndex}) {
      const otherInboundItem = this.otherInboundData[rowIndex];
      const {productId, warehouseId} = otherInboundItem;
      if (this.isEmpty(productId) || this.isEmpty(warehouseId)) {
        return;
      }
      Inventory.list({productId}).then(res => {
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
          this.otherInboundData[rowIndex].quantityTips = `总库存：${totalQuantity}\r\n仓库库存：${quantity}`;
        } else {
          this.otherInboundData[rowIndex].quantityTips = `总库存：0\r\n仓库库存：0`;
        }
      });
    },
    quantityBlur(type, {rowIndex}) {
      const inboundItem = this.otherInboundData[rowIndex];
      const quantity = inboundItem.quantity || 1;
      const unitPrice = inboundItem.unitPrice;
      const subtotal = inboundItem.subtotal;
      switch (type) {
        case "subtotal":
          if (subtotal) {
            this.otherInboundData[rowIndex].unitPrice = (parseFloat(subtotal) / parseInt(quantity)).toFixed(2);
            this.otherInboundData[rowIndex].subtotal = parseFloat(subtotal).toFixed(2);
          }
          break;
        case "unitPrice":
          if (unitPrice) {
            this.otherInboundData[rowIndex].subtotal = (parseFloat(unitPrice) * parseInt(quantity)).toFixed(2);
            this.otherInboundData[rowIndex].unitPrice = parseFloat(unitPrice).toFixed(2);
          }
          break;
        case "quantity":
          if (unitPrice && quantity) {
            this.otherInboundData[rowIndex].subtotal = (parseFloat(unitPrice) * parseInt(quantity)).toFixed(2);
          }
          break;
        default:
          break;
      }
    },
    loadEditForm(id) {
      this.increase = false;
      this.otherInboundData = [];
      OtherInbound.load(this.otherInboundId || id).then(
          ({data}) => {
            if (data && data.length > 0) {
              this.form.id = data[0].id;
              this.form.customerId = data[0].customerId;
              this.form.supplierId = data[0].supplierId;
              this.form.remarks = data[0].remarks;
              this.form.inboundType = data[0].inboundType;
              this.form.stockTakeId = data[0].stockTakeId;
              this.form.orderDate = data[0].inboundDate;
              this.form.adminName = data[0].adminName;
              this.form.orderStatus = data[0].orderStatus;
              let totalAmount = 0;
              let totalQuantity = 0;
              data.forEach(item => {
                totalAmount += parseFloat(item.subtotal);
                totalQuantity += parseInt(item.quantity);
                this.otherInboundData.push(newRow({
                  productUrl: '',
                  productCode: item.productCode,
                  productName: item.productName,
                  productId: item.productId,
                  productSpecification: item.productSpecification,
                  productCategoryName: item.productCategoryName,
                  productUnitId: item.productUnitId,
                  productUnitName: item.productUnitName,
                  warehouseName: item.warehouseName,
                  warehouseId: item.warehouseId,
                  quantity: item.quantity,
                  unitPrice: item.unitPrice,
                  subtotal: item.subtotal
                }));
              });
              this.form.totalAmount = totalAmount;
              this.form.totalQuantity = totalQuantity;
            }
          }
      );
    },
    loadDict(callback) {
      Promise.all([Product.select(), Warehouse.select(), Customer.select(), Supplier.select()])
          .then((results) => {
            this.productList = results[0].data || [];
            this.productList.forEach(item => {
              item.customName = `${item.code}--${item.name}`;
            });
            this.warehouseList = results[1].data || [];
            this.customerList = results[2].data || [];
            this.supplierList = results[3].data || [];
            this.warehouseId = this.warehouseList.find(
                (val) => val.systemDefault || val.isDefault
            )?.id;
            if (callback) {
              callback();
            }
          })
          .finally(() => LoadingPlugin(false));
    },
    initIncreaseForm() {
      this.increase = true;
      this.newOtherInboundData();
      this.form.adminName = this.user.admin.name;
      this.form.id = null;
      if (this.stockTakeId) {
        this.form.inboundType = "盘盈入库";
        this.otherInboundData = (JSON.parse(JSON.stringify(this.importInbound)) || []).map((row) => newRow(row));
      }
    },
    approved() {
      DialogPlugin.confirm({
        header: "审核提示",
        body: `确认审核该订单?`,
        onConfirm: () => {
          return OtherInbound.approved('已审核', [this.form.id]).then(() => {
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
          return OtherInbound.approved('已保存', [this.form.id]).then(() => {
            MessagePlugin.success("操作成功~");
            this.closeWindow();
          });
        }
      });
    },
    closeWindow() {
      this.closeSelfTab(this.index);
      this.pushTab({
        keepAlive: false,
        key: "OtherInboundList",
        title: "其他入库单"
      });
    },
  },
  created() {
    LoadingPlugin(true);
    this.loadDict(() => {
      if (this.otherInboundId) {
        this.loadEditForm();
        return;
      }
      this.initIncreaseForm();
    });
  },
};
</script>
<style lang="less" scoped>

.product-img {
  max-width: 48px;
  max-height: 48px;
  object-fit: contain;
}
</style>
