<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <div class="detail-toolbar">
        <div class="detail-toolbar__left">
          <label class="mr-20px" style="font-size: 16px !important;">供货商：{{ form.supplierName }}</label>
          <label class="mr-20px ml-16px" style="font-size: 16px !important;">单据日期：{{ form.orderDate }}</label>
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
          <label class="mr-16px  w-80px">备注说明：</label>
          <span>{{form.remarks}}</span>
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item">
          <span class="ml-8px"> 优惠率：{{  form.discountRate  }}</span>
          <span class="ml-8px"> 优惠金额：¥ {{  form.discountAmount  }}</span>
          <span class="ml-8px"> 优惠后金额：¥ {{  form.finalAmount  }}</span>
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
import PurchaseOrder from "@js/api/purchase/PurchaseOrder";
import Stamp from "@views/common/Stamp.vue";

function sumField(list, field) {
  return (list || []).reduce((acc, row) => acc + Number(row[field] || 0), 0);
}

export default {
  name: "PurchaseOrderDetail",
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
        { colKey: 'secondaryUnitName', title: '采购单位', width: 80, align: 'center' },
        { colKey: 'warehouseName', title: '仓库', width: 120, align: 'center' },
        { colKey: 'secondaryQuantity', title: '数量', width: 90 },
        { colKey: 'baseUnitName', title: '基本单位', width: 80, align: 'center' },
        { colKey: 'quantity', title: '基本数量', width: 90 },
        { colKey: 'secondaryPrice', title: '购货单价', width: 100 },
        { colKey: 'discountRate', title: '折扣率(%)', width: 100 },
        { colKey: 'discountAmount', title: '折扣额', width: 100 },
        { colKey: 'subtotal', title: '购货金额', width: 100 },
        { colKey: 'remark', title: '备注', ellipsis: true }
      ]
    }
  },
  computed: {
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
  methods: {
    backApproved() {
      let ids = [this.form.id]
      DialogPlugin.confirm({
        header: "反审核提示",
        body: `本次反审核此订单?`,
        onConfirm: () => {
          PurchaseOrder.approved('已保存', ids).then(() => {
            MessagePlugin.success("操作成功~");
            this.closeWindow();
          })
        }
      })
    },
    closeWindow() {
      this.$store.commit('closeTabKey', this.$store.state.currentTab);
      this.$store.commit('newTab', "PurchaseOrderList");
      this.$nextTick(() => {
        this.$store.commit('SET_TAB_DATA', {refresh: true});
      });
    },
  },
  created() {
    LoadingPlugin(true);
    if (this.orderId) {
      PurchaseOrder.load(this.orderId).then(({data: {purchaseOrder, purchaseOrderItemList}}) => {
        if (purchaseOrder) {
          CopyObj(this.form, purchaseOrder);
        }
        this.productData = purchaseOrderItemList || [];
      }).finally(() => LoadingPlugin(false));
    }
  },
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
