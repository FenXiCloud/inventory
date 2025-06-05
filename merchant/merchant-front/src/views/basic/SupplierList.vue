<template>
  <div class="frame-page flex flex-column">
    <div class="parent_container">
      <div class="left">
        <vxe-table
            ref="supplierCategoryGridRef"
            size="mini"
            :data="supplierCategoryDataList"
            highlight-hover-row
            show-overflow
            @radio-change="onSupplierCategoryChange"
            :rowConfig="{isCurrent: true,isHover: true}"
            :radio-config="{trigger: 'row',labelField: 'name',highlight: true}">
          <vxe-column field="name" title="供货商分类"></vxe-column>
          <vxe-column title="" align="center" width="120">
            <template #default="{row}">
              <template v-if="row.id !=null">
                <i class="primary-color h-icon-edit ml-10px" @click="showSupplierCategoryForm(row)"></i>
                <i class="primary-color h-icon-trash ml-10px" @click="deleteSupplierCategory(row)"></i>
              </template>
            </template>
          </vxe-column>
        </vxe-table>
      </div>
      <div class="right">
        <vxe-toolbar>
          <template #buttons>
            <Button class="ml-10px" @click="showSupplierForm()" color="primary">新 增</Button>
            <Button @click="showSupplierCategoryForm()">新增分类</Button>
          </template>
          <template #tools>
            <Search v-model.trim="params.filter" search-button-theme="h-btn-default"
                    show-search-button class="w-300px"
                    placeholder="请输入供货商名称" @search="searchSupplier">查询
            </Search>
          </template>
        </vxe-toolbar>

          <vxe-table row-id="id"
                     ref="table"
                     :data="supplierDataList"
                     highlight-hover-row
                     show-overflow
                     :row-config="{height: 48}"
                     :column-config="{resizable: true}"
                     :loading="loading">
            <vxe-column type="seq" width="40" title="#"/>
            <vxe-column title="编码" field="code" width="120"/>
            <vxe-column title="供货商名称" field="name" min-width="200"/>
            <vxe-column title="余额" field="balance" min-width="120"/>
            <vxe-column title="联系人" field="contact" width="120"/>
            <vxe-column title="电话" field="phone" width="120"/>
            <vxe-column title="分类" field="categoryName" width="120"/>
            <vxe-column title="操作" align="center" width="160">
              <template #default="{row}">
                <i class="primary-color h-icon-edit ml-10px" @click="showSupplierForm(row)"></i>
                <i class="primary-color h-icon-trash ml-10px" @click="deleteSupplier(row)"></i>
              </template>
            </vxe-column>
          </vxe-table>
        <vxe-pager perfect @page-change="loadSupplier(false)"
                     v-model:current-page="pagination.page"
                     v-model:page-size="pagination.pageSize"
                     :total="pagination.total"
                     :layouts="[ 'PrevPage', 'Number', 'NextPage', 'Sizes', 'Total']">
            <template #left>
              <vxe-button @click="loadSupplier(false)" type="text" size="mini" icon="h-icon-refresh"
                          :loading="loading"></vxe-button>
            </template>
          </vxe-pager>
      </div>
    </div>
  </div>
</template>

<script>

import SupplierForm from "./SupplierForm.vue";
import Supplier from "@js/api/basic/Supplier";
import {confirm, message} from "heyui.ext";
import {layer} from "@layui/layer-vue";
import {h} from "vue";
import SupplierCategoryForm from "@views/basic/SupplierCategoryForm.vue";
import SupplierCategory from "@js/api/basic/SupplierCategory";

/**
 * @功能描述: 供货商管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "SupplierList",
  components: {SupplierForm},
  data() {
    return {
      loading: false,
      params: {
        name: null,
        supplierCategoryId: null
      },
      supplierCategoryDataList: [],
      supplierDataList: [],
      pagination: {
        page: 1,
        size: 20,
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

    // 默认选中第一个单据类型
    selectDefaultSupplierCategory() {
      const table = this.$refs.supplierCategoryGridRef;
      if (this.supplierCategoryDataList[0]) {
        table.setRadioRow(this.supplierCategoryDataList[0]);
      }
    },

    // 单选框变化时的处理函数
    onSupplierCategoryChange(data) {
      this.params.supplierCategoryId = data.row.id;
      this.loadSupplier();
    },

    //添加或编辑供货商分类Form
    showSupplierCategoryForm(entity) {
      let layerId = layer.open({
        title: "供货商分类",
        shadeClose: false,
        closeBtn: false,
        area: ['400px', '230px'],
        content: h(SupplierCategoryForm, {
          entity,
          onClose: () => {
            layer.close(layerId);
          },
          onSuccess: () => {
            this.loadSupplierCategory();
            layer.close(layerId);
          }
        })
      });
    },

    //删除供货商分类
    deleteSupplierCategory(row) {
      confirm({
        title: "系统提示",
        content: `确认删除供货商：${row.name}?`,
        onConfirm: () => {
          SupplierCategory.delete(row.id).then(() => {
            message("删除成功~");
            this.loadSupplierCategory();
          })
        }
      })
    },

    //查询供货商分类
    loadSupplierCategory() {
      Promise.all([
        SupplierCategory.select(),
      ]).then((results) => {
        let data = results[0].data || [];
        data.unshift({id: null, code: 'ALL', parentId: null, name: '全部分类'})
        this.supplierCategoryDataList = data;
        this.selectDefaultSupplierCategory();
      });
    },

    //查询供货商按钮
    searchSupplier() {
      this.pagination.page = 1;
      this.loadSupplier();
    },

    //添加或编辑供货商Form
    showSupplierForm(entity) {
      let layerId = layer.open({
        title: "供货商信息",
        shadeClose: false,
        closeBtn: false,
        area: ['700px', '500px'],
        content: h(SupplierForm, {
          entity,
          onClose: () => {
            layer.close(layerId);
          },
          onSuccess: () => {
            this.searchSupplier();
            layer.close(layerId);
          }
        })
      });
    },

    //删除供货商
    deleteSupplier(row) {
      confirm({
        title: "系统提示",
        content: `确认删除供货商：${row.name}?`,
        onConfirm: () => {
          Supplier.remove(row.id).then(() => {
            message("删除成功~");
            this.loadSupplier();
          })
        }
      })
    },

    //加载供货商列表
    loadSupplier() {
      this.loading = true;
      Supplier.list(this.queryParams).then(({data: {results, total}}) => {
        this.supplierDataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
  },
  created() {
    //初始化供货商分类列表
    this.loadSupplierCategory();
    //初始化供货商列表
    this.loadSupplier();
  }
}
</script>
<style lang="less" scoped>
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