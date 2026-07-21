<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <vxe-toolbar class-name="!size--mini">
        <template #buttons>
          <label class="mr-20px ml-16px" style="font-size: 16px !important">单据日期：</label>
          <DatePicker v-model="form.orderDate" :disabled="isAudited || looked"
                      :option="{ start: accountBook.checkoutDate }"
                      :clearable="false">
          </DatePicker>
        </template>
        <template #tools>
          <Stamp v-if="isAudited"/>
        </template>
      </vxe-toolbar>
      <vxe-table :edit-rules="validRules" size="mini" ref="xTable" border show-overflow keep-source
                 :edit-config="editConfig" :row-config="{ height: 40, isCurrent: true, isHover: true }"
                 :tooltip-config="tooltipConfig" show-footer :footer-method="footerMethod" stripe
                 :data="costAdjustmentData"
                 @current-change="currentChangeEvent" @cell-click="tableCellClick">
        <vxe-column title="操作" field="seq" width="70" align="center" fixed="left">
          <template #default="{ row, rowIndex }">
            <div v-if="rowIsSelect(rowIndex)">
              <div class="fa fa-plus text-hover mr-5px" @click="adjustRows('insert', rowIndex)"></div>
              <div v-if="costAdjustmentData.length !== 1" class="fa fa-minus text-hover-danger"
                   @click="adjustRows('delete', rowIndex)"></div>
            </div>
            <div v-else>
              {{ rowIndex + 1 }}
            </div>
          </template>
        </vxe-column>
        <vxe-column field="productUrl" title="产品图片" width="100" :cell-render="imgUrlCellRender"></vxe-column>
        <vxe-column field="productCode" title="产品编码" width="100"></vxe-column>
        <vxe-column field="productName" title="产品名称" min-width="300">
          <template #default="scope">
            <div class="h-input-group goodsSelect" v-if="!isAudited && !looked">
              <Select :deletable="false" ref="ms" v-model="scope.row.productId" :datas="productList" filterable :equalWidth="false"
                      placeholder="输入编码/名称" keyName="id" titleName="customName" @change="(e) => changeRow(scope, 'product', e)">
                <template v-slot:top>
                  <table class="h-table" style="width: 100%">
                    <thead class="h-table-header">
                    <tr>
                      <td width="150" align="center">产品编号</td>
                      <td width="150" align="center">产品图片</td>
                      <td width="150" align="center">产品名称</td>
                      <td width="150" align="center">产品类别</td>
                      <td width="150" align="center">产品规格</td>
                    </tr>
                    </thead>
                  </table>
                </template>
                <template v-slot:item="{ item }">
                  <table>
                    <tbody class="h-table-body-table">
                    <tr>
                      <td width="150" align="center">{{ item.code }}</td>
                      <td width="150" align="center">{{ item.imageUrl }}</td>
                      <td width="150" align="center">{{ item.name }}</td>
                      <td width="150" align="center">{{ item.productCategoryName }}</td>
                      <td width="150" align="center">{{ item.specification }}</td>
                    </tr>
                    </tbody>
                  </table>
                </template>
              </Select>
            </div>
            <div v-else class="flex">
              <div class="flex1 ml-8px">
                <div>{{ scope.row.productCode }}--{{ scope.row.productName }}</div>
              </div>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="规格型号" field="productSpecification" align="center" width="80"></vxe-column>
        <vxe-column title="产品类别" field="productCategoryName" align="center" width="120"></vxe-column>
        <vxe-column title="单位" field="productUnitName" width="90"/>
        <vxe-column title="仓库" field="warehouseName" width="300">
          <template #default="scope">
            <div class="h-input-group goodsSelect" v-if="!isAudited && !looked">
              <Select :deletable="false" ref="ms" v-model="scope.row.warehouseId" :datas="warehouseList" filterable
                      placeholder="请选择仓库" keyName="id" titleName="name" @change="(e) => changeRow(scope, 'warehouse', e)">
                <template v-slot:item="{ item }">
                  <div>{{ item.name }}</div>
                </template>
              </Select>
            </div>
            <div v-else class="flex">
              <div class="flex1 ml-8px">
                <div>{{ scope.row.warehouseName }}</div>
              </div>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="调整金额" field="adjustmentAmount" width="100">
          <template #default="scope">
            <vxe-tooltip v-if="!isAudited && !looked" theme="light" :content="scope.row.quantityTips">
              <vxe-input @focus="getTotalCost(scope)"
                         v-model.number="scope.row.adjustmentAmount" type="int" min="0" :controls="false">
              </vxe-input>
            </vxe-tooltip>
            <div v-else class="flex">
              <div class="flex1 ml-8px">
                <div>{{ scope.row.adjustmentAmount }}</div>
              </div>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="备注" field="remarks" width="100">
          <template #default="scope">
            <vxe-input v-if="!isAudited && !looked" v-model="scope.row.remarks" :controls="false">
            </vxe-input>
            <div v-else class="flex">
              <div class="flex1 ml-8px">
                <div>{{ scope.row.remarks }}</div>
              </div>
            </div>
          </template>
        </vxe-column>
      </vxe-table>
      <div class="mt-10px"></div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px w-80px">备注说明：</label>
          <Input :disabled="isAudited || looked" placeholder="请输入备注" type="text" maxlength="150"
                 style="width: 80%"
                 v-model="form.remarks"/>
          <label class="ml-16px w-180px">制单人：{{ form.adminName }}</label>
        </div>
      </div>
    </div>
    <div class="page-column-footer modal-column-between bg-white-color border">
      <Button @click="closeWindow" :loading="loading"> 取消</Button>
      <div>
        <Button v-if="!isAudited && !looked" color="primary" @click="saveOrder('add')" :loading="loading">
          保存并新增
        </Button>
        <Button v-if="!isAudited && !looked" @click="saveOrder('save')" :loading="loading"> 保存</Button>
        <Button @click="doPrint" :loading="loading"> 打印 </Button>
        <Button v-if="form.id && !isAudited && !looked" @click="approved()" :loading="loading"> 审核</Button>
        <Button v-if="isAudited && !looked" @click="backApproved()" :loading="loading"> 反审核</Button>
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
      validRules: {
        productName: [
          {required: true, message: '请选择产品名称'},
        ],
        warehouseName: [
          {required: true, message: '请选择仓库'},
        ],
        quantity: [
          {required: true, message: '请填写数量'},
        ]
      },
      tooltipConfig: {
        showAll: false,
        enterable: false,
      },
      editConfig: {trigger: 'click', mode: 'row'}
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
    footerMethod({columns, data}) {
      let totalQuantity = 0;
      let totalAmount = 0.00;
      (columns || []).forEach(column => {
        if (column.property && ['quantity', 'subtotal'].includes(column.property)) {
          (data || []).forEach((row) => {
            switch (column.property) {
              case 'quantity': {
                let rd = row[column.property];
                if (rd) {
                  totalQuantity += Number(rd || 0);
                }
                break;
              }
              case 'subtotal': {
                let rd = row[column.property];
                if (rd) {
                  totalAmount += Number(rd || 0);
                }
                break;
              }
              default:
                break;
            }
          });
        }
      });
      return [
        ["", "", "", "", "", "", "", "", totalQuantity, "", totalAmount],
      ];
    },
    resolveDefaultWarehouse() {
      return (this.warehouseList || []).find(w => w.systemDefault || w.isDefault) || null;
    },
    changeRow({rowIndex}, type, selected) {
      const row = this.costAdjustmentData?.[rowIndex];
      if (!row) return;
      switch (type) {
        case 'product': {
          const selectedProduct = (selected && typeof selected === 'object') ? selected : null;
          let value = selectedProduct
              ? (selectedProduct.id ?? selectedProduct.productId)
              : row.productId;
          if (value && typeof value === 'object') {
            value = value.id ?? value.productId;
          }
          row.productId = value;
          if (this.isEmpty(value)) {
            return;
          }
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
            this.$forceUpdate();
          };
          if (selectedProduct && selectedProduct.name) {
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
          const selectedWarehouse = (selected && typeof selected === 'object') ? selected : null;
          let value = selectedWarehouse ? selectedWarehouse.id : row.warehouseId;
          if (value && typeof value === 'object') {
            value = value.id;
          }
          row.warehouseId = value;
          if (this.isEmpty(value)) {
            return;
          }
          if (selectedWarehouse && selectedWarehouse.name) {
            row.warehouseName = selectedWarehouse.name;
            row.warehouseId = selectedWarehouse.id;
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
      if (filterCostAdjustmentData.some((c) => (Number(c.totalCost || 0) + Number(c.adjustmentAmount || 0)) <= 0)) {
        MessagePlugin.error("调整后金额不能小于等于零~");
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
        this.costAdjustmentData.splice(index + 1, 0, {isNew: true});
      } else {
        this.costAdjustmentData.splice(index, 1);
      }
    },
    newCostAdjustmentData() {
      for (let index = 0; index < 5; index++) {
        this.costAdjustmentData.push({productId: null, warehouseId: null, quantity: null});
      }
    },
    rowIsSelect() {
      return !this.isAudited && !this.looked;
    },
    currentChangeEvent({rowIndex}) {
      this.selectRowIndex = rowIndex;
    },
    tableCellClick() {
    },
    loadEditForm(id) {
      this.editConfig = {trigger: 'click', mode: 'row'};
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
                this.costAdjustmentData.push({
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
      this.editConfig = {trigger: 'click', mode: 'row'};
    },
    initAuditsForm() {
      this.editConfig = {};
    },
    approved() {
      if (!this.form.id) {
        MessagePlugin.warning("请先保存单据~");
        return;
      }
      DialogPlugin.confirm({
        title: "审核提示",
        content: `确认审核该订单?`,
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
        title: "反审核提示",
        content: `确认反审核该订单?`,
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
