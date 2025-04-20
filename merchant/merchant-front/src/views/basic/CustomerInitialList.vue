<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <Button @click="addForm()" color="primary">新 增</Button>
        <Button @click="batchDelete()" >批量删除</Button>
      </template>
      <template #tools>
        <div class="h-input-group">
          <span class="h-input-addon">客户：</span>
          <Select class="w-178px" :multiple="true" v-model="params.customerIds"  keyName="id" titleName="name"
                  :datas="customerList" placeholder="请选择客户"/>
        </div>
        <Search v-model.trim="params.filter" search-button-theme="h-btn-default"
                show-search-button class="w-280px ml-8px"
                placeholder="请输入客户编码/名称" @search="doSearch">
          <i class="h-icon-search"/>
        </Search>
      </template>
    </vxe-toolbar>
    <div class="flex1">
      <vxe-table row-id="id"
                 ref="table"
                 :data="dataList"
                 highlight-hover-row
                 show-overflow
                 show-footer
                 :footer-method="footerMethod"
                 stripe
                 :row-config="{height: 48}"
                 :column-config="{resizable: true}"
                 :loading="loading">
        <vxe-column type="checkbox" width="40" align="center"/>
        <vxe-column type="seq" width="60" title="序号"/>
        <vxe-column title="客户编码" field="customerCode" width="150"/>
        <vxe-column title="客户名称" field="customerName"/>
        <vxe-column title="期初应收款" field="balanceBefore"/>
        <vxe-column title="期初预收款" field="amount"/>
        <vxe-column title="期初余额" field="balanceAfter"/>
        <vxe-column title="操作" align="center" width="150">
          <template #default="{row}">
            <i class="primary-color h-icon-edit ml-10px text-hover" @click="addForm('edit',row.id)"></i>
            <i class="primary-color h-icon-trash ml-10px text-hover" @click="doRemove(row)"></i>
          </template>
        </vxe-column>
      </vxe-table>
      <vxe-pager perfect @page-change="loadList(false)"
                 v-model:current-page="pagination.page"
                 v-model:page-size="pagination.pageSize"
                 :total="pagination.total"
                 :layouts="[ 'PrevPage', 'Number', 'NextPage', 'Sizes', 'Total']">
        <template #left>
          <vxe-button @click="loadList(false)" type="text" size="mini" icon="h-icon-refresh"
                      :loading="loading"></vxe-button>
        </template>
      </vxe-pager>
    </div>
  </div>
</template>

<script>
import SupplierInitial from "@js/api/basic/SupplierInitial";
import {mapMutations} from "vuex";
import {loading, confirm,message} from "heyui.ext";
import Customer from "@js/api/basic/Customer";

/**
 * @功能描述: 客户交易流水/期初
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "CustomerInitialList",
  data() {
    return {
      loading: false,
      dataList: [],
      params: {
        filter: null,
        customerFlowType:'期初',
        customerIds:[],
      },
      pagination: {
        page: 1,
        size: 20,
        total: 0
      },
      customerList: [],
    }
  },
  computed: {
    queryParams() {
      return Object.assign(this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
      })
    }
  },
  methods: {
    ...mapMutations(['pushTab']),
    doSearch() {
      this.loadList();
    },
    loadList() {
      this.loading = true;
      const params = JSON.parse(JSON.stringify(this.queryParams));
      params.customerIds = params.customerIds.join(",");
      CustomerInitial.list(params).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
    doRemove(row) {
      confirm({
        title: "系统提示",
        content: `确认删除：${row.customerName}?`,
        onConfirm: () => {
          CustomerInitial.remove(row.id).then(() => {
            message("删除成功~");
            this.loadList();
          })
        }
      })
    },
    footerMethod({columns, data}) {
      let totalBalanceBefore = 0;
      let totalAmount = 0;
      let totalBalanceAfter = 0;
      columns.forEach((column) => {
        if (column.property && ['balanceBefore','amount','balanceAfter'].includes(column.property)) {

          data.forEach((row) => {
            let rd = row[column.property];
            if (column.property === 'balanceBefore') {
              if (rd) {
                totalBalanceBefore += Number(rd || 0);
              }
            }else if (column.property === 'amount') {
              if (rd) {
                totalAmount += Number(rd || 0);
              }
            }else if (column.property === 'balanceAfter') {
              if (rd) {
                totalBalanceAfter += Number(rd || 0);
              }
            }
          });
        }
      })
      return [["", "", "","",totalBalanceBefore.toFixed(2),totalAmount.toFixed(2),totalBalanceAfter.toFixed(2)]];
    },
    addForm(type = 'add', customerInitialId = null) {
      console.log(type, customerInitialId);
      this.pushTab({
        key: 'CustomerInitialForm',
        title: type === 'edit' ? '编辑客户期初余额' : '新增客户期初余额',
        params: {type: type, customerInitialId: customerInitialId}
      });
    },
    batchDelete(){
      const selectedRows = this.$refs.table.getCheckboxRecords();
      console.log(selectedRows);
      if (selectedRows.length === 0) {
        message.error("请选择至少一条数据");
        return;
      }
      confirm({
        content: `确定批量删除数据？`,
        onConfirm: () => {
          const ids = selectedRows.map(row => row.id);
          let params = {
            ids: ids,
          };
          CustomerInitial.batchDelete(params).then((success) => {
            if (success) {
              message.success("批量删除成功");
              this.loadList(); // Refresh the list
            }
          }).finally(() =>
              loading.close()
          );
        }
      })
    },
  },
  created() {
    this.loadList();
    Promise.all([
      Customer.select(),
    ]).then((results) => {
      this.customerList = results[0].data || [];
    }).finally(() => loading.close());
  }
}
</script>
