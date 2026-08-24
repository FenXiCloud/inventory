<template>
  <div class="dashboard">
    <div class="dashboard__head">
      <div class="dashboard__quick">
        <div class="dashboard__quick-btn" @click="openTab('QuickOrder', '快速开单', 200)">
          <t-icon name="add-rectangle" size="28px"/>
          <span>快速开单</span>
        </div>
        <div class="dashboard__quick-btn" @click="openTab('PurchaseInboundForm', '快速入库')">
          <t-icon name="download" size="28px"/>
          <span>快速入库</span>
        </div>
        <div class="dashboard__quick-btn" @click="openTab('InvoiceIssue', '快速开票')">
          <t-icon name="file-attachment" size="28px"/>
          <span>快速开票</span>
        </div>
      </div>
      <div class="dashboard__welcome">
        <div class="dashboard__hello">
          <div class="dashboard__name">{{ user.admin.name }}</div>
          <div class="dashboard__text">你好，欢迎使用进销存管理系统</div>
        </div>
        <div class="dashboard__meta">
          <div class="dashboard__date-label">今日</div>
          <div class="dashboard__date-value">{{ todayText }}</div>
        </div>
      </div>
      <div class="dashboard__calendar">
        <t-date-picker v-model="date" mode="date" :clearable="false"/>
      </div>
    </div>

    <div class="dashboard__metrics">
      <div class="dashboard__metric">
        <div class="dashboard__metric-label">今日销售</div>
        <div class="dashboard__metric-value">¥{{ fmtMoney(metrics.todaySales) }}</div>
      </div>
      <div class="dashboard__metric">
        <div class="dashboard__metric-label">今日毛利</div>
        <div class="dashboard__metric-value dashboard__metric-value--profit">¥{{ fmtMoney(metrics.todayGrossProfit) }}</div>
      </div>
      <div class="dashboard__metric">
        <div class="dashboard__metric-label">今日收款</div>
        <div class="dashboard__metric-value dashboard__metric-value--blue">¥{{ fmtMoney(metrics.todayCollection) }}</div>
      </div>
      <div class="dashboard__metric">
        <div class="dashboard__metric-label">本月开票额</div>
        <div class="dashboard__metric-value">¥{{ fmtMoney(metrics.monthInvoiceAmount) }}</div>
      </div>
    </div>

    <div class="dashboard__charts">
      <div class="dashboard__card dashboard__chart-card">
        <div class="dashboard__card-title">近 7 日销售趋势</div>
        <div ref="trendChart" class="dashboard__chart"></div>
      </div>
      <div class="dashboard__card dashboard__chart-card">
        <div class="dashboard__card-title">销售品类占比</div>
        <div ref="categoryChart" class="dashboard__chart"></div>
      </div>
    </div>

    <div class="dashboard__bottom">
      <div class="dashboard__card dashboard__todo">
        <div class="dashboard__card-title">待办事项</div>
        <div class="dashboard__todo-item" @click="openTab('SalesOrderList', '销售订单')">
          <div class="dashboard__todo-label">待审核单据</div>
          <div class="dashboard__todo-value">{{ todo.pendingAuditCount }} 笔</div>
        </div>
        <div class="dashboard__todo-item" @click="openTab('OrderReceiptList', '收款单')">
          <div class="dashboard__todo-label">待收款</div>
          <div class="dashboard__todo-value dashboard__todo-value--danger">¥{{ fmtMoney(todo.pendingCollection) }}</div>
        </div>
        <div class="dashboard__todo-item" @click="openTab('InvoiceIssue', '蓝字发票')">
          <div class="dashboard__todo-label">待开票</div>
          <div class="dashboard__todo-value">{{ todo.pendingInvoice }} 笔</div>
        </div>
      </div>

      <div class="dashboard__card dashboard__alerts">
        <div class="dashboard__card-title">库存预警</div>
        <div class="dashboard__alerts-grid">
          <div class="dashboard__alert-card" @click="openReport('InventoryWarning', '库存预警', 216)">
            <div class="dashboard__alert-label">缺货预警</div>
            <div class="dashboard__alert-value dashboard__alert-value--danger">{{ summary.shortageCount }}</div>
            <div class="dashboard__alert-desc">低于预警库存</div>
          </div>
          <div class="dashboard__alert-card" @click="openReport('InventoryOverstock', '库存上限预警', 256)">
            <div class="dashboard__alert-label">库存超储</div>
            <div class="dashboard__alert-value dashboard__alert-value--warning">{{ summary.overstockCount }}</div>
            <div class="dashboard__alert-desc">达到/超过库存上限</div>
          </div>
          <div class="dashboard__alert-card" @click="openReport('ShelfLifeList', '保质期管理', 214)">
            <div class="dashboard__alert-label">保质期预警</div>
            <div class="dashboard__alert-value dashboard__alert-value--warning">{{ summary.expiryCount }}</div>
            <div class="dashboard__alert-desc">30 天内到期或已过期</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts/core';
import { LineChart, PieChart } from 'echarts/charts';
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import {mapState} from "vuex";
import manba from "manba";
import {openDialog, closeDialog} from '@common/dialog';
import {h} from "vue";
import AccountBookForm from "@views/setting/AccountBookForm.vue";
import InventoryReport from "@js/api/inventory/InventoryReport";
import Dashboard from "@js/api/Dashboard";

echarts.use([LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer]);

export default {
  name: "DashboardMain",
  data() {
    return {
      date: manba().format("YYYY-MM-dd"),
      summary: {
        shortageCount: 0,
        overstockCount: 0,
        expiryCount: 0,
      },
      metrics: {
        todaySales: 0,
        todayGrossProfit: 0,
        todayCollection: 0,
        monthInvoiceAmount: 0,
      },
      todo: {
        pendingAuditCount: 0,
        pendingCollection: 0,
        pendingInvoice: 0,
      },
      trend: [],
      category: [],
      trendChart: null,
      categoryChart: null,
    }
  },
  computed: {
    ...mapState(['user', 'accountBooks']),
    todayText() {
      return manba().format("YYYY年MM月DD日");
    }
  },
  methods: {
    fmtMoney(v) {
      const n = Number(v) || 0;
      return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    },
    loadWarningSummary() {
      InventoryReport.warningSummary().then(({ data }) => {
        if (data) {
          this.summary = {
            shortageCount: data.shortageCount || 0,
            overstockCount: data.overstockCount || 0,
            expiryCount: data.expiryCount || 0,
          };
        }
      }).catch(() => {});
    },
    loadOverview() {
      Dashboard.overview().then(({ data }) => {
        if (!data) return;
        this.metrics = Object.assign(this.metrics, data.metrics || {});
        this.todo = Object.assign(this.todo, data.todo || {});
        this.trend = data.trend || [];
        this.category = data.category || [];
        this.$nextTick(() => this.renderCharts());
      }).catch(() => {});
    },
    renderCharts() {
      this.renderTrend();
      this.renderCategory();
    },
    renderTrend() {
      if (!this.trendChart) return;
      const x = this.trend.map(t => (t.date || '').slice(5));
      const y = this.trend.map(t => Number(t.sales) || 0);
      this.trendChart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 50, right: 20, top: 30, bottom: 30 },
        xAxis: { type: 'category', data: x, boundaryGap: false },
        yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed' } } },
        series: [{
          name: '销售额',
          type: 'line',
          data: y,
          smooth: true,
          areaStyle: { opacity: 0.12 },
          itemStyle: { color: '#0052d9' },
        }],
      });
    },
    renderCategory() {
      if (!this.categoryChart) return;
      this.categoryChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
        legend: { orient: 'vertical', right: 10, top: 'middle', textStyle: { fontSize: 12 } },
        series: [{
          name: '品类占比',
          type: 'pie',
          radius: ['42%', '68%'],
          center: ['38%', '50%'],
          data: this.category,
          label: { show: false },
          emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.3)' } },
        }],
      });
    },
    handleResize() {
      try {
        if (this.trendChart && this.trendChart.isDisposed?.() === false) {
          this.trendChart.resize();
        }
        if (this.categoryChart && this.categoryChart.isDisposed?.() === false) {
          this.categoryChart.resize();
        }
      } catch (e) {
        // 忽略 resize 错误
      }
    },
    openReport(key, title, menuId) {
      this.$store.commit('pushTab', {
        keepAlive: false,
        key,
        title,
        menuId,
      });
    },
    openTab(key, title, menuId) {
      this.$store.commit('pushTab', {
        keepAlive: false,
        key,
        title,
        menuId,
      });
    },
    addAccountBook() {
      let dialogId = openDialog({
        header: "请先添加账套信息",
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '50vw',
        body: h(AccountBookForm, {
          onClose: () => {
            closeDialog(dialogId);
          },
          onSuccess: () => {
            window.location.replace("/");
            closeDialog(dialogId);
          }
        })
      });
    },
  },
  mounted() {
    this.trendChart = echarts.init(this.$refs.trendChart);
    this.categoryChart = echarts.init(this.$refs.categoryChart);
    window.addEventListener('resize', this.handleResize);
    this.loadOverview();
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize);
    this.trendChart && this.trendChart.dispose();
    this.categoryChart && this.categoryChart.dispose();
  },
  created() {
    if (!this.accountBooks || this.accountBooks === null) {
      this.addAccountBook()
    } else {
      this.loadWarningSummary()
    }
  }
}
</script>

<style scoped>
.dashboard {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-sizing: border-box;
  overflow-y: auto !important;
  padding-bottom: 12px;
}

.dashboard__head {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.dashboard__quick {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dashboard__quick-btn {
  width: 88px;
  height: 88px;
  background: #fff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  color: #0052d9;
  font-size: 13px;
  font-weight: 500;
  border: 1px solid #e7e7e7;
  transition: box-shadow .2s, border-color .2s;
}

.dashboard__quick-btn:hover {
  border-color: #0052d9;
  box-shadow: 0 2px 8px rgba(0, 82, 217, .15);
}

.dashboard__welcome {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 4px;
  padding: 28px 32px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  box-sizing: border-box;
}

.dashboard__name {
  font-size: 28px;
  font-weight: 600;
  color: #1f2329;
  line-height: 1.2;
  margin-bottom: 10px;
}

.dashboard__text {
  font-size: 15px;
  color: #646a73;
}

.dashboard__meta {
  text-align: right;
  flex-shrink: 0;
}

.dashboard__date-label {
  font-size: 13px;
  color: #8f959e;
  margin-bottom: 6px;
}

.dashboard__date-value {
  font-size: 16px;
  color: #1f2329;
  font-weight: 500;
}

.dashboard__calendar {
  flex-shrink: 0;
  background: #fff;
  border-radius: 4px;
  padding: 8px;
  box-sizing: border-box;
}

.dashboard__metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  flex-shrink: 0;
}

.dashboard__metric {
  background: #fff;
  border-radius: 6px;
  padding: 18px 22px;
  box-sizing: border-box;
  border: 1px solid #f0f0f0;
}

.dashboard__metric-label {
  font-size: 13px;
  color: #646a73;
  margin-bottom: 10px;
}

.dashboard__metric-value {
  font-size: 24px;
  font-weight: 600;
  color: #1f2329;
  line-height: 1;
}

.dashboard__metric-value--profit {
  color: #00a870;
}

.dashboard__metric-value--blue {
  color: #0052d9;
}

.dashboard__charts {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 12px;
  flex-shrink: 0;
}

.dashboard__card {
  background: #fff;
  border-radius: 6px;
  padding: 16px 20px;
  box-sizing: border-box;
  border: 1px solid #f0f0f0;
}

.dashboard__card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 12px;
}

.dashboard__chart {
  width: 100%;
  height: 260px;
}

.dashboard__bottom {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 12px;
  flex-shrink: 0;
}

.dashboard__todo-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 4px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}

.dashboard__todo-item:last-child {
  border-bottom: none;
}

.dashboard__todo-item:hover .dashboard__todo-label {
  color: #0052d9;
}

.dashboard__todo-label {
  font-size: 14px;
  color: #1f2329;
  transition: color .2s;
}

.dashboard__todo-value {
  font-size: 14px;
  font-weight: 500;
  color: #1f2329;
}

.dashboard__todo-value--danger {
  color: #e34d59;
}

.dashboard__alerts-grid {
  display: flex;
  gap: 12px;
}

.dashboard__alert-card {
  flex: 1;
  min-width: 120px;
  border: 1px solid #e7e7e7;
  border-radius: 6px;
  padding: 14px 16px;
  cursor: pointer;
  transition: box-shadow .2s, border-color .2s;
  background: #fafafa;
}

.dashboard__alert-card:hover {
  border-color: #0052d9;
  box-shadow: 0 2px 8px rgba(0, 82, 217, .12);
}

.dashboard__alert-label {
  font-size: 13px;
  color: #646a73;
  margin-bottom: 6px;
}

.dashboard__alert-value {
  font-size: 26px;
  font-weight: 600;
  line-height: 1;
  margin-bottom: 8px;
}

.dashboard__alert-value--danger {
  color: #e34d59;
}

.dashboard__alert-value--warning {
  color: #ed7b2f;
}

.dashboard__alert-desc {
  font-size: 12px;
  color: #8f959e;
}
</style>
