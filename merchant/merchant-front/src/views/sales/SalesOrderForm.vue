<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <vxe-toolbar class-name="!size--mini">
        <template #buttons>
          <label class="mr-20px" style="font-size: 16px !important;">客户:</label>
          <Select class="w-300px" filterable required :datas="customerList" keyName="id" titleName="name"
                  :deletable="false" @change="changeCustomer($event)" v-model="customerId" placeholder="请选择客户"/>
          <label class="mr-20px ml-16px" style="font-size: 16px !important;">单据日期:</label>
          <DatePicker v-model="form.orderDate" :option="{start:accountBook.checkoutDate}"
                      :clearable="false"></DatePicker>
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
        <vxe-column title="商品信息" width="300">
          <template #default="{row,rowIndex}">
            <div class="h-input-group goodsSelect" v-if="row.isNew" @keyup.stop="void(0)">
              <Select ref="ms" @change="selectProduct($event,rowIndex)" :datas="productList" v-model="product"
                      keyName="id" titleName="name" filterable placeholder="输入编码/名称">
                <template v-slot:item="{ item }">
                  <div>{{ item.code }} {{ item.name }}</div>
                </template>
              </Select>
            </div>
            <div v-else class="flex">
              <div class="flex1 ml-8px">
                <div>{{ row.productCode }}--{{ row.productName }}</div>
              </div>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="仓库" field="warehouse" align="center" width="120">
          <template #default="{row,rowIndex}">
            <template v-if="!row.isNew">
              <Select :deletable="false" v-model="row.warehouseId" :datas="warehouseList" filterable keyName="id"
                      titleName="name"/>
            </template>
          </template>
        </vxe-column>
        <vxe-column title="数量" field="orderQuantity" width="90">
          <template #default="{row,rowIndex,columnIndex}">
            <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+3"
                       @blur="updateQuantity(row)" ref="inputQuantity" v-model.number="row.orderQuantity" type="float"
                       min="0" :controls="false"></vxe-input>
          </template>
        </vxe-column>
        <vxe-column title="单位" field="unitName" align="center" width="80"/>
        <vxe-column title="单价" field="orderPrice" width="100">
          <template #default="{row,rowIndex}">
            <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+4"
                       @blur="updatePrice(row)" v-model.number="row.orderPrice" type="float" min="0"
                       :controls="false"></vxe-input>
          </template>
        </vxe-column>
        <vxe-column title="折扣率(%)" field="discount" width="100">
          <template #default="{row,rowIndex}">
            <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+5"
                       @blur="updateDiscount(row)" v-model.number="row.discount" type="float" min="0"
                       :controls="false"></vxe-input>
          </template>
        </vxe-column>
        <vxe-column title="折扣额" field="discountAmount" width="100">
          <template #default="{row,rowIndex}">
            <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+6"
                       @blur="updateDiscountAmount(row)" v-model.number="row.discountAmount" type="float" min="0"
                       :controls="false"></vxe-input>
          </template>
        </vxe-column>
        <vxe-column title="金额" field="finalAmount" width="100">
          <template #default="{row,rowIndex}">
            <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+7"
                       @blur="updateFinalAmount(row)" v-model.number="row.finalAmount" type="float" min="0"
                       :controls="false"></vxe-input>
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
      <div class="filler-panel">
        <div class="filler-item" style="flex: 1;margin: 5px 0 !important;">
          <label class="mr-16px  w-80px">备注说明：</label>
          <Input placeholder="请输入备注" maxlength="150" style="width: 90%" v-model="form.remarks"/>
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item" style="flex: 1;margin: 5px 0 !important;">
          <label class="mr-16px  w-100px">优惠率(%)：</label>
          <Input v-model="form.discountRate" readonly/>
          <label class="ml-10px mr-16px  w-80px">优惠金额：</label>
          <Input v-model="form.discountAmount" readonly/>
          <label class="ml-16px mr-16px  w-100px">优惠后金额：</label>
          <Input v-model="form.finalAmount" readonly/>
        </div>
      </div>
    </div>
    <div class="modal-column-between bg-white-color  border">

      <Button @click="closeWindow" :loading="loading">
        取消
      </Button>
      <div>
        <Button color="primary" @click="saveOrder" :loading="loading">
          保存并新增
        </Button>
        <Button @click="saveOrder" :loading="loading">
          保存
        </Button>
        <!-- 当状态为已审核时不显示,审核后订单上显示已审核图片 -->
        <Button @click="saveOrder" :loading="loading">
          审核
        </Button>
        <!-- 仅当状态为审核时显示 -->
        <Button @click="saveOrder" :loading="loading">
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
import {mapState} from "vuex";
import SalesOrder from "@js/api/sales/SalesOrder";
import Product from "@js/api/basic/Product";

export default {
  name: "PurchaseOrderForm",
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
      product: null,
      warehouseList: [],
      customerList: [],
      customerId: null,
      warehouseId: null,
      form: {
        id: null,
        orderDate: manba().format("YYYY-MM-dd"),
        customerId: null,
        discountAmount: 0.00,
        discountRate: 0.00,
        finalAmount: 0.00,
        remarks: null,
      },
      productData: [],
      orderId:null,
      type:null,
    }
  },
  methods: {
    //footer合计
    footerMethod({columns, data}) {
      let orderQuantity = 0;
      let discountAmount = 0;
      let finalAmount = 0;
      columns.forEach((column) => {
        if (column.property && ['orderQuantity', 'discountAmount', 'finalAmount'].includes(column.property)) {
          let total = 0;
          data.forEach((row) => {
            if (column.property === 'orderQuantity') {
              let rd = row[column.property];
              if (rd) {
                orderQuantity += Number(rd || 0);
              }
            } else if (column.property === 'discountAmount') {
              let rd = row[column.property];
              if (rd) {
                discountAmount += Number(rd || 0);
              }
            } else if (column.property === 'finalAmount') {
              let rd = row[column.property];
              if (rd) {
                finalAmount += Number(rd || 0);
              }
            }
          });
        }
      })
      this.form.orderQuantity = orderQuantity;
      this.form.discountAmount = discountAmount;
      this.form.finalAmount = finalAmount;
      this.form.totalAmount = discountAmount+finalAmount;
      this.form.discountRate = (discountAmount/this.form.totalAmount)*100;

      return [["", "", "", "", orderQuantity.toFixed(2), "", "", "",discountAmount,finalAmount,""]];
    },

    //选择商品
    selectProduct(d, index) {
      if (d) {
        let g = {
          orderQuantity: 1,
          orderPrice: d.price || 0,
          warehouseId: this.warehousesId,
          discountAmount: 0.00,
          discount: 0.00,
          finalAmount: d.price || 0,
          unitId: d.unitId,
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
      }
      this.product = null;
    },

    //保存订单
    saveOrder() {
      loading("保存中....");
      if (!this.form.customerId) {
        message.error("请选择客户~");
        loading.close()
        return
      }
      let productData = this.productData.filter(c => c.orderQuantity > 0);
      if (productData.length <= 0) {
        message.error("请选择商品~");
        loading.close()
        return
      }
      let warehouse = this.productData.filter(c => c.warehouseId === null);
      if (warehouse.length > 0) {
        message.error("请选择仓库~");
        loading.close()
        return
      }
      this.productData.forEach((item, index) => {
        //单价
        item.unitPrice = item.orderPrice;
        //基础单位id
        item.baseUnitId = item.unitId;
        //折扣金额
        item.discountValue = item.discountAmount;
        //数量
        item.quantity = item.finalAmount;
        //小计
        item.subtotal = item.finalAmount;
      })
      SalesOrder.save({
        salesOrder: Object.assign(this.form),
        salesOrderItemList: productData
      }).then((success) => {
        if (success) {
          message("保存成功~");
          this.clearForm()
        }
      }).finally(() =>
          loading.close());
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
      row.orderPrice = (item.price || 0).toFixed(2) || 0
      row.num = item.num || 1;
      row.sysQuantity = (row.orderQuantity * row.num).toFixed(2);
      row.finalAmount = (row.orderQuantity * row.orderPrice).toFixed(2);
    },

    //更新数量
    updateQuantity(item) {
      item.orderQuantity = item.orderQuantity || 1;
      item.finalAmount = ((item.orderQuantity * item.orderPrice * (100 - item.discount)) / 100).toFixed(2);
      item.discountAmount = (((item.orderQuantity * item.orderPrice) * item.discount) / 100).toFixed(2);
      this.$refs.xTable.updateFooter();
    },

    //更新单价
    updatePrice(item) {
      item.orderPrice = item.orderPrice || 0.00
      item.discountAmount = (item.orderPrice * item.orderQuantity * item.discount / 100).toFixed(2);
      item.finalAmount = (item.orderPrice * item.orderQuantity - item.discountAmount).toFixed(2);
      this.$refs.xTable.updateFooter();
    },

    //更新折扣
    updateDiscount(item) {
      item.discount = item.discount || 0.00;
      item.finalAmount = ((item.orderQuantity || 0) * item.orderPrice * (100 - item.discount || 0) / 100).toFixed(2);
      item.discountAmount = ((item.orderQuantity || 0) * item.orderPrice - item.finalAmount).toFixed(2);
      this.$refs.xTable.updateFooter();
    },

    //更新折扣金额
    updateDiscountAmount(item) {
      item.discountAmount = item.discountAmount || 0.00;
      item.discount = (((item.discountAmount / (item.orderPrice * item.orderQuantity)) * 100) || 0).toFixed(2);
      item.finalAmount = (item.orderPrice * item.orderQuantity - item.discountAmount).toFixed(2);
      this.$refs.xTable.updateFooter();
    },

    //更新折后金额
    updateFinalAmount(item) {
      item.finalAmount = item.finalAmount || 0
      item.orderPrice = ((item.finalAmount) / ((100 - item.discount)) * 100 / item.orderQuantity).toFixed(2);
      item.discoutPrice = (item.orderPrice - item.finalAmount).toFixed(2);
      this.$refs.xTable.updateFooter();
    },

    //关闭窗口
    closeWindow() {
      let cache = localStorage.getItem("SYS_TABS");
      let tagList = cache ? JSON.parse(cache) : [];
      if (tagList) {
        let index = tagList.findIndex(val => val.name === "NewPurchaserOrder")
        tagList.splice(index, 1);
        let newRoute;
        if (tagList.length > 0) {
          newRoute = tagList[index - 1];
        } else {
          this.$router.push({name: 'DashboardMain'});
        }
        if (newRoute) this.$router.replace(newRoute);
        localStorage.setItem("SYS_TABS", JSON.stringify(newRoute))
      }
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
      Product.select()
    ]).then((results) => {
      this.customerList = results[0].data || [];
      this.warehouseList = results[1].data || [];
      this.productList = results[2].data || [];
      console.log("this.productList", this.productList);
      //订单详情/编辑订单
      const tabData = this.$store.state.currentTabData;
      console.log("tabData", tabData)
      this.type = tabData?.type;
      this.orderId = tabData?.orderId;
      if (this.orderId) {
        SalesOrder.getInfo(this.orderId).then(response => {
          let salesOrder = response.data;
          console.log("response.data", salesOrder)
          this.form = salesOrder;
          this.customerId = salesOrder.customerId;
          this.form.discountRate = (this.form.discountAmount/this.form.totalAmount)*100;
          console.log("this.form", this.form)
          this.productData = salesOrder.salesOrderItemList || [];
          this.productData.push({isNew: true});
        });
      }
    }).finally(() => loading.close());
  },
}
</script>
