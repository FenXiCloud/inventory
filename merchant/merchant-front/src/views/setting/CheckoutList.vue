<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <label class="mr-5px" style="font-size: 16px !important;">结账日期：</label>
        <DatePicker v-model="billDate" :clearable="false"></DatePicker>
      </template>
      <template #tools>
        <Button @click="toCheck" color="primary">结账</Button>
        <Button  @click="antiCheckout">反结账</Button>
      </template>
    </vxe-toolbar>
    <div class="mb-5px">结账日期不能小于系统启用日期：{{accountBook.startDate}}，也不能小于或等于上次结账日期: {{accountBook.checkoutDate}}，结账日期之前的数据只能查询，不能修改。</div>
    <div class="flex1">
      <vxe-table row-id="id"
                 ref="table"
                 height="auto"
                 :data="dataList"
                 highlight-hover-row
                 show-overflow
                 show-footer
                 :row-config="{height: 48}"
                 :column-config="{resizable: true}"
                 :sort-config="{remote:true}"
                 :loading="loading">
        <vxe-column type="seq" width="50" title="序号"/>
        <vxe-column title="结账日" field="checkDate" />
        <vxe-column title="操作日期" field="createDate" />
        <vxe-column title="操作员" field="checkName"  />
      </vxe-table>
    </div>
    <div class="flex justify-between items-center pt-5px">
      <div></div>
      <vxe-pager perfect @page-change="loadList(false)"
                 v-model:current-page="pagination.page"
                 v-model:page-size="pagination.pageSize"
                 :total="pagination.total"
                 :layouts="['PrevJump', 'PrevPage', 'Number', 'NextPage', 'NextJump', 'Sizes', 'FullJump', 'Total']">
        <template #left>
          <vxe-button @click="loadList(false)" type="text" size="mini" icon="fa fa-refresh"
                      :loading="loading"></vxe-button>
        </template>
      </vxe-pager>
    </div>
  </div>
</template>
<script>
import {confirm, message} from "heyui.ext";
import Checkout from "@js/api/setting/Checkout";
import manba from "manba";
import {mapState} from 'vuex';

export default {
  name: "CheckoutList",
  data() {
    return {
      dataList: [],
      loading: false,
      billDate: manba().format("YYYY-MM-dd"),
      startDate:null,
      checkoutDate:null,
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
    }
  },
  computed: {
    ...mapState(['accountBook']),
    queryParams() {
      return Object.assign({
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
      })
    },
  },
  methods: {
    loadList() {
      this.loading = true;
      Checkout.list(this.queryParams).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
    toCheck(){
      if (this.billDate){
      Checkout.toCheck({checkDate:this.billDate}).then(({data,success})=>{
        if (success){
          message("结账成功~");
          this.$store.commit('updateAccountBook', data);
          window.location.replace("/");
        }
      }).finally(()=>{
        this.loadList()
      })
      }else {
        message.error("请选择结账时间")
      }
    },
    antiCheckout() {
      confirm({
        title: "系统提示",
        content: `确认要反结账吗?`,
        onConfirm: () => {
          Checkout.antiCheckout().then(({data}) => {
            message("操作成功~");
            this.$store.commit('updateAccountBook', data);
            this.loadList();
          })
        }
      })
    }
  },
  created() {
    this.loadList();
  }
}
</script>
