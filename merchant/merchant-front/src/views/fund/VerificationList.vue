<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <vxe-toolbar class-name="!size--mini">
        <template #buttons>
          <label class="mr-20px" style="font-size: 16px !important">业务类型：</label>
          <Select
            :deletable="false"
            v-model="form.type"
            class="w-140px z-index-1"
            :datas="businessTypeList"
            keyName="type"
            titleName="name"
            placeholder="选择业务类型"
            :disabled="isAudited"
            @change="selectBusinessType($event)"
          />
          <template v-if="form.type == '1'">
            <label class="mr-20px ml-16px" style="font-size: 16px !important">
              <span style="color: red">*</span>客户：
            </label>
            <Select
              :filterable="true"
              v-model="form.personnelName"
              class="w-160px z-index-1"
              :datas="customerDataList"
              keyName="name"
              titleName="name"
              placeholder="选择客户"
              :disabled="isAudited"
              @change="selectPerson($event)"
            />
          </template>
          <template v-else-if="form.type == '2'">
            <label class="mr-20px ml-16px" style="font-size: 16px !important">
              <span style="color: red">*</span>供应商：
            </label>
            <Select
              :filterable="true"
              v-model="form.personnelName"
              class="w-160px z-index-1"
              :datas="supplierDataList"
              keyName="name"
              titleName="name"
              placeholder="选择供应商"
              :disabled="isAudited"
              @change="selectPerson($event)"
            />
          </template>
          <label class="mr-20px ml-16px" style="font-size: 16px !important">业务员：</label>
          <Select
            :filterable="true"
            ref="selectRef"
            style="z-index: 1"
            v-model="form.orderStaffName"
            class="w-140px"
            :datas="orderStaffList"
            keyName="name"
            titleName="name"
            placeholder="选择业务员"
            :disabled="isAudited"
            @change="selectOrderStaff($event)"
          >
            <template #bottom>
              <Button no-border icon="add" @click="addOrderStaff()">新建</Button>
            </template>
          </Select>
          <label class="mr-20px ml-16px" style="font-size: 16px !important">
            <span style="color: red">*</span>单据日期：
          </label>
          <DatePicker
            class="w-140px"
            v-model="form.orderDate"
            :clearable="false"
            :disabled="isAudited"
          />
        </template>
        <template #tools>
          <Stamp v-if="isAudited" />
        </template>
      </vxe-toolbar>

      <vxe-toolbar>
        <template v-if="!isAudited" #tools>
          <Button v-show="form.type == '1'" @click="sourceForm('预收')">选择预收单据</Button>
          <Button v-show="form.type == '2'" @click="sourceForm('预付')">选择预付单据</Button>
        </template>
      </vxe-toolbar>

      <vxe-table
        ref="tableRef"
        size="mini"
        border
        stripe
        show-overflow
        :row-config="{ height: 40 }"
        :edit-config="isAudited ? undefined : editConfig"
        :data="tableData"
        :show-footer="showFooter"
        :footer-method="footerMethod"
      >
        <vxe-column type="seq" title="序号" width="60" align="center" fixed="left" />
        <vxe-column title="操作" field="seq" width="70" align="center" fixed="left">
          <template #default="{ rowIndex }">
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
        </vxe-column>
        <vxe-column field="businessNo" title="源单编号" min-width="140" />
        <vxe-column field="businessType" title="业务类别" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.businessType">{{
              form.type == 1 ? '收款' : '付款'
            }}</span>
          </template>
        </vxe-column>
        <vxe-column field="businessDate" title="单据日期" width="120" align="center" />
        <vxe-column field="documentAmount" title="单据金额" width="110" align="right" />
        <vxe-column field="verifiedAmount" title="已核销金额" width="110" align="right" />
        <vxe-column field="unverifiedAmount" title="未核销金额" width="110" align="right" />
        <vxe-column field="businessRemarks" title="源单备注" min-width="120" />
        <vxe-column
          width="160"
          field="currentVerifyAmount"
          title="本次核销金额"
          :edit-render="{}"
        >
          <template #header>
            <span style="color: red">*</span>本次核销金额
          </template>
          <template #default="{ row }">
            <span>{{ row.currentVerifyAmount }}</span>
          </template>
          <template #edit="{ row }">
            <vxe-input
              v-model="row.currentVerifyAmount"
              type="number"
              :max="row.documentAmount"
              min="0"
            />
          </template>
        </vxe-column>
        <vxe-column field="remarks" title="备注" min-width="120" :edit-render="{ name: 'input' }" />
      </vxe-table>

      <vxe-toolbar>
        <template v-if="!isAudited" #tools>
          <Button v-show="form.type == '1'" @click="sourceForm('应收')">选择应收单据</Button>
          <Button v-show="form.type == '2'" @click="sourceForm('应付')">选择应付单据</Button>
          <Button @click="autoReconciliation">自动核销</Button>
        </template>
      </vxe-toolbar>

      <vxe-table
        ref="tableRef2"
        size="mini"
        border
        stripe
        show-overflow
        :row-config="{ height: 40 }"
        :edit-config="isAudited ? undefined : editConfig"
        :data="tableData2"
        :show-footer="showFooter"
        :footer-method="footerMethod2"
      >
        <vxe-column type="seq" title="序号" width="60" align="center" fixed="left" />
        <vxe-column title="操作" field="seq" width="70" align="center">
          <template #default="{ rowIndex }">
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
        </vxe-column>
        <vxe-column field="businessNo" title="源单编号" min-width="140" />
        <vxe-column field="businessType" title="业务类别" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.businessType">
              <span v-if="row.businessType == '2'">期初余额</span>
              <span v-else-if="form.type == '1'">普通销售</span>
              <span v-else>普通采购</span>
            </span>
          </template>
        </vxe-column>
        <vxe-column field="businessDate" title="单据日期" width="120" align="center" />
        <vxe-column field="documentAmount" title="单据金额" width="110" align="right" />
        <vxe-column field="verifiedAmount" title="已核销金额" width="110" align="right" />
        <vxe-column field="unverifiedAmount" title="未核销金额" width="110" align="right" />
        <vxe-column field="businessRemarks" title="源单备注" min-width="120" />
        <vxe-column
          field="currentVerifyAmount"
          title="本次核销金额"
          width="160"
          :edit-render="{}"
        >
          <template #header>
            <span style="color: red">*</span>本次核销金额
          </template>
          <template #default="{ row }">
            <span>{{ row.currentVerifyAmount }}</span>
          </template>
          <template #edit="{ row }">
            <vxe-input
              v-model="row.currentVerifyAmount"
              type="number"
              :max="row.documentAmount"
              min="0"
            />
          </template>
        </vxe-column>
        <vxe-column field="remarks" title="备注" min-width="120" :edit-render="{ name: 'input' }" />
      </vxe-table>

      <div class="mt-10px"></div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px w-80px">备注说明：</label>
          <Input
            placeholder="请输入备注"
            maxlength="150"
            v-model="form.remarks"
            :disabled="isAudited"
          />
          <div class="verification-extra-actions">
            <Button @click="historyForm()">历史单据</Button>
            <Button :title="logContent">操作日志</Button>
          </div>
        </div>
      </div>
    </div>

    <div class="page-column-footer modal-column-between bg-white-color border">
      <Button :loading="loading" @click="closeWindow">取消</Button>
      <div>
        <template v-if="!isAudited">
          <Button color="primary" :loading="loading" @click="saveForm('add')">保存并新增</Button>
          <Button :loading="loading" @click="saveForm('save')">保存</Button>
          <Button
            v-if="form.orderStatus == '已保存'"
            :loading="loading"
            @click="saveForm('audit', '已审核')"
          >审核</Button>
        </template>
        <Button
          v-else
          :loading="loading"
          @click="batchAudit('已保存')"
        >反审核</Button>
      </div>
    </div>
  </div>
</template>
<script>
import { DialogPlugin, LoadingPlugin, MessagePlugin } from 'tdesign-vue-next';
import { openDialog, closeDialog } from '@common/dialog';
import { h } from 'vue';
import OrderReceipt from '@js/api/fund/OrderReceipt';
import Verification from '@js/api/fund/Verification';
import Customer from '@js/api/basic/Customer';
import Supplier from '@js/api/basic/Supplier';
import OrderStaffForm from './OrderStaffForm';
import sourceForm from './sourceByVerfication.vue';
import { mapState, mapMutations } from 'vuex';
import Stamp from '../common/Stamp.vue';
const Big = require('big.js');
import manba from 'manba';
export default {
  name: 'VerificationList',
  components: { Stamp },
  props: {
    orderId: [String, Number],
    type: String,
    index: Number
  },
  data() {
    const tableData = [{}];
    const tableData2 = [{}];
    const editConfig = {
      trigger: 'click',
      mode: 'cell'
    };

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
      tableData,
      tableData2,
      customerDataList: [],
      supplierDataList: [],
      customOrSupplierBalance: 0,
      orderStaffList: [],
      totalTb1: 0,
      totalTb2: 0,
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
      },
      showFooter: true,
      editConfig
    };
  },
  computed: {
    ...mapState(['user']),
    isAudited() {
      return this.form.orderStatus === '已审核';
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
    clerarData() {
      this.form = {
        type: '1',
        orderDate: manba().format('YYYY-MM-DD')
      };
      this.tableData = [{}];
      this.tableData2 = [{}];
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
      Verification.details({ id: this.orderId })
        .then(({ data: { order, collectionList, itemList } }) => {
          this.form = order;
          this.tableData = collectionList || [];
          this.tableData2 = itemList || [];
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
          .map(({ _X_ROW_KEY, ...rest }) => rest)
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
      this.addEdit(type, params);
    },
    batchAudit(orderStatus) {
      let params = {
        id: this.form.id,
        orderStatus: orderStatus,
        approvedBy: this.$store.state.user.admin.id
      };
      const isAnti = orderStatus === '已保存';
      DialogPlugin.confirm({
        content: isAnti ? '确定反审核该核销单？' : '确定审核该核销单？',
        onConfirm: () => {
          Verification.batchAudit(params)
            .then((success) => {
              if (success) {
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
    footerMethodFormat({ columns, data }, list, totalName) {
      const footerRow = new Array(columns.length).fill('');
      footerRow[0] = '合计';
      columns.forEach((column, index) => {
        if (list.includes(column.property)) {
          let total = 0;
          data.forEach((row) => {
            const value = parseFloat(row[column.property]);
            if (!isNaN(value)) {
              total += value;
            }
          });
          footerRow[index] = total.toFixed(2);
          this[totalName] = total;
        }
      });
      return [footerRow];
    },
    footerMethod({ columns, data }) {
      return this.footerMethodFormat(
        { columns, data },
        [
          'documentAmount',
          'verifiedAmount',
          'unverifiedAmount',
          'currentVerifyAmount'
        ],
        'totalTb1'
      );
    },
    footerMethod2({ columns, data }) {
      return this.footerMethodFormat(
        { columns, data },
        [
          'documentAmount',
          'verifiedAmount',
          'unverifiedAmount',
          'currentVerifyAmount'
        ],
        'totalTb2'
      );
    },
    canDelete(tableData) {
      return tableData.length > 1;
    },
    adjustRows(type, index, tableData) {
      if (type === 'insert') {
        tableData.splice(index + 1, 0, { isNew: true });
      } else if (type === 'delete' && this.canDelete(tableData)) {
        tableData.splice(index, 1);
      }
    },
    doSearch() {
      this.pagination.page = 1;
    },
    addEdit(type, params) {
      this.loading = true;
      Verification.addEdit(params)
        .then(() => {
          MessagePlugin.success('保存成功~');
          if (type == 'save' || type == 'add') {
            this.clerarData();
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
      OrderReceipt.orderStaffList()
        .then(({ data }) => {
          this.orderStaffList = data || [];
        })
        .finally();
    },
    selectBusinessType($event) {
      this.clerarData();
      this.form.type = $event?.type;
    },
    selectPerson(e) {
      this.form.personnelId = e?.id;
      this.customOrSupplierBalance = e?.balance;
      this.form = {
        ...this.form
      };
      this.tableData = [{}];
      this.tableData2 = [{}];
    },
    selectOrderStaff(e) {
      this.form.orderStaffId = e ? e.id : null;
      this.form.orderStaffName = e ? e.name : null;
    },
    addOrderStaff() {
      document.getElementsByClassName('h-dropdown')[0].style.zIndex = 1;
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
        body: h(sourceForm, {
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
                  return item;
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
                  return item;
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

      this.$refs.tableRef.reloadData(this.tableData);
      this.$refs.tableRef2.reloadData(this.tableData2);
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
:deep(.vxe-select > .vxe-input) {
  width: 100%;
  height: 100%;
}

.verification-extra-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}
</style>
