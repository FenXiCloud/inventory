<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <vxe-toolbar class-name="!size--mini">
        <template #buttons>
          <label class="mr-20px ml-16px" style="font-size: 16px !important">单据日期：</label>
          <DatePicker v-model="form.orderDate" :disabled="looked"
                      :option="{ start: accountBook.checkoutDate }"
                      :clearable="false">
          </DatePicker>
        </template>
        <template #tools>
          <Stamp v-if="approved"/>
        </template>
      </vxe-toolbar>
      <vxe-table :edit-rules="validRules" size="mini" ref="xTable" border="border" show-overflow keep-source
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
        <vxe-column field="productUrl" title="商品图片" width="100" :cell-render="imgUrlCellRender"></vxe-column>
        <vxe-column field="productCode" title="商品编码" width="100"></vxe-column>
        <vxe-column field="productName" title="商品名称" min-width="300">
          <template #default="scope">
            <div class="h-input-group goodsSelect" v-if="!looked">
              <Select :deletable="false" ref="ms" v-model="scope.row.productId" :datas="productList" filterable :equalWidth="false"
                      placeholder="输入编码/名称" keyName="id" titleName="customName" @change="changeRow(scope, 'product')">
                <template v-slot:top>
                  <table class="h-table" style="width: 100%">
                    <thead class="h-table-header">
                    <tr>
                      <td width="150" align="center">商品编号</td>
                      <td width="150" align="center">商品图片</td>
                      <td width="150" align="center">商品名称</td>
                      <td width="150" align="center">商品类别</td>
                      <td width="150" align="center">商品规格</td>
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
        <vxe-column title="商品类别" field="productCategoryName" align="center" width="120"></vxe-column>
        <!-- <vxe-column title="品牌" field="productBrand" width="90"></vxe-column> -->
        <!-- <vxe-column title="产地" field="productOrigin" align="center" width="80" /> -->
        <vxe-column title="单位" field="productUnitName" width="90"/>
        <vxe-column title="仓库" field="warehouseName" width="300">
          <template #default="scope">
            <div class="h-input-group goodsSelect" v-if="!looked">
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
        <vxe-column title="调整金额" field="adjustmentAmount" width="100">
          <template #default="scope">
            <vxe-tooltip v-if="!looked" theme="light" :content="scope.row.quantityTips">
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
            <vxe-input v-if="!looked" v-model.number="scope.row.remarks" :controls="false">
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
          const value = this.costAdjustmentData[rowIndex].productId;
          if (this.isEmpty(value)) {
            return;
          }
          // 根据id获取商品信息更新
          Product.list({id: value}).then(res => {
            const {success, data} = res;
            if (success) {
              const item = data.results[0];
              this.costAdjustmentData[rowIndex].productName = item.name;
              this.costAdjustmentData[rowIndex].productId = item.id;
              this.costAdjustmentData[rowIndex].productCode = item.code;
              this.costAdjustmentData[rowIndex].productSpecification = item.specification;
              this.costAdjustmentData[rowIndex].productCategoryName = item.productCategoryName;
              this.costAdjustmentData[rowIndex].productUnitName = item.unitName;
              this.costAdjustmentData[rowIndex].productUnitId = item.unitId;
              this.costAdjustmentData[rowIndex].purchasePrice = item.purchasePrice;
              const warehouseId = this.costAdjustmentData[rowIndex].warehouseId;
              if (!warehouseId) {
                let find = this.warehouseList.find(warehouse => {
                  return warehouse.systemDefault;
                });
                if (find) {
                  const warehouseId = find.id;
                  this.costAdjustmentData[rowIndex].warehouseId = warehouseId;
                  // 根据id获取仓库信息
                  Warehouse.list({id: warehouseId}).then(res => {
                    console.info("Warehouse res:", res);
                    const {success, data} = res;
                    if (success) {
                      const item = data[0];
                      this.costAdjustmentData[rowIndex].warehouseName = item.name;
                      this.costAdjustmentData[rowIndex].warehouseId = item.id;
                      this.$forceUpdate();
                    }
                  });
                }
              }
              this.$forceUpdate();
            }
          });
          break;
        }
        case 'warehouse': {
          const value = this.costAdjustmentData[rowIndex].warehouseId;
          if (this.isEmpty(value)) {
            return;
          }
          // 根据id获取仓库信息
          Warehouse.list({id: value}).then(res => {
            console.info("Warehouse res:", res);
            const {success, data} = res;
            if (success) {
              const item = data[0];
              this.costAdjustmentData[rowIndex].warehouseName = item.name;
              this.costAdjustmentData[rowIndex].warehouseId = item.id;
              this.$forceUpdate();
            }
          });
          break;
        }
        default:
          break;
      }
    },
    getTotalCost({rowIndex}) {
      const item = this.costAdjustmentData[rowIndex];
      if (item.productId && item.warehouseId) {
        Inventory.totalCost(item.productId, item.warehouseId).then(res => {
          this.costAdjustmentData[rowIndex].totalCost = res.data;
          this.costAdjustmentData[rowIndex].quantityTips = `总成本：${res.data}`;
        });
      }
    },
    isEmpty(value) {
      return (value !== 0 && !value) || value === '';
    },
    //保存新增、保存
    saveOrder(type) {
      const filterCostAdjustmentData = this.costAdjustmentData.filter(item => !this.isEmpty(item.productId) || !this.isEmpty(item.warehouseId) || !this.isEmpty(item.quantity) || !this.isEmpty(item.remarks));
      // 校验
      this.validatorsForm(filterCostAdjustmentData);
      // 操作对象
      const params = this.getSaveOrderParams(filterCostAdjustmentData, type);
      CostAdjustment.save(params)
          .then(({success, data}) => {
            if (success) {
              message("保存成功~");
              setTimeout(() => {
                if (type === "increase") {
                  this.clearForm();
                  // 刷新列表为编辑
                  this.closeWindow();
                  this.pushTab({
                    key: 'CostAdjustmentForm',
                    title: '新增成本调整单',
                    params: {type: type, costAdjustmentId: null}
                  });
                } else {
                  // 刷新列表为编辑
                  this.closeWindow();
                  this.pushTab({
                    key: 'CostAdjustmentForm',
                    title: '编辑成本调整单',
                    params: {type: 'edit', costAdjustmentId: data.id}
                  });
                  this.$emit("update:costAdjustmentId", data.id);
                  this.$emit("update:type", "edit");
                  this.loadEditForm(data.id);
                }
              }, 300);
            }
          })
          .finally(() => loading.close());
    },
    //校验提交表单
    validatorsForm(filterCostAdjustmentData) {
      if (filterCostAdjustmentData.length === 0) {
        throw new Error("请填写操作数据~")
      }
      loading("保存中....");
      let productData = filterCostAdjustmentData.filter((c) => this.isEmpty(c.productId));
      console.info("productData:", productData)
      if (productData.length > 0) {
        loading.close();
        throw new Error("请选择商品~")
      }
      let warehouse = filterCostAdjustmentData.filter((c) => this.isEmpty(c.warehouseId));
      if (warehouse.length > 0) {
        loading.close();
        throw new Error("请选择仓库~")
      }
      let adjustmentAmount = filterCostAdjustmentData.filter((c) => this.isEmpty(c.adjustmentAmount) || Number(c.adjustmentAmount) === 0);
      if (adjustmentAmount.length > 0) {
        loading.close();
        throw new Error("请填写调整金额~")
      }
      let totalCost = filterCostAdjustmentData.filter((c) => (c.totalCost + c.adjustmentAmount) <= 0);
      if (totalCost.length > 0) {
        loading.close();
        throw new Error("调整后金额不能小于等于零~")
      }
    },
    //获取保存新增、保存方法提交数据
    getSaveOrderParams(filterCostAdjustmentData, type) {
      const costAdjustmentItems = [];
      const costAdjustment = {
        adjustmentType: this.form.adjustmentType,
        djustmentDate: this.form.orderDate,
        remarks: this.form.remarks
      };
      costAdjustment.id = this.form.id;
      filterCostAdjustmentData.forEach(item => {
        costAdjustmentItems.push({
          productId: item.productId,
          baseUnitId: item.productUnitId,
          remarks: item.remarks,
          warehouseId: item.warehouseId,
          adjustmentAmount: item.adjustmentAmount,
          totalCost: item.totalCost,
        });
      });
      return {
        costAdjustment: costAdjustment,
        costAdjustmentItems: costAdjustmentItems
      };
    },
    //清除Form
    clearForm() {
      this.form = {
        id: null,
        orderDate: manba().format("YYYY-MM-dd"),
        remarks: null,
        adjustmentType: '入库调整',
        adminName: '',
        orderStatus: '已保存'
      };
      this.costAdjustmentData = [];
      this.newCostAdjustmentData();
    },

    //添加行或减少行
    adjustRows(type, index) {
      if (type === "insert") {
        this.costAdjustmentData.splice(index + 1, 0, {isNew: true});
      } else {
        this.costAdjustmentData.splice(index, 1);
      }
    },
    //新增默认初始化行数
    newCostAdjustmentData() {
      for (let index = 0; index < 5; index++) {
        this.costAdjustmentData.push({productId: null, warehouseId: null, quantity: null});
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
    //加载编辑表单
    loadEditForm(id) {
      this.editConfig = {trigger: 'click', mode: 'row'};
      this.increase = false;
      this.costAdjustmentData = [];
      CostAdjustment.load(this.costAdjustmentId || id).then(
          ({data}) => {
            if (data && data.length > 0) {
              this.form.id = data[0].id;
              this.form.remarks = data[0].costRemarks;
              this.form.adjustmentType = data[0].adjustmentType;
              this.form.djustmentDate = data[0].djustmentDate;
              this.form.adminName = data[0].adminName;
              this.form.orderStatus = data[0].orderStatus;
              let totalAdjustmentAmount = 0;
              data.forEach(item => {
                totalAdjustmentAmount += parseFloat(item.adjustmentAmount);
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
    //加载字典
    loadDict(callback) {
      Promise.all([Product.select(), Warehouse.select(), Customer.select(), Supplier.select()])
          .then((results) => {
            this.productList = results[0].data || [];
            // 调整productList的name值
            this.productList.forEach(item => {
              item.customName = `${item.code}--${item.name}`;
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
      this.newCostAdjustmentData();
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
        const filterCostAdjustmentData = this.costAdjustmentData.filter(item => !this.isEmpty(item.productId) || !this.isEmpty(item.warehouseId) || !this.isEmpty(item.quantity) || !this.isEmpty(item.remarks));
        // 校验
        this.validatorsForm(filterCostAdjustmentData);
        // 操作对象
        const params = this.getSaveOrderParams(filterCostAdjustmentData, type);
        const res = await CostAdjustment.save(params);
        if (!res.success) {
          return;
        }
        id = res.data.id;
      }
      const params = {id, type: operateType};
      loading("审核中....");
      CostAdjustment.approve(params)
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
        key: "CostAdjustmentList",
        title: "成本调整单"
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
        return
      }
      this.initIncreaseForm();
    });
  },
};
</script>
