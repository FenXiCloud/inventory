<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <vxe-toolbar class-name="!size--mini">
        <template #buttons>
          <label class="mr-20px ml-16px" style="font-size: 16px !important">单据日期：</label>
          <DatePicker v-model="form.orderDate" :disabled="auditOperate" :option="{ start: accountBook.checkoutDate }"
                      :clearable="false">
          </DatePicker>
          <label class="mr-20px ml-20px" style="font-size: 16px !important">供应商：</label>
          <Select class="w-178px" filterable required :datas="supplierList" keyName="id" titleName="name"
                  v-model="form.supplierId" placeholder="请选择供应商" :disabled="auditOperate"/>
          <label class="mr-20px ml-20px" style="font-size: 16px !important">客户：</label>
          <Select class="w-178px" filterable required :datas="customerList" keyName="id" titleName="name"
                  v-model="form.customerId" placeholder="请选择客户" :disabled="auditOperate"/>
          <label class="mr-20px ml-16px" style="font-size: 16px !important">业务类型：</label>
          <Select class="w-178px" filterable required :datas="inboundTypeList" keyName="id" titleName="name"
                  :deletable="false" v-model="form.inboundType" :disabled="auditOperate" placeholder="请选择业务类型"/>
        </template>
      </vxe-toolbar>
      <vxe-table :edit-rules="validRules" size="mini" ref="xTable" border="border" show-overflow keep-source
                 :edit-config="editConfig" :row-config="{ height: 40, isCurrent: true, isHover: true }"
                 :tooltip-config="tooltipConfig" show-footer :footer-method="footerMethod" stripe
                 :data="otherInboundData"
                 @current-change="currentChangeEvent" @cell-click="tableCellClick">
        <vxe-column title="操作" field="seq" width="70" align="center" fixed="left">
          <template #default="{ row, rowIndex }">
            <div v-if="rowIsSelect(rowIndex)">
              <div class="fa fa-plus text-hover mr-5px" @click="adjustRows('insert', rowIndex)"></div>
              <div v-if="otherInboundData.length !== 1" class="fa fa-minus text-hover-danger"
                   @click="adjustRows('delete', rowIndex)"></div>
            </div>
            <div v-else>
              {{ rowIndex + 1 }}
            </div>
          </template>
        </vxe-column>
        <vxe-column field="productUrl" title="商品图片" width="100" :cell-render="imgUrlCellRender"></vxe-column>
        <vxe-column field="productCode" title="商品编码" width="100"></vxe-column>
        <vxe-column field="productName" title="商品名称" min-width="300">
          <template #default="scope">
            <div class="h-input-group goodsSelect" v-if="!auditOperate">
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
        <vxe-column title="规格型号" field="productSpecification" align="center" width="80"></vxe-column>
        <vxe-column title="商品类别" field="productCategoryName" align="center" width="120"></vxe-column>
        <!-- <vxe-column title="品牌" field="productBrand" width="90"></vxe-column> -->
        <!-- <vxe-column title="产地" field="productOrigin" align="center" width="80" /> -->
        <vxe-column title="单位" field="productUnitName" width="90"/>
        <vxe-column title="仓库" field="warehouseName" width="300">
          <template #default="scope">
            <div class="h-input-group goodsSelect" v-if="!auditOperate">
              <Select :deletable="false" ref="ms" v-model="scope.row.warehouseId" :datas="warehouseList" filterable
                      placeholder="请选择仓库" keyName="id" titleName="name" @change="changeRow(scope, 'warehouse')">
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
            <vxe-tooltip v-if="!auditOperate" :content="scope.row.quantityTips" theme="light">
              <vxe-input @focus="quantityFocus(scope)" @blur="quantityBlur('quantity',scope)"
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
        <vxe-column title="入库单价" field="unitPrice" width="125">
          <template #default="scope">
            <vxe-input v-if="!auditOperate" @focus="quantityFocus(scope)" @blur="quantityBlur('unitPrice',scope)"
                       v-model.number="scope.row.unitPrice" type="int" min="0" :controls="false">
            </vxe-input>
            <div v-else class="flex">
              <div class="flex1 ml-8px">
                <div>{{ scope.row.unitPrice }}</div>
              </div>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="入库金额" field="subtotal" width="125">
          <template #default="scope">
            <vxe-input v-if="!auditOperate" @focus="quantityFocus(scope)"
                       @blur="quantityBlur('subtotal',scope)"
                       v-model.number="scope.row.subtotal" type="int" min="0" :controls="false">
            </vxe-input>
            <div v-else class="flex">
              <div class="flex1 ml-8px">
                <div>{{ scope.row.subtotal }}</div>
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
          <Input :disabled="auditOperate" placeholder="请输入备注" type="text" maxlength="150" style="width: 80%"
                 v-model="form.remarks"/>
          <label class="ml-16px w-180px">制单人：{{ form.adminName }}</label>
        </div>
      </div>
    </div>
    <div class="modal-column-between bg-white-color border">
      <Button @click="closeWindow" :loading="loading"> 取消</Button>
      <div>
        <Button color="primary" v-if="!auditOperate" @click="saveOrder('increase')" :loading="loading">
          保存并新增
        </Button>
        <Button @click="saveOrder" v-if="form.orderStatus !== '已审核' && !auditOperate" :loading="loading"> 保存
        </Button>
        <!-- 当状态为已审核时不显示,审核后订单上显示已审核图片 -->
        <Button v-if="type === 'audits'" @click="auditForm" :loading="loading"> 审核</Button>
        <!-- 仅当状态为审核时显示 -->
        <Button v-if="type === 'antiAudits'" @click="auditForm" :loading="loading"> 反审核</Button>
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
import Customer from "@js/api/basic/Customer";
import Supplier from "@js/api/basic/Supplier";
import OtherInbound from "@js/api/inventory/OtherInbound";
import Inventory from "@js/api/inventory/Inventory";
import {mapMutations, mapState} from "vuex";

export default {
  name: "OtherInboundForm",
  props: {
    otherInboundId: [String, Number],
    stockTakeId: [String, Number],
    importInbound: Array,
    type: String,
    index: Number
  },
  computed: {
    ...mapState(["user", "accountBook"]),
    auditOperate() {
      return ['audits', 'antiAudits'].includes(this.type);
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
    ...mapMutations(['closeSelfTab','updateTab']),
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
    changeRow({rowIndex}, type) {
      switch (type) {
        case 'product': {
          const value = this.otherInboundData[rowIndex].productId;
          if (this.isEmpty(value)) {
            return;
          }
          // 根据id获取商品信息更新
          Product.list({id: value}).then(res => {
            const {success, data} = res;
            if (success) {
              const item = data.results[0];
              console.info("Product info:", item);
              this.otherInboundData[rowIndex].productName = item.name;
              this.otherInboundData[rowIndex].productId = item.id;
              this.otherInboundData[rowIndex].productCode = item.code;
              this.otherInboundData[rowIndex].productSpecification = item.specification;
              this.otherInboundData[rowIndex].productCategoryName = item.productCategoryName;
              this.otherInboundData[rowIndex].productUnitName = item.unitName;
              this.otherInboundData[rowIndex].productUnitId = item.unitId;
              this.$forceUpdate();
            }
          });
          break;
        }
        case 'warehouse': {
          const value = this.otherInboundData[rowIndex].warehouseId;
          if (this.isEmpty(value)) {
            return;
          }
          // 根据id获取仓库信息
          Warehouse.list({id: value}).then(res => {
            console.info("Warehouse res:", res);
            const {success, data} = res;
            if (success) {
              const item = data[0];
              this.otherInboundData[rowIndex].warehouseName = item.name;
              this.otherInboundData[rowIndex].warehouseId = item.id;
              this.$forceUpdate();
            }
          });
          break;
        }
        default:
          break;
      }
    },
    isEmpty(value) {
      return (value !== 0 && !value) || value === '';
    },
    //保存新增、保存
    saveOrder(type) {
      const filterOtherInboundData = this.otherInboundData.filter(item => !this.isEmpty(item.productId) || !this.isEmpty(item.warehouseId) || !this.isEmpty(item.quantity) || !this.isEmpty(item.remarks));
      // 校验
      this.validatorsForm(filterOtherInboundData);
      // 操作对象
      const params = this.getSaveOrderParams(filterOtherInboundData, type);
      OtherInbound.save(params)
          .then((success) => {
            if (success) {
              message("保存成功~");
              this.clearForm();
              setTimeout(() => {
                this.closeWindow();
              }, 300);
            }
          })
          .finally(() => loading.close());
    },
    //校验提交表单
    validatorsForm(filterOtherInboundData) {
      if (filterOtherInboundData.length === 0) {
        throw new Error("请填写操作数据~")
      }
      loading("保存中....");
      let productData = filterOtherInboundData.filter((c) => this.isEmpty(c.productId));
      console.info("productData:", productData)
      if (productData.length > 0) {
        loading.close();
        throw new Error("请选择商品~")
      }
      let warehouse = filterOtherInboundData.filter((c) => this.isEmpty(c.warehouseId));
      if (warehouse.length > 0) {
        loading.close();
        throw new Error("请选择仓库~")
      }
      let quantity = filterOtherInboundData.filter((c) => this.isEmpty(c.quantity) || Number(c.quantity) === 0);
      if (quantity.length > 0) {
        loading.close();
        throw new Error("请填写数量~")
      }
      let unitPrice = filterOtherInboundData.filter((c) => this.isEmpty(c.unitPrice) || Number(c.unitPrice) === 0);
      if (unitPrice.length > 0) {
        loading.close();
        throw new Error("请填写入库单价~")
      }
      let subtotal = filterOtherInboundData.filter((c) => this.isEmpty(c.subtotal) || Number(c.subtotal) === 0);
      if (subtotal.length > 0) {
        loading.close();
        throw new Error("请填写入库金额~")
      }
    },
    //获取保存新增、保存方法提交数据
    getSaveOrderParams(filterOtherInboundData, type) {
      const otherInboundItems = [];
      const otherInbound = {
        customerId: this.form.customerId,
        supplierId: this.form.supplierId,
        inboundType: this.form.inboundType,
        inboundDate: this.form.orderDate,
        remarks: this.form.remarks
      };
      if (type !== "increase") {
        otherInbound.id = this.form.id;
      } else {
        // 新增处理盘点主表id
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
    //清除Form
    clearForm() {
      this.form = {
        id: null,
        orderDate: manba().format("YYYY-MM-dd"),
        remarks: null,
        customerId: null,
        supplierId: null,
        inboundType: '其他入库',
        totalAmount: 0.00,
        totalQuantity: 0,
        quantityTips: ''
      };
      this.otherInboundData = [];
      this.newOtherInboundData();
    },

    //添加行或减少行
    adjustRows(type, index) {
      if (type === "insert") {
        this.otherInboundData.splice(index + 1, 0, {isNew: true});
      } else {
        this.otherInboundData.splice(index, 1);
      }
    },
    //新增默认初始化行数
    newOtherInboundData() {
      for (let index = 0; index < 5; index++) {
        this.otherInboundData.push({productId: null, warehouseId: null, quantity: null});
      }
    },
    //行是否选中
    rowIsSelect(rowIndex) {
      return (this.increase || rowIndex === this.selectRowIndex) && !this.auditOperate;
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
      const otherOutboundItem = this.otherInboundData[rowIndex];
      const {productId, warehouseId} = otherOutboundItem;
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
          this.otherInboundData[rowIndex].quantityTips = `总库存：${totalQuantity}\r\n仓库库存：${quantity}`;
        } else {
          this.otherInboundData[rowIndex].quantityTips = `总库存：0\r\n仓库库存：0`;
        }
      });
    },
    //失去焦点
    quantityBlur(type, {rowIndex}) {
      console.info("quantityBlur:", type);
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
    //加载编辑表单
    loadEditForm() {
      this.editConfig = {trigger: 'click', mode: 'row'};
      this.increase = false;
      this.otherInboundData = [];
      OtherInbound.load(this.otherInboundId).then(
          ({data}) => {
            if (data && data.length > 0) {
              this.form.id = data[0].id;
              this.form.customerId = data[0].customerId;
              this.form.supplierId = data[0].supplierId;
              this.form.remarks = data[0].remarks;
              this.form.outboundType = data[0].outboundType;
              this.form.orderDate = data[0].inboundDate;
              this.form.adminName = data[0].adminName;
              this.form.orderStatus = data[0].orderStatus;
              let totalAmount = 0;
              let totalQuantity = 0;
              data.forEach(item => {
                totalAmount += parseFloat(item.subtotal);
                totalQuantity += parseInt(item.quantity);
                this.otherInboundData.push({
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
      Promise.all([Product.select(), Warehouse.select(), Customer.select(), Supplier.select()])
          .then((results) => {
            this.productList = results[0].data || [];
            // 调整productList的name值
            this.productList.forEach(item => {
              item.name = `${item.code}--${item.name}`;
            });
            this.warehouseList = results[1].data || [];
            this.customerList = results[2].data || [];
            this.supplierList = results[3].data || [];
            if (this.warehouseList != null) {
              this.warehouseId = this.warehouseList.find(
                  (val) => val.isDefault
              )?.id;
            }
            console.log("results[3].data:", results[3].data);
            if (callback) {
              callback();
            }
          })
          .finally(() => loading.close());
    },
    //初始化表单
    initIncreaseForm() {
      this.increase = true;
      this.newOtherInboundData();
      this.form.adminName = this.user.admin.name;
      this.form.id = null;
      this.editConfig = {trigger: 'click', mode: 'row'};
      if (this.stockTakeId) {
        this.form.inboundType = "盘盈入库";
        this.otherInboundData = JSON.parse(JSON.stringify(this.importInbound));
      }
    },
    //初始化审核表单
    initAuditsForm() {
      //表格不可编辑
      this.editConfig = {};
    },
    //审核表单
    auditForm() {
      const type = this.type;
      let operateType = "AUDITS";
      if (type !== "audits") {
        operateType = "ANTI_AUDIT";
      }
      const {id} = this.form;
      const params = {id, type: operateType};
      loading("审核中....");
      OtherInbound.approve(params)
          .then((success) => {
            if (success) {
              message("审核成功~");
              setTimeout(() => {
                this.closeWindow();
              }, 300);
            }
          })
          .finally(() => loading.close());
    },
    closeWindow() {
      this.closeSelfTab(this.index);
      this.updateTab("OtherInboundList");
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
      if (this.otherInboundId) {
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
