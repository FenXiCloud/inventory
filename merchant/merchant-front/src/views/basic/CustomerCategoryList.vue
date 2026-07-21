<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="showForm()">新 增</t-button>
        <t-input
            v-model="params.name"
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
          table-layout="fixed"
          :data="filteredList"
          :columns="columns"
          :loading="loading"
      >
        <template #ops="{ row }">
          <t-space size="small">
            <t-link theme="primary" @click="showForm(row)"><t-icon name="edit"/></t-link>
            <t-link v-if="!row.systemDefault" theme="primary" @click="doRemove(row)">
              <t-icon name="delete"/>
            </t-link>
          </t-space>
        </template>
      </t-table>
    </div>
  </div>
</template>

<script>
import CustomerCategory from '@js/api/basic/CustomerCategory';
import CustomerCategoryForm from '@views/basic/CustomerCategoryForm.vue';
import {MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import {h} from 'vue';
import {openDialog, closeDialog} from '@common/dialog';

/**
 * @功能描述: 客户分类
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: 'CustomerCategoryList',
  data() {
    return {
      loading: false,
      dataList: [],
      params: {name: ''},
      columns: [
        {colKey: 'ops', title: '操作', width: 90, fixed: 'left', align: 'center'},
        {colKey: 'name', title: '名称', minWidth: 200, ellipsis: true}
      ]
    };
  },
  computed: {
    filteredList() {
      const name = (this.params.name || '').trim();
      if (!name) return this.dataList;
      return this.dataList.filter((row) => (row.name || '').includes(name));
    }
  },
  methods: {
    showForm(entity) {
      const dialogId = openDialog({
        header: '客户分类',
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '400px',
        body: h(CustomerCategoryForm, {
          entity: entity || null,
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.doSearch();
            closeDialog(dialogId);
          }
        })
      });
    },
    doSearch() {
      this.loadList();
    },
    loadList() {
      this.loading = true;
      CustomerCategory.list()
        .then(({data}) => {
          this.dataList = Array.isArray(data) ? data : [];
        })
        .finally(() => (this.loading = false));
    },
    doRemove(row) {
      DialogPlugin.confirm({
        title: '系统提示',
        content: `确认删除客户分类：${row.name}?`,
        onConfirm: () => {
          CustomerCategory.remove(row.id).then(() => {
            MessagePlugin.success('删除成功~');
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
