<template>
  <div class="frame-page" style="margin: 0">
    <div class="h-panel">
      <div class="h-panel-body">
        <div class="table-toolbar">
          <div class="table-toolbar-left">
            <div class="h-input-group">
              <Search
                  v-model="params.filter"
                  search-button-theme="h-btn-default"
                  show-search-button
                  class="w-360px pl-8px"
                  placeholder="请输入编码、名称"
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
          <vxe-column title="最高采购价" field="maxPurchasePrice" align="left"/>
          <vxe-column title="最近采购价" field="recentlyPurchasePrice" align="left"/>

          <vxe-column title="零售客户价" field="retailCustomerPrice" align="left"/>
          <vxe-column title="批发客户价" field="wholesaleCustomerPrice" align="left"/>
          <vxe-column title="VIP客户价" field="vipCustomerPrice" align="left"/>

          <vxe-column title="最低销售价" field="minSalesPrice" align="left"/>
          <vxe-column title="最近销售价" field="recentlySalesPrice" align="left"/>
          <vxe-column title="最后修改时间" field="updatedAt" align="left"/>
<!--          <vxe-column title="操作" align="center" fixed="right">-->
<!--            <template #default="{row}">-->
<!--              <div class="flex items-center justify-center">-->
<!--                <span class=" primary-color text-hover ml-10px" @click="showForm(row)" size="s">编辑</span>-->
<!--              </div>-->
<!--            </template>-->
<!--          </vxe-column>-->
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
  </div>
</template>

<script>
import PriceRecord from "@js/api/basic/PriceRecord";
import ProductForm from "@views/basic/ProductForm.vue";
import {layer} from "@layui/layer-vue";
import {h} from "vue";
import ProductPriceForm from "@views/basic/ProductPriceForm.vue";


export default {
  name: "ProductPrice",
  data() {
    return {
      loading: false,
      dataList: [],
      params: {
        filter: null,
        productId:null,
        name: null,
      },
      pagination: {
        page: 1,
        pageSize: 10,
        total: 0
      },
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
    //添加或编辑产品Form
    showForm(entity) {
      let layerId = layer.open({
        title: "产品信息",
        shadeClose: false,
        closeBtn: false,
        area: ['1000px', '680px'],
        content: h(ProductPriceForm, {
          entity,
          onClose: () => {
            layer.close(layerId);
          },
          onSuccess: () => {
            this.doSearch();
            layer.close(layerId);
          }
        })
      });
      // let layerId = layer.open({
      //   title: "规则编码",
      //   shadeClose: false,
      //   area: ['50vw', 'auto'],
      //   content: h(CodeRuleForm, {
      //     CodeRule,
      //     onClose: () => {
      //       layer.close(layerId);
      //     },
      //     onSuccess: () => {
      //       this.doSearch();
      //       layer.close(layerId);
      //     }
      //   })
      // });
    },
    loadList() {
      this.loading = true;
      PriceRecord.productList(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
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
