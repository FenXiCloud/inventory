<template>
    <div class="frame-page flex flex-column">
      <vxe-toolbar>
        <template #buttons>
          <Search v-model.trim="params.name" search-button-theme="h-btn-default"
                  show-search-button class="w-360px"
                  placeholder="请输入名称" @search="doSearch">
            <i class="h-icon-search"/>
          </Search>
        </template>
        <template #tools>
          <Button @click="showForm()" color="primary">新 增</Button>
        </template>
      </vxe-toolbar>
      <div class="flex1">
        <vxe-table row-id="id"
                   ref="table"
                   :data="dataList"
                   highlight-hover-row
                   show-overflow
                   :loading="loading">
          <vxe-column type="seq" width="60" align="center"/>
          <vxe-column title="名称" field="name" min-width="150"/>
          <vxe-column title="启用日期" field="startDate" width="120" formatter="formatMonth"/>
          <vxe-column title="状态" field="enabled" width="80" align="center" >
            <template #default="{row}">
              <Tag color="primary" v-if="row.enabled" @click="trigger(row)" class="cursor-pointer">启用</Tag>
              <Tag color="red" v-else @click="trigger(row)" class="cursor-pointer">禁用</Tag>
            </template>
          </vxe-column>
          <vxe-column title="操作" align="center" width="120" fixed="right">
            <template #default="{row}">
              <div class="flex items-center justify-center">
                <span class=" primary-color text-hover ml-10px" @click="showForm(row)" size="s">编辑</span>
              </div>
            </template>
          </vxe-column>
        </vxe-table>
        <div class="flex justify-between items-center pt-5px">
          <div></div>
          <vxe-pager perfect @page-change="loadList()"
                     v-model:current-page="pagination.page"
                     v-model:page-size="pagination.pageSize"
                     :total="pagination.total"
                     :layouts="['PrevJump', 'PrevPage', 'Number', 'NextPage', 'NextJump', 'Sizes', 'FullJump', 'Total']">
            <template #left>
              <vxe-button @click="loadList(false)" type="text" size="mini" icon="fa fa-refresh"
                          :loading="loading"></vxe-button>
            </template>
          </vxe-pager>
        </div>
    </div>
  </div>
</template>

<script>
import AccountBook from "@js/api/setting/AccountBook";
import {confirm, message} from "heyui.ext";
import AccountBookForm from "./AccountBookForm.vue";
import {layer} from "@layui/layer-vue";
import {h} from "vue";


export default {
  name: "AccountBookList",
  props: {
    merchant: Object,
  },
  components: {AccountBookForm},
  data() {
    return {
      opened: true,
      loading: false,
      params: {
        areaId: null,
        name: null,
        merchantId: null,
      },
      checkedRows: [],
      dataList: [],
      areaList: [],
      merchantList: [],
      pagination: {
        page: 1,
        size: 20,
        total: 0
      },
      param: [
        {title: '启用', key: 'enabled'},
        {title: '禁用', key: 'disabled'},
      ]

    }
  },
  computed: {
    queryParams() {
      return Object.assign(this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.size
      })
    }
  },
  methods: {
    showForm(accountBook) {
      let layerId = layer.open({
        title: "组织信息",
        shadeClose: false,
        area: ['50vw', 'auto'],
        content: h(AccountBookForm, {
          accountBook,
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
      AccountBook.list(this.queryParams).then(({data}) => {
        this.dataList = data.results;
        this.pagination.total = data.total;
      }).finally(() => this.loading = false);
    },
    pageChange() {
      this.loadList();
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    doRemove(row) {
      confirm({
        title: "系统提示",
        content: `确认删除账套：${row.name}?`,
        onConfirm: () => {
          AccountBook.remove(row.id).then(() => {
            message("删除成功~");
            this.doSearch();
          })
        }
      })
    },
    trigger(row) {
      let enabled = !row.enabled;
      confirm({
        title: "系统提示",
        content: `确认要「${enabled ? "启用" : "禁用"}」账套：${row.name}?`,
        onConfirm: () => {
          AccountBook.save({id: row.id, enabled}).then(() => {
            message("操作成功~");
            this.loadList();
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
