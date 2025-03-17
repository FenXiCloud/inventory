<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <vxe-toolbar class-name="!size--mini">
        <template #buttons>
          <label class="mr-20px" style="font-size: 16px !important;">供货商：{{ form.supplierName }}</label>
          <label class="mr-20px ml-16px" style="font-size: 16px !important;">单据日期：{{ form.orderDate }}</label>
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
        <vxe-column title="商品信息" width="300">
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
        <div class="filler-item" style="flex: 1;margin: 5px 0 !important;">
          <label class="mr-16px  w-80px">备注说明：</label>
          <span>{{form.remarks}}</span>
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item" style="flex: 1;margin: 5px 0 !important;">
          <span class="ml-8px"> 优惠率：{{  form.discountRate  }}</span>
          <span class="ml-8px"> 优惠金额：¥ {{  form.discountAmount  }}</span>
          <span class="ml-8px"> 优惠后金额：¥ {{  form.finalAmount  }}</span>
        </div>
      </div>
    </div>
  </div>
</template>
<script>

import {loading,} from "heyui.ext";
import manba from "manba";
import {CopyObj} from "@common/utils";
import PurchaseOrder from "@js/api/purchase/PurchaseOrder";

export default {
  name: "PurchaseOrderDetail",
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
  },

  created() {
    loading("加载中....");
    //订单详情
    if (this.orderId) {
      PurchaseOrder.load(this.orderId).then(({data: {purchaseOrder, purchaseOrderItemList}}) => {
        if (purchaseOrder) {
          CopyObj(this.form, purchaseOrder);
        }
        this.productData = purchaseOrderItemList || [];
      }).finally(() => loading.close());
    }
  },
}
</script>
