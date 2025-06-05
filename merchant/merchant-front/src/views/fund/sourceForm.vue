<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <vxe-table
        row-id="id"
        ref="table"
        :data="dataList"
        highlight-hover-row
        show-overflow
        show-footer
        :footer-method="footerMethod"
        :row-config="{ height: 48 }"
        :column-config="{ resizable: true }"
        :sort-config="{ remote: true }"
        :loading="loading"
      >
        <vxe-column type="checkbox" width="40" align="center" />
        <!--   <vxe-column title="操作" align="center" width="120">
          <template #default="{ row }">
            <span
              class="primary-color text-hover ml-10px"
              @click="showForm('add', row.id)"
              >编辑</span
            >
            <span
              class="primary-color text-hover ml-10px"
              @click="doRemove(row)"
              >删除</span
            >
          </template>
        </vxe-column> -->

        <vxe-column title="订单编号" field="salesOrderNo" width="200" />
        <vxe-column title="业务类别" field="businessType">
          <template #default="{ row }">
            {{ row.businessType == 1 ? '普通销售' : '' }}
          </template>
        </vxe-column>

        <vxe-column
          title="单据日期"
          field="businessDate"
          align="center"
          width="130"
        />
        <vxe-column title="单据金额" field="documentAmount" width="120" />
        <vxe-column title="已核销金额" field="verifiedAmount" width="120" />
        <vxe-column title="未核销金额" field="unverifiedAmount" width="120" />
        <!-- <vxe-column title="客户地址" field="finalAmount" width="120" />
        <vxe-column title="业务员" field="finalAmount" width="120" />
        <vxe-column title="备注" field="finalAmount" width="120" /> -->
      </vxe-table>
      <!-- <div class="flex justify-between items-center pt-5px"> -->
      <vxe-pager
        perfect
        @page-change="loadList(false)"
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :layouts="[
          'PrevJump',
          'PrevPage',
          'Number',
          'NextPage',
          'NextJump',
          'Sizes',
          'Total'
        ]"
      >
      </vxe-pager>
      <!-- </div> -->
    </div>

    <div class="modal-column-between">
      <Button @click="$emit('close')" :loading="loading"> 取消 </Button>
      <Button color="primary" @click="confirm" :loading="loading">
        保存
      </Button>
    </div>
  </div>
</template>

<script>
/**
 * @功能描述: 账户FORM
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */

import { message } from 'heyui.ext';
import OrderReceipt from '@js/api/fund/OrderReceipt';
import OrderPayment from '@js/api/fund/OrderPayment';
import { add, objectEach } from 'xe-utils';
// import {CopyObj} from "@common/utils";

export default {
  name: 'sourceForm',
  props: {
    params: Object,
    URL: {
      type: String,
      default: 'OrderReceipt'
    }
  },
  data() {
    return {
      loading: false,
      model: {},
      validationRules: {},
      pagination: {
        page: 1,
        pageSize: 5,
        total: 0
      }
    };
  },
  methods: {
    loadList() {
      const apiMap = {
        OrderReceipt,
        OrderPayment
      };
      const apiModule = apiMap[this.URL];
      this.loading = true;
      console.log(apiModule, 'apiModule');

      let params = {};
      if (this.URL === 'OrderReceipt') {
        params.customerId = this.params.customerId; // 客户ID
      } else if (this.URL === 'OrderPayment') {
        params.supplierId = this.params.supplierId; // 供应商ID
      }
      params.page = this.pagination.page;
      params.pageSize = this.pagination.pageSize;

      apiModule
        .writeOffTheOrder(params)
        .then(({ data: { results, total } }) => {
          this.dataList = results || [];
          this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
    },
    confirm() {
      let checkList = this.$refs.table.getCheckboxRecords().map((item) => {
        if (this.URL === 'OrderPayment') {
          item.businessId = item.salesOrderId;
          item.businessNo = item.salesOrderNo;
        }

        delete item.id;
        return item;
      });
      console.log(checkList, 'checkListClonecheckListClonecheckListClone');
      this.$emit('success', checkList);
    }
  },
  created() {
    this.loadList();

    // this.orderPayment
  }
};
</script>
