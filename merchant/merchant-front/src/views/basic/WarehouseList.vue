<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="showWarehouseForm()">新 增</t-button>
        <t-input
            v-model="params.name"
            clearable
            placeholder="请输入仓库名称"
            style="width: 240px; border-radius: 4px"
            @enter="searchWarehouse"
        >
          <template #suffixIcon>
            <t-icon name="search" style="cursor:pointer" @click="searchWarehouse"/>
          </template>
        </t-input>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="searchWarehouse">查询</t-button>
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
          :data="warehouseDataList"
          :columns="columns"
          :loading="loading"
      >
        <template #ops="{ row }">
          <t-space size="small">
            <t-link theme="primary" @click="showWarehouseForm(row)"><t-icon name="edit"/></t-link>
            <t-link theme="primary" @click="deleteWarehouse(row)"><t-icon name="delete"/></t-link>
          </t-space>
        </template>
        <template #systemDefault="{ row }">
          <t-tag :theme="row.systemDefault ? 'primary' : 'warning'" variant="light">
            {{ row.systemDefault ? '是' : '否' }}
          </t-tag>
        </template>
        <template #enabled="{ row }">
          <t-tag :theme="row.enabled ? 'primary' : 'danger'" variant="light">
            {{ row.enabled ? '启用' : '禁用' }}
          </t-tag>
        </template>
      </t-table>
    </div>
  </div>
</template>

<script>
import {openDialog, closeDialog} from '@common/dialog';
import {h} from 'vue';
import WarehouseForm from '@views/basic/WarehouseForm';
import Warehouse from '@js/api/basic/Warehouse';
import {MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';

export default {
  name: 'WarehouseList',
  data() {
    return {
      loading: false,
      params: {name: ''},
      warehouseDataList: [],
      columns: [
        {colKey: 'ops', title: '操作', width: 90, fixed: 'left', align: 'center'},
        {colKey: 'code', title: '仓库编码', width: 140},
        {colKey: 'name', title: '仓库名称', minWidth: 160, ellipsis: true},
        {colKey: 'address', title: '仓库地址', minWidth: 200, ellipsis: true},
        {colKey: 'systemDefault', title: '默认', width: 90, align: 'center'},
        {colKey: 'enabled', title: '状态', width: 90, align: 'center', fixed: 'right'}
      ]
    };
  },
  computed: {
    queryParams() {
      return Object.assign({}, this.params);
    }
  },
  methods: {
    showWarehouseForm(warehouse = null) {
      const dialogId = openDialog({
        header: '仓库信息',
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '520px',
        body: h(WarehouseForm, {
          warehouse,
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.searchWarehouse();
            closeDialog(dialogId);
          }
        })
      });
    },
    loadWarehouse() {
      this.loading = true;
      Warehouse.list(this.queryParams)
        .then(({data}) => {
          this.warehouseDataList = data || [];
        })
        .finally(() => (this.loading = false));
    },
    searchWarehouse() {
      this.loadWarehouse();
    },
    deleteWarehouse(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除：${row.name}?`,
        onConfirm: () => {
          Warehouse.delete(row.id).then(() => {
            MessagePlugin.success('删除成功~');
            this.searchWarehouse();
          });
        }
      });
    }
  },
  created() {
    this.loadWarehouse();
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
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
</style>
