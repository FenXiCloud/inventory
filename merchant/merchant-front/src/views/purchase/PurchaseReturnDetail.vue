<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <div class="detail-toolbar">
        <div class="detail-toolbar__left">
          <label class="mr-20px" style="font-size: 16px !important;">供货商：{{ form.supplierName }}</label>
          <label class="mr-20px ml-16px" style="font-size: 16px !important;">退货日期：{{ form.returnDate }}</label>
        </div>
        <div class="detail-toolbar__right">
          <Stamp v-if="form.orderStatus === '已审核' " />
        </div>
      </div>
      <t-table
          row-key="__rowKey"
          size="small"
          bordered
          stripe
          hover
          table-layout="fixed"
          :data="tableData"
          :columns="columns"
          :foot-data="footData"
          :loading="loading"
      />
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
      <t-button @click="closeWindow" :loading="loading">
        取消
      </t-button>
      <div>
        <t-button @click="backApproved()" :loading="loading">
          反审核
        </t-button>
      </div>
    </div>
  </div>
</template>
<script>
import {LoadingPlugin, MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';
import manba from "manba";
import {CopyObj} from "@common/utils";
import {mapState} from "vuex";
import PurchaseReturn from "@js/api/purchase/PurchaseReturn";
import Stamp from "@views/common/Stamp.vue";

function sumField(list, field) {
  return (list || []).reduce((acc, row) => acc + Number(row[field] || 0), 0);
}

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
    },
    tableData() {
      return (this.productData || []).map((row, index) => ({
        ...row,
        __rowKey: row.id != null ? row.id : `row-${index}`
      }));
    },
    footData() {
      return [{
        seq: '合计',
        quantity: sumField(this.productData, 'quantity').toFixed(2),
        discountAmount: sumField(this.productData, 'discountAmount').toFixed(2),
        subtotal: sumField(this.productData, 'subtotal').toFixed(2)
      }];
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
      columns: [
        {
          colKey: 'seq',
          title: '序号',
          width: 60,
          align: 'center',
          fixed: 'left',
          cell: (h, { rowIndex }) => rowIndex + 1
        },
        {
          colKey: 'productInfo',
          title: '产品信息',
          minWidth: 300,
          cell: (h, { row }) => `${row.productCode || ''}--${row.productName || ''}`
        },
        { colKey: 'categoryName', title: '类别', width: 80, align: 'center' },
        { colKey: 'spec', title: '规格', width: 80, align: 'center' },
        { colKey: 'secondaryUnitName', title: '采购单位', width: 80, align: 'center' },
        { colKey: 'warehouseName', title: '仓库', width: 120, align: 'center' },
        { colKey: 'secondaryQuantity', title: '数量', width: 90 },
        { colKey: 'baseUnitName', title: '基本单位', width: 80, align: 'center' },
        { colKey: 'quantity', title: '基本数量', width: 90 },
        { colKey: 'secondaryPrice', title: '退货单价', width: 100 },
        { colKey: 'discountRate', title: '折扣率(%)', width: 100 },
        { colKey: 'discountAmount', title: '折扣额', width: 100 },
        { colKey: 'subtotal', title: '退货金额', width: 100 },
        { colKey: 'returnReason', title: '备注', ellipsis: true }
      ]
    }
  },
  methods: {
    backApproved() {
      let ids = [this.form.id]
      DialogPlugin.confirm({
        header: "反审核提示",
        body: `本次反审核此订单?`,
        onConfirm: () => {
          PurchaseReturn.approved('已保存', ids).then(() => {
            MessagePlugin.success("操作成功~");
            this.closeWindow();
          })
        }
      })
    },
    closeWindow() {
      this.$store.commit('closeTabKey', this.$store.state.currentTab);
      this.$store.commit('newTab', "PurchaseReturnList");
      this.$nextTick(() => {
        this.$store.commit('SET_TAB_DATA', {refresh: true});
      });
    },
  },
  beforeDestroy() {
    DialogPlugin.confirm({
      header: "系统提示",
      body: `确认?`,
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
<style scoped>
.detail-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0 12px;
}
.detail-toolbar__left,
.detail-toolbar__right {
  display: flex;
  align-items: center;
}
</style>
