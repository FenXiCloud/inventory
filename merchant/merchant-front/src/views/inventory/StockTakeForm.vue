<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <vxe-toolbar class-name="!size--mini">
        <template #buttons>
          <label class="mr-20px ml-16px" style="font-size: 16px !important">盘点日期：</label>
          <label style="font-size: 15px !important">{{ form.checkDate }}</label>
          <label class="mr-20px ml-20px" style="font-size: 16px !important">仓库：</label>
          <Select class="w-178px" filterable required :datas="warehouseList" keyName="id" titleName="name"
                  v-model="form.warehouseId" placeholder="请选择仓库" :disabled="auditOperate || form.id"
                  @change="changeWarehouseId"/>
          <label class="mr-20px ml-20px" style="font-size: 16px !important">商品：</label>
          <Select class="w-178px mr-20px" filterable required :datas="productList" keyName="id" titleName="name"
                  v-model="form.productId" placeholder="请选择商品" :disabled="auditOperate || form.id"/>
        </template>
        <template #tools>
          <Search v-if="!form.id" v-model.trim="form.filter" search-button-theme="h-btn-default"
                  show-search-button class="w-360px ml-8px"
                  placeholder="请输入商品编号/商品名称" @search="doSearch">
            <i class="h-icon-search"/>
          </Search>
        </template>
      </vxe-toolbar>
      <vxe-table :edit-rules="validRules" size="mini" ref="xTable" border="border" show-overflow keep-source
                 :edit-config="editConfig" :row-config="{ height: 40, isCurrent: true, isHover: true }"
                 :tooltip-config="tooltipConfig" show-footer :footer-method="footerMethod" stripe
                 :data="stockTakeData"
                 @current-change="currentChangeEvent" @cell-click="tableCellClick">
        <vxe-column title="序号" type="seq" width="70" align="center" fixed="left"/>
        <vxe-column field="warehouseName" title="仓库" width="150"></vxe-column>
        <vxe-column field="productUrl" title="商品图片" width="150" :cell-render="imgUrlCellRender"></vxe-column>
        <vxe-column field="productCode" title="商品编码" width="160"></vxe-column>
        <vxe-column field="productName" title="商品名称" min-width="250">
        </vxe-column>
        <vxe-column title="规格型号" field="productSpecification" align="center" width="120"></vxe-column>
        <vxe-column title="商品类别" field="productCategoryName" align="center" width="110"></vxe-column>
        <!-- <vxe-column title="品牌" field="productBrand" width="90"></vxe-column> -->
        <!-- <vxe-column title="产地" field="productOrigin" align="center" width="80" /> -->
        <vxe-column title="单位" field="productUnitName" width="100"/>
        <vxe-column title="系统库存" field="systemQuantity" width="100"/>
        <vxe-column title="盘点库存" field="actualQuantity" width="100">
          <template #default="scope">
            <vxe-input v-if="!auditOperate" @input="quantityInput(scope)"
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
            <vxe-input v-if="!auditOperate" v-model="scope.row.differenceReason"></vxe-input>
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
        <Button color="primary" v-if="!auditOperate" :disabled="form.generatedDisabled" @click="openGeneratedModal"
                :loading="loading">
          生成盘点单据
        </Button>
        <Button @click="saveOrder" v-if="form.orderStatus !== '已审核' && !auditOperate && !form.id" :loading="loading">
          保存
        </Button>
        <!-- 当状态为已审核时不显示,审核后订单上显示已审核图片 -->
        <Button v-if="type === 'audits'" @click="auditForm" :loading="loading"> 审核</Button>
        <!-- 仅当状态为审核时显示 -->
        <Button v-if="type === 'antiAudits'" @click="auditForm" :loading="loading"> 反审核</Button>
      </div>
    </div>
    <Modal v-model="opened" hasCloseIcon>
      <div slot="header">
        <div>
          生成盘点单据
        </div>
      </div>
      <div class="mt-10px">
        <div class="h-full flex justify-center items-center flex-column" v-if="inbounds.length > 0">
          <Button color="primary" @click="generatedInbounds" :loading="loading">盘盈单</Button>
        </div>
        <div class="mt-10px h-full flex justify-center items-center flex-column" v-if="outbounds.length > 0">
          <Button color="primary" @click="generatedOutbounds" :loading="loading">盘亏单</Button>
        </div>
      </div>
    </Modal>
  </div>
</template>
<script>
import {confirm, loading, message} from "heyui.ext";
import manba from "manba";
import {CopyObj} from "@common/utils";
import Product from "@js/api/basic/Product";
import Warehouse from "@js/api/basic/Warehouse";
import Inventory from "@js/api/inventory/Inventory";
import {mapMutations, mapState} from "vuex";
import StockTake from "@js/api/inventory/StockTake";

export default {
  name: "StockTakeForm",
  props: {
    stockTakeId: [String, Number],
    type: String,
    index: Number,
    status: String,
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
      opened: false,
      form: {
        id: null,
        checkDate: manba().format("YYYY-MM-dd"),
        remarks: null,
        warehouseId: null,
        productId: null,
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
      editConfig: {trigger: 'click', mode: 'row'},
      // 盘盈
      outbounds: [],
      // 盘亏
      inbounds: []
    };
  },
  // 待优化使用hook方式调用
  methods: {
    // 关闭tab
    ...mapMutations(['closeSelfTab', 'pushTab', 'updateTab']),
    //footer合计
    footerMethod({columns, data}) {
      let totalQuantity = 0;
      let totalAmount = 0.00;
      columns.forEach(column => {
        if (column.property && ['actualQuantity'].includes(column.property)) {
          data.forEach((row) => {
            switch (column.property) {
              case 'actualQuantity': {
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
    isEmpty(value) {
      return (value !== 0 && !value) || value === '';
    },
    //保存新增、保存
    saveOrder(type) {
      const filterStockTakeData = this.stockTakeData.filter(item => !this.isEmpty(item.productId) || !this.isEmpty(item.warehouseId) || !this.isEmpty(item.quantity) || !this.isEmpty(item.remarks));
      // 校验
      this.validatorsForm(filterStockTakeData);
      // 操作对象
      const params = this.getSaveOrderParams(filterStockTakeData, type);
      StockTake.save(params)
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
    // 打开生成盘点单
    openGeneratedModal() {
      this.opened = true;
    },
    // 盘盈单
    generatedInbounds() {
      this.pushTab({
        key: 'OtherInboundForm',
        title: '新增其他入库单',
        params: {type: "add", otherInboundId: null, stockTakeId: this.form.id, importInbound: this.inbounds}
      });
      this.opened = false;
    },
    // 盘亏单
    generatedOutbounds() {
      this.pushTab({
        key: 'OtherOutboundForm',
        title: '新增其他出库单',
        params: {type: "add", otherOutboundId: null, stockTakeId: this.form.id, importOutbound: this.outbounds}
      });
      this.opened = false;
    },
    //校验提交表单
    validatorsForm(filterStockTakeData) {
      if (filterStockTakeData.length === 0) {
        throw new Error("请填写操作数据~")
      }
      loading("保存中....");
      let quantity = filterStockTakeData.filter((c) => this.isEmpty(c.actualQuantity) || Number(c.actualQuantity) === 0);
      if (quantity.length > 0) {
        loading.close();
        throw new Error("请填写盘点库存~")
      }
    },
    //获取保存新增、保存方法提交数据
    getSaveOrderParams(filterStockTakeData, type) {
      const stockTakeItems = [];
      const stockTake = {
        checkDate: this.form.checkDate,
        remarks: this.form.remarks,
        warehouseId: this.form.warehouseId,
      };
      if (type !== "increase") {
        stockTake.id = this.form.id;
      }
      filterStockTakeData.forEach(item => {
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
        quantityTips: '',
        generatedDisabled: true
      };
      this.stockTakeData = [];
      this.newStockTakeData();
    },

    //添加行或减少行
    adjustRows(type, index) {
      if (type === "insert") {
        this.stockTakeData.splice(index + 1, 0, {isNew: true});
      } else {
        this.stockTakeData.splice(index, 1);
      }
    },
    //新增默认初始化行数
    newStockTakeData() {
      // 获取所有商品、仓库展示
      Inventory.products({
        warehouseId: this.form.warehouseId,
        productId: this.form.productId,
        filter: this.form.filter
      }).then((res) => {
        this.stockTakeData = res.data || [];
      });
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
    //输入事件
    quantityInput({rowIndex}) {
      const {actualQuantity, systemQuantity} = this.stockTakeData[rowIndex];
      if (actualQuantity < 0) {
        this.stockTakeData[rowIndex].actualQuantity = 0;
        return
      }
      this.stockTakeData[rowIndex].deficient = actualQuantity - systemQuantity;
    },
    changeWarehouseId() {
      this.doSearch();
    },
    //加载编辑表单
    loadEditForm() {
      this.editConfig = {trigger: 'click', mode: 'row'};
      this.increase = false;
      this.stockTakeData = [];
      StockTake.load(this.stockTakeId).then(
          ({data}) => {
            if (data && data.length > 0) {
              this.form.id = data[0].id;
              this.form.warehouseId = data[0].mainWarehouseId;
              this.form.remarks = data[0].remarks;
              this.form.checkDate = data[0].checkDate;
              this.form.adminName = data[0].adminName;
              this.form.orderStatus = data[0].orderStatus;
              let totalQuantity = 0;
              data.forEach(item => {
                totalQuantity += parseInt(item.quantity);
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
    //加载字典
    loadDict(callback) {
      loading("加载中....");
      Promise.all([Product.select(), Warehouse.select()])
          .then((results) => {
            this.productList = results[0].data || [];
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
      this.newStockTakeData();
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
    auditForm() {
      const type = this.type;
      let operateType = "AUDITS";
      if (type !== "audits") {
        operateType = "ANTI_AUDIT";
      }
      const {id} = this.form;
      const params = {id, type: operateType};
      loading("审核中....");
      StockTake.approve(params)
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
      this.updateTab("StockTakeList");
    },
    doSearch() {
      if (this.stockTakeId) {
        const warehouseId = this.form.warehouseId;
        const productId = this.form.productId;
        const filter = this.form.filter;
        const newStockTakeData = [];
        // 搜索当前盘点单数据
        this.originalStockTakeData.forEach(item => {
          let isDone = true;
          if (warehouseId && warehouseId !== item.warehouseId) {
            isDone = false;
          }
          if (productId && productId !== item.productId) {
            isDone = false;
          }
          if (!this.isEmpty(filter) && (filter.indexOf(item.productCode) === -1 && filter.indexOf(item.productName) === -1)) {
            isDone = false;
          }
          if (isDone) {
            newStockTakeData.push(item);
          }
        });
        this.stockTakeData = newStockTakeData;
      } else {
        // 搜索所有仓库商品
        this.newStockTakeData();
      }
    },
    // 获取盘点单据
    getInventoryList() {
      if (this.stockTakeId) {
        StockTake.export(this.stockTakeId).then(res => {
          const data = res.data;
          const {outbounds, inbounds} = data;
          if (outbounds && outbounds.length > 0) {
            this.outbounds = outbounds;
          }
          if (inbounds && inbounds.length > 0) {
            this.inbounds = inbounds;
          }
          if (this.inbounds.length > 0 || this.outbounds.length > 0) {
            this.form.generatedDisabled = false;
          }
        });
      }
    }
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
      if (this.stockTakeId) {
        this.loadEditForm();
        // 获取盘点单据
        if (this.status && this.status === "已审核") {
          this.getInventoryList();
        }
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
