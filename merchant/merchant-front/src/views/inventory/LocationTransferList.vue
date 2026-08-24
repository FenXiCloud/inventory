<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="addTransfer">新 增</t-button>
        <t-select
            v-model="params.orderStatus"
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
        <template #orderStatus="{ row }">
          <t-tag :theme="statusTheme(row.orderStatus)" variant="light">
            {{ row.orderStatus }}
          </t-tag>
        </template>
        <template #op="{ row }">
          <t-space>
            <t-link theme="primary" @click="viewDetail(row)">查看</t-link>
            <t-link v-if="row.orderStatus === '已保存'" theme="success" @click="approveTransfer(row)">审核</t-link>
            <t-link v-if="row.orderStatus === '已保存'" theme="danger" @click="deleteTransfer(row)">删除</t-link>
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

    <!-- 货位调拨单表单弹窗 -->
    <t-dialog
        v-model:visible="showForm"
        :header="formTitle"
        :width="700"
        :footer="false"
    >
      <location-transfer-form
          :data="currentTransfer"
          @success="onFormSuccess"
          @cancel="showForm = false"
      />
    </t-dialog>
  </div>
</template>

<script>
import {MessagePlugin, DialogPlugin} from "tdesign-vue-next";
import LocationTransfer from "@js/api/inventory/LocationTransfer";
import LocationTransferForm from "./LocationTransferForm.vue";

export default {
  name: "LocationTransferList",
  components: {LocationTransferForm},
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
        orderStatus: null,
        keyword: null
      },
      statusOptions: [
        {label: '已保存', value: '已保存'},
        {label: '已审核', value: '已审核'}
      ],
      columns: [
        {colKey: 'serial-number', title: '序号', width: 60},
        {colKey: 'orderNo', title: '调拨单号', width: 180},
        {colKey: 'transferDate', title: '调拨日期', width: 120},
        {colKey: 'productCode', title: '商品编码', width: 120},
        {colKey: 'productName', title: '商品名称', width: 150},
        {colKey: 'quantity', title: '调拨数量', width: 100, align: 'right'},
        {colKey: 'orderStatus', title: '状态', width: 100, align: 'center'},
        {colKey: 'remark', title: '备注', minWidth: 150},
        {colKey: 'op', title: '操作', width: 150, align: 'center'}
      ],
      showForm: false,
      formTitle: '新增货位调拨',
      currentTransfer: null
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
      LocationTransfer.list(this.queryParams).then(({data: {results, total}}) => {
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
    statusTheme(status) {
      const map = {'已保存': 'warning', '已审核': 'success'};
      return map[status] || 'default';
    },
    addTransfer() {
      this.formTitle = '新增货位调拨';
      this.currentTransfer = null;
      this.showForm = true;
    },
    viewDetail(row) {
      LocationTransfer.getById(row.id).then(({data}) => {
        this.formTitle = '查看货位调拨';
        this.currentTransfer = data;
        this.showForm = true;
      });
    },
    approveTransfer(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认审核调拨单"${row.orderNo}"吗？`,
        onConfirm: () => {
          LocationTransfer.approve(row.id).then(() => {
            MessagePlugin.success('审核成功');
            this.loadList();
          });
        }
      });
    },
    deleteTransfer(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除调拨单"${row.orderNo}"吗？`,
        onConfirm: () => {
          LocationTransfer.delete(row.id).then(() => {
            MessagePlugin.success('删除成功');
            this.loadList();
          });
        }
      });
    },
    onFormSuccess() {
      this.showForm = false;
      this.loadList();
    }
  },
  created() {
    this.loadList();
  }
};
</script>

<style scoped>
</style>
