<template>
  <div class="frame-page" style="margin: 0">
    <div class="h-panel">
      <div class="h-panel-body">
        <div class="table-toolbar">
          <div class="table-toolbar-left">
            <label for="name">名称</label>
            <Input id="name" v-model="params.name" class="flex-1" placeholder="请输入名称"/>
            <Button color="primary" icon="fa fa-search" :loading="loading" @click="doSearch">查询</Button>
          </div>
          <div class="table-toolbar-right">
            <Button @click="showForm()" color="primary">添加</Button>
          </div>
        </div>
      </div>
    </div>
    <div>
      <vxe-table row-id="id"
                 ref="table"
                 height="auto"
                 :data="dataList"
                 highlight-hover-row
                 show-overflow
                 :loading="loading">s
        <vxe-column title="名称" field="name" />
        <vxe-column title="启用时间" field="startDate" width="120"/>
        <vxe-column title="状态" field="enabled" width="80" align="center">
          <template #default="{row}">
            <Tag color="primary" v-if="row.enabled" @click="trigger(row)" class="cursor-pointer">启用</Tag>
            <Tag color="red" v-else @click="trigger(row)" class="cursor-pointer">禁用</Tag>
          </template>
        </vxe-column>
        <vxe-column title="操作" align="center" width="160" fixed="right">
          <template #default="{row}">
            <div class="flex items-center justify-center">
              <span class=" primary-color text-hover ml-10px" @click="showForm(row)" size="s">编辑</span>
              <template v-if="!row.systemDefault">
                <span class="primary-color ml-10px text-hover" @click="doRemove(row)" size="s">删除</span>
              </template>
            </div>
          </template>
        </vxe-column>
      </vxe-table>
    </div>
    <Pagination align="right" class="vx-pagination" v-model="pagination" @change="pageChange" small/>
  </div>
</template>

<script>
import AccountBook from "@js/api/AccountBook";
import {confirm, message} from "heyui.ext";
import AccountBookForm from "@components/merchant/accountBook/AccountBookForm.vue";
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
        name: null,
        merchantId: null,
      },
      checkedRows: [],
      dataList: [],
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
      let merchantId = this.merchant.id;
      let layerId = layer.open({
        title: "门店信息",
        shadeClose: false,
        closeBtn: false,
        area: ['680px', 'auto'],
        content: h(AccountBookForm, {
          accountBook, merchantId,
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
    tableCheck() {
      this.checkedRows = this.$refs.table.getCheckboxRecords();
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    doRemove(row) {
      confirm({
        title: "系统提示",
        content: `确认删除：${row.name}?`,
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
            this.doSearch();
          })
        }
      })
    }
  },
  created() {
    this.queryParams.merchantId = this.merchant.id;
    this.doSearch();
  }
}
</script>
