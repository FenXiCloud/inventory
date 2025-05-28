<template>
  <div class="frame-page flex flex-column" style="height: auto !important">
    <vxe-toolbar>
      <template #buttons>
        <!-- <Search
          v-model.trim="form.filter"
          search-button-theme="h-btn-default"
          show-search-button
          class="w-360px ml-8px"
          placeholder="请输入客户名称"
          @search="doSearch"
        >
          <i class="h-icon-search" />
        </Search> -->
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
              @change="selectCustomer($event)"
            >
              <template #bottom>
                <Button no-border icon="h-icon-plus" @click="addCustomer()"
                  >新建</Button
                >
              </template>
            </Select>
          </div>
        </div>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">总欠款</span>
          <Input v-model="form.totalAmountsOwed" class="w-120px" disabled />
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
          <span class="h-input-addon ml-8px">订单日期</span>
          <DatePicker
            class="w-120px"
            v-model="form.orderDate"
            :format="format"
          ></DatePicker>
          <!-- <DateRangePicker v-model="dateRange"></DateRangePicker> -->
        </div>
      </template>

      <template #tools>
        <Button @click="addForm()" color="primary">保存并新增</Button>
        <Button @click="saveForm()" color="primary">保 存</Button>
        <Button>审 核</Button>
      </template>
    </vxe-toolbar>

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
        <vxe-column
          field="settlementAccount"
          title="结算账户"
          :edit-render="{}"
        >
          <template #header>
            <span style="color: red">*</span>结算账户
          </template>
          <template #default="{ row }">
            <span> {{ row.settlementAccount }}</span>
          </template>
          <template #edit="{ row }">
            <vxe-select
              @change="changeAccount(row.settlementAccount, row)"
              v-model="row.settlementAccount"
              placeholder="请选择"
              :multiple="false"
              transfer
            >
              <vxe-option
                v-for="item in settlementAccount"
                :key="item.id"
                :value="item.name"
                :label="item.name"
              ></vxe-option>
            </vxe-select>
          </template>
        </vxe-column>
        <vxe-column field="amount" title="收款金额" :edit-render="{}">
          <template #header>
            <span style="color: red">*</span>收款金额
          </template>
          <template #default="{ row }">
            <span> {{ row.amount }}</span>
          </template>
          <template #edit="{ row }">
            <vxe-input
              @change="updateFootEvent"
              v-model="row.amount"
              type="number"
              placeholder="请输入数值"
            ></vxe-input>
          </template>
        </vxe-column>

        <!-- <vxe-column field="num1" title="Number" :edit-render="{}">
          <template #edit="{ row }">
            <vxe-input
              v-model="row.num1"
              type="number"
              placeholder="请输入数值"
            ></vxe-input>
          </template>
        </vxe-column> -->
        <vxe-column
          field="paymentMethodName"
          title="结算方式"
          :edit-render="{}"
        >
          <template #default="{ row }">
            <span> {{ row.paymentMethodName }}</span>
          </template>

          <template #edit="{ row }">
            <vxe-select
              @change="changePaymentMethod(row.paymentMethodName, row)"
              v-model="row.paymentMethodName"
              placeholder="请选择"
              :multiple="false"
              transfer
            >
              <vxe-option
                v-for="item in paymentMethodList"
                :key="item.id"
                :value="item.name"
                :label="item.name"
              ></vxe-option>
            </vxe-select>
          </template>
        </vxe-column>
        <!-- <vxe-column
          title="结算号"
          :edit-render="{ name: 'input' }"
        ></vxe-column> -->
        <vxe-column
          field="remarks"
          title="备注"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
        <vxe-column
          field="theOnlineTransactionNumber"
          title="在线交易"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
      </vxe-table>

      <vxe-toolbar>
        <template #tools>
          <Button @click="sourceForm()" color="">选择源单</Button>
          <Button>自动核销</Button>
        </template>
      </vxe-toolbar>
      <vxe-table
        border
        :edit-config="editConfig"
        show-overflow
        :data="tableData2"
        :show-footer="showFooter"
        :footer-method="footerMethod2"
      >
        <vxe-column type="seq" width="70" fixed="left"></vxe-column>
        <vxe-column title="操作" field="seq" width="70" align="center">
          <template #default="{ rowIndex }">
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
        </vxe-column>
        <vxe-column
          field="salesOrderNo"
          title="源单编号"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
        <vxe-column
          field="businessType"
          title="业务类别"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
        <vxe-column
          field="businessDate"
          title="单据日期"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
        <!-- <vxe-column
          title="收款到期日"
          :edit-render="{ name: 'input' }"
        ></vxe-column> -->
        <vxe-column
          field="documentAmount"
          title="单据金额"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
        <vxe-column
          field="verifiedAmount"
          title="已核销金额"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
        <vxe-column
          field="unverifiedAmount"
          title="未核销金额"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
        <vxe-column
          field="currentVerifyAmount"
          title="本次核销金额"
          :edit-render="{}"
        >
          <template #header>
            <span style="color: red">*</span>本次核销金额
          </template>
          <template #default="{ row }">
            <span> {{ row.currentVerifyAmount }}</span>
          </template>
          <template #edit="{ row }">
            <vxe-input
              v-model="row.currentVerifyAmount"
              type="number"
              :max="row.documentAmount"
            ></vxe-input>
          </template>
        </vxe-column>
        <vxe-column
          field="remarks"
          title="单据备注"
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

    <vxe-toolbar>
      <template #buttons>
        <div class="filler-panel">
          <div class="filler-item" style="flex: 1; margin: 5px 0 !important">
            <label class="mr-16px w-80px">整单折扣:</label>
            <Input
              type="number"
              v-model="form.discountRate"
              @blur="changeDiscountRate"
              min="0.00"
            />

            <label class="ml-10px w-90px">本单预收款:</label>
            <Input disabled v-model="form.collectionAmount" />
          </div>
        </div>
      </template>

      <template #tools>
        <Button @click="addForm()" color="">历史单据</Button>
        <Button>操作日志</Button>
      </template>
    </vxe-toolbar>

    <div class="mb-10px"></div>
    <!-- <div class="flex justify-between items-center pt-5px">
      <vxe-pager
        perfect
        @page-change="loadList(false)"
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :layouts="[
          'PrevJump',
          'PrevPage',
          'Number',
          'NextPage',
          'NextJump',
          'Sizes',
          'Total'
        ]"
      >
        <template #left>
          <span class="mr-12px text-16px">总金额：{{ amountTotal }}元</span>
          <vxe-button
            @click="loadList(false)"
            type="text"
            size="mini"
            icon="h-icon-refresh"
            :loading="loading"
          ></vxe-button>
        </template>
      </vxe-pager>
    </div> -->
  </div>
</template>
<script>
import manba from 'manba';
import { layer } from '@layui/layer-vue';
import { h } from 'vue';
import OrderReceipt from '@js/api/fund/OrderReceipt';
import Account from '@js/api/fund/Account';
import PaymentMethod from '@js/api/basic/PaymentMethod';
import Customer from '@js/api/basic/Customer';
// import { mapMutations } from 'vuex';
import OrderStaffForm from './OrderStaffForm';
import sourceForm from './sourceForm.vue';
import { mapState } from 'vuex';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-dd');
// const endTime = manba().endOf(manba.DAY).format('YYYY-MM-dd');

export default {
  name: 'OrderReceiptList',
  data() {
    const tableData = [{}, {}];
    const tableData2 = [{}, {}];
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
      val1: [],

      form: {},
      tableData,
      tableData2,
      customerDataList: [],
      orderStaffList: [],
      paymentMethodList: [],
      totalTb1: 0,
      totalTb2: 0,
      // customerData: {},
      // orderStaffData: {},

      loading: false,
      amountTotal: 0,
      // totalParams: {},
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
      dateRange: {
        start: manba(startTime).format('YYYY-MM-dd')
        // end: manba(endTime).format('YYYY-MM-dd')
      },
      showFooter: true,

      editConfig,
      accountOptions
    };
  },
  watch: {
    calcCollectionAmount(newVal) {
      this.form.collectionAmount = newVal;
    },
    deep: true
  },
  computed: {
    ...mapState(['user']),
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
    saveForm() {
      console.log(
        'saveForm----------------------------------------------------------'
      );
      let orderReceipt = {
        ...this.form,
        documentSource: 1,
        createdBy: this.user.admin.id,
        updateBy: this.user.admin.id,
        orderStatus: '已保存' //||已审核
      };
      // console.log(this.form);
      // console.log(this.tableData);
      // console.log(this.tableData2);

      let params = {
        orderReceipt,
        collectionList: this.tableData,
        itemList: this.tableData2
      };

      this.addEdit(params);
    },
    // ...mapMutations(['pushTab']),
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

      this.form.collectionAmount = (
        (parseFloat(this.totalTb1) || 0) -
        (parseFloat(this.totalTb2) || 0) +
        (parseFloat(this.form.discountRate) || 0)
      ).toFixed(2);

      return [footerRow]; // 返回二维数组用于渲染 footer
    },
    changeDiscountRate() {},
    footerMethod({ columns, data }) {
      return this.footerMethodFormat({ columns, data }, ['amount'], 'totalTb1');
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
      // this.loadList();
    },
    addEdit(params) {
      this.loading = true;
      OrderReceipt.addEdit(params)
        .then(({ data: { results, total } }) => {
          this.dataList = results || [];
          this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
    },
    //加载客户列表
    loadCustomer() {
      this.loading = true;
      Customer.list(this.queryParams)
        .then(({ data: { results, total } }) => {
          this.customerDataList = results || [];
          this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
    },
    //加载业务员列表
    loadOrderStaff() {
      OrderReceipt.orderStaffList()
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
      this.form.customerId = e?.id;
      this.form.totalAmountsOwed = e?.balance;

      this.form = {
        ...this.form
      };
    },
    selectOrderStaff(e) {
      this.form.orderStaffId = e.id;
    },
    changeAccount(value, row) {
      const selectedItem = this.settlementAccount.find(
        (item) => item.name === value
      );
      if (selectedItem) {
        row.settlementAccountId = selectedItem.id; // 设置 id
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
      document.getElementsByClassName('h-dropdown')[0].style.zIndex = 9;
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
    },
    sourceForm() {
      if (!this.form.customerId) {
        return layer.msg('请先选择客户');
      }
      let params = {
        customerId: this.form.customerId
      };
      let layerId = layer.open({
        title: '选择源单',
        shadeClose: false,
        closeBtn: false,
        area: ['900px', '580px'],
        content: h(sourceForm, {
          params,
          onClose: () => {
            console.log(this.$refs.selectRef);
            layer.close(layerId);
          },
          onSuccess: (checkList) => {
            const merged = new Map(
              this.tableData2.map((item) => [item.salesOrderNo, item])
            );
            checkList.forEach((item) => {
              if (!merged.has(item.salesOrderNo)) {
                merged.set(item.salesOrderNo, item);
              }
            });
            this.tableData2 = Array.from(merged.values()).filter(
              (item) => item.salesOrderNo
            );

            console.log(this.tableData2, 'tableData2tableData2');
            // this.loadOrderStaff();
            layer.close(layerId);
          }
        })
      });
    }
  },
  created() {
    this.loadCustomer();
    this.loadOrderStaff();
    this.loadPaymentMethod();
    this.loadAccountMethod();
  }
};
</script>
<style lang="less" scoped>
:deep(.vxe-select > .vxe-input) {
  width: 100%;
  height: 100%;
}
</style>
