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
        <template #productCode="{ row }">
          {{ productList.find(item => item.id === row.productId)?.code || '-' }}
        </template>
        <template #productName="{ row, rowIndex }">
          <div class="input-group goodsSelect" @keyup.stop="void(0)">
            <t-select ref="ms" @change="selectProduct($event, rowIndex)" :options="productList"
                      v-model="row.productId"
                      :keys="{ value: 'id', label: 'name' }" filterable placeholder="输入编码/名称"
                      :clearable="false" :disabled="type === 'edit'"/>
          </div>
        </template>
        <template #unitName="{ row }">
          {{ getUnitName(row) }}
        </template>
        <template #specification="{ row }">
          {{ productList.find(item => item.id === row.productId)?.specification || '-' }}
        </template>
        <template #warehouseId="{ row }">
          <t-select :clearable="false" v-model="row.warehouseId" :options="warehouseList" filterable
                    :keys="{ value: 'id', label: 'name' }" :disabled="type === 'edit'"/>
        </template>
        <template #quantity="{ row, rowIndex }">
          <t-input-number
              :id="'r' + rowIndex + '3'"
              v-model="row.quantity"
              theme="normal"
              :min="0"
              :decimal-places="qtyDp"
              style="width: 100%"
              @blur="updateQuantity(row)"
          />
        </template>
        <template #unitPrice="{ row, rowIndex }">
          <t-input-number
              :id="'r' + rowIndex + '4'"
              v-model="row.unitPrice"
              theme="normal"
              :min="0"
              :decimal-places="priceDp"
              style="width: 100%"
              @blur="updatePrice(row)"
          />
        </template>
        <template #subtotal="{ row }">
          <t-input-number
              v-model="row.subtotal"
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
        <t-button v-auth="'inventoryInitial:edit'" theme="primary" @click="save" :loading="loading">保存</t-button>
      </div>
    </div>
  </div>
</template>

<script>
import InventoryInitial from "@js/api/basic/InventoryInitial";
import {mapMutations, mapState} from "vuex";
import {LoadingPlugin, MessagePlugin} from "tdesign-vue-next";
import Warehouse from "@js/api/basic/Warehouse";
import Product from "@js/api/basic/Product";
import Unit from "@js/api/basic/Unit";

let rowSeq = 0;
function round2(n) {
  return Number((n).toFixed(2));
}
function newRow(extra = {}) {
  return { _rowKey: `r-${++rowSeq}`, productId: null, quantity: null, ...extra };
}

function requiredTitle(label) {
  return (h) => [
    h('span', {style: {color: 'red'}}, '*'),
    label
  ];
}

export default {
  name: "InventoryInitialForm",
  props: {
    inventoryInitialId: [String, Number],
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
        {colKey: 'productCode', title: '产品编码', width: 200},
        {colKey: 'productName', title: requiredTitle('产品名称'), minWidth: 200},
        {colKey: 'unitName', title: '单位'},
        {colKey: 'specification', title: '规格型号'},
        {colKey: 'warehouseId', title: requiredTitle('仓库'), align: 'center', width: 180},
        {colKey: 'quantity', title: requiredTitle('数量')},
        {colKey: 'unitPrice', title: requiredTitle('单价')},
        {colKey: 'subtotal', title: '金额'},
      );
      return cols;
    }
  },
  data() {
    return {
      loading: false,
      dataList: [],
      params: {
        filter: null,
        operationType:'期初库存'
      },
      pagination: {
        page: 1,
        size: 20,
        total: 0
      },
      productList: [],
      warehouseList: [],
      unitList: [],
    }
  },
  methods: {
    ...mapMutations(['closeSelfTab', 'pushTab']),

    getUnitName(row) {
      const product = this.productList.find(item => item.id === row.productId);
      if (product?.unitId) {
        const unit = this.unitList.find(item => item.id === product.unitId);
        return unit?.name || '-';
      } else {
        const unit = this.unitList.find(item => item.id === row.unitId);
        return unit?.name || '-';
      }
    },

    updateQuantity(item) {
      //改数量
      const qty = Number(item.quantity) || 0;
      const price = Number(item.unitPrice) || 0;
      const amount = Number(item.subtotal) || 0;
      if (price > 0 && amount > 0 && Math.abs(price * qty - amount) > 0.009) {
        //金额是手工录入(与 数量×单价 不一致)：保持金额权威，反算单价 = 金额 / 数量
        item.unitPrice = qty > 0 ? round2(amount / qty) : 0;
      } else {
        //常规：单价权威，金额 = 数量 × 单价
        this.forwardByPrice(item);
      }
    },

    updatePrice(item) {
      //改单价：金额 = 数量 × 单价
      this.forwardByPrice(item);
    },
    updateSubtotal(item) {
      //改金额：以金额为准反算单价 = 金额 / 数量(数量>0时)
      item.subtotal = round2(Number(item.subtotal) || 0);
      const qty = Number(item.quantity) || 0;
      const amount = Number(item.subtotal) || 0;
      item.unitPrice = qty > 0 ? round2(amount / qty) : 0;
    },
    forwardByPrice(item) {
      const qty = Number(item.quantity) || 0;
      const price = Number(item.unitPrice) || 0;
      item.subtotal = round2(qty * price);
    },

    //选择产品
    selectProduct(item, index) {
      if(this.type === 'edit'){
        return
      }
      const selected = this.productList.find(c => c.id === item) || item;
      let g = newRow({
        quantity: 0,
        unitPrice: 0,
        subtotal: 0,
        baseUnitId: selected.unitId,
        unitName: selected.unitName,
        productId: selected.id,
        productCode: selected.code,
        productName: selected.name,
        specification: selected.specification,
        operationType: this.params.operationType
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
        inventoryItemList: this.dataList,
      };
      //移除掉productId 为null的数据
      requestData.inventoryItemList = requestData.inventoryItemList.filter(item => item.productId);
      //校验
      if (!this.checkHttp(requestData.inventoryItemList)) {
        return
      }
      this.loading = true;
      InventoryInitial.batch(requestData).then(({data}) => {
        this.dataList = data;
        this.closeWindow();
      }).finally(() => this.loading = false);
    },

    checkHttp(requestData) {
      if (requestData.length === 0) {
        MessagePlugin.error("请选择产品~");
        return false
      }
      let quantityFlag = false
      let unitPriceFlag = false
      let subtotalFlag = false
      let warehouseFlag = false
      let productFlag = false
      requestData.map(item => {
        if (item.quantity === 0 || !item.quantity) {
          quantityFlag = true
        }
        if (item.unitPrice === 0 || !item.unitPrice) {
          unitPriceFlag = true
        }
        if (item.subtotal === 0 || !item.subtotal) {
          subtotalFlag = true
        }
        if (!item.warehouseId) {
          warehouseFlag = true
        }
        if (!item.productId) {
          productFlag = true
        }
      })
      if (productFlag) {
        MessagePlugin.error("请选择产品~");
        return false
      }
      if (warehouseFlag) {
        MessagePlugin.error("请选择仓库~");
        return false
      }
      if (quantityFlag) {
        MessagePlugin.error("请填写数量~");
        return false
      }
      if (unitPriceFlag) {
        MessagePlugin.error("请填写单价~");
        return false
      }
      if (subtotalFlag) {
        MessagePlugin.error("金额不能为空~");
        return false
      }
      return true
    },

    closeWindow() {
      this.closeSelfTab(this.index);
      this.pushTab({
        keepAlive: false,
        key: "InventoryInitialList",
      });
    },
    editForm(){
      InventoryInitial.load(this.inventoryInitialId).then(({data}) => {
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
    if (this.inventoryInitialId) {
      this.editForm();
    }else{
      this.initForm();
    }

    Promise.all([
      Product.select(),
      Warehouse.select(),
      Unit.select(),
    ]).then((results) => {
      this.productList = results[0].data || [];
      this.warehouseList = results[1].data || [];
      this.unitList = results[2].data || [];
      this.productList.forEach(item => {
        item.name = `${item.code}--${item.name}`;
      });
    }).finally(() => LoadingPlugin(false));
  }
}
</script>
