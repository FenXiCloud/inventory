<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="addForm()">新 增</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="doRemove()">删 除</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="approved()">审 核</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="backApproved()">反审核</t-button>
        <t-select
            v-model="params.orderType"
            :options="businessTypeList"
            :keys="{ value: 'type', label: 'name' }"
            clearable
            placeholder="业务类型"
            style="width: 160px; border-radius: 4px"
        />
        <t-date-range-picker
            v-model="dateRangeValue"
            clearable
            allow-input
            placeholder="单据日期"
            style="width: 260px; border-radius: 4px"
        />
        <t-input
            v-model="params.orderNo"
            clearable
            placeholder="请输入单据号"
            style="width: 220px; background: #fff; border-radius: 4px"
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
            <template v-if="row.orderStatus != '已审核'">
              <t-link theme="primary" @click="addForm('edit', row.id)">编辑</t-link>
              <t-link theme="danger" @click="doRemove(row)">删除</t-link>
            </template>
            <template v-else>
              <t-link theme="primary" @click="addForm('edit', row.id)">查看</t-link>
            </template>
          </t-space>
        </template>
        <template #type="{ row }">
          {{ row.type == "1" ? "预收冲应收" : "预付冲应付" }}
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
import {mapMutations} from "vuex";
import {MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';
import Verification from "@js/api/fund/Verification";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-DD");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-DD");

/**
 * @功能描述: 核销单列表
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "VerificationList",
  data() {
    return {
      businessTypeList: [
        {name: "预收冲应收", type: "1"},
        {name: "预付冲应付", type: "2"},
      ],
      dataList: [],
      selectedRowKeys: [],
      selectedRows: [],
      loading: false,
      amountTotal: 0,
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0,
      },
      params: {
        orderNo: null,
        orderType: null,
      },
      dateRangeValue: [startTime, endTime],
      columns: [
        {colKey: 'row-select', type: 'multiple', width: 46},
        {colKey: 'ops', title: '操作', width: 110, fixed: 'left', align: 'center'},
        {colKey: 'orderDate', title: '单据日期', width: 120, align: 'center'},
        {colKey: 'orderNo', title: '单据编号', minWidth: 160, ellipsis: true},
        {colKey: 'type', title: '业务类型', width: 120, align: 'center'},
        {colKey: 'personnelName', title: '客户/供应商', minWidth: 140, ellipsis: true},
        {colKey: 'orderStaffName', title: '业务员', width: 100, align: 'center'},
        {colKey: 'remarks', title: '备注', minWidth: 120, ellipsis: true},
      ]
    };
  },
  computed: {
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        startTime: start || null,
        endTime: end || null,
      });
    },
    footData() {
      return [{ops: '合计'}];
    }
  },
  methods: {
    ...mapMutations(["pushTab", "closeTabKey"]),
    onSelectChange(keys, {selectedRowData}) {
      this.selectedRowKeys = keys;
      this.selectedRows = selectedRowData || [];
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    clearSelection() {
      this.selectedRowKeys = [];
      this.selectedRows = [];
    },
    getSelectedIds() {
      return (this.selectedRowKeys || []).join(",");
    },
    addForm(type = "add", orderId = null) {
      this.closeTabKey("VerificationForm");
      this.pushTab({
        keepAlive: false,
        key: "VerificationForm",
        params: {type: type, orderId: orderId},
        title: "核销单",
      });
    },
    loadTotal() {
      Verification.total(this.queryParams).then(({data}) => {
        this.amountTotal = data || 0;
      })
    },
    loadList() {
      this.loading = true;
      Verification.list(this.queryParams)
          .then(({data: {results, total}}) => {
            this.dataList = results || [];
            this.pagination.total = total;
          })
          .finally(() => (this.loading = false));
    },
    doRemove(row = null) {
      let ids = null;
      if (!row) {
        ids = this.getSelectedIds();
      } else {
        ids = row.id;
      }
      if (!ids) {
        return MessagePlugin.error('请选择至少一个订单');
      }
      DialogPlugin.confirm({
        title: "系统提示",
        content: `确认删除?`,
        onConfirm: () => {
          Verification.remove({id: ids}).then(() => {
            MessagePlugin.success("删除成功~");
            this.clearSelection();
            this.loadList();
          });
        },
      });
    },
    approved() {
      const selectedRows = this.getSelectedIds();
      if (!selectedRows) {
        MessagePlugin.error('请选择至少一个订单');
        return;
      }
      DialogPlugin.confirm({
        content: `确定审核订单？`,
        onConfirm: () => {
          Verification.approved('已审核', selectedRows).then((success) => {
            if (success !== false) {
              MessagePlugin.success('审核成功');
              this.clearSelection();
              this.loadList();
            }
          });
        }
      });
    },
    backApproved() {
      const selectedRows = this.getSelectedIds();
      if (!selectedRows) {
        MessagePlugin.error('请选择至少一个订单');
        return;
      }
      DialogPlugin.confirm({
        content: `确定反审核订单？`,
        onConfirm: () => {
          Verification.approved('已保存', selectedRows).then((success) => {
            if (success !== false) {
              MessagePlugin.success('反审核成功');
              this.clearSelection();
              this.loadList();
            }
          });
        }
      });
    },
    doSearch() {
      this.pagination.page = 1;
      this.clearSelection();
      this.loadList();
      this.loadTotal();
    },
  },
  created() {
    this.loadTotal();
    this.loadList();
  },
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

.simple-page__pager {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid var(--td-component-border, #dcdcdc);
  background: #fff;
}

.simple-page__total {
  font-size: 14px;
  color: #333639;
  flex-shrink: 0;
}
</style>
