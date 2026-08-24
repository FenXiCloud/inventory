<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <div class="form-toolbar">
        <div class="form-toolbar__left">
          <label class="mr-20px" style="font-size: 16px !important">
            <span style="color: red">*</span>业务类型：
          </label>
          <t-radio-group v-model="form.type" :disabled="isAudited" @change="onTypeChange">
            <t-radio :value="1">客户结算</t-radio>
            <t-radio :value="2">供应商结算</t-radio>
          </t-radio-group>
          <label class="mr-20px ml-16px" style="font-size: 16px !important">
            <span style="color: red">*</span>{{ form.type === 2 ? '供应商' : '客户' }}：
          </label>
          <t-select
            v-model="form.personnelName"
            class="w-200px"
            :options="personnelList"
            :keys="{ value: 'name', label: 'name' }"
            placeholder="请选择"
            filterable
            clearable
            :disabled="isAudited"
            @change="selectPersonnel"
          />
          <label class="mr-20px ml-16px" style="font-size: 16px !important">业务员：</label>
          <t-select
            v-model="form.orderStaffName"
            class="w-140px"
            :options="orderStaffList"
            :keys="{ value: 'name', label: 'name' }"
            placeholder="选择业务员"
            filterable
            clearable
            :disabled="isAudited"
            @change="selectOrderStaff"
          />
          <label class="mr-20px ml-16px" style="font-size: 16px !important">单据日期：</label>
          <t-date-picker class="w-140px" v-model="form.orderDate" :clearable="false" :disabled="isAudited" />
        </div>
        <Stamp v-if="isAudited" />
      </div>

      <!-- 出入库单据（货/账） -->
      <div class="section-title">{{ form.type === 2 ? '采购入库单' : '销售出库单' }}（货/账）</div>
      <t-table
        row-key="_rowKey" size="small" bordered stripe hover table-layout="fixed"
        :columns="inventoryColumns" :data="inventoryTable" :foot-data="inventoryFoot"
        :loading="loading"
      >
        <template #ops="{ rowIndex }">
          <template v-if="!isAudited">
            <div class="fa fa-plus text-hover mr-5px" @click="adjustRows('insert', rowIndex, inventoryTable)"></div>
            <div class="fa fa-minus text-hover" v-if="inventoryTable.length > 1" @click="adjustRows('delete', rowIndex, inventoryTable)"></div>
          </template>
        </template>
        <template #businessNo="{ row }">
          <t-select v-if="!isAudited" v-model="row.businessNo" placeholder="选择单据"
            :options="inventoryDocList" :keys="{ value: 'orderNo', label: 'orderNo' }" filterable clearable
            @change="selectInventoryDoc(row.businessNo, row)" />
          <span v-else>{{ row.businessNo }}</span>
        </template>
        <template #currentVerifyAmount="{ row }">
          <t-input-number v-if="!isAudited" v-model="row.currentVerifyAmount" theme="normal" :min="0" :decimal-places="2" style="width: 100%" />
          <span v-else>{{ row.currentVerifyAmount }}</span>
        </template>
      </t-table>

      <!-- 收付款单据（钱） -->
      <div class="section-title mt-12px">{{ form.type === 1 ? '收款单' : '付款单' }}（钱）</div>
      <t-table
        row-key="_rowKey" size="small" bordered stripe hover table-layout="fixed"
        :columns="fundColumns" :data="fundTable" :foot-data="fundFoot"
        :loading="loading"
      >
        <template #ops="{ rowIndex }">
          <template v-if="!isAudited">
            <div class="fa fa-plus text-hover mr-5px" @click="adjustRows('insert', rowIndex, fundTable)"></div>
            <div class="fa fa-minus text-hover" v-if="fundTable.length > 1" @click="adjustRows('delete', rowIndex, fundTable)"></div>
          </template>
        </template>
        <template #businessNo="{ row }">
          <t-select v-if="!isAudited" v-model="row.businessNo" placeholder="选择单据"
            :options="fundDocList" :keys="{ value: 'orderNo', label: 'orderNo' }" filterable clearable
            @change="selectFundDoc(row.businessNo, row)" />
          <span v-else>{{ row.businessNo }}</span>
        </template>
        <template #currentVerifyAmount="{ row }">
          <t-input-number v-if="!isAudited" v-model="row.currentVerifyAmount" theme="normal" :min="0" :decimal-places="2" style="width: 100%" />
          <span v-else>{{ row.currentVerifyAmount }}</span>
        </template>
      </t-table>

      <!-- 合计栏 -->
      <div class="filler-panel">
        <div class="filler-item">
          <span>货合计：{{ inventoryTotal }}&nbsp;&nbsp;&nbsp;钱合计：{{ fundTotal }}&nbsp;&nbsp;&nbsp;</span>
          <span v-if="inventoryTotal !== fundTotal" style="color: red">金额不平衡！</span>
          <span v-else style="color: green">已平衡</span>
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item">
          <label>备注说明：</label>
          <t-textarea v-model="form.remarks" :disabled="isAudited" placeholder="请输入备注" :maxlength="200" style="width: 400px" />
        </div>
      </div>
    </div>

    <div class="page-column-footer modal-column-between bg-white-color border">
      <t-button @click="closeWindow" :loading="loading">取消</t-button>
      <div>
        <template v-if="!isAudited">
          <t-button theme="primary" @click="saveForm('save')" :loading="loading">保存</t-button>
          <t-button v-if="form.orderStatus == '已保存'" @click="saveForm('audit')" :loading="loading">审核</t-button>
        </template>
        <t-button v-else @click="approved('已保存')" :loading="loading">反审核</t-button>
      </div>
    </div>
  </div>
</template>

<script>
import {mapState, mapMutations} from "vuex";
import {MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';
import Settlement from "@js/api/fund/Settlement";
import Customer from "@js/api/basic/Customer";
import Supplier from "@js/api/basic/Supplier";
import OrderStaff from "@js/api/basic/OrderStaff";
import OrderReceipt from "@js/api/fund/OrderReceipt";
import OrderPayment from "@js/api/fund/OrderPayment";
import PurchaseInbound from "@js/api/purchase/PurchaseInbound";
import SalesOutbound from "@js/api/sales/SalesOutbound";
import Stamp from "@views/common/Stamp.vue";
import manba from "manba";

let rowSeq = 0;
function newRow(extra = {}) {
  return { _rowKey: `r-${++rowSeq}`, currentVerifyAmount: 0, ...extra };
}

const baseCols = [
  {colKey: 'ops', title: '操作', width: 50, align: 'center', fixed: 'left'},
  {colKey: 'businessNo', title: '单据编号', width: 200},
  {colKey: 'businessType', title: '单据类型', width: 100, align: 'center'},
  {colKey: 'documentAmount', title: '单据金额', width: 120, align: 'right'},
  {colKey: 'verifiedAmount', title: '已结算', width: 100, align: 'right'},
  {colKey: 'currentVerifyAmount', title: '本次结算', width: 130, align: 'right'},
  {colKey: 'unverifiedAmount', title: '剩余未结', width: 100, align: 'right'},
];

export default {
  name: "SettlementForm",
  components: {Stamp},
  props: { orderId: [String, Number], type: String, index: Number },
  computed: {
    ...mapState(['user']),
    isAudited() {
      return this.form.orderStatus === '已审核';
    },
    inventoryColumns() { return baseCols; },
    fundColumns() { return baseCols; },
    inventoryFoot() {
      const s = (list, key) => (list||[]).reduce((a,r)=>a+Number(r[key]||0),0).toFixed(2);
      return [{_rowKey:'foot', documentAmount:s(this.inventoryTable,'documentAmount'), currentVerifyAmount:s(this.inventoryTable,'currentVerifyAmount')}];
    },
    fundFoot() {
      const s = (list, key) => (list||[]).reduce((a,r)=>a+Number(r[key]||0),0).toFixed(2);
      return [{_rowKey:'foot', documentAmount:s(this.fundTable,'documentAmount'), currentVerifyAmount:s(this.fundTable,'currentVerifyAmount')}];
    },
    inventoryTotal() {
      return (this.inventoryTable||[]).reduce((a,r)=>a+Number(r.currentVerifyAmount||0),0).toFixed(2);
    },
    fundTotal() {
      return (this.fundTable||[]).reduce((a,r)=>a+Number(r.currentVerifyAmount||0),0).toFixed(2);
    },
    personnelList() {
      return this.form.type === 2 ? this.supplierList : this.customerList;
    },
  },
  data() {
    return {
      loading: false,
      customerList: [], supplierList: [], orderStaffList: [],
      inventoryDocList: [], fundDocList: [],
      form: { type: 1, personnelId: null, personnelName: null, orderDate: manba().format('YYYY-MM-DD'), orderStatus: '已保存', orderStaffId: null, orderStaffName: null, remarks: null },
      inventoryTable: [newRow()],
      fundTable: [newRow()],
    };
  },
  methods: {
    ...mapMutations(['pushTab', 'closeSelfTab']),
    closeWindow() {
      this.closeSelfTab(this.index);
      this.pushTab({keepAlive: false, key: 'SettlementList', title: '结算单'});
    },
    onTypeChange() {
      this.form.personnelName = null; this.form.personnelId = null;
      this.inventoryDocList = []; this.fundDocList = [];
      this.inventoryTable = [newRow()]; this.fundTable = [newRow()];
    },
    selectPersonnel(value) {
      const list = this.form.type === 2 ? this.supplierList : this.customerList;
      const sel = list.find(i => i.name === value);
      if (sel) this.form.personnelId = sel.id;
      this.loadAllDocs();
    },
    selectOrderStaff(value) {
      const sel = this.orderStaffList.find(i => i.name === value);
      if (sel) this.form.orderStaffId = sel.id;
    },
    loadAllDocs() {
      if (!this.form.personnelId) return;
      if (this.form.type === 1) {
        // 客户结算：货=销售出库单，钱=收款单
        SalesOutbound.list({customerId: this.form.personnelId, state: '已审核'}).then(({data:{results}}) => { this.inventoryDocList = results || []; });
        OrderReceipt.list({customerId: this.form.personnelId, orderStatus: '已审核'}).then(({data:{results}}) => { this.fundDocList = results || []; });
      } else {
        // 供应商结算：货=采购入库单，钱=付款单
        PurchaseInbound.list({supplierId: this.form.personnelId, state: '已审核'}).then(({data:{results}}) => { this.inventoryDocList = results || []; });
        OrderPayment.list({supplierId: this.form.personnelId, orderStatus: '已审核'}).then(({data:{results}}) => { this.fundDocList = results || []; });
      }
    },
    selectInventoryDoc(orderNo, row) {
      const doc = this.inventoryDocList.find(i => i.orderNo === orderNo);
      if (doc) this.fillRow(row, doc, 'INVENTORY');
    },
    selectFundDoc(orderNo, row) {
      const doc = this.fundDocList.find(i => i.orderNo === orderNo);
      if (doc) this.fillRow(row, doc, 'FUND');
    },
    fillRow(row, doc, category) {
      row.businessId = doc.id;
      row.businessCategory = category;
      row.businessType = doc.businessType || (category === 'FUND' ? (this.form.type === 1 ? '收款单' : '付款单') : (this.form.type === 1 ? '销售出库单' : '采购入库单'));
      row.documentAmount = doc.collectionAmount || doc.finalAmount || doc.totalAmount || 0;
      row.verifiedAmount = doc.hasVerificationAmount || 0;
      row.unverifiedAmount = (row.documentAmount || 0) - (row.verifiedAmount || 0);
      row.businessDate = doc.orderDate;
      row.businessRemarks = doc.remarks;
    },
    adjustRows(type, index, tableData) {
      if (type === 'insert') tableData.splice(index + 1, 0, newRow());
      else if (type === 'delete' && tableData.length > 1) tableData.splice(index, 1);
    },
    saveForm(mode = 'save') {
      if (!this.form.type) return MessagePlugin.error('请选择业务类型');
      if (!this.form.personnelId) return MessagePlugin.error('请选择' + (this.form.type === 2 ? '供应商' : '客户'));

      const invItems = this.inventoryTable.map(({_rowKey,...r}) => r).filter(r => r.businessId);
      const fundItems = this.fundTable.map(({_rowKey,...r}) => r).filter(r => r.businessId);

      if (!invItems.length) return MessagePlugin.error('请选择出入库单据');
      if (!fundItems.length) return MessagePlugin.error('请选择收付款单据');

      const invTotal = invItems.reduce((a,r) => a + Number(r.currentVerifyAmount||0), 0);
      const fundTotal = fundItems.reduce((a,r) => a + Number(r.currentVerifyAmount||0), 0);
      if (Math.abs(invTotal - fundTotal) > 0.01) {
        return MessagePlugin.error('货方和钱方金额不平衡，请调整本次结算金额');
      }

      let order = {...this.form, createdBy: this.user.admin.id, updatedBy: this.user.admin.id};
      if (mode === 'audit') { order.orderStatus = '已审核'; order.approvedBy = this.user.admin.id; }

      this.loading = true;
      Settlement.save({order, itemList: [...invItems, ...fundItems]}).then(() => {
        MessagePlugin.success(mode === 'audit' ? '审核成功~' : '保存成功~');
        if (mode === 'save') this.closeWindow();
        else this.loadData();
      }).finally(() => this.loading = false);
    },
    approved(status) {
      DialogPlugin.confirm({
        body: '确定反审核？',
        onConfirm: () => Settlement.approved(status, this.form.id).then(s => { if (s!==false) { MessagePlugin.success('反审核成功'); this.loadData(); } })
      });
    },
    loadData() {
      if (!this.orderId) return;
      this.loading = true;
      Settlement.load(this.orderId).then(({data:{order,itemList}}) => {
        this.form = order;
        const inv = [], fund = [];
        (itemList||[]).forEach(r => {
          if (r.businessCategory === 'FUND') fund.push(newRow(r));
          else inv.push(newRow(r));
        });
        this.inventoryTable = inv.length ? inv : [newRow()];
        this.fundTable = fund.length ? fund : [newRow()];
      }).finally(() => this.loading = false);
    },
    loadOptions() {
      Customer.select().then(({data}) => { this.customerList = data || []; });
      Supplier.select().then(({data}) => { this.supplierList = data || []; });
      OrderStaff.select().then(({data}) => { this.orderStaffList = data || []; });
    },
  },
  created() {
    this.loadOptions();
    if (this.orderId) this.loadData();
  },
};
</script>

<style scoped>
.section-title {
  font-size: 14px;
  font-weight: bold;
  padding: 8px 0 4px;
  color: #333;
}
.mt-12px { margin-top: 12px; }
</style>
