<template>
  <div class="supplier-page">
    <aside class="supplier-page__aside">
      <div class="supplier-category-panel">
        <div class="supplier-category-panel__hd">
          <span>分类名称</span>
          <t-link theme="primary" title="新增分类" @click="showSupplierCategoryForm()">
            <t-icon name="add"/>
          </t-link>
        </div>
        <div class="supplier-category-panel__bd">
          <t-tree
              :data="categoryTree"
              :keys="{ value: 'id', label: 'name', children: 'children' }"
              activable
              hover
              line
              expand-all
              transition
              :expand-on-click-node="false"
              :actived="selectedCategoryKeys"
              @active="onCategoryActive"
          >
            <template #operations="{ node }">
              <div v-if="node.value !== 'ALL'" class="supplier-category-ops" @click.stop>
                <t-link theme="primary" title="编辑" @click="showSupplierCategoryForm(node.data)">
                  <t-icon name="edit"/>
                </t-link>
                <t-link theme="primary" title="删除" @click="deleteSupplierCategory(node.data)">
                  <t-icon name="delete"/>
                </t-link>
              </div>
            </template>
          </t-tree>
        </div>
      </div>
    </aside>

    <section class="supplier-main">
      <div class="supplier-main__toolbar">
        <t-space break-line>
          <t-button theme="primary" style="border-radius: 4px" @click="showSupplierForm()">新 增</t-button>
          <t-input
              v-model="params.filter"
              clearable
              placeholder="请输入货商名称"
              style="width: 240px; background: #fff; border-radius: 4px"
              @enter="searchSupplier"
          >
            <template #suffixIcon>
              <t-icon name="search" style="cursor:pointer" @click="searchSupplier"/>
            </template>
          </t-input>
          <t-input
              v-model="params.taxNo"
              clearable
              placeholder="请输入税号"
              style="width: 200px; background: #fff; border-radius: 4px"
              @enter="searchSupplier"
          >
            <template #suffixIcon>
              <t-icon name="search" style="cursor:pointer" @click="searchSupplier"/>
            </template>
          </t-input>
          <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="searchSupplier">查询</t-button>
        </t-space>
      </div>

      <div class="supplier-main__table">
        <t-table
            row-key="id"
            size="medium"
            bordered
            stripe
            hover
            height="100%"
            table-layout="auto"
            :data="supplierDataList"
            :columns="supplierColumns"
            :loading="loading"
        >
          <template #ops="{ row }">
            <t-space size="small">
              <t-link theme="primary" @click="showSupplierForm(row)"><t-icon name="edit"/></t-link>
              <t-link theme="primary" @click="deleteSupplier(row)"><t-icon name="delete"/></t-link>
            </t-space>
          </template>
          <template #enabled="{ row }">
            <t-tag :theme="row.enabled ? 'primary' : 'danger'" variant="light">
              {{ row.enabled ? '启用' : '禁用' }}
            </t-tag>
          </template>
        </t-table>
      </div>

      <div class="supplier-main__pager">
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
    </section>
  </div>
</template>

<script>
import SupplierForm from './SupplierForm.vue';
import Supplier from '@js/api/basic/Supplier';
import {MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import {openDialog, closeDialog} from '@common/dialog';
import {h} from 'vue';
import SupplierCategoryForm from '@views/basic/SupplierCategoryForm.vue';
import SupplierCategory from '@js/api/basic/SupplierCategory';
export default {
  name: 'SupplierList',
  components: {SupplierForm},
  data() {
    return {
      loading: false,
      params: {
        filter: null,
        taxNo: null,
        supplierCategoryId: null
      },
      supplierCategoryDataList: [],
      supplierDataList: [],
      selectedCategoryKeys: ['ALL'],
      pagination: {
        page: 1,
        pageSize: 10,
        total: 0
      },
      supplierColumns: [
        {colKey: 'ops', title: '操作', width: 90, fixed: 'left', align: 'center'},
        {colKey: 'code', title: '编码', width: 120},
        {colKey: 'name', title: '货商名称', minWidth: 160, ellipsis: true},
        {colKey: 'categoryName', title: '分类', width: 100},
        {colKey: 'contact', title: '联系人', width: 100},
        {colKey: 'phone', title: '电话', width: 120},
        {colKey: 'taxNo', title: '税号', width: 160},
        {colKey: 'balance', title: '应付余额', width: 110, align: 'right'},
        {colKey: 'enabled', title: '状态', width: 90, align: 'center', fixed: 'right'}
      ]
    };
  },
  computed: {
    categoryTree() {
      const list = (this.supplierCategoryDataList || []).map((item) => ({
        id: item.id,
        name: item.name
      }));
      return [{id: 'ALL', name: '全部分类'}, ...list];
    },
    queryParams() {
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize
      });
    }
  },
  methods: {
    onCategoryActive(value) {
      const key = value && value.length ? value[0] : 'ALL';
      this.selectedCategoryKeys = [key];
      this.params.supplierCategoryId = key === 'ALL' ? null : key;
      this.pagination.page = 1;
      this.loadSupplier();
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadSupplier();
    },
    showSupplierCategoryForm(entity) {
      const dialogId = openDialog({
        header: '货商分类',
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '400px',
        body: h(SupplierCategoryForm, {
          entity,
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.loadSupplierCategory();
            closeDialog(dialogId);
          }
        })
      });
    },
    deleteSupplierCategory(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除货商分类：${row.name}?`,
        onConfirm: () => {
          SupplierCategory.delete(row.id).then(() => {
            MessagePlugin.success('删除成功~');
            this.loadSupplierCategory();
            this.params.supplierCategoryId = null;
            this.loadSupplier();
          });
        }
      });
    },
    loadSupplierCategory() {
      SupplierCategory.select().then(({data}) => {
        this.supplierCategoryDataList = data || [];
        this.selectedCategoryKeys = ['ALL'];
        this.params.supplierCategoryId = null;
      });
    },
    searchSupplier() {
      this.pagination.page = 1;
      this.loadSupplier();
    },
    showSupplierForm(entity) {
      const dialogId = openDialog({
        header: '货商信息',
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '720px',
        body: h(SupplierForm, {
          entity,
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.searchSupplier();
            closeDialog(dialogId);
          }
        })
      });
    },
    deleteSupplier(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除货商：${row.name}?`,
        onConfirm: () => {
          Supplier.remove(row.id).then(() => {
            MessagePlugin.success('删除成功~');
            this.loadSupplier();
          });
        }
      });
    },
    loadSupplier() {
      this.loading = true;
      Supplier.list(this.queryParams)
        .then(({data: {results, total}}) => {
          this.supplierDataList = results || [];
          this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
    }
  },
  created() {
    this.loadSupplierCategory();
    this.loadSupplier();
  }
};
</script>

<style scoped>
.supplier-page {
  height: 100%;
  min-height: 0;
  display: flex;
  gap: 5px;
  overflow: hidden;
  box-sizing: border-box;
}

.supplier-page__aside {
  width: 260px;
  flex-shrink: 0;
  min-height: 0;
  background: #fff;
  border-radius: 4px;
  overflow: hidden;
}

.supplier-category-panel {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.supplier-category-panel__hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid var(--td-component-border, #dcdcdc);
  font-weight: 600;
  flex-shrink: 0;
}

.supplier-category-panel__bd {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 4px 5px;
}

.supplier-category-ops {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.supplier-main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  background: #fff;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  padding: 0 12px;
  box-sizing: border-box;
  overflow: hidden;
}

.supplier-main__toolbar {
  flex-shrink: 0;
  padding: 8px 0;
}

.supplier-main__table {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.supplier-main__pager {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid var(--td-component-border, #dcdcdc);
  background: #fff;
}
</style>
