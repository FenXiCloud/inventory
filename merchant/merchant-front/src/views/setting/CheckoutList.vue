<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line align="center">
        <span style="font-size: 16px">结账日期：</span>
        <t-date-picker
            v-model="billDate"
            :clearable="false"
            allow-input
            style="width: 180px; border-radius: 4px"
        />
        <t-button theme="primary" style="border-radius: 4px" @click="toCheck">结账</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="antiCheckout"
                  :disabled="!canAntiCheckout">反结账</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="showMonthlySummary">月结库存表</t-button>
      </t-space>
    </div>

    <div class="simple-page__hint">
      结账日期不能小于系统启用日期：{{ accountBook.startDate }}，也不能小于或等于上次结账日期: {{ accountBook.checkoutDate }}，结账日期之前的数据只能查询，不能修改。
    </div>

    <!-- 结账前检查报告弹窗 -->
    <t-dialog
        v-model:visible="showCheckReport"
        header="结账前检查报告"
        :width="600"
        :footer="false"
    >
      <div class="check-report">
        <div v-if="checkResult.success" class="check-success">
          <t-icon name="check-circle" style="color: #00b578; font-size: 24px;" />
          <span style="margin-left: 8px; font-size: 16px;">所有检查项通过，可以结账</span>
        </div>
        <div v-else class="check-error">
          <t-icon name="close-circle" style="color: #e34d59; font-size: 24px;" />
          <span style="margin-left: 8px; font-size: 16px;">存在以下问题，请先处理</span>
        </div>

        <div v-if="checkResult.errors && checkResult.errors.length > 0" class="check-items">
          <div v-for="(item, index) in checkResult.errors" :key="'error-' + index" class="check-item error">
            <t-icon name="close-circle" style="color: #e34d59;" />
            <span>{{ item }}</span>
          </div>
        </div>

        <div v-if="checkResult.warnings && checkResult.warnings.length > 0" class="check-items">
          <div v-for="(item, index) in checkResult.warnings" :key="'warning-' + index" class="check-item warning">
            <t-icon name="error-circle" style="color: #ed7b2f;" />
            <span>{{ item }}</span>
          </div>
        </div>

        <div class="check-actions">
          <t-button theme="primary" @click="confirmCheckout" :disabled="!checkResult.success">
            确认结账
          </t-button>
          <t-button variant="outline" @click="showCheckReport = false">关闭</t-button>
        </div>
      </div>
    </t-dialog>

    <!-- 月结库存表弹窗 -->
    <t-dialog
        v-model:visible="showMonthlyDialog"
        header="月结库存表"
        :width="1000"
        :footer="false"
    >
      <div class="monthly-summary">
        <div class="monthly-header">
          <t-select
              v-model="selectedPeriod"
              :options="periodOptions"
              placeholder="选择月份"
              style="width: 200px;"
              @change="loadMonthlySummary"
          />
          <t-button theme="primary" variant="outline" @click="exportMonthlySummary">导出</t-button>
        </div>
        <t-table
            :data="monthlyData"
            :columns="monthlyColumns"
            bordered
            stripe
            size="small"
            max-height="400"
        />
      </div>
    </t-dialog>

    <div class="simple-page__table">
      <t-table
          row-key="id"
          size="medium"
          bordered
          stripe
          hover
          height="100%"
          table-layout="auto"
          :data="dataList"
          :columns="columns"
          :loading="loading"
      />
    </div>

    <div class="simple-page__pager">
      <t-pagination
          v-model:current="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :show-jumper="true"
          :show-page-size="true"
          :popup-props="{ attach: 'body' }"
          @change="onPageChange"
      />
    </div>
  </div>
</template>

<script>
import {MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';
import Checkout from "@js/api/setting/Checkout";
import manba from "manba";
import {mapState} from 'vuex';

export default {
  name: "CheckoutList",
  data() {
    return {
      dataList: [],
      loading: false,
      billDate: manba().format("YYYY-MM-dd"),
      startDate: null,
      checkoutDate: null,
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      columns: [
        {colKey: 'serial-number', title: '序号', width: 60},
        {colKey: 'checkDate', title: '结账日', minWidth: 140},
        {colKey: 'createDate', title: '操作日期', minWidth: 140},
        {colKey: 'checkName', title: '操作员', minWidth: 120}
      ],
      showCheckReport: false,
      checkResult: {
        success: false,
        errors: [],
        warnings: []
      },
      showMonthlyDialog: false,
      selectedPeriod: null,
      monthlyData: [],
      monthlyColumns: [
        {colKey: 'productCode', title: '商品编码', width: 120},
        {colKey: 'productName', title: '商品名称', width: 150},
        {colKey: 'warehouseName', title: '仓库', width: 100},
        {colKey: 'unitName', title: '单位', width: 60},
        {colKey: 'beginQty', title: '期初数量', width: 100, align: 'right'},
        {colKey: 'beginAmount', title: '期初金额', width: 100, align: 'right'},
        {colKey: 'inQty', title: '入库数量', width: 100, align: 'right'},
        {colKey: 'inAmount', title: '入库金额', width: 100, align: 'right'},
        {colKey: 'outQty', title: '出库数量', width: 100, align: 'right'},
        {colKey: 'outAmount', title: '出库金额', width: 100, align: 'right'},
        {colKey: 'endQty', title: '期末数量', width: 100, align: 'right'},
        {colKey: 'endAmount', title: '期末金额', width: 100, align: 'right'},
        {colKey: 'endCostPrice', title: '期末成本价', width: 100, align: 'right'},
      ],
      periodOptions: [],
    }
  },
  computed: {
    ...mapState(['accountBook']),
    queryParams() {
      return {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
      }
    },
    canAntiCheckout() {
      // 判断是否可以反结账（有结账记录）
      return this.dataList && this.dataList.length > 0;
    }
  },
  methods: {
    loadList() {
      this.loading = true;
      Checkout.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    toCheck() {
      if (!this.billDate) {
        MessagePlugin.error("请选择结账时间");
        return;
      }

      // 执行结账前检查
      Checkout.preCheck({checkDate: this.billDate}).then(({data}) => {
        this.checkResult = data;
        this.showCheckReport = true;
      }).catch(error => {
        MessagePlugin.error("检查失败：" + (error?.message || "请检查后端服务是否启动"));
      });
    },
    confirmCheckout() {
      if (!this.checkResult.success) {
        MessagePlugin.error("存在未通过的检查项，请先处理");
        return;
      }

      this.showCheckReport = false;

      // 执行结账
      Checkout.toCheck({checkDate: this.billDate}).then(({data, success}) => {
        if (success) {
          MessagePlugin.success("结账成功~");
          this.$store.commit('updateAccountBook', data);
          window.location.replace("/");
        }
      }).finally(() => {
        this.loadList();
      });
    },
    antiCheckout() {
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认要反结账吗?`,
        onConfirm: () => {
          Checkout.antiCheckout().then(({data}) => {
            MessagePlugin.success("操作成功~");
            this.$store.commit('updateAccountBook', data);
            this.loadList();
          })
        }
      })
    },
    showMonthlySummary() {
      // 生成月份选项
      this.generatePeriodOptions();
      this.showMonthlyDialog = true;
    },
    generatePeriodOptions() {
      // 从结账历史中获取月份选项
      Checkout.history().then(({data}) => {
        if (data && data.length > 0) {
          this.periodOptions = data
              .filter(item => item.status === 1)
              .map(item => ({
                label: item.closePeriod,
                value: item.closePeriod
              }));
          if (this.periodOptions.length > 0) {
            this.selectedPeriod = this.periodOptions[0].value;
            this.loadMonthlySummary();
          }
        }
      });
    },
    loadMonthlySummary() {
      if (!this.selectedPeriod) return;

      Checkout.monthlySummary({period: this.selectedPeriod}).then(({data}) => {
        this.monthlyData = data || [];
      });
    },
    exportMonthlySummary() {
      // TODO: 导出月结库存表
      MessagePlugin.info("导出功能开发中...");
    }
  },
  created() {
    this.loadList();
  }
}
</script>

<style scoped>
.simple-page__hint {
  flex-shrink: 0;
  padding: 0 0 8px;
  color: var(--td-text-color-secondary, #666);
  line-height: 1.5;
}

.check-report {
  padding: 16px;
}

.check-success,
.check-error {
  display: flex;
  align-items: center;
  padding: 12px;
  margin-bottom: 16px;
  border-radius: 4px;
}

.check-success {
  background-color: #e8f8f2;
  color: #00b578;
}

.check-error {
  background-color: #fef0f0;
  color: #e34d59;
}

.check-items {
  margin-bottom: 16px;
}

.check-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  gap: 8px;
}

.check-item.error {
  color: #e34d59;
}

.check-item.warning {
  color: #ed7b2f;
}

.check-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

.monthly-summary {
  padding: 16px;
}

.monthly-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
</style>
