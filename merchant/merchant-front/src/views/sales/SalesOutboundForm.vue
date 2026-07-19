<template>
  <div class="sales-outbound-wrapper">
    <div class="page-column">
      <div class="page-column-full-body">
        <vxe-toolbar class-name="!size--mini">
          <template #buttons>
            <label class="mr-20px" style="font-size: 16px !important;">客户:</label>
            <Select class="w-300px" filterable required :datas="customerList" keyName="id" titleName="name"
                    :deletable="false" @change="changeCustomer($event)" v-model="customerId" placeholder="请选择客户"/>
            <label class="mr-20px ml-16px" style="font-size: 16px !important;">出库日期:</label>
            <DatePicker v-model="form.outboundDate" :option="{start:accountBook.checkoutDate}"
                        :clearable="false"></DatePicker>
            <Button v-if="type==='add'" @click="addOrEditForm()" color="primary" style="margin-left: 20px">选择源单</Button>
          </template>
          <template #tools>
            <Stamp v-if="form.orderStatus === '已审核' " />
          </template>
        </vxe-toolbar>
        <vxe-table
            size="mini"
            ref="xTable"
            border="border"
            :row-config="{height: 40}"
            show-footer
            :footer-method="footerMethod"
            stripe
            :data="productData">
          <vxe-column title="序号" type="seq" width="60" align="center" fixed="left"/>
          <vxe-column title="操作" field="seq" width="70" align="center" fixed="left">
            <template #default="{row,rowIndex}">
              <div class="fa fa-plus text-hover mr-5px" @click="adjustRows('insert',rowIndex)"></div>
              <div class="fa fa-minus text-hover" v-if="isDeleting" @click="adjustRows('delete',rowIndex)"></div>
            </template>
          </vxe-column>
          <vxe-column field="imgPath" title="产品图片" width="100">
            <template #default="{row}">
              <img
                  :src="productList.find(item => item.id === row.productId)?.imgPath || '-'"
                  alt=""
                  class="product-img cursor-pointer"
                  @click="previewImage(productList.find(item => item.id === row.productId)?.imgPath)">
            </template>
          </vxe-column>
          <vxe-column field="productCode" title="产品编码" width="240"></vxe-column>
          <vxe-column title="产品信息" width="180" align="center">
            <template #default="scope">
              <div class="h-input-group goodsSelect" @keyup.stop="void(0)">
                <Select ref="ms" @change="selectProduct($event,scope.rowIndex)" :datas="productList" v-model="scope.row.productId"
                        keyName="id" titleName="name" filterable placeholder="输入编码/名称" :deletable="false" :equalWidth="false">
                  <template v-slot:top>
                    <table class="h-table" style="width: 100%">
                      <thead class="h-table-header">
                      <tr>
                        <td width="150" align="center">产品编号</td>
                        <td width="150" align="center">产品图片</td>
                        <td width="150" align="center">产品名称</td>
                        <td width="150" align="center">产品类别</td>
                        <td width="150" align="center">产品规格</td>
                      </tr>
                      </thead>
                    </table>
                  </template>
                  <template v-slot:item="{ item }">
                    <table>
                      <tbody class="h-table-body-table">
                      <tr>
                        <td width="150" align="center">{{ item.code }}</td>
                        <td width="150" align="center">{{ item.imageUrl }}</td>
                        <td width="150" align="center">{{ item.name }}</td>
                        <td width="150" align="center">{{ item.productCategoryName }}</td>
                        <td width="150" align="center">{{ item.specification }}</td>
                      </tr>
                      </tbody>
                    </table>
                  </template>
                </Select>
              </div>
            </template>
          </vxe-column>
          <vxe-column title="规格型号" field="specification" align="center" width="100">
            <template #default="{row}">
              {{ productList.find(item => item.id === row.productId)?.specification || '-' }}
            </template>
          </vxe-column>
          <vxe-column title="产品类别" field="productCategoryName" align="center" width="100">
            <template #default="{row}">
              {{ productList.find(item => item.id === row.productId)?.productCategoryName || '-' }}
            </template>
          </vxe-column>
          <vxe-column title="仓库" field="warehouse" align="center" width="180">
            <template #default="{row,rowIndex}">
              <template v-if="!row.isNew">
                <Select :deletable="false" v-model="row.warehouseId" :datas="warehouseList" filterable keyName="id"
                        titleName="name" @change="handleWarehouseChange(row, $event)"/>
              </template>
            </template>
          </vxe-column>
          <vxe-column title="数量" field="quantity" width="90">
            <template #default="{row,rowIndex,columnIndex}">
              <vxe-tooltip theme="light">
                <template #content>
                  <div>当前库存: {{row.currentStockQuantity || 0}}</div>
                  <div>总库存: {{row.totalStockQuantity || 0}}</div>
                </template>
                <vxe-input
                    :id="'r'+rowIndex+''+3"
                    @blur="updateQuantity(row)"
                    @focus="showStockQuantity(row)"
                    ref="inputQuantity"
                    v-model.number="row.quantity"
                    type="float"
                    min="0"
                    :controls="false">
                </vxe-input>
              </vxe-tooltip>
            </template>
          </vxe-column>
          <vxe-column title="单位" field="unitName" align="center" width="80"/>
          <vxe-column title="单价" field="unitPrice" width="100">
            <template #default="{row,rowIndex}">
              <vxe-tooltip theme="light">
                <template #content>
                  <div class="recent-sales-table">
                    <table>
                      <thead>
                      <tr>
                        <th>最近销售时间</th>
                        <th>最近销售价</th>
                        <th>{{ customerLevel }}</th>
                      </tr>
                      </thead>
                      <tbody>
                      <tr v-for="(item, index) in recentSales || []" :key="index">
                        <td>{{item.orderDate || '-'}}</td>
                        <td>{{item.unitPrice || '-'}}</td>
                        <td>{{item.customerPrice || '-'}}</td>
                      </tr>
                      </tbody>
                    </table>
                  </div>
                </template>
                <vxe-input
                    :id="'r'+rowIndex+''+4"
                    @blur="updatePrice(row)"
                    @focus="showPrice(row)"
                    v-model.number="row.unitPrice"
                    type="float"
                    min="0"
                    :controls="false">

                </vxe-input>
              </vxe-tooltip>
            </template>
          </vxe-column>
          <vxe-column title="折扣率(%)" field="discountRate" width="100">
            <template #default="{row,rowIndex}">
              <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+5"
                         @blur="updateDiscount(row)" v-model.number="row.discountRate" type="float" min="0"
                         :controls="false"></vxe-input>
            </template>
          </vxe-column>
          <vxe-column title="折扣额" field="discountValue" width="100">
            <template #default="{row,rowIndex}">
              <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+6"
                         @blur="updateDiscountAmount(row)" v-model.number="row.discountValue" type="float" min="0"
                         :controls="false"></vxe-input>
            </template>
          </vxe-column>
          <vxe-column title="金额" field="subtotal" width="100">
            <template #default="{row,rowIndex}">
              <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+7"
                         @blur="updateFinalAmount(row)" v-model.number="row.subtotal" type="float" min="0"
                         :controls="false" readonly disabled></vxe-input>
            </template>
          </vxe-column>
          <vxe-column title="备注" field="remark" width="100">
            <template #default="{row,rowIndex}">
              <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+8"
                         v-model="row.remark" placeholder="输入备注" :controls="false"></vxe-input>
            </template>
          </vxe-column>
          <vxe-column title="关联销售单号" field="salesOrderNo" width="200">
            <template #default="{row,rowIndex}">
              <vxe-input v-if="!row.isNew" :id="'r'+rowIndex+''+9"
                         v-model="row.salesOrderNo" placeholder="关联销售单号" :controls="false" readonly disabled></vxe-input>
            </template>
          </vxe-column>


        </vxe-table>
        <div class="mt-10px"></div>
        <div class="filler-panel" v-if="type==='edit'">
          <div class="filler-item">
            <label class="mr-16px  w-100px">单据编号：</label>
            <Input v-model="form.orderNo" readonly/>
          </div>
        </div>
        <div class="filler-panel">
          <div class="filler-item">
            <label class="mr-16px  w-100px">优惠率(%)：</label>
            <vxe-input v-model.number="form.discountRate" @blur="discountRateComputeFinalAmount" type="float" min="0"
                       :controls="false">
            </vxe-input>
            <label class="ml-10px mr-16px  w-80px">优惠金额：</label>
            <vxe-input v-model.number="form.discountAmount" @blur="discountAmountComputeFinalAmount" type="float" min="0"
                       :controls="false">
            </vxe-input>
            <label class="ml-16px mr-16px  w-100px">优惠后金额：</label>
            <vxe-input v-model.number="form.finalAmount" type="float" min="0"
                       :controls="false" readonly disabled>
            </vxe-input>
          </div>
        </div>
        <div class="filler-panel">
          <div class="filler-item">
            <label class="mr-16px  w-100px">备注说明：</label>
            <Input placeholder="请输入备注" maxlength="150"  v-model="form.remarks"/>
          </div>
        </div>
      </div>
      <div class="page-column-footer modal-column-between bg-white-color border">
        <Button @click="closeWindow" :loading="loading">
          取消
        </Button>
        <div>
          <Button color="primary" @click="saveOrder('new')" v-if="form.orderStatus !== '已审核' " :loading="loading">
            保存并新增
          </Button>
          <Button @click="saveOrder('save')" v-if="form.orderStatus !== '已审核' " :loading="loading">
            保存
          </Button>
          <!-- 当状态为已审核时不显示,审核后订单上显示已审核图片 -->
          <Button @click="auditOrder('已审核')" v-if="form.orderStatus !== '已审核' " :loading="loading">
            审核
          </Button>
          <!-- 仅当状态为审核时显示 -->
          <Button @click="auditOrder('已保存')" v-if="form.orderStatus === '已审核' " :loading="loading">
            反审核
        </Button>
        </div>
      </div>
    </div>
    <div v-if="previewVisible" class="image-preview-modal" @click="previewVisible = false">
      <div class="image-preview-container">
        <img :src="previewImageUrl" class="preview-image" alt="产品图片预览">
      </div>
    </div>
  </div>
</template>
<script>

import {DialogPlugin, LoadingPlugin, MessagePlugin} from "tdesign-vue-next";
import manba from "manba";
import Customer from "@js/api/basic/Customer";
import Warehouse from "@js/api/basic/Warehouse";
import {mapMutations, mapState} from "vuex";
import Product from "@js/api/basic/Product";
import {openDialog, closeDialog} from '@common/dialog';
import {h} from "vue";
import SalesOrderSelect from "@views/sales/SalesOrderSelect.vue";
import Unit from "@js/api/basic/Unit";
import SalesOutbound from "@js/api/sales/SalesOutbound";
import Inventory from "@js/api/inventory/Inventory";
import Stamp from "@views/common/Stamp.vue";
import PriceRecord from "@js/api/basic/PriceRecord";

export default {
  name: "SalesOutboundForm",
  components: {Stamp},
  computed: {
    ...mapState(['accountBook']),
    isDeleting() {
      return this.productData.length > 1;
    }
  },
  data() {
    return {
      loading: false,
      productList: [],
      unitList: [],
      product: null,
      warehouseList: [],
      customerList: [],
      customerId: null,
      warehouseId: null,
      form: {
        id: null,
        outboundDate: manba().format("YYYY-MM-dd"),
        customerId: null,
        discountAmount: 0.00,
        discountRate: 0.00,
        finalAmount: 0.00,
        remarks: null,
      },
      productData: [],
      //保存选择的源单
      selectSalesOrderIdList: [],
      orderId:null,
      type:null,
      previewVisible: false,
      previewImageUrl: '',
      recentSales: [],
      customerLevel:null,
      customerLevelId:null,
      customerPrice:null
    }
  },
  methods: {
    ...mapMutations(['newTab']),

    //添加或编辑Form
    addOrEditForm(entity) {
      if (!this.form.customerId) {
        MessagePlugin.error("请选择客户~");
        return
      }
      let dialogId = openDialog({
        header: "请选择销售订单",
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '1000px',
        body: h(SalesOrderSelect, {
          // 传递参数到子组件
          customerId: this.customerId,  // 客户ID
          onClose: () => {
            closeDialog(dialogId);
          },
          onSuccess: (params) => {  // 添加参数接收
            // 处理选中的订单数据
            console.log('选中的订单数据:', params);
            // 处理你业务逻辑
            this.handleSelectedOrders(params);

            //this.doSearch();
            closeDialog(dialogId);
          },
        }),
      });
    },

    handleSelectedOrders(params) {
      this.itemTemp = params.itemList;
      let itemList = params.itemList;
      const unitMap = new Map(this.unitList.map(unit => [unit.id, unit]));
      itemList.forEach(row => {
        // 根据 baseUnitId 查找对应的 unitName
        const unit = unitMap.get(row.baseUnitId);
        if (unit) {
          row.unitName = unit.name;
        }
        //订单产品id 后台需要存储关联
        row.tempId =row.id;
        //将id置为空，因为是新增的产品
        row.id = null;
      });

      //this.productList  封装产品名称和产品编码，进行回显  productList中的id 和 itemList中的productId  需要做判断
      console.log("this.productList",this.productList)
      this.productList.map(item => {
        itemList.map(item2 => {
          if(item.id === item2.productId){
            item2.productName = item.name;
            item2.productCode = item.code;
          }
        })
      })
      console.log('处理后的订单数据:', itemList)
      // 将 itemList 赋值给 productData
      this.productData = itemList;
      this.selectSalesOrderIdList = params.selectSalesOrderIdList;
      console.log('this.selectSalesOrderIdList',this.selectSalesOrderIdList)
      console.log('itemList',itemList)

      // this.productData = itemList.map(item => ({
      //   ...item,
      //   //封装产品名称和产品编码，进行回显
      //   productName: item.name,
      //   productCode: item.code,
      //   //封装产品名称和code
      //   // 如果需要添加或修改其他字段可以在这里处理
      // }));
    },
    //footer合计
    footerMethod({columns, data}) {
      let quantity = 0;
      let discountValue = 0;
      let subtotal = 0;
      columns.forEach((column) => {
        if (column.property && ['quantity', 'discountValue', 'subtotal'].includes(column.property)) {
          let total = 0;
          data.forEach((row) => {
            if (column.property === 'quantity') {
              let rd = row[column.property];
              if (rd) {
                quantity += Number(rd || 0);
              }
            } else if (column.property === 'discountValue') {
              let rd = row[column.property];
              if (rd) {
                discountValue += Number(rd || 0);
              }
            } else if (column.property === 'subtotal') {
              let rd = row[column.property];
              if (rd) {
                subtotal += Number(rd || 0);
              }
            }
          });
        }
      })
      this.form.orderQuantity = quantity.toFixed(2);
      this.form.totalAmount = subtotal.toFixed(2);
      //计算优惠后金额
      if(this.form.totalAmount > 0){
        if(this.form.discountRate > 0){
          this.discountRateComputeFinalAmount()
        }else if(this.form.discountAmount > 0){
          this.discountAmountComputeFinalAmount()
        }else{
          this.form.finalAmount = this.form.totalAmount
        }
      }
      console.log("subtotal",subtotal)

      return [["", "", "", "", "", "", "","",quantity.toFixed(2), "", "", "",discountValue,subtotal,""]];
    },

    //选择产品
    selectProduct(d, index) {
      console.log("selectProduct",d);
      let customerLevelPriceList = d.customerLevelPriceList;
      if(customerLevelPriceList && customerLevelPriceList.length > 0){
        //this.customerLevelId
        customerLevelPriceList.forEach(cp => {
          if(cp.customerLevelId === this.customerLevelId && d.id === cp.productId){
            this.customerPrice = cp.price;
            return;
          }
        })
      }
      if (d) {
        let g = {
          quantity: 1,
          unitPrice: d.lastSalePrice || 0,
          warehouseId: this.warehousesId,
          discountValue: 0.00,
          discountRate: 0.00,
          subtotal: d.lastSalePrice || 0,
          baseUnitId: d.unitId,
          unitName: d.unitName,
          productId: d.id,
          productCode: d.code,
          productName: d.name,
          remark: "",
        };
        //选择产品后自动带出默认仓库
        //warehouseList 中属性为systemDefault = true 为默认仓库
        let defaultWarehouse = this.warehouseList.find(item => item.systemDefault === true);
        if(defaultWarehouse){
          g.warehouseId = defaultWarehouse.id;
        }
        this.productData[index] = g;
        console.log("this.productData",this.productData)
        if (!this.productData[index + 1]) {
          this.productData.push({isNew: true});
        }
        this.$refs.xTable.loadData(this.productData).then(() => {
          this.$nextTick(() => {
            let str = index + '' + 3
            let element = document.querySelector('#r' + str + ' input');
            setTimeout(() => {
              element.focus()
              element.select()
            }, 100);
          })
        });
        this.showStockQuantity(g);
        this.showPrice(g);
      }
    },

    // 仓库选择框变化时触发
    handleWarehouseChange(row) {
      this.showStockQuantity(row);
    },

    showStockQuantity(row) {
      let productId = row.productId;
      let warehouseId = row.warehouseId;
      if (!productId) {
        console.log("请选择产品")
        return;
      }
      // 获取产品库存进行提示
      let param = {
        productId: productId,
        page:1,
        pageSize:1000
      }
      Inventory.list(param).then(res => {
        const {data} = res;
        if (data && data.results) {
          let totalQuantity = 0;
          let quantity = 0;
          data.results.forEach(item => {
            totalQuantity += Number(item.currentQuantity);
            if (Number(item.warehouseId) === Number(warehouseId)) {
              quantity = item.currentQuantity;
            }
          });
          row.currentStockQuantity = quantity;
          row.totalStockQuantity = totalQuantity;
        }
      });
    },

    checkHttp() {
      console.log("this.productData.length",this.productData.length)
      if (this.productData.length === 0) {
        MessagePlugin.error("请选择产品~");
        LoadingPlugin(false)
        return false
      }
      if (this.productData.length === 1) {
        let item = this.productData[0]
        if (item.isNew) {
          MessagePlugin.error("请选择产品~");
          LoadingPlugin(false)
          return false
        }
      }
      let quantityFlag = false
      let unitPriceFlag = false
      let subtotalFlag = false
      let warehouseFlag = false
      this.productData.map(item => {
        console.log("item",item)
        if (item.isNew) {
          return;
        }
        if (item.quantity === 0 || !item.quantity) {
          quantityFlag = true
          LoadingPlugin(false)
        }
        if (item.unitPrice === 0 || !item.unitPrice) {
          unitPriceFlag = true
          LoadingPlugin(false)
        }
        if (item.subtotal === 0 || !item.subtotal) {
          subtotalFlag = true
          LoadingPlugin(false)
        }
        if (!item.warehouseId) {
          warehouseFlag = true
          LoadingPlugin(false)
        }
      })
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
      if (warehouseFlag) {
        MessagePlugin.error("请选择仓库~");
        return false
      }
      return true
    },

    auditOrder(orderStatus){
      if (!this.form.customerId) {
        MessagePlugin.error("请选择客户~");
        LoadingPlugin(false)
        return
      }
      if (!this.checkHttp()) {
        return
      }
      DialogPlugin.confirm({
        content: `确定审核订单？`,
        onConfirm: () => {
          LoadingPlugin(true);
          let salesOutbound ={
            id: this.form.id,
            orderStatus: orderStatus
          }
          SalesOutbound.audit({
            salesOutbound: salesOutbound,
          }).then((success) => {
            if (success) {
              MessagePlugin.success("审核成功~");
              this.closeWindow()
            }
          }).catch(() => {
          }).finally(() => {
            LoadingPlugin(false)
          });
        }
      })
    },

    //保存订单
    saveOrder(saveType) {
      if (!this.form.customerId) {
        MessagePlugin.error("请选择客户~");
        LoadingPlugin(false)
        return
      }
      if (!this.checkHttp()) {
        return
      }
      DialogPlugin.confirm({
        content: `确定保存订单？`,
        onConfirm: () => {
          LoadingPlugin(true);
          let productData = this.productData.filter(c => c.quantity > 0);
          SalesOutbound.save({
            salesOutbound: Object.assign(this.form),
            salesOutboundItemList: productData,
            selectSalesOrderIdList:this.selectSalesOrderIdList
          }).then((success) => {
            if (success) {
              MessagePlugin.success("保存成功~");
              this.clearForm();
              //保存
              if(saveType === 'save'){
                this.closeWindow()
              }

            }
          }).finally(() =>
              LoadingPlugin(false)
          );
        }
      })
    },

    //清除Form
    clearForm() {
      this.form = {
        id: null,
        orderDate: manba().format("YYYY-MM-dd"),
        customerId: null,
        remark: null,
        finalAmount: null
      }
      this.productData = []
      this.customerId = null
    },

    //添加行或减少行
    adjustRows(type, index) {
      if (type === 'insert') {
        this.productData.splice(index + 1, 0, {isNew: true});
      } else {
        this.productData.splice(index, 1);
      }
    },

    //修改客户
    changeCustomer(e) {
      if(e.customerLevelId === 1){
        this.customerLevel = "零售价"
      }else {
        this.customerLevel = "会员价"
      }
      this.customerLevelId = e.customerLevelId
      console.log("e",e)
      if (!e) {
        this.form.customerId = null;
        this.productData = [{isNew: true}];
      } else if (e.id !== this.form.customerId) {
        if (this.productData.length > 1) {
          DialogPlugin.confirm({
            title: "系统提示",
            content: `修改客户后，将清除已选择的产品数据，确定修改？`,
            onConfirm: () => {
              this.productData = [{isNew: true}];
              this.form.customerId = e.id;
            }
          })
        } else {
          this.form.customerId = e.id;
          this.productData = [{isNew: true}];
        }
      }
    },

    //修改产品多单位
    changeProductUnit(item, row) {
      row.orderUnitName = item.unitName
      row.unitPrice = (item.price || 0).toFixed(2) || 0
      row.num = item.num || 1;
      row.sysQuantity = (row.quantity * row.num).toFixed(2);
      row.subtotal = (row.quantity * row.unitPrice).toFixed(2);
    },

    //更新数量
    updateQuantity(item) {
      if(!item.productId){
        return;
      }
      item.quantity = item.quantity || 1;
      item.subtotal = ((item.quantity * item.unitPrice * (100 - item.discountRate)) / 100).toFixed(2);
      item.discountValue = (((item.quantity * item.unitPrice) * item.discountRate) / 100).toFixed(2);
      this.$refs.xTable.updateFooter();
    },

    //更新单价
    updatePrice(item) {
      if(!item.productId){
        return;
      }
      item.unitPrice = item.unitPrice || 0.00
      item.discountValue = (item.unitPrice * item.quantity * item.discountRate / 100).toFixed(2);
      item.subtotal = (item.unitPrice * item.quantity - item.discountValue).toFixed(2);
      this.$refs.xTable.updateFooter();
    },
    showPrice(row){
      let productId = row.productId;
      if (!productId) {
        console.log("请选择产品")
        return;
      }
      // 获取产品库存进行提示
      let param = {
        productId: productId,
        priceSource:'最近销售价格',
        priceType:'最近销售价格',
        page:1,
        pageSize:5
      }
      PriceRecord.showPrice(param).then(({data: {results}}) => {
        this.recentSales = results || [];
        this.recentSales.forEach(item => {
          item.customerPrice = this.customerPrice
        })
        console.log("results",results)
      }).finally(() => this.loading = false);
    },

    //更新折扣
    updateDiscount(item) {
      item.discountRate = item.discountRate || 0.00;
      item.subtotal = ((item.quantity || 0) * item.unitPrice * (100 - item.discountRate || 0) / 100).toFixed(2);
      item.discountValue = ((item.quantity || 0) * item.unitPrice - item.subtotal).toFixed(2);
      this.$refs.xTable.updateFooter();
    },

    //更新折扣金额
    updateDiscountAmount(item) {
      item.discountValue = item.discountValue || 0.00;
      item.discountRate = (((item.discountValue / (item.unitPrice * item.quantity)) * 100) || 0).toFixed(2);
      item.subtotal = (item.unitPrice * item.quantity - item.discountValue).toFixed(2);
      this.$refs.xTable.updateFooter();
    },

    //更新折后金额
    updateFinalAmount(item) {
      item.subtotal = item.subtotal || 0
      item.unitPrice = ((item.subtotal) / ((100 - item.discountRate)) * 100 / item.quantity).toFixed(2);
      item.discoutPrice = (item.unitPrice - item.subtotal).toFixed(2);
      this.$refs.xTable.updateFooter();
    },

    //关闭窗口
    closeWindow() {
      console.log("this.$store.state.currentTab", this.$store.state.currentTab)
      //this.$store.commit('closeTabKey', this.$store.state.currentTab);
      this.$store.commit('closeTabKey', this.$store.state.currentTab);
      this.$store.commit('newTab', "SalesOutboundList");
      // 使用 nextTick 确保在 DOM 更新后执行
      this.$nextTick(() => {
        // 通过 eventBus 或 vuex 触发刷新
        this.$store.commit('SET_TAB_DATA_OUTBOUND', { refresh: true });
      });
    },

    discountRateComputeFinalAmount(){
      //计算优惠率
      this.form.finalAmount = (this.form.totalAmount * (100 - this.form.discountRate) / 100).toFixed(2);
      //计算优惠率
      this.form.discountAmount = (this.form.totalAmount - this.form.finalAmount).toFixed(2);
    },
    discountAmountComputeFinalAmount(){
      //计算优惠金额
      this.form.finalAmount = (this.form.totalAmount - this.form.discountAmount).toFixed(2);
      //计算优惠率
      this.form.discountRate = ((this.form.totalAmount - this.form.finalAmount) / this.form.totalAmount * 100).toFixed(2);
    },
    previewImage(row) {
      console.log("row", row)
      this.previewImageUrl = row;
      this.previewVisible = true;
    }
  },
  beforeDestroy() {
    DialogPlugin.confirm({
      title: "系统提示",
      content: `确认?`,
      onConfirm: () => {

      }
    })
  },
  created() {
    LoadingPlugin(true);
    Promise.all([
      Customer.select(),
      Warehouse.select(),
      Product.select(),
      Unit.select()
    ]).then((results) => {
      this.customerList = results[0].data || [];
      this.warehouseList = results[1].data || [];
      this.productList = results[2].data || [];
      this.unitList = results[3].data || [];
      console.log("this.productList", this.productList);
      this.productList.forEach(item => {
        item.name = `${item.code}--${item.name}`;
      });
      //订单详情/编辑订单
      const tabData = this.$store.state.currentTabDataOutbound;
      //清空参数
      this.$store.commit('SET_TAB_DATA_OUTBOUND', null);
      console.log("tabData", tabData)
      this.type = tabData?.type;
      this.orderId = tabData?.orderId;
      if (this.orderId) {
        SalesOutbound.getInfo(this.orderId).then(response => {
          let salesOutbound = response.data;
          console.log("response.data", salesOutbound)
          this.form = salesOutbound;
          this.customerId = salesOutbound.customerId;
          //this.form.discountRate = ((this.form.discountAmount/this.form.totalAmount)*100).toFixed(2);
          console.log("this.form", this.form)
          this.productData = salesOutbound.salesOutboundItemList || [];
          this.productData.push({isNew: true});
        });
      }
    }).finally(() => LoadingPlugin(false));
  },
}
</script>
<style scoped>
.sales-outbound-wrapper {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.recent-sales-table {
  min-width: 300px;
}

.recent-sales-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  border: 1px solid #dfe6ec;
}

.recent-sales-table th,
.recent-sales-table td {
  padding: 8px 12px;
  text-align: left;
  border: 1px solid #dfe6ec;
}

.recent-sales-table th {
  background-color: #f5f7fa;
  font-weight: bold;
  color: #606266;
}

.recent-sales-table tbody tr:hover {
  background-color: #f5f7fa;
}

.image-preview-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-preview-container {
  max-width: 90%;
  max-height: 90%;
  background: #fff;
  padding: 10px;
  border-radius: 5px;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
</style>
