<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button v-auth="'customerInitial:edit'" theme="primary" style="border-radius: 4px" @click="addForm()">新 增</t-button>
        <t-button v-auth="'customerInitial:delete'" variant="outline" style="border-radius: 4px" @click="batchDelete">批量删除</t-button>
        <t-select
            v-model="params.customerIds"
            :options="customerList"
            :keys="{ value: 'id', label: 'name' }"
            multiple
            filterable
            clearable
            placeholder="请选择客户"
            style="width: 200px; border-radius: 4px"
        />
        <t-input
            v-model="params.filter"
            clearable
            placeholder="请输入客户编码/名称"
            style="width: 220px; border-radius: 4px"
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
            <t-link v-auth="'customerInitial:edit'" theme="primary" @click="addForm('edit', row.id)"><t-icon name="edit"/></t-link>
            <t-link v-auth="'customerInitial:delete'" theme="primary" @click="doRemove(row)"><t-icon name="delete"/></t-link>
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
  </div>
</template>

<script>
import {mapMutations} from 'vuex';
import {MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import Customer from '@js/api/basic/Customer';
import CustomerInitial from '@js/api/basic/CustomerInitial';
export default {
  name: 'CustomerInitialList',
  data() {
    return {
      loading: false,
      dataList: [],
      selectedRowKeys: [],
      params: {
        filter: '',
        customerFlowType: '期初',
        customerIds: []
      },
      pagination: {page: 1, pageSize: 10, total: 0},
      customerList: [],
      columns: [
        {colKey: 'row-select', type: 'multiple', width: 46},
        {colKey: 'ops', title: '操作', width: 90, fixed: 'left', align: 'center'},
        {colKey: 'customerCode', title: '客户编码', width: 140},
        {colKey: 'customerName', title: '客户名称', minWidth: 160, ellipsis: true},
        {colKey: 'balanceBefore', title: '期初应收款', width: 120, align: 'right'},
        {colKey: 'amount', title: '期初预收款', width: 120, align: 'right'},
        {colKey: 'balanceAfter', title: '期初应收余额', width: 120, align: 'right'}
      ]
    };
  },
  computed: {
    footData() {
      const sum = (key) =>
        this.dataList.reduce((s, r) => s + Number(r[key] || 0), 0).toFixed(2);
      return [{
        customerCode: '合计',
        balanceBefore: sum('balanceBefore'),
        amount: sum('amount'),
        balanceAfter: sum('balanceAfter')
      }];
    }
  },
  methods: {
    ...mapMutations(['pushTab']),
    onSelectChange(keys) {
      this.selectedRowKeys = keys;
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    loadList() {
      this.loading = true;
      const query = {
        customerFlowType: this.params.customerFlowType,
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        customerIds: (this.params.customerIds || []).join(',')
      };
      if (this.params.filter) query.filter = this.params.filter;
      CustomerInitial.list(query)
        .then(({data: {results, total}}) => {
          this.dataList = results || [];
          this.pagination.total = total || 0;
          this.selectedRowKeys = [];
        })
        .finally(() => (this.loading = false));
    },
    doRemove(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除：${row.customerName}?`,
        onConfirm: () => {
          CustomerInitial.remove(row.id).then(() => {
            MessagePlugin.success('删除成功~');
            this.loadList();
          });
        }
      });
    },
    addForm(type = 'add', customerInitialId = null) {
      this.pushTab({
        keepAlive: false,
        key: 'CustomerInitialForm',
        title: type === 'edit' ? '编辑客户期初余额' : '新增客户期初余额',
        params: {type, customerInitialId}
      });
    },
    batchDelete() {
      if (!this.selectedRowKeys.length) {
        MessagePlugin.error('请选择至少一条数据');
        return;
      }
      DialogPlugin.confirm({
        header: '系统提示',
        body: '确定批量删除数据？',
        onConfirm: () => {
          CustomerInitial.batchDelete({ids: this.selectedRowKeys}).then(() => {
            MessagePlugin.success('批量删除成功');
            this.loadList();
          });
        }
      });
    }
  },
  created() {
    this.loadList();
    Customer.select().then(({data}) => {
      this.customerList = data || [];
    });
  }
};
</script>

