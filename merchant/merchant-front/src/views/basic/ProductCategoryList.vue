<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="showForm()">新 增</t-button>
        <t-input
            v-model="params.name"
            clearable
            placeholder="请输入产品分类名称"
            style="width: 240px; border-radius: 4px"
            @enter="doSearch"
        >
          <template #suffixIcon>
            <t-icon name="search" style="cursor:pointer" @click="doSearch"/>
          </template>
        </t-input>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="doSearch">查询</t-button>
      </t-space>
    </div>

    <div class="simple-page__table">
      <t-table
          row-key="id"
          size="medium"
          bordered
          stripe
          hover
          height="100%"
          table-layout="fixed"
          :data="dataList"
          :columns="columns"
          :loading="loading"
          :tree="treeConfig"
      >
        <template #ops="{ row }">
          <t-space size="small">
            <t-link theme="primary" @click="showForm(null, row)">下级</t-link>
            <t-link theme="primary" @click="showForm(row)"><t-icon name="edit"/></t-link>
            <t-link theme="primary" @click="doRemove(row)"><t-icon name="delete"/></t-link>
          </t-space>
        </template>
        <template #imgPath="{ row }">
          <img
              :src="row.imgPath || defaultImg"
              alt=""
              style="width: 40px; height: 40px; object-fit: cover; border-radius: 2px"
          >
        </template>
      </t-table>
    </div>
  </div>
</template>

<script>
import {MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import {openDialog, closeDialog} from '@common/dialog';
import {h} from 'vue';
import ProductCategory from '@js/api/basic/ProductCategory';
import ProductCategoryForm from '@views/basic/ProductCategoryForm.vue';
import {toArrayTree} from 'xe-utils';
import defaultImg from '@/assets/good-img-bg.png';

export default {
  name: 'ProductCategoryList',
  data() {
    return {
      loading: false,
      defaultImg,
      params: {name: ''},
      dataList: [],
      treeConfig: {
        childrenKey: 'children',
        treeNodeColumnIndex: 2,
        defaultExpandAll: true,
        indent: 24
      },
      columns: [
        {colKey: 'ops', title: '操作', width: 140, fixed: 'left', align: 'center'},
        {colKey: 'code', title: '编码', width: 100},
        {colKey: 'name', title: '名称', minWidth: 180, ellipsis: true},
        {colKey: 'sort', title: '排序', width: 80},
        {colKey: 'imgPath', title: '图片', width: 80, align: 'center'}
      ]
    };
  },
  methods: {
    showForm(productCategory, parent) {
      const dialogId = openDialog({
        header: '分类信息',
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '400px',
        body: h(ProductCategoryForm, {
          productCategory: productCategory || null,
          parent: parent || null,
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.doSearch();
            closeDialog(dialogId);
          }
        })
      });
    },
    loadList() {
      this.loading = true;
      const query = {};
      if (this.params.name) query.name = this.params.name;
      ProductCategory.list(query)
        .then(({data}) => {
          const list = Array.isArray(data) ? data : [];
          this.dataList = toArrayTree(list, {key: 'id', parentKey: 'pid', children: 'children'});
        })
        .finally(() => (this.loading = false));
    },
    doSearch() {
      this.loadList();
    },
    doRemove(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除：${row.name}?`,
        onConfirm: () => {
          ProductCategory.remove(row.id).then(() => {
            MessagePlugin.success('删除成功~');
            this.doSearch();
          });
        }
      });
    }
  },
  created() {
    this.doSearch();
  }
};
</script>

<style scoped>
.simple-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
  padding: 0 12px;
  box-sizing: border-box;
  overflow: hidden;
}

.simple-page__toolbar {
  flex-shrink: 0;
  padding: 8px 0;
}

.simple-page__table {
  flex: 1 1 0;
  height: 0;
  min-height: 0;
  overflow: hidden;
}
</style>
