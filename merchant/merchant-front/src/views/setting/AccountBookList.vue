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
        <template #costAccounting="{ row }">
          {{ costMethodText(row.costAccounting) }}
        </template>
        <template #availableInventory="{ row }">
          {{ allowNegativeText(row.availableInventory) }}
        </template>
        <template #quantityDecimal="{ row }">
          {{ decimalText(row.quantityDecimal) }}
        </template>
        <template #priceDecimal="{ row }">
          {{ decimalText(row.priceDecimal) }}
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
  components: {AccountBookForm},
  data() {
    return {
      loading: false,
      params: {
        areaId: null,
        name: null,
        merchantId: null,
      },
      dataList: [],
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      columns: [
        {colKey: 'name', title: '名称', minWidth: 150, ellipsis: true},
        {colKey: 'costAccounting', title: '成本核算方法', width: 115, align: 'center'},
        {colKey: 'availableInventory', title: '可用库存允许为负', width: 135, align: 'center'},
        {colKey: 'quantityDecimal', title: '数量小数位', width: 95, align: 'center'},
        {colKey: 'priceDecimal', title: '单价小数位', width: 95, align: 'center'},
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
    // 未设置过参数时按账套参数默认值展示（移动平均法/不允许/2位）
    costMethodText(value) {
      return value === 2 ? '先进先出法' : '移动平均法';
    },
    allowNegativeText(value) {
      return value === 1 ? '是' : '否';
    },
    decimalText(value) {
      return value == null ? 2 : value;
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
        width: '520px',
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
        header: "系统提示",
        body: `确认删除账套：${row.name}?`,
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

