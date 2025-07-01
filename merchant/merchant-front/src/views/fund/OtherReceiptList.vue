<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar class-name="!size--mini">
      <template #tools>
        <Stamp v-if="form.orderStatus === '已审核'" />
      </template>
    </vxe-toolbar>

    <!-- 下拉选择 -->
    <vxe-toolbar>
      <template #buttons>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">
            <span style="color: red">*</span>客户</span
          >

          <div style="position: relative">
            <Select
              v-model="form.customerName"
              class="w-120px z-index-1"
              :datas="customerDataList"
              keyName="name"
              titleName="name"
              placeholder="选择客户"
              :filterable="true"
              @change="selectCustomer($event)"
            >
              <!-- <template #bottom>
                <Button no-border icon="h-icon-plus" @click="addCustomer()"
                  >新建</Button
                >
              </template> -->
            </Select>
          </div>
        </div>

        <div class="h-input-group">
          <span class="h-input-addon ml-8px">业务员</span>
          <Select
            ref="selectRef"
            style="z-index: 1"
            v-model="form.orderStaffName"
            class="w-120px"
            :datas="orderStaffList"
            keyName="name"
            titleName="name"
            placeholder="选择业务员"
            :filterable="true"
            @change="selectOrderStaff($event)"
          >
            <template #bottom>
              <Button no-border icon="h-icon-plus" @click="addOrderStaff()"
                >新建</Button
              >
            </template>
          </Select>
        </div>

        <div class="h-input-group">
          <span class="h-input-addon ml-8px">单据日期</span>
          <DatePicker class="w-120px" v-model="form.orderDate"></DatePicker>
        </div>

        <div class="h-input-group">
          <span class="h-input-addon ml-8px">收款到期日</span>
          <DatePicker
            class="w-120px"
            v-model="form.expirationDate"
          ></DatePicker>
        </div>
      </template>

      <template #tools>
        <Button
          v-if="form.orderStatus != '已审核'"
          @click="saveForm('add')"
          color="primary"
          >保存并新增</Button
        >
        <Button
          v-if="form.orderStatus != '已审核'"
          @click="saveForm('save')"
          color="primary"
          >保 存</Button
        >
        <Button
          v-if="form.orderStatus == '已保存'"
          @click="saveForm('audit', '已审核')"
          >审 核</Button
        >
        <Button
          v-if="form.orderStatus == '已审核'"
          @click="batchAudit('已保存')"
          >反审核</Button
        >
      </template>
    </vxe-toolbar>

    <!-- 表格输入 -->
    <div class="flex1">
      <vxe-table
        border
        show-overflow
        :edit-config="editConfig"
        :data="tableData"
        :show-footer="showFooter"
        :footer-method="footerMethod"
      >
        <vxe-column type="seq" width="70" fixed="left"></vxe-column>
        <vxe-column
          title="操作"
          field="seq"
          width="70"
          align="center"
          fixed="left"
        >
          <template #default="{ rowIndex }">
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
        </vxe-column>
        <vxe-column field="accountTypeName" title="收入类别" :edit-render="{}">
          <template #header>
            <span style="color: red">*</span>收入类别
          </template>
          <template #default="{ row }">
            <span> {{ row.accountTypeName }}</span>
          </template>
          <template #edit="{ row }">
            <vxe-select
              @change="changeAccount(row.accountTypeName, row)"
              v-model="row.accountTypeName"
              placeholder="请选择"
              :multiple="false"
              transfer
            >
              <vxe-option
                v-for="item in accountTypeList"
                :key="item.id"
                :value="item.name"
                :label="item.name"
              ></vxe-option>
            </vxe-select>
          </template>
        </vxe-column>
        <vxe-column field="amount" title="金额" :edit-render="{}">
          <template #header> <span style="color: red">*</span>金额 </template>
          <template #default="{ row }">
            <span> {{ row.amount }}</span>
          </template>
          <template #edit="{ row }">
            <vxe-input
              @change="updateFootEvent"
              v-model="row.amount"
              type="number"
              placeholder="请输入数值"
              min="0"
            ></vxe-input>
          </template>
        </vxe-column>
        <vxe-column
          field="remarks"
          title="备注"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
      </vxe-table>
      <div class="mt-10px"></div>
      <div class="filler-panel">
        <div class="filler-item" style="flex: 1; margin: 5px 0 !important">
          <Textarea
            placeholder="请输入备注"
            maxlength="150"
            style="width: 100%"
            v-model="form.remarks"
          />
        </div>
      </div>
    </div>
    <!-- 底部 -->
    <vxe-toolbar>
      <template #buttons>
        <div class="filler-panel">
          <div class="filler-item" style="flex: 1; margin: 5px 0 !important">
            <label class="mr-16px w-80px">
              <span style="color: red">*</span>结算账户:</label
            >
            <!-- <Input
              type="number"
              v-model="form.discountRate"
              @blur="changeDiscountRate"
              min="0"
            /> -->
            <Select
              v-model="form.settlementAccount"
              :datas="settlementAccount"
              keyName="name"
              titleName="name"
              placeholder="选择结算账户"
              :filterable="true"
              @change="selectAccounter($event)"
            >
            </Select>

            <label class="ml-10px w-90px">收款金额:</label>
            <Input
              @change="form.arrearsAmount = calcArrearsAmount()"
              v-model="form.collectionAmount"
            />

            <label class="ml-10px w-90px">本次欠款:</label>
            <Input disabled v-model="form.arrearsAmount" />
          </div>
        </div>
      </template>

      <template #tools>
        <Button @click="historyForm()" color="">历史单据</Button>
        <!-- <vxe-tooltip theme="light" :content="logContent"> -->
        <Button :title="logContent">操作日志</Button>
        <!-- </vxe-tooltip> -->
      </template>
    </vxe-toolbar>

    <div class="mb-10px"></div>
  </div>
</template>
<script>
import manba from 'manba';
import { confirm, loading, message } from 'heyui.ext';
import { layer } from '@layui/layer-vue';
import { h } from 'vue';
import OtherReceipt from '@js/api/fund/OtherReceipt';
import Account from '@js/api/fund/Account';
import PaymentMethod from '@js/api/basic/PaymentMethod';
import Customer from '@js/api/basic/Customer';
import OrderStaff from '@js/api/basic/OrderStaff';
import AccountType from '@js/api/basic/AccountType';

import OrderStaffForm from './OrderStaffForm';
import { mapState, mapMutations } from 'vuex';
import Stamp from '../common/Stamp.vue';
export default {
  name: 'OtherReceiptList',
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
    const accountOptions = [
      // { name: '现金', id: 'cash' },
      // { name: '银行', id: 'bank_deposit' },
      // { name: '微信', id: 'wechat_pay' },
      // { name: '支付宝', id: 'alipay' }
    ];

    return {
      logContent: null,
      val1: [],

      form: {
        customerName: null,
        orderStaffName: null,
        orderDate: manba().format('YYYY-MM-DD')
      },
      tableData,
      tableData2,
      customerDataList: [],
      orderStaffList: [],
      paymentMethodList: [],
      accountTypeList: [],
      settlementAccount: [],

      totalTb1: 0,
      totalTb2: 0,

      // customerData: {},
      // orderStaffData: {},

      loading: false,
      amountTotal: 0,
      // totalParams: {},
      pagination: {
        page: 1,
        pageSize: 100,
        total: 0
      },
      params: {
        filter: null,
        state: null,
        sortCol: null,
        sort: null
      },

      showFooter: true,

      editConfig,
      accountOptions
    };
  },
  watch: {
    // currentTabData(newVal) {
    //   console.log('tab 参数更新:', newVal);
    //   // 在这里处理参数变化
    // },
    // 监听 store 中的 currentTabData
    // '$store.state.currentTabReceiptRecord': {
    //   handler(newVal) {
    //     console.log('currentTabReceiptRecord changed:', newVal);
    //     if (newVal && newVal.refresh) {
    //       this.loadList();
    //       // 重置刷新标志
    //       this.$store.commit('SET_TAB_DATA_RECEIPTRECORD', null);
    //     }
    //   },
    //   deep: true
    // }
  },

  computed: {
    ...mapState(['user']),
    // currentTabData() {
    //   const tab = this.$store.state.tabs.find(
    //     (tab) => tab.key === this.$store.state.currentTab
    //   );
    //   return tab ? tab.params : {};
    // },

    queryParams() {
      return Object.assign(this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize
      });
    }
  },
  methods: {
    ...mapMutations(['pushTab', 'closeSelfTab']),
    loadAccountType() {
      AccountType.list().then((res) => {
        this.accountTypeList = res.data;
      });
    },
    getLog() {
      // this.logContent
      let {
        createName = this.user.admin.name,
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
      ].filter((entry) => entry); // 过滤掉 null 的条目

      this.logContent = logEntries.join('\n');
    },
    calcCollectionAmount() {
      return (
        (parseFloat(this.totalTb1) || 0) -
        (parseFloat(this.totalTb2) || 0) +
        (parseFloat(this.form.discountRate) || 0)
      ).toFixed(2);
    },
    calcArrearsAmount() {
      return (
        (parseFloat(this.totalTb1) || 0) -
        (parseFloat(this.totalTb2) || 0) -
        (parseFloat(this.form.collectionAmount) || 0)
      ).toFixed(2);
    },

    batchAudit(orderStatus) {
      let params = {
        id: this.form.id,
        orderStatus: orderStatus,
        approvedBy: this.$store.state.user.admin.id
      };
      confirm({
        content: `确定审核订单？`,
        onConfirm: () => {
          OtherReceipt.batchAudit(params)
            .then((success) => {
              if (success) {
                if (orderStatus === '已审核') {
                  message.success('审核成功');
                } else {
                  message.success('反审核成功');
                }
                this.loadList(); // Refresh the list
              }
            })
            .finally(() => loading.close());
        }
      });
    },
    clerarData() {
      this.form = {};
      this.tableData = [{}];
      this.tableData2 = [{}];
    },

    updatePage(type = 'add', orderId = null) {
      this.closeSelfTab(this.index);
      // 打开当前
      this.pushTab({
        keepAlive: false,
        key: 'OtherReceiptList',
        params: { type: type, orderId: orderId },
        title: '其他收入单'
      });
    },
    loadList() {
      this.loading = true;
      // const params = JSON.parse(JSON.stringify(this.queryParams));
      // params.customerIds = params.customerIds.join(',');
      OtherReceipt.details({ id: this.orderId })
        .then(({ data: { order, itemList } }) => {
          this.form = order;
          this.tableData = itemList || [];
          // this.pagination.total = total;
          this.getLog();
        })
        .finally(() => (this.loading = false));
    },
    addForm(type = 'add', orderId = null) {
      // this.form = {};
      // this.tableData = [{}];
      // this.tableData2 = [{}];
      // this.updatePage();
    },
    historyForm() {
      this.pushTab({
        keepAlive: false,
        key: 'OtherReceiptRecord',
        title: '其他收入单记录'
      });
    },
    saveForm(type = 'add', orderStatus = '已保存') {
      // this.type = type;
      // console.log(
      //   'saveForm----------------------------------------------------------'
      // );
      let orderReceipt = {
        // documentSource: 1,
        createdBy: this.user.admin.id,
        updateBy: this.user.admin.id,
        orderStatus: orderStatus, //||已审核

        ...this.form
      };
      if (type === 'audit') {
        orderReceipt.orderStatus = orderStatus;
        orderReceipt.approvedBy = this.user.admin.id;
        orderReceipt.approvedName = this.user.admin.name;
      }
      // console.log(this.tableData2);
      const filterEmptyObjects = (arr) =>
        arr
          .map(({ _X_ROW_KEY, ...rest }) => rest)
          .filter((row) => Object.keys(row).length);

      let params = {
        order: orderReceipt,
        itemList: filterEmptyObjects(this.tableData)
      };

      if (!this.form.customerId) {
        return message.error('请选择客户');
      } else if (!this.form.settlementAccountId) {
        return message.error('请选择结算账户');
      } else if (!this.tableData.length || !this.tableData[0].accountTypeId) {
        return message.error('请选择收入类别');
      } else if (!this.tableData.length || !this.tableData[0].amount) {
        return message.error('请输入金额');
      }

      this.addEdit(type, params);
    },

    footerMethodFormat({ columns, data }, list, totalName) {
      // 初始化合计行，默认所有列为空字符串
      const footerRow = new Array(columns.length).fill('');

      // 设置第一列为“合计”
      footerRow[0] = '合计';

      // 遍历列，仅对需要合计的字段进行计算
      columns.forEach((column, index) => {
        if (list.includes(column.property)) {
          let total = 0;
          data.forEach((row) => {
            const value = parseFloat(row[column.property]);
            if (!isNaN(value)) {
              total += value;
            }
          });
          footerRow[index] = total.toFixed(2); // 将合计值放入对应位置

          this[totalName] = total;
        }
      });

      this.form.collectionAmount = this.calcCollectionAmount();
      this.form.arrearsAmount = this.calcArrearsAmount();

      return [footerRow]; // 返回二维数组用于渲染 footer
    },

    footerMethod({ columns, data }) {
      return this.footerMethodFormat({ columns, data }, ['amount'], 'totalTb1');
    },

    canDelete(tableData) {
      return tableData.length > 1;
    },
    adjustRows(type, index, tableData) {
      if (type === 'insert') {
        tableData.splice(index + 1, 0, {});
      } else if (type === 'delete' && this.canDelete(tableData)) {
        tableData.splice(index, 1);
      }
    },

    addEdit(type, params) {
      this.loading = true;
      OtherReceipt.addEdit(params)
        .then(() => {
          message('提交成功~');
          this.clerarData();
          if (type == 'save') {
            this.historyForm();
          }
        })
        .finally(() => (this.loading = false));
    },
    //加载客户列表
    loadCustomer() {
      this.loading = true;
      Customer.select()
        .then(({ data }) => {
          this.customerDataList = data || [];
          // this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
    },
    //加载业务员列表
    loadOrderStaff() {
      OrderStaff.orderStaffList()
        .then(({ data }) => {
          this.orderStaffList = data || [];
          // this.pagination.total = total;
        })
        .finally();
    },
    loadPaymentMethod() {
      this.loading = true;
      PaymentMethod.list()
        .then(({ data }) => {
          this.paymentMethodList = data;
        })
        .finally(() => (this.loading = false));
    },
    loadAccountMethod() {
      this.loading = true;
      Account.list()
        .then(({ data }) => {
          this.settlementAccount = data;
        })
        .finally(() => (this.loading = false));
    },

    selectCustomer(e) {
      this.form.customerId = e?.id || null;
      // this.form.totalAmountsOwed = e?.balance || null;

      this.form = {
        ...this.form
      };
    },
    selectAccounter(e) {
      this.form.settlementAccountId = e?.id || null;
      // this.form.totalAmountsOwed = e?.balance || null;

      this.form = {
        ...this.form
      };
    },
    selectOrderStaff(e) {
      this.form.orderStaffId = e?.id || null;
    },
    changeAccount(value, row) {
      const selectedItem = this.accountTypeList.find(
        (item) => item.name === value
      );
      if (selectedItem) {
        row.accountTypeId = selectedItem.id; // 设置 id
      }

      console.log(row, 'changeAccount');
    },
    changePaymentMethod(value, row) {
      console.log(value, 'changePaymentMethod');
      const selectedItem = this.paymentMethodList.find(
        (item) => item.name === value
      );
      if (selectedItem) {
        row.paymentMethodId = selectedItem.id; // 设置 id
      }
    },
    addOrderStaff() {
      document.getElementsByClassName('h-dropdown')[0].style.zIndex = 1;
      this.showForm();
    },

    showForm(entity) {
      let type = 0;
      let layerId = layer.open({
        title: '新增职员',
        shadeClose: false,
        closeBtn: false,
        area: ['600px', '480px'],
        content: h(OrderStaffForm, {
          entity,
          type,
          onClose: () => {
            console.log(this.$refs.selectRef);
            layer.close(layerId);
          },
          onSuccess: () => {
            this.loadOrderStaff();
            layer.close(layerId);
          }
        })
      });
    }
  },
  created() {
    console.log(this.orderId, this.type, 'orderIdorderId');
    if (this.orderId) {
      this.loadList();
    }
    this.loadCustomer();
    this.loadOrderStaff();
    this.loadPaymentMethod();
    this.loadAccountMethod();
    this.loadAccountType();
    setTimeout(() => {
      this.getLog();
    }, 500);
    // this.getLog()
  }
};
</script>
<style lang="less" scoped>
:deep(.vxe-select > .vxe-input) {
  width: 100%;
  height: 100%;
}
</style>
