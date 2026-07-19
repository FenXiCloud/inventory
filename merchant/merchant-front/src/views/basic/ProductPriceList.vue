<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-input
            v-model="params.filter"
            clearable
            placeholder="请输入编码、名称"
            style="width: 240px; border-radius: 4px"
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
import PriceRecord from '@js/api/basic/PriceRecord';

export default {
  name: 'ProductPriceList',
  data() {
    return {
      loading: false,
      dataList: [],
      params: {filter: ''},
      pagination: {page: 1, pageSize: 10, total: 0},
      columns: [
        {colKey: 'code', title: '编码', width: 100},
        {colKey: 'name', title: '名称', minWidth: 140, ellipsis: true},
        {colKey: 'productCategoryName', title: '产品类别', minWidth: 120, ellipsis: true},
        {colKey: 'specification', title: '规格', width: 100, ellipsis: true},
        {colKey: 'unitName', title: '单位', width: 80},
        {colKey: 'purchasePrice', title: '预计采购价', width: 110},
        {colKey: 'maxPurchasePrice', title: '最高采购价', width: 110},
        {colKey: 'recentlyPurchasePrice', title: '最近采购价', width: 110},
        {colKey: 'retailCustomerPrice', title: '零售客户价', width: 110},
        {colKey: 'wholesaleCustomerPrice', title: '批发客户价', width: 110},
        {colKey: 'vipCustomerPrice', title: 'VIP客户价', width: 100},
        {colKey: 'minSalesPrice', title: '最低销售价', width: 110},
        {colKey: 'recentlySalesPrice', title: '最近销售价', width: 110}
      ]
    };
  },
  methods: {
    loadList() {
      this.loading = true;
      const query = {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize
      };
      if (this.params.filter) query.filter = this.params.filter;
      PriceRecord.productList(query)
        .then(({data: {results, total}}) => {
          this.dataList = results || [];
          this.pagination.total = total || 0;
        })
        .finally(() => (this.loading = false));
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    }
  },
  created() {
    this.loadList();
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
