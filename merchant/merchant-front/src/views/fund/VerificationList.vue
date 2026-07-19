<template>
  <div class="frame-page flex flex-column" style="height: auto !important">
    <vxe-toolbar>
      <template #buttons>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px"> 业务类型</span>

          <div style="position: relative">
            <Select
              :deletable="false"
              v-model="form.type"
              class="w-120px z-index-1"
              :datas="businessTypeList"
              keyName="type"
              titleName="name"
              placeholder="选择业务类型"
              @change="selectBusinessType($event)"
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
              :filterable="true"
              v-model="form.personnelName"
              class="w-120px z-index-1"
              :datas="customerDataList"
              keyName="name"
              titleName="name"
              placeholder="选择客户"
              @change="selectPerson($event)"
            >
              <!-- <template #bottom>
                <Button no-border icon="add" @click="addCustomer()"
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
             :filterable="true"
              v-model="form.personnelName"
              class="w-120px z-index-1"
              :datas="supplierDataList"
              keyName="name"
              titleName="name"
              placeholder="选择供应商"
              @change="selectPerson($event)"
            >
              <!-- <template #bottom>
                <Button no-border icon="add" @click="addCustomer()"
                  >新建</Button
                >
              </template> -->
            </Select>
          </div>
        </div>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">业务员</span>
          <Select
            :filterable="true"
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
              <Button no-border icon="add" @click="addOrderStaff()"
                >新建</Button
              >
            </template>
          </Select>
        </div>

        <div class="h-input-group">
          <span class="h-input-addon ml-8px"
            ><span style="color: red">*</span>单据日期</span
          >
          <DatePicker class="w-120px" v-model="form.orderDate"></DatePicker>
          <!-- <DateRangePicker v-model="dateRange"></DateRangePicker> -->
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

    <div class="flex1">
      <vxe-toolbar>
        <template #tools>
          <Button v-show="form.type == '1'" @click="sourceForm('预收')" color=""
            >选择预收单据</Button
          >
          <Button v-show="form.type == '2'" @click="sourceForm('预付')" color=""
            >选择预付单据</Button
          >
        </template>
      </vxe-toolbar>
      <vxe-table
        ref="tableRef"
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
        <!-- <vxe-column
          v-for="(item, index) in tableJson"
          :key="index + 'tableJson'"
          :title="item.title"
          :field="item.toField"
        >
        </vxe-column> -->
        <vxe-column field="businessNo" title="源单编号"></vxe-column>
        <vxe-column field="businessType" title="业务类别">
          <template #default="{ row }">
            <span v-if="row.businessType">{{
              form.type == 1 ? "收款" : "付款"
            }}</span>
          </template>
        </vxe-column>
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
          <Button v-show="form.type == '1'" @click="sourceForm('应收')" color=""
            >选择应收单据</Button
          >
          <Button v-show="form.type == '2'" @click="sourceForm('应付')" color=""
            >选择应付单据</Button
          >
          <Button @click="autoReconciliation">自动核销</Button>
        </template>
      </vxe-toolbar>
      <vxe-table
        border
        ref="tableRef2"
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
        <vxe-column field="businessType" title="业务类别">
          <template #default="{ row }">
            <span v-if="row.businessType">
              <span v-if="row.businessType == '2'">期初余额</span>
              <span v-else-if="form.type =='1'">普通销售</span>
              <span v-else>普通采购</span>
            </span>
          </template>
        </vxe-column>
        <vxe-column field="businessDate" title="单据日期"></vxe-column>
        <vxe-column field="documentAmount" title="单据金额"></vxe-column>
        <vxe-column field="verifiedAmount" title="已核销金额"></vxe-column>
        <vxe-column field="unverifiedAmount" title="未核销金额"></vxe-column>
        <vxe-column field="businessRemarks" title="源单备注"> </vxe-column>

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
        <Button :title="logContent">操作日志</Button>
      </template>
    </vxe-toolbar>

    <div class="mb-10px"></div>
  </div>
</template>
<script>
import { DialogPlugin, LoadingPlugin, MessagePlugin } from "tdesign-vue-next";
import {openDialog, closeDialog} from '@common/dialog';
import { h } from "vue";
import OrderReceipt from "@js/api/fund/OrderReceipt";
import Account from "@js/api/fund/Account";
import Verification from "@js/api/fund/Verification";
import PaymentMethod from "@js/api/basic/PaymentMethod";
import Customer from "@js/api/basic/Customer";
import Supplier from "@js/api/basic/Supplier";
import OrderStaffForm from "./OrderStaffForm";
import sourceForm from "./sourceByVerfication.vue";
import { mapState, mapMutations } from "vuex";
const Big = require("big.js");
import manba from 'manba';
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

    return {
      logContent: null,
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
        orderStaffName: null,
        orderDate: manba().format('YYYY-MM-DD')
      },
      tableData,
      tableData2,
      customerDataList: [],
      supplierDataList: [], //供应商列表
      customOrSupplierBalance:0, //客户或供应商余额
      orderStaffList: [],
      totalTb1: 0,
      totalTb2: 0,

      loading: false,
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
    };
  },
  watch: {},

  computed: {
    ...mapState(["user"]),
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
      this.form = {
        orderDate: manba().format('YYYY-MM-DD')
      };
      this.tableData = [{}];
      this.tableData2 = [{}];
    },
    getLog() {
      // this.logContent
      
      let createName = this.user.admin.name
      let {
        updateName,
        createdAt,
        updateAt,
        approvedName,
        approvedAt,
      } = this.form;
      console.log(createName)
      const logEntries = [
        `制单人: ${createName}`,
        createdAt ? `制单时间: ${createdAt}` : null,
        updateName ? `最后修改人: ${updateName}` : null,
        updateAt ? `最后修改时间: ${updateAt}` : null,
        approvedName ? `审核人: ${approvedName}` : null,
        approvedAt ? `审核时间: ${approvedAt}` : null,
      ].filter((entry) => entry); // 过滤掉 null 的条目

      this.logContent = logEntries.join("\n");
    },
    loadList() {
      this.loading = true;
      // const params = JSON.parse(JSON.stringify(this.queryParams));
      // params.customerIds = params.customerIds.join(',');
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
        key: "OrderVerificationRecord",
        title: "核销单记录",
      });
    },
    saveForm(type = "add", orderStatus = "已保存") {
      let order = {
        ...this.form,
        createdBy: this.user.admin.id,
        updateBy: this.user.admin.id,
        orderStatus: "已保存", //||已审核
      };
      if (!this.form.orderDate || this.form.orderDate == "") {
        return MessagePlugin.warning("请选择单据日期");
      }
      if (type === "audit") {
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
        itemList: filterEmptyObjects(this.tableData2),
      };

      if (this.form.type == "1" && !this.form.personnelId) {
        return MessagePlugin.error("请选择客户");
      } else if (this.form.type == "2" && !this.form.personnelId) {
        return MessagePlugin.error("请选择供应商");
      } else if (!this.tableData[0].businessId || !this.tableData2[0].businessId) {
        return MessagePlugin.error("请选择核销单据");
      }else if(this.tableData2[0].currentVerifyAmount&&this.tableData2[0].currentVerifyAmount == 0){
        return MessagePlugin.error("本次核销金额不可为0");
      }
      const isEqual = this.checkTotalVerificationAmountEqual();

      if (!isEqual) {
        MessagePlugin.warning("请检查本次核销金额");
        return;
      }
      this.addEdit(type, params);
    },
    batchAudit(orderStatus) {
      let params = {
        id: this.form.id,
        orderStatus: orderStatus,
        approvedBy: this.$store.state.user.admin.id,
      };
      DialogPlugin.confirm({
        content: `确定审核订单？`,
        onConfirm: () => {
          Verification.batchAudit(params)
            .then((success) => {
              if (success) {
                if (orderStatus === "已审核") {
                  MessagePlugin.success("审核成功");
                } else {
                  MessagePlugin.success("反审核成功");
                }
                this.loadList(); // Refresh the list
              }
            })
            .finally(() => LoadingPlugin(false));
        },
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

      // 可选：保留两位小数后再比较，防止浮点精度问题
      const fixedSum1 = parseFloat(sumTable1.toFixed(2));
      const fixedSum2 = parseFloat(sumTable2.toFixed(2));

      return fixedSum1 === fixedSum2 && fixedSum1 != 0;
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
      return [footerRow]; // 返回二维数组用于渲染 footer
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
      Verification.addEdit(params)
        .then(() => {
          MessagePlugin.success("保存成功~");
          if (type == "save" || type == "add") {
            this.clerarData();
            this.historyForm();
          }
          if (type == "audit") {
            this.loadList();
          }
        })
        .finally(() => (this.loading = false));
    },
    //加载客户列表
    loadCustomer() {
      this.loading = true;
      Customer.select()
        .then((results) => {
          this.customerDataList = results.data || [];
        })
        .finally(() => (this.loading = false));
    },
    //加载供应商列表
    loadSupplier() {
      this.loading = true;
      Supplier.select()
        .then((results) => {
          this.supplierDataList = results.data || [];
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
    selectBusinessType($event) {
      this.clerarData();
      this.form.type = $event?.type;
    },
    selectPerson(e) {
      this.form.personnelId = e?.id;
      this.customOrSupplierBalance = e?.balance;
      this.form = {
        ...this.form,
      };
      this.tableData = [{}];
      this.tableData2 = [{}];
    },
    selectOrderStaff(e) {
      this.form.orderStaffId = e ? e.id : null;
      this.form.orderStaffName = e ? e.name : null;
    },
    selectBlur() {
      debugger;
    },
    addOrderStaff() {
      // this.$refs.selectRef.hidePanel();
      document.getElementsByClassName("h-dropdown")[0].style.zIndex = 1;
      this.showForm();
    },

    showForm(entity) {
      let type = 0;
      let dialogId = openDialog({
        header: "新增职员",
        closeOnOverlayClick: false,
        closeBtn: false,
        width: "600px",
        body: h(OrderStaffForm, {
          entity,
          type,
          onClose: () => {
            console.log(this.$refs.selectRef);
            closeDialog(dialogId);
          },
          onSuccess: () => {
            this.loadOrderStaff();
            closeDialog(dialogId);
          },
        }),
      });
    },
    sourceForm(sourceType) {
      if (this.form.type == "1" && !this.form.personnelId) {
        return MessagePlugin.warning("请先选择客户");
      }
      if (this.form.type == "2" && !this.form.personnelId) {
        return MessagePlugin.warning("请先选择供应商");
      }
      this.form.sourceType = sourceType;
      let params = { ...this.form };
      params.balance = this.customOrSupplierBalance
      let dialogId = openDialog({
        header: "选择源单",
        closeOnOverlayClick: false,
        closeBtn: false,
        width: "900px",
        body: h(sourceForm, {
          params,
          onClose: () => {
            console.log(this.$refs.selectRef);
            closeDialog(dialogId);
          },
          onSuccess: (checkList, tableJson) => {
            console.log("onsuccess", checkList);
            let tableArr = this.mergeCheckListWithTableJson(
              checkList,
              tableJson
            );
            console.log(tableArr, "tableArr");
            if (
              this.form.sourceType == "预收" ||
              this.form.sourceType == "预付"
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
          },
        }),
      });
    },
    mergeCheckListWithTableJson(checkList, tableJson) {
      return checkList.map((item) => {
        const newItem = {};
        tableJson.forEach((config) => {
          // 根据 tableJson 的配置映射字段
          newItem[config.toField] = item[config.field];
          if (config.toField == "unverifiedAmount") {
            newItem.currentVerifyAmount = 0;
          }
        });

        return newItem;
      });
    },
    autoReconciliation() {
      if (!this.tableData[0].businessId || !this.tableData2[0].businessId) {
        return MessagePlugin.warning("请先选择需要核销的记录");
      }

      this.initTableData();

      const tempTable1 = this.tableData.map((item) => ({ ...item }));
      const tempTable2 = this.tableData2.map((item) => ({ ...item }));
      for (let i = 0; i < tempTable2.length; i++) {
        const table2Item = tempTable2[i];
        let availableAmount = new Big(table2Item.unverifiedAmount || 0);

        if (availableAmount <= 0) continue;

        // 在 table1 中寻找可以核销的项
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

          // 更新核销金额
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

      // 更新原始数据源
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
        table2Total,
      };
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
    setTimeout(() => {
      this.getLog();
    }, 500);
  },
};
</script>
<style lang="less" scoped>
:deep(.vxe-select > .vxe-input) {
  width: 100%;
  height: 100%;
}
</style>
