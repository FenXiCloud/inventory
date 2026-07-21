<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <vxe-toolbar class-name="!size--mini">
        <template #buttons>
          <label class="mr-20px ml-16px" style="font-size: 16px !important">单据日期：</label>
          <DatePicker v-model="form.transferDate" :disabled="isLocked"
                      :option="{ start: accountBook.checkoutDate }"
                      :clearable="false">
          </DatePicker>
          <label class="mr-20px ml-20px" style="font-size: 16px !important">调出仓库：</label>
          <Select class="w-178px" filterable required :datas="warehouseList" keyName="id" titleName="name"
                  v-model="form.fromWarehouseId" placeholder="请选择调出仓库"
                  :disabled="isLocked"
                  @change="changeFromWarehouseId"/>
          <label class="mr-20px ml-20px" style="font-size: 16px !important">调入仓库：</label>
          <Select class="w-178px" filterable required :datas="warehouseList" keyName="id" titleName="name"
                  v-model="form.toWarehouseId" placeholder="请选择调入仓库"
                  :disabled="isLocked"/>
        </template>
        <template #tools>
          <Stamp v-if="isAudited"/>
        </template>
      </vxe-toolbar>
      <vxe-table :edit-rules="validRules" size="mini" ref="xTable" border show-overflow keep-source
                 :edit-config="editConfig" :row-config="{ height: 40, isCurrent: true, isHover: true }"
                 :tooltip-config="tooltipConfig" show-footer :footer-method="footerMethod" stripe
                 :data="inventoryTransferData"
                 @current-change="currentChangeEvent" @cell-click="tableCellClick">
        <vxe-column title="操作" field="seq" width="70" align="center" fixed="left">
          <template #default="{ row, rowIndex }">
            <div v-if="rowIsSelect(rowIndex)">
              <div class="fa fa-plus text-hover mr-5px" @click="adjustRows('insert', rowIndex)"></div>
              <div v-if="inventoryTransferData.length !== 1" class="fa fa-minus text-hover-danger"
                   @click="adjustRows('delete', rowIndex)"></div>
            </div>
            <div v-else>
              {{ rowIndex + 1 }}
            </div>
          </template>
        </vxe-column>
        <vxe-column field="productUrl" title="产品图片" width="250" :cell-render="imgUrlCellRender"></vxe-column>
        <vxe-column field="productCode" title="产品编码" width="240"></vxe-column>
        <vxe-column field="productName" title="产品名称" min-width="350">
          <template #default="scope">
            <div class="h-input-group goodsSelect" v-if="!isLocked">
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
        <vxe-column title="规格型号" field="productSpecification" align="center" width="100"></vxe-column>
        <vxe-column title="产品类别" field="productCategoryName" align="center" width="100"></vxe-column>
        <vxe-column title="单位" field="productUnitName" width="100"/>
        <vxe-column title="总库存" field="warehouseTotal" width="100"/>
        <vxe-column title="仓库库存" field="warehouseQuantity" width="100"/>
        <vxe-column title="数量" field="quantity" width="100">
          <template #default="scope">
            <vxe-input v-if="!isLocked"
                       v-model.number="scope.row.quantity" type="int" min="0" :controls="false">
            </vxe-input>
            <div v-else class="flex">
              <div class="flex1 ml-8px">
                <div>{{ scope.row.quantity }}</div>
              </div>
            </div>
          </template>
        </vxe-column>
      </vxe-table>
      <div class="mt-10px"></div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px w-80px">备注说明：</label>
          <Input :disabled="isLocked" placeholder="请输入备注" type="text" maxlength="150"
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
        <Button v-if="form.id && !isAudited" @click="approved()" :loading="loading"> 审核</Button>
        <Button v-if="isAudited" @click="backApproved()" :loading="loading"> 反审核</Button>
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
    footerMethod({columns, data}) {
      let totalQuantity = 0;
      columns.forEach(column => {
        if (column.property && ['quantity'].includes(column.property)) {
          data.forEach((row) => {
            switch (column.property) {
              case 'quantity': {
                let rd = row[column.property];
                if (rd) {
                  totalQuantity += Number(rd || 0);
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
        ["", "", "", "", "", "", "", "", "", totalQuantity],
      ];
    },
    changeRow({rowIndex}, type, selected) {
      switch (type) {
        case 'product': {
          const selectedProduct = (selected && typeof selected === 'object') ? selected : null;
          let value = selectedProduct
              ? (selectedProduct.id ?? selectedProduct.productId)
              : this.inventoryTransferData[rowIndex].productId;
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
          if (selectedProduct && selectedProduct.name) {
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
        this.inventoryTransferData.splice(index + 1, 0, {isNew: true});
      } else {
        this.inventoryTransferData.splice(index, 1);
      }
    },
    newInventoryTransferData() {
      for (let index = 0; index < 5; index++) {
        this.inventoryTransferData.push({productId: null, quantity: null});
      }
    },
    rowIsSelect() {
      return !this.isLocked;
    },
    currentChangeEvent({rowIndex}) {
      this.selectRowIndex = rowIndex;
    },
    tableCellClick() {
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
      this.editConfig = {trigger: 'click', mode: 'row'};
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
              this.inventoryTransferData.push({
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
              });
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
      this.editConfig = {trigger: 'click', mode: 'row'};
    },
    approved() {
      DialogPlugin.confirm({
        title: "审核提示",
        content: `确认审核该订单?`,
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
        title: "反审核提示",
        content: `确认反审核该订单?`,
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
