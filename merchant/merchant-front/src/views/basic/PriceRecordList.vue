<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-select
            v-model="params.priceSource"
            :options="priceSourceOptions"
            clearable
            placeholder="价格来源"
            style="width: 160px; border-radius: 4px"
        />
        <t-select
            v-model="params.productIds"
            :options="productList"
            :keys="{ value: 'id', label: 'name' }"
            multiple
            filterable
            clearable
            placeholder="请选择产品"
            style="width: 200px; border-radius: 4px"
        />
        <t-select
            v-model="params.productCategoryIds"
            :options="productCategoryList"
            :keys="{ value: 'id', label: 'name' }"
            multiple
            filterable
            clearable
            placeholder="请选择类别"
            style="width: 200px; border-radius: 4px"
        />
        <t-input
            v-model="params.filter"
            clearable
            placeholder="请输入编码、名称"
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
import Product from '@js/api/basic/Product';
import ProductCategory from '@js/api/basic/ProductCategory';

/**
 * @功能描述: 价格记录表
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: 'PriceRecordList',
  data() {
    return {
      loading: false,
      dataList: [],
      params: {
        filter: '',
        priceSource: null,
        productIds: [],
        productCategoryIds: []
      },
      pagination: {page: 1, pageSize: 10, total: 0},
      productList: [],
      productCategoryList: [],
      priceSourceOptions: [
        {label: '产品价格资料', value: '产品价格资料'},
        {label: '最近采购价格', value: '最近采购价格'},
        {label: '最近销售价格', value: '最近销售价格'}
      ],
      columns: [
        {colKey: 'productCode', title: '编码', width: 100},
        {colKey: 'productName', title: '名称', minWidth: 140, ellipsis: true},
        {colKey: 'productCategory', title: '产品类别', minWidth: 120, ellipsis: true},
        {colKey: 'specification', title: '规格', width: 100, ellipsis: true},
        {colKey: 'unitName', title: '单位', width: 80},
        {colKey: 'unitPrice', title: '价格', width: 100},
        {colKey: 'priceType', title: '价格类型', width: 100},
        {colKey: 'priceSource', title: '价格来源', width: 120},
        {colKey: 'orderDate', title: '创建时间', width: 160}
      ]
    };
  },
  methods: {
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
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        productIds: (this.params.productIds || []).join(','),
        productCategoryIds: (this.params.productCategoryIds || []).join(',')
      };
      if (this.params.filter) query.filter = this.params.filter;
      if (this.params.priceSource) query.priceSource = this.params.priceSource;
      PriceRecord.list(query)
        .then(({data: {results, total}}) => {
          this.dataList = results || [];
          this.pagination.total = total || 0;
        })
        .finally(() => (this.loading = false));
    },
    loadOptions() {
      Promise.all([Product.select(), ProductCategory.select()]).then((results) => {
        this.productList = results[0].data || [];
        this.productCategoryList = results[1].data || [];
      });
    }
  },
  created() {
    this.loadOptions();
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
