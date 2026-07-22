<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <t-table
          ref="xTable"
          row-key="_rowKey"
          size="small"
          bordered
          stripe
          hover
          table-layout="fixed"
          :data="dataList"
          :columns="columns"
          :loading="loading"
      >
        <template #ops="{ rowIndex }">
          <div class="fa fa-plus text-hover mr-5px" @click="adjustRows('insert', rowIndex)"></div>
          <div class="fa fa-minus text-hover" v-if="isDeleting" @click="adjustRows('delete', rowIndex)"></div>
        </template>
        <template #supplierCode="{ row }">
          {{ supplierList.find(item => item.id === row.supplierId)?.code || '-' }}
        </template>
        <template #supplierName="{ row, rowIndex }">
          <div class="input-group goodsSelect" @keyup.stop="void(0)">
            <t-select ref="ms" @change="selectCustomer($event, rowIndex)" :options="supplierList"
                      v-model="row.supplierId"
                      :keys="{ value: 'id', label: 'name' }" filterable placeholder="输入编码/名称"
                      :clearable="false" :disabled="type === 'edit'"/>
          </div>
        </template>
        <template #balanceBefore="{ row, rowIndex }">
          <t-input-number
              :id="'r' + rowIndex + '3'"
              v-model="row.balanceBefore"
              theme="normal"
              :min="0"
              :decimal-places="2"
              style="width: 100%"
              @blur="updateQuantity(row)"
          />
        </template>
        <template #amount="{ row, rowIndex }">
          <t-input-number
              :id="'r' + rowIndex + '4'"
              v-model="row.amount"
              theme="normal"
              :min="0"
              :decimal-places="2"
              style="width: 100%"
              @blur="updatePrice(row)"
          />
        </template>
        <template #balanceAfter="{ row }">
          <t-input-number
              v-model="row.balanceAfter"
              theme="normal"
              :min="0"
              :decimal-places="2"
              style="width: 100%"
              @blur="updateSubtotal(row)"
          />
        </template>
      </t-table>
    </div>
    <div class="page-column-footer modal-column-between bg-white-color border">
      <t-button @click="closeWindow" :loading="loading">取消</t-button>
      <div>
        <t-button theme="primary" @click="save" :loading="loading">保存</t-button>
      </div>
    </div>
  </div>
</template>

<script>
import {mapMutations, mapState} from "vuex";
import {LoadingPlugin, MessagePlugin} from "tdesign-vue-next";
import Supplier from "@js/api/basic/Supplier";
import SupplierInitial from "@js/api/basic/SupplierInitial";

let rowSeq = 0;
function newRow(extra = {}) {
  return { _rowKey: `r-${++rowSeq}`, supplierId: null, ...extra };
}

export default {
  name: "SupplierInitialForm",
  props: {
    supplierInitialId: [String, Number],
    type: String,
    index: Number
  },
  computed: {
    ...mapState(['accountBook']),
    isDeleting() {
      return this.dataList.length > 1;
    },
    columns() {
      const cols = [
        {
          colKey: 'seq',
          title: '序号',
          width: 60,
          align: 'center',
          fixed: 'left',
          cell: (h, {rowIndex}) => rowIndex + 1
        },
      ];
      if (this.type === 'add') {
        cols.push({
          colKey: 'ops',
          title: '操作',
          width: 70,
          align: 'center',
          fixed: 'left'
        });
      }
      cols.push(
        {colKey: 'supplierCode', title: '货商编码', width: 200},
        {colKey: 'supplierName', title: '货商名称', minWidth: 200},
        {colKey: 'balanceBefore', title: '期初应付款'},
        {colKey: 'amount', title: '期初预付款'},
        {colKey: 'balanceAfter', title: '期初余额'},
      );
      return cols;
    }
  },
  data() {
    return {
      loading: false,
      dataList: [],
      params: {
        supplierFlowType:'期初',
        filter: null,
      },
      supplierList: [],
    }
  },
  methods: {
    ...mapMutations(['closeSelfTab', 'pushTab']),

    updateQuantity(item) {
      this.compute(item);
    },

    updatePrice(item) {
      this.compute(item);
    },
    updateSubtotal(item){
      this.compute(item);
    },
    compute(item){
      item.balanceAfter = (item.balanceBefore - item.amount).toFixed(2);
    },

    //选择产品
    selectCustomer(item, index) {
      if(this.type === 'edit'){
        return
      }
      const selected = this.supplierList.find(c => c.id === item) || item;
      let g = newRow({
        balanceBefore: 0,
        amount: 0,
        balanceAfter: 0,
        supplierId: selected.id,
        supplierCode: selected.code,
        supplierName: selected.name,
        supplierFlowType: this.params.supplierFlowType
      });
      this.dataList[index] = g;
      if (!this.dataList[index + 1]) {
        this.dataList.push(newRow());
      }
      this.$nextTick(() => {
        let str = index + '' + 3
        let element = document.querySelector('#r' + str + ' input');
        setTimeout(() => {
          if (element) {
            element.focus()
            element.select()
          }
        }, 100);
      });
      this.$forceUpdate();
    },//添加行或减少行
    adjustRows(type, index) {
      if (type === 'insert') {
        this.dataList.splice(index + 1, 0, newRow({isNew: true}));
      } else {
        this.dataList.splice(index, 1);
      }
    },

    save(){
      let requestData = {
        supplierFlowList: this.dataList,
      };
      //移除掉productId 为null的数据
      requestData.supplierFlowList = requestData.supplierFlowList.filter(item => item.supplierId);
      //校验
      if (!this.checkHttp(requestData.supplierFlowList)) {
        return
      }
      this.loading = true;
      SupplierInitial.batch(requestData).then(({data}) => {
        this.dataList = data;
        this.closeWindow();
      }).finally(() => this.loading = false);
    },

    checkHttp(requestData) {
      if (requestData.length === 0) {
        MessagePlugin.error("请选择货商");
        return false
      }
      let quantityFlag = false
      let unitPriceFlag = false
      let subtotalFlag = false
      let customerFlag = false
      requestData.map(item => {
        if (item.balanceBefore === 0 || !item.balanceBefore) {
          quantityFlag = true
        }
        if (item.amount === 0 || !item.amount) {
          unitPriceFlag = true
        }
        if (item.balanceAfter === 0 || !item.balanceAfter) {
          subtotalFlag = true
        }
        if (!item.supplierId) {
          customerFlag = true
        }
      })
      if (customerFlag) {
        MessagePlugin.error("请选择货商~");
        return false
      }
      if (quantityFlag) {
        MessagePlugin.error("期初应付款为空~");
        return false
      }
      if (unitPriceFlag) {
        MessagePlugin.error("期初预付款为空~");
        return false
      }
      if (subtotalFlag) {
        MessagePlugin.error("期初余额为空~");
        return false
      }
      return true
    },

    closeWindow() {
      this.closeSelfTab(this.index);
      this.pushTab({
        keepAlive: false,
        key: "SupplierInitialList",
      });
    },
    editForm(){
      SupplierInitial.load(this.supplierInitialId).then(({data}) => {
        this.dataList[0] = newRow(data);
      }).finally(() => this.loading = false);
    },
    initForm(){
      for (let index = 0; index < 5; index++) {
        this.dataList.push(newRow());
      }
    },
  },
  created() {
    //LoadingPlugin(true);
    if (this.supplierInitialId) {
      this.editForm();
    }else{
      this.initForm();
    }

    Promise.all([
      Supplier.select(),
    ]).then((results) => {
      this.supplierList = results[0].data || [];
      this.supplierList.forEach(item => {
        item.name = `${item.code}--${item.name}`;
      });
    }).finally(() => LoadingPlugin(false));
  }
}
</script>
