<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <vxe-table
                 ref="xTable"
                 size="mini"
                 border
                 :data="dataList"
                 highlight-hover-row
                 show-overflow
                 stripe
                 :row-config="{height: 40}"
                 :column-config="{resizable: true}"
                 :loading="loading">
        <vxe-column title="序号" type="seq" width="60" align="center" fixed="left"/>
        <vxe-column title="操作" field="seq" width="70" align="center" fixed="left" v-if="this.type === 'add'">
          <template #default="{row,rowIndex}">
            <div class="fa fa-plus text-hover mr-5px" @click="adjustRows('insert',rowIndex)"></div>
            <div class="fa fa-minus text-hover" v-if="isDeleting" @click="adjustRows('delete',rowIndex)"></div>
          </template>
        </vxe-column>
        <vxe-column field="productCode" title="产品编码" width="200">
          <template #default="{row}">
            {{ productList.find(item => item.id === row.productId)?.code || '-' }}
          </template>
        </vxe-column>
        <vxe-column field="productName" title="产品名称" min-width="200">
          <template #header>
            <span style="color: red">*</span>产品名称
          </template>
          <template #default="scope">
            <div class="input-group goodsSelect" @keyup.stop="void(0)">
              <t-select ref="ms" @change="selectProduct($event, scope.rowIndex)" :options="productList"
                        v-model="scope.row.productId"
                        :keys="{ value: 'id', label: 'name' }" filterable placeholder="输入编码/名称"
                        :clearable="false" :disabled="type === 'edit'"/>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="单位" field="unitName">
          <template #default="{row}">
            {{ getUnitName(row) }}
          </template>
        </vxe-column>
        <vxe-column title="规格型号" field="specification">
          <template #default="{row}">
            {{ productList.find(item => item.id === row.productId)?.specification || '-' }}
          </template>
        </vxe-column>
        <vxe-column title="仓库" field="warehouseId" align="center" width="180">
          <template #header>
            <span style="color: red">*</span>仓库
          </template>
          <template #default="{row,rowIndex}">
            <t-select :clearable="false" v-model="row.warehouseId" :options="warehouseList" filterable
                      :keys="{ value: 'id', label: 'name' }" :disabled="type === 'edit'"/>
          </template>
        </vxe-column>
        <vxe-column title="数量" field="quantity">
          <template #header>
            <span style="color: red">*</span>数量
          </template>
          <template #default="{row,rowIndex,columnIndex}">
            <vxe-input
                :id="'r'+rowIndex+''+3"
                @blur="updateQuantity(row)"
                ref="inputQuantity"
                v-model.number="row.quantity"
                type="float"
                min="0"
                :controls="false">
            </vxe-input>
          </template>
        </vxe-column>
        <vxe-column title="单价" field="unitPrice">
          <template #header>
            <span style="color: red">*</span>单价
          </template>
          <template #default="{row,rowIndex,}">
            <vxe-input
                :id="'r'+rowIndex+''+4"
                @blur="updatePrice(row)"
                ref="inputPrice"
                v-model.number="row.unitPrice"
                type="float"
                min="0"
                :controls="false">
            </vxe-input>
          </template>
        </vxe-column>
        <vxe-column title="金额" field="subtotal">
          <template #default="{row,rowIndex,}">
            <vxe-input ref="inputAmount"
                       v-model.number="row.subtotal"
                       type="float"
                       min="0"
                       :controls="false"
                       @blur="updateSubtotal(row)"
            >
            </vxe-input>
          </template>
        </vxe-column>
      </vxe-table>
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
import InventoryInitial from "@js/api/basic/InventoryInitial";
import {mapMutations, mapState} from "vuex";
import {LoadingPlugin, MessagePlugin} from "tdesign-vue-next";
import Warehouse from "@js/api/basic/Warehouse";
import Product from "@js/api/basic/Product";
import Unit from "@js/api/basic/Unit";


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
      this.compute(item);
      this.$refs.xTable.updateFooter();
    },

    updatePrice(item) {
      this.compute(item);
      this.$refs.xTable.updateFooter();
    },
    updateSubtotal(item){
      this.compute(item);
      this.$refs.xTable.updateFooter();
    },
    compute(item){
      //计算总价 = 数量 * 单价
      item.subtotal = (item.quantity * item.unitPrice).toFixed(2);
    },

    //选择产品
    selectProduct(item, index) {
      if(this.type === 'edit'){
        return
      }
      let g = {
        quantity: 0,
        unitPrice: 0,
        subtotal: 0,
        baseUnitId: item.unitId,
        unitName: item.unitName,
        productId: item.id,
        productCode: item.code,
        productName: item.name,
        specification: item.specification,
        operationType: this.params.operationType
      };
      this.dataList[index] = g;
      if (!this.dataList[index + 1]) {
        this.dataList.push({productId: null,quantity: null});
      }
      //强制更新视图
      this.$refs.xTable.loadData(this.dataList).then(() => {
        this.$nextTick(() => {
          let str = index + '' + 3
          let element = document.querySelector('#r' + str + ' input');
          setTimeout(() => {
            element.focus()
            element.select()
          }, 100);
        })
      });
      this.$forceUpdate();
    },//添加行或减少行
    adjustRows(type, index) {
      if (type === 'insert') {
        this.dataList.splice(index + 1, 0, {isNew: true});
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
        this.dataList[0] = data;
      }).finally(() => this.loading = false);
    },
    initForm(){
      for (let index = 0; index < 5; index++) {
        this.dataList.push({productId: null,quantity: null});
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
