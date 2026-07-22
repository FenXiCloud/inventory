<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line align="center">
        <span style="font-size: 16px">结账日期：</span>
        <t-date-picker
            v-model="billDate"
            :clearable="false"
            allow-input
            style="width: 180px; border-radius: 4px"
        />
        <t-button theme="primary" style="border-radius: 4px" @click="toCheck">结账</t-button>
        <t-button variant="outline" style="border-radius: 4px" @click="antiCheckout">反结账</t-button>
      </t-space>
    </div>

    <div class="simple-page__hint">
      结账日期不能小于系统启用日期：{{ accountBook.startDate }}，也不能小于或等于上次结账日期: {{ accountBook.checkoutDate }}，结账日期之前的数据只能查询，不能修改。
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
      />
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
import {MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';
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
      startDate: null,
      checkoutDate: null,
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      columns: [
        {colKey: 'serial-number', title: '序号', width: 60},
        {colKey: 'checkDate', title: '结账日', minWidth: 140},
        {colKey: 'createDate', title: '操作日期', minWidth: 140},
        {colKey: 'checkName', title: '操作员', minWidth: 120}
      ]
    }
  },
  computed: {
    ...mapState(['accountBook']),
    queryParams() {
      return {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
      }
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
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    },
    toCheck() {
      if (this.billDate) {
        Checkout.toCheck({checkDate: this.billDate}).then(({data, success}) => {
          if (success) {
            MessagePlugin.success("结账成功~");
            this.$store.commit('updateAccountBook', data);
            window.location.replace("/");
          }
        }).finally(() => {
          this.loadList()
        })
      } else {
        MessagePlugin.error("请选择结账时间")
      }
    },
    antiCheckout() {
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认要反结账吗?`,
        onConfirm: () => {
          Checkout.antiCheckout().then(({data}) => {
            MessagePlugin.success("操作成功~");
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

<style scoped>

.simple-page__hint {
  flex-shrink: 0;
  padding: 0 0 8px;
  color: var(--td-text-color-secondary, #666);
  line-height: 1.5;
}

</style>
