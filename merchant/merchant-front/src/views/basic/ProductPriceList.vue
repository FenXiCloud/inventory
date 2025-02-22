<template>
  <div class="frame-page" style="margin: 0">
    <div class="h-panel">
      <div class="h-panel-body">
        <div class="table-toolbar">
          <div class="table-toolbar-left">
            <div class="h-input-group">
              <Search
                  v-model="params.name"
                  search-button-theme="h-btn-default"
                  show-search-button
                  class="w-360px pl-8px"
                  placeholder="请输入商品名称"
                  @search="doSearch">
                <i class="h-icon-search"/>
              </Search>
            </div>
          </div>
          <div class="table-toolbar-right">
            <!--            <Button @click="showImport()" >导 入</Button>-->
            <!--            <Button @click="download()" >导 出</Button>-->
          </div>
        </div>
        <vxe-table row-id="id"
                   :stripe="false"
                   ref="table"
                   :data="dataList"
                   highlight-hover-row
                   show-overflow
                   :row-config="{height: 48}"
                   :loading="loading">
          <vxe-column type="seq" width="80" title="#"/>
          <vxe-column title="编码" field="code" align="left" width="100"/>
          <vxe-column title="名称" field="name" align="left"/>
          <vxe-column title="商品类别" field="productCategoryName" align="left"/>
          <vxe-column title="规格" field="specification" align="left"/>
          <vxe-column title="单位" field="unitName" align="left"/>
          <vxe-column title="预计采购价" field="purchasePrice" align="left"/>
          <vxe-column title="销售价（动态列）" field="" align="left"/>
          <vxe-column title="最后修改时间" field="" align="left"/>
          <vxe-column title="操作" align="center" width="200" fixed="right">
            <template #default="{row}">
              <div class="flex items-center justify-center">
                <i class="primary-color h-icon-edit ml-10px" @click=""></i>
              </div>
            </template>
          </vxe-column>
        </vxe-table>
      </div>
    </div>
  </div>
</template>

<script>
import Product from "@js/api/basic/Product";

export default {
  name: "ProductPrice",
  data() {
    return {
      loading: false,
      params: {
        name: null,
      },
      dataList: [],
    }
  },
  computed: {
    queryParams() {
      return Object.assign(this.params, {})
    }
  },
  methods: {
    loadList() {
      this.loading = true;
      Product.list(this.queryParams).then(({data}) => {
        this.dataList = data;
      }).finally(() => this.loading = false);
    },
    doSearch() {
      this.loadList();
    },
  },
  created() {
    this.doSearch();
  }
}
</script>
