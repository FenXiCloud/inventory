<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-input
            v-model="params.filter"
            clearable
            placeholder="请输入品牌名称"
            style="width: 220px; background: #fff; border-radius: 4px"
            @enter="doSearch"
        >
          <template #suffixIcon>
            <t-icon name="search" style="cursor:pointer" @click="doSearch"/>
          </template>
        </t-input>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="doSearch">查询</t-button>
      </t-space>
    </div>

    <div class="simple-page__hint">
      品牌为商品档案上的自由文本字段。此处用于统一维护：可重命名品牌（同步更新所有引用商品）或删除品牌（清空对应商品的品牌）。
    </div>

    <div class="simple-page__table">
      <t-table
          row-key="brand"
          size="medium"
          bordered
          stripe
          hover
          height="100%"
          table-layout="fixed"
          :data="dataList"
          :columns="columns"
          :loading="loading"
      >
        <template #ops="{ row }">
          <t-space size="small">
            <t-link theme="primary" @click="showRename(row)">重命名</t-link>
            <t-link theme="danger" @click="doDelete(row)">删除</t-link>
          </t-space>
        </template>
      </t-table>
    </div>

    <div class="simple-page__pager">
      <span class="simple-page__total">共 {{ dataList.length }} 个品牌</span>
    </div>
  </div>
</template>

<script>
import Product from "@js/api/basic/Product";
import {MessagePlugin, DialogPlugin} from "tdesign-vue-next";

export default {
  name: "BrandList",
  data() {
    return {
      loading: false,
      params: {
        filter: null
      },
      dataList: [],
      columns: [
        {colKey: 'ops', title: '操作', width: 160, fixed: 'left', align: 'center'},
        {colKey: 'brand', title: '品牌名称', minWidth: 200, ellipsis: true},
        {colKey: 'count', title: '商品数量', width: 120, align: 'center'},
      ]
    };
  },
  computed: {
    filteredList() {
      const kw = (this.params.filter || '').trim();
      if (!kw) return this.dataList;
      return this.dataList.filter(item => (item.brand || '').includes(kw));
    }
  },
  methods: {
    doSearch() {
      this.loadList();
    },
    loadList() {
      this.loading = true;
      Product.brands().then(({data}) => {
        this.dataList = data || [];
      }).finally(() => this.loading = false);
    },
    showRename(row) {
      let newName = row.brand;
      const dialog = DialogPlugin({
        header: '重命名品牌',
        body: h => h('div', {style: 'padding: 8px 0;'}, [
          h('div', {style: 'margin-bottom: 8px; color: #666;'}, `原品牌：${row.brand}`),
          h('input', {
            value: newName,
            style: 'width: 100%; height: 32px; border: 1px solid #dcdcdc; border-radius: 3px; padding: 0 8px;',
            onInput: (e) => { newName = e.target.value; }
          })
        ]),
        confirmBtn: '确定',
        cancelBtn: '取消',
        onConfirm: () => {
          if (!newName || !newName.trim()) {
            MessagePlugin.warning('品牌名称不能为空');
            return;
          }
          if (newName.trim() === row.brand) {
            return;
          }
          Product.renameBrand({oldBrand: row.brand, newBrand: newName.trim()}).then(() => {
            MessagePlugin.success('重命名成功~');
            this.loadList();
          });
        }
      });
    },
    doDelete(row) {
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认删除品牌「${row.brand}」？将清空 ${row.count} 个商品的品牌字段。`,
        onConfirm: () => {
          return Product.deleteBrand(row.brand).then(() => {
            MessagePlugin.success("删除成功~");
            this.loadList();
          });
        }
      });
    }
  },
  created() {
    this.loadList();
  }
};
</script>

<style scoped>
.simple-page__hint {
  flex-shrink: 0;
  margin-bottom: 8px;
  padding: 8px 12px;
  border-radius: 4px;
  background: #f3f3f3;
  color: #555;
  font-size: 13px;
  line-height: 1.6;
}
</style>
