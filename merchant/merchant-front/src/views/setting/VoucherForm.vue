<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <vxe-toolbar>
        <template #buttons>
        </template>
        <template #tools>
          <div class="h-input-group h-table-checkbox-wrap">
            <span class="h-input-addon ml-8px">仓库：</span>
            <Select :multiple="true" v-model="params.warehouseIds" class="w-120px" keyName="id" titleName="name"
                    :datas="warehouseList"/>
          </div>
          <div class="h-input-group h-table-checkbox-wrap">
            <span class="h-input-addon ml-8px">商品：</span>
            <Select :multiple="true" v-model="params.productIds" class="w-120px" keyName="id" titleName="name"
                    :datas="productList"/>
          </div>
          <div class="h-input-group h-table-checkbox-wrap">
            <span class="h-input-addon ml-8px">往来单位：</span>
            <Select :multiple="true" v-model="params.supplierIds" class="w-120px" keyName="id" titleName="name"
                    :datas="supplierList"/>
          </div>
          <div class="h-input-group h-table-checkbox-wrap">
            <span class="h-input-addon ml-8px">业务类型：</span>
            <Select autosize :filterable="true" :multiple="true" v-model="params.operationTypes" class="w-120px"
                    :datas="operationTypeList"/>
          </div>
          <div class="h-input-group">
            <span class="h-input-addon ml-8px">单据日期：</span>
            <DateRangePicker v-model="dateRange"></DateRangePicker>
          </div>
          <Search v-model.trim="params.filter" search-button-theme="h-btn-default"
                  show-search-button class="w-360px ml-8px"
                  placeholder="请输入商品名称/单据编号" @search="doSearch">
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
                   :row-config="{height: 48}"
                   :column-config="{resizable: true}"
                   :sort-config="{remote:true}"
                   :loading="loading">
          <vxe-column type="checkbox" width="40" align="center"/>
          <vxe-column title="商品编号" field="productCode" align="center" width="130"/>
          <vxe-column title="商品名称" field="productName" width="200"/>
          <vxe-column title="商品类别" field="productCategoryName" width="200"/>
          <vxe-column title="规格型号" field="productSpecification" min-width="120"/>
          <vxe-column title="单据日期" field="createdAt" width="120"/>
          <vxe-column title="业务类型" field="operationType" width="120"/>
          <vxe-column title="单据编号" field="batchNumber" width="200"/>
          <vxe-column title="往来单位" field="supplierName" width="120">
            <template #default="{ row }">
              <div v-if="row.supplierName && row.supplierName !== ''">
                {{ row.supplierName }}
              </div>
              <div v-else-if="row.customerName && row.customerName !== ''">
                {{ row.customerName }}
              </div>
              <div v-else>
              </div>
            </template>
          </vxe-column>
          <vxe-column title="仓库" field="warehouseName" align="center" width="100"/>
          <vxe-column title="单位" field="unitName" width="80"/>
          <vxe-column title="商品名称备注" field="productRemarks" width="80"/>
          <vxe-column title="入库数量" field="quantity" width="80">
            <template #default="{ row }">
              <div v-if="inboundItems.includes(row['operationType'])">
                {{ row.quantity }}
              </div>
              <div v-else>
              </div>
            </template>
          </vxe-column>
          <vxe-colgroup title="入库" align="center">
            <vxe-column title="基本单位数量" field="quantity" align="center" width="100">
              <template #default="{ row }">
                <div v-if="inboundItems.includes(row['operationType'])">
                  {{ row.quantity }}
                </div>
                <div v-else>
                </div>
              </template>
            </vxe-column>
            <vxe-column title="单位成本" field="unitPrice" align="center" width="100">
              <template #default="{ row }">
                <div v-if="inboundItems.includes(row['operationType'])">
                  {{ row.unitPrice }}
                </div>
                <div v-else>
                </div>
              </template>
            </vxe-column>
            <vxe-column title="成本" field="subtotal" align="center" width="100">
              <template #default="{ row }">
                <div
                    v-if="inboundItems.includes(row['operationType'])">
                  {{ row.subtotal }}
                </div>
                <div v-else>
                </div>
              </template>
            </vxe-column>
          </vxe-colgroup>
          <vxe-column title="出库数量" field="quantity" width="80">
            <template #default="{ row }">
              <div v-if="outboundItems.includes(row['operationType'])">
                {{ getAbsoluteValue(row.quantity) }}
              </div>
              <div v-else>
              </div>
            </template>
          </vxe-column>
          <vxe-colgroup title="出库" align="center">
            <vxe-column title="基本单位数量" field="quantity" align="center" width="100">
              <template #default="{ row }">
                <div v-if="outboundItems.includes(row['operationType'])">
                  {{ getAbsoluteValue(row.quantity) }}
                </div>
                <div v-else>
                </div>
              </template>
            </vxe-column>
            <vxe-column title="单位成本" field="unitPrice" align="center" width="100">
              <template #default="{ row }">
                <div v-if="outboundItems.includes(row['operationType'])">
                  {{ row.unitPrice }}
                </div>
                <div v-else>
                </div>
              </template>
            </vxe-column>
            <vxe-column title="成本" field="subtotal" align="center" width="100">
              <template #default="{ row }">
                <div v-if="outboundItems.includes(row['operationType'])">
                  {{ row.subtotal }}
                </div>
                <div v-else>
                </div>
              </template>
            </vxe-column>
          </vxe-colgroup>
          <vxe-colgroup title="结存" align="center">
            <vxe-column title="基本单位数量" field="currentQuantity" align="center" width="100"/>
            <vxe-column title="单位成本" field="averageCost" align="center" width="100"/>
            <vxe-column title="成本" field="totalCost" align="center" width="100"/>
          </vxe-colgroup>
        </vxe-table>
      </div>
      <div class="flex justify-between items-center pt-5px">
        <vxe-pager perfect @page-change="loadList(false)"
                   v-model:current-page="pagination.page"
                   v-model:page-size="pagination.pageSize"
                   :total="pagination.total"
                   :layouts="['PrevJump', 'PrevPage', 'Number', 'NextPage', 'NextJump', 'Sizes', 'Total']">
          <template #left>
            <!--          <span class="mr-12px text-16px">总金额：{{ amountTotal }}元</span>-->
            <vxe-button @click="loadList(false)" type="text" size="mini" icon="h-icon-refresh"
                        :loading="loading"></vxe-button>
          </template>
        </vxe-pager>
      </div>
      <div class="mt-10px"></div>
      <div class="filler-panel">
        <div class="filler-item" style="flex: 1; margin: 5px 0 !important">
          <label class="mr-16px w-80px">备注说明：</label>
          <Input placeholder="请输入备注" type="text" maxlength="150"
                 style="width: 80%"
                 v-model="form.remarks"/>
        </div>
      </div>
    </div>
    <div class="modal-column-right">
      <Button icon="fa fa-close" @click="$emit('close')" :loading="loading">
        取消
      </Button>
      <Button icon="fa fa-save" color="primary" @click="confirm" :loading="loading">
        保存
      </Button>
    </div>
  </div>
</template>
<script>
import manba from "manba";
import InventoryItem from "@js/api/inventory/InventoryItem";
import {mapMutations} from "vuex";
import {loading, message} from "heyui.ext";
import Product from "@js/api/basic/Product";
import Warehouse from "@js/api/basic/Warehouse";
import Supplier from "@js/api/basic/Supplier";
import FinanceVoucher from "@js/api/setting/FinanceVoucher";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-dd");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-dd");

export default {
  name: "VoucherForm",
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
        productIds: [],
        supplierIds: [],
        warehouseIds: [],
        operationTypes: [],
        state: null,
        sortCol: null,
        sort: null,
      },
      dateRange: {
        start: manba(startTime).format("YYYY-MM-dd"),
        end: manba(endTime).format("YYYY-MM-dd")
      },
      warehouseList: [],
      productList: [],
      supplierList: [],
      outboundItems: ["采购退货", "销售出库", "调拨出库", "盘亏出库", "其他出库"],
      inboundItems: ["采购入库", "销售退货", "调拨入库", "其他入库", "盘盈入库"],
      operationTypeList: {
        "采购入库": "采购入库",
        "销售退货": "销售退货",
        "调拨入库": "调拨入库",
        "其他入库": "其他入库",
        "盘盈入库": "盘盈入库",
        "采购退货": "采购退货",
        "销售出库": "销售出库",
        "调拨出库": "调拨出库",
        "盘亏出库": "盘亏出库",
        "其他出库": "其他出库",
      },
      form: {}
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
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    loadDict(callback) {
      loading("加载中....");
      Promise.all([Product.select(), Warehouse.select(), Supplier.select()])
          .then((results) => {
            this.productList = results[0].data || [];
            this.warehouseList = results[1].data || [];
            this.supplierList = results[2].data || [];
            callback();
          })
          .finally(() => loading.close());
    },
    loadList(type = true) {
      this.loading = true;
      const params = JSON.parse(JSON.stringify(this.queryParams));
      params.warehouseIds = params.warehouseIds.join(",");
      params.productIds = params.productIds.join(",");
      params.supplierIds = params.supplierIds.join(",");
      params.operationTypes = params.operationTypes.join(",");
      params.exclusion = true;
      InventoryItem.report(params).then(({data: {results, total}}) => {
        this.dataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
    getAbsoluteValue(number) {
      if (number < 0) {
        return -number;
      } else {
        return number;
      }
    },
    confirm() {
      const selectRecords = this.$refs.table.getCheckboxRecords();
      if (!selectRecords || selectRecords.length === 0) {
        message.warn("请选择要操作的数据~");
        return;
      }
      if (selectRecords.length > 1) {
        message.warn("请选择一条数据推送~");
        return;
      }
      const record = selectRecords[0];
      FinanceVoucher.save({
        orderId: record.id,
        productId: record.productId,
        customerId: record.customerId,
        supplierId: record.supplierId,
        amount: record.subtotal,
        type: record.operationType,
        orderTime: record.createdAt,
        remark: this.form.remark,
        orderName: record.productName,
      }).then((success) => {
        if (success) {
          message("推送成功~");
          this.$refs.table.clearCheckboxRow();
          this.loadList();
        }
      });
    }
  },
  created() {
    this.loadDict(() => {
    });
    this.loadList();
  }
}
</script>
