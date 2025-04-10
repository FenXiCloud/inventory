<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <vxe-toolbar class-name="!size--mini">
        <template #buttons>
          <label class="mr-20px" style="font-size: 16px !important;">供货商：{{ form.supplierName }}</label>
          <label class="mr-20px ml-16px" style="font-size: 16px !important;">退货日期：{{ form.returnDate }}</label>
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
        <vxe-column title="商品信息" width="300">
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
        <div class="filler-item" style="flex: 1;margin: 5px 0 !important;">
          <label class="mr-16px  w-80px">退货原因：{{ form.returnReason }}</label>
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item" style="flex: 1;margin: 5px 0 !important;">
          <label class="mr-16px  w-80px">备注说明：{{ form.remarks }}</label>
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item" style="flex: 1;margin: 5px 0 !important;">
          <span class="ml-8px"> 优惠率：{{ form.discountRate }}</span>
          <span class="ml-8px"> 优惠金额：{{ form.discountAmount }}</span>
          <span class="ml-8px"> 本次退款：{{ form.refundAmount }}</span>
        </div>
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
import {layer} from "@layui/layer-vue";
import {h} from "vue";
import PurchaseReturnOrderSelect from "@views/purchase/PurchaseReturnOrderSelect.vue";
import PurchaseReturn from "@js/api/purchase/PurchaseReturn";

export default {
  name: "PurchaseReturnDetail",
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

    //关闭窗口
    closeWindow() {
      let cache = localStorage.getItem("SYS_TABS");
      let tagList = cache ? JSON.parse(cache) : [];
      if (tagList) {
        let index = tagList.findIndex(val => val.name === "PurchaseReturnList")
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
    PurchaseReturn.load(this.orderId).then(({data: {purchaseReturn, purchaseReturnItemList}}) => {
      if (purchaseReturn) {
        CopyObj(this.form, purchaseReturn);
        this.supplierId = purchaseReturn.supplierId
        if ('copy' === this.type) {
          this.form.id = null;
        }
      }
      this.productData = purchaseReturnItemList || [];
    }).finally(() => loading.close())
  }
}
</script>
