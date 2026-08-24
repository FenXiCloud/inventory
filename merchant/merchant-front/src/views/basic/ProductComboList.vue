<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="showForm()">新 增</t-button>
        <t-input
            v-model="filter"
            clearable
            placeholder="请输入套餐名称/编码"
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
        <template #enabled="{ row }">
          <t-tag :theme="row.enabled ? 'primary' : 'danger'" variant="light">
            {{ row.enabled ? '启用' : '禁用' }}
          </t-tag>
        </template>
        <template #updatedAt="{ row }">
          <span>{{ fmtTime(row.updatedAt) }}</span>
        </template>
        <template #ops="{ row }">
          <t-space size="small">
            <t-link theme="primary" @click="viewComponents(row)">组件</t-link>
            <t-link theme="primary" @click="showForm(row)"><t-icon name="edit"/></t-link>
            <t-link theme="primary" @click="doRemove(row)"><t-icon name="delete"/></t-link>
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
import ProductCombo from '@js/api/basic/ProductCombo';
import Product from '@js/api/basic/Product';
import ProductComboForm from '@views/basic/ProductComboForm.vue';
import {MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import {openDialog, closeDialog} from '@common/dialog';
import {h} from 'vue';

export default {
  name: 'ProductComboList',
  data() {
    return {
      loading: false,
      filter: '',
      dataList: [],
      productMap: {},
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      columns: [
        {colKey: 'ops', title: '操作', width: 140, fixed: 'left', align: 'center'},
        {colKey: 'code', title: '编码', width: 140},
        {colKey: 'name', title: '套餐名称', minWidth: 180, ellipsis: true},
        {colKey: 'remarks', title: '备注', ellipsis: true},
        {colKey: 'updatedAt', title: '更新时间', width: 160, align: 'center'},
        {colKey: 'enabled', title: '状态', width: 90, align: 'center', fixed: 'right'}
      ]
    };
  },
  methods: {
    showForm(entity) {
      const dialogId = openDialog({
        header: entity?.id ? '编辑商品套餐' : '新增商品套餐',
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '800px',
        body: h(ProductComboForm, {
          entity: entity || null,
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.loadList();
            closeDialog(dialogId);
          }
        })
      });
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    loadList() {
      this.loading = true;
      ProductCombo.list({
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        filter: this.filter || null
      })
        .then(({data}) => {
          this.dataList = data?.results || [];
          this.pagination.total = data?.total || 0;
        })
        .finally(() => (this.loading = false));
    },
    fmtTime(v) {
      if (!v) return '-';
      const s = String(v).replace('T', ' ');
      return s.length > 16 ? s.substring(0, 16) : s;
    },
    viewComponents(row) {
      ProductCombo.load(row.id).then(({data}) => {
        const items = (data && data.comboItemList) || [];
        const rows = items.map((it) => {
          const p = this.productMap[it.productId] || {};
          return { name: p.name || `商品#${it.productId}`, unitName: p.unitName || '', quantity: it.quantity };
        });
        const body = h('div', { style: 'max-height:420px;overflow:auto' },
          rows.length
            ? rows.map((r) => h('div', {
                style: 'display:flex;justify-content:space-between;align-items:center;padding:9px 12px;border-bottom:1px solid #f0f0f0;font-size:14px'
              }, [
                h('span', { style: 'color:#1f2329' }, r.name),
                h('span', { style: 'color:#646a73' }, `${r.unitName ? r.unitName + ' × ' : ''}${r.quantity}`)
              ]))
            : [h('div', { style: 'padding:20px;text-align:center;color:#8f959e' }, '暂无组件')]
        );
        openDialog({
          header: `套餐组件：${row.name}`,
          closeOnOverlayClick: true,
          closeBtn: true,
          width: '480px',
          body
        });
      });
    },
    doRemove(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除套餐：${row.name}?`,
        onConfirm: () => {
          ProductCombo.remove(row.id).then(() => {
            MessagePlugin.success('删除成功~');
            this.loadList();
          });
        }
      });
    }
  },
  created() {
    this.loadList();
    Product.select().then(({data}) => {
      this.productMap = {};
      (data || []).forEach((p) => {
        this.productMap[p.id] = p;
      });
    });
  }
};
</script>
