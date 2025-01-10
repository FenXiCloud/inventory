<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <vxe-toolbar class-name="!size--mini">
        <template #buttons>
          <label class="mr-20px" style="font-size: 16px !important;">供货商：</label>
          <Select class="w-300px" filterable required :datas="supplierList" keyName="id" titleName="name"
                  :deletable="false" @change="changeSupplier($event)" v-model="supplierId" placeholder="请选择供货商"/>
          <label class="mr-20px ml-16px" style="font-size: 16px !important;">单据日期：</label>
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
              <Select ref="ms" @change="selectProduct($event,rowIndex)" v-model="product" :datas="productList"
                      filterable
                      placeholder="输入编码/名称" keyName="productId">
                <template v-slot:item="{ item }">
                  <div>{{ item.productCode }} {{ item.productName }}</div>
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
        <vxe-column title="采购单位" field="orderUnitName" align="center" width="80">
          <template #default="{row,rowIndex}">
            <template v-if="!row.isNew">
              <Select v-if="row.auxiliaryUnitPrices" :deletable="false" @change="changeProductUnit($event,row)"
                      v-model="row.orderUnitId" :datas="row.auxiliaryUnitPrices" filterable placeholder="输入采购单位"
                      keyName="unitId" titleName="unitName"/>
              <span v-else>{{ row.orderUnitName }}</span>
            </template>
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
            <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+3" @keyup="handleEnter($event,rowIndex,3)"
                       @blur="updateQuantity(row)" ref="inputQuantity" v-model.number="row.orderQuantity" type="float"
                       min="0" :controls="false"></vxe-input>
          </template>
        </vxe-column>
        <vxe-column title="基本单位" field="unitName" align="center" width="80"/>
        <vxe-column title="基本数量" field="sysQuantity" width="90"/>
        <vxe-column title="购货单价" field="orderPrice" width="100">
          <template #default="{row,rowIndex}">
            <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+4" @keyup="handleEnter($event,rowIndex,4)"
                       @blur="updatePrice(row)" v-model.number="row.orderPrice" type="float" min="0"
                       :controls="false"></vxe-input>
          </template>
        </vxe-column>
        <vxe-column title="折扣率(%)" field="discount" width="100">
          <template #default="{row,rowIndex}">
            <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+5" @keyup="handleEnter($event,rowIndex,5)"
                       @blur="updateDiscount(row)" v-model.number="row.discount" type="float" min="0"
                       :controls="false"></vxe-input>
          </template>
        </vxe-column>
        <vxe-column title="折扣额" field="discountAmount" width="100">
          <template #default="{row,rowIndex}">
            <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+6" @keyup="handleEnter($event,rowIndex,6)"
                       @blur="updateDiscountAmount(row)" v-model.number="row.discountAmount" type="float" min="0"
                       :controls="false"></vxe-input>
          </template>
        </vxe-column>
        <vxe-column title="购货金额" field="finalAmount" width="100">
          <template #default="{row,rowIndex}">
            <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+7" @keyup="handleEnter($event,rowIndex,7)"
                       @blur="updateFinalAmount(row)" v-model.number="row.finalAmount" type="float" min="0"
                       :controls="false"></vxe-input>
          </template>
        </vxe-column>
        <vxe-column title="备注" field="remark">
          <template #default="{row,rowIndex}">
            <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+8" @keyup="handleEnter($event,rowIndex,8)"
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
          <label class="mr-16px  w-80px">优惠率：</label>
          <Input v-model="form.discountRate"/>
          <label class="ml-10px mr-16px  w-80px">优惠金额：</label>
          <Input v-model="form.discountAmount"/>
          <label class="ml-16px mr-16px  w-100px">优惠后金额：</label>
          <Input v-model="form.finalAmount"/>
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
import PurchaseOrder from "@js/api/purchase/PurchaseOrder";
import Supplier from "@js/api/basic/Supplier";
import Warehouse from "@js/api/basic/Warehouse";
import {mapState} from "vuex";

export default {
  name: "PurchaseOrderForm",
  props: {
    orderId: [String, Number],
    type: String,
  },
  computed: {
    ...mapState(['accountBook']),
    finalAmount() {
      let total = 0;
      this.productData.forEach(val => {
        if (val.sysQuantity > 0) {
          total += parseFloat(val.finalAmount);
        }
      });
      return total.toFixed(2);
    },
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
      supplierList: [],
      supplierId: null,
      warehouseId: null,
      form: {
        id: null,
        orderDate: manba().format("YYYY-MM-dd"),
        supplierId: null,
        discountAmount: 0.00,
        discountRate: 0.00,
        finalAmount: 0.00,
        remarks: null,
      },
      productData: [],
    }
  },
  methods: {
    handleEnter(e, index, num) {
      e.$event.stopPropagation();
      if (e.$event.keyCode === 13) {
        //回车
        e.$input.blur()
        if (num === 8) {
          if (index >= this.productData.length - 2) {
            this.$refs.ms.$el.querySelector('input').click()
            this.$refs.ms.$el.querySelector('input').select()
          } else {
            let str = 'r' + (index + 1) + '' + 3
            document.getElementById(str).querySelector('input').focus()
            document.getElementById(str).querySelector('input').select()
          }
        } else {
          let str = 'r' + index + '' + (num + 1)
          document.getElementById(str).querySelector('input').focus()
          document.getElementById(str).querySelector('input').select()
        }
      } else if (e.$event.keyCode === 38) {
        //按上
        if (index > 0) {
          e.$input.blur()
          let str = 'r' + (index - 1) + '' + num
          document.getElementById(str).querySelector('input').focus()
          document.getElementById(str).querySelector('input').select()
        }
      } else if (e.$event.keyCode === 40) {
        //按下
        e.$input.blur()
        if (index < this.productData.length - 2) {
          let str = 'r' + (index + 1) + '' + num
          document.getElementById(str).querySelector('input').focus()
          document.getElementById(str).querySelector('input').select()
        } else {
          this.$refs.ms.$el.querySelector('input').click()
          this.$refs.ms.$el.querySelector('input').select()
        }
      }
    },

    //footer合计
    footerMethod({columns, data}) {
      let sums = [];
      let sysQuantity = 0;
      columns.forEach((column) => {
        if (column.property && ['sysQuantity', 'discountAmount', 'finalAmount'].includes(column.property)) {
          let total = 0;
          data.forEach((row) => {
            if (column.property === 'sysQuantity') {
              let rd = row[column.property];
              if (rd) {
                sysQuantity += Number(rd || 0);
              }
            } else {
              let rd = row[column.property];
              if (rd) {
                total += Number(rd || 0);
              }
            }
          });
          if (column.property !== 'sysQuantity') {
            sums.push(total.toFixed(2));
          }
        }
      })
      return [["", "", "", "", "", "", "", sysQuantity.toFixed(2), "", ""].concat(sums)];
    },

    //选择商品
    selectProduct(d, index) {
      if (d) {
        let g = {
          sysQuantity: 1,
          orderQuantity: 1,
          orderPrice: d.price || 0,
          warehouseId: this.warehousesId,
          price: d.price || 0,
          discountAmount: 0.00,
          discount: 0.00,
          finalAmount: d.price || 0,
          num: 1,
          orderUnitId: d.unitId,
          orderUnitName: d.unitName,
          remark: ""
        };
        this.productData[index] = Object.assign(Object.assign(g, d), d);
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
      if (!this.form.supplierId) {
        message.error("请选择购货商~");
        loading.close()
        return
      }
      let productData = this.productData.filter(c => c.sysQuantity > 0);
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
      PurchaseOrder.save({
        order: Object.assign(this.form, {finalAmount: this.finalAmount}),
        type: this.type,
        detailList: productData
      }).then((success) => {
        if (success) {
          message("保存成功~");
          this.clear()
        }
      }).finally(() =>
          loading.close());
    },

    //清除Form
    clearForm() {
      this.form = {
        id: null,
        orderDate: manba().format("YYYY-MM-dd"),
        supplierId: null,
        remark: null,
        finalAmount: null
      }
      this.productData = []
      this.supplierId = null
    },

    //添加行或减少行
    adjustRows(type, index) {
      if (type === 'insert') {
        this.productData.splice(index + 1, 0, {isNew: true});
      } else {
        this.productData.splice(index, 1);
      }
    },

    //修改供货商
    changeSupplier(e) {
      if (!e) {
        this.form.supplierId = null;
        this.productData = [{isNew: true}];
      } else if (e.id !== this.form.supplierId) {
        if (this.productData.length > 1) {
          confirm({
            title: "系统提示",
            content: `修改供货商后，将清除已选择的商品数据，确定修改？`,
            onConfirm: () => {
              this.productData = [{isNew: true}];
              this.form.supplierId = e.id;
              this.loadProductsBySupplier();
            }
          })
        } else {
          this.form.supplierId = e.id;
          this.productData = [{isNew: true}];
          this.loadProductsBySupplier();
        }
      }

    },

    //根据供货商加载商品列表
    loadProductsBySupplier() {
      if (this.form.supplierId) {
        Supplier.selectProducts(this.form.supplierId).then(({data}) => {
          this.productList = data || [];
          if (!this.form.id) {
            this.productData = [{isNew: true}];
          }
        }).finally(() =>
            this.$refs.xTable.loadData(this.productData).then(() => {
              this.$nextTick(() => {
                this.$refs.ms.$el.querySelector('input').click()
                this.$refs.ms.$el.querySelector('input').select()
              })
              this.products = null;
            })
        );
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
      item.sysQuantity = (item.orderQuantity * (item.num || 1)).toFixed(2);
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
      Supplier.select(),
      Warehouse.select()
    ]).then((results) => {
      this.supplierList = results[0].data || [];
      this.warehouseList = results[1].data || [];
      if (this.warehouseList != null) {
        this.warehouseId = this.warehouseList.find(val => val.isDefault).id
      }
      //订单详情/编辑订单
      if (this.orderId) {
        PurchaseOrder.load(this.orderId).then(({data: {order, productData}}) => {
          if (order) {
            CopyObj(this.form, order);
            this.supplierId = order.supplierId
            if ('copy' === this.type) {
              this.form.id = null;
            }
          }
          this.loadProductsBySupplier();
          this.productData = productData || [];
          this.productData.push({isNew: true});
        })
      }
    }).finally(() => loading.close());
  },
}
</script>
