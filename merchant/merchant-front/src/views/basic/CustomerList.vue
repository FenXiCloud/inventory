<template>
  <div class="frame-page flex flex-column">
    <div class="parent_container">
      <div class="left">
        <vxe-table
            ref="customerTypeGridRef"
            size="mini"
            :data="customerTypeDataList"
            highlight-hover-row
            show-overflow
            @radio-change="handleCustomerTypeChange"
            :rowConfig="{isCurrent: true,isHover: true}"
            :radio-config="{trigger: 'row',labelField: 'name',highlight: true}">
          <vxe-column field="name" title="客户类型"></vxe-column>
          <vxe-column title="" align="center" width="120">
            <template #default="{row}">
              <template v-if="row.id !=null">
                <i class="primary-color h-icon-edit ml-10px" @click="addOrEditCategoryForm(row)"></i>
                <i class="primary-color h-icon-trash ml-10px" @click="doRemoveCategory(row)"></i>
              </template>
            </template>
          </vxe-column>
        </vxe-table>
      </div>
      <div class="right">

        <vxe-toolbar>
          <template #buttons>
            <Button @click="addOrEditForm()" color="primary">新 增</Button>
            <Button @click="addOrEditCategoryForm()">新增分类</Button>
          </template>
          <template #tools>
            <Search v-model.trim="params.name" search-button-theme="h-btn-default"
                    show-search-button class="w-300px"
                    placeholder="请输入客户名称" @search="doSearch">查询
            </Search>
          </template>
        </vxe-toolbar>

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
          <vxe-column title="联系人" field="contact" width="120"/>
          <vxe-column title="电话" field="phone" width="120"/>
          <vxe-column title="分类" field="categoryName" width="120"/>
          <vxe-column title="等级" field="levelName" width="120"/>
          <vxe-column title="备注" field="remarks" min-width="120"/>
          <vxe-column title="操作" align="center" width="160">
            <template #default="{row}">
              <i class="primary-color h-icon-edit ml-10px" @click="addOrEditForm(row)"></i>
              <i class="primary-color h-icon-trash ml-10px" @click="doRemove(row)"></i>
            </template>
          </vxe-column>
        </vxe-table>
        <vxe-pager perfect @page-change="loadData(false)"
                   v-model:current-page="pagination.page"
                   v-model:page-size="pagination.pageSize"
                   :total="pagination.total"
                   :layouts="[ 'PrevPage', 'Number', 'NextPage', 'Sizes', 'Total']">
          <template #left>
            <vxe-button @click="loadData(false)" type="text" size="mini" icon="h-icon-refresh"
                        :loading="loading"></vxe-button>
          </template>
        </vxe-pager>
      </div>
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
import CustomerCategory from "@js/api/basic/CustomerCategory";

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
        customerCategoryId: null
      },
      customerTypeDataList: [{id: null, code: 'ALL', parentId: null, name: '全部分类'}],
      dataList: [],
      pagination: {
        page: 1,
        size: 20,
        total: 0
      }
    }
  },
  computed: {

    //查询客户参数
    queryParams() {
      return Object.assign(this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
      })
    }
  },
  methods: {

    selectFirstCustomerType() {
      // 默认选中第一个单据类型
      const table = this.$refs.customerTypeGridRef;

      if (this.customerTypeDataList[0]){
        table.setRadioRow(this.customerTypeDataList[0]);
      }

    },

    handleCustomerTypeChange(data) {
      // 单选框变化时的处理函数
      console.log(data.row.id);
      this.params.customerCategoryId = data.row.id;
      this.loadData();
    },

    //添加或编辑客户分类Form
    addOrEditCategoryForm(entity) {
      let layerId = layer.open({
        title: "客户分类",
        shadeClose: false,
        closeBtn: false,
        area: ['400px', '230px'],
        content: h(CustomerCategoryForm, {
          entity,
          onClose: () => {
            layer.close(layerId);
          },
          onSuccess: () => {
            this.loadCategoryData();
            layer.close(layerId);
          }
        })
      });
    },

    //删除客户分类
    doRemoveCategory(row) {
      confirm({
        title: "系统提示",
        content: `确认删除客户类型：${row.name}?`,
        onConfirm: () => {
          CustomerCategory.remove(row.id).then(() => {
            message("删除成功~");
            this.loadCategoryData();
            this.params.customerCategoryId = null;
            this.loadData();
          })
        }
      })
    },

    //查询客户分类
    loadCategoryData() {
      Promise.all([
        CustomerCategory.select(),
      ]).then((results) => {
        let data = results[0].data || [];
        data.unshift({id: null, code: 'ALL', parentId: null, name: '全部分类'})
        this.customerTypeDataList = data;
        this.selectFirstCustomerType();
      });
    },

    //查询客户按钮
    doSearch() {
      this.pagination.page = 1;
      this.loadData();
    },

    //添加或编辑客户Form
    addOrEditForm(entity) {
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

    //删除客户
    doRemove(row) {
      confirm({
        title: "系统提示",
        content: `确认删除客户：${row.name}?`,
        onConfirm: () => {
          Customer.remove(row.id).then(() => {
            message("删除成功~");
            this.loadData();
          })
        }
      })
    },

    //加载客户列表
    loadData() {
      this.loading = true;
      Customer.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
  },
  created() {
    //初始化客户分类列表
    this.loadCategoryData();

    //初始化客户列表
    this.loadData();
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