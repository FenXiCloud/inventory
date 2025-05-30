<template>
  <div class="frame-page flex flex-column" style="height: auto !important">
    <vxe-toolbar>
      <template #buttons>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px"> 业务类型</span>

          <div style="position: relative">
            <Select
              v-model="form.type"
              class="w-120px z-index-1"
              :datas="businessTypeList"
              keyName="type"
              titleName="name"
              placeholder="选择业务类型"
            >
            </Select>
          </div>
        </div>
        <div v-show="form.type == '1'" class="h-input-group">
          <span class="h-input-addon ml-8px">
            <span style="color: red">*</span>客户</span
          >

          <div style="position: relative">
            <Select
              v-model="form.personnelName"
              class="w-120px z-index-1"
              :datas="customerDataList"
              keyName="name"
              titleName="name"
              placeholder="选择客户"
              @change="selectPerson($event)"
            >
              <!-- <template #bottom>
                <Button no-border icon="h-icon-plus" @click="addCustomer()"
                  >新建</Button
                >
              </template> -->
            </Select>
          </div>
        </div>
        <div v-show="form.type == '2'" class="h-input-group">
          <span class="h-input-addon ml-8px">
            <span style="color: red">*</span>供应商</span
          >

          <div style="position: relative">
            <Select
              v-model="form.personnelName"
              class="w-120px z-index-1"
              :datas="supplierDataList"
              keyName="id"
              titleName="name"
              placeholder="选择供应商"
              @change="selectPerson($event)"
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
          <!-- <DateRangePicker v-model="dateRange"></DateRangePicker> -->
        </div>
      </template>

      <template #tools>
        <Button @click="saveForm('add')" color="primary">保存并新增</Button>
        <Button @click="saveForm('save')" color="primary">保 存</Button>
        <Button>审 核</Button>
      </template>
    </vxe-toolbar>

    <div class="flex1">
      <vxe-toolbar>
        <template #tools>
          <Button v-show="form.type == '1'" @click="sourceForm()" color=""
            >选择预收单据</Button
          >
          <Button v-show="form.type == '2'" @click="sourceForm()" color=""
            >选择预付单据</Button
          >
        </template>
      </vxe-toolbar>
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
        <vxe-column field="businessNo" title="源单编号"> </vxe-column>
        <vxe-column field="businessType" title="业务类别"> </vxe-column>
        <vxe-column field="businessDate" title="单据日期"> </vxe-column>

        <vxe-column field="documentAmount" title="单据金额"> </vxe-column>
        <vxe-column field="verifiedAmount" title="已核销金额"> </vxe-column>
        <vxe-column field="unverifiedAmount" title="未核销金额"> </vxe-column>
        <vxe-column field="businessRemarks" title="源单备注"> </vxe-column>
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
            <span> {{ row.currentVerifyAmount }}</span>
          </template>
          <template #edit="{ row }">
            <vxe-input
              v-model="row.currentVerifyAmount"
              type="number"
              :max="row.documentAmount"
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

      <vxe-toolbar>
        <template #tools>
          <Button v-show="form.type == '1'" @click="sourceForm()" color=""
            >选择应收单据</Button
          >
          <Button v-show="form.type == '2'" @click="sourceForm()" color=""
            >选择应付单据</Button
          >
          <Button @click="autoReconciliation">自动核销</Button>
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
        <vxe-column field="businessNo" title="源单编号"></vxe-column>
        <vxe-column field="businessType" title="业务类别"></vxe-column>
        <vxe-column field="businessDate" title="单据日期"></vxe-column>
        <vxe-column field="documentAmount" title="单据金额"></vxe-column>
        <vxe-column field="verifiedAmount" title="已核销金额"></vxe-column>
        <vxe-column field="unverifiedAmount" title="未核销金额"></vxe-column>
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

    <vxe-toolbar>
      <template #tools>
        <Button @click="historyForm()" color="">历史单据</Button>
        <!-- <Button>操作日志</Button> -->
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
import manba from "manba";
import { confirm, loading, message } from "heyui.ext";
import { layer } from "@layui/layer-vue";
import { h } from "vue";
import OrderReceipt from "@js/api/fund/OrderReceipt";
import Account from "@js/api/fund/Account";
import PaymentMethod from "@js/api/basic/PaymentMethod";
import Customer from "@js/api/basic/Customer";
import Supplier from "@js/api/basic/Supplier";
import OrderStaffForm from "./OrderStaffForm";
import sourceForm from "./sourceForm.vue";
import { mapState, mapMutations } from "vuex";
// import Stamp from '../common/Stamp.vue';
export default {
  name: "VerificationList",
  // components: { Stamp },
  props: {
    orderId: [String, Number],
    type: String,
    index: Number,
  },
  data() {
    const tableData = [{}];
    const tableData2 = [{}];
    const editConfig = {
      trigger: "click",
      mode: "cell",
    };
    const accountOptions = [
      // { name: '现金', id: 'cash' },
      // { name: '银行', id: 'bank_deposit' },
      // { name: '微信', id: 'wechat_pay' },
      // { name: '支付宝', id: 'alipay' }
    ];

    return {
      val1: [],
      businessTypeList: [
        {
          name: "预收冲应收",
          type: "1",
        },
        {
          name: "预付冲应付",
          type: "2",
        },
      ],
      form: {
        type: "1",
        customerName: null,
        orderStaffName: null,
      },
      tableData,
      tableData2,
      customerDataList: [],
      supplierDataList: [], //供应商列表
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
        total: 0,
      },
      params: {
        filter: null,
        state: null,
        sortCol: null,
        sort: null,
      },

      showFooter: true,

      editConfig,
      accountOptions,
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
    ...mapState(["user"]),
    // currentTabData() {
    //   const tab = this.$store.state.tabs.find(
    //     (tab) => tab.key === this.$store.state.currentTab
    //   );
    //   return tab ? tab.params : {};
    // },
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
        pageSize: this.pagination.pageSize,
      });
    },
  },
  methods: {
    ...mapMutations(["pushTab", "closeSelfTab"]),
    clerarData() {
      this.form = {};
      this.tableData = [{}];
      this.tableData2 = [{}];
    },

    updatePage(type = "add", orderId = null) {
      this.closeSelfTab(this.index);
      // this.pushTab({
      //   keepAlive: false,
      //   key: 'OrderReceiptRecord',
      //   title: '收款单记录'
      // });
      // 打开当前
      this.pushTab({
        keepAlive: false,
        key: "OrderReceiptList",
        params: { type: type, orderId: orderId },
        title: "收款单",
      });
    },
    loadList() {
      this.loading = true;
      // const params = JSON.parse(JSON.stringify(this.queryParams));
      // params.customerIds = params.customerIds.join(',');
      OrderReceipt.details({ id: this.orderId })
        .then(({ data: { orderReceipt, collectionList, itemList } }) => {
          this.form = orderReceipt;
          this.tableData = collectionList || [];
          this.tableData2 = itemList || [];
          // this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
    },
    addForm(type = "add", orderId = null) {
      // this.form = {};
      // this.tableData = [{}];
      // this.tableData2 = [{}];
      // this.updatePage();
    },
    historyForm() {
      this.pushTab({
        keepAlive: false,
        key: "OrderVerificationRecord",
        title: "核销单记录",
      });
    },
    saveForm(type = "add") {
      // this.type = type;
      // console.log(
      //   'saveForm----------------------------------------------------------'
      // );
      let orderReceipt = {
        ...this.form,
        documentSource: 1,
        createdBy: this.user.admin.id,
        updateBy: this.user.admin.id,
        orderStatus: "已保存", //||已审核
      };
      // console.log(this.tableData2);
      const filterEmptyObjects = (arr) =>
        arr
          .map(({ _X_ROW_KEY, ...rest }) => rest)
          .filter((row) => Object.keys(row).length);

      let params = {
        orderReceipt,
        collectionList: filterEmptyObjects(this.tableData),
        itemList: filterEmptyObjects(this.tableData2),
      };

      if (!this.form.customerId) {
        return message.error("请选择客户");
      } else if (
        !this.tableData.length ||
        !this.tableData[0].settlementAccountId
      ) {
        return message.error("请选择结算账户");
      } else if (!this.tableData.length || !this.tableData[0].amount) {
        return message.error("请输入金额");
      }

      this.addEdit(type, params);
    },

    footerMethodFormat({ columns, data }, list, totalName) {
      // 初始化合计行，默认所有列为空字符串
      const footerRow = new Array(columns.length).fill("");

      // 设置第一列为“合计”
      footerRow[0] = "合计";

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
    changeDiscountRate() {
      this.form.collectionAmount = (
        (parseFloat(this.totalTb1) || 0) -
        (parseFloat(this.totalTb2) || 0) +
        (parseFloat(this.form.discountRate) || 0)
      ).toFixed(2);
    },
    footerMethod({ columns, data }) {
      return this.footerMethodFormat(
        { columns, data },
        [
          "documentAmount",
          "verifiedAmount",
          "unverifiedAmount",
          "currentVerifyAmount",
        ],
        "totalTb1"
      );
    },
    footerMethod2({ columns, data }) {
      return this.footerMethodFormat(
        { columns, data },
        [
          "documentAmount",
          "verifiedAmount",
          "unverifiedAmount",
          "currentVerifyAmount",
        ],
        "totalTb2"
      );
    },

    canDelete(tableData) {
      return tableData.length > 1;
    },
    adjustRows(type, index, tableData) {
      if (type === "insert") {
        tableData.splice(index + 1, 0, { isNew: true });
      } else if (type === "delete" && this.canDelete(tableData)) {
        tableData.splice(index, 1);
      }
    },
    doSearch() {
      this.pagination.page = 1;
      // this.loadList();
    },
    addEdit(type, params) {
      this.loading = true;
      OrderReceipt.addEdit(params)
        .then(() => {
          message("保存成功~");
          this.clerarData();
          if (type == "save") {
            this.historyForm();
          }
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
    //加载供应商列表
    loadSupplier() {
      this.loading = true;
      Supplier.select()
        .then(({ data: { results, total } }) => {
          this.supplierDataList = results[0].data || [];
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
    selectPerson(e) {
      this.form.personnelId = e?.id;

      this.form = {
        ...this.form,
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

      console.log(row, "changeAccount");
    },
    changePaymentMethod(value, row) {
      console.log(value, "changePaymentMethod");
      const selectedItem = this.paymentMethodList.find(
        (item) => item.name === value
      );
      if (selectedItem) {
        row.paymentMethodId = selectedItem.id; // 设置 id
      }
    },
    addOrderStaff() {
      document.getElementsByClassName("h-dropdown")[0].style.zIndex = 1;
      this.showForm();
    },

    showForm(entity) {
      let type = 0;
      let layerId = layer.open({
        title: "新增职员",
        shadeClose: false,
        closeBtn: false,
        area: ["600px", "480px"],
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
          },
        }),
      });
    },
    sourceForm() {
      if (!this.form.customerId) {
        return layer.msg("请先选择客户");
      }
      let params = {
        customerId: this.form.customerId,
      };
      let layerId = layer.open({
        title: "选择源单",
        shadeClose: false,
        closeBtn: false,
        area: ["900px", "580px"],
        content: h(sourceForm, {
          params,
          onClose: () => {
            console.log(this.$refs.selectRef);
            layer.close(layerId);
          },
          onSuccess: (checkList) => {
            debugger;
            const merged = new Map(
              this.tableData2.map((item) => [item.salesOrderNo, item])
            );
            checkList.forEach((item) => {
              if (!merged.has(item.salesOrderNo)) {
                merged.set(item.salesOrderNo, item);
              }
            });
            this.tableData2 = Array.from(merged.values())
              .filter((item) => item.salesOrderNo)
              .map((item) => {
                delete item._X_ROW_KEY;
                return item;
              });

            console.log(this.tableData2, "tableData2tableData2");
            // this.loadOrderStaff();
            layer.close(layerId);
          },
        }),
      });
    },
    autoReconciliation() {
      const { type } = this.form; // 1: 预收冲应收, 2: 预付冲应付
      const customerOrSupplierId = this.form.personnelId;
      const totalAvailable = this.getTotalUnverified(); // 获取当前可核销总额

      let remaining = totalAvailable;

      const targetTable = type === "1" ? this.tableData : this.tableData2;

      targetTable.forEach((row) => {
        if (remaining <= 0 || !row.businessNo) return;

        const maxCanVerify = Math.min(row.unverifiedAmount, remaining);
        row.currentVerifyAmount = maxCanVerify;
        remaining -= maxCanVerify;
      });

      this.$message.success("自动核销完成");
    },
    getTotalUnverified() {
      const { type } = this.form;
      const table = type === "1" ? this.tableData : this.tableData2;

      return table.reduce((sum, row) => {
        const unverified = parseFloat(row.unverifiedAmount);
        return sum + (isNaN(unverified) ? 0 : unverifiedAmount);
      }, 0);
    },
  },
  created() {
    console.log(this.orderId, this.type, "orderIdorderId");
    if (this.orderId) {
      this.loadList();
    }
    this.loadCustomer();
    this.loadSupplier();
    this.loadOrderStaff();
    this.loadPaymentMethod();
    this.loadAccountMethod();
  },
};
</script>
<style lang="less" scoped>
:deep(.vxe-select > .vxe-input) {
  width: 100%;
  height: 100%;
}
</style>
