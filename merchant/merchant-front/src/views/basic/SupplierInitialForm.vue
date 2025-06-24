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
        <vxe-column field="customerCode" title="货商编码" width="200">
          <template #default="{row}">
            {{ supplierList.find(item => item.id === row.supplierId)?.code || '-' }}
          </template>
        </vxe-column>
        <vxe-column field="productName" title="货商名称" min-width="200">
          <template #default="scope">
            <div class="h-input-group goodsSelect" @keyup.stop="void(0)">
              <Select ref="ms" @change="selectCustomer($event,scope.rowIndex)" :datas="supplierList" v-model="scope.row.supplierId"
                      keyName="id" titleName="name" filterable placeholder="输入编码/名称" :deletable="false" :disabled="this.type === 'edit'">
                <template v-slot:item="{ item }">
                  <div>{{ item.name }}</div>
                </template>
              </Select>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="期初应收款" field="balanceBefore">
          <template #default="{row,rowIndex,columnIndex}">
            <vxe-input
                :id="'r'+rowIndex+''+3"
                @blur="updateQuantity(row)"
                ref="inputQuantity"
                v-model.number="row.balanceBefore"
                type="float"
                min="0"
                :controls="false">
            </vxe-input>
          </template>
        </vxe-column>
        <vxe-column title="期初预收款" field="amount">
          <template #default="{row,rowIndex,}">
            <vxe-input
                :id="'r'+rowIndex+''+4"
                @blur="updatePrice(row)"
                ref="inputPrice"
                v-model.number="row.amount"
                type="float"
                min="0"
                :controls="false">
            </vxe-input>
          </template>
        </vxe-column>
        <vxe-column title="期初余额" field="balanceAfter">
          <template #default="{row,rowIndex,}">
            <vxe-input ref="inputAmount"
                       v-model.number="row.balanceAfter"
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
import {mapMutations, mapState} from "vuex";
import {loading, message} from "heyui.ext";
import Supplier from "@js/api/basic/Supplier";
import SupplierInitial from "@js/api/basic/SupplierInitial";


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
      item.balanceAfter = (item.balanceBefore - item.amount).toFixed(2);
    },

    //选择产品
    selectCustomer(item, index) {
      console.log("item",item)
      console.log("index",index)
      if(this.type === 'edit'){
        return
      }
      let g = {
        balanceBefore: 0,
        amount: 0,
        balanceAfter: 0,
        supplierId: item.id,
        supplierCode: item.code,
        supplierName: item.name,
        supplierFlowType: this.params.supplierFlowType
      };
      this.dataList[index] = g;
      if (!this.dataList[index + 1]) {
        this.dataList.push({supplierId: null});
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
      SupplierInitial.batchSave(requestData).then(({data}) => {
        this.dataList = data;
        console.log("data",data)
        this.closeWindow();
      }).finally(() => this.loading = false);
    },

    checkHttp(requestData) {
      if (requestData.length === 0) {
        message.error("请选择客户");
        return false
      }
      let quantityFlag = false
      let unitPriceFlag = false
      let subtotalFlag = false
      let customerFlag = false
      requestData.map(item => {
        console.log("item",item)
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
        message.error("请选择货商~");
        return false
      }
      if (quantityFlag) {
        message.error("期初应收款~");
        return false
      }
      if (unitPriceFlag) {
        message.error("期初预收款~");
        return false
      }
      if (subtotalFlag) {
        message.error("期初余额为空~");
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
      SupplierInitial.getInfo(this.supplierInitialId).then(({data}) => {
        this.dataList[0] = data;
        console.log("data",data)
      }).finally(() => this.loading = false);
    },
    initForm(){
      for (let index = 0; index < 5; index++) {
        this.dataList.push({supplierId: null});
      }
    },
  },
  created() {
    //loading("加载中....");
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
    }).finally(() => loading.close());
  }
}
</script>
