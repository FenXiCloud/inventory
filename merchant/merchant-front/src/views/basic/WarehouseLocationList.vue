<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="addLocation">新 增</t-button>
        <t-button theme="danger" variant="outline" style="border-radius: 4px" @click="batchDelete"
                  :disabled="selectedRowKeys.length === 0">批量删除</t-button>
        <t-select
            v-model="params.warehouseId"
            :options="warehouseList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            placeholder="选择仓库"
            style="width: 160px; border-radius: 4px"
        />
        <t-select
            v-model="params.type"
            :options="typeOptions"
            filterable
            clearable
            placeholder="货位类型"
            style="width: 120px; border-radius: 4px"
        />
        <t-input
            v-model="params.keyword"
            clearable
            placeholder="编码/名称"
            style="width: 200px; background: #fff; border-radius: 4px"
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
          :selected-row-keys="selectedRowKeys"
          @select-change="onSelectChange"
      >
        <template #type="{ row }">
          <t-tag :theme="row.type === 'WHOLE' ? 'primary' : 'warning'" variant="light">
            {{ row.type === 'WHOLE' ? '整货' : '零货' }}
          </t-tag>
        </template>
        <template #enabled="{ row }">
          <t-tag :theme="row.enabled ? 'success' : 'danger'" variant="light">
            {{ row.enabled ? '启用' : '禁用' }}
          </t-tag>
        </template>
        <template #op="{ row }">
          <t-space>
            <t-link theme="primary" @click="editLocation(row)">编辑</t-link>
            <t-link theme="danger" @click="deleteLocation(row)">删除</t-link>
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

    <!-- 货位表单弹窗 -->
    <t-dialog
        v-model:visible="showForm"
        :header="formTitle"
        :width="600"
        :footer="false"
    >
      <warehouse-location-form
          :data="currentLocation"
          :warehouse-list="warehouseList"
          @success="onFormSuccess"
          @cancel="showForm = false"
      />
    </t-dialog>
  </div>
</template>

<script>
import {MessagePlugin, DialogPlugin} from "tdesign-vue-next";
import WarehouseLocation from "@js/api/basic/WarehouseLocation";
import Warehouse from "@js/api/basic/Warehouse";
import WarehouseLocationForm from "./WarehouseLocationForm.vue";

export default {
  name: "WarehouseLocationList",
  components: {WarehouseLocationForm},
  data() {
    return {
      dataList: [],
      loading: false,
      selectedRowKeys: [],
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      params: {
        warehouseId: null,
        type: null,
        keyword: null
      },
      warehouseList: [],
      typeOptions: [
        {label: '整货', value: 'WHOLE'},
        {label: '零货', value: 'ZERO'}
      ],
      columns: [
        {colKey: 'serial-number', title: '序号', width: 60},
        {colKey: 'warehouseName', title: '所属仓库', width: 150},
        {colKey: 'code', title: '货位编码', width: 120},
        {colKey: 'name', title: '货位名称', width: 180},
        {colKey: 'type', title: '货位类型', width: 100, align: 'center'},
        {colKey: 'capacity', title: '容量', width: 100, align: 'right'},
        {colKey: 'currentQty', title: '当前库存', width: 100, align: 'right'},
        {colKey: 'enabled', title: '状态', width: 80, align: 'center'},
        {colKey: 'sort', title: '排序', width: 80, align: 'center'},
        {colKey: 'remark', title: '备注', minWidth: 150},
        {colKey: 'op', title: '操作', width: 120, align: 'center'}
      ],
      showForm: false,
      formTitle: '新增货位',
      currentLocation: null
    };
  },
  computed: {
    queryParams() {
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize
      });
    }
  },
  methods: {
    loadList() {
      this.loading = true;
      WarehouseLocation.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
    loadWarehouses() {
      Warehouse.select().then(({data}) => {
        this.warehouseList = data || [];
      });
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
    onSelectChange(selectedKeys) {
      this.selectedRowKeys = selectedKeys;
    },
    addLocation() {
      this.formTitle = '新增货位';
      this.currentLocation = null;
      this.showForm = true;
    },
    editLocation(row) {
      this.formTitle = '编辑货位';
      this.currentLocation = Object.assign({}, row);
      this.showForm = true;
    },
    deleteLocation(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除货位"${row.name}"吗？`,
        onConfirm: () => {
          WarehouseLocation.delete(row.id).then(() => {
            MessagePlugin.success('删除成功');
            this.loadList();
          });
        }
      });
    },
    batchDelete() {
      if (this.selectedRowKeys.length === 0) {
        MessagePlugin.warning('请选择要删除的货位');
        return;
      }
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除选中的${this.selectedRowKeys.length}个货位吗？`,
        onConfirm: () => {
          WarehouseLocation.batchDelete(this.selectedRowKeys).then(() => {
            MessagePlugin.success('删除成功');
            this.selectedRowKeys = [];
            this.loadList();
          });
        }
      });
    },
    onFormSuccess() {
      this.showForm = false;
      this.loadList();
    }
  },
  created() {
    this.loadWarehouses();
    this.loadList();
  }
};
</script>

<style scoped>
</style>
