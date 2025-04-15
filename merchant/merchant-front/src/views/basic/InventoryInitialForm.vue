<template>
  <div class="frame-page flex flex-column">
    <div class="flex1">
      <vxe-table
                 ref="xTable"
                 :data="dataList"
                 highlight-hover-row
                 show-overflow
                 stripe
                 :row-config="{height: 48}"
                 :column-config="{resizable: true}"
                 :loading="loading">
        <vxe-column title="序号" type="seq" width="60" align="center" fixed="left"/>
        <vxe-column title="操作" field="seq" width="70" align="center" fixed="left" v-if="this.type === 'add'">
          <template #default="{row,rowIndex}">
            <div class="fa fa-plus text-hover mr-5px" @click="adjustRows('insert',rowIndex)"></div>
            <div class="fa fa-minus text-hover" v-if="isDeleting" @click="adjustRows('delete',rowIndex)"></div>
          </template>
        </vxe-column>
        <vxe-column field="productCode" title="商品编码" width="200">
          <template #default="{row}">
            {{ productList.find(item => item.id === row.productId)?.code || '-' }}
          </template>
        </vxe-column>
        <vxe-column field="productName" title="商品名称" min-width="200">
          <template #default="scope">
            <div class="h-input-group goodsSelect" @keyup.stop="void(0)">
              <Select ref="ms" @change="selectProduct($event,scope.rowIndex)" :datas="productList" v-model="scope.row.productId"
                      keyName="id" titleName="name" filterable placeholder="输入编码/名称" :deletable="false">
                <template v-slot:item="{ item }">
                  <div>{{ item.name }}</div>
                </template>
              </Select>
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
          <template #default="{row,rowIndex}">
            <Select :deletable="false" v-model="row.warehouseId" :datas="warehouseList" filterable keyName="id"
                    titleName="name"
            />
          </template>
        </vxe-column>
        <vxe-column title="数量" field="quantity">
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
    <div class="modal-column-between bg-white-color border">
      <Button @click="closeWindow" :loading="loading"> 取消</Button>
      <div>
        <Button color="primary" @click="save" :loading="loading"> 保存</Button>
      </div>
    </div>
  </div>
</template>

<script>
import InventoryInitial from "@js/api/basic/InventoryInitial";
import {mapMutations, mapState} from "vuex";
import {confirm, loading, message} from "heyui.ext";
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

    //选择商品
    selectProduct(item, index) {
      console.log("item",item)
      console.log("index",index)
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
      console.log("this.dataList",this.dataList);
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
      this.loading = true;

      let requestData = {
        inventoryItemList: this.dataList,
      };
      //移除掉productId 为null的数据
      requestData.inventoryItemList = requestData.inventoryItemList.filter(item => item.productId);
      InventoryInitial.batchSave(requestData).then(({data}) => {
        this.dataList = data;
        console.log("data",data)
        this.closeWindow();
      }).finally(() => this.loading = false);
    },

    closeWindow() {
      this.closeSelfTab(this.index);
      this.pushTab({
        keepAlive: false,
        key: "InventoryInitialList",
      });
    },
    editForm(){
      InventoryInitial.getInfo(this.inventoryInitialId).then(({data}) => {
        this.dataList[0] = data;
        console.log("data",data)
      }).finally(() => this.loading = false);
    },
    initForm(){
      for (let index = 0; index < 5; index++) {
        this.dataList.push({productId: null,quantity: null});
      }
    },
  },
  created() {
    //loading("加载中....");
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
    }).finally(() => loading.close());
  }
}
</script>
