<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="addForm()">新 增</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="approved()">审 核</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="backApproved()">反审核</t-button>
        <t-select
            v-model="params.state"
            :options="stateOptions"
            clearable
            placeholder="审核状态"
            style="width: 140px; border-radius: 4px"
        />
        <t-date-range-picker
            v-model="dateRangeValue"
            clearable
            allow-input
            placeholder="入库日期"
            style="width: 260px; border-radius: 4px"
        />
        <t-select
            v-model="params.supplierId"
            :options="supplierList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            placeholder="请选择供货商"
            style="width: 180px; border-radius: 4px"
        />
        <t-input
            v-model="params.filter"
            clearable
            placeholder="请输入订单号"
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
          :foot-data="footData"
          @select-change="onSelectChange"
      >
        <template #ops="{ row }">
          <t-space size="small">
            <template v-if="row.orderStatus === '已保存'">
              <t-link theme="primary" @click="addForm('edit', row.id)">编辑</t-link>
              <t-link theme="primary" @click="doRemove(row)">删除</t-link>
            </template>
            <template v-else-if="row.orderStatus === '已审核'">
              <t-link theme="primary" @click="detail(row.id)">详情</t-link>
            </template>
          </t-space>
        </template>
        <template #orderStatus="{ row }">
          <t-tag
              :theme="row.orderStatus === '已审核' ? 'success' : 'warning'"
              variant="light"
          >
            {{ row.orderStatus === '已保存' ? '未审核' : row.orderStatus }}
          </t-tag>
        </template>
      </t-table>
    </div>

    <div class="simple-page__pager">
      <span class="simple-page__total">合计金额：{{ amountTotal }}元</span>
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
import {mapMutations} from "vuex";
import {MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';
import PurchaseInbound from "@js/api/purchase/PurchaseInbound";
import Supplier from "@js/api/basic/Supplier";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-DD");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-DD");

/**
 * @功能描述: 采购入库单列表
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "PurchaseInboundList",
  data() {
    return {
      dataList: [],
      supplierList: [],
      selectedRowKeys: [],
      selectedRows: [],
      loading: false,
      amountTotal: 0,
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      params: {
        filter: null,
        state: null,
        sortCol: null,
        sort: null,
        supplierId: null,
      },
      dateRangeValue: [startTime, endTime],
      stateOptions: [
        {label: '未审核', value: '已保存'},
        {label: '已审核', value: '已审核'},
      ],
      columns: [
        {colKey: 'row-select', type: 'multiple', width: 46},
        {colKey: 'ops', title: '操作', width: 110, fixed: 'left', align: 'center'},
        {colKey: 'inboundDate', title: '入库日期', width: 120, align: 'center'},
        {colKey: 'orderNo', title: '订单编号', minWidth: 160, ellipsis: true},
        {colKey: 'purchaseOrderNos', title: '关联采购单', minWidth: 140, ellipsis: true},
        {colKey: 'supplierName', title: '供货商', minWidth: 120, ellipsis: true},
        {colKey: 'totalAmount', title: '采购金额', width: 110, align: 'right'},
        {colKey: 'discountAmount', title: '折扣金额', width: 110, align: 'right'},
        {colKey: 'finalAmount', title: '折后金额', width: 110, align: 'right'},
        {colKey: 'secondarySum', title: '数量', width: 90, align: 'right'},
        {colKey: 'createdName', title: '制单人', width: 90, align: 'center'},
        {colKey: 'createdAt', title: '制单时间', width: 160, align: 'center', ellipsis: true},
        {colKey: 'orderStatus', title: '审核状态', width: 100, align: 'center', fixed: 'right'},
      ]
    }
  },
  computed: {
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        start: start || null,
        end: end || null,
      })
    },
    footData() {
      const sum = (key, digits = 2) => {
        const total = (this.dataList || []).reduce((acc, row) => acc + Number(row[key] || 0), 0);
        return total.toFixed(digits);
      };
      return [{
        ops: '合计',
        totalAmount: sum('totalAmount'),
        discountAmount: sum('discountAmount'),
        finalAmount: sum('finalAmount'),
        secondarySum: sum('secondarySum', 0),
      }];
    }
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
    addForm(type = 'add', orderId = null) {
      this.pushTab({
        key: 'PurchaseInboundForm',
        title: type == 'edit' ? '编辑采购入库单' : '新增采购入库单',
        params: {type: type, orderId: orderId}
      });
    },
    detail(orderId = null) {
      this.pushTab({
        key: 'PurchaseInboundDetail',
        title: '采购入库单',
        params: {orderId: orderId}
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
          PurchaseInbound.approved('已审核', ids).then(() => {
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
      const ids = this.selectedRows
          .filter(val => val.orderStatus == '已审核' && !val.purchaseReturnOrderNo)
          .map(val => val.id);
      if (!ids.length) {
        MessagePlugin.error("所选数据无需反审核~");
        return;
      }
      DialogPlugin.confirm({
        header: "批量反审核提示",
        body: `本次反审核${ids.length}条?`,
        onConfirm: () => {
          PurchaseInbound.approved('已保存', ids).then(() => {
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
      this.loadTotal();
      this.loadList();
    },
    loadSupplier() {
      Supplier.select().then(({data}) => {
        this.supplierList = data || [];
      })
    },
    loadList() {
      this.loading = true;
      PurchaseInbound.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
    loadTotal() {
      PurchaseInbound.total(this.queryParams).then(({data}) => {
        this.amountTotal = data || 0;
      })
    },
    doRemove(row) {
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认删除：${row.orderNo}?`,
        onConfirm: () => {
          PurchaseInbound.remove(row.id).then(() => {
            MessagePlugin.success("删除成功~");
            this.loadList();
          })
        }
      })
    },
  },
  created() {
    this.loadSupplier();
    this.loadList();
    this.loadTotal();
  }
}
</script>

<style scoped>
.simple-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
  padding: 0 12px;
  box-sizing: border-box;
  overflow: hidden;
}

.simple-page__toolbar {
  flex-shrink: 0;
  padding: 8px 0;
}

.simple-page__table {
  flex: 1 1 0;
  height: 0;
  min-height: 0;
  overflow: hidden;
}

.simple-page__pager {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid var(--td-component-border, #dcdcdc);
  background: #fff;
}

.simple-page__total {
  font-size: 14px;
  color: #333639;
  flex-shrink: 0;
}
</style>
