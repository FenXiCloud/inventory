<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <div class="form-toolbar">
        <div class="form-toolbar__left">
          <label class="mr-20px ml-16px" style="font-size: 16px !important">单据类型：</label>
          <t-select class="w-140px" :options="typeList"
                    :keys="{ value: 'id', label: 'name' }"
                    :clearable="false" v-model="form.orderType" :disabled="isLocked"
                    placeholder="请选择类型"/>
          <label class="mr-20px ml-20px" style="font-size: 16px !important">{{ mainProductLabel }}：</label>
          <t-select class="w-260px" filterable :options="productList"
                    :keys="{ value: 'id', label: 'customName' }"
                    v-model="form.productId" placeholder="输入编码/名称" :disabled="isLocked" clearable/>
          <label class="mr-20px ml-20px" style="font-size: 16px !important">数量：</label>
          <t-input-number class="w-140px" v-model="form.quantity" :min="0" :decimal-places="0" :disabled="isLocked"/> <!-- 装配成品数量按整件，恒0位；装配单明细行数量随账套参数 -->
          <label class="mr-20px ml-20px" style="font-size: 16px !important">仓库：</label>
          <t-select class="w-178px" filterable :options="warehouseList"
                    :keys="{ value: 'id', label: 'name' }"
                    v-model="form.warehouseId" placeholder="请选择仓库" :disabled="isLocked" clearable/>
        </div>
        <Stamp v-if="isAudited"/>
      </div>

      <div class="section-title">组件明细</div>
      <t-table
          ref="xTable"
          row-key="_rowKey"
          size="small"
          bordered
          stripe
          hover
          table-layout="fixed"
          :columns="columns"
          :data="itemData"
      >
        <template #ops="{ rowIndex }">
          <template v-if="!isLocked">
            <div class="fa fa-plus text-hover mr-5px" @click="adjustRows('insert', rowIndex)"></div>
            <div v-if="itemData.length !== 1" class="fa fa-minus text-hover-danger"
                 @click="adjustRows('delete', rowIndex)"></div>
          </template>
          <template v-else>{{ rowIndex + 1 }}</template>
        </template>
        <template #productName="{ row, rowIndex }">
          <div class="input-group goodsSelect" v-if="!isLocked">
            <t-select :clearable="false" v-model="row.productId" :options="productList" filterable
                      placeholder="输入编码/名称" :keys="{ value: 'id', label: 'customName' }"
                      @change="(e) => changeRow(rowIndex, e)"/>
          </div>
          <div v-else class="flex">
            <div class="flex1 ml-8px">
              <div>{{ row.productCode }}--{{ row.productName }}</div>
            </div>
          </div>
        </template>
        <template #quantity="{ row }">
          <t-input-number
              v-if="!isLocked"
              v-model="row.quantity"
              theme="normal"
              :min="0"
              :decimal-places="qtyDp"
              style="width: 100%"
          />
          <div v-else class="flex">
            <div class="flex1 ml-8px">
              <div>{{ row.quantity }}</div>
            </div>
          </div>
        </template>
      </t-table>

      <div class="mt-10px"></div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px w-80px">备注说明：</label>
          <t-input :disabled="isLocked" placeholder="请输入备注" maxlength="150"
                   style="width: 80%"
                   v-model="form.remarks"/>
        </div>
      </div>
    </div>

    <div class="page-column-footer modal-column-between bg-white-color border">
      <t-button @click="closeWindow" :loading="loading"> 取消</t-button>
      <div>
        <t-button @click="saveOrder()" v-if="!isLocked" v-auth="'assemblyOrder:edit'"
                  :loading="loading"> 保存
        </t-button>
        <t-button v-if="$can('assemblyOrder:audit') && form.id && !isLocked" theme="primary" @click="approved()" :loading="loading"> 审核</t-button>
        <t-button v-if="$can('assemblyOrder:audit') && isAudited && !looked" @click="backApproved()" :loading="loading"> 反审核</t-button>
      </div>
    </div>
  </div>
</template>
<script>
import {LoadingPlugin, MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';
import Product from "@js/api/basic/Product";
import Warehouse from "@js/api/basic/Warehouse";
import AssemblyOrder from "@js/api/inventory/AssemblyOrder";
import {mapMutations, mapState} from "vuex";
import Stamp from "../common/Stamp.vue";

let rowSeq = 0;
function newRow(extra = {}) {
  return {_rowKey: `r-${++rowSeq}`, productId: null, quantity: null, productCode: '', productName: '', productSpecification: '', unitName: '', ...extra};
}

export default {
  name: "AssemblyOrderForm",
  components: {Stamp},
  data() {
    return {
      loading: false,
      productList: [],
      warehouseList: [],
      typeList: [{id: '组装', name: '组装'}, {id: '拆卸', name: '拆卸'}],
      form: {
        id: null,
        orderType: '组装',
        productId: null,
        quantity: null,
        warehouseId: null,
        remarks: null,
        orderStatus: '已保存'
      },
      itemData: [],
      assemblyOrderId: null,
      type: null
    };
  },
  computed: {
    ...mapState(["user"]),
    isAudited() {
      return this.form.orderStatus === '已审核';
    },
    looked() {
      return this.type === 'look';
    },
    isLocked() {
      return this.isAudited || this.looked;
    },
    mainProductLabel() {
      return this.form.orderType === '拆卸' ? '被拆品' : '成品';
    },
    columns() {
      return [
        {
          colKey: 'ops',
          title: '操作',
          width: 70,
          align: 'center',
          fixed: 'left'
        },
        {colKey: 'productCode', title: '组件编码', width: 130},
        {colKey: 'productName', title: '组件名称', minWidth: 260},
        {colKey: 'productSpecification', title: '规格型号', align: 'center', width: 100},
        {colKey: 'unitName', title: '单位', width: 80},
        {colKey: 'quantity', title: '数量', width: 130},
      ];
    }
  },
  methods: {
    ...mapMutations(['closeSelfTab', 'pushTab']),
    changeRow(rowIndex, selected) {
      let value = (selected && typeof selected === 'object') ? (selected.id ?? selected.productId) : selected;
      this.itemData[rowIndex].productId = value;
      const p = (this.productList || []).find(x => x.id === value || x.productId === value);
      if (p) {
        this.itemData[rowIndex].productCode = p.code;
        this.itemData[rowIndex].productName = p.name;
        this.itemData[rowIndex].productSpecification = p.specification;
        this.itemData[rowIndex].unitName = p.unitName;
      }
    },
    adjustRows(type, index) {
      if (type === "insert") {
        this.itemData.splice(index + 1, 0, newRow());
      } else {
        this.itemData.splice(index, 1);
      }
    },
    isEmpty(value) {
      return (value !== 0 && !value) || value === '';
    },
    validators() {
      if (this.isEmpty(this.form.orderType)) {
        MessagePlugin.error("请选择单据类型~");
        return false;
      }
      if (this.isEmpty(this.form.productId)) {
        MessagePlugin.error("请选择成品/被拆品~");
        return false;
      }
      if (this.isEmpty(this.form.quantity) || Number(this.form.quantity) <= 0) {
        MessagePlugin.error("请填写数量~");
        return false;
      }
      if (this.isEmpty(this.form.warehouseId)) {
        MessagePlugin.error("请选择仓库~");
        return false;
      }
      const items = this.itemData.filter(item => !this.isEmpty(item.productId) || !this.isEmpty(item.quantity));
      if (items.length === 0) {
        MessagePlugin.error("请至少添加一条组件明细~");
        return false;
      }
      if (items.some(c => this.isEmpty(c.productId))) {
        MessagePlugin.error("组件明细存在未选择产品的行~");
        return false;
      }
      if (items.some(c => this.isEmpty(c.quantity) || Number(c.quantity) <= 0)) {
        MessagePlugin.error("组件明细存在未填写数量的行~");
        return false;
      }
      return true;
    },
    saveOrder() {
      if (!this.validators()) {
        return;
      }
      const assemblyOrderItemList = this.itemData
          .filter(item => !this.isEmpty(item.productId))
          .map(item => ({
            id: item.id || null,
            productId: item.productId,
            quantity: item.quantity
          }));
      const params = {
        assemblyOrder: {
          id: this.form.id,
          orderType: this.form.orderType,
          productId: this.form.productId,
          quantity: this.form.quantity,
          warehouseId: this.form.warehouseId,
          remarks: this.form.remarks
        },
        assemblyOrderItemList
      };
      LoadingPlugin(true);
      AssemblyOrder.save(params)
          .then(({success}) => {
            if (success) {
              MessagePlugin.success("保存成功~");
              this.closeWindow();
            }
          })
          .finally(() => LoadingPlugin(false));
    },
    approved() {
      DialogPlugin.confirm({
        header: "审核提示",
        body: `确认审核该单据?`,
        onConfirm: () => {
          return AssemblyOrder.approved('已审核', [this.form.id]).then(() => {
            MessagePlugin.success("操作成功~");
            this.closeWindow();
          });
        }
      });
    },
    backApproved() {
      DialogPlugin.confirm({
        header: "反审核提示",
        body: `确认反审核该单据?`,
        onConfirm: () => {
          return AssemblyOrder.approved('已保存', [this.form.id]).then(() => {
            MessagePlugin.success("操作成功~");
            this.closeWindow();
          });
        }
      });
    },
    loadEditForm() {
      this.itemData = [];
      AssemblyOrder.load(this.assemblyOrderId).then(({data}) => {
        if (!data || !data.assemblyOrder) {
          MessagePlugin.error("单据不存在~");
          return;
        }
        const order = data.assemblyOrder;
        this.form.id = order.id;
        this.form.orderType = order.orderType;
        this.form.productId = order.productId;
        this.form.quantity = order.quantity;
        this.form.warehouseId = order.warehouseId;
        this.form.remarks = order.remarks;
        this.form.orderStatus = order.orderStatus;
        (data.assemblyOrderItemList || []).forEach(item => {
          this.itemData.push(newRow({
            id: item.id,
            productId: item.productId,
            quantity: item.quantity,
            productCode: item.productCode,
            productName: item.productName,
            productSpecification: item.productSpecification,
            unitName: item.unitName
          }));
        });
        if (this.itemData.length === 0) {
          this.itemData.push(newRow());
        }
      });
    },
    loadDict(callback) {
      Promise.all([Product.select(), Warehouse.select()])
          .then((results) => {
            this.productList = results[0].data || [];
            this.productList.forEach(item => {
              item.customName = `${item.code}--${item.name}`;
            });
            this.warehouseList = results[1].data || [];
            if (callback) {
              callback();
            }
          })
          .finally(() => LoadingPlugin(false));
    },
    initForm() {
      this.itemData = [newRow()];
      this.form.id = null;
    },
    closeWindow() {
      const currentIndex = this.$store.state.tabs.findIndex(tab => tab.key === this.$store.state.currentTab);
      if (currentIndex !== -1) {
        this.closeSelfTab(currentIndex);
      }
      this.pushTab({
        key: "AssemblyOrderList",
        title: "组装拆卸单"
      });
    },
  },
  created() {
    const tabData = this.$store.state.currentTabData;
    this.$store.commit('SET_TAB_DATA', null);
    this.type = tabData?.type;
    this.assemblyOrderId = tabData?.assemblyOrderId;
    LoadingPlugin(true);
    this.loadDict(() => {
      if (this.assemblyOrderId) {
        this.loadEditForm();
        return;
      }
      this.initForm();
    });
  },
};
</script>
<style lang="less" scoped>
.section-title {
  margin: 12px 16px;
  font-size: 15px;
  font-weight: 600;
}
</style>
