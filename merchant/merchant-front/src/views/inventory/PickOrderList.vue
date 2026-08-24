<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-select
            v-model="params.status"
            :options="statusOptions"
            filterable
            clearable
            placeholder="状态"
            style="width: 120px; border-radius: 4px"
        />
        <t-input
            v-model="params.keyword"
            clearable
            placeholder="单号"
            style="width: 200px; background: #fff; border-radius: 4px"
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
          table-layout="auto"
          :data="dataList"
          :columns="columns"
          :loading="loading"
      >
        <template #locationType="{ row }">
          <t-tag :theme="row.locationType === 'WHOLE' ? 'primary' : 'warning'" variant="light">
            {{ row.locationType === 'WHOLE' ? '整货区' : '零货区' }}
          </t-tag>
        </template>
        <template #status="{ row }">
          <t-tag :theme="statusTheme(row.status)" variant="light">
            {{ statusText(row.status) }}
          </t-tag>
        </template>
        <template #op="{ row }">
          <t-space>
            <t-link theme="primary" @click="viewDetail(row)">查看</t-link>
            <t-link v-if="row.status === 0" theme="success" @click="startPick(row)">开始拣货</t-link>
            <t-link v-if="row.status === 1" theme="success" @click="completePick(row)">完成拣货</t-link>
            <t-link v-if="row.status === 0" theme="danger" @click="deleteOrder(row)">删除</t-link>
          </t-space>
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

    <!-- 拣货单详情弹窗 -->
    <t-dialog
        v-model:visible="showDetail"
        header="拣货单详情"
        :width="900"
        :footer="false"
    >
      <pick-order-detail
          :data="currentOrder"
          @close="showDetail = false"
      />
    </t-dialog>
  </div>
</template>

<script>
import {MessagePlugin, DialogPlugin} from "tdesign-vue-next";
import PickOrder from "@js/api/inventory/PickOrder";
import PickOrderDetail from "./PickOrderDetail.vue";

export default {
  name: "PickOrderList",
  components: {PickOrderDetail},
  data() {
    return {
      dataList: [],
      loading: false,
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      params: {
        status: null,
        keyword: null
      },
      statusOptions: [
        {label: '待拣货', value: 0},
        {label: '拣货中', value: 1},
        {label: '已完成', value: 2}
      ],
      columns: [
        {colKey: 'serial-number', title: '序号', width: 60},
        {colKey: 'orderNo', title: '拣货单号', width: 180},
        {colKey: 'sourceNo', title: '关联单号', width: 180},
        {colKey: 'pickDate', title: '拣货日期', width: 120},
        {colKey: 'locationType', title: '库区', width: 100, align: 'center'},
        {colKey: 'status', title: '状态', width: 100, align: 'center'},
        {colKey: 'remark', title: '备注', minWidth: 150},
        {colKey: 'op', title: '操作', width: 200, align: 'center'}
      ],
      showDetail: false,
      currentOrder: null
    };
  },
  computed: {
    queryParams() {
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize
      });
    }
  },
  methods: {
    loadList() {
      this.loading = true;
      PickOrder.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    statusText(status) {
      const map = {0: '待拣货', 1: '拣货中', 2: '已完成'};
      return map[status] || '未知';
    },
    statusTheme(status) {
      const map = {0: 'warning', 1: 'primary', 2: 'success'};
      return map[status] || 'default';
    },
    viewDetail(row) {
      PickOrder.getById(row.id).then(({data}) => {
        this.currentOrder = data;
        this.showDetail = true;
      });
    },
    startPick(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: '确认开始拣货？',
        onConfirm: () => {
          PickOrder.updateStatus(row.id, 1).then(() => {
            MessagePlugin.success('已开始拣货');
            this.loadList();
          });
        }
      });
    },
    completePick(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: '确认完成拣货？',
        onConfirm: () => {
          PickOrder.updateStatus(row.id, 2).then(() => {
            MessagePlugin.success('拣货完成');
            this.loadList();
          });
        }
      });
    },
    deleteOrder(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除拣货单"${row.orderNo}"吗？`,
        onConfirm: () => {
          PickOrder.delete(row.id).then(() => {
            MessagePlugin.success('删除成功');
            this.loadList();
          });
        }
      });
    }
  },
  created() {
    this.loadList();
  }
};
</script>

<style scoped>
</style>
