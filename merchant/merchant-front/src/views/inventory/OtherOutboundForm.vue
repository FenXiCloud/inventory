<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <vxe-toolbar class-name="!size--mini">
        <template #buttons>
          <label class="mr-20px ml-16px" style="font-size: 16px !important">单据日期：</label>
          <DatePicker v-model="form.orderDate" :disabled="isLocked"
                      :option="{ start: accountBook.checkoutDate }"
                      :clearable="false">
          </DatePicker>
          <label class="mr-20px ml-20px" style="font-size: 16px !important">客户：</label>
          <Select class="w-178px" filterable required :datas="customerList" keyName="id" titleName="name"
                  v-model="form.customerId" placeholder="请选择客户" :disabled="isLocked"/>
          <label class="mr-20px ml-16px" style="font-size: 16px !important">业务类型：</label>
          <Select class="w-178px" filterable required :datas="outboundTypeList" keyName="id" titleName="name"
                  :deletable="false" v-model="form.outboundType" :disabled="isLocked"
                  placeholder="请选择业务类型"/>
        </template>
        <template #tools>
          <Stamp v-if="isAudited"/>
        </template>
      </vxe-toolbar>
      <vxe-table :edit-rules="validRules" size="mini" ref="xTable" border show-overflow keep-source
                 :edit-config="isLocked ? undefined : editConfig" :row-config="{ height: 40, isCurrent: true, isHover: true }"
                 :tooltip-config="tooltipConfig" show-footer :footer-method="footerMethod" stripe
                 :data="otherOutboundData"
                 @current-change="currentChangeEvent" @cell-click="tableCellClick">
        <vxe-column title="操作" field="seq" width="70" align="center" fixed="left">
          <template #default="{ row, rowIndex }">
            <div v-if="!isLocked">
              <div class="fa fa-plus text-hover mr-5px" @click="adjustRows('insert', rowIndex)"></div>
              <div v-if="otherOutboundData.length !== 1" class="fa fa-minus text-hover-danger"
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
        <vxe-column title="规格型号" field="productSpecification" align="center" width="80"></vxe-column>
        <vxe-column title="产品类别" field="productCategoryName" align="center" width="120"></vxe-column>
        <!-- <vxe-column title="品牌" field="productBrand" width="90"></vxe-column> -->
        <!-- <vxe-column title="产地" field="productOrigin" align="center" width="80" /> -->
        <vxe-column title="单位" field="productUnitName" width="90"/>
        <vxe-column title="仓库" field="warehouseName" width="300">
          <template #default="scope">
            <div class="h-input-group goodsSelect" v-if="!isLocked">
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
        <vxe-column title="数量" field="quantity" width="100">
          <template #default="scope">
            <vxe-tooltip v-if="!isLocked" theme="light" :content="scope.row.quantityTips">
              <vxe-input @focus="quantityFocus(scope)" @blur="quantityBlur"
                         v-model.number="scope.row.quantity" type="int" min="0" :controls="false">
              </vxe-input>
            </vxe-tooltip>
            <div v-else class="flex">
              <div class="flex1 ml-8px">
                <div>{{ scope.row.quantity }}</div>
              </div>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="出库单位成本" field="unitPrice" width="100"></vxe-column>
        <vxe-column title="出库成本" field="subtotal" width="100"></vxe-column>
        <!-- <vxe-column title="备注" field="remarks" :edit-render="{}" width="100">
          <template #edit="scope">
            <vxe-input v-model="scope.row.remarks"></vxe-input>
          </template>
        </vxe-column> -->
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
        <Button color="primary" v-if="!isLocked" @click="saveOrder('add')"
                :loading="loading">
          保存并新增
        </Button>
        <Button @click="saveOrder('save')" v-if="!isLocked"
                :loading="loading"> 保存
        </Button>
        <Button @click="doPrint" :loading="loading"> 打印 </Button>
        <Button v-if="form.id && !isLocked" @click="approved()" :loading="loading"> 审核</Button>
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
import OtherOutbound from "@js/api/inventory/OtherOutbound";
import Inventory from "@js/api/inventory/Inventory";
import {mapMutations, mapState} from "vuex";
import Stamp from "../common/Stamp.vue";

export default {
  name: "OtherOutboundForm",
  components: {Stamp},
  props: {
    otherOutboundId: [String, Number],
    stockTakeId: [String, Number],
    importOutbound: Array,
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
      product: null,
      warehouseList: [],
      warehouseId: null,
      form: {
        id: null,
        stockTakeId: null,
        orderDate: manba().format("YYYY-MM-dd"),
        remarks: null,
        customerId: null,
        outboundType: '其他出库',
        totalAmount: 0.00,
        totalQuantity: 0,
        adminName: '',
        // quantityTips: '编辑数量后，查看库存',
        orderStatus: '已保存'
      },
      productData: [],
      otherOutboundData: [],
      selectRowIndex: null,
      increase: true,
      outboundTypeList: [{id: '其他出库', name: '其他出库'}, {id: '盘亏出库', name: '盘亏出库'}],
      customerList: [],
      // 表格校验规则
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
      // 提示配置
      tooltipConfig: {
        showAll: false,
        enterable: false,
      },
      // 编辑配置
      editConfig: {trigger: 'click', mode: 'row'}
    };
  },
  // 待优化使用hook方式调用
  methods: {
    doPrint() {
      const items = (this.otherOutboundData || []).filter(r => r && !r.isNew && r.productId).map(r => {
        const p = (this.productList || []).find(x => (x.productId || x.id) === r.productId) || {};
        return {
          ...r,
          productName: r.productName || p.productName || p.name || p.customName || '',
          quantity: r.quantity ?? r.secondaryQuantity,
          price: r.unitPrice ?? r.secondaryPrice ?? r.price,
          amount: r.subtotal ?? r.amount,
        };
      });
      openPrint('其他出库单', {
        header: {
          ...this.form,
          partner: (this.customerList.find(s => s.id === this.form.customerId) || {}).name || '',
          amount: this.form.finalAmount ?? this.form.totalAmount,
        },
        items,
      });
    },

    // 关闭tab
    ...mapMutations(['closeSelfTab', 'pushTab']),
    //footer合计
    footerMethod({columns, data}) {
      let totalQuantity = 0;
      let totalAmount = 0.00;
      columns.forEach(column => {
        if (column.property && ['quantity', 'subtotal'].includes(column.property)) {
          data.forEach((row) => {
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
    // 设置行数据
    changeRow({rowIndex}, type, selected) {
      switch (type) {
        case 'product': {
          const selectedProduct = (selected && typeof selected === 'object') ? selected : null;
          let value = selectedProduct
              ? (selectedProduct.id ?? selectedProduct.productId)
              : this.otherOutboundData[rowIndex].productId;
          if (value && typeof value === 'object') {
            value = value.id ?? value.productId;
          }
          this.otherOutboundData[rowIndex].productId = value;
          if (this.isEmpty(value)) {
            return;
          }
          const applyProduct = (item) => {
            if (!item) return;
            this.otherOutboundData[rowIndex].productName = item.name;
            this.otherOutboundData[rowIndex].productId = item.id;
            this.otherOutboundData[rowIndex].productCode = item.code;
            this.otherOutboundData[rowIndex].productSpecification = item.specification;
            this.otherOutboundData[rowIndex].productCategoryName = item.productCategoryName;
            this.otherOutboundData[rowIndex].productUnitName = item.unitName;
            this.otherOutboundData[rowIndex].productUnitId = item.unitId;
            if (!this.otherOutboundData[rowIndex].warehouseId) {
              const defaultId = this.resolveDefaultWarehouseId();
              const find = (this.warehouseList || []).find(w => w.id === defaultId);
              if (find) {
                this.otherOutboundData[rowIndex].warehouseId = find.id;
                this.otherOutboundData[rowIndex].warehouseName = find.name;
              }
            }
            this.$forceUpdate();
          };
          if (selectedProduct && selectedProduct.name) {
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
          const selectedWarehouse = (selected && typeof selected === 'object') ? selected : null;
          let value = selectedWarehouse ? selectedWarehouse.id : this.otherOutboundData[rowIndex].warehouseId;
          if (value && typeof value === 'object') {
            value = value.id;
          }
          this.otherOutboundData[rowIndex].warehouseId = value;
          if (this.isEmpty(value)) {
            return;
          }
          if (selectedWarehouse && selectedWarehouse.name) {
            this.otherOutboundData[rowIndex].warehouseName = selectedWarehouse.name;
            this.otherOutboundData[rowIndex].warehouseId = selectedWarehouse.id;
            this.$forceUpdate();
          } else {
            Warehouse.list({id: value}).then(res => {
              const {success, data} = res;
              if (success) {
                const item = data?.[0];
                if (!item) return;
                this.otherOutboundData[rowIndex].warehouseName = item.name;
                this.otherOutboundData[rowIndex].warehouseId = item.id;
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
    //保存新增、保存
    saveOrder(type) {
      const filterOtherOutboundData = this.otherOutboundData.filter(item => !this.isEmpty(item.productId) || !this.isEmpty(item.warehouseId) || !this.isEmpty(item.quantity) || !this.isEmpty(item.remarks));
      if (!this.validatorsForm(filterOtherOutboundData)) {
        return;
      }
      const params = this.getSaveOrderParams(filterOtherOutboundData, type);
      LoadingPlugin(true);
      OtherOutbound.save(params)
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
    closeWindow() {
      this.closeSelfTab(this.index);
      this.pushTab({
        keepAlive: false,
        key: "OtherOutboundList",
        title: "其他出库单"
      });
    },
    //校验提交表单
    validatorsForm(filterOtherOutboundData) {
      if (filterOtherOutboundData.length === 0) {
        MessagePlugin.error("请填写操作数据~");
        return false;
      }
      if (filterOtherOutboundData.some((c) => this.isEmpty(c.productId))) {
        MessagePlugin.error("请选择产品~");
        return false;
      }
      if (filterOtherOutboundData.some((c) => this.isEmpty(c.warehouseId))) {
        MessagePlugin.error("请选择仓库~");
        return false;
      }
      if (filterOtherOutboundData.some((c) => this.isEmpty(c.quantity) || Number(c.quantity) === 0)) {
        MessagePlugin.error("请填写数量~");
        return false;
      }
      if (filterOtherOutboundData.some((c) => c.warehouseQuantity - c.quantity < 0)) {
        MessagePlugin.error("出库数量不能大于仓库库存数量~");
        return false;
      }
      return true;
    },
    //获取保存新增、保存方法提交数据
    getSaveOrderParams(filterOtherOutboundData) {
      const otherOutboundItems = [];
      const otherOutbound = {
        customerId: this.form.customerId,
        outboundType: this.form.outboundType,
        inboundDate: this.form.orderDate,
        remarks: this.form.remarks
      };
      otherOutbound.id = this.form.id;
      if (this.form.stockTakeId !== null) {
        otherOutbound.stockTakeId = this.form.stockTakeId;
      } else {
        otherOutbound.stockTakeId = this.stockTakeId;
      }
      filterOtherOutboundData.forEach(item => {
        otherOutboundItems.push({
          productId: item.productId,
          baseUnitId: item.productUnitId,
          quantity: item.quantity,
          warehouseId: item.warehouseId,
        });
      });
      return {
        otherOutbound: otherOutbound,
        otherOutboundItems: otherOutboundItems
      };
    },
    //清除Form
    clearForm() {
      this.form = {
        id: null,
        stockTakeId: null,
        orderDate: manba().format("YYYY-MM-dd"),
        remarks: null,
        customerId: null,
        outboundType: '其他出库',
        totalAmount: 0.00,
        totalQuantity: 0,
        adminName: this.user?.admin?.name || '',
        orderStatus: '已保存',
        quantityTips: ''
      };
      this.otherOutboundData = [];
      this.newOtherOutboundData();
    },

    //添加行或减少行
    adjustRows(type, index) {
      if (type === "insert") {
        this.otherOutboundData.splice(index + 1, 0, {isNew: true});
      } else {
        this.otherOutboundData.splice(index, 1);
      }
    },
    //新增默认初始化行数
    newOtherOutboundData() {
      for (let index = 0; index < 5; index++) {
        this.otherOutboundData.push({productId: null, warehouseId: null, quantity: null});
      }
    },
    //行选中事件
    currentChangeEvent({rowIndex}) {
      this.selectRowIndex = rowIndex;
    },
    //表格行点击事件
    tableCellClick({rowIndex}) {
      console.info(rowIndex);
    },
    //数量焦点获取库存数量到titile-prefix中
    quantityFocus({rowIndex}) {
      const otherOutboundItem = this.otherOutboundData[rowIndex];
      const {productId, warehouseId} = otherOutboundItem;
      if (this.isEmpty(productId) || this.isEmpty(warehouseId)) {
        return;
      }
      // 获取产品库存进行提示
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
          this.otherOutboundData[rowIndex].quantityTips = `总库存：${totalQuantity}\r\n仓库库存：${quantity}`;
          this.otherOutboundData[rowIndex].warehouseQuantity = quantity;
        } else {
          this.otherOutboundData[rowIndex].quantityTips = `总库存：0\r\n仓库库存：0`;
          this.otherOutboundData[rowIndex].warehouseQuantity = 0;
        }
      });
    },
    //失去焦点
    quantityBlur() {
      // this.form.quantityTips = "编辑数量后，查看库存";
    },
    //加载编辑表单
    loadEditForm(id) {
      this.editConfig = {trigger: 'click', mode: 'row'};
      this.increase = false;
      this.otherOutboundData = [];
      OtherOutbound.load(this.otherOutboundId || id).then(
          ({data}) => {
            if (data && data.length > 0) {
              this.form.id = data[0].id;
              this.form.customerId = data[0].customerId;
              this.form.remarks = data[0].remarks;
              this.form.outboundType = data[0].outboundType;
              this.form.orderDate = data[0].inboundDate;
              this.form.stockTakeId = data[0].stockTakeId;
              this.form.adminName = data[0].adminName;
              this.form.orderStatus = data[0].orderStatus;
              let totalAmount = 0;
              let totalQuantity = 0;
              data.forEach(item => {
                totalAmount += parseFloat(item.subtotal);
                totalQuantity += parseInt(item.quantity);
                this.otherOutboundData.push({
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
                });
              });
              this.form.totalAmount = totalAmount;
              this.form.totalQuantity = totalQuantity;
            }
          }
      );
    },
    //加载字典
    loadDict(callback) {
      Promise.all([Product.select(), Warehouse.select(), Customer.select()])
          .then((results) => {
            this.productList = results[0].data || [];
            this.productList.forEach(item => {
              item.customName = `${item.code}--${item.name}`;
            });
            this.warehouseList = results[1].data || [];
            this.customerList = results[2].data || [];
            this.warehouseId = this.warehouseList.find(
                (val) => val.systemDefault || val.isDefault
            )?.id;
            if (callback) {
              callback();
            }
          })
          .finally(() => LoadingPlugin(false));
    },
    //初始化表单
    initIncreaseForm() {
      this.increase = true;
      this.newOtherOutboundData();
      this.form.adminName = this.user.admin.name;
      this.form.id = null;
      this.editConfig = {trigger: 'click', mode: 'row'};
      if (this.stockTakeId) {
        this.form.outboundType = "盘亏出库";
        this.otherOutboundData = JSON.parse(JSON.stringify(this.importOutbound));
      }
    },
    approved() {
      DialogPlugin.confirm({
        title: "审核提示",
        content: `确认审核该订单?`,
        onConfirm: () => {
          return OtherOutbound.approved('已审核', [this.form.id]).then(() => {
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
          return OtherOutbound.approved('已保存', [this.form.id]).then(() => {
            MessagePlugin.success("操作成功~");
            this.closeWindow();
          });
        }
      });
    }
  },
  created() {
    LoadingPlugin(true);
    this.loadDict(() => {
      if (this.otherOutboundId) {
        this.loadEditForm();
        return;
      }
      this.initIncreaseForm();
    });
  },
};
</script>
