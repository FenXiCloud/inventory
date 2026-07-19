<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <vxe-toolbar class-name="!size--mini">
        <template #buttons>
          <label class="mr-20px" style="font-size: 16px !important;">供货商：{{ form.supplierName }}</label>
          <label class="mr-20px ml-16px" style="font-size: 16px !important;">单据日期：{{ form.orderDate }}</label>
        </template>
        <template #tools>
          <Stamp v-if="form.orderStatus === '已审核' "/>
        </template>
      </vxe-toolbar>
      <vxe-table
          size="mini"
          ref="xTable"
          border="border"
          :row-config="{height: 40}"
          show-footer
          stripe
          :footer-method="footerMethod"
          :data="productData">
        <vxe-column title="序号" type="seq" width="60" align="center" fixed="left"/>
        <vxe-column title="产品信息" min-width="300">
          <template #default="{row}">
            <div class="flex">
              <div class="flex1 ml-8px">
                <div>{{ row.productCode }}--{{ row.productName }}</div>
              </div>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="采购单位" field="secondaryUnitName" align="center" width="80"/>
        <vxe-column title="仓库" field="warehouseName" align="center" width="120"/>
        <vxe-column title="数量" field="secondaryQuantity" width="90"/>
        <vxe-column title="基本单位" field="baseUnitName" align="center" width="80"/>
        <vxe-column title="基本数量" field="quantity" width="90"/>
        <vxe-column title="购货单价" field="secondaryPrice" width="100"/>
        <vxe-column title="折扣率(%)" field="discountRate" width="100"/>
        <vxe-column title="折扣额" field="discountAmount" width="100"/>
        <vxe-column title="购货金额" field="subtotal" width="100"/>
        <vxe-column title="备注" field="remark"/>
      </vxe-table>
      <div class="mt-10px"></div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px  w-80px">备注说明：</label>
          <span>{{ form.remarks }}</span>
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item">
          <span class="ml-8px"> 优惠率：{{ form.discountRate }}</span>
          <span class="ml-8px"> 优惠金额：¥ {{ form.discountAmount }}</span>
          <span class="ml-8px"> 优惠后金额：¥ {{ form.finalAmount }}</span>
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
import PurchaseInbound from "@js/api/purchase/PurchaseInbound";
import Stamp from "@views/common/Stamp.vue";

export default {
  name: "PurchaseInboundDetail",
  components: {Stamp},
  props: {
    orderId: [String, Number],
  },
  data() {
    return {
      loading: false,
      form: {
        id: null,
        orderDate: manba().format("YYYY-MM-dd"),
        supplierId: null,
        supplierName: null,
        orderStatus: null,
        discountAmount: 0.00,
        discountRate: 0.00,
        finalAmount: 0.00,
        remarks: null,
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
      this.allFinalAmount = sums[1]
      return [["", "", "", "", "", "", "", quantity.toFixed(2), ""].concat(sums)];
    },

    backApproved() {
      let ids = [this.form.id]
      DialogPlugin.confirm({
        title: "反审核提示",
        content: `本次反审核此订单?`,
        onConfirm: () => {
          PurchaseInbound.approved('已保存', ids).then(() => {
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
      this.$store.commit('newTab', "PurchaseInboundList");
      // 使用 nextTick 确保在 DOM 更新后执行
      this.$nextTick(() => {
        // 通过 eventBus 或 vuex 触发刷新
        this.$store.commit('SET_TAB_DATA', {refresh: true});
      });
    },
  },

  created() {
    LoadingPlugin(true);
    //订单详情
    if (this.orderId) {
      PurchaseInbound.load(this.orderId).then(({data: {purchaseInbound, purchaseInboundItemList}}) => {
        if (purchaseInbound) {
          CopyObj(this.form, purchaseInbound);
        }
        this.productData = purchaseInboundItemList || [];
      }).finally(() => LoadingPlugin(false));
    }
  },
}
</script>
