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
        <t-select
            v-model="adjustStatus"
            :options="adjustStatusOptions"
            clearable
            placeholder="调整状态"
            style="width: 150px; border-radius: 4px"
            @change="onAdjustStatusChange"
        />
        <t-date-range-picker
            v-model="dateRangeValue"
            clearable
            allow-input
            placeholder="盘点日期"
            style="width: 260px; border-radius: 4px"
        />
        <t-select
            v-model="params.warehouseIds"
            :options="warehouseList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            multiple
            placeholder="仓库"
            style="width: 160px; border-radius: 4px"
        />
        <t-select
            v-model="params.productCategoryIds"
            :options="productCategoryList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            multiple
            placeholder="产品类别"
            style="width: 160px; border-radius: 4px"
        />
        <t-input
            v-model="params.filter"
            clearable
            placeholder="请输入单据编号/仓库名称"
            style="width: 240px; background: #fff; border-radius: 4px"
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
        <template #ops="{ row }">
          <t-space size="small">
            <template v-if="editable(row)">
              <t-link theme="primary" @click="addForm('edit', row.id, row.orderStatus)">编辑</t-link>
              <t-link theme="danger" @click="doRemove(row)">删除</t-link>
            </template>
            <template v-else>
              <t-link theme="primary" @click="addForm('look', row.id, row.orderStatus)">查看</t-link>
              <t-link
                  v-if="canGenerateInbound(row)"
                  theme="primary"
                  @click="addForm('look', row.id, row.orderStatus, 'inbound')"
              >
                生成盘盈
              </t-link>
              <t-link
                  v-if="canGenerateOutbound(row)"
                  theme="danger"
                  @click="addForm('look', row.id, row.orderStatus, 'outbound')"
              >
                生成盘亏
              </t-link>
            </template>
          </t-space>
        </template>
        <template #orderNoResult="{ row }">
          <template v-if="row.relatedOrders && row.relatedOrders.length">
            <div v-for="(item, index) in row.relatedOrders" :key="index" class="order-no-item">
              <t-link theme="primary" @click="openRelatedOrder(item)">{{ item.label || item.orderNo }}</t-link>
            </div>
          </template>
          <template v-else-if="row.orderNos && row.orderNos.length">
            <div v-for="(item, index) in row.orderNos" :key="index" class="order-no-item">
              {{ item }}
            </div>
          </template>
          <span v-else class="text-muted">-</span>
        </template>
        <template #adjustStatus="{ row }">
          <template v-if="row.orderStatus !== '已审核'">
            <span class="text-muted">-</span>
          </template>
          <t-space v-else size="4px" break-line>
            <t-tag v-if="!row.needInbound && !row.needOutbound" theme="default" variant="light" size="small">
              账实相符
            </t-tag>
            <template v-else>
              <t-tag
                  v-if="row.needInbound"
                  :theme="row.inboundGenerated ? 'success' : 'warning'"
                  variant="light"
                  size="small"
              >
                {{ row.inboundGenerated ? '盘盈已生成' : '待生成盘盈' }}
              </t-tag>
              <t-tag
                  v-if="row.needOutbound"
                  :theme="row.outboundGenerated ? 'success' : 'danger'"
                  variant="light"
                  size="small"
              >
                {{ row.outboundGenerated ? '盘亏已生成' : '待生成盘亏' }}
              </t-tag>
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
import StockTake from "@js/api/inventory/StockTake";
import Warehouse from "@js/api/basic/Warehouse";
import ProductCategory from "@js/api/basic/ProductCategory";
import {mapMutations} from "vuex";
import {MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-DD");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-DD");
export default {
  name: "StockTakeList",
  data() {
    return {
      dataList: [],
      warehouseList: [],
      productCategoryList: [],
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
        warehouseIds: [],
        productCategoryIds: [],
        state: null,
        sortCol: null,
        sort: null,
      },
      adjustStatus: null,
      dateRangeValue: [startTime, endTime],
      stateOptions: [
        {label: '未审核', value: '已保存'},
        {label: '已审核', value: '已审核'},
      ],
      adjustStatusOptions: [
        {label: '待生成', value: 'pending'},
        {label: '已完成', value: 'done'},
        {label: '账实相符', value: 'matched'},
      ],
      columns: [
        {colKey: 'row-select', type: 'multiple', width: 46},
        {colKey: 'ops', title: '操作', width: 200, fixed: 'left', align: 'center'},
        {colKey: 'checkDate', title: '盘点日期', width: 120, align: 'center'},
        {colKey: 'orderNo', title: '单据编号', minWidth: 160, ellipsis: true},
        {colKey: 'warehouseName', title: '仓库', minWidth: 120, ellipsis: true},
        {colKey: 'adjustStatus', title: '调整状态', width: 180},
        {colKey: 'orderNoResult', title: '关联单据', minWidth: 180},
        {colKey: 'createdByName', title: '制单人', width: 100, align: 'center'},
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
    addForm(type = 'add', stockTakeId = null, orderStatus = null, autoGenerate = false) {
      this.pushTab({
        key: 'StockTakeForm',
        title: type === 'edit' ? '编辑盘点单' : type === 'look' ? '查看盘点单' : '新增盘点单',
        params: {
          type,
          stockTakeId,
          status: orderStatus,
          autoGenerate
        }
      });
    },
    canGenerateInbound(row) {
      return row.orderStatus === '已审核' && row.needInbound && !row.inboundGenerated;
    },
    canGenerateOutbound(row) {
      return row.orderStatus === '已审核' && row.needOutbound && !row.outboundGenerated;
    },
    onAdjustStatusChange() {
      if (this.adjustStatus) {
        this.params.state = '已审核';
      }
      this.doSearch();
    },
    matchAdjustStatus(row) {
      if (!this.adjustStatus) return true;
      if (row.orderStatus !== '已审核') return false;
      const pendingInbound = row.needInbound && !row.inboundGenerated;
      const pendingOutbound = row.needOutbound && !row.outboundGenerated;
      const matched = !row.needInbound && !row.needOutbound;
      const done = !matched && !pendingInbound && !pendingOutbound;
      if (this.adjustStatus === 'pending') return pendingInbound || pendingOutbound;
      if (this.adjustStatus === 'done') return done;
      if (this.adjustStatus === 'matched') return matched;
      return true;
    },
    openRelatedOrder(item) {
      if (!item || !item.id) return;
      if (item.type === 'outbound') {
        this.pushTab({
          key: 'OtherOutboundForm',
          title: '查看其他出库单',
          params: {type: 'look', otherOutboundId: item.id}
        });
        return;
      }
      this.pushTab({
        key: 'OtherInboundForm',
        title: '查看其他入库单',
        params: {type: 'look', otherInboundId: item.id}
      });
    },
    clearSelection() {
      this.selectedRowKeys = [];
      this.selectedRows = [];
    },
    editable(row) {
      return row.orderStatus === '已保存';
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
          return StockTake.approved('已审核', ids).then(() => {
            MessagePlugin.success("操作成功~");
            this.clearSelection();
            this.loadList();
          });
        }
      });
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
          return StockTake.approved('已保存', ids).then(() => {
            MessagePlugin.success("操作成功~");
            this.clearSelection();
            this.loadList();
          });
        }
      });
    },
    doSearch() {
      this.pagination.page = 1;
      this.clearSelection();
      this.loadList();
    },
    loadList() {
      this.loading = true;
      const params = JSON.parse(JSON.stringify(this.queryParams));
      params.warehouseIds = (params.warehouseIds || []).join(",");
      params.productCategoryIds = (params.productCategoryIds || []).join(",");
      // 调整状态筛选依赖盈亏标记，适当放大页容量再前端过滤
      if (this.adjustStatus) {
        params.state = '已审核';
        params.pageSize = Math.max(params.pageSize || 20, 100);
        params.page = 1;
      }
      StockTake.list(params).then(({data: {results, total}}) => {
        let list = results || [];
        if (this.adjustStatus) {
          list = list.filter((row) => this.matchAdjustStatus(row));
          this.pagination.total = list.length;
          this.pagination.page = 1;
        } else {
          this.pagination.total = total;
        }
        this.dataList = list;
      }).finally(() => this.loading = false);
    },
    loadDict() {
      Promise.all([Warehouse.select(), ProductCategory.select()])
          .then((results) => {
            this.warehouseList = results[0]?.data || [];
            this.productCategoryList = results[1]?.data || [];
          });
    },
    doRemove(row) {
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认删除：${row.orderNo || '当前数据'}?`,
        onConfirm: () => {
          return StockTake.remove(row.id).then(() => {
            MessagePlugin.success("删除成功~");
            this.loadList();
          });
        },
      });
    },
  },
  created() {
    this.loadDict();
    this.loadList();
  }
}
</script>

<style scoped>

.order-no-item {
  margin: 4px 0;
}

.text-muted {
  color: var(--td-text-color-placeholder, #999);
}
</style>
