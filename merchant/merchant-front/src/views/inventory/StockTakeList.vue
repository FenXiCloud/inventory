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
          <span class="h-input-addon ml-8px">盘点日期：</span>
          <DateRangePicker v-model="dateRange"></DateRangePicker>
        </div>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">仓库：</span>
          <Select v-model="params.warehouseId" class="w-120px" :datas="warehouseList" keyName="id" titleName="name"/>
        </div>
        <Search v-model.trim="params.filter" search-button-theme="h-btn-default"
                show-search-button class="w-360px ml-8px"
                placeholder="请输入单据编号/仓库名称" @search="doSearch">
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
                 :row-config="{height: '60'}"
                 :column-config="{resizable: true}"
                 :sort-config="{remote:true}"
                 :loading="loading">
        <vxe-column type="checkbox" width="40" align="center"/>
        <vxe-column title="操作" align="center" width="220">
          <template #default="{row}">
            <span class="primary-color  text-hover ml-10px" @click="addForm('edit',row.id,row.orderStatus)">编辑</span>
            <span class="primary-color  text-hover ml-10px" @click="doRemove(row)">删除</span>
          </template>
        </vxe-column>
        <vxe-column title="盘点日期" field="checkDate" align="center" width="130"/>
        <vxe-column title="单据编号" field="orderNo" width="200"/>
        <!--        <vxe-column title="单据来源" field="code" width="200"/>-->
        <vxe-column title="仓库" field="warehouseName" min-width="120"/>
        <vxe-column title="盘点结果" field="orderNoResult" width="200">
          <template #default="{row}">
            <span v-if="row['orderNos'] !== undefined">
              <div class="mb-5px mt-5px" v-for="(item) in row['orderNos']">
                {{ item }}
              </div>
            </span>
          </template>
        </vxe-column>
        <vxe-column title="制单人" field="createdByName" align="center" width="120"/>
        <vxe-column title="制单时间" field="createdAt" align="center" width="150"/>
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
import StockTake from "@js/api/inventory/StockTake";
import Warehouse from "@js/api/basic/Warehouse";
import {mapMutations} from "vuex";
import {confirm, message} from "heyui.ext";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-dd");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-dd");

export default {
  name: "StockTakeList",
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
      },
      dateRange: {
        start: manba(startTime).format("YYYY-MM-dd"),
        end: manba(endTime).format("YYYY-MM-dd")
      },
      warehouseList: []
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
    addForm(type = 'add', stockTakeId = null, orderStatus = null) {
      console.log(type, stockTakeId);
      this.pushTab({
        key: 'StockTakeForm',
        title: type === 'edit' ? '编辑盘点单' : '新增盘点单',
        params: {type: type, stockTakeId: stockTakeId, status: orderStatus}
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
      StockTake.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
    //加载字典
    loadDict(callback) {
      Promise.all([Warehouse.select()])
          .then((results) => {
            this.warehouseList = results[0].data || [];
            if (callback) {
              callback();
            }
          })
          .finally(() => loading.close());
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
            key: 'StockTakeForm',
            title: '审核盘点单',
            params: {type: type, stockTakeId: item.id}
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
            key: 'StockTakeForm',
            title: '反审核盘点单',
            params: {type: type, stockTakeId: item.id}
          });
        });
      }
    },
    doRemove({id}) {
      confirm({
        title: "系统提示",
        content: `是否删除当前数据?`,
        onConfirm: () => {
          StockTake.delete(id).then(({data}) => {
            console.log(data);
            message.success("操作成功～");
            this.loadList();
          });
        },
      });
    }
  },
  created() {
    this.loadDict(() => {
      this.loadList();
    });
  }
}
</script>
