<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <div class="form-toolbar">
        <div class="form-toolbar__left">
          <label class="mr-20px ml-16px" style="font-size: 16px !important">单据日期：</label>
          <t-date-picker v-model="form.transferDate" :disabled="isLocked"
                         :clearable="false"/>
          <label class="mr-20px ml-20px" style="font-size: 16px !important">调出仓库：</label>
          <t-select class="w-178px" filterable :options="warehouseList"
                    :keys="{ value: 'id', label: 'name' }"
                    v-model="form.fromWarehouseId" placeholder="请选择调出仓库"
                    :disabled="isLocked" clearable
                    @change="changeFromWarehouseId"/>
          <label class="mr-20px ml-20px" style="font-size: 16px !important">调入仓库：</label>
          <t-select class="w-178px" filterable :options="warehouseList"
                    :keys="{ value: 'id', label: 'name' }"
                    v-model="form.toWarehouseId" placeholder="请选择调入仓库"
                    :disabled="isLocked" clearable/>
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
          :data="inventoryTransferData"
          :foot-data="footData"
          :loading="loading"
      >
        <template #ops="{ rowIndex }">
          <template v-if="rowIsSelect(rowIndex)">
            <div class="fa fa-plus text-hover mr-5px" @click="adjustRows('insert', rowIndex)"></div>
            <div v-if="inventoryTransferData.length !== 1" class="fa fa-minus text-hover-danger"
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
        <template #quantity="{ row }">
          <!-- 库存调拨为仓内移库整件口径，恒0位、不随账套参数 -->
          <t-input-number
              v-if="!isLocked"
              v-model="row.quantity"
              theme="normal"
              :min="0"
              :decimal-places="0"
              style="width: 100%"
          />
          <div v-else class="flex">
            <div class="flex1 ml-8px">
              <div>{{ row.quantity }}</div>
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
        <t-button v-if="!isAudited && !looked" v-auth="'inventoryTransfer:edit'" theme="primary" @click="saveOrder('add')" :loading="loading">
          保存并新增
        </t-button>
        <t-button v-if="!isAudited && !looked" v-auth="'inventoryTransfer:edit'" @click="saveOrder('save')" :loading="loading"> 保存</t-button>
        <t-button @click="doPrint" :loading="loading"> 打印 </t-button>
        <t-button v-if="$can('inventoryTransfer:audit') && form.id && !isAudited" @click="approved()" :loading="loading"> 审核</t-button>
        <t-button v-if="$can('inventoryTransfer:audit') && isAudited" @click="backApproved()" :loading="loading"> 反审核</t-button>
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
import InventoryTransfer from "@js/api/inventory/InventoryTransfer";
import Inventory from "@js/api/inventory/Inventory";
import {mapMutations, mapState} from "vuex";
import Stamp from "../common/Stamp.vue";

let rowSeq = 0;
function newRow(extra = {}) {
  return { _rowKey: `r-${++rowSeq}`, productId: null, quantity: null, ...extra };
}

export default {
  name: "InventoryTransferForm",
  components: {Stamp},
  props: {
    inventoryTransferId: [String, Number],
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
        { colKey: 'productUrl', title: '产品图片', width: 250 },
        { colKey: 'productCode', title: '产品编码', width: 240 },
        { colKey: 'productName', title: '产品名称', minWidth: 350 },
        { colKey: 'productSpecification', title: '规格型号', align: 'center', width: 100 },
        { colKey: 'productCategoryName', title: '产品类别', align: 'center', width: 100 },
        { colKey: 'productUnitName', title: '单位', width: 100 },
        { colKey: 'warehouseTotal', title: '总库存', width: 100 },
        { colKey: 'warehouseQuantity', title: '仓库库存', width: 100 },
        { colKey: 'quantity', title: '数量', width: 100 },
      ];
    },
    footData() {
      let totalQuantity = 0;
      (this.inventoryTransferData || []).forEach((row) => {
        totalQuantity += Number(row.quantity || 0);
      });
      return [{ ops: '合计', quantity: totalQuantity }];
    }
  },
  data() {
    return {
      refresh: false,
      loading: false,
      productList: [],
      selectProductList: [],
      product: null,
      warehouseList: [],
      warehouseId: null,
      form: {
        id: null,
        transferDate: manba().format("YYYY-MM-dd"),
        remarks: null,
        toWarehouseId: null,
        fromWarehouseId: null,
        totalAmount: 0.00,
        totalQuantity: 0,
        adminName: '',
        orderStatus: '已保存'
      },
      productData: [],
      inventoryTransferData: [],
      selectRowIndex: null,
      increase: true,
    };
  },
  methods: {
    doPrint() {
      const items = (this.inventoryTransferData || []).filter(r => r && !r.isNew && r.productId).map(r => {
        const p = (this.productList || []).find(x => (x.productId || x.id) === r.productId) || {};
        return {
          ...r,
          productName: r.productName || p.productName || p.name || p.customName || '',
          quantity: r.quantity ?? r.secondaryQuantity,
          price: r.unitPrice ?? r.secondaryPrice ?? r.price,
          amount: r.subtotal ?? r.amount,
        };
      });
      openPrint('调拨单', {
        header: {
          ...this.form,
          partner: ((this.warehouseList.find(w => w.id === this.form.fromWarehouseId) || {}).name || '') + ' -> ' + ((this.warehouseList.find(w => w.id === this.form.toWarehouseId) || {}).name || ''),
          amount: this.form.finalAmount ?? this.form.totalAmount,
        },
        items,
      });
    },

    ...mapMutations(['closeSelfTab', 'pushTab']),
    changeRow({rowIndex}, type, selected) {
      switch (type) {
        case 'product': {
          let value = (selected && typeof selected === 'object')
              ? (selected.id ?? selected.productId)
              : (selected != null && typeof selected !== 'object'
                  ? selected
                  : this.inventoryTransferData[rowIndex].productId);
          if (value && typeof value === 'object') {
            value = value.id ?? value.productId;
          }
          this.inventoryTransferData[rowIndex].productId = value;
          if (this.isEmpty(value)) {
            return;
          }
          let hasProduct = false;
          for (let i = 0; i < this.inventoryTransferData.length; i++) {
            const item = this.inventoryTransferData[i];
            if (Number(item.productId) === Number(value) && Number(i) !== Number(rowIndex)) {
              hasProduct = true;
              break;
            }
          }
          if (hasProduct) {
            setTimeout(() => {
              this.inventoryTransferData[rowIndex].productId = null;
              this.inventoryTransferData[rowIndex].productName = null;
              this.inventoryTransferData[rowIndex].productCode = null;
              this.inventoryTransferData[rowIndex].productSpecification = null;
              this.inventoryTransferData[rowIndex].productCategoryName = null;
              this.inventoryTransferData[rowIndex].productUnitName = null;
              this.inventoryTransferData[rowIndex].productUnitId = null;
              this.inventoryTransferData[rowIndex].warehouseQuantity = null;
              this.inventoryTransferData[rowIndex].warehouseTotal = null;
            }, 0);
            return;
          }
          const selectedProduct = (selected && typeof selected === 'object' && selected.name)
              ? selected
              : (this.productList || []).find(p => p.id === value || p.productId === value);
          const applyProduct = (item) => {
            if (!item) return;
            this.inventoryTransferData[rowIndex].productName = item.name;
            this.inventoryTransferData[rowIndex].productId = item.id;
            this.inventoryTransferData[rowIndex].productCode = item.code;
            this.inventoryTransferData[rowIndex].productSpecification = item.specification;
            this.inventoryTransferData[rowIndex].productCategoryName = item.productCategoryName;
            this.inventoryTransferData[rowIndex].productUnitName = item.unitName;
            this.inventoryTransferData[rowIndex].productUnitId = item.unitId;
            this.$forceUpdate();
          };
          if (selectedProduct) {
            applyProduct(selectedProduct);
          } else {
            Product.list({id: value}).then(res => {
              const {success, data} = res;
              if (success) {
                applyProduct(data?.results?.[0]);
              }
            });
          }
          break;
        }
        default:
          break;
      }
      this.quantityFocus({rowIndex});
    },
    isEmpty(value) {
      return (value !== 0 && !value) || value === '';
    },
    saveOrder(type) {
      const filterInventoryTransferData = this.inventoryTransferData.filter(item => !this.isEmpty(item.productId) || !this.isEmpty(item.warehouseId) || !this.isEmpty(item.quantity) || !this.isEmpty(item.remarks));
      this.validatorsForm(filterInventoryTransferData);
      const params = this.getSaveOrderParams(filterInventoryTransferData, type);
      InventoryTransfer.save(params)
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
    validatorsForm(filterInventoryTransferData) {
      if (filterInventoryTransferData.length === 0) {
        throw new Error("请填写操作数据~")
      }
      LoadingPlugin(true);
      let productData = filterInventoryTransferData.filter((c) => this.isEmpty(c.productId));
      if (productData.length > 0) {
        LoadingPlugin(false);
        throw new Error("请选择产品~")
      }
      if (this.isEmpty(this.form.fromWarehouseId)) {
        LoadingPlugin(false);
        throw new Error("请选择调出仓库~")
      }
      if (this.isEmpty(this.form.toWarehouseId)) {
        LoadingPlugin(false);
        throw new Error("请选择调入仓库~")
      }
      if (this.form.fromWarehouseId === this.form.toWarehouseId) {
        LoadingPlugin(false);
        throw new Error("调出仓库和调入仓库不能是同一个～");
      }
      let quantity = filterInventoryTransferData.filter((c) => this.isEmpty(c.quantity) || Number(c.quantity) === 0);
      if (quantity.length > 0) {
        LoadingPlugin(false);
        throw new Error("请填写数量~")
      }
      let hasQuantity = true;
      filterInventoryTransferData.forEach(item => {
        const {warehouseQuantity, quantity} = item;
        if (Number(warehouseQuantity) < Number(quantity)) {
          hasQuantity = false;
        }
      });
      if (!hasQuantity) {
        LoadingPlugin(false);
        throw new Error("调出数量不能大于当前仓库库存~")
      }
    },
    getSaveOrderParams(filterInventoryTransferData) {
      const inventoryTransferItems = [];
      const inventoryTransfer = {
        fromWarehouseId: this.form.fromWarehouseId,
        toWarehouseId: this.form.toWarehouseId,
        transferDate: this.form.transferDate,
        remarks: this.form.remarks
      };
      inventoryTransfer.id = this.form.id;
      filterInventoryTransferData.forEach(item => {
        inventoryTransferItems.push({
          productId: item.productId,
          quantity: item.quantity,
          fromWarehouseId: this.form.fromWarehouseId,
          toWarehouseId: this.form.toWarehouseId,
        });
      });
      return {
        inventoryTransfer: inventoryTransfer,
        inventoryTransferItems: inventoryTransferItems
      };
    },
    clearForm() {
      this.form = {
        id: null,
        transferDate: manba().format("YYYY-MM-dd"),
        remarks: null,
        toWarehouseId: null,
        fromWarehouseId: null,
        totalAmount: 0.00,
        totalQuantity: 0,
        adminName: this.user?.admin?.name || '',
        orderStatus: '已保存'
      };
      this.inventoryTransferData = [];
      this.newInventoryTransferData();
      this.applyWarehouseDefaults();
    },
    applyWarehouseDefaults() {
      const list = this.warehouseList || [];
      const defaultWh = list.find(w => w.systemDefault || w.isDefault);
      this.form.fromWarehouseId = defaultWh?.id ?? list[0]?.id ?? null;
      const toWh = list.find(w => w.id !== this.form.fromWarehouseId);
      this.form.toWarehouseId = toWh?.id ?? null;
    },

    adjustRows(type, index) {
      if (type === "insert") {
        this.inventoryTransferData.splice(index + 1, 0, newRow({isNew: true}));
      } else {
        this.inventoryTransferData.splice(index, 1);
      }
    },
    newInventoryTransferData() {
      for (let index = 0; index < 5; index++) {
        this.inventoryTransferData.push(newRow());
      }
    },
    rowIsSelect() {
      return !this.isLocked;
    },
    quantityFocus({rowIndex}) {
      const inventoryTransferItem = this.inventoryTransferData[rowIndex];
      const {productId} = inventoryTransferItem;
      const warehouseId = this.form.fromWarehouseId;
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
          this.inventoryTransferData[rowIndex].quantityTips = `总库存：${totalQuantity}\r\n仓库库存：${quantity}`;
          this.inventoryTransferData[rowIndex].warehouseQuantity = quantity;
          this.inventoryTransferData[rowIndex].warehouseTotal = totalQuantity;
        } else {
          this.inventoryTransferData[rowIndex].quantityTips = `总库存：0\r\n仓库库存：0`;
          this.inventoryTransferData[rowIndex].warehouseQuantity = 0;
          this.inventoryTransferData[rowIndex].warehouseTotal = 0;
        }
      });
    },
    changeFromWarehouseId() {
      const warehouseId = this.form.fromWarehouseId;
      if (this.isEmpty(warehouseId)) {
        return;
      }
      const inventoryTransferData = this.inventoryTransferData;
      inventoryTransferData.forEach(async inventoryTransferItem => {
        const {productId} = inventoryTransferItem;
        if (this.isEmpty(productId)) {
          return;
        }
        const {data} = await Inventory.list({productId});
        if (data && data.results) {
          let totalQuantity = 0;
          let quantity = 0;
          data.results.forEach(item => {
            totalQuantity += Number(item.currentQuantity);
            if (Number(item.warehouseId) === Number(warehouseId)) {
              quantity = item.currentQuantity;
            }
          });
          inventoryTransferItem.quantityTips = `总库存：${totalQuantity}\r\n仓库库存：${quantity}`;
          inventoryTransferItem.warehouseQuantity = quantity;
          inventoryTransferItem.warehouseTotal = totalQuantity;
        } else {
          inventoryTransferItem.quantityTips = `总库存：0\r\n仓库库存：0`;
          inventoryTransferItem.warehouseQuantity = 0;
          inventoryTransferItem.warehouseTotal = 0;
        }
      });
    },
    quantityBlur() {
    },
    loadEditForm(id) {
      this.increase = false;
      this.inventoryTransferData = [];
      InventoryTransfer.load(this.inventoryTransferId || id).then(
          ({data}) => {
            const first = data?.[0];
            if (!first) return;
            this.form.id = first.id;
            this.form.fromWarehouseId = first.fromWarehouseId;
            this.form.toWarehouseId = first.toWarehouseId;
            this.form.remarks = first.remarks;
            this.form.transferDate = first.transferDate;
            this.form.adminName = first.adminName;
            this.form.orderStatus = first.orderStatus;
            let totalQuantity = 0;
            data.forEach(item => {
              totalQuantity += parseInt(item.quantity);
              this.inventoryTransferData.push(newRow({
                productUrl: '',
                productCode: item.productCode,
                productName: item.productName,
                productId: item.productId,
                productSpecification: item.productSpecification,
                productCategoryName: item.productCategoryName,
                productUnitId: item.productUnitId,
                productUnitName: item.productUnitName,
                quantity: item.quantity,
                warehouseQuantity: item.warehouseQuantity,
                warehouseTotal: item.warehouseTotal,
              }));
            });
            this.form.totalQuantity = totalQuantity;
          }
      );
    },
    loadDict(callback) {
      Promise.all([Product.select(), Warehouse.select()])
          .then((results) => {
            this.productList = results[0]?.data || [];
            this.productList.forEach(item => {
              item.customName = `${item.code}--${item.name}`;
            });
            this.warehouseList = results[1]?.data || [];
            if (!this.inventoryTransferId) {
              this.applyWarehouseDefaults();
            }
            if (callback) {
              callback();
            }
          })
          .finally(() => LoadingPlugin(false));
    },
    initIncreaseForm() {
      this.increase = true;
      this.newInventoryTransferData();
      this.form.adminName = this.user.admin.name;
      this.form.id = null;
    },
    approved() {
      DialogPlugin.confirm({
        header: "审核提示",
        body: `确认审核该订单?`,
        onConfirm: () => {
          return InventoryTransfer.approved('已审核', [this.form.id]).then(() => {
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
          return InventoryTransfer.approved('已保存', [this.form.id]).then(() => {
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
        key: "InventoryTransferList",
        title: "调拨单"
      });
    },
  },
  created() {
    LoadingPlugin(true);
    this.loadDict(() => {
      if (this.inventoryTransferId) {
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
