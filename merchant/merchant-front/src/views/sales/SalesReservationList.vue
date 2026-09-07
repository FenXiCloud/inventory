<template>
  <div class="simple-page">
    <!-- 流程引导 -->
    <flow-guide
      :current="0"
      :steps="flowSteps"
      title="以销定购流程"
    />

    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" v-auth="'salesReservation:edit'" @click="addForm()">新 增</t-button>
        <t-button variant="outline" style="border-radius: 4px" v-auth="'salesReservation:delete'" @click="batchDelete()">删 除</t-button>
        <t-button variant="outline" style="border-radius: 4px" v-auth="'salesReservation:audit'" @click="approved()">审 核</t-button>
        <t-button variant="outline" style="border-radius: 4px" v-auth="'salesReservation:audit'" @click="backApproved()">反审核</t-button>
        <t-select
            v-model="params.state"
            :options="statusOptions"
            clearable
            placeholder="审核状态"
            style="width: 140px; border-radius: 4px"
        />
        <t-select
            v-model="params.customerId"
            :options="customerList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            placeholder="请选择客户"
            style="width: 180px; border-radius: 4px"
        />
        <t-date-range-picker
            v-model="dateRangeValue"
            clearable
            allow-input
            placeholder="单据日期"
            style="width: 260px; border-radius: 4px"
        />
        <t-input
            v-model="params.filter"
            clearable
            placeholder="请输入单据编号"
            style="width: 220px; background: #fff; border-radius: 4px"
            @enter="doSearch"
        >
          <template #suffixIcon>
            <t-icon name="search" style="cursor:pointer" @click="doSearch"/>
          </template>
        </t-input>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="doSearch">查询</t-button>
      </t-space>
    </div>

    <div class="simple-page__table">
      <t-table
          row-key="id"
          size="medium"
          bordered
          stripe
          hover
          height="100%"
          table-layout="fixed"
          :data="dataList"
          :columns="columns"
          :loading="loading"
          :selected-row-keys="selectedRowKeys"
          @select-change="onSelectChange"
      >
        <template #orderNo="{ row }">
          <t-link theme="primary" hover="color" @click="loadOrder(row)">{{ row.orderNo }}</t-link>
        </template>
        <template #orderStatusText="{ row }">
          <t-tag :theme="getStatusTheme(row.orderStatusText)" variant="light">
            {{ row.orderStatusText }}
          </t-tag>
        </template>
        <template #statusText="{ row }">
          <span class="text-gray-500">{{ row.statusText }}</span>
        </template>
      </t-table>
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
import manba from "manba";
import {mapMutations, mapState} from "vuex";
import {MessagePlugin, DialogPlugin, LoadingPlugin} from "tdesign-vue-next";
import SalesReservation from "@js/api/sales/SalesReservation";
import Customer from "@js/api/basic/Customer";
import FlowGuide from "@/views/components/FlowGuide.vue";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-DD");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-DD");

export default {
  name: "SalesReservationList",
  components: {FlowGuide},
  data() {
    return {
      flowSteps: [
        {title: '销售预订', content: '记录客户选购商品'},
        {title: '进货预订', content: '根据销售预订进货'},
        {title: '采购入库', content: '进货后入库'}
      ],
      dataList: [],
      customerList: [],
      selectedRowKeys: [],
      selectedRows: [],
      loading: false,
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      params: {
        filter: null,
        state: null,
        customerId: null
      },
      dateRangeValue: [startTime, endTime],
      statusOptions: [
        {label: '未审核', value: '已保存'},
        {label: '已审核', value: '已审核'},
      ],
      columns: [
        {colKey: 'row-select', type: 'multiple', width: 46},
        {colKey: 'orderNo', title: '单据编号', width: 150},
        {colKey: 'customerName', title: '客户', width: 150},
        {colKey: 'orderDate', title: '单据日期', width: 120},
        {colKey: 'totalQuantity', title: '总数量', width: 100, align: 'right'},
        {colKey: 'totalAmount', title: '总金额', width: 100, align: 'right'},
        {colKey: 'discountAmount', title: '优惠额', width: 100, align: 'right'},
        {colKey: 'finalAmount', title: '成交金额', width: 100, align: 'right'},
        {colKey: 'statusText', title: '转进货状态', width: 100, align: 'center'},
        {colKey: 'orderStatusText', title: '状态', width: 100, align: 'center'},
        {colKey: 'remarks', title: '备注', width: 200},
        {colKey: 'createdName', title: '创建人', width: 100}
      ]
    }
  },
  computed: {
    ...mapState(['isAdmin', 'accountBook']),
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        start: start || null,
        end: end || null,
      })
    },
  },
  methods: {
    ...mapMutations(['pushTab']),
    onSelectChange(keys, {selectedRowData}) {
      this.selectedRowKeys = keys;
      this.selectedRows = selectedRowData || [];
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    addForm() {
      this.pushTab({
        key: 'SalesReservationForm',
        title: '新增销售预约单',
      });
    },
    loadOrder(row) {
      this.pushTab({
        key: 'SalesReservationForm',
        title: '编辑销售预约单',
        params: {orderId: row.id},
      });
    },
    batchDelete() {
      if (!this.selectedRows.length) return MessagePlugin.warning('请先选择要删除的单据');
      const ids = this.selectedRows.map(r => r.id);
      DialogPlugin.confirm({
        header: "批量删除",
        body: `确认删除选中的 ${ids.length} 张单据？`,
        onConfirm: () => {
          LoadingPlugin(true);
          const tasks = ids.map(id => SalesReservation.delete(id));
          Promise.all(tasks).then(() => {
            MessagePlugin.success(`成功删除 ${ids.length} 张单据`);
            this.clearSelection();
            this.loadList();
          }).finally(() => LoadingPlugin(false));
        }
      });
    },
    clearSelection() {
      this.selectedRowKeys = [];
      this.selectedRows = [];
    },
    approved() {
      if (!this.selectedRows.length) {
        MessagePlugin.error("未选择数据~");
        return;
      }
      const ids = this.selectedRows.filter(val => val.orderStatus == '已保存').map(val => val.id);
      if (!ids.length) {
        MessagePlugin.error("所选数据无需审核~");
        return;
      }
      DialogPlugin.confirm({
        header: "批量审核提示",
        body: `本次审核${ids.length}条?`,
        onConfirm: () => {
          return SalesReservation.approved(ids, '已审核').then(() => {
            MessagePlugin.success("操作成功~");
            this.clearSelection();
            this.loadList();
          })
        }
      })
    },
    backApproved() {
      if (!this.selectedRows.length) {
        MessagePlugin.error("未选择数据~");
        return;
      }
      const ids = this.selectedRows.filter(val => val.orderStatus == '已审核').map(val => val.id);
      if (!ids.length) {
        MessagePlugin.error("所选数据无需反审核~");
        return;
      }
      DialogPlugin.confirm({
        header: "批量反审核提示",
        body: `本次反审核${ids.length}条?`,
        onConfirm: () => {
          return SalesReservation.approved(ids, '已保存').then(() => {
            MessagePlugin.success("操作成功~");
            this.clearSelection();
            this.loadList();
          })
        }
      })
    },
    doSearch() {
      this.pagination.page = 1;
      this.clearSelection();
      this.loadList();
    },
    loadCustomer() {
      Customer.select().then(({data}) => {
        this.customerList = data || [];
      });
    },
    loadList() {
      this.loading = true;
      SalesReservation.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
    getStatusTheme(status) {
      const themeMap = {
        '待审核': 'warning',
        '已审核': 'primary',
        '已转进货': 'success',
        '已完成': 'default'
      };
      return themeMap[status] || 'default';
    },
  },
  created() {
    this.loadCustomer();
    this.loadList();
  }
}
</script>
