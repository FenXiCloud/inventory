<template>
  <div class="frame-page flex flex-column" style="height: auto !important">
    <vxe-toolbar>
      <template #buttons>
        <!-- <Search
          v-model.trim="form.filter"
          search-button-theme="h-btn-default"
          show-search-button
          class="w-360px ml-8px"
          placeholder="请输入客户名称"
          @search="doSearch"
        >
          <i class="h-icon-search" />
        </Search> -->
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">
            <span style="color: red">*</span>客户</span
          >
          <div style="position: relative;">

          <Select
          
            v-model="form.customerName"
            class="w-120px z-index-1"
            :datas="customerDataList"
            keyName="id"
            titleName="name"
            placeholder="选择客户"
            @change="selectCustomer($event)"
          >
            <template #bottom>
              <Button no-border icon="h-icon-plus" @click="addCustomer()"
                >新建</Button
              >
            </template>
          </Select>
        </div>

        </div>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">总欠款</span>
          <Input type="text" class="w-120px" value="123" disabled />
        </div>
        <div class="h-input-group">
          <span class="h-input-addon ml-8px">业务员</span>
          <Select
          ref="selectRef"
          style="z-index: 1;"
            v-model="form.orderStaffName"
            class="w-120px"
            :datas="orderStaffList"
            keyName="id"
            titleName="name"
            placeholder="选择业务员"
            @change="selectOrderStaff($event)"
          >
            <template #bottom>
              <Button no-border icon="h-icon-plus" @click="addOrderStaff()"
                >新建</Button
              >
            </template>
          </Select>
        </div>

        <div class="h-input-group">
          <span class="h-input-addon ml-8px">订单日期</span>
          <DateRangePicker v-model="dateRange"></DateRangePicker>
        </div>
      </template>

      <template #tools>
        <Button @click="addForm()" color="primary">保存并新增</Button>
        <Button @click="addForm()" color="primary">保 存</Button>
        <Button>审 核</Button>
      </template>
    </vxe-toolbar>
    <div class="flex1">
      <vxe-table
        border
        show-overflow
        :edit-config="editConfig"
        :data="tableData"
        :show-footer="showFooter"
        :footer-method="footerMethod"
      >
        <vxe-column type="seq" width="70" fixed="left"></vxe-column>
        <vxe-column
          title="操作"
          field="seq"
          width="70"
          align="center"
          fixed="left"
        >
          <template #default="{ row, rowIndex }">
            <div
              class="fa fa-plus text-hover mr-5px"
              @click="adjustRows('insert', rowIndex)"
            ></div>
            <div
              class="fa fa-minus text-hover"
              v-if="isDeleting"
              @click="adjustRows('delete', rowIndex)"
            ></div>
          </template>
        </vxe-column>
        <vxe-column
          field="name"
          title="结算账户"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
        <vxe-column
          field="name2"
          title="收款金额"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
        <vxe-column field="sex" title="结算方式" :edit-render="{}">
          <template #default="{ row }">
            <span> {{ row.sex }}</span>
          </template>

          <template #edit="{ row }">
            <vxe-select
              v-model="row.sex"
              placeholder="请选择"
              :multiple="false"
              transfer
            >
              <vxe-option
                v-for="item in sexOptions"
                :key="item.value"
                :value="item.label"
                :label="item.label"
              ></vxe-option>
            </vxe-select>
          </template>
        </vxe-column>
        <vxe-column
          title="结算号"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
        <vxe-column title="备注" :edit-render="{ name: 'input' }"></vxe-column>
        <vxe-column
          title="在线交易"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
      </vxe-table>

      <!-- <vxe-toolbar
        :tools="toolbarTools"
        @tool-click="toolClickEvent"
      ></vxe-toolbar> -->

      <vxe-toolbar>
        <template #tools>
          <Button @click="addForm()" color="">选择源单</Button>
          <Button>自动核销</Button>
        </template>
      </vxe-toolbar>
      <vxe-table
        border
        show-overflow
        :data="tableData2"
        :show-footer="showFooter"
        :footer-method="footerMethod"
      >
        <vxe-column type="seq" width="70"></vxe-column>
        <vxe-column
          field="name"
          title="源单编号"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
        <vxe-column
          title="业务类别"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
        <vxe-column field="sex" title="结算方式" :edit-render="{}">
          <template #default="{ row }">
            <span> {{ row.sex }}</span>
          </template>

          <template #edit="{ row }">
            <vxe-select
              v-model="row.sex"
              placeholder="请选择"
              :multiple="false"
              transfer
            >
              <vxe-option
                v-for="item in sexOptions"
                :key="item.value"
                :value="item.label"
                :label="item.label"
              ></vxe-option>
            </vxe-select>
          </template>
        </vxe-column>
        <vxe-column
          title="单据日期"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
        <vxe-column
          title="收款到期日"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
        <vxe-column
          title="单据金额"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
        <vxe-column
          title="已核销金额"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
        <vxe-column
          title="未核销金额"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
        <vxe-column
          title="本次核销金额"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
        <vxe-column
          title="单据备注"
          :edit-render="{ name: 'input' }"
        ></vxe-column>
      </vxe-table>
      <div class="mt-10px"></div>
      <div class="filler-panel">
        <div class="filler-item" style="flex: 1; margin: 5px 0 !important">
          <Textarea
            placeholder="请输入备注"
            maxlength="150"
            style="width: 100%"
            v-model="form.remarks"
          />
        </div>
      </div>
    </div>

    <vxe-toolbar>
      <template #buttons>
        <div class="filler-panel">
          <div class="filler-item" style="flex: 1; margin: 5px 0 !important">
            <label class="mr-16px w-80px">整单折扣:</label>
            <Input v-model="form.discountRate" @blur="changeDiscountRate" />

            <label class="ml-10px w-90px">本单预收款:</label>
            <Input
              disabled
              v-model="form.discountRate"
              @blur="changeDiscountRate"
            />
          </div>
        </div>
      </template>

      <template #tools>
        <Button @click="addForm()" color="">历史单据</Button>
        <Button>操作日志</Button>
      </template>
    </vxe-toolbar>

    <div class="mb-10px"></div>
    <!-- <div class="flex justify-between items-center pt-5px">
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
        <template #left>
          <span class="mr-12px text-16px">总金额：{{ amountTotal }}元</span>
          <vxe-button
            @click="loadList(false)"
            type="text"
            size="mini"
            icon="h-icon-refresh"
            :loading="loading"
          ></vxe-button>
        </template>
      </vxe-pager>
    </div> -->
    
  </div>
</template>
<script>
import manba from 'manba';
import {layer} from "@layui/layer-vue";
import {h} from "vue";
import OrderReceipt from '@js/api/fund/OrderReceipt';
import Customer from '@js/api/basic/Customer';
// import { mapMutations } from 'vuex';
import OrderStaffForm from "./OrderStaffForm.vue";

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-dd');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-dd');

export default {
  name: 'OrderReceiptList',
  data() {
    const tableData = [{}, {}];
    const tableData2 = [{}, {}];
    const editConfig = {
      trigger: 'click',
      mode: 'cell'
    };
    const sexOptions = [
      { label: '男', value: '1' },
      { label: '女', value: '0' }
    ];

    const footerMethod = () => {
      return [['合计', '', '282', '', '', '', '']];
    };

    const toolbarTools = [
      { name: '选择源单', code: 'add', status: 'button' },
      { name: '自动核销', code: 'auto', status: 'button' }
    ];
    // const formatSex = (value) => {
    //   if (value === '1') {
    //     return '男';
    //   }
    //   if (value === '0') {
    //     return '女';
    //   }
    //   return '';
    // };
    // const formatMultiSex = (values) => {
    //   if (values) {
    //     return values.map((val) => formatSex(val)).join(',');
    //   }
    //   return '';
    // };

    return {
      val1: [],

      form: {},
      tableData,
      tableData2,
      customerDataList: [],
      orderStaffList: [],
      // customerData: {},
      // orderStaffData: {},

      loading: false,
      amountTotal: 0,
      // totalParams: {},
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      params: {
        filter: null,
        state: null,
        sortCol: null,
        sort: null
      },
      dateRange: {
        start: manba(startTime).format('YYYY-MM-dd'),
        end: manba(endTime).format('YYYY-MM-dd')
      },
      showFooter: true,

      editConfig,
      sexOptions,
      footerMethod,
      toolbarTools
      // formatSex,
      // formatMultiSex
    };
  },
  computed: {
    isDeleting() {
      return this.tableData.length > 1;
    },
    queryParams() {
      return Object.assign(this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize
      });
    }
  },
  methods: {
    // ...mapMutations(['pushTab']),
    footerMethod({ columns, data }) {
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
      });
      return [['', '', '', '', '', ''].concat(sums)];
    },

    //添加行或减少行
    adjustRows(type, index) {
      if (type === 'insert') {
        this.tableData.splice(index + 1, 0, { isNew: true });
      } else {
        this.tableData.splice(index, 1);
      }
    },
    doSearch() {
      this.pagination.page = 1;
      // this.loadList();
    },
    addEdit() {
      this.loading = true;
      OrderReceipt.addEdit(this.dataParams)
        .then(({ data: { results, total } }) => {
          this.dataList = results || [];
          this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
    },
    //加载客户列表
    loadCustomer() {
      this.loading = true;
      Customer.list(this.queryParams)
        .then(({ data: { results, total } }) => {
          this.customerDataList = results || [];
          this.pagination.total = total;
        })
        .finally(() => (this.loading = false));
    },
    //加载业务员列表
    loadOrderStaff() {
      OrderReceipt.orderStaffList()
        .then(({ data }) => {
          this.orderStaffList = data || [];
          // this.pagination.total = total;
        })
        .finally();
    },
    selectCustomer(e) {
      this.form.customerId = e.id;
    },
    selectOrderStaff(e) {
      this.form.orderStaffId = e.id;
    },
    addOrderStaff(){
      // this.$refs.selectRef?.close()
      this.showForm();
    },
    showForm(entity) {
      let type = 0;
      let layerId = layer.open({
        title: "新增职员",
        shadeClose: false,
        closeBtn: false,
        area: ['600px', '480px'],
        content: h(OrderStaffForm, {
          entity, type,
          onClose: () => {
            layer.close(layerId);
          },
          onSuccess: () => {
            this.doSearch();
            layer.close(layerId);
          }
        })
      });
    },
  },
  created() {
    this.loadCustomer();
    this.loadOrderStaff();
  }
};
</script>
<style lang="less" scoped>
:deep(.vxe-select > .vxe-input) {
  width: 100%;
  height: 100%;
}
:deep(.h-dropdown) {
    z-index: 999 !important;
}
// .z-index-1{
//   z-index: 1;
// }
</style>
