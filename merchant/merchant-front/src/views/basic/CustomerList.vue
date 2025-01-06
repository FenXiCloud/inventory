<template><div class="frame-page flex flex-column">
  <vxe-toolbar>
    <template #buttons>
      <Search v-model.trim="params.filter" search-button-theme="h-btn-default"
              show-search-button class="w-260px"
              placeholder="请输入客户名称" @search="doSearch">
        <i class="h-icon-search"/>
      </Search>
    </template>
    <template #tools>
      <Button @click="showForm()" color="primary">新 增</Button>
      <Button @click="addCategoryForm()" >新增分类</Button>
    </template>
  </vxe-toolbar>
  <div class="flex1">
        <vxe-table row-id="id"
                   ref="table"
                   :data="dataList"
                   highlight-hover-row
                   show-overflow
                   :row-config="{height: 48}"
                   :column-config="{resizable: true}"
                   :loading="loading">
          <vxe-column type="seq" width="40" title="#"/>
          <vxe-column title="编码" field="code" width="120"/>
          <vxe-column title="客户名称" field="name" min-width="200"/>
          <vxe-column title="联系人" field="linkman" width="120"/>
          <vxe-column title="电话" field="phone" width="120"/>
          <vxe-column title="分类" field="categoryName" width="120"/>
          <vxe-column title="等级" field="levelName" width="120"/>
          <vxe-column title="备注" field="remark" min-width="120"/>
          <vxe-column title="操作" align="center" width="160">
            <template #default="{row}">
                <i class="primary-color h-icon-edit ml-10px" @click="showForm(row)"></i>
                <i class="primary-color h-icon-trash ml-10px" @click="doRemove(row)"></i>
            </template>
          </vxe-column>
        </vxe-table>
      </div>
  <div class="flex justify-between items-center pt-5px">
    <div>
    </div>
    <vxe-pager perfect @page-change="loadList(false)"
               v-model:current-page="pagination.page"
               v-model:page-size="pagination.pageSize"
               :total="pagination.total"
               :layouts="['PrevJump', 'PrevPage', 'Number', 'NextPage', 'NextJump', 'Sizes', 'FullJump', 'Total']">
      <template #left>
        <!--          <span class="mr-12px text-16px">总金额：{{ amountTotal }}元</span>-->
        <vxe-button @click="loadList(false)" type="text" size="mini" icon="fa fa-refresh"
                    :loading="loading"></vxe-button>
      </template>
    </vxe-pager>
  </div>
  </div>
</template>

<script>
import CustomerForm from "./CustomerForm.vue";
import Customer from "@js/api/basic/Customer";
import {confirm, message} from "heyui.ext";
import {layer} from "@layui/layer-vue";
import {h} from "vue";
import CustomerCategoryForm from "@views/basic/CustomerCategoryForm.vue";

/**
 * @功能描述: 客户管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "CustomerList",
  components: {CustomerForm},
  data() {
    return {
      loading: false,
      params: {
        name: null,
      },
      dataList: [],
      pagination: {
        page: 1,
        size: 20,
        total: 0
      },
    }
  },
  computed: {
    queryParams() {
      return Object.assign(this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,})
    }
  },
  methods: {
    addCategoryForm(entity) {
      let type = 0;
      let layerId = layer.open({
        title: "客户分类",
        shadeClose: false,
        closeBtn: false,
        area: ['400px', '230px'],
        content: h(CustomerCategoryForm, {
          entity, type,
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
    showForm(entity) {
      let layerId = layer.open({
        title: "客户信息",
        shadeClose: false,
        closeBtn: false,
        area: ['700px', '500px'],
        content: h(CustomerForm, {
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
    loadList() {
      this.loading = true;
      Customer.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    doRemove(row) {
      confirm({
        title: "系统提示",
        content: `确认删除客户：${row.name}?`,
        onConfirm: () => {
          Customer.remove(row.id).then(() => {
            message("删除成功~");
            this.loadList();
          })
        }
      })
    }
  },
  created() {
    this.loadList();
  }
}
</script>
