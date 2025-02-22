<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <Button @click="addForm()" color="primary">新增</Button>
        <Button @click="auditsForm('audits')">审核</Button>
        <Button @click="auditsForm('antiAudits')">反审核</Button>
      </template>
      <template #tools>
        <Select v-model="params.state" class="w-120px" :datas="{已保存:'未审核',已审核:'已审核'}"
                placeholder="审核状态："/>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">日期：</span>
          <DateRangePicker v-model="dateRange"></DateRangePicker>
        </div>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">业务类型：</span>
          <Select v-model="params.outboundType" class="w-120px" :datas="{盘亏出库:'盘亏出库',其他出库:'其他出库'}"/>
        </div>
        <Search v-model.trim="params.filter" search-button-theme="h-btn-default"
                show-search-button class="w-360px ml-8px"
                placeholder="请输入单据编号/客户名称/制单人" @search="doSearch">
          <i class="h-icon-search"/>
        </Search>
      </template>
    </vxe-toolbar>
    <div class="flex1">
      <vxe-table row-id="id"
                 ref="table"
                 height="auto"
                 :data="dataList"
                 highlight-hover-row
                 show-overflow
                 show-footer
                 :footer-method="footerMethod"
                 :row-config="{height: 48}"
                 :column-config="{resizable: true}"
                 :sort-config="{remote:true}"
                 :loading="loading">
        <vxe-column type="checkbox" width="40" align="center"/>
        <vxe-column title="操作" align="center" width="120">
          <template #default="{row}">
            <span class="primary-color  text-hover ml-10px" @click="addForm('edit',row.id)">编辑</span>
            <span v-if="editable(row)" class="red-color  text-hover ml-10px" @click="doRemove(row)">删除</span>
          </template>
        </vxe-column>
        <vxe-column title="单据日期" field="inboundDate" align="center" width="130"/>
        <vxe-column title="单据编号" field="orderNo" width="200"/>
        <!-- <vxe-column title="单据来源" field="code" width="200"/> -->
        <vxe-column title="业务类型" field="outboundType" min-width="120"/>
        <vxe-column title="订单金额" field="totalAmount" width="120"/>
        <vxe-column title="客户编码" field="customerCode" width="120"/>
        <vxe-column title="客户" field="customerName" width="120"/>
        <vxe-column title="数量" field="quantity" align="center" width="100"/>
        <vxe-column title="制单人" field="createdByName" align="center" width="100"/>
        <!-- <vxe-column title="打印次数" field="createDate" align="center" width="100"/> -->
        <vxe-column title="单据备注" field="remarks" align="center" width="100"/>
        <vxe-column title="审核状态" field="orderStatus" width="80"/>

      </vxe-table>
    </div>
    <div class="flex justify-between items-center pt-5px">
      <vxe-pager perfect @page-change="loadList(false)"
                 v-model:current-page="pagination.page"
                 v-model:page-size="pagination.pageSize"
                 :total="pagination.total"
                 :layouts="['PrevJump', 'PrevPage', 'Number', 'NextPage', 'NextJump', 'Sizes', 'Total']">
        <template #left>
          <span class="mr-12px text-16px">总金额：{{ amountTotal }}元</span>
          <vxe-button @click="loadList(false)" type="text" size="mini" icon="h-icon-refresh"
                      :loading="loading"></vxe-button>
        </template>
      </vxe-pager>
    </div>
  </div>
</template>
<script>
import manba from "manba";
import OtherOutbound from "@js/api/inventory/OtherOutbound";
import {mapMutations} from "vuex";
import {confirm, message} from "heyui.ext";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-dd");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-dd");

export default {
  name: "OtherOutboundList",
  data() {
    return {
      dataList: [],
      loading: false,
      amountTotal: 0,
      totalParams: {},
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      params: {
        filter: null,
        state: null,
        sortCol: null,
        sort: null,
        outboundType: null
      },
      dateRange: {
        start: manba(startTime).format("YYYY-MM-dd"),
        end: manba(endTime).format("YYYY-MM-dd")
      },
    }
  },
  computed: {
    queryParams() {
      return Object.assign(this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        start: this.dateRange.start,
        end: this.dateRange.end,
      })
    },
  },
  methods: {
    ...mapMutations(['pushTab']),
    addForm(type = 'add', otherOutboundId = null) {
      console.log(type, otherOutboundId);
      this.pushTab({
        key: 'OtherOutboundForm',
        title: type == 'edit' ? '编辑其他出库单' : '新增其他出库单',
        params: {type: type, otherOutboundId: otherOutboundId}
      });
    },
    footerMethod({columns, data}) {
      let sums = [];
      columns.forEach((column) => {
        if (column.property && ['finalAmount'].includes(column.property)) {
          let total = 0;
          data.forEach((row) => {
            let rd = row[column.property];
            if (rd) {
              total += Number(rd || 0);
            }
          });
          sums.push(total.toFixed(2));
        }
      })
      return [["", "", "", "", "", ""].concat(sums)];
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    loadList(type = true) {
      this.loading = true;
      OtherOutbound.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
        let amountTotal = 0;
        this.dataList.forEach(item => {
          amountTotal += Number(item.totalAmount);
        });
        this.amountTotal = amountTotal;
      }).finally(() => this.loading = false);
    },
    auditsForm(type) {
      const selectRecords = this.$refs.table.getCheckboxRecords();
      if (!selectRecords || selectRecords.length === 0) {
        message.warn("请选择要操作的数据~");
        return;
      }
      if (type === "audits") {
        const filterRecords = selectRecords.filter(item => item.orderStatus === "已保存");
        if (!filterRecords || filterRecords.length === 0) {
          message.warn("请选择状态为已保存的数据，进行审核~");
          return;
        }
        if (filterRecords.length > 1) {
          message.warn("请选择单条数据，进行审核~");
          return;
        }
        filterRecords.forEach(item => {
          this.pushTab({
            key: 'OtherOutboundForm',
            title: '审核其他出库单',
            params: {type: type, otherOutboundId: item.id}
          });
        });
        return;
      }
      if (type === "antiAudits") {
        console.info("selectRecords:", selectRecords);
        const filterRecords = selectRecords.filter(item => item.orderStatus === "已审核");
        if (!filterRecords || filterRecords.length === 0) {
          message.warn("请选择状态为已审核的数据，进行审核~");
          return;
        }
        if (filterRecords.length > 1) {
          message.warn("请选择单条数据，进行审核~");
          return;
        }
        filterRecords.forEach(item => {
          this.pushTab({
            key: 'OtherOutboundForm',
            title: '反审核其他出库单',
            params: {type: type, otherOutboundId: item.id}
          });
        });
      }
    },
    doRemove({id}) {
      confirm({
        title: "系统提示",
        content: `是否删除当前数据?`,
        onConfirm: () => {
          OtherOutbound.delete(id).then(({data}) => {
            console.log(data);
            message.success("操作成功～");
            this.loadList();
          });
        },
      });
    },
    editable(row) {
      return ['已保存'].includes(row.orderStatus);
    }
  },
  created() {
    this.loadList();
  }
}
</script>
