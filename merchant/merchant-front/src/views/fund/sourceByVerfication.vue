<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <vxe-table
        row-id="id"
        ref="table"
        :data="dataList"
        highlight-hover-row
        show-overflow
        :row-config="{ height: 48 }"
        :column-config="{ resizable: true }"
        :sort-config="{ remote: true }"
        :loading="loading"
      >
        <vxe-column type="checkbox" width="40" align="center" />
        <vxe-column
          v-for="(item, index) in tableColumns"
          :key="index + 'tableJson'"
          :title="item.title"
          :field="item.field"
          :width="item.width"
        >
          <template
            #default="{ row }"
            v-if="item.field == 'businessType' || item.field == 'orderType'"
          >
            <span v-if="params.sourceType == '预收'">收款</span>
            <span v-if="params.sourceType == '应收'">{{row.businessType != 2 ? "普通销售" : "期初余额"}}</span>
            <span v-if="params.sourceType == '预付'">付款</span>
            <span v-if="params.sourceType == '应付'">{{
              row.businessType != 2 ? "普通采购" : "期初余额"
            }}</span>
          </template>
        </vxe-column>
      </vxe-table>
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
          'Total',
        ]"
      >
      </vxe-pager>
      <!-- </div> -->
    </div>

    <div class="modal-column-between">
      <Button @click="$emit('close')" :loading="loading"> 取消 </Button>
      <Button color="primary" @click="confirm" :loading="loading">
        保存
      </Button>
    </div>
  </div>
</template>

<script>
/**
 * @功能描述: 账户FORM
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */

import { message } from "heyui.ext";
import OrderReceipt from "@js/api/fund/OrderReceipt";
import OrderPayment from "@js/api/fund/OrderPayment";
import { add, objectEach } from "xe-utils";
// import {CopyObj} from "@common/utils";

export default {
  name: "sourceForm",
  props: { params: Object },
  data() {
    const now = new Date();
    const year = now.getFullYear(); // 获取年份
    const month = now.getMonth() + 1; // 获取月份（0-11，需要+1）
    const day = now.getDate(); // 获取日期

    // 格式化为 "YYYY-MM-DD"
    const today = `${year}-${month < 10 ? "0" + month : month}-${
      day < 10 ? "0" + day : day
    }`;
    return {
      loading: false,
      model: {},
      validationRules: {},
      dataList: [],
      pagination: {
        page: 1,
        pageSize: 5,
        total: 0,
      },
      tableJson: [],
      customBalance:0,
      openingBalance: {
        salesOrderId: "-1",
        salesOrderNo: "期初余额",
        businessType: 2,
        businessDate: today,
        documentAmount: 0,
        verifiedAmount: 0,
        unverifiedAmount: 0,
      },
    };
  },
  computed: {
    tableColumns() {
      return this.tableJson.filter((i) => !i.none);
    },
  },
  methods: {
    loadList() {
      this.loading = true;
      this.openingBalance.documentAmount = this.params.balance;
      this.openingBalance.unverifiedAmount = this.params.balance;
      if (this.params.sourceType == "预收") {
        this.getAdvanceReceipt();
      } else if (this.params.sourceType == "应收") {
        this.getAccountsReceivable();
      } else if (this.params.sourceType == "预付") {
        this.getAdvancePayment();
      } else if (this.params.sourceType == "应付") {
        this.getAccountsPayable();
      }
    },
    /* 预收源单列表 */
    getAdvanceReceipt() {
      this.tableJson = [
        {
          title: "源单ID",
          field: "id",
          toField: "businessId",
          none: true,
        },
        {
          title: "源单编号",
          field: "orderNo",
          toField: "businessNo",
          width: 150,
        },
        {
          title: "业务类别",
          field: "orderType",
          toField: "businessType",
          width: 100,
        },
        {
          title: "单据日期",
          field: "orderDate",
          toField: "businessDate",
          width: 130,
        },
        {
          title: "单据金额",
          field: "shouldVerificationAmount",
          toField: "documentAmount",
          width: 120,
        },
        {
          title: "已核销金额",
          field: "hasVerificationAmount",
          toField: "verifiedAmount",
          width: 120,
        },

        {
          title: "未核销金额",
          field: "notVerificationAmount",
          toField: "unverifiedAmount",
          width: 120,
        },
        {
          title: "备注",
          field: "remarks",
          toField: "businessRemarks",
        },
      ];
      OrderReceipt.list({
        writeOff: 1,
        customerId: this.params.personnelId,
        orderType: 2,
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
      })
        .then(({ data: { results, total } }) => {
          this.dataList = results || [];
          this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
    },
    /* 应收源单列表 */
    getAccountsReceivable() {
      this.tableJson = [
        {
          title: "源单ID",
          field: "salesOrderId",
          toField: "businessId",
          none: true,
        },
        {
          title: "源单编号",
          field: "salesOrderNo",
          toField: "businessNo",
          width: 200,
        },
        {
          title: "业务类别",
          field: "businessType",
          toField: "businessType",
          width: 100,
        },
        {
          title: "单据日期",
          field: "businessDate",
          toField: "businessDate",
          width: 130,
        },
        {
          title: "单据金额",
          field: "documentAmount",
          toField: "documentAmount",
          width: 120,
        },
        {
          title: "已核销金额",
          field: "verifiedAmount",
          toField: "verifiedAmount",
          width: 120,
        },
        {
          title: "未核销金额",
          field: "unverifiedAmount",
          toField: "unverifiedAmount",
        },
      ];
      OrderReceipt.writeOffTheOrder({
        customerId: this.params.personnelId,
        orderType: this.params.type,
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
      })
        .then(({ data: { results, total } }) => {
          this.dataList = [this.openingBalance]
          this.dataList = this.dataList.concat(...results || [])
          this.pagination.total = total + 1;
        })
        .finally(() => (this.loading = false));
    },
    /* 预付源单列表 */
    getAdvancePayment() {
      this.tableJson = [
        {
          title: "源单ID",
          field: "id",
          toField: "businessId",
          none: true,
        },
        {
          title: "源单编号",
          field: "orderNo",
          toField: "businessNo",
          width: 150,
        },
        {
          title: "业务类别",
          field: "orderType",
          toField: "businessType",
          width: 100,
        },
        {
          title: "单据日期",
          field: "orderDate",
          toField: "businessDate",
          width: 130,
        },
        {
          title: "单据金额",
          field: "shouldVerificationAmount",
          toField: "documentAmount",
          width: 120,
        },
        {
          title: "已核销金额",
          field: "hasVerificationAmount",
          toField: "verifiedAmount",
          width: 120,
        },

        {
          title: "未核销金额",
          field: "notVerificationAmount",
          toField: "unverifiedAmount",
          width: 120,
        },
        {
          title: "备注",
          field: "remarks",
          toField: "businessRemarks",
        },
      ];
      OrderPayment.list({
        writeOff: 1,
        orderType: 2,
        supplierId: this.params.personnelId,
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
      })
        .then(({ data: { results, total } }) => {
          this.dataList = results || [];
          this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
    },
    /* 应付源单列表 */
    getAccountsPayable() {
      this.tableJson = [
        {
          title: "源单ID",
          field: "salesOrderId",
          toField: "businessId",
          none: true,
        },
        {
          title: "源单编号",
          field: "salesOrderNo",
          toField: "businessNo",
          width: 200,
        },
        {
          title: "业务类别",
          field: "businessType",
          toField: "businessType",
          width: 100,
        },
        {
          title: "单据日期",
          field: "businessDate",
          toField: "businessDate",
          width: 130,
        },
        {
          title: "单据金额",
          field: "documentAmount",
          toField: "documentAmount",
          width: 120,
        },
        {
          title: "已核销金额",
          field: "verifiedAmount",
          toField: "verifiedAmount",
          width: 120,
        },
        {
          title: "未核销金额",
          field: "unverifiedAmount",
          toField: "unverifiedAmount",
        },
      ];
      OrderPayment.writeOffTheOrder({
        supplierId: this.params.personnelId,
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
      })
        .then(({ data: { results, total } }) => {
          this.dataList = [this.openingBalance];
          this.dataList = this.dataList.concat(...results || [])
          this.pagination.total = total + 1;
        })
        .finally(() => (this.loading = false));
    },
    confirm() {
      let checkList = this.$refs.table.getCheckboxRecords();
      console.log(checkList, "checkListcheckListcheckList");
      this.$emit("success", checkList, this.tableJson);
    },
  },
  created() {
    this.loadList();
  },
};
</script>
