<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <vxe-toolbar class-name="!size--mini">
        <template #buttons>
          <label class="mr-20px" style="font-size: 16px !important;">客户:</label>
          <Select class="w-300px" filterable required :datas="customerList" keyName="id" titleName="name"
                  :deletable="false" @change="changeCustomer($event)" v-model="customerId" placeholder="请选择客户"/>
          <label class="mr-20px ml-16px" style="font-size: 16px !important;">出库日期:</label>
          <DatePicker v-model="form.outboundDate" :option="{start:accountBook.checkoutDate}"
                      :clearable="false"></DatePicker>
          <Button v-if="type==='add'" @click="addOrEditForm()" color="primary" style="margin-left: 20px">选择源单</Button>
        </template>
      </vxe-toolbar>
      <vxe-table
          size="mini"
          ref="xTable"
          border="border"
          :row-config="{height: 40}"
          show-footer
          :footer-method="footerMethod"
          stripe
          :data="productData">
        <vxe-column title="序号" type="seq" width="60" align="center" fixed="left"/>
        <vxe-column title="操作" field="seq" width="70" align="center" fixed="left">
          <template #default="{row,rowIndex}">
            <div class="fa fa-plus text-hover mr-5px" @click="adjustRows('insert',rowIndex)"></div>
            <div class="fa fa-minus text-hover" v-if="isDeleting" @click="adjustRows('delete',rowIndex)"></div>
          </template>
        </vxe-column>
        <vxe-column title="商品信息" width="180" align="center">
          <template #default="scope">
            <div class="h-input-group goodsSelect" @keyup.stop="void(0)">
              <Select ref="ms" @change="selectProduct($event,scope.rowIndex)" :datas="productList" v-model="scope.row.productId"
                      keyName="id" titleName="name" filterable placeholder="输入编码/名称" :deletable="false">
                <template v-slot:item="{ item }">
                  <div>{{ item.name }}</div>
                </template>
              </Select>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="仓库" field="warehouse" align="center" width="180">
          <template #default="{row,rowIndex}">
            <template v-if="!row.isNew">
              <Select :deletable="false" v-model="row.warehouseId" :datas="warehouseList" filterable keyName="id"
                      titleName="name" @change="handleWarehouseChange(row, $event)"/>
            </template>
          </template>
        </vxe-column>
        <vxe-column title="数量" field="quantity" width="90">
          <template #default="{row,rowIndex,columnIndex}">
            <vxe-tooltip theme="light">
              <template #content>
                <div>当前库存: {{row.currentStockQuantity || 0}}</div>
                <div>总库存: {{row.totalStockQuantity || 0}}</div>
              </template>
              <vxe-input
                  :id="'r'+rowIndex+''+3"
                  @blur="updateQuantity(row)"
                  @focus="showStockQuantity(row)"
                  ref="inputQuantity"
                  v-model.number="row.quantity"
                  type="float"
                  min="0"
                  :controls="false">
              </vxe-input>
            </vxe-tooltip>
          </template>
        </vxe-column>
        <vxe-column title="单位" field="unitName" align="center" width="80"/>
        <vxe-column title="单价" field="unitPrice" width="100">
          <template #default="{row,rowIndex}">
            <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+4"
                       @blur="updatePrice(row)" v-model.number="row.unitPrice" type="float" min="0"
                       :controls="false"></vxe-input>
          </template>
        </vxe-column>
        <vxe-column title="折扣率(%)" field="discountRate" width="100">
          <template #default="{row,rowIndex}">
            <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+5"
                       @blur="updateDiscount(row)" v-model.number="row.discountRate" type="float" min="0"
                       :controls="false"></vxe-input>
          </template>
        </vxe-column>
        <vxe-column title="折扣额" field="discountValue" width="100">
          <template #default="{row,rowIndex}">
            <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+6"
                       @blur="updateDiscountAmount(row)" v-model.number="row.discountValue" type="float" min="0"
                       :controls="false"></vxe-input>
          </template>
        </vxe-column>
        <vxe-column title="金额" field="subtotal" width="100">
          <template #default="{row,rowIndex}">
            <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+7"
                       @blur="updateFinalAmount(row)" v-model.number="row.subtotal" type="float" min="0"
                       :controls="false" readonly disabled></vxe-input>
          </template>
        </vxe-column>
        <vxe-column title="备注" field="remark">
          <template #default="{row,rowIndex}">
            <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+8"
                       v-model="row.remark" placeholder="输入备注" :controls="false"></vxe-input>
          </template>
        </vxe-column>
      </vxe-table>
      <div class="mt-10px"></div>
      <div class="filler-panel" v-if="type==='edit'">
        <div class="filler-item" style="flex: 1;margin: 5px 0 !important;">
          <label class="mr-16px  w-100px">单据编号：</label>
          <Input v-model="form.orderNo" readonly/>
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item" style="flex: 1;margin: 5px 0 !important;">
          <label class="mr-16px  w-100px">优惠率(%)：</label>
          <Input v-model="form.discountRate" readonly type="number"/>
          <label class="ml-10px mr-16px  w-80px">优惠金额：</label>
          <Input v-model="form.discountAmount" type="number" readonly/>
          <label class="ml-16px mr-16px  w-100px">优惠后金额：</label>
          <Input v-model="form.finalAmount" type="number" readonly/>
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item" style="flex: 1;margin: 5px 0 !important;">
          <label class="mr-16px  w-100px">备注说明：</label>
          <Input placeholder="请输入备注" maxlength="150" style="width: 90%" v-model="form.remarks"/>
        </div>
      </div>
    </div>
    <div class="modal-column-between bg-white-color  border">
      <Button @click="closeWindow" :loading="loading">
        取消
      </Button>
      <div>
        <Button color="primary" @click="saveOrder('new')" :loading="loading">
          保存并新增
        </Button>
        <Button @click="saveOrder('save')" :loading="loading">
          保存
        </Button>
        <!-- 当状态为已审核时不显示,审核后订单上显示已审核图片 -->
        <Button @click="auditOrder('已审核')" v-if="form.orderStatus === '已保存' " :loading="loading">
          审核
        </Button>
        <!-- 仅当状态为审核时显示 -->
        <Button @click="auditOrder('已保存')" v-if="form.orderStatus === '已审核' " :loading="loading">
          反审核
      </Button>
      </div>
    </div>
  </div>
</template>
<script>

import {confirm, loading, message} from "heyui.ext";
import manba from "manba";
import {CopyObj} from "@common/utils";
import Customer from "@js/api/basic/Customer";
import Warehouse from "@js/api/basic/Warehouse";
import {mapMutations, mapState} from "vuex";
import Product from "@js/api/basic/Product";
import {layer} from "@layui/layer-vue";
import {h} from "vue";
import SalesOrderList from "@views/sales/SalesOrderList.vue";
import CustomerForm from "@views/basic/CustomerForm.vue";
import SalesOrderSelect from "@views/sales/SalesOrderSelect.vue";
import Unit from "@js/api/basic/Unit";
import SalesOutbound from "@js/api/sales/SalesOutbound";
import Inventory from "@js/api/inventory/Inventory";

export default {
  name: "SalesOutboundForm",
  computed: {
    ...mapState(['accountBook']),
    isDeleting() {
      return this.productData.length > 1;
    }
  },
  data() {
    return {
      loading: false,
      productList: [],
      unitList: [],
      product: null,
      warehouseList: [],
      customerList: [],
      customerId: null,
      warehouseId: null,
      form: {
        id: null,
        outboundDate: manba().format("YYYY-MM-dd"),
        customerId: null,
        discountAmount: 0.00,
        discountRate: 0.00,
        finalAmount: 0.00,
        remarks: null,
      },
      productData: [],
      //保存选择的源单
      selectSalesOrderIdList: [],
      orderId:null,
      type:null,
    }
  },
  methods: {
    ...mapMutations(['newTab']),

    //添加或编辑Form
    addOrEditForm(entity) {
      if (!this.form.customerId) {
        message.error("请选择客户~");
        return
      }
      let layerId = layer.open({
        title: "请选择销售订单",
        shadeClose: false,
        closeBtn: false,
        area: ['1000px', '600px'],
        content: h(SalesOrderSelect, {
          // 传递参数到子组件
          customerId: this.customerId,  // 客户ID
          onClose: () => {
            layer.close(layerId);
          },
          onSuccess: (params) => {  // 添加参数接收
            // 处理选中的订单数据
            console.log('选中的订单数据:', params);
            // 处理你业务逻辑
            this.handleSelectedOrders(params);

            //this.doSearch();
            layer.close(layerId);
          },
        }),
      });
    },

    handleSelectedOrders(params) {
      let itemList = params.itemList;
      const unitMap = new Map(this.unitList.map(unit => [unit.id, unit]));
      itemList.forEach(row => {
        // 根据 baseUnitId 查找对应的 unitName
        const unit = unitMap.get(row.baseUnitId);
        if (unit) {
          row.unitName = unit.name;
        }
        //将id置为空，因为是新增的商品
        row.id = null;
      });
      console.log('处理后的订单数据:', itemList)
      // 将 itemList 赋值给 productData
      this.productData = itemList;
      this.selectSalesOrderIdList = params.selectSalesOrderIdList;

      // this.productData = itemList.map(item => ({
      //   ...item,
      //   //封装产品名称和产品编码，进行回显
      //   productName: item.name,
      //   productCode: item.code,
      //   // 如果需要添加或修改其他字段可以在这里处理
      // }));
    },
    //footer合计
    footerMethod({columns, data}) {
      let quantity = 0;
      let discountValue = 0;
      let subtotal = 0;
      columns.forEach((column) => {
        if (column.property && ['quantity', 'discountValue', 'subtotal'].includes(column.property)) {
          let total = 0;
          data.forEach((row) => {
            if (column.property === 'quantity') {
              let rd = row[column.property];
              if (rd) {
                quantity += Number(rd || 0);
              }
            } else if (column.property === 'discountValue') {
              let rd = row[column.property];
              if (rd) {
                discountValue += Number(rd || 0);
              }
            } else if (column.property === 'subtotal') {
              let rd = row[column.property];
              if (rd) {
                subtotal += Number(rd || 0);
              }
            }
          });
        }
      })
      this.form.orderQuantity = quantity.toFixed(2);
      this.form.discountAmount = discountValue.toFixed(2);
      this.form.finalAmount = subtotal.toFixed(2);
      this.form.totalAmount = (discountValue+subtotal).toFixed(2);
      if(!!this.form.discountAmount && !!this.form.totalAmount){
        this.form.discountRate = ((this.form.discountAmount/this.form.totalAmount)*100).toFixed(2);
      }
      console.log("subtotal",subtotal)

      return [["", "", "", "", quantity.toFixed(2), "", "", "",discountValue,subtotal,""]];
    },

    //选择商品
    selectProduct(d, index) {
      if (d) {
        let g = {
          quantity: 1,
          unitPrice: d.price || 0,
          warehouseId: this.warehousesId,
          discountValue: 0.00,
          discountRate: 0.00,
          subtotal: d.price || 0,
          baseUnitId: d.unitId,
          unitName: d.unitName,
          productId: d.id,
          productCode: d.code,
          productName: d.name,
          remark: "",
        };
        this.productData[index] = g;
        console.log("this.productData",this.productData)
        if (!this.productData[index + 1]) {
          this.productData.push({isNew: true});
        }
        this.$refs.xTable.loadData(this.productData).then(() => {
          this.$nextTick(() => {
            let str = index + '' + 3
            let element = document.querySelector('#r' + str + ' input');
            setTimeout(() => {
              element.focus()
              element.select()
            }, 100);
          })
        });
        this.showStockQuantity(g);
      }
    },

    // 仓库选择框变化时触发
    handleWarehouseChange(row) {
      this.showStockQuantity(row);
    },

    showStockQuantity(row) {
      let productId = row.productId;
      let warehouseId = row.warehouseId;
      if (!productId) {
        console.log("请选择产品")
        return;
      }
      // 获取商品库存进行提示
      let param = {
        productId: productId,
        page:1,
        pageSize:1000
      }
      Inventory.list(param).then(res => {
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
          row.currentStockQuantity = quantity;
          row.totalStockQuantity = totalQuantity;
        }
      });
    },

    checkHttp() {
      console.log("this.productData.length",this.productData.length)
      if (this.productData.length === 0) {
        message.error("请选择商品~");
        loading.close()
        return false
      }
      if (this.productData.length === 1) {
        let item = this.productData[0]
        if (item.isNew) {
          message.error("请选择商品~");
          loading.close()
          return false
        }
      }
      let quantityFlag = false
      let unitPriceFlag = false
      let subtotalFlag = false
      let warehouseFlag = false
      this.productData.map(item => {
        console.log("item",item)
        if (item.isNew) {
          return;
        }
        if (item.quantity === 0 || !item.quantity) {
          quantityFlag = true
          loading.close()
        }
        if (item.unitPrice === 0 || !item.unitPrice) {
          unitPriceFlag = true
          loading.close()
        }
        if (item.subtotal === 0 || !item.subtotal) {
          subtotalFlag = true
          loading.close()
        }
        if (!item.warehouseId) {
          warehouseFlag = true
          loading.close()
        }
      })
      if (quantityFlag) {
        message.error("请填写数量~");
        return false
      }
      if (unitPriceFlag) {
        message.error("请填写单价~");
        return false
      }
      if (subtotalFlag) {
        message.error("金额不能为空~");
        return false
      }
      if (warehouseFlag) {
        message.error("请选择仓库~");
        return false
      }
      return true
    },

    auditOrder(orderStatus){
      if (!this.form.customerId) {
        message.error("请选择客户~");
        loading.close()
        return
      }
      if (!this.checkHttp()) {
        return
      }
      confirm({
        content: `确定审核订单？`,
        onConfirm: () => {
          loading("保存中....");
          let salesOutbound = Object.assign(this.form);
          salesOutbound.orderStatus = orderStatus
          SalesOutbound.audit({
            salesOutbound: salesOutbound,
          }).then((success) => {
            if (success) {
              message("审核成功~");
              this.closeWindow()
            }
          }).finally(() =>
              loading.close()
          );
        }
      })
    },

    //保存订单
    saveOrder(saveType) {
      if (!this.form.customerId) {
        message.error("请选择客户~");
        loading.close()
        return
      }
      if (!this.checkHttp()) {
        return
      }
      confirm({
        content: `确定保存订单？`,
        onConfirm: () => {
          loading("保存中....");
          let productData = this.productData.filter(c => c.quantity > 0);
          SalesOutbound.save({
            salesOutbound: Object.assign(this.form),
            salesOutboundItemList: productData,
            selectSalesOrderIdList:this.selectSalesOrderIdList
          }).then((success) => {
            if (success) {
              message("保存成功~");
              this.clearForm();
              //保存
              if(saveType === 'save'){
                this.closeWindow()
              }

            }
          }).finally(() =>
              loading.close()
          );
        }
      })
    },

    //清除Form
    clearForm() {
      this.form = {
        id: null,
        orderDate: manba().format("YYYY-MM-dd"),
        customerId: null,
        remark: null,
        finalAmount: null
      }
      this.productData = []
      this.customerId = null
    },

    //添加行或减少行
    adjustRows(type, index) {
      if (type === 'insert') {
        this.productData.splice(index + 1, 0, {isNew: true});
      } else {
        this.productData.splice(index, 1);
      }
    },

    //修改客户
    changeCustomer(e) {
      console.log("e",e)
      if (!e) {
        this.form.customerId = null;
        this.productData = [{isNew: true}];
      } else if (e.id !== this.form.customerId) {
        if (this.productData.length > 1) {
          confirm({
            title: "系统提示",
            content: `修改供货商后，将清除已选择的商品数据，确定修改？`,
            onConfirm: () => {
              this.productData = [{isNew: true}];
              this.form.customerId = e.id;
            }
          })
        } else {
          this.form.customerId = e.id;
          this.productData = [{isNew: true}];
        }
      }
    },

    //修改商品多单位
    changeProductUnit(item, row) {
      row.orderUnitName = item.unitName
      row.unitPrice = (item.price || 0).toFixed(2) || 0
      row.num = item.num || 1;
      row.sysQuantity = (row.quantity * row.num).toFixed(2);
      row.subtotal = (row.quantity * row.unitPrice).toFixed(2);
    },

    //更新数量
    updateQuantity(item) {
      item.quantity = item.quantity || 1;
      item.subtotal = ((item.quantity * item.unitPrice * (100 - item.discountRate)) / 100).toFixed(2);
      item.discountValue = (((item.quantity * item.unitPrice) * item.discountRate) / 100).toFixed(2);
      this.$refs.xTable.updateFooter();
    },

    //更新单价
    updatePrice(item) {
      item.unitPrice = item.unitPrice || 0.00
      item.discountValue = (item.unitPrice * item.quantity * item.discountRate / 100).toFixed(2);
      item.subtotal = (item.unitPrice * item.quantity - item.discountValue).toFixed(2);
      this.$refs.xTable.updateFooter();
    },

    //更新折扣
    updateDiscount(item) {
      item.discountRate = item.discountRate || 0.00;
      item.subtotal = ((item.quantity || 0) * item.unitPrice * (100 - item.discountRate || 0) / 100).toFixed(2);
      item.discountValue = ((item.quantity || 0) * item.unitPrice - item.subtotal).toFixed(2);
      this.$refs.xTable.updateFooter();
    },

    //更新折扣金额
    updateDiscountAmount(item) {
      item.discountValue = item.discountValue || 0.00;
      item.discountRate = (((item.discountValue / (item.unitPrice * item.quantity)) * 100) || 0).toFixed(2);
      item.subtotal = (item.unitPrice * item.quantity - item.discountValue).toFixed(2);
      this.$refs.xTable.updateFooter();
    },

    //更新折后金额
    updateFinalAmount(item) {
      item.subtotal = item.subtotal || 0
      item.unitPrice = ((item.subtotal) / ((100 - item.discountRate)) * 100 / item.quantity).toFixed(2);
      item.discoutPrice = (item.unitPrice - item.subtotal).toFixed(2);
      this.$refs.xTable.updateFooter();
    },

    //关闭窗口
    closeWindow() {
      console.log("this.$store.state.currentTab", this.$store.state.currentTab)
      //this.$store.commit('closeTabKey', this.$store.state.currentTab);
      this.$store.commit('closeTabKey', this.$store.state.currentTab);
      this.$store.commit('newTab', "SalesOutboundList");
      // 使用 nextTick 确保在 DOM 更新后执行
      this.$nextTick(() => {
        // 通过 eventBus 或 vuex 触发刷新
        this.$store.commit('SET_TAB_DATA_OUTBOUND', { refresh: true });
      });
    }
  },
  beforeDestroy() {
    confirm({
      title: "系统提示",
      content: `确认?`,
      onConfirm: () => {

      }
    })
  },
  created() {
    loading("加载中....");
    Promise.all([
      Customer.select(),
      Warehouse.select(),
      Product.select(),
      Unit.select()
    ]).then((results) => {
      this.customerList = results[0].data || [];
      this.warehouseList = results[1].data || [];
      this.productList = results[2].data || [];
      this.unitList = results[3].data || [];
      console.log("this.productList", this.productList);
      this.productList.forEach(item => {
        item.name = `${item.code}--${item.name}`;
      });
      //订单详情/编辑订单
      const tabData = this.$store.state.currentTabDataOutbound;
      //清空参数
      this.$store.commit('SET_TAB_DATA_OUTBOUND', null);
      console.log("tabData", tabData)
      this.type = tabData?.type;
      this.orderId = tabData?.orderId;
      if (this.orderId) {
        SalesOutbound.getInfo(this.orderId).then(response => {
          let salesOutbound = response.data;
          console.log("response.data", salesOutbound)
          this.form = salesOutbound;
          this.customerId = salesOutbound.customerId;
          this.form.discountRate = ((this.form.discountAmount/this.form.totalAmount)*100).toFixed(2);
          console.log("this.form", this.form)
          this.productData = salesOutbound.salesOutboundItemList || [];
          this.productData.push({isNew: true});
        });
      }
    }).finally(() => loading.close());
  },
}
</script>
