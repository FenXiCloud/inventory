<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <div class="form-toolbar">
        <div class="form-toolbar__left">
          <label class="mr-20px" style="font-size: 16px !important">业务类型：</label>
          <t-select
            :clearable="false"
            v-model="form.type"
            class="w-140px z-index-1"
            :options="businessTypeList"
            :keys="{ value: 'type', label: 'name' }"
            placeholder="选择业务类型"
            :disabled="isAudited"
            @change="selectBusinessType"
          />
          <template v-if="form.type == '1'">
            <label class="mr-20px ml-16px" style="font-size: 16px !important">
              <span style="color: red">*</span>客户：
            </label>
            <t-select
              filterable
              clearable
              v-model="form.personnelName"
              class="w-160px z-index-1"
              :options="customerDataList"
              :keys="{ value: 'name', label: 'name' }"
              placeholder="选择客户"
              :disabled="isAudited"
              @change="selectPerson"
            />
          </template>
          <template v-else-if="form.type == '2'">
            <label class="mr-20px ml-16px" style="font-size: 16px !important">
              <span style="color: red">*</span>供应商：
            </label>
            <t-select
              filterable
              clearable
              v-model="form.personnelName"
              class="w-160px z-index-1"
              :options="supplierDataList"
              :keys="{ value: 'name', label: 'name' }"
              placeholder="选择供应商"
              :disabled="isAudited"
              @change="selectPerson"
            />
          </template>
          <label class="mr-20px ml-16px" style="font-size: 16px !important">业务员：</label>
          <span style="display: inline-flex; align-items: center; z-index: 1">
            <t-select
              filterable
              clearable
              ref="selectRef"
              v-model="form.orderStaffName"
              class="w-140px"
              :options="orderStaffList"
              :keys="{ value: 'name', label: 'name' }"
              placeholder="选择业务员"
              :disabled="isAudited"
              @change="selectOrderStaff"
            />
            <t-button v-if="!isAudited" variant="text" @click="addOrderStaff()">新建</t-button>
          </span>
          <label class="mr-20px ml-16px" style="font-size: 16px !important">
            <span style="color: red">*</span>单据日期：
          </label>
          <t-date-picker
            class="w-140px"
            v-model="form.orderDate"
            :clearable="false"
            :disabled="isAudited"
          />
        </div>
        <div class="form-toolbar__right">
          <Stamp v-if="isAudited" />
        </div>
      </div>

      <div v-if="!isAudited" class="form-toolbar form-toolbar--end">
        <t-button v-show="form.type == '1'" @click="sourceForm('预收')">选择预收单据</t-button>
        <t-button v-show="form.type == '2'" @click="sourceForm('预付')">选择预付单据</t-button>
      </div>

      <t-table
        row-key="_rowKey"
        size="small"
        bordered
        stripe
        hover
        table-layout="fixed"
        :data="tableData"
        :columns="prepayColumns"
        :foot-data="footData"
        :loading="loading"
      >
        <template #ops="{ rowIndex }">
          <template v-if="!isAudited">
            <div
              class="fa fa-plus text-hover mr-5px"
              @click="adjustRows('insert', rowIndex, tableData)"
            ></div>
            <div
              class="fa fa-minus text-hover"
              v-if="canDelete(tableData)"
              @click="adjustRows('delete', rowIndex, tableData)"
            ></div>
          </template>
        </template>
        <template #businessType="{ row }">
          <span v-if="row.businessType">{{
            form.type == 1 ? '收款' : '付款'
          }}</span>
        </template>
        <template #currentVerifyAmount="{ row }">
          <t-input-number
            v-if="!isAudited"
            v-model="row.currentVerifyAmount"
            theme="normal"
            :min="0"
            :max="row.documentAmount"
            :decimal-places="2"
            style="width: 100%"
          />
          <span v-else>{{ row.currentVerifyAmount }}</span>
        </template>
        <template #remarks="{ row }">
          <t-input v-if="!isAudited" v-model="row.remarks" />
          <span v-else>{{ row.remarks }}</span>
        </template>
      </t-table>

      <div v-if="!isAudited" class="form-toolbar form-toolbar--end">
        <t-button v-show="form.type == '1'" @click="sourceForm('应收')">选择应收单据</t-button>
        <t-button v-show="form.type == '2'" @click="sourceForm('应付')">选择应付单据</t-button>
        <t-button @click="autoReconciliation">自动核销</t-button>
      </div>

      <t-table
        row-key="_rowKey"
        size="small"
        bordered
        stripe
        hover
        table-layout="fixed"
        :data="tableData2"
        :columns="receivableColumns"
        :foot-data="footData2"
        :loading="loading"
      >
        <template #ops="{ rowIndex }">
          <template v-if="!isAudited">
            <div
              class="fa fa-plus text-hover mr-5px"
              @click="adjustRows('insert', rowIndex, tableData2)"
            ></div>
            <div
              class="fa fa-minus text-hover"
              v-if="canDelete(tableData2)"
              @click="adjustRows('delete', rowIndex, tableData2)"
            ></div>
          </template>
        </template>
        <template #businessType="{ row }">
          <span v-if="row.businessType">
            <span v-if="row.businessType == '2'">期初余额</span>
            <span v-else-if="form.type == '1'">普通销售</span>
            <span v-else>普通采购</span>
          </span>
        </template>
        <template #currentVerifyAmount="{ row }">
          <t-input-number
            v-if="!isAudited"
            v-model="row.currentVerifyAmount"
            theme="normal"
            :min="0"
            :max="row.documentAmount"
            :decimal-places="2"
            style="width: 100%"
          />
          <span v-else>{{ row.currentVerifyAmount }}</span>
        </template>
        <template #remarks="{ row }">
          <t-input v-if="!isAudited" v-model="row.remarks" />
          <span v-else>{{ row.remarks }}</span>
        </template>
      </t-table>

      <div class="mt-10px"></div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px w-80px">备注说明：</label>
          <t-input
            placeholder="请输入备注"
            :maxlength="150"
            v-model="form.remarks"
            :disabled="isAudited"
          />
          <div class="verification-extra-actions">
            <t-button @click="historyForm()">历史单据</t-button>
            <t-button :title="logContent">操作日志</t-button>
          </div>
        </div>
      </div>
    </div>

    <div class="page-column-footer modal-column-between bg-white-color border">
      <t-button :loading="loading" @click="closeWindow">取消</t-button>
      <div>
        <template v-if="!isAudited">
          <t-button theme="primary" :loading="loading" @click="saveForm('add')">保存并新增</t-button>
          <t-button :loading="loading" @click="saveForm('save')">保存</t-button>
          <t-button
            v-if="form.orderStatus == '已保存'"
            :loading="loading"
            @click="saveForm('audit', '已审核')"
          >审核</t-button>
        </template>
        <t-button
          v-else
          :loading="loading"
          @click="approved('已保存')"
        >反审核</t-button>
      </div>
    </div>
  </div>
</template>
<script>
import {LoadingPlugin, MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import { openDialog, closeDialog } from '@common/dialog';
import { h } from 'vue';
import OrderReceipt from '@js/api/fund/OrderReceipt';
import Verification from '@js/api/fund/Verification';
import Customer from '@js/api/basic/Customer';
import Supplier from '@js/api/basic/Supplier';
import OrderStaff from '@js/api/basic/OrderStaff';
import OrderStaffForm from './OrderStaffForm';
import SourceByVerification from './SourceByVerification.vue';
import { mapState, mapMutations } from 'vuex';
import Stamp from '../common/Stamp.vue';
const Big = require('big.js');
import manba from 'manba';

let rowSeq = 0;
function newRow(extra = {}) {
  return { _rowKey: `r-${++rowSeq}`, ...extra };
}

export default {
  name: 'VerificationForm',
  components: { Stamp },
  props: {
    orderId: [String, Number],
    type: String,
    index: Number
  },
  data() {
    return {
      logContent: null,
      val1: [],
      businessTypeList: [
        {
          name: '预收冲应收',
          type: '1'
        },
        {
          name: '预付冲应付',
          type: '2'
        }
      ],
      form: {
        type: '1',
        orderStaffName: null,
        orderDate: manba().format('YYYY-MM-DD')
      },
      tableData: [newRow()],
      tableData2: [newRow()],
      customerDataList: [],
      supplierDataList: [],
      customOrSupplierBalance: 0,
      orderStaffList: [],
      loading: false,
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      params: {
        filter: null,
        state: null,
        sortCol: null,
        sort: null
      }
    };
  },
  computed: {
    ...mapState(['user']),
    isAudited() {
      return this.form.orderStatus === '已审核';
    },
    totalTb1() {
      return (this.tableData || []).reduce((sum, row) => {
        const value = parseFloat(row.currentVerifyAmount);
        return sum + (isNaN(value) ? 0 : value);
      }, 0);
    },
    totalTb2() {
      return (this.tableData2 || []).reduce((sum, row) => {
        const value = parseFloat(row.currentVerifyAmount);
        return sum + (isNaN(value) ? 0 : value);
      }, 0);
    },
    calcCollectionAmount() {
      return (
        (parseFloat(this.totalTb1) || 0) -
        (parseFloat(this.totalTb2) || 0) +
        (parseFloat(this.form.discountRate) || 0)
      ).toFixed(2);
    },
    queryParams() {
      return Object.assign(this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize
      });
    },
    footData() {
      const sum = (key) => {
        const total = (this.tableData || []).reduce((acc, row) => {
          const value = parseFloat(row[key]);
          return acc + (isNaN(value) ? 0 : value);
        }, 0);
        return total.toFixed(2);
      };
      return [{
        seq: '合计',
        documentAmount: sum('documentAmount'),
        verifiedAmount: sum('verifiedAmount'),
        unverifiedAmount: sum('unverifiedAmount'),
        currentVerifyAmount: sum('currentVerifyAmount')
      }];
    },
    footData2() {
      const sum = (key) => {
        const total = (this.tableData2 || []).reduce((acc, row) => {
          const value = parseFloat(row[key]);
          return acc + (isNaN(value) ? 0 : value);
        }, 0);
        return total.toFixed(2);
      };
      return [{
        seq: '合计',
        documentAmount: sum('documentAmount'),
        verifiedAmount: sum('verifiedAmount'),
        unverifiedAmount: sum('unverifiedAmount'),
        currentVerifyAmount: sum('currentVerifyAmount')
      }];
    },
    prepayColumns() {
      return [
        {
          colKey: 'seq',
          title: '序号',
          width: 60,
          align: 'center',
          fixed: 'left',
          cell: (h, { rowIndex }) => rowIndex + 1
        },
        { colKey: 'ops', title: '操作', width: 70, align: 'center', fixed: 'left' },
        { colKey: 'businessNo', title: '源单编号', minWidth: 140 },
        { colKey: 'businessType', title: '业务类别', width: 110, align: 'center' },
        { colKey: 'businessDate', title: '单据日期', width: 120, align: 'center' },
        { colKey: 'documentAmount', title: '单据金额', width: 110, align: 'right' },
        { colKey: 'verifiedAmount', title: '已核销金额', width: 110, align: 'right' },
        { colKey: 'unverifiedAmount', title: '未核销金额', width: 110, align: 'right' },
        { colKey: 'businessRemarks', title: '源单备注', minWidth: 120 },
        { colKey: 'currentVerifyAmount', title: '*本次核销金额', width: 160, align: 'right' },
        { colKey: 'remarks', title: '备注', minWidth: 120 }
      ];
    },
    receivableColumns() {
      return [
        {
          colKey: 'seq',
          title: '序号',
          width: 60,
          align: 'center',
          fixed: 'left',
          cell: (h, { rowIndex }) => rowIndex + 1
        },
        { colKey: 'ops', title: '操作', width: 70, align: 'center' },
        { colKey: 'businessNo', title: '源单编号', minWidth: 140 },
        { colKey: 'businessType', title: '业务类别', width: 110, align: 'center' },
        { colKey: 'businessDate', title: '单据日期', width: 120, align: 'center' },
        { colKey: 'documentAmount', title: '单据金额', width: 110, align: 'right' },
        { colKey: 'verifiedAmount', title: '已核销金额', width: 110, align: 'right' },
        { colKey: 'unverifiedAmount', title: '未核销金额', width: 110, align: 'right' },
        { colKey: 'businessRemarks', title: '源单备注', minWidth: 120 },
        { colKey: 'currentVerifyAmount', title: '*本次核销金额', width: 160, align: 'right' },
        { colKey: 'remarks', title: '备注', minWidth: 120 }
      ];
    }
  },
  methods: {
    ...mapMutations(['pushTab', 'closeSelfTab']),
    closeWindow() {
      this.closeSelfTab(this.index);
      this.pushTab({
        keepAlive: false,
        key: 'VerificationList',
        title: '核销单'
      });
    },
    clearData() {
      this.form = {
        type: '1',
        orderDate: manba().format('YYYY-MM-DD')
      };
      this.tableData = [newRow()];
      this.tableData2 = [newRow()];
    },
    getLog() {
      let createName = this.user.admin.name;
      let {
        updateName,
        createdAt,
        updateAt,
        approvedName,
        approvedAt
      } = this.form;
      const logEntries = [
        `制单人: ${createName}`,
        createdAt ? `制单时间: ${createdAt}` : null,
        updateName ? `最后修改人: ${updateName}` : null,
        updateAt ? `最后修改时间: ${updateAt}` : null,
        approvedName ? `审核人: ${approvedName}` : null,
        approvedAt ? `审核时间: ${approvedAt}` : null
      ].filter((entry) => entry);

      this.logContent = logEntries.join('\n');
    },
    loadList() {
      this.loading = true;
      Verification.load(this.orderId )
        .then(({ data: { order, collectionList, itemList } }) => {
          this.form = order;
          this.tableData = (collectionList || []).map((item) => newRow(item));
          this.tableData2 = (itemList || []).map((item) => newRow(item));
          this.getLog();
        })
        .finally(() => (this.loading = false));
    },
    historyForm() {
      this.pushTab({
        keepAlive: false,
        key: 'VerificationList',
        title: '核销单'
      });
    },
    saveForm(type = 'add', orderStatus = '已保存') {
      let order = {
        ...this.form,
        createdBy: this.user.admin.id,
        updateBy: this.user.admin.id,
        orderStatus: '已保存'
      };
      if (!this.form.orderDate || this.form.orderDate == '') {
        return MessagePlugin.warning('请选择单据日期');
      }
      if (type === 'audit') {
        order.orderStatus = orderStatus;
        order.approvedBy = this.user.admin.id;
        order.approvedName = this.user.admin.name;
      }
      const filterEmptyObjects = (arr) =>
        arr
          .map(({ _X_ROW_KEY, _rowKey, ...rest }) => rest)
          .filter((row) => Object.keys(row).length);

      let params = {
        order,
        collectionList: filterEmptyObjects(this.tableData),
        itemList: filterEmptyObjects(this.tableData2)
      };

      if (this.form.type == '1' && !this.form.personnelId) {
        return MessagePlugin.error('请选择客户');
      } else if (this.form.type == '2' && !this.form.personnelId) {
        return MessagePlugin.error('请选择供应商');
      } else if (!this.tableData[0].businessId || !this.tableData2[0].businessId) {
        return MessagePlugin.error('请选择核销单据');
      } else if (
        this.tableData2[0].currentVerifyAmount &&
        this.tableData2[0].currentVerifyAmount == 0
      ) {
        return MessagePlugin.error('本次核销金额不可为0');
      }
      const isEqual = this.checkTotalVerificationAmountEqual();

      if (!isEqual) {
        MessagePlugin.warning('请检查本次核销金额');
        return;
      }
      this.save(type, params);
    },
    approved(orderStatus) {
      const isAnti = orderStatus === '已保存';
      DialogPlugin.confirm({
        body: isAnti ? '确定反审核？' : '确定审核？',
        onConfirm: () => {
          OrderReceipt.approved(orderStatus, this.form.id)
            .then((success) => {
              if (success !== false) {
                MessagePlugin.success(isAnti ? '反审核成功' : '审核成功');
                this.loadList();
              }
            })
            .finally(() => LoadingPlugin(false));
        }
      });
    },
    checkTotalVerificationAmountEqual() {
      const sumTable1 = (this.tableData || []).reduce((sum, row) => {
        const val = parseFloat(row.currentVerifyAmount);
        return sum + (isNaN(val) ? 0 : val);
      }, 0);

      const sumTable2 = (this.tableData2 || []).reduce((sum, row) => {
        const val = parseFloat(row.currentVerifyAmount);
        return sum + (isNaN(val) ? 0 : val);
      }, 0);

      const fixedSum1 = parseFloat(sumTable1.toFixed(2));
      const fixedSum2 = parseFloat(sumTable2.toFixed(2));

      return fixedSum1 === fixedSum2 && fixedSum1 != 0;
    },
    canDelete(tableData) {
      return tableData.length > 1;
    },
    adjustRows(type, index, tableData) {
      if (type === 'insert') {
        tableData.splice(index + 1, 0, newRow({ isNew: true }));
      } else if (type === 'delete' && this.canDelete(tableData)) {
        tableData.splice(index, 1);
      }
    },
    doSearch() {
      this.pagination.page = 1;
    },
    save(type, params) {
      this.loading = true;
      Verification.save(params)
        .then(() => {
          MessagePlugin.success('保存成功~');
          if (type == 'save' || type == 'add') {
            this.clearData();
            this.historyForm();
          }
          if (type == 'audit') {
            this.loadList();
          }
        })
        .finally(() => (this.loading = false));
    },
    loadCustomer() {
      this.loading = true;
      Customer.select()
        .then((results) => {
          this.customerDataList = results.data || [];
        })
        .finally(() => (this.loading = false));
    },
    loadSupplier() {
      this.loading = true;
      Supplier.select()
        .then((results) => {
          this.supplierDataList = results.data || [];
        })
        .finally(() => (this.loading = false));
    },
    loadOrderStaff() {
      OrderStaff.select()
        .then(({ data }) => {
          this.orderStaffList = data || [];
        })
        .finally();
    },
    selectBusinessType(value) {
      this.clearData();
      this.form.type = value;
    },
    selectPerson(value) {
      const list = this.form.type == '1' ? this.customerDataList : this.supplierDataList;
      const e = list.find((item) => item.name === value);
      this.form.personnelId = e?.id;
      this.customOrSupplierBalance = e?.balance;
      this.form = {
        ...this.form
      };
      this.tableData = [newRow()];
      this.tableData2 = [newRow()];
    },
    selectOrderStaff(value) {
      const e = this.orderStaffList.find((item) => item.name === value);
      this.form.orderStaffId = e ? e.id : null;
      this.form.orderStaffName = e ? e.name : null;
    },
    addOrderStaff() {
      this.showForm();
    },
    showForm(entity) {
      let type = 0;
      let dialogId = openDialog({
        header: '新增职员',
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '600px',
        body: h(OrderStaffForm, {
          entity,
          type,
          onClose: () => {
            closeDialog(dialogId);
          },
          onSuccess: () => {
            this.loadOrderStaff();
            closeDialog(dialogId);
          }
        })
      });
    },
    sourceForm(sourceType) {
      if (this.form.type == '1' && !this.form.personnelId) {
        return MessagePlugin.warning('请先选择客户');
      }
      if (this.form.type == '2' && !this.form.personnelId) {
        return MessagePlugin.warning('请先选择供应商');
      }
      this.form.sourceType = sourceType;
      let params = { ...this.form };
      params.balance = this.customOrSupplierBalance;
      let dialogId = openDialog({
        header: '选择源单',
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '900px',
        body: h(SourceByVerification, {
          params,
          onClose: () => {
            closeDialog(dialogId);
          },
          onSuccess: (checkList, tableJson) => {
            let tableArr = this.mergeCheckListWithTableJson(
              checkList,
              tableJson
            );
            if (
              this.form.sourceType == '预收' ||
              this.form.sourceType == '预付'
            ) {
              const merged = new Map(
                this.tableData.map((item) => [item.businessNo, item])
              );
              tableArr.forEach((item) => {
                if (!merged.has(item.businessNo)) {
                  merged.set(item.businessNo, item);
                }
              });
              this.tableData = Array.from(merged.values())
                .filter((item) => item.businessNo)
                .map((item) => {
                  delete item._X_ROW_KEY;
                  return newRow(item);
                })
                .sort((a, b) => a.unverifiedAmount - b.unverifiedAmount);
            } else {
              const merged = new Map(
                this.tableData2.map((item) => [item.businessNo, item])
              );
              tableArr.forEach((item) => {
                if (!merged.has(item.businessNo)) {
                  merged.set(item.businessNo, item);
                }
              });
              this.tableData2 = Array.from(merged.values())
                .filter((item) => item.businessNo)
                .map((item) => {
                  delete item._X_ROW_KEY;
                  return newRow(item);
                })
                .sort((a, b) => b.unverifiedAmount - a.unverifiedAmount);
            }

            closeDialog(dialogId);
          }
        })
      });
    },
    mergeCheckListWithTableJson(checkList, tableJson) {
      return checkList.map((item) => {
        const newItem = {};
        tableJson.forEach((config) => {
          newItem[config.toField] = item[config.field];
          if (config.toField == 'unverifiedAmount') {
            newItem.currentVerifyAmount = 0;
          }
        });

        return newItem;
      });
    },
    autoReconciliation() {
      if (!this.tableData[0].businessId || !this.tableData2[0].businessId) {
        return MessagePlugin.warning('请先选择需要核销的记录');
      }

      this.initTableData();

      const tempTable1 = this.tableData.map((item) => ({ ...item }));
      const tempTable2 = this.tableData2.map((item) => ({ ...item }));
      for (let i = 0; i < tempTable2.length; i++) {
        const table2Item = tempTable2[i];
        let availableAmount = new Big(table2Item.unverifiedAmount || 0);

        if (availableAmount <= 0) continue;

        for (let j = 0; j < tempTable1.length && availableAmount > 0; j++) {
          const table1Item = tempTable1[j];
          const remainingUnverified = parseFloat(
            new Big(table1Item.unverifiedAmount || 0).minus(
              table1Item.currentVerifyAmount || 0
            )
          );

          if (remainingUnverified <= 0) continue;

          const verifyAmount = Math.min(availableAmount, remainingUnverified);
          const bigVerifyAmount = new Big(verifyAmount);

          table1Item.currentVerifyAmount = parseFloat(
            new Big(table1Item.currentVerifyAmount || 0).plus(bigVerifyAmount)
          );
          table2Item.currentVerifyAmount = parseFloat(
            new Big(table2Item.currentVerifyAmount || 0).plus(bigVerifyAmount)
          );

          availableAmount = parseFloat(
            new Big(availableAmount).minus(bigVerifyAmount)
          );
        }
      }

      this.tableData = tempTable1;
      this.tableData2 = tempTable2;
    },
    initTableData() {
      this.tableData.forEach((item) => {
        item.currentVerifyAmount = 0;
      });
      this.tableData2.forEach((item) => {
        item.currentVerifyAmount = 0;
      });
    },
    getTotalUnverified() {
      const table1 = this.tableData;
      const table2 = this.tableData2;
      const table1Total = table1.reduce((sum, row) => {
        const unverified = parseFloat(row.unverifiedAmount);
        return sum + (isNaN(unverified) ? 0 : row.unverifiedAmount);
      }, 0);
      const table2Total = table2.reduce((sum, row) => {
        const unverified = parseFloat(row.unverifiedAmount);
        return sum + (isNaN(unverified) ? 0 : row.unverifiedAmount);
      }, 0);
      return {
        table1Total,
        table2Total
      };
    }
  },
  created() {
    if (this.orderId) {
      this.loadList();
    }
    this.loadCustomer();
    this.loadSupplier();
    this.loadOrderStaff();
    setTimeout(() => {
      this.getLog();
    }, 500);
  }
};
</script>
<style lang="less" scoped>
.form-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.form-toolbar__left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  flex: 1;
}
.form-toolbar__right {
  margin-left: auto;
}
.form-toolbar--end {
  justify-content: flex-end;
  margin: 8px 0;
}

.verification-extra-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}
</style>
