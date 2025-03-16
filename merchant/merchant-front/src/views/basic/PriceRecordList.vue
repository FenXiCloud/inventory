<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>

      </template>
      <template #tools>
<!--        <Input id="name" v-model="params.filter" class="flex-1" placeholder="请输入名称"/>-->

        <Select v-model="params.priceSource" class="w-120px" :datas="{商品价格资料:'商品价格资料',最近采购价格:'最近采购价格',最近销售价格:'最近销售价格'}"
                placeholder="价格来源："/>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">商品：</span>
          <Select class="w-178px" filterable :datas="productList" keyName="id" titleName="name"
                  v-model="params.productId" placeholder="请选择商品"  />
        </div>
        <Search v-model.trim="params.filter" search-button-theme="h-btn-default"
                show-search-button class="w-280px ml-8px"
                placeholder="请输入编码、名称" @search="doSearch">
          <i class="h-icon-search"/>
        </Search>


<!--        <Button color="primary" :loading="loading" @click="doSearch">查询</Button>-->
      </template>
    </vxe-toolbar>
    <div class="flex1">
      <vxe-table row-id="id"
                 ref="table"
                 :data="dataList"
                 highlight-hover-row
                 show-overflow
                 stripe
                 :row-config="{height: 48}"
                 :column-config="{resizable: true}"
                 :loading="loading">
        <vxe-column type="seq" width="40" title="#"/>
        <vxe-column title="编码" field="productCode" align="left" width="100"/>
        <vxe-column title="名称" field="productName" align="left"/>
        <vxe-column title="商品类别" field="productCategoryName" align="left"/>
        <vxe-column title="规格" field="specification" align="left"/>
        <vxe-column title="单位" field="unitName" align="left"/>
        <vxe-column title="价格" field="unitPrice" align="left"/>
        <vxe-column title="价格类型" field="priceType" align="left"/>
        <vxe-column title="价格来源" field="priceSource" align="left"/>
        <vxe-column title="创建时间" field="orderDate" align="left"/>
      </vxe-table>
      <vxe-pager perfect @page-change="loadList(false)"
                 v-model:current-page="pagination.page"
                 v-model:page-size="pagination.pageSize"
                 :total="pagination.total"
                 :layouts="[ 'PrevPage', 'Number', 'NextPage', 'Sizes', 'Total']">
        <template #left>
          <vxe-button @click="loadList(false)" type="text" size="mini" icon="h-icon-refresh"
                      :loading="loading"></vxe-button>
        </template>
      </vxe-pager>
    </div>
  </div>
</template>

<script>
import PriceRecord from "@js/api/basic/PriceRecord";
import {confirm, loading, message} from "heyui.ext";
import Product from "@js/api/basic/Product";

/**
 * @功能描述: 价格记录表
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "PriceRecordList",
  data() {
    return {
      loading: false,
      dataList: [],
      params: {
        filter: null,
        productId:null
      },
      pagination: {
        page: 1,
        pageSize: 10,
        total: 0
      },
      productList:[]
    }
  },
  computed: {
    //查询货商参数
    queryParams() {
      return Object.assign(this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
      })
    }
  },
  methods: {
    doSearch() {
      this.loadList();
    },
    loadList() {
      this.loading = true;
      PriceRecord.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);

      Promise.all([
        Product.select(),
      ]).then((results) => {
        this.productList = results[0].data || [];
      }).finally(() => loading.close());
    },
  },
  created() {
    this.loadList();
  }
}
</script>
