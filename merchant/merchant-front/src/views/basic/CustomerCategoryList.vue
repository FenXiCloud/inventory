<template>
  <div class="frame-page" style="margin: 0">
    <div class="h-panel">
      <div class="h-panel-body">
        <div class="table-toolbar">
          <div class="table-toolbar-left">
            <Search
                v-model="params.name"
                search-button-theme="h-btn-default"
                show-search-button
                class="w-360px pl-8px"
                placeholder="请输入名称"
                :loading="loading"
                @search="doSearch">
              <i class="h-icon-search"/>
            </Search>
          </div>
          <div class="table-toolbar-right">
            <Button @click="showForm()" color="primary">新 增</Button>
          </div>
        </div>
        <vxe-table row-id="id"
                   ref="table"
                   :data="dataList"
                   highlight-hover-row
                   show-overflow
                   :row-config="{height: 48}"
                   :loading="loading">
          <vxe-column type="seq" width="60" title="#"/>
          <vxe-column title="名称" field="name"/>
<!--          <vxe-column title="是否默认" field="systemDefault" width="100">-->
<!--            <template #default="{row}">-->
<!--              <Tag color="green" v-if="row.systemDefault">是</Tag>-->
<!--              <Tag color="gray" v-else >否</Tag>-->
<!--            </template>-->
<!--          </vxe-column>-->
          <vxe-column title="操作" align="center" width="200">
            <template #default="{row}">
              <i class="primary-color h-icon-edit ml-10px" @click="showForm(row)"></i>
              <i class="primary-color h-icon-trash ml-10px" v-if="!row.systemDefault"  @click="doRemove(row)"></i>
            </template>
          </vxe-column>
        </vxe-table>
      </div>
    </div>
  </div>
</template>

<script>
import CustomerCategory from "@js/api/basic/CustomerCategory";
import CustomerCategoryForm from "./CustomerCategoryForm.vue";
import {confirm, message} from "heyui.ext";
import {h} from "vue";
import {layer} from "@layui/layer-vue";

/**
 * @功能描述: 客户分类
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "CustomerCategoryList",
  data() {
    return {
      loading: false,
      dataList: [],
      params: {
        name: null,
      },
    }
  },
  methods: {
    showForm(entity) {
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
    doSearch() {
      this.loadList();
    },
    loadList() {
      this.loading = true;
      CustomerCategory.list().then(({data}) => {
        this.dataList = data;
      }).finally(() => this.loading = false);
    },
    doRemove(row) {
      confirm({
        title: "系统提示",
        content: `确认删除客户分类：${row.name}?`,
        onConfirm: () => {
          CustomerCategory.remove(row.id).then(() => {
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
