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
                  placeholder="请输入商品分类名称"
                  @search="doSearch">
                <i class="h-icon-search"/>
              </Search>
            </div>
          </div>
          <div class="table-toolbar-right">
            <!--            <Button @click="download()" color="green">导 出</Button>-->
            <Button @click="showForm()" color="primary">新 增</Button>
            <!--            <Button @click="showImport()" color="green">导 入</Button>-->
          </div>
        </div>
        <vxe-table row-id="id"
                   :stripe="false"
                   :tree-config="{transform:true, rowField: 'id', parentField: 'pid'}"
                   ref="table"
                   :data="dataList"
                   highlight-hover-row
                   show-overflow
                   :row-config="{height: 48}"
                   :loading="loading">
          <vxe-column type="seq" width="80" title="#"/>
          <vxe-column title="编码" field="code" align="left" width="100"/>
          <vxe-column title="排序" field="sort" align="left" width="100"/>
          <vxe-column title="图片" width="400" field="imgPath">
            <template #default="{row}">
              <div class="flex">
                <div class="flex">
                  <img v-if="row.imgPath" :src="row.imgPath" style="width: 40px;height: 40px;">
                  <img v-else src="@/assets/good-img-bg.png" style="height: 40px;width: 40px"/>
                </div>
              </div>
            </template>
          </vxe-column>
          <vxe-column title="名称" field="name" tree-node align="left"/>
          <vxe-column title="操作" align="center" width="200" fixed="right">
            <template #default="{row}">
              <div class="flex items-center justify-center">
                <span class=" primary-color text-hover ml-10px" @click="showForm(null,row)" size="s">创建下级</span>
                <i class="primary-color h-icon-edit ml-10px" @click="showForm(row)"></i>
                <i class="primary-color h-icon-trash ml-10px" @click="doRemove(row)"></i>
              </div>
            </template>
          </vxe-column>
        </vxe-table>
      </div>
    </div>
  </div>
</template>

<script>
import {confirm, message} from "heyui.ext";
import {layer} from "@layui/layer-vue";
import {h} from "vue";
import ProductCategory from "@js/api/basic/ProductCategory";
import ProductCategoryForm from "@views/basic/ProductCategoryForm.vue";

export default {
  name: "ProductPrice",
  props: {
    merchant: Object,
  },
  data() {
    return {
      opened: true,
      loading: false,
      params: {
        name: null,
      },
      checkedRows: [],
      dataList: [],
      merchantList: [],
    }
  },
  computed: {
    queryParams() {
      return Object.assign(this.params, {})
    }
  },
  methods: {
    download() {

    },
    showForm(productCategory, parent) {
      let layerId = layer.open({
        title: "分类信息",
        shadeClose: false,
        area: ['450px', '420px'],
        content: h(ProductCategoryForm, {
          productCategory, parent, list: this.dataList,
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
    loadList() {
      this.loading = true;
      ProductCategory.list(this.queryParams).then(({data}) => {
        this.dataList = data;
      }).finally(() => this.loading = false);
    },
    doSearch() {
      this.loadList();
    },
    doRemove(row) {
      confirm({
        title: "系统提示",
        content: `确认删除：${row.name}?`,
        onConfirm: () => {
          ProductCategory.remove(row.id).then(() => {
            message("删除成功~");
            this.doSearch();
          })
        }
      })
    }
  },
  created() {
    this.doSearch();
  }
}
</script>
