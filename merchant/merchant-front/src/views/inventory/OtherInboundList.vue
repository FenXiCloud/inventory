<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="addForm()">新 增</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="approved()">审 核</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="backApproved()">反审核</t-button>
        <t-select
            v-model="params.supplierIds"
            :options="supplierList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            multiple
            placeholder="供应商"
            style="width: 160px; border-radius: 4px"
        />
        <t-select
            v-model="params.customerIds"
            :options="customerList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            multiple
            placeholder="客户"
            style="width: 160px; border-radius: 4px"
        />
        <t-select
            v-model="params.warehouseIds"
            :options="warehouseList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            multiple
            placeholder="仓库"
            style="width: 160px; border-radius: 4px"
        />
        <t-select
            v-model="params.productIds"
            :options="productList"
            :keys="{ value: 'id', label: 'name' }"
            filterable
            clearable
            multiple
            placeholder="产品"
            style="width: 160px; border-radius: 4px"
        />
        <t-select
            v-model="params.state"
            :options="stateOptions"
            clearable
            placeholder="审核状态"
            style="width: 140px; border-radius: 4px"
        />
        <t-select
            v-model="params.inboundType"
            :options="inboundTypeOptions"
            clearable
            placeholder="业务类型"
            style="width: 140px; border-radius: 4px"
        />
        <t-date-range-picker
            v-model="dateRangeValue"
            clearable
            allow-input
            placeholder="单据日期"
            style="width: 260px; border-radius: 4px"
        />
        <t-input
            v-model="params.filter"
            clearable
            placeholder="请输入单据编号/客户名称/制单人"
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
          :foot-data="footData"
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
      <span class="simple-page__total">总金额：{{ amountTotal }}元</span>
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
import manba from "manba";
import OtherInbound from "@js/api/inventory/OtherInbound";
import {mapMutations} from "vuex";
import {MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';
import Product from "@js/api/basic/Product";
import Warehouse from "@js/api/basic/Warehouse";
import Supplier from "@js/api/basic/Supplier";
import Customer from "@js/api/basic/Customer";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-DD");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-DD");
export default {
  name: "OtherInboundList",
  data() {
    return {
      dataList: [],
      supplierList: [],
      warehouseList: [],
      customerList: [],
      productList: [],
      selectedRowKeys: [],
      selectedRows: [],
      loading: false,
      amountTotal: 0,
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      params: {
        productIds: [],
        warehouseIds: [],
        customerIds: [],
        supplierIds: [],
        filter: null,
        state: null,
        sortCol: null,
        sort: null,
        inboundType: null
      },
      dateRangeValue: [startTime, endTime],
      stateOptions: [
        {label: '未审核', value: '已保存'},
        {label: '已审核', value: '已审核'},
      ],
      inboundTypeOptions: [
        {label: '盘盈入库', value: '盘盈入库'},
        {label: '其他入库', value: '其他入库'},
      ],
      columns: [
        {colKey: 'row-select', type: 'multiple', width: 46},
        {colKey: 'ops', title: '操作', width: 110, fixed: 'left', align: 'center'},
        {colKey: 'inboundDate', title: '单据日期', width: 120, align: 'center'},
        {colKey: 'orderNo', title: '单据编号', minWidth: 160, ellipsis: true},
        {colKey: 'inboundType', title: '业务类型', width: 110, ellipsis: true},
        {colKey: 'totalAmount', title: '金额', width: 110, align: 'right'},
        {colKey: 'supplierCode', title: '供应商编号', width: 120, ellipsis: true},
        {colKey: 'supplierName', title: '供应商', minWidth: 120, ellipsis: true},
        {colKey: 'customerCode', title: '客户编码', width: 100, align: 'center'},
        {colKey: 'customerName', title: '客户', width: 100, align: 'center', ellipsis: true},
        {colKey: 'quantity', title: '数量', width: 90, align: 'right'},
        {colKey: 'createdByName', title: '制单人', width: 90, align: 'center'},
        {colKey: 'remarks', title: '单据备注', minWidth: 100, ellipsis: true},
        {colKey: 'orderStatus', title: '审核状态', width: 100, align: 'center', fixed: 'right'},
      ]
    }
  },
  computed: {
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        start: start || null,
        end: end || null,
      })
    },
    footData() {
      const sum = (key, digits = 2) => {
        const total = (this.dataList || []).reduce((acc, row) => acc + Number(row[key] || 0), 0);
        return total.toFixed(digits);
      };
      return [{
        ops: '合计',
        totalAmount: sum('totalAmount'),
        quantity: sum('quantity', 0),
      }];
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
    addForm(type = 'add', otherInboundId = null) {
      this.pushTab({
        key: 'OtherInboundForm',
        title: type === 'edit' ? '编辑其他入库单' : type === 'look' ? '查看其他入库单' : '新增其他入库单',
        params: {type: type, otherInboundId: otherInboundId}
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
          return OtherInbound.approved('已审核', ids).then(() => {
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
          return OtherInbound.approved('已保存', ids).then(() => {
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
      const params = JSON.parse(JSON.stringify(this.queryParams));
      params.productIds = (params.productIds || []).join(",");
      params.warehouseIds = (params.warehouseIds || []).join(",");
      params.supplierIds = (params.supplierIds || []).join(",");
      params.customerIds = (params.customerIds || []).join(",");
      OtherInbound.list(params).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
        let amountTotal = 0;
        this.dataList.forEach(item => {
          amountTotal += Number(item.totalAmount);
        });
        this.amountTotal = amountTotal;
      }).finally(() => this.loading = false);
    },
    loadDict() {
      Promise.all([Product.select(), Warehouse.select(), Supplier.select(), Customer.select()])
          .then((results) => {
            this.productList = results[0].data || [];
            this.warehouseList = results[1].data || [];
            this.supplierList = results[2].data || [];
            this.customerList = results[3].data || [];
          });
    },
    doRemove(row) {
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认删除：${row.orderNo}?`,
        onConfirm: () => {
          return OtherInbound.remove(row.id).then(() => {
            MessagePlugin.success("删除成功~");
            this.loadList();
          });
        },
      });
    },
  },
  created() {
    this.loadDict();
    this.loadList();
  }
}
</script>

