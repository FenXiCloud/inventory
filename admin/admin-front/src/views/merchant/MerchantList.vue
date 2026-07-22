<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="showForm()">新 增</t-button>
        <t-input
            v-model="params.name"
            clearable
            placeholder="请输入商户名称"
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
      >
        <template #ops="{ row }">
          <t-link theme="primary" @click="showForm(row)">编辑</t-link>
        </template>
        <template #name="{ row }">
          <t-link theme="primary" title="用户管理" @click="doConfig(row)">{{ row.name }}</t-link>
        </template>
        <template #enabled="{ row }">
          <t-tag
              :theme="row.enabled ? 'primary' : 'danger'"
              variant="light"
              style="cursor: pointer"
              @click="trigger(row)"
          >
            {{ row.enabled ? '启用' : '禁用' }}
          </t-tag>
        </template>
      </t-table>
    </div>

    <div class="simple-page__pager">
      <t-pagination
          v-model:current="pagination.page"
          v-model:page-size="pagination.size"
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
import Merchant from "@js/api/Merchant";
import {DialogPlugin, MessagePlugin} from "tdesign-vue-next";
import MerchantForm from "@/views/merchant/MerchantForm";
import MerchantSetting from "@/views/merchant/MerchantSetting";
import {h} from "vue";
export default {
  name: "MerchantList",
  components: {MerchantForm, MerchantSetting},
  data() {
    return {
      merchant: null,
      loading: false,
      params: {
        name: null
      },
      dataList: [],
      pagination: {
        page: 1,
        size: 20,
        total: 0
      },
      columns: [
        {colKey: 'ops', title: '操作', width: 80, fixed: 'left', align: 'center'},
        {colKey: 'id', title: 'ID', width: 80},
        {colKey: 'code', title: '商户编码', width: 280, ellipsis: true},
        {colKey: 'name', title: '商户名称', minWidth: 160, ellipsis: true},
        {colKey: 'contact', title: '联系人', width: 100},
        {colKey: 'mobile', title: '联系人电话', width: 130},
        {colKey: 'createdAt', title: '创建时间', width: 160, align: 'center'},
        {colKey: 'enabled', title: '状态', width: 100, align: 'center', fixed: 'right'}
      ]
    }
  },
  computed: {
    queryParams() {
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.size
      })
    }
  },
  methods: {
    showForm(merchant) {
      const dialog = DialogPlugin({
        header: "商户信息",
        closeOnOverlayClick: false,
        closeBtn: false,
        footer: false,
        width: '800px',
        body: h(MerchantForm, {
          merchant,
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
      Merchant.list(this.queryParams).then(({data}) => {
        this.dataList = data.results;
        this.pagination.total = data.total;
      }).finally(() => this.loading = false);
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.size = pageInfo.pageSize;
      this.loadList();
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    doConfig(merchant) {
      DialogPlugin({
        header: `${merchant.name} - 商户配置`,
        placement: 'center',
        closeOnOverlayClick: false,
        footer: false,
        width: '1200px',
        dialogClassName: 'merchant-setting-dialog',
        body: h(MerchantSetting, { merchant, defaultSetting: 'AdminList' })
      });
    },
    doRemove(row) {
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认删除商户：${row.name}?`,
        onConfirm: () => {
          Merchant.remove(row.id).then(() => {
            MessagePlugin.success("删除成功~");
            this.loadList();
          })
        }
      })
    },
    trigger(row) {
      let enabled = !row.enabled;
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认要「${enabled ? "启用" : "禁用"}」商户：${row.name}?`,
        onConfirm: () => {
          Merchant.save({id: row.id, enabled}).then(() => {
            MessagePlugin.success("操作成功~");
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
  background: #fff;
}
</style>
