<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <vxe-toolbar class-name="!size--mini">
        <template #buttons>
          <label class="mr-20px ml-16px" style="font-size: 16px !important">盘点日期：</label>
          <label style="font-size: 15px !important">{{ form.checkDate }}</label>
          <label class="mr-20px ml-20px" style="font-size: 16px !important">仓库：</label>
          <t-select class="w-178px" filterable multiple :options="warehouseList"
                    :keys="{ value: 'id', label: 'name' }"
                    v-model="form.warehouseIds" :placeholder="warehousePlaceholder"
                    :disabled="isLocked || !!form.id" clearable
                    @change="changeWarehouseId"/>
          <label class="mr-20px ml-20px" style="font-size: 16px !important">产品：</label>
          <t-select class="w-178px mr-20px" filterable :options="productList"
                    :keys="{ value: 'id', label: 'customName' }"
                    v-model="form.productId" placeholder="请选择产品" :disabled="isLocked || !!form.id" clearable/>
        </template>
        <template #tools>
          <Stamp v-if="isAudited"/>
          <t-input v-if="!form.id" v-model.trim="form.filter"
                   clearable class="w-360px ml-8px"
                   placeholder="请输入产品编号/产品名称" @enter="doSearch">
            <template #suffixIcon>
              <t-icon name="search" style="cursor:pointer" @click="doSearch"/>
            </template>
          </t-input>
        </template>
      </vxe-toolbar>
      <vxe-table :edit-rules="validRules" size="mini" ref="xTable" border show-overflow keep-source
                 :edit-config="isLocked ? undefined : editConfig" :row-config="{ height: 40, isCurrent: true, isHover: true }"
                 :tooltip-config="tooltipConfig" show-footer :footer-method="footerMethod" stripe
                 :data="stockTakeData"
                 @current-change="currentChangeEvent" @cell-click="tableCellClick">
        <vxe-column title="序号" type="seq" width="70" align="center" fixed="left"/>
        <vxe-column field="warehouseName" title="仓库" width="150"></vxe-column>
        <vxe-column field="productUrl" title="产品图片" width="150" :cell-render="imgUrlCellRender"></vxe-column>
        <vxe-column field="productCode" title="产品编码" width="160"></vxe-column>
        <vxe-column field="productName" title="产品名称" min-width="250">
        </vxe-column>
        <vxe-column title="规格型号" field="productSpecification" align="center" width="120"></vxe-column>
        <vxe-column title="产品类别" field="productCategoryName" align="center" width="110"></vxe-column>
        <vxe-column title="单位" field="productUnitName" width="100"/>
        <vxe-column title="系统库存" field="systemQuantity" width="100"/>
        <vxe-column title="盘点库存" field="actualQuantity" width="100">
          <template #default="scope">
            <vxe-input v-if="!isLocked" @input="quantityInput(scope)"
                       v-model.number="scope.row.actualQuantity" type="int" min="0" :controls="false">
            </vxe-input>
            <div v-else class="flex">
              <div class="flex1 ml-8px">
                <div>{{ scope.row.actualQuantity }}</div>
              </div>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="盘点亏盈" field="deficient" width="100">
          <template #default="scope">
            <div class="flex">
              <div class="flex1 ml-8px">
                <div style="color: green" v-if="scope.row.deficient > 0">{{ scope.row.deficient }}</div>
                <div style="color: red" v-else>{{ scope.row.deficient }}</div>
              </div>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="备注" field="differenceReason" width="100">
          <template #default="scope">
            <vxe-input v-if="!isLocked" v-model="scope.row.differenceReason"></vxe-input>
            <div v-else class="flex">
              <div class="flex1 ml-8px">
                <div>{{ scope.row.differenceReason }}</div>
              </div>
            </div>
          </template>
        </vxe-column>
      </vxe-table>
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
        <t-button v-if="!isAudited && !looked" theme="primary" @click="saveOrder('add')" :loading="loading">
          保存并新增
        </t-button>
        <t-button theme="primary" :disabled="form.generatedDisabled" @click="openGeneratedModal" :loading="loading">
          生成盘点单据
        </t-button>
        <t-button v-if="!isAudited && !looked" @click="saveOrder('save')" :loading="loading">保存</t-button>
        <t-button @click="doPrint" :loading="loading"> 打印 </t-button>
        <t-button v-if="form.id && !isAudited && !looked" @click="approved()" :loading="loading"> 审核</t-button>
        <t-button v-if="isAudited && !looked" @click="backApproved()" :loading="loading"> 反审核</t-button>
      </div>
    </div>
    <t-dialog v-model:visible="opened" header="生成盘点单据" :footer="false" width="360px">
      <div class="mt-10px">
        <div class="flex justify-center items-center flex-column" v-if="inbounds.length > 0">
          <t-button theme="primary" @click="generatedInbounds" :loading="loading">盘盈单</t-button>
        </div>
        <div class="mt-10px flex justify-center items-center flex-column" v-if="outbounds.length > 0">
          <t-button theme="primary" @click="generatedOutbounds" :loading="loading">盘亏单</t-button>
        </div>
      </div>
    </t-dialog>
  </div>
</template>
<script>
import {LoadingPlugin, MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';
import {openPrint} from '@common/print';
import manba from "manba";
import Product from "@js/api/basic/Product";
import Warehouse from "@js/api/basic/Warehouse";
import Inventory from "@js/api/inventory/Inventory";
import {mapMutations, mapState} from "vuex";
import StockTake from "@js/api/inventory/StockTake";
import Stamp from "../common/Stamp.vue";

export default {
  name: "StockTakeForm",
  components: {Stamp},
  props: {
    stockTakeId: [String, Number],
    type: String,
    index: Number,
    status: String,
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
  },
  data() {
    return {
      warehousePlaceholder: '请选择仓库',
      refresh: false,
      loading: false,
      productList: [],
      product: null,
      warehouseList: [],
      warehouseId: null,
      opened: false,
      form: {
        id: null,
        checkDate: manba().format("YYYY-MM-dd"),
        remarks: null,
        warehouseId: null,
        warehouseIds: [],
        productId: null,
        filter: null,
        totalAmount: 0.00,
        totalQuantity: 0,
        adminName: '',
        orderStatus: '已保存',
        generatedDisabled: true
      },
      productData: [],
      stockTakeData: [],
      originalStockTakeData: [],
      selectRowIndex: null,
      increase: true,
      validRules: {
        productName: [
          {required: true, message: '请选择产品名称'},
        ],
        warehouseName: [
          {required: true, message: '请选择仓库'},
        ],
        actualQuantity: [
          {required: true, message: '请填写盘点库存'},
        ]
      },
      tooltipConfig: {
        showAll: false,
        enterable: false,
      },
      editConfig: {trigger: 'click', mode: 'row'},
      outbounds: [],
      inbounds: []
    };
  },
  methods: {
    doPrint() {
      const items = (this.stockTakeData || []).filter(r => r && !r.isNew && r.productId).map(r => {
        const p = (this.productList || []).find(x => (x.productId || x.id) === r.productId) || {};
        return {
          ...r,
          productName: r.productName || p.productName || p.name || p.customName || '',
          quantity: r.actualQuantity ?? r.quantity ?? r.secondaryQuantity,
          price: r.unitPrice ?? r.secondaryPrice ?? r.price,
          amount: r.subtotal ?? r.amount,
        };
      });
      openPrint('盘点单', {
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
      (columns || []).forEach(column => {
        if (column.property && ['actualQuantity'].includes(column.property)) {
          (data || []).forEach((row) => {
            if (column.property === 'actualQuantity') {
              totalQuantity += Number(row?.[column.property] || 0);
            }
          });
        }
      });
      return [
        ["", "", "", "", "", "", "", "", "", totalQuantity],
      ];
    },
    isEmpty(value) {
      return (value !== 0 && !value) || value === '';
    },
    saveOrder(type) {
      try {
        const filterStockTakeData = (this.stockTakeData || []).filter(item =>
            !this.isEmpty(item.productId)
            || !this.isEmpty(item.warehouseId)
            || !this.isEmpty(item.actualQuantity)
            || !this.isEmpty(item.differenceReason)
        );
        this.validatorsForm(filterStockTakeData);
        const params = this.getSaveOrderParams(filterStockTakeData);
        StockTake.save(params)
            .then(({success}) => {
              if (success) {
                MessagePlugin.success("保存成功~");
                this.clearForm();
                if (type === 'save') {
                  this.closeWindow();
                } else {
                  this.initIncreaseForm();
                }
              }
            })
            .finally(() => LoadingPlugin(false));
      } catch (e) {
        LoadingPlugin(false);
        MessagePlugin.error(e?.message || "保存失败~");
      }
    },
    openGeneratedModal() {
      this.opened = true;
    },
    generatedInbounds() {
      this.pushTab({
        key: 'OtherInboundForm',
        title: '新增其他入库单',
        params: {type: "add", otherInboundId: null, stockTakeId: this.form.id, importInbound: this.inbounds}
      });
      this.opened = false;
    },
    generatedOutbounds() {
      this.pushTab({
        key: 'OtherOutboundForm',
        title: '新增其他出库单',
        params: {type: "add", otherOutboundId: null, stockTakeId: this.form.id, importOutbound: this.outbounds}
      });
      this.opened = false;
    },
    validatorsForm(filterStockTakeData) {
      if (!filterStockTakeData || filterStockTakeData.length === 0) {
        throw new Error("请填写操作数据~")
      }
      if (!(this.form.warehouseIds && this.form.warehouseIds.length > 0)) {
        throw new Error("请选择仓库~")
      }
      LoadingPlugin(true);
      const emptyQty = filterStockTakeData.filter((c) => this.isEmpty(c.actualQuantity));
      if (emptyQty.length > 0) {
        LoadingPlugin(false);
        throw new Error("请填写盘点库存~")
      }
    },
    getSaveOrderParams(filterStockTakeData) {
      const stockTakeItems = [];
      const stockTake = {
        checkDate: this.form.checkDate,
        remarks: this.form.remarks,
        warehouseId: this.form.warehouseId,
        warehouseIds: (this.form.warehouseIds || []).join(","),
      };
      stockTake.id = this.form.id;
      (filterStockTakeData || []).forEach(item => {
        stockTakeItems.push({
          actualQuantity: item.actualQuantity,
          systemQuantity: item.systemQuantity,
          productId: item.productId,
          warehouseId: item.warehouseId,
          differenceReason: item.differenceReason,
        });
      });
      return {
        stockTake: stockTake,
        stockTakeItems: stockTakeItems
      };
    },
    clearForm() {
      this.form = {
        id: null,
        checkDate: manba().format("YYYY-MM-dd"),
        remarks: null,
        warehouseId: null,
        warehouseIds: [],
        productId: null,
        filter: null,
        totalAmount: 0.00,
        totalQuantity: 0,
        adminName: this.user?.admin?.name || '',
        orderStatus: '已保存',
        generatedDisabled: true
      };
      this.stockTakeData = [];
      this.originalStockTakeData = [];
      this.inbounds = [];
      this.outbounds = [];
      this.opened = false;
    },
    adjustRows(type, index) {
      if (type === "insert") {
        this.stockTakeData.splice(index + 1, 0, {isNew: true});
      } else {
        this.stockTakeData.splice(index, 1);
      }
    },
    newStockTakeData() {
      Inventory.products({
        warehouseId: this.form.warehouseId,
        warehouseIds: (this.form.warehouseIds || []).join(","),
        productId: this.form.productId,
        filter: this.form.filter
      }).then((res) => {
        this.stockTakeData = res?.data || [];
      });
    },
    rowIsSelect() {
      return !this.isLocked;
    },
    currentChangeEvent({rowIndex}) {
      this.selectRowIndex = rowIndex;
    },
    tableCellClick({rowIndex}) {
    },
    quantityInput({rowIndex}) {
      const row = this.stockTakeData?.[rowIndex];
      if (!row) return;
      let actualQuantity = Number(row.actualQuantity);
      const systemQuantity = Number(row.systemQuantity) || 0;
      if (Number.isNaN(actualQuantity) || actualQuantity < 0) {
        row.actualQuantity = 0;
        actualQuantity = 0;
      }
      row.deficient = actualQuantity - systemQuantity;
    },
    changeWarehouseId() {
      if (this.form.warehouseIds && this.form.warehouseIds.length > 0) {
        this.warehousePlaceholder = "";
      } else {
        this.warehousePlaceholder = "请选择仓库";
      }
      this.doSearch();
    },
    loadEditForm(id) {
      this.increase = false;
      this.stockTakeData = [];
      StockTake.load(this.stockTakeId || id).then(
          ({data}) => {
            if (data && data.length > 0) {
              this.form.id = data[0].id;
              this.form.warehouseId = data[0].mainWarehouseId;
              if (data[0].mainWarehouseIds) {
                this.form.warehouseIds = String(data[0].mainWarehouseIds).split(',').filter(Boolean);
                this.warehousePlaceholder = this.form.warehouseIds.length ? "" : "请选择仓库";
              }
              this.form.remarks = data[0].remarks;
              this.form.checkDate = data[0].checkDate;
              this.form.adminName = data[0].adminName;
              this.form.orderStatus = data[0].orderStatus;
              let totalQuantity = 0;
              data.forEach(item => {
                totalQuantity += Number(item.actualQuantity) || 0;
                this.stockTakeData.push({
                  productUrl: '',
                  productCode: item.productCode,
                  productName: item.productName,
                  productId: item.productId,
                  productSpecification: item.productSpecification,
                  productCategoryName: item.productCategoryName,
                  productUnitId: item.productUnitId,
                  productUnitName: item.productUnitName,
                  systemQuantity: item.systemQuantity,
                  actualQuantity: item.actualQuantity,
                  warehouseName: item.warehouseName,
                  warehouseId: item.warehouseId,
                  deficient: item.deficient,
                  differenceReason: item.differenceReason
                });
              });
              this.originalStockTakeData = JSON.parse(JSON.stringify(this.stockTakeData));
              this.form.totalQuantity = totalQuantity;
            }
          }
      );
    },
    loadDict(callback) {
      LoadingPlugin(true);
      Promise.all([Product.select(), Warehouse.select()])
          .then((results) => {
            this.productList = results[0]?.data || [];
            this.productList.forEach(item => {
              item.customName = `${item.code}--${item.name}`;
            });
            this.warehouseList = results[1]?.data || [];
            if (callback) {
              callback();
            }
          })
          .finally(() => LoadingPlugin(false));
    },
    initIncreaseForm() {
      this.increase = true;
      this.form.adminName = this.user?.admin?.name || '';
      this.form.id = null;
      this.form.orderStatus = '已保存';
      this.newStockTakeData();
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
          return StockTake.approved('已审核', [this.form.id]).then(() => {
            MessagePlugin.success("操作成功~");
            this.loadEditForm(this.form.id);
            this.getInventoryList();
          });
        }
      });
    },
    backApproved() {
      if (!this.form.id) {
        MessagePlugin.warning("请先保存单据~");
        return;
      }
      DialogPlugin.confirm({
        header: "反审核提示",
        body: `确认反审核该订单?`,
        onConfirm: () => {
          return StockTake.approved('已保存', [this.form.id]).then(() => {
            MessagePlugin.success("操作成功~");
            this.inbounds = [];
            this.outbounds = [];
            this.form.generatedDisabled = true;
            this.loadEditForm(this.form.id);
          });
        }
      });
    },
    closeWindow() {
      this.closeSelfTab(this.index);
      this.pushTab({
        keepAlive: false,
        key: "StockTakeList",
        title: "盘点单"
      });
    },
    doSearch() {
      if (this.stockTakeId || this.form.id) {
        const warehouseId = this.form.warehouseId;
        const warehouseIds = this.form.warehouseIds || [];
        const productId = this.form.productId;
        const filter = this.form.filter;
        const newStockTakeData = [];
        (this.originalStockTakeData || []).forEach(item => {
          let isDone = true;
          if (warehouseId && warehouseId !== item.warehouseId) {
            isDone = false;
          }
          if (productId && productId !== item.productId) {
            isDone = false;
          }
          if (warehouseIds.length > 0 && !warehouseIds.includes(item.warehouseId) && !warehouseIds.includes(String(item.warehouseId))) {
            isDone = false;
          }
          if (!this.isEmpty(filter) && (String(item.productCode || '').indexOf(filter) === -1 && String(item.productName || '').indexOf(filter) === -1)) {
            isDone = false;
          }
          if (isDone) {
            newStockTakeData.push(item);
          }
        });
        this.stockTakeData = newStockTakeData;
      } else {
        this.newStockTakeData();
      }
    },
    getInventoryList() {
      const id = this.stockTakeId || this.form.id;
      if (!id) return;
      StockTake.export(id).then(res => {
        const data = res?.data;
        if (data) {
          const {outbounds, inbounds} = data;
          this.outbounds = outbounds || [];
          this.inbounds = inbounds || [];
          this.form.generatedDisabled = !(this.inbounds.length > 0 || this.outbounds.length > 0);
        } else {
          this.outbounds = [];
          this.inbounds = [];
          this.form.generatedDisabled = true;
        }
      });
    }
  },
  created() {
    LoadingPlugin(true);
    this.loadDict(() => {
      if (this.stockTakeId) {
        this.loadEditForm();
        if (this.status === "已审核" || this.isAudited) {
          this.getInventoryList();
        }
        return;
      }
      this.initIncreaseForm();
    });
  },
};
</script>
