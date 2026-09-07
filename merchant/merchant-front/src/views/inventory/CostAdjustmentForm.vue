<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <div class="form-toolbar">
        <div class="form-toolbar__left">
          <label class="mr-20px ml-16px" style="font-size: 16px !important">单据日期：</label>
          <t-date-picker v-model="form.orderDate" :disabled="isAudited || looked"
                         :clearable="false"/>
          <label class="mr-20px ml-16px" style="font-size: 16px !important">调整类型：</label>
          <t-select
              v-model="form.adjustmentType"
              :options="adjustmentTypeOptions"
              :disabled="isAudited || looked"
              style="width: 140px"
          />
          <t-button v-if="!isAudited && !looked" variant="outline" theme="warning" size="small" class="ml-16px"
                    :loading="loading" @click="loadTailDifference">
            尾差调整
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
          :data="costAdjustmentData"
          :foot-data="footData"
          :loading="loading"
      >
        <template #ops="{ rowIndex }">
          <template v-if="rowIsSelect(rowIndex)">
            <div class="fa fa-plus text-hover mr-5px" @click="adjustRows('insert', rowIndex)"></div>
            <div v-if="costAdjustmentData.length !== 1" class="fa fa-minus text-hover-danger"
                 @click="adjustRows('delete', rowIndex)"></div>
          </template>
          <template v-else>{{ rowIndex + 1 }}</template>
        </template>
        <template #productUrl="{ row }">
          <img v-if="row.productUrl" :src="row.productUrl" alt="" class="product-img"/>
        </template>
        <template #productName="{ row, rowIndex }">
          <div class="input-group goodsSelect" v-if="!isAudited && !looked">
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
          <div class="input-group goodsSelect" v-if="!isAudited && !looked">
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
        <template #currentPrice="{ row }">
          <span v-if="row.currentPrice != null">{{ row.currentPrice }}</span>
          <span v-else class="text-gray-400">-</span>
        </template>
        <template #adjustmentAmount="{ row, rowIndex }">
          <t-input-number
              v-if="!isAudited && !looked"
              v-model="row.adjustmentAmount"
              theme="normal"
              :decimal-places="2"
              style="width: 100%"
              @change="() => calcAdjustedPrice(row)"
          />
          <div v-else class="flex">
            <div class="flex1 ml-8px">
              <div>{{ row.adjustmentAmount }}</div>
            </div>
          </div>
        </template>
        <template #adjustedPrice="{ row }">
          <span v-if="row.adjustedPrice != null" :class="row.adjustedPrice < 0 ? 'text-red' : ''">
            {{ row.adjustedPrice }}
          </span>
          <span v-else class="text-gray-400">-</span>
        </template>
        <template #remarks="{ row }">
          <t-input v-if="!isAudited && !looked" v-model="row.remarks"/>
          <div v-else class="flex">
            <div class="flex1 ml-8px">
              <div>{{ row.remarks }}</div>
            </div>
          </div>
        </template>
      </t-table>
      <div class="mt-10px"></div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px w-80px">备注说明：</label>
          <t-input :disabled="isAudited || looked" placeholder="请输入备注" maxlength="150"
                   style="width: 80%"
                   v-model="form.remarks"/>
          <label class="ml-16px w-180px">制单人：{{ form.adminName }}</label>
        </div>
      </div>
    </div>
    <div class="page-column-footer modal-column-between bg-white-color border">
      <t-button @click="closeWindow" :loading="loading"> 取消</t-button>
      <div>
        <t-button v-if="!isAudited && !looked" v-auth="'costAdjustment:edit'" theme="primary" @click="saveOrder('add')" :loading="loading">
          保存并新增
        </t-button>
        <t-button v-if="!isAudited && !looked" v-auth="'costAdjustment:edit'" @click="saveOrder('save')" :loading="loading"> 保存</t-button>
        <t-button @click="doPrint" :loading="loading"> 打印 </t-button>
        <t-button v-if="$can('costAdjustment:audit') && form.id && !isAudited && !looked" @click="approved()" :loading="loading"> 审核</t-button>
        <t-button v-if="$can('costAdjustment:audit') && isAudited && !looked" @click="backApproved()" :loading="loading"> 反审核</t-button>
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
import Inventory from "@js/api/inventory/Inventory";
import {mapMutations, mapState} from "vuex";
import CostAdjustment from "@js/api/inventory/CostAdjustment";
import Stamp from "../common/Stamp.vue";

let rowSeq = 0;
function newRow(extra = {}) {
  return { _rowKey: `r-${++rowSeq}`, productId: null, warehouseId: null, quantity: null, ...extra };
}

export default {
  name: "CostAdjustmentForm",
  components: {Stamp},
  props: {
    costAdjustmentId: [String, Number],
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
        { colKey: 'currentPrice', title: '当前单价', width: 100, align: 'right' },
        { colKey: 'adjustmentAmount', title: '调整金额', width: 120 },
        { colKey: 'adjustedPrice', title: '调整后单价', width: 100, align: 'right' },
        { colKey: 'remarks', title: '备注', width: 100 },
      ];
    },
    footData() {
      let totalAmount = 0;
      (this.costAdjustmentData || []).forEach((row) => {
        totalAmount += Number(row.adjustmentAmount || 0);
      });
      return [{ ops: '合计', adjustmentAmount: totalAmount.toFixed(2) }];
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
        orderDate: manba().format("YYYY-MM-dd"),
        remarks: null,
        adjustmentType: '入库调整',
        adminName: '',
        orderStatus: '已保存',
        totalAdjustmentAmount: 0
      },
      productData: [],
      costAdjustmentData: [],
      selectRowIndex: null,
      increase: true,
      customerList: [],
      supplierList: [],
      adjustmentTypeOptions: [
        {label: '入库调整', value: '入库调整'},
        {label: '出库调整', value: '出库调整'},
      ],
    };
  },
  methods: {
    doPrint() {
      const items = (this.costAdjustmentData || []).filter(r => r && !r.isNew && r.productId).map(r => {
        const p = (this.productList || []).find(x => (x.productId || x.id) === r.productId) || {};
        return {
          ...r,
          productName: r.productName || p.productName || p.name || p.customName || '',
          quantity: r.quantity ?? r.secondaryQuantity,
          price: r.unitPrice ?? r.secondaryPrice ?? r.price,
          amount: r.subtotal ?? r.amount,
        };
      });
      openPrint('成本调整单', {
        header: {
          ...this.form,
          partner: '',
          amount: this.form.finalAmount ?? this.form.totalAmount,
        },
        items,
      });
    },

    ...mapMutations(['closeSelfTab', 'pushTab']),
    resolveDefaultWarehouse() {
      return (this.warehouseList || []).find(w => w.systemDefault || w.isDefault) || null;
    },
    changeRow({rowIndex}, type, selected) {
      const row = this.costAdjustmentData?.[rowIndex];
      if (!row) return;
      switch (type) {
        case 'product': {
          let value = (selected && typeof selected === 'object')
              ? (selected.id ?? selected.productId)
              : (selected != null && typeof selected !== 'object' ? selected : row.productId);
          if (value && typeof value === 'object') {
            value = value.id ?? value.productId;
          }
          row.productId = value;
          if (this.isEmpty(value)) {
            return;
          }
          const selectedProduct = (selected && typeof selected === 'object' && selected.name)
              ? selected
              : (this.productList || []).find(p => p.id === value || p.productId === value);
          const applyProduct = (item) => {
            if (!item) return;
            row.productName = item.name;
            row.productId = item.id;
            row.productCode = item.code;
            row.productSpecification = item.specification;
            row.productCategoryName = item.productCategoryName;
            row.productUnitName = item.unitName;
            row.productUnitId = item.unitId;
            row.purchasePrice = item.purchasePrice;
            if (!row.warehouseId) {
              const find = this.resolveDefaultWarehouse();
              if (find) {
                row.warehouseId = find.id;
                row.warehouseName = find.name;
              }
            }
            this.loadCostDetail(row);
            this.$forceUpdate();
          };
          if (selectedProduct) {
            applyProduct(selectedProduct);
          } else {
            Product.list({id: value}).then(res => {
              const {success, data} = res || {};
              if (success) {
                applyProduct(data?.results?.[0]);
              }
            });
          }
          break;
        }
        case 'warehouse': {
          let value = (selected && typeof selected === 'object')
              ? selected.id
              : (selected != null && typeof selected !== 'object' ? selected : row.warehouseId);
          if (value && typeof value === 'object') {
            value = value.id;
          }
          row.warehouseId = value;
          if (this.isEmpty(value)) {
            return;
          }
          const selectedWarehouse = (selected && typeof selected === 'object' && selected.name)
              ? selected
              : (this.warehouseList || []).find(w => w.id === value);
          if (selectedWarehouse) {
            row.warehouseName = selectedWarehouse.name;
            row.warehouseId = selectedWarehouse.id;
            this.loadCostDetail(row);
            this.$forceUpdate();
          } else {
            Warehouse.list({id: value}).then(res => {
              const {success, data} = res || {};
              if (success) {
                const item = data?.[0];
                if (item) {
                  row.warehouseName = item.name;
                  row.warehouseId = item.id;
                  this.$forceUpdate();
                }
              }
            });
          }
          break;
        }
        default:
          break;
      }
    },
    getTotalCost({rowIndex}) {
      const item = this.costAdjustmentData?.[rowIndex];
      if (!item?.productId || !item?.warehouseId) {
        return;
      }
      Inventory.totalCost(item.productId, item.warehouseId).then(res => {
        const totalCost = res?.data ?? 0;
        item.totalCost = totalCost;
        item.quantityTips = `总成本：${totalCost}`;
      });
    },
    loadCostDetail(row) {
      if (!row.productId || !row.warehouseId) return;
      Inventory.costDetail(row.productId, row.warehouseId).then(res => {
        const data = res?.data || {};
        row.totalCost = data.totalCost || 0;
        const qty = data.currentQuantity || 0;
        row.currentQuantity = qty;
        row.currentPrice = qty !== 0 ? (data.totalCost / qty).toFixed(2) : '0.00';
        this.calcAdjustedPrice(row);
        this.$forceUpdate();
      });
    },
    calcAdjustedPrice(row) {
      const qty = row.currentQuantity || 0;
      const totalCost = Number(row.totalCost || 0);
      const adjustment = Number(row.adjustmentAmount || 0);
      if (qty !== 0) {
        row.adjustedPrice = ((totalCost + adjustment) / qty).toFixed(2);
      } else {
        row.adjustedPrice = '0.00';
      }
    },
    loadTailDifference() {
      this.loading = true;
      Inventory.tailDifference().then(res => {
        const list = res?.data || [];
        if (list.length === 0) {
          MessagePlugin.info('没有发现尾差记录');
          return;
        }
        this.costAdjustmentData = list.map(item => newRow({
          productId: item.productId,
          productCode: item.productCode,
          productName: item.productName,
          warehouseId: item.warehouseId,
          warehouseName: item.warehouseName,
          totalCost: item.totalCost,
          currentQuantity: 0,
          currentPrice: '0.00',
          adjustmentAmount: -item.totalCost,
          adjustedPrice: '0.00',
        }));
        this.form.adjustmentType = '出库调整';
        MessagePlugin.success(`已加载 ${list.length} 条尾差记录`);
      }).finally(() => this.loading = false);
    },
    isEmpty(value) {
      return (value !== 0 && !value) || value === '';
    },
    saveOrder(type) {
      const filterCostAdjustmentData = (this.costAdjustmentData || []).filter(item =>
          !this.isEmpty(item.productId) || !this.isEmpty(item.warehouseId) || !this.isEmpty(item.quantity) || !this.isEmpty(item.remarks) || !this.isEmpty(item.adjustmentAmount)
      );
      if (!this.validatorsForm(filterCostAdjustmentData)) {
        return;
      }
      const params = this.getSaveOrderParams(filterCostAdjustmentData);
      LoadingPlugin(true);
      CostAdjustment.save(params)
          .then(({success, data}) => {
            if (success) {
              MessagePlugin.success("保存成功~");
              setTimeout(() => {
                if (type === 'add') {
                  this.clearForm();
                  this.closeWindow();
                  this.pushTab({
                    key: 'CostAdjustmentForm',
                    title: '新增成本调整单',
                    params: {type: 'add', costAdjustmentId: null}
                  });
                } else {
                  this.closeWindow();
                  this.pushTab({
                    key: 'CostAdjustmentForm',
                    title: '编辑成本调整单',
                    params: {type: 'edit', costAdjustmentId: data?.id}
                  });
                  this.$emit("update:costAdjustmentId", data?.id);
                  this.$emit("update:type", "edit");
                  if (data?.id) {
                    this.loadEditForm(data.id);
                  }
                }
              }, 300);
            }
          })
          .finally(() => LoadingPlugin(false));
    },
    validatorsForm(filterCostAdjustmentData) {
      if (!filterCostAdjustmentData || filterCostAdjustmentData.length === 0) {
        MessagePlugin.error("请填写操作数据~");
        return false;
      }
      if (filterCostAdjustmentData.some((c) => this.isEmpty(c.productId))) {
        MessagePlugin.error("请选择产品~");
        return false;
      }
      if (filterCostAdjustmentData.some((c) => this.isEmpty(c.warehouseId))) {
        MessagePlugin.error("请选择仓库~");
        return false;
      }
      if (filterCostAdjustmentData.some((c) => this.isEmpty(c.adjustmentAmount) || Number(c.adjustmentAmount) === 0)) {
        MessagePlugin.error("请填写调整金额~");
        return false;
      }
      if (filterCostAdjustmentData.some((c) => (Number(c.totalCost || 0) + Number(c.adjustmentAmount || 0)) < 0)) {
        MessagePlugin.error("调整后总成本不能为负数~");
        return false;
      }
      return true;
    },
    getSaveOrderParams(filterCostAdjustmentData) {
      // Backend entity field is typo'd as djustmentDate — keep API payload aligned
      const costAdjustment = {
        adjustmentType: this.form.adjustmentType,
        djustmentDate: this.form.orderDate,
        remarks: this.form.remarks,
        id: this.form.id
      };
      const costAdjustmentItems = (filterCostAdjustmentData || []).map(item => ({
        productId: item.productId,
        baseUnitId: item.productUnitId,
        remarks: item.remarks,
        warehouseId: item.warehouseId,
        adjustmentAmount: item.adjustmentAmount,
        totalCost: item.totalCost,
      }));
      return {
        costAdjustment,
        costAdjustmentItems
      };
    },
    clearForm() {
      this.form = {
        id: null,
        orderDate: manba().format("YYYY-MM-dd"),
        remarks: null,
        adjustmentType: '入库调整',
        adminName: '',
        orderStatus: '已保存',
        totalAdjustmentAmount: 0
      };
      this.costAdjustmentData = [];
      this.newCostAdjustmentData();
    },
    adjustRows(type, index) {
      if (type === "insert") {
        this.costAdjustmentData.splice(index + 1, 0, newRow({isNew: true}));
      } else {
        this.costAdjustmentData.splice(index, 1);
      }
    },
    newCostAdjustmentData() {
      for (let index = 0; index < 5; index++) {
        this.costAdjustmentData.push(newRow());
      }
    },
    rowIsSelect() {
      return !this.isAudited && !this.looked;
    },
    loadEditForm(id) {
      this.increase = false;
      this.costAdjustmentData = [];
      CostAdjustment.load(this.costAdjustmentId || id).then(
          ({data}) => {
            if (data && data.length > 0) {
              const first = data[0] || {};
              this.form.id = first.id;
              this.form.remarks = first.costRemarks;
              this.form.adjustmentType = first.adjustmentType;
              // Map backend typo djustmentDate → UI orderDate so DatePicker restores
              this.form.orderDate = first.djustmentDate || first.adjustmentDate || this.form.orderDate;
              this.form.adminName = first.adminName;
              this.form.orderStatus = first.orderStatus;
              let totalAdjustmentAmount = 0;
              data.forEach(item => {
                totalAdjustmentAmount += parseFloat(item.adjustmentAmount || 0);
                const row = newRow({
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
                  remarks: item.remarks,
                  totalCost: item.totalCost,
                  adjustmentAmount: item.adjustmentAmount
                });
                this.costAdjustmentData.push(row);
                this.loadCostDetail(row);
              });
              this.form.totalAdjustmentAmount = totalAdjustmentAmount;
            }
          }
      );
    },
    loadDict(callback) {
      Promise.all([Product.select(), Warehouse.select(), Customer.select(), Supplier.select()])
          .then((results) => {
            this.productList = results[0]?.data || [];
            this.productList.forEach(item => {
              item.customName = `${item.code}--${item.name}`;
            });
            this.warehouseList = results[1]?.data || [];
            this.customerList = results[2]?.data || [];
            this.supplierList = results[3]?.data || [];
            this.warehouseId = this.resolveDefaultWarehouse()?.id || null;
            if (callback) {
              callback();
            }
          })
          .finally(() => LoadingPlugin(false));
    },
    initIncreaseForm() {
      this.increase = true;
      this.newCostAdjustmentData();
      this.form.adminName = this.user?.admin?.name || '';
      this.form.id = null;
    },
    initAuditsForm() {
    },
    approved() {
      if (!this.form.id) {
        MessagePlugin.warning("请先保存单据~");
        return;
      }
      DialogPlugin.confirm({
        header: "审核提示",
        body: `确认审核该订单?`,
        onConfirm: () => {
          return CostAdjustment.approved('已审核', [this.form.id]).then(() => {
            MessagePlugin.success("操作成功~");
            this.closeWindow();
          });
        }
      });
    },
    backApproved() {
      if (!this.form.id) {
        return;
      }
      DialogPlugin.confirm({
        header: "反审核提示",
        body: `确认反审核该订单?`,
        onConfirm: () => {
          return CostAdjustment.approved('已保存', [this.form.id]).then(() => {
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
        key: "CostAdjustmentList",
        title: "成本调整单"
      });
    },
  },
  created() {
    LoadingPlugin(true);
    this.loadDict(() => {
      if (this.costAdjustmentId) {
        this.loadEditForm();
        const type = this.type;
        switch (type) {
          case 'audits':
          case 'antiAudits':
            this.initAuditsForm();
            break;
          default:
            break;
        }
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
