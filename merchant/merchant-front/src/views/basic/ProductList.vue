<template>
  <div class="frame-page flex flex-column">
    <div class="parent_container">
      <div class="left">
        <vxe-table
            ref="productCategoryGridRef"
            size="mini"
            :data="productCategoryDataList"
            highlight-hover-row
            show-overflow
            @radio-change="onProductCategoryChange"
            :rowConfig="{isCurrent: true,isHover: true}"
            :tree-config="{transform:true, rowField: 'id', parentField: 'pid'}"
            :radio-config="{trigger: 'row',labelField: 'name',highlight: true}">
          <vxe-column field="name" tree-node title="分类名称"></vxe-column>
          <vxe-column title="" align="center" width="120">
            <template #default="{row}">
              <template v-if="row.id !=null">
                <i class="primary-color h-icon-plus ml-10px" @click="showProductCategoryForm(null,row)"></i>
                <i class="primary-color h-icon-edit ml-10px" @click="showProductCategoryForm(row)"></i>
                <i class="primary-color h-icon-trash ml-10px" @click="deleteProductCategory(row)"></i>
              </template>
            </template>
          </vxe-column>
        </vxe-table>
      </div>
      <div class="right">
        <vxe-toolbar>
          <template #buttons>
            <Button @click="showProductForm()" color="primary">新 增</Button>
            <Button @click="showProductCategoryForm()">新增分类</Button>
          </template>
          <template #tools>
            <Search v-model.trim="params.filter" search-button-theme="h-btn-default"
                    show-search-button class="w-300px"
                    placeholder="请输入产品名称" @search="searchProduct">查询
            </Search>
          </template>
        </vxe-toolbar>

        <vxe-table row-id="id"
                   ref="table"
                   :data="productDataList"
                   highlight-hover-row
                   show-overflow
                   :row-config="{height: 48}"
                   :column-config="{resizable: true}"
                   :loading="loading">
          <vxe-column title="操作" align="center" width="100" fixed="left">
            <template #default="{row}">
              <i class="primary-color h-icon-edit ml-10px" @click="showProductForm(row)"></i>
              <i class="primary-color h-icon-trash ml-10px" @click="deleteProduct(row)"></i>
            </template>
          </vxe-column>
          <vxe-column title="编码" field="code" width="80"/>
          <vxe-column title="产品名称" field="name" min-width="200"/>
          <vxe-column title="分类" field="productCategoryName" width="100"/>
          <vxe-column title="规格" field="specification" width="100"/>
          <vxe-column title="单位" field="unitName" width="80"/>
          <vxe-column title="参考进价" field="purchasePrice" width="80"/>
          <vxe-column title="当前库存" field="stockQuantity" width="80"/>
          <!--          <vxe-column title="预警库存" field="alertQuantity" width="80"/>-->
          <!--          <vxe-column title="创建时间" field="createdAt" width="120"/>-->
          <!--          <vxe-column title="更新时间" field="updatedAt" width="120"/>-->
          <vxe-column title="备注" field="remarks"/>
          <vxe-column title="排序号" field="sort" width="80"/>
          <vxe-column title="状态" field="enabled" width="100" align="center" fixed="right">
            <template #default="{row}">
              <Tag color="primary" @click="trigger(row)" v-if="row.enabled">启用</Tag>
              <Tag color="red" @click="trigger(row)" v-else>禁用</Tag>
            </template>
          </vxe-column>

        </vxe-table>
        <vxe-pager perfect @page-change="loadProduct(false)"
                   v-model:current-page="pagination.page"
                   v-model:page-size="pagination.pageSize"
                   :total="pagination.total"
                   :layouts="[ 'PrevPage', 'Number', 'NextPage', 'Sizes', 'Total']">
          <template #left>
            <vxe-button @click="loadProduct(false)" type="text" size="mini" icon="h-icon-refresh"
                        :loading="loading"></vxe-button>
          </template>
        </vxe-pager>
      </div>
    </div>
  </div>
</template>

<script>

import ProductForm from "./ProductForm.vue";
import Product from "@js/api/basic/Product";
import {confirm, message} from "heyui.ext";
import {layer} from "@layui/layer-vue";
import {h} from "vue";
import ProductCategoryForm from "@views/basic/ProductCategoryForm.vue";
import ProductCategory from "@js/api/basic/ProductCategory";

/**
 * @功能描述: 产品管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "ProductList",
  components: {ProductForm},
  data() {
    return {
      loading: false,
      params: {
        name: null,
        productCategoryId: null
      },
      productDataList: [],
      productCategoryDataList: [],
      pagination: {
        page: 1,
        size: 20,
        total: 0
      },
    }
  },
  computed: {

    //查询产品参数
    queryParams() {
      return Object.assign(this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
      })
    }
  },
  methods: {
    // 默认选中第一个单据类型
    selectDefaultProductCategory() {
      const table = this.$refs.productCategoryGridRef;
      if (this.productCategoryDataList[0]) {
        table.setRadioRow(this.productCategoryDataList[0]);
      }
    },

    // 单选框变化时的处理函数
    onProductCategoryChange(data) {
      this.params.productCategoryId = data.row.id;
      this.loadProduct();
    },

    //启用/禁用产品
    trigger(row) {
      let enabled = !row.enabled;
      confirm({
        title: "系统提示",
        content: `确认要「${enabled ? "启用" : "禁用"}」名称：${row.name}?`,
        onConfirm: () => {
          Product.save({id: row.id, enabled}).then(() => {
            message("操作成功~");
            this.loadList();
          })
        }
      })
    },

    //添加或编辑产品分类Form
    showProductCategoryForm(productCategory, parent) {
      let layerId = layer.open({
        title: "产品分类",
        shadeClose: false,
        closeBtn: false,
        area: ['400px', '400px'],
        content: h(ProductCategoryForm, {
          productCategory, parent,
          onClose: () => {
            layer.close(layerId);
          },
          onSuccess: () => {
            this.loadProductCategory();
            layer.close(layerId);
          }
        })
      });
    },

    //删除产品分类
    deleteProductCategory(row) {
      confirm({
        title: "系统提示",
        content: `确认删除产品分类：${row.name}?`,
        onConfirm: () => {
          ProductCategory.remove(row.id).then(() => {
            message("删除成功~");
            this.loadProductCategory();
          })
        }
      })
    },

    //查询产品分类
    loadProductCategory() {
      Promise.all([
        ProductCategory.select(),
      ]).then((results) => {
        let data = results[0].data || [];
        data.unshift({id: null, code: 'ALL', parentId: null, name: '全部分类'})
        this.productCategoryDataList = data;
        this.selectDefaultProductCategory()
      });
    },

    //查询产品按钮
    searchProduct() {
      this.pagination.page = 1;
      this.loadProduct();
    },

    //添加或编辑产品Form
    showProductForm(entity) {
      let layerId = layer.open({
        title: "产品信息",
        shadeClose: false,
        closeBtn: false,
        area: ['1000px', '680px'],
        content: h(ProductForm, {
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
    },

    //删除产品
    deleteProduct(row) {
      confirm({
        title: "系统提示",
        content: `确认删除产品：${row.name}?`,
        onConfirm: () => {
          Product.remove(row.id).then(() => {
            message("删除成功~");
            this.loadProduct();
          })
        }
      })
    },

    //加载产品列表
    loadProduct() {
      this.loading = true;
      Product.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
  },
  created() {
    //初始化产品分类列表
    this.loadProductCategory();
    //初始化产品列表
    this.loadProduct();
  }
}
</script>
<style lang="less">
.parent_container {
  display: flex;
  height: 100%;
}

.left {
  width: 300px; /* 固定宽度 */
  padding: 20px;
  //background-color: #f8e1e1;
}

.right {
  flex: 1; /* 占用剩余空间 */
  padding: 20px;
  //background-color: #b8b7b7;
}

.selected {
  background-color: #dddddd;
}
</style>