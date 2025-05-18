<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <vxe-toolbar class-name="!size--mini">
        <template #buttons>
          <label class="mr-20px" style="font-size: 16px !important;">供货商：</label>
          <Select class="w-300px" filterable required :datas="supplierList" keyName="id" titleName="name"
                  :deletable="false" @change="changeSupplier($event)" v-model="supplierId" placeholder="请选择供货商"/>
          <label class="mr-20px ml-16px" style="font-size: 16px !important;">退货日期：</label>
          <DatePicker v-model="form.returnDate" :option="{start:accountBook.checkoutDate}"
                      :clearable="false"></DatePicker>
          <Button v-if="type==='add'" @click="selectPurchaseOrder()" color="primary" style="margin-left: 20px">
            选择源单
          </Button>
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
            <div class="fa fa-minus text-hover" v-if="isDeleting" @click="adjustRows('delete',rowIndex)"></div>
          </template>
        </vxe-column>
        <vxe-column field="imgPath" title="商品图片" width="100">
          <template #default="{row}">
            <img
                :src="productList.find(item => item.id === row.productId)?.imgPath || '-'"
                alt=""
                class="product-img cursor-pointer"
                @click="previewImage(productList.find(item => item.id === row.productId)?.imgPath)">
          </template>
        </vxe-column>
        <vxe-column title="商品信息" width="200">
          <template #default="{row,rowIndex}">
            <div class="flex">
              <div class="flex1 ml-8px">
                <div>{{ row.productCode }}--{{ row.productName }}</div>
              </div>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="商品类别" field="categoryName" align="center" width="80"/>
        <vxe-column title="规格型号" field="spec" align="center" width="80"/>
        <vxe-column title="采购单位" field="secondaryUnitName" align="center" width="80">
          <template #default="{row,rowIndex}">
            <template v-if="!row.isNew">
              <Select v-if="row.auxiliaryUnitPrices" :deletable="false" @change="changeProductUnit($event,row)"
                      v-model="row.secondaryUnitId" :datas="row.auxiliaryUnitPrices" filterable
                      placeholder="输入采购单位"
                      keyName="unitId" titleName="unitName"/>
              <span v-else>{{ row.secondaryUnitName }}</span>
            </template>
          </template>
        </vxe-column>
        <vxe-column title="仓库" field="warehouse" align="center" width="120">
          <template #default="{row,rowIndex}">
            <template v-if="!row.isNew">
              <Select :deletable="false" v-model="row.warehouseId" :datas="warehouseList" filterable keyName="id"
                      titleName="name" @change="handleWarehouseChange(row, $event)"/>
            </template>
          </template>
        </vxe-column>
        <vxe-column title="退货数量" field="secondaryQuantity" width="90">
          <template #default="{row,rowIndex,columnIndex}">
            <template v-if="!row.isNew">
              <vxe-tooltip theme="light" v-if="!row.isNew">
                <template #content>
                  <div>当前库存: {{ row.currentStockQuantity || 0 }}</div>
                  <div>总库存: {{ row.totalStockQuantity || 0 }}</div>
                </template>
                <vxe-input
                    :id="'r'+rowIndex+''+3"
                    @blur="updateQuantity(row)"
                    @focus="showStockQuantity(row)"
                    ref="inputQuantity"
                    v-model.number="row.secondaryQuantity"
                    type="float"
                    min="0"
                    :max="row.returnQuantity"
                    :controls="false">
                </vxe-input>
              </vxe-tooltip>
            </template>
          </template>
        </vxe-column>
        <vxe-column title="基本单位" field="baseUnitName" align="center" width="80"/>
        <vxe-column title="基本数量" field="quantity" width="90"/>
        <vxe-column title="购货单价" field="secondaryPrice" width="100">
          <template #default="{row,rowIndex}">
            <template v-if="!row.isNew">
              <vxe-tooltip theme="light" v-if="!row.isNew">
                <template #content>
                  <div class="recent-sales-table">
                    <table>
                      <thead>
                      <tr>
                        <th>最近采购时间</th>
                        <th>最近采购价</th>
                        <th>供货商</th>
                      </tr>
                      </thead>
                      <tbody>
                      <tr v-for="(item, index) in recentSales || []" :key="index">
                        <td>{{ item.orderDate || '-' }}</td>
                        <td>{{ item.unitPrice || '-' }}</td>
                        <td>{{ item.supplierName || '-' }}</td>
                      </tr>
                      </tbody>
                    </table>
                  </div>
                </template>
                <vxe-input :id="'r'+rowIndex+''+4" @keyup="handleEnter($event,rowIndex,4)"
                           @blur="updatePrice(row)"
                           @focus="showPrice(row.productId)"
                           v-model.number="row.secondaryPrice" type="float" min="0"
                           :controls="false"></vxe-input>
              </vxe-tooltip>
            </template>
          </template>
        </vxe-column>
        <vxe-column title="折扣率(%)" field="discountRate" width="100">
          <template #default="{row,rowIndex}">
            <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+5" @keyup="handleEnter($event,rowIndex,5)"
                       @blur="updateDiscount(row)" v-model.number="row.discountRate" type="float" min="0"
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
        <vxe-column title="退货金额" field="subtotal" width="100">
          <template #default="{row,rowIndex}">
            <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+7" @keyup="handleEnter($event,rowIndex,7)"
                       @blur="updateSubtotal(row)" v-model.number="row.subtotal" type="float" min="0"
                       :controls="false"></vxe-input>
          </template>
        </vxe-column>
        <vxe-column title="备注" field="returnReason" width="160">
          <template #default="{row,rowIndex}">
            <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+8" @keyup="handleEnter($event,rowIndex,8)"
                       v-model="row.returnReason" placeholder="输入备注" :controls="false"></vxe-input>
          </template>
        </vxe-column>
        <vxe-column title="关联采购入库号" field="purchaseInboundOrderNo" align="center" width="180"/>
      </vxe-table>
      <div class="mt-10px"></div>
      <div class="filler-panel">
        <div class="filler-item" style="flex: 1;margin: 5px 0 !important;">
          <label class="mr-16px  w-80px">退货原因：</label>
          <Input placeholder="请输入退货原因" maxlength="150" style="width: 90%" v-model="form.returnReason"/>
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item" style="flex: 1;margin: 5px 0 !important;">
          <label class="mr-16px  w-80px">备注说明：</label>
          <Input placeholder="请输入备注" maxlength="150" style="width: 90%" v-model="form.remarks"/>
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item" style="flex: 1;margin: 5px 0 !important;">
          <label class="mr-16px  w-80px">优惠率：</label>
          <Input v-model="form.discountRate" @blur="changeDiscountRate"/>
          <label class="ml-10px mr-16px  w-80px">优惠金额：</label>
          <Input v-model="form.discountAmount" @blur="changeDiscountAmount"/>
          <!--          <label class="ml-10px mr-16px  w-100px">供货商承担：</label>-->
          <!--          <Input v-model="form.supplierAmount" @blur="changeSupplierAmount"/>-->
          <label class="ml-16px mr-16px  w-100px">本次退款：</label>
          <Input v-model="form.refundAmount" @blur="changeRefundAmount"/>
        </div>
      </div>
    </div>
    <div class="modal-column-between bg-white-color  border">
      <Button @click="closeWindow" :loading="loading">
        取消
      </Button>
      <div>
        <Button color="primary" @click="saveOrder('add')" :loading="loading">
          保存并新增
        </Button>
        <Button @click="saveOrder(type='save')" :loading="loading">
          保存
        </Button>
        <!-- 当状态为已审核时不显示,审核后订单上显示已审核图片 -->
        <Button @click="approved" :loading="loading" v-if="form.id">
          审核
        </Button>
      </div>
    </div>
  </div>
</template>
<script>

import {confirm, loading, message} from "heyui.ext";
import manba from "manba";
import {CopyObj} from "@common/utils";
import Supplier from "@js/api/basic/Supplier";
import Warehouse from "@js/api/basic/Warehouse";
import {mapState} from "vuex";
import {layer} from "@layui/layer-vue";
import {h} from "vue";
import PurchaseReturnOrderSelect from "@views/purchase/PurchaseReturnOrderSelect.vue";
import PurchaseReturn from "@js/api/purchase/PurchaseReturn";
import PurchaseInbound from "@js/api/purchase/PurchaseInbound";
import PriceRecord from "@js/api/basic/PriceRecord";
import Inventory from "@js/api/inventory/Inventory";

export default {
  name: "PurchaseReturnForm",
  props: {
    orderId: [String, Number],
    type: String,
  },
  computed: {
    ...mapState(['accountBook']),
    refundAmount() {
      let total = 0;
      this.productData.forEach(val => {
        if (val.quantity > 0) {
          total += parseFloat(val.subtotal);
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
      allRefundAmount: 0,
      warehouseList: [],
      supplierList: [],
      inboundIds: [],
      supplierId: null,
      warehouseId: null,
      form: {
        id: null,
        returnDate: manba().format("YYYY-MM-dd"),
        supplierId: null,
        discountAmount: 0.00,
        discountRate: 0.00,
        refundAmount: 0.00,
        remarks: null,
        returnReason: null,
      },
      productData: [],
      recentSales: [],
    }
  },
  watch: {
    allRefundAmount(val) {
      this.form.discountAmount = (val * this.form.discountRate * 0.01).toFixed(2)
      this.form.refundAmount = (val - this.form.discountAmount).toFixed(2)
    }
  },
  methods: {
    //添加行或减少行
    adjustRows(type, index) {
      if (type === 'insert') {
        this.productData.splice(index + 1, 0, {isNew: true});
      } else {
        this.productData.splice(index, 1);
      }
    },
    selectPurchaseOrder(entity) {
      if (!this.form.supplierId) {
        message.error("请选择供货商~");
        return
      }
      let layerId = layer.drawer({
        title: "请选择采购入库单",
        shadeClose: false,
        closeBtn: false,
        area: ['1200px', '100vh'],
        content: h(PurchaseReturnOrderSelect, {
          supplierId: this.supplierId,
          onClose: () => {
            layer.close(layerId);
          },
          onSuccess: (params) => {
            console.log(params)
            this.loadToInbound(params);
            layer.close(layerId);
          },
        }),
      });
    },


    showPrice(productId) {
      if (!productId) {
        console.log("请选择产品")
        return;
      }
      // 获取商品库存进行提示
      let param = {
        productId: productId,
      }
      PriceRecord.showPurchasePrice(param).then(({data}) => {
        this.recentSales = data || [];
        console.log(this.recentSales)
      }).finally(() => this.loading = false);
    },

    loadToInbound(params) {
      this.inboundIds = params.orderIds
      PurchaseInbound.toReturn(this.form.supplierId, params.orderIds).then(({data}) => {
        this.productData = data || [];
      })
    },
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
      let quantity = 0;
      columns.forEach((column) => {
        if (column.property && ['quantity', 'discountAmount', 'subtotal'].includes(column.property)) {
          let total = 0;
          data.forEach((row) => {
            if (column.property === 'quantity') {
              let rd = row[column.property];
              if (rd) {
                quantity += Number(rd || 0);
              }
            } else {
              let rd = row[column.property];
              if (rd) {
                total += Number(rd || 0);
              }
            }
          });
          if (column.property !== 'quantity') {
            sums.push(total.toFixed(2));
          }
        }
      })
      this.allRefundAmount = sums[1]
      return [["", "", "", "", "", "", "", quantity.toFixed(2), "", ""].concat(sums)];
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
        page: 1,
        pageSize: 1000
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

    //保存订单
    saveOrder(type) {
      loading("保存中....");
      if (!this.form.supplierId) {
        message.error("请选择购货商~");
        loading.close()
        return
      }
      let productData = this.productData.filter(c => c.quantity > 0);
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
      PurchaseReturn.save({
        purchaseReturn: Object.assign(this.form, {refundTotalAmount: this.allRefundAmount}),
        type: this.type,
        inboundIds: this.inboundIds,
        purchaseReturnItemList: productData
      }).then((success) => {
        if (success) {
          message("保存成功~");
          this.clearForm()
          //保存
          if (type === 'save') {
            this.closeWindow()
          }
        }
      }).finally(() =>
          loading.close());
    },

    //清除Form
    clearForm() {
      this.form = {
        id: null,
        returnDate: manba().format("YYYY-MM-dd"),
        supplierId: null,
        remark: null,
        discountAmount: 0.00,
        discountRate: 0.00,
        refundAmount: 0.00,
      }
      this.allRefundAmount = 0
      this.productData = []
      this.supplierId = null
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
        }
      }
    },

    //修改优惠率
    changeDiscountRate() {
      this.form.discountRate = parseFloat(this.form.discountRate) || 0;
      this.form.discountAmount = (this.allRefundAmount * this.form.discountRate * 0.01).toFixed(2)
      this.form.refundAmount = (this.allRefundAmount - this.form.discountAmount).toFixed(2)
    },
    //修改优惠金额
    changeDiscountAmount() {
      this.form.discountAmount = parseFloat(this.form.discountAmount) || 0;
      this.form.refundAmount = (this.allRefundAmount - this.form.discountAmount).toFixed(2)
      this.form.discountRate = this.form.discountAmount === 0 ? 0 : ((this.form.discountAmount / this.allRefundAmount) * 100).toFixed(2)
    },
    //修改优惠后金额
    changeRefundAmount() {
      this.form.refundAmount = parseFloat(this.form.refundAmount) || 0;
      this.form.discountAmount = (this.allRefundAmount - this.form.refundAmount).toFixed(2)
      this.form.discountRate = this.form.discountAmount === 0 ? 0 : ((this.form.discountAmount / this.allRefundAmount) * 100).toFixed(2)
    },
    //修改商品多单位
    changeProductUnit(item, row) {
      row.secondaryUnitName = item.unitName
      row.secondaryPrice = (item.price || 0).toFixed(2) || 0
      row.conversionRate = item.conversionRate || 1;
      row.quantity = (row.secondaryQuantity * row.conversionRate).toFixed(2);
      row.subtotal = (row.secondaryQuantity * row.secondaryPrice).toFixed(2);
    },

    //更新数量
    updateQuantity(item) {
      item.secondaryQuantity = item.secondaryQuantity || 1;
      item.subtotal = ((item.secondaryQuantity * item.secondaryPrice * (100 - item.discountRate)) / 100).toFixed(2);
      item.discountAmount = (((item.secondaryQuantity * item.secondaryPrice) * item.discountRate) / 100).toFixed(2);
      item.quantity = (item.secondaryQuantity * (item.conversionRate || 1)).toFixed(2);
      this.$refs.xTable.updateFooter();
    },

    //更新单价
    updatePrice(item) {
      item.secondaryPrice = item.secondaryPrice || 0.00
      item.discountAmount = (item.secondaryPrice * item.secondaryQuantity * item.discountRate / 100).toFixed(2);
      item.subtotal = (item.secondaryPrice * item.secondaryQuantity - item.discountAmount).toFixed(2);
      this.$refs.xTable.updateFooter();
    },

    //更新折扣
    updateDiscount(item) {
      item.discountRate = item.discountRate || 0.00;
      item.subtotal = ((item.secondaryQuantity || 0) * item.secondaryPrice * (100 - item.discountRate || 0) / 100).toFixed(2);
      item.discountAmount = ((item.secondaryQuantity || 0) * item.secondaryPrice - item.subtotal).toFixed(2);
      this.$refs.xTable.updateFooter();
    },

    //更新折扣金额
    updateDiscountAmount(item) {
      item.discountAmount = item.discountAmount || 0.00;
      item.discountRate = (((item.discountAmount / (item.secondaryPrice * item.secondaryQuantity)) * 100) || 0).toFixed(2);
      item.subtotal = (item.secondaryPrice * item.secondaryQuantity - item.discountAmount).toFixed(2);
      this.$refs.xTable.updateFooter();
    },

    //更新折后金额
    updateSubtotal(item) {
      item.subtotal = item.subtotal || 0
      item.secondaryPrice = ((item.subtotal) / ((100 - item.discountRate)) * 100 / item.secondaryQuantity).toFixed(2);
      item.discoutPrice = (item.secondaryPrice - item.subtotal).toFixed(2);
      this.$refs.xTable.updateFooter();
    },

    //审核
    approved() {
      let ids = [this.form.id]
      confirm({
        title: "审核提示",
        content: `确认审核该订单?`,
        onConfirm: () => {
          PurchaseReturn.approved('已审核', ids).then(() => {
            message("操作成功~");
            this.closeWindow();
          })
        }
      })
    },

    //关闭窗口
    closeWindow() {
      console.log("this.$store.state.currentTab", this.$store.state.currentTab)
      this.$store.commit('closeTabKey', this.$store.state.currentTab);
      this.$store.commit('newTab', "PurchaseReturnList");
      // 使用 nextTick 确保在 DOM 更新后执行
      this.$nextTick(() => {
        // 通过 eventBus 或 vuex 触发刷新
        this.$store.commit('SET_TAB_DATA', {refresh: true});
      });
    },
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
        this.warehouseId = this.warehouseList.find(val => val.isDefault)?.id
      }
      //订单详情/编辑订单
      if (this.orderId) {
        PurchaseReturn.load(this.orderId).then(({data: {purchaseReturn, purchaseReturnItemList}}) => {
          if (purchaseReturn) {
            CopyObj(this.form, purchaseReturn);
            this.supplierId = purchaseReturn.supplierId
            if ('copy' === this.type) {
              this.form.id = null;
            }
          }
          this.productData = purchaseReturnItemList || [];
        })
      }
    }).finally(() => loading.close());
  },
}
</script>
<style scoped>

.recent-sales-table {
  min-width: 300px;
}

.recent-sales-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  border: 1px solid #dfe6ec;
}

.recent-sales-table th,
.recent-sales-table td {
  padding: 8px 12px;
  text-align: left;
  border: 1px solid #dfe6ec;
}

.recent-sales-table th {
  background-color: #f5f7fa;
  font-weight: bold;
  color: #606266;
}

.recent-sales-table tbody tr:hover {
  background-color: #f5f7fa;
}
</style>
