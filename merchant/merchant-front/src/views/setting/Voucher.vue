<template>
  <div class="modal-column">
    <div class="frame-page flex flex-column">
      <div class="parent_container">
        <div class="left">
          <vxe-table
              border
              ref="documentTypeGridRef"
              size="mini"
              :data="documentTypeDataList"
              @radio-change="handleDocumentTypeChange"
              :rowConfig="{isCurrent: true,isHover: true}"
              :radio-config="{trigger: 'row',labelField: 'documentType',highlight: true}">
            <vxe-column field="documentType" title="单据类型"></vxe-column>
          </vxe-table>
        </div>

        <div class="right">
          <div class="table-container">
            <vxe-table row-id="id"
                       ref="table"
                       :data="dataList"
                       height="auto"
                       highlight-hover-row
                       show-overflow
                       show-footer
                       :row-config="{height: 48}"
                       :column-config="{resizable: true}"
                       :sort-config="{remote:true}"
                       :loading="loading">
              <!--<vxe-column type="checkbox" width="40" align="center"/>-->
              <vxe-column title="操作" align="center" width="150">
                <template #default="{row}">
                  <span v-if="!row.voucherCode" class="primary-color  text-hover ml-10px"
                        @click="pushVoucher(row)">推送</span>
                  <span v-else class="primary-color  text-hover ml-10px" @click="editVoucher(row,'edit')">编辑</span>
                  <span v-if="row.voucherCode" class="primary-color  text-hover ml-10px"
                        @click="editVoucher(row,'look')">查看</span>
                </template>
              </vxe-column>
              <vxe-column title="商品编号" field="productCode" align="center" width="130"/>
              <vxe-column title="商品名称" field="productName" width="200"/>
              <vxe-column title="商品类别" field="productCategoryName" width="200"/>
              <vxe-column title="规格型号" field="productSpecification" min-width="120"/>
              <vxe-column title="单位" field="unitName" width="120"/>
              <vxe-column title="仓库" field="warehouseName" width="120"/>
              <vxe-colgroup title="期初" align="center">
                <vxe-column title="数量" field="initialQuantity" align="center" width="100"/>
                <vxe-column title="成本" field="initialSubtotal" align="center" width="100"/>
              </vxe-colgroup>
              <vxe-colgroup title="采购入库" align="center">
                <vxe-column title="数量" field="purchaseStockQuantity" align="center" width="100"/>
                <vxe-column title="成本" field="purchaseStockSubtotal" align="center" width="100"/>
              </vxe-colgroup>
              <vxe-colgroup title="销售退货" align="center">
                <vxe-column title="数量" field="salesReturnsQuantity" align="center" width="100"/>
                <vxe-column title="成本" field="salesReturnsSubtotal" align="center" width="100"/>
              </vxe-colgroup>
              <vxe-colgroup title="调拨入库" align="center">
                <vxe-column title="数量" field="channelInQuantity" align="center" width="100"/>
                <vxe-column title="成本" field="channelInSubtotal" align="center" width="100"/>
              </vxe-colgroup>
              <vxe-colgroup title="其他入库" align="center">
                <vxe-column title="数量" field="otherInQuantity" align="center" width="100"/>
                <vxe-column title="成本" field="otherInSubtotal" align="center" width="100"/>
              </vxe-colgroup>
              <vxe-colgroup title="盘盈单" align="center">
                <vxe-column title="数量" field="takeProfitQuantity" align="center" width="100"/>
                <vxe-column title="成本" field="takeProfitSubtotal" align="center" width="100"/>
              </vxe-colgroup>
              <vxe-colgroup title="入库合计" align="center">
                <vxe-column title="数量" field="inQuantityTotal" align="center" width="100"/>
                <vxe-column title="成本" field="inSubtotalTotal" align="center" width="100"/>
              </vxe-colgroup>
              <vxe-colgroup title="采购退货" align="center">
                <vxe-column title="数量" field="purchaseReturnsQuantity" align="center" width="100"/>
                <vxe-column title="成本" field="purchaseReturnsSubtotal" align="center" width="100"/>
              </vxe-colgroup>
              <vxe-colgroup title="销售出库" align="center">
                <vxe-column title="数量" field="sellOutQuantity" align="center" width="100"/>
                <vxe-column title="成本" field="sellOutSubtotal" align="center" width="100"/>
              </vxe-colgroup>
              <vxe-colgroup title="调拨出库" align="center">
                <vxe-column title="数量" field="channelOutQuantity" align="center" width="100"/>
                <vxe-column title="成本" field="channelOutSubtotal" align="center" width="100"/>
              </vxe-colgroup>
              <vxe-colgroup title="其他出库" align="center">
                <vxe-column title="数量" field="otherOutQuantity" align="center" width="100"/>
                <vxe-column title="成本" field="otherOutSubtotal" align="center" width="100"/>
              </vxe-colgroup>
              <vxe-colgroup title="盘亏单" align="center">
                <vxe-column title="数量" field="taskDeficitQuantity" align="center" width="100"/>
                <vxe-column title="成本" field="taskDeficitSubtotal" align="center" width="100"/>
              </vxe-colgroup>
              <vxe-colgroup title="出库合计" align="center">
                <vxe-column title="数量" field="outQuantityTotal" align="center" width="100"/>
                <vxe-column title="成本" field="outSubtotalTotal" align="center" width="100"/>
              </vxe-colgroup>
              <vxe-colgroup title="成本调整" align="center">
                <vxe-column title="数量" field="costQuantity" align="center" width="100"/>
                <vxe-column title="成本" field="costSubtotal" align="center" width="100"/>
              </vxe-colgroup>
              <vxe-colgroup title="结存" align="center">
                <vxe-column title="数量" field="currentQuantity" align="center" width="100"/>
                <vxe-column title="成本" field="totalCost" align="center" width="100"/>
              </vxe-colgroup>

            </vxe-table>
          </div>
          <div class="flex justify-between items-center pt-5px">
            <vxe-pager perfect @page-change="loadList(false)"
                       style="width: 100%"
                       v-model:current-page="pagination.page"
                       v-model:page-size="pagination.pageSize"
                       :total="pagination.total"
                       :layouts="['PrevJump', 'PrevPage', 'Number', 'NextPage', 'NextJump', 'Sizes', 'Total']">
              <template #left>
                <vxe-button @click="loadList(false)" type="text" size="mini" icon="h-icon-refresh"
                            :loading="loading"></vxe-button>
              </template>
            </vxe-pager>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import manba from "manba";
import {layer} from "@layui/layer-vue";
import {h} from "vue";
import FinanceVoucher from "@js/api/setting/FinanceVoucher";
import InventoryItem from "@js/api/inventory/InventoryItem";
import {confirm, message} from "heyui.ext";
import VoucherForm from "./VoucherForm.vue";

const startTime = manba().startOf(manba.MONTH).format("YYYY-MM-dd");
const endTime = manba().endOf(manba.DAY).format("YYYY-MM-dd");

export default {
  name: "Voucher",
  data() {
    return {
      loading: false,
      documentTypeDataList: [
        {id: 1, documentType: '期初余额', type: 1},
        {id: 2, documentType: '采购入库', type: 1},
        {id: 3, documentType: '销售退货', type: 1},
        {id: 4, documentType: '其他入库', type: 1},
        {id: 5, documentType: '盘盈入库', type: 1},
        {id: 6, documentType: '调拨入库', type: 1},
        {id: 7, documentType: '采购退货', type: 1},
        {id: 8, documentType: '销售出库', type: 1},
        {id: 9, documentType: '其他出库', type: 1},
        {id: 10, documentType: '调拨出库', type: 1},
        {id: 11, documentType: '盘亏出库', type: 1},
        {id: 12, documentType: '成本调整', type: 1}
      ],
      dataList: [],
      params: {
        type: '期初余额',
      },
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      dataParams: {
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
    }
  },
  computed: {
    queryParams() {
      return Object.assign(this.dataParams, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        start: this.dateRange.start,
        end: this.dateRange.end,
      })
    },
  },
  methods: {
    showForm(id) {
      console.info("showForm:", id)
      let layerId = layer.open({
        title: "选择生成凭证",
        offset: ['50px', 'auto'],
        shadeClose: false,
        area: ['90%', '600px'],
        content: h(VoucherForm, {
          id,
          onClose: () => {
            this.loadList();
            layer.close(layerId);
          },
          onSuccess: () => {
            this.loadList();
            layer.close(layerId);
          }
        })
      });
    },
    loadList() {
      this.loading = true;
      const params = JSON.parse(JSON.stringify(this.queryParams));
      params.warehouseIds = params.warehouseIds.join(",");
      params.productIds = params.productIds.join(",");
      params.supplierIds = params.supplierIds.join(",");
      params.operationTypes = this.params.type;
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
    doRemove(id) {
      confirm({
        title: "系统提示",
        content: `是否删除当前数据?`,
        onConfirm: () => {
          FinanceVoucher.delete(id).then(({data}) => {
            message.success("操作成功～");
            this.loadList();
          });
        },
      });
    },
    handleDocumentTypeChange(data) {
      // 单选框变化时的处理函数
      this.params.type = data.row.documentType;
      this.loadList();

    },
    selectFirstDocumentType() {
      // 默认选中第一个单据类型
      const table = this.$refs.documentTypeGridRef;
      table.setRadioRow(this.documentTypeDataList[0]);
    },
    pushVoucher(record) {
      FinanceVoucher.save({
        orderId: record.id,
        productId: record.productId,
        customerId: record.customerId,
        supplierId: record.supplierId,
        amount: record.subtotal,
        type: record.operationType,
        orderTime: record.createdAt,
        orderName: record.productName,
      }).then((success) => {
        if (success) {
          message("推送成功~");
          this.loadList();
        }
      });
    },
    editVoucher(row, type) {
      let layerId = layer.open({
        title: "编辑凭证",
        offset: 't',
        shadeClose: false,
        area: ['90%', '700px'],
        content: h(VoucherForm, {
          voucherId: row.voucherId,
          type: type,
          onClose: () => {
            this.loadList();
            layer.close(layerId);
          },
          onSuccess: () => {
            this.loadList();
            layer.close(layerId);
          }
        })
      });
    }
  },
  created() {

  },
  mounted() {
    // 默认选中第一个单据类型
    this.selectFirstDocumentType();
    this.loadList();
  },
}
</script>

<script setup>


</script>

<style lang="less" scoped>

.parent_container {
  display: flex;
  height: 100%;
}

.left {
  width: 300px; /* 固定宽度 */
  padding: 20px;
}

.right {
  flex: 1; /* 占用剩余空间 */
  padding: 20px;
  width: calc(100% - 380px);
}

.table-container {
  height: calc(100% - 50px);
}

.selected {
  background-color: #dddddd;
}

</style>