<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <vxe-toolbar class-name="!size--mini">
        <template #buttons>
          <label class="mr-20px ml-16px" style="font-size: 16px !important">单据日期：</label>
          <DatePicker v-model="form.transferDate" :disabled="looked"
                      :option="{ start: accountBook.checkoutDate }"
                      :clearable="false">
          </DatePicker>
          <label class="mr-20px ml-20px" style="font-size: 16px !important">调出仓库：</label>
          <Select class="w-178px" filterable required :datas="warehouseList" keyName="id" titleName="name"
                  v-model="form.fromWarehouseId" placeholder="请选择调出仓库"
                  :disabled="looked"
                  @change="changeFromWarehouseId"/>
          <label class="mr-20px ml-20px" style="font-size: 16px !important">调入仓库：</label>
          <Select class="w-178px" filterable required :datas="warehouseList" keyName="id" titleName="name"
                  v-model="form.toWarehouseId" placeholder="请选择调入仓库"
                  :disabled="looked"/>
        </template>
        <template #tools>
          <Stamp v-if="approved"/>
        </template>
      </vxe-toolbar>
      <vxe-table :edit-rules="validRules" size="mini" ref="xTable" border="border" show-overflow keep-source
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
        <vxe-column field="productUrl" title="商品图片" width="250" :cell-render="imgUrlCellRender"></vxe-column>
        <vxe-column field="productCode" title="商品编码" width="240"></vxe-column>
        <vxe-column field="productName" title="商品名称" min-width="350">
          <template #default="scope">
            <div class="h-input-group goodsSelect" v-if="!looked">
              <Select :deletable="false" ref="ms" v-model="scope.row.productId" :datas="productList" filterable
                      placeholder="输入编码/名称" keyName="id" titleName="name" @change="changeRow(scope, 'product')">
                <template v-slot:item="{ item }">
                  <div>{{ item.name }}</div>
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
        <vxe-column title="商品类别" field="productCategoryName" align="center" width="100"></vxe-column>
        <!-- <vxe-column title="品牌" field="productBrand" width="90"></vxe-column> -->
        <!-- <vxe-column title="产地" field="productOrigin" align="center" width="80" /> -->
        <vxe-column title="单位" field="productUnitName" width="100"/>
        <vxe-column title="总库存" field="warehouseTotal" width="100"/>
        <vxe-column title="仓库库存" field="warehouseQuantity" width="100"/>
        <vxe-column title="数量" field="quantity" width="100">
          <template #default="scope">
            <vxe-input v-if="!looked"
                       v-model.number="scope.row.quantity" type="int" min="0" :controls="false">
            </vxe-input>
            <div v-else class="flex">
              <div class="flex1 ml-8px">
                <div>{{ scope.row.quantity }}</div>
              </div>
            </div>
          </template>
        </vxe-column>
        <!-- <vxe-column title="备注" field="remarks" :edit-render="{}" width="100">
          <template #edit="scope">
            <vxe-input v-model="scope.row.remarks"></vxe-input>
          </template>
        </vxe-column> -->
      </vxe-table>
      <div class="mt-10px"></div>
      <div class="filler-panel">
        <div class="filler-item" style="flex: 1; margin: 5px 0 !important">
          <label class="mr-16px w-80px">备注说明：</label>
          <Input :disabled="looked" placeholder="请输入备注" type="text" maxlength="150"
                 style="width: 80%"
                 v-model="form.remarks"/>
          <label class="ml-16px w-180px">制单人：{{ form.adminName }}</label>
        </div>
      </div>
    </div>
    <div class="modal-column-between bg-white-color border">
      <Button @click="closeWindow" :loading="loading"> 取消</Button>
      <div>
        <Button v-if="!approved && !looked" color="primary" @click="saveOrder('increase')" :loading="loading">
          保存并新增
        </Button>
        <Button v-if="!approved && !looked" @click="saveOrder" :loading="loading"> 保存</Button>
        <!-- 当状态为已审核时不显示,审核后订单上显示已审核图片 -->
        <Button v-if="!approved && !looked" @click="auditForm('AUDITS')" :loading="loading"> 审核</Button>
        <!-- 仅当状态为审核时显示 -->
        <Button v-if="approved && !looked" @click="auditForm('ANTI_AUDIT')" :loading="loading"> 反审核</Button>
      </div>
    </div>
  </div>
</template>
<script>
import {confirm, loading, message} from "heyui.ext";
import manba from "manba";
import {CopyObj} from "@common/utils";
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
    auditOperate() {
      return ['audits', 'antiAudits'].includes(this.type);
    },
    approved() {
      return ['已审核'].includes(this.form.orderStatus);
    },
    looked() {
      return ['look'].includes(this.type);
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
      // 表格校验规则
      validRules: {
        productName: [
          {required: true, message: '请选择商品名称'},
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
    // 关闭tab
    ...mapMutations(['closeSelfTab', 'pushTab']),
    //footer合计
    footerMethod({columns, data}) {
      let totalQuantity = 0;
      let totalAmount = 0.00;
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
    // 设置行数据
    changeRow({rowIndex}, type) {
      switch (type) {
        case 'product': {
          const value = this.inventoryTransferData[rowIndex].productId;
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
          // 根据id获取商品信息更新
          Product.list({id: value}).then(res => {
            const {success, data} = res;
            if (success) {
              const item = data.results[0];
              console.info("Product info:", item);
              this.inventoryTransferData[rowIndex].productName = item.name;
              this.inventoryTransferData[rowIndex].productId = item.id;
              this.inventoryTransferData[rowIndex].productCode = item.code;
              this.inventoryTransferData[rowIndex].productSpecification = item.specification;
              this.inventoryTransferData[rowIndex].productCategoryName = item.productCategoryName;
              this.inventoryTransferData[rowIndex].productUnitName = item.unitName;
              this.inventoryTransferData[rowIndex].productUnitId = item.unitId;
              this.$forceUpdate();
            }
          });
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
    //保存新增、保存
    saveOrder(type) {
      const filterInventoryTransferData = this.inventoryTransferData.filter(item => !this.isEmpty(item.productId) || !this.isEmpty(item.warehouseId) || !this.isEmpty(item.quantity) || !this.isEmpty(item.remarks));
      // 校验
      this.validatorsForm(filterInventoryTransferData);
      // 操作对象
      const params = this.getSaveOrderParams(filterInventoryTransferData, type);
      InventoryTransfer.save(params)
          .then(({success, data}) => {
            console.info("success", success);
            if (success) {
              message("保存成功~");
              setTimeout(() => {
                if (type === "increase") {
                  this.clearForm();
                  // 刷新列表为编辑
                  this.closeWindow();
                  this.pushTab({
                    key: 'InventoryTransferForm',
                    title: '编辑调拨单',
                    params: {type: "edit", inventoryTransferId: data.id}
                  });
                } else {
                  // 刷新列表为编辑
                  this.closeWindow();
                  this.pushTab({
                    key: 'InventoryTransferForm',
                    title: '编辑调拨单',
                    params: {type: "edit", inventoryTransferId: data.id}
                  });
                  this.$emit("update:inventoryTransferId", data.id);
                  this.$emit("update:type", "edit");
                  this.loadEditForm(data.id);
                }
              }, 300);
            }
          })
          .finally(() => loading.close());
    },
    //校验提交表单
    validatorsForm(filterInventoryTransferData) {
      if (filterInventoryTransferData.length === 0) {
        throw new Error("请填写操作数据~")
      }
      loading("保存中....");
      let productData = filterInventoryTransferData.filter((c) => this.isEmpty(c.productId));
      console.info("productData:", productData)
      if (productData.length > 0) {
        loading.close();
        throw new Error("请选择商品~")
      }
      if (this.isEmpty(this.form.fromWarehouseId)) {
        loading.close();
        throw new Error("请选择调出仓库~")
      }
      if (this.isEmpty(this.form.toWarehouseId)) {
        loading.close();
        throw new Error("请选择调入仓库~")
      }
      if (this.form.fromWarehouseId === this.form.toWarehouseId) {
        loading.close();
        throw new Error("调出仓库和调入仓库不能是同一个～");
      }
      let quantity = filterInventoryTransferData.filter((c) => this.isEmpty(c.quantity) || Number(c.quantity) === 0);
      if (quantity.length > 0) {
        loading.close();
        throw new Error("请填写数量~")
      }
      // 校验调出仓库库存是否足够
      let hasQuantity = true;
      filterInventoryTransferData.forEach(item => {
        const {warehouseQuantity, quantity} = item;
        if (Number(warehouseQuantity) < Number(quantity)) {
          hasQuantity = false;
        }
      });
      if (!hasQuantity) {
        loading.close();
        throw new Error("调出数量不能大于当前仓库库存~")
      }
    },
    //获取保存新增、保存方法提交数据
    getSaveOrderParams(filterInventoryTransferData, type) {
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
    //清除Form
    clearForm() {
      this.form = {
        id: null,
        orderDate: manba().format("YYYY-MM-dd"),
        remarks: null,
        toWarehouseId: null,
        fromWarehouseId: null,
        totalAmount: 0.00,
        totalQuantity: 0,
        quantityTips: ''
      };
      this.inventoryTransferData = [];
      this.newInventoryTransferData();
    },

    //添加行或减少行
    adjustRows(type, index) {
      if (type === "insert") {
        this.inventoryTransferData.splice(index + 1, 0, {isNew: true});
      } else {
        this.inventoryTransferData.splice(index, 1);
      }
    },
    //新增默认初始化行数
    newInventoryTransferData() {
      for (let index = 0; index < 5; index++) {
        this.inventoryTransferData.push({productId: null, quantity: null});
      }
    },
    //行是否选中
    rowIsSelect(rowIndex) {
      return !this.looked;
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
      const inventoryTransferItem = this.inventoryTransferData[rowIndex];
      const {productId} = inventoryTransferItem;
      const warehouseId = this.form.fromWarehouseId;
      if (this.isEmpty(productId) || this.isEmpty(warehouseId)) {
        return;
      }
      // 获取商品库存进行提示
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
    // 更改调出仓库
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
        // 获取商品库存进行提示
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
    //失去焦点
    quantityBlur(type, {rowIndex}) {
      console.info("quantityBlur:", type, rowIndex);
    },
    //加载编辑表单
    loadEditForm(id) {
      this.editConfig = {trigger: 'click', mode: 'row'};
      this.increase = false;
      this.inventoryTransferData = [];
      InventoryTransfer.load(this.inventoryTransferId || id).then(
          ({data}) => {
            if (data && data.length > 0) {
              this.form.id = data[0].id;
              this.form.fromWarehouseId = data[0].fromWarehouseId;
              this.form.toWarehouseId = data[0].toWarehouseId;
              this.form.remarks = data[0].remarks;
              this.form.transferDate = data[0].transferDate;
              this.form.adminName = data[0].adminName;
              this.form.orderStatus = data[0].orderStatus;
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
          }
      );
    },
    //加载字典
    loadDict(callback) {
      Promise.all([Product.select(), Warehouse.select()])
          .then((results) => {
            this.productList = results[0].data || [];
            // 调整productList的name值
            this.productList.forEach(item => {
              item.name = `${item.code}--${item.name}`;
            });
            this.warehouseList = results[1].data || [];
            if (this.warehouseList != null) {
              this.form.fromWarehouseId = this.warehouseList.find(
                  (val) => val.systemDefault
              )?.id;
            }
            if (callback) {
              callback();
            }
          })
          .finally(() => loading.close());
    },
    //初始化表单
    initIncreaseForm() {
      this.increase = true;
      this.newInventoryTransferData();
      this.form.adminName = this.user.admin.name;
      this.form.id = null;
      this.editConfig = {trigger: 'click', mode: 'row'};
    },
    //初始化审核表单
    initAuditsForm() {
      //表格不可编辑
      this.editConfig = {};
    },
    //审核表单
    async auditForm(operateType) {
      const type = this.type;
      let {id} = this.form;
      if (!id) {
        const filterInventoryTransferData = this.inventoryTransferData.filter(item => !this.isEmpty(item.productId) || !this.isEmpty(item.warehouseId) || !this.isEmpty(item.quantity) || !this.isEmpty(item.remarks));
        // 校验
        this.validatorsForm(filterInventoryTransferData);
        // 操作对象
        const params = this.getSaveOrderParams(filterInventoryTransferData, type);
        const res = await InventoryTransfer.save(params);
        if (!res.success) {
          return;
        }
        id = res.data.id;
      }
      const params = {id, type: operateType};
      loading("审核中....");
      InventoryTransfer.approve(params)
          .then((success) => {
            if (success) {
              message("审核成功~");
              setTimeout(() => {
                this.loadEditForm(id);
              }, 300);
            }
          })
          .finally(() => loading.close());
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
  beforeDestroy() {
    confirm({
      title: "系统提示",
      content: `确认?`,
      onConfirm: () => {
      },
    });
  },
  created() {
    loading("加载中....");
    this.loadDict(() => {
      //订单详情/编辑订单
      if (this.inventoryTransferId) {
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
        return
      }
      this.initIncreaseForm();
    });
  },
};
</script>
