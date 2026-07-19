<template>
  <div class="frame-page" style="margin: 0">
    <div class="t-panel p-16px">
      <div class="toolbar">
        <div class="toolbar-left">
          <label for="name">名称</label>
          <t-input id="name" v-model="params.name" class="flex-1" placeholder="请输入名称"/>
          <t-button theme="primary" :loading="loading" @click="doSearch">查询</t-button>
        </div>
        <div class="toolbar-right">
          <t-button @click="showForm()" theme="primary">添加</t-button>
        </div>
      </div>
      <vxe-table row-id="id"
                 ref="table"
                 height="auto"
                 :data="dataList"
                 highlight-hover-row
                 show-overflow
                 :loading="loading">
        <vxe-column type="seq" width="60" title="序列"/>
        <vxe-column title="名称" field="name" />
        <vxe-column title="启用时间" field="startDate" width="120"/>
        <vxe-column title="状态" field="enabled" width="80" align="center">
          <template #default="{row}">
            <t-tag theme="primary" v-if="row.enabled" @click="trigger(row)" class="cursor-pointer">启用</t-tag>
            <t-tag theme="danger" v-else @click="trigger(row)" class="cursor-pointer">禁用</t-tag>
          </template>
        </vxe-column>
        <vxe-column title="操作" align="center" width="160" fixed="right">
          <template #default="{row}">
            <div class="flex items-center justify-center">
              <span class=" primary-color text-hover ml-10px" @click="showForm(row)">编辑</span>
              <template v-if="!row.systemDefault">
                <span class="primary-color ml-10px text-hover" @click="doRemove(row)">删除</span>
              </template>
            </div>
          </template>
        </vxe-column>
      </vxe-table>
      <t-pagination class="mt-16px" v-model:current="pagination.page" :total="pagination.total" :page-size="pagination.size" @change="pageChange" size="small"/>
    </div>
  </div>
</template>

<script>
import AccountBook from "@js/api/AccountBook";
import {DialogPlugin, MessagePlugin} from "tdesign-vue-next";
import AccountBookForm from "@/views/merchant/accountBook/AccountBookForm.vue";
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
      const dialog = DialogPlugin({
        header: "账套信息",
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '680px',
        body: h(AccountBookForm, {
          accountBook, merchantId,
          onClose: () => dialog.hide(),
          onSuccess: () => {
            this.doSearch();
            dialog.hide();
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
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认删除：${row.name}?`,
        onConfirm: () => {
          AccountBook.remove(row.id).then(() => {
            MessagePlugin.success("删除成功~");
            this.doSearch();
          })
        }
      })
    },
    trigger(row) {
      let enabled = !row.enabled;
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认要「${enabled ? "启用" : "禁用"}」账套：${row.name}?`,
        onConfirm: () => {
          AccountBook.save({id: row.id, enabled}).then(() => {
            MessagePlugin.success("操作成功~");
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
