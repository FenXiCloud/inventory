<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <Select v-model="params.enabled" class="w-200px" dict="statusRadios" placeholder="全部商品"/>
        <Search
            v-model="params.filter"
            search-button-theme="h-btn-default"
            show-search-button
            class="w-360px pl-8px"
            placeholder="请输入商品名称/编码"
            @search="doSearch">
          <i class="h-icon-search"/>
        </Search>
      </template>
      <template #tools>
        <Button @click="showForm()" color="primary">新 增</Button>
        <Button @click="showForm()">新增分类</Button>
      </template>
    </vxe-toolbar>
    <div class="flex flex-1">
      <div class="flex border w-200px" style="height:calc(100vh - 226px); overflow-y:auto;min-width: 200px">
        <Tree :option="categoryOption" ref="categoryOption" class="w-200px" @select="categorySelect" filterable
              selectOnClick
              className="h-tree-theme-row-selected">
        </Tree>
      </div>
      <div class="flex-1 w-0px ml-6px">
        <vxe-table row-id="id"
                   ref="table"
                   height="auto"
                   :data="dataList"
                   :checkbox-config="{ reserve: true }"
                   highlight-hover-row
                   @checkbox-change="checkboxChange"
                   @checkbox-all="selectAllCheckboxChange"
                   :sort-config="{remote:true}"
                   @sort-change="sortChangeEvent"
                   show-overflow
                   :row-config="{height: 50}"
                   :column-config="{resizable: true}"
                   :loading="loading">
<!--          <vxe-column type="checkbox" width="50" align="center"/>-->
          <vxe-column title="商品分类" field="categoryName" width="150"/>
          <vxe-column title="商品" width="300" field="imgPath">
            <template #default="{row}">
              <div class="flex">
                <div class="flex">
                  <img v-if="row.imgPath" :src="row.imgPath" style="width: 40px;height: 40px;">
                  <img v-else src="@/assets/good-img-bg.png" style="height: 40px;width: 40px"/>
                </div>
                <div class="flex1 ml-8px">
                  <div>{{ row.name }}</div>
                  <div>编码 {{ row.code }}</div>
                </div>
              </div>
            </template>
          </vxe-column>
          <vxe-column title="规格" field="specification" min-width="120"/>
          <vxe-column title="单位" field="unitName" width="90"/>
          <vxe-column title="副单位" field="unitName1" width="90">
            <template #default="{row}">
              <template v-if="row.enableMultiUnit && row.multiUnit">
                <span> {{ row.multiUnit[0].unitName }}</span>
                <span> {{ row.multiUnit[1] ? '/' + row.multiUnit[1]?.unitName : '' }}</span>
                <span> {{ row.multiUnit[2] ? '/' + row.multiUnit[2]?.unitName : '' }}</span>
              </template>
            </template>
          </vxe-column>
          <vxe-column title="排序" field="sort" width="100" sortable align="center" >
            <template #edit="{row}">
              <vxe-input placeholder.number="排序" v-model="row.sort" min="0" type="integer"></vxe-input>
            </template>
          </vxe-column>
          <vxe-column title="状态" field="enabled" width="80"  align="center">
            <template #default="{row}">
              <Tag color="primary" v-if="row.enabled" class="cursor-pointer">正常</Tag>
              <Tag color="gray" v-else class="cursor-pointer">禁用</Tag>
            </template>
          </vxe-column>
          <vxe-column title="操作" width="90" align="center" fixed="right">
            <template #default="{row}">
              <i class="primary-color h-icon-edit ml-10px" @click="showForm(row)"></i>
              <i class="primary-color h-icon-trash ml-10px" @click="doRemove(row)"></i>
            </template>
          </vxe-column>
        </vxe-table>
      </div>
    </div>
    <div class="flex justify-between items-center pt-5px">
      <div>
<!--        <template v-if="selectionRows.length">-->
<!--          <span class="mr-9px text-10px primary-color">已选择{{ selectionRows.length }}个商品</span>-->
<!--          <vxe-button @click="batchEnabled(true,'启用')">启用</vxe-button>-->
<!--          <vxe-button @click="batchEnabled(false,'禁用')">禁用</vxe-button>-->
<!--        </template>-->
      </div>
      <vxe-pager background @page-change="loadList"
                 v-model:current-page="pagination.page"
                 v-model:page-size="pagination.pageSize"
                 :total="pagination.total"
                 :layouts="['PrevJump', 'PrevPage', 'Number', 'NextPage', 'NextJump', 'Sizes', 'FullJump', 'Total']">
        <template #left>
          <vxe-button @click="loadList()" type="text" size="mini" icon="h-iconx-refresh"
                      :loading="loading"></vxe-button>
        </template>
      </vxe-pager>
    </div>
  </div>
</template>

<script>
import {confirm, loading, message} from "heyui.ext";
import {layer} from "@layui/layer-vue";
import {h} from "vue";
import Product from "@js/api/Product";
import ProductCategory from "@js/api/ProductCategory";
import ProductForm from "@views/basic/ProductForm.vue";

/**
 * @功能描述: 商品列表
 * @创建时间: 2024年05月06日
 * @公司官网:    www.fenxi365.com
 * @公司信息:    纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "ProductList",
  data() {
    return {
      loading: false,
      dataList: [],
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      category: null,
      params: {
        categoryId: null,
        path: null,
        filter: null,
        enabled: null,
        sortCol: null,
        sort: null,
      },
      categoryOption: {
        keyName: 'id',
        titleName: 'name',
        dataMode: 'list',
        parentName: 'pid',
        datas: []
      },
      selectionRows: [],
    }
  },
  computed: {
    queryParams() {
      return Object.assign(this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        categoryId: this.category ? this.category.id : null,
        path: this.category ? this.category.path : null
      })
    }
  },
  watch: {
    category() {
      this.pagination.page = 1;
      this.loadList();
    }
  },
  methods: {
    sortChangeEvent({field, order}) {
      this.params.sortCol = field;
      this.params.sort = order;
      this.loadList();
    },
    categorySelect(item) {
      this.category = Object.freeze(item);
    },
    showForm(entity) {
      let layerId = layer.drawer({
        title: "商品信息",
        shadeClose: false,
        area: ['80vw', '100vh'],
        content: h(ProductForm, {
          entity,
          onClose: () => {
            layer.close(layerId);
          },
          onSuccess: () => {
            this.loadList();
            layer.close(layerId);
          }
        })
      });
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    loadList() {
      this.loading = true;
      loading("加载中...");
      Product.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results;
        this.pagination.total = total;
      }).finally(() => {
        loading.close();
        this.loading = false
      });
    },
    doRemove(row) {
      confirm({
        title: "系统提示",
        content: `确认删除：${row.name}?`,
        onConfirm: () => {
          Product.remove(row.id).then(() => {
            message("删除成功~");
            this.loadList();
          })
        }
      })
    },
    batchEnabled(enabled, stateName) {
      let ids = this.selectionRows.map(val => val.id);
      confirm({
        title: "系统提示",
        content: `确认批量：${stateName} ${this.selectionRows.length} 个商品?`,
        onConfirm: () => {
          Product.batchEnabled(enabled, ids).then(() => {
            message("批量操作成功~");
            this.selectionRows = [];
            this.$refs.table.clearCheckboxRow();
            this.loadList();
          })
        }
      })
    },
    checkboxChange({checked, records, reserves, row}) {
      if (checked) {
        if (reserves.length === 0) {
          this.selectionRows = records;
        } else {
          this.selectionRows = [...reserves, ...records];
        }
      } else {
        this.selectionRows = this.selectionRows.filter(item => item.id !== row.id)
      }
    },
    selectAllCheckboxChange({checked, records, reserves}) {
      if (checked) {
        if (reserves.length === 0) {
          this.selectionRows = records;
        } else {
          this.selectionRows = [...reserves, ...records];
        }
      } else {
        this.selectionRows = reserves;
      }
    },
    initSelect() {
      Promise.all([
        ProductCategory.select(),
      ]).then((results) => {
        let data = results[0].data || [];
        data.unshift({id: null, code: 'QB', parentId: null, name: '全部分类'})
        this.categoryOption.datas = data;
        this.category = data[0];
      });
    }
  },
  created() {
    this.initSelect();
  }
}
</script>
