<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-input
            v-model="params.name"
            clearable
            placeholder="客户名称"
            style="width: 180px"
            @enter="doSearch"
        />
        <t-button theme="primary" variant="outline" :loading="loading" @click="doSearch">查询</t-button>
      </t-space>
    </div>

    <div class="simple-page__hint">信用额度为空表示不限制；剩余额度 = 信用额度 - 应收余额。</div>

    <div class="simple-page__table">
      <t-table
          row-key="id"
          size="medium"
          bordered
          stripe
          hover
          height="100%"
          :data="dataList"
          :columns="columns"
          :loading="loading"
      >
        <template #balance="{ row }"><span class="num">{{ fmt(row.balance) }}</span></template>
        <template #creditLimit="{ row }">
          <span class="num" v-if="row.creditLimit != null">{{ fmt(row.creditLimit) }}</span>
          <span v-else class="muted">不限制</span>
        </template>
        <template #remaining="{ row }">
          <span class="num" :class="{'warn': row.creditLimit != null && remainingOf(row) < 0}">
            {{ row.creditLimit != null ? fmt(remainingOf(row)) : '—' }}
          </span>
        </template>
        <template #ops="{ row }">
          <t-link theme="primary" @click="openEdit(row)">设置额度</t-link>
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

    <t-dialog
        v-model:visible="dialogVisible"
        header="设置信用额度"
        :confirm-loading="saving"
        @confirm="saveCreditLimit"
    >
      <t-form label-width="90px" :colon="false">
        <t-form-item label="客户">
          <span>{{ current.name }}</span>
        </t-form-item>
        <t-form-item label="应收余额">
          <span>¥{{ fmt(current.balance) }}</span>
        </t-form-item>
        <t-form-item label="信用额度">
          <t-input-number
              v-model="editValue"
              theme="normal"
              :min="0"
              :decimal-places="2"
              placeholder="留空表示不限制"
              style="width: 220px"
          />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script>
import Customer from "@js/api/basic/Customer";
import {MessagePlugin} from "tdesign-vue-next";

export default {
  name: "CreditLimit",
  data() {
    return {
      loading: false,
      saving: false,
      dataList: [],
      params: {name: ''},
      pagination: {page: 1, pageSize: 20, total: 0},
      dialogVisible: false,
      current: {},
      editValue: null,
      columns: [
        {colKey: 'code', title: '客户编码', width: 140},
        {colKey: 'name', title: '客户名称', minWidth: 160, ellipsis: true},
        {colKey: 'categoryName', title: '客户分类', width: 130},
        {colKey: 'balance', title: '应收余额', width: 130, align: 'right'},
        {colKey: 'creditLimit', title: '信用额度', width: 130, align: 'right'},
        {colKey: 'remaining', title: '剩余额度', width: 130, align: 'right'},
        {colKey: 'ops', title: '操作', width: 110, align: 'center'},
      ],
    };
  },
  methods: {
    fmt(v) {
      const n = Number(v) || 0;
      return n.toLocaleString('zh-CN', {minimumFractionDigits: 2, maximumFractionDigits: 2});
    },
    remainingOf(row) {
      return (Number(row.creditLimit) || 0) - (Number(row.balance) || 0);
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
      Customer.list({
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        name: this.params.name || null,
      })
        .then(({data}) => {
          this.dataList = data?.results || [];
          this.pagination.total = data?.total || 0;
        })
        .finally(() => (this.loading = false));
    },
    openEdit(row) {
      this.current = row;
      this.editValue = row.creditLimit != null ? Number(row.creditLimit) : null;
      this.dialogVisible = true;
    },
    saveCreditLimit() {
      this.saving = true;
      Customer.updateCreditLimit({
        id: this.current.id,
        creditLimit: this.editValue == null || this.editValue === '' ? null : this.editValue,
      })
        .then(() => {
          MessagePlugin.success('信用额度已更新');
          this.dialogVisible = false;
          this.loadList();
        })
        .finally(() => (this.saving = false));
    },
  },
  created() {
    this.loadList();
  }
};
</script>

<style scoped>
.num {
  font-variant-numeric: tabular-nums;
}
.muted {
  color: #8f959e;
}
.warn {
  color: #d54941;
}
</style>
