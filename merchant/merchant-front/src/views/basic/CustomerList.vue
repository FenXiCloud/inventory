<template>
  <div class="customer-page">
    <aside class="customer-page__aside">
      <div class="customer-category-panel">
        <div class="customer-category-panel__hd">
          <span>分类名称</span>
          <t-link theme="primary" title="新增分类" @click="showCustomerCategoryForm()">
            <t-icon name="add"/>
          </t-link>
        </div>
        <div class="customer-category-panel__bd">
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
              <div v-if="node.value !== 'ALL'" class="customer-category-ops" @click.stop>
                <t-link theme="primary" title="编辑" @click="showCustomerCategoryForm(node.data)">
                  <t-icon name="edit"/>
                </t-link>
                <t-link theme="primary" title="删除" @click="deleteCustomerCategory(node.data)">
                  <t-icon name="delete"/>
                </t-link>
              </div>
            </template>
          </t-tree>
        </div>
      </div>
    </aside>

    <section class="customer-main">
      <div class="customer-main__toolbar">
        <t-space break-line>
          <t-button theme="primary" style="border-radius: 4px" @click="showCustomerForm()">新 增</t-button>
          <t-button style="border-radius: 4px" @click="showCustomerImportForm()">导入</t-button>
          <t-button style="border-radius: 4px" @click="exportCustomerToExcel()">导出</t-button>
          <t-input
              v-model="params.name"
              clearable
              placeholder="请输入客户名称"
              style="width: 240px; background: #fff; border-radius: 4px"
              @enter="searchCustomer"
          >
            <template #suffixIcon>
              <t-icon name="search" style="cursor:pointer" @click="searchCustomer"/>
            </template>
          </t-input>
          <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="searchCustomer">查询</t-button>
        </t-space>
      </div>

      <div class="customer-main__table">
        <t-table
            row-key="id"
            size="medium"
            bordered
            stripe
            hover
            height="100%"
            table-layout="auto"
            :data="customerDataList"
            :columns="customerColumns"
            :loading="loading"
        >
          <template #ops="{ row }">
            <t-space size="small">
              <t-link theme="primary" @click="showCustomerForm(row)"><t-icon name="edit"/></t-link>
              <t-link theme="primary" @click="deleteCustomer(row)"><t-icon name="delete"/></t-link>
            </t-space>
          </template>
        </t-table>
      </div>

      <div class="customer-main__pager">
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
import CustomerForm from './CustomerForm.vue';
import Customer from '@js/api/basic/Customer';
import {MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import {openDialog, closeDialog} from '@common/dialog';
import {h} from 'vue';
import CustomerCategoryForm from '@views/basic/CustomerCategoryForm.vue';
import CustomerCategory from '@js/api/basic/CustomerCategory';
import CustomerImportForm from '@views/basic/CustomerImportForm.vue';
import {downloadBlob} from 'download.js';
export default {
  name: 'CustomerList',
  components: {CustomerForm},
  data() {
    return {
      loading: false,
      params: {
        name: null,
        customerCategoryId: null
      },
      customerCategoryDataList: [],
      customerDataList: [],
      selectedCategoryKeys: ['ALL'],
      pagination: {
        page: 1,
        pageSize: 10,
        total: 0
      },
      customerColumns: [
        {colKey: 'ops', title: '操作', width: 90, fixed: 'left', align: 'center'},
        {colKey: 'code', title: '编码', width: 120},
        {colKey: 'name', title: '客户名称', minWidth: 160, ellipsis: true},
        {colKey: 'categoryName', title: '分类', width: 100},
        {colKey: 'levelName', title: '等级', width: 100},
        {colKey: 'contact', title: '联系人', width: 100},
        {colKey: 'phone', title: '电话', width: 120},
        {colKey: 'balance', title: '应收余额', width: 110, align: 'right'},
        {colKey: 'remarks', title: '备注', minWidth: 120, ellipsis: true}
      ]
    };
  },
  computed: {
    categoryTree() {
      const list = (this.customerCategoryDataList || []).map((item) => ({
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
      this.params.customerCategoryId = key === 'ALL' ? null : key;
      this.pagination.page = 1;
      this.loadCustomer();
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadCustomer();
    },
    exportCustomerToExcel() {
      this.loading = true;
      Customer.exportToExcel()
        .then((blob) => {
          downloadBlob('客户档案.xlsx', blob);
        })
        .finally(() => {
          this.loading = false;
        });
    },
    showCustomerImportForm() {
      const dialogId = openDialog({
        header: '客户导入',
        closeOnOverlayClick: false,
        width: '50vw',
        body: h(CustomerImportForm, {
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.searchCustomer();
            closeDialog(dialogId);
          }
        })
      });
    },
    showCustomerCategoryForm(entity) {
      const dialogId = openDialog({
        header: '客户分类',
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '400px',
        body: h(CustomerCategoryForm, {
          entity,
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.loadCustomerCategory();
            closeDialog(dialogId);
          }
        })
      });
    },
    deleteCustomerCategory(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除客户分类：${row.name}?`,
        onConfirm: () => {
          CustomerCategory.remove(row.id).then(() => {
            MessagePlugin.success('删除成功~');
            this.loadCustomerCategory();
            this.params.customerCategoryId = null;
            this.loadCustomer();
          });
        }
      });
    },
    loadCustomerCategory() {
      CustomerCategory.select().then(({data}) => {
        this.customerCategoryDataList = data || [];
        this.selectedCategoryKeys = ['ALL'];
        this.params.customerCategoryId = null;
      });
    },
    searchCustomer() {
      this.pagination.page = 1;
      this.loadCustomer();
    },
    showCustomerForm(entity) {
      const dialogId = openDialog({
        header: '客户信息',
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '720px',
        body: h(CustomerForm, {
          entity,
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.searchCustomer();
            closeDialog(dialogId);
          }
        })
      });
    },
    deleteCustomer(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除客户：${row.name}?`,
        onConfirm: () => {
          Customer.delete(row.id).then(() => {
            MessagePlugin.success('删除成功~');
            this.loadCustomer();
          });
        }
      });
    },
    loadCustomer() {
      this.loading = true;
      Customer.list(this.queryParams)
        .then(({data: {results, total}}) => {
          this.customerDataList = results || [];
          this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
    }
  },
  created() {
    this.loadCustomerCategory();
    this.loadCustomer();
  }
};
</script>

<style scoped>
.customer-page {
  height: 100%;
  min-height: 0;
  display: flex;
  gap: 5px;
  overflow: hidden;
  box-sizing: border-box;
}

.customer-page__aside {
  width: 260px;
  flex-shrink: 0;
  min-height: 0;
  background: #fff;
  border-radius: 4px;
  overflow: hidden;
}

.customer-category-panel {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.customer-category-panel__hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid var(--td-component-border, #dcdcdc);
  font-weight: 600;
  flex-shrink: 0;
}

.customer-category-panel__bd {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 4px 5px;
}

.customer-category-ops {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.customer-main {
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

.customer-main__toolbar {
  flex-shrink: 0;
  padding: 8px 0;
}

.customer-main__table {
  flex: 1;
  height: 0;
  min-height: 0;
  overflow: auto;
}

.customer-main__pager {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid var(--td-component-border, #dcdcdc);
  background: #fff;
}
</style>
