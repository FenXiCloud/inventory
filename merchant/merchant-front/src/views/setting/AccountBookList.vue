<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="showForm()">新 增</t-button>
        <t-input
            v-model.trim="params.name"
            clearable
            placeholder="请输入名称"
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
          table-layout="auto"
          :data="dataList"
          :columns="columns"
          :loading="loading"
      >
        <template #startDate="{ row }">
          {{ formatMonth(row.startDate) }}
        </template>
        <template #enabled="{ row }">
          <t-tag
              :theme="row.enabled ? 'primary' : 'danger'"
              variant="light"
              style="cursor:pointer"
              @click="trigger(row)"
          >
            {{ row.enabled ? '启用' : '禁用' }}
          </t-tag>
        </template>
        <template #ops="{ row }">
          <t-space size="small">
            <t-link theme="primary" @click="showForm(row)">编辑</t-link>
            <t-link theme="primary" @click="showConfigForm(row)">参数设置</t-link>
          </t-space>
        </template>
      </t-table>
    </div>

    <div class="simple-page__pager">
      <t-pagination
          v-model:current="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :show-jumper="true"
          :show-page-size="true"
          :popup-props="{ attach: 'body' }"
          @change="onPageChange"
      />
    </div>
  </div>
</template>

<script>
import AccountBook from "@js/api/setting/AccountBook";
import {MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';
import AccountBookForm from "./AccountBookForm.vue";
import SystemConfigForm from "./SystemConfigForm.vue";
import {openDialog, closeDialog} from '@common/dialog';
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
        pageSize: 20,
        total: 0
      },
      param: [
        {title: '启用', key: 'enabled'},
        {title: '禁用', key: 'disabled'},
      ],
      columns: [
        {colKey: 'name', title: '名称', minWidth: 150, ellipsis: true},
        {colKey: 'startDate', title: '启用日期', width: 120},
        {colKey: 'enabled', title: '状态', width: 90, align: 'center'},
        {colKey: 'ops', title: '操作', width: 180, fixed: 'right', align: 'center'}
      ]
    }
  },
  computed: {
    queryParams() {
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize
      })
    }
  },
  methods: {
    formatMonth(value) {
      if (!value) return '';
      const str = String(value);
      return str.length >= 7 ? str.substring(0, 7) : str;
    },
    showForm(accountBook) {
      let dialogId = openDialog({
        header: "组织信息",
        closeOnOverlayClick: false,
        width: '50vw',
        body: h(AccountBookForm, {
          accountBook,
          onClose: () => {
            closeDialog(dialogId);
          },
          onSuccess: () => {
            this.doSearch();
            closeDialog(dialogId);
          }
        })
      });
    },
    showConfigForm(accountBook) {
      let dialogId = openDialog({
        header: "参数设置",
        closeOnOverlayClick: false,
        width: '800px',
        body: h(SystemConfigForm, {
          accountBook,
          onClose: () => {
            closeDialog(dialogId);
          },
          onSuccess: () => {
            this.doSearch();
            closeDialog(dialogId);
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
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    doRemove(row) {
      DialogPlugin.confirm({
        title: "系统提示",
        content: `确认删除账套：${row.name}?`,
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
        title: "系统提示",
        content: `确认要「${enabled ? "启用" : "禁用"}」账套：${row.name}?`,
        onConfirm: () => {
          AccountBook.save({id: row.id, enabled}).then(() => {
            MessagePlugin.success("操作成功~");
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

.simple-page__pager {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid var(--td-component-border, #dcdcdc);
}
</style>
