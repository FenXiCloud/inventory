<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="addForm()">新 增</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="approved()">审 核</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="backApproved()">反审核</t-button>
        <t-select
            v-model="params.type"
            :options="typeOptions"
            clearable
            placeholder="单据类型"
            style="width: 140px; border-radius: 4px"
        />
        <t-select
            v-model="params.state"
            :options="stateOptions"
            clearable
            placeholder="审核状态"
            style="width: 140px; border-radius: 4px"
        />
        <t-input
            v-model="params.filter"
            clearable
            placeholder="请输入单据编号"
            style="width: 260px; background: #fff; border-radius: 4px"
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
          :selected-row-keys="selectedRowKeys"
          @select-change="onSelectChange"
      >
        <template #ops="{ row }">
          <t-space size="small">
            <template v-if="editable(row)">
              <t-link theme="primary" @click="addForm('edit', row.id)">编辑</t-link>
              <t-link theme="danger" @click="doRemove(row)">删除</t-link>
            </template>
            <template v-else>
              <t-link theme="primary" @click="addForm('look', row.id)">查看</t-link>
            </template>
          </t-space>
        </template>
        <template #orderType="{ row }">
          <t-tag :theme="row.orderType === '组装' ? 'primary' : 'warning'" variant="light">
            {{ row.orderType }}
          </t-tag>
        </template>
        <template #orderStatus="{ row }">
          <t-tag
              :theme="row.orderStatus === '已审核' ? 'success' : 'warning'"
              variant="light"
          >
            {{ row.orderStatus === '已保存' ? '未审核' : row.orderStatus }}
          </t-tag>
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
import AssemblyOrder from "@js/api/inventory/AssemblyOrder";
import {mapMutations} from "vuex";
import {MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';

export default {
  name: "AssemblyOrderList",
  data() {
    return {
      dataList: [],
      selectedRowKeys: [],
      selectedRows: [],
      loading: false,
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      params: {
        filter: null,
        state: null,
        type: null
      },
      typeOptions: [
        {label: '组装', value: '组装'},
        {label: '拆卸', value: '拆卸'},
      ],
      stateOptions: [
        {label: '未审核', value: '已保存'},
        {label: '已审核', value: '已审核'},
      ],
      columns: [
        {colKey: 'row-select', type: 'multiple', width: 46},
        {colKey: 'ops', title: '操作', width: 110, fixed: 'left', align: 'center'},
        {colKey: 'orderNo', title: '单据编号', minWidth: 160, ellipsis: true},
        {colKey: 'orderType', title: '单据类型', width: 100, align: 'center'},
        {colKey: 'productCode', title: '成品/被拆品编码', width: 130, ellipsis: true},
        {colKey: 'productName', title: '成品/被拆品名称', minWidth: 150, ellipsis: true},
        {colKey: 'productSpecification', title: '规格型号', width: 100, ellipsis: true},
        {colKey: 'productUnitName', title: '单位', width: 80, align: 'center'},
        {colKey: 'quantity', title: '数量', width: 90, align: 'right'},
        {colKey: 'warehouseName', title: '仓库', width: 120, ellipsis: true},
        {colKey: 'costAmount', title: '成本金额', width: 110, align: 'right'},
        {colKey: 'remarks', title: '备注', minWidth: 100, ellipsis: true},
        {colKey: 'orderStatus', title: '审核状态', width: 100, align: 'center', fixed: 'right'},
      ]
    }
  },
  computed: {
    queryParams() {
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
      })
    }
  },
  methods: {
    ...mapMutations(['pushTab']),
    onSelectChange(keys, {selectedRowData}) {
      this.selectedRowKeys = keys;
      this.selectedRows = selectedRowData || [];
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    addForm(type = 'add', id = null) {
      const docName = '组装拆卸单';
      this.$store.commit('SET_TAB_DATA', {type, assemblyOrderId: id});
      this.pushTab({
        key: 'AssemblyOrderForm',
        title: type === 'edit' ? `编辑${docName}` : type === 'look' ? `查看${docName}` : `新增${docName}`,
      });
    },
    clearSelection() {
      this.selectedRowKeys = [];
      this.selectedRows = [];
    },
    editable(row) {
      return row.orderStatus === '已保存';
    },
    approved() {
      if (!this.selectedRows.length) {
        MessagePlugin.error("未选择数据~");
        return;
      }
      const ids = this.selectedRows.filter(val => val.orderStatus == '已保存').map(val => val.id);
      if (!ids.length) {
        MessagePlugin.error("所选数据无需审核~");
        return;
      }
      DialogPlugin.confirm({
        header: "批量审核提示",
        body: `本次审核${ids.length}条?`,
        onConfirm: () => {
          return AssemblyOrder.approved('已审核', ids).then(() => {
            MessagePlugin.success("操作成功~");
            this.clearSelection();
            this.loadList();
          });
        }
      });
    },
    backApproved() {
      if (!this.selectedRows.length) {
        MessagePlugin.error("未选择数据~");
        return;
      }
      const ids = this.selectedRows.filter(val => val.orderStatus == '已审核').map(val => val.id);
      if (!ids.length) {
        MessagePlugin.error("所选数据无需反审核~");
        return;
      }
      DialogPlugin.confirm({
        header: "批量反审核提示",
        body: `本次反审核${ids.length}条?`,
        onConfirm: () => {
          return AssemblyOrder.approved('已保存', ids).then(() => {
            MessagePlugin.success("操作成功~");
            this.clearSelection();
            this.loadList();
          });
        }
      });
    },
    doSearch() {
      this.pagination.page = 1;
      this.clearSelection();
      this.loadList();
    },
    loadList() {
      this.loading = true;
      AssemblyOrder.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
    doRemove(row) {
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认删除：${row.orderNo}?`,
        onConfirm: () => {
          return AssemblyOrder.remove(row.id).then(() => {
            MessagePlugin.success("删除成功~");
            this.loadList();
          });
        },
      });
    },
  },
  created() {
    this.loadList();
  }
}
</script>
