<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <vxe-toolbar class-name="!size--mini">
        <template #buttons>
          <label class="mr-20px" style="font-size: 16px !important;">供货商：{{ form.supplierName }}</label>
          <label class="mr-20px ml-16px" style="font-size: 16px !important;">退货日期：{{ form.returnDate }}</label>
        </template>
        <template #tools>
          <Stamp v-if="form.orderStatus === '已审核' " />
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
        <vxe-column title="产品信息" min-width="300">
          <template #default="{row,rowIndex}">
            <div class="flex">
              <div class="flex1 ml-8px">
                <div>{{ row.productCode }}--{{ row.productName }}</div>
              </div>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="类别" field="categoryName" align="center" width="80"/>
        <vxe-column title="规格" field="spec" align="center" width="80"/>
        <vxe-column title="采购单位" field="secondaryUnitName" align="center" width="80"/>
        <vxe-column title="仓库" field="warehouseName" align="center" width="120"/>
        <vxe-column title="数量" field="secondaryQuantity" width="90"/>
        <vxe-column title="基本单位" field="baseUnitName" align="center" width="80"/>
        <vxe-column title="基本数量" field="quantity" width="90"/>
        <vxe-column title="退货单价" field="secondaryPrice" width="100"/>
        <vxe-column title="折扣率(%)" field="discountRate" width="100"/>
        <vxe-column title="折扣额" field="discountAmount" width="100"/>
        <vxe-column title="退货金额" field="subtotal" width="100"/>
        <vxe-column title="备注" field="returnReason"/>
      </vxe-table>
      <div class="mt-10px"></div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px  w-80px">退货原因：{{ form.returnReason }}</label>
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px  w-80px">备注说明：{{ form.remarks }}</label>
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item">
          <span class=""> 优惠率：{{ form.discountRate }}</span>
          <span class="ml-8px"> 优惠金额：{{ form.discountAmount }}</span>
          <span class="ml-8px"> 本次退款：{{ form.refundAmount }}</span>
        </div>
      </div>
    </div>
    <div class="page-column-footer modal-column-between bg-white-color border">
      <Button @click="closeWindow" :loading="loading">
        取消
      </Button>
      <div>
        <!-- 当状态为已审核时不显示,审核后订单上显示已审核图片 -->
        <Button @click="backApproved()" :loading="loading">
          反审核
        </Button>
      </div>
    </div>
  </div>
</template>
<script>

import {DialogPlugin, LoadingPlugin, MessagePlugin} from "tdesign-vue-next";
import manba from "manba";
import {CopyObj} from "@common/utils";
import {mapState} from "vuex";
import PurchaseReturn from "@js/api/purchase/PurchaseReturn";
import Stamp from "@views/common/Stamp.vue";

export default {
  name: "PurchaseReturnDetail",
  components: {Stamp},
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
      supplierId: null,
      warehouseId: null,
      form: {
        id: null,
        returnDate: manba().format("YYYY-MM-dd"),
        supplierName: null,
        orderStatus: null,
        discountAmount: 0.00,
        discountRate: 0.00,
        refundAmount: 0.00,
        remarks: null,
        returnReason: null,
      },
      productData: [],
    }
  },
  methods: {
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


    backApproved() {
      let ids = [this.form.id]
      DialogPlugin.confirm({
        title: "反审核提示",
        content: `本次反审核此订单?`,
        onConfirm: () => {
          PurchaseReturn.approved('已保存', ids).then(() => {
            MessagePlugin.success("操作成功~");
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
    DialogPlugin.confirm({
      title: "系统提示",
      content: `确认?`,
      onConfirm: () => {

      }
    })
  },
  created() {
    LoadingPlugin(true);
    PurchaseReturn.load(this.orderId).then(({data: {purchaseReturn, purchaseReturnItemList}}) => {
      if (purchaseReturn) {
        CopyObj(this.form, purchaseReturn);
        this.supplierId = purchaseReturn.supplierId
        if ('copy' === this.type) {
          this.form.id = null;
        }
      }
      this.productData = purchaseReturnItemList || [];
    }).finally(() => LoadingPlugin(false))
  }
}
</script>
