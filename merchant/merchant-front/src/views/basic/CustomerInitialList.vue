<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="addForm()">新 增</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="batchDelete">批量删除</t-button>
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
            <t-link theme="primary" @click="addForm('edit', row.id)"><t-icon name="edit"/></t-link>
            <t-link theme="primary" @click="doRemove(row)"><t-icon name="delete"/></t-link>
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
import {DialogPlugin, MessagePlugin} from 'tdesign-vue-next';
import Customer from '@js/api/basic/Customer';
import CustomerInitial from '@js/api/basic/CustomerInitial';

/**
 * @功能描述: 客户交易流水/期初
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
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
        {colKey: 'balanceBefore', title: '期初应收款', width: 120},
        {colKey: 'amount', title: '期初预收款', width: 120},
        {colKey: 'balanceAfter', title: '期初余额', width: 120}
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
        title: '系统提示',
        content: `确认删除：${row.customerName}?`,
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
        title: '系统提示',
        content: '确定批量删除数据？',
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
  justify-content: flex-end;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid var(--td-component-border, #dcdcdc);
}
</style>
