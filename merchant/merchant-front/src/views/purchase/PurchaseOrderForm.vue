<template>
  <div class="page-column">
    <div class="page-column-full-body">
      <div class="form-toolbar">
        <div class="form-toolbar__left">
          <label class="mr-20px" style="font-size: 16px !important">供货商：</label>
          <t-select
            class="w-300px"
            filterable
            :options="supplierList"
            :clearable="false"
            @change="changeSupplier($event)"
            v-model="supplierId"
            placeholder="请选择供货商"
            :keys="{ value: 'id', label: 'name' }"
          />
          <label class="mr-20px ml-16px" style="font-size: 16px !important">单据日期：</label>
          <t-date-picker
            v-model="form.orderDate"
            :clearable="false"
          />
        </div>
      </div>
      <t-table
        ref="xTable"
        row-key="_rowKey"
        size="small"
        bordered
        stripe
        hover
        table-layout="fixed"
        :columns="columns"
        :data="productData"
        :foot-data="footData"
        :loading="loading"
      >
        <template #ops="{ rowIndex }">
          <div class="fa fa-plus text-hover mr-5px" @click="adjustRows('insert', rowIndex)"></div>
          <div
            class="fa fa-minus text-hover"
            v-if="isDeleting"
            @click="adjustRows('delete', rowIndex)"
          ></div>
        </template>
        <template #imgPath="{ row }">
          <img
            :src="productImage(row)"
            alt=""
            class="product-img cursor-pointer"
            @click="previewImage(productImage(row))"
          />
        </template>
        <template #productInfo="{ row, rowIndex }">
          <!-- 新商品：显示商品名称输入框 -->
          <div v-if="row.isNewProduct" class="input-group">
            <t-input
              v-model="row.newProductName"
              placeholder="输入商品名称"
              size="small"
            />
          </div>
          <!-- 已有商品选择 -->
          <div class="input-group goodsSelect" @keyup.stop="void 0" v-else>
            <t-select
              ref="ms"
              @change="selectProduct($event, rowIndex)"
              @create="createProduct($event, rowIndex)"
              v-model="row.productId"
              :options="productList"
              filterable
              creatable
              placeholder="输入编码/名称"
              :keys="{ value: 'productId', label: 'productName' }"
            />
          </div>
        </template>
        <template #secondaryUnitName="{ row }">
          <!-- 新商品：显示单位输入框 -->
          <t-input
            v-if="row.isNewProduct"
            v-model="row.secondaryUnitName"
            placeholder="输入单位"
            size="small"
          />
          <template v-else-if="!row.isNew">
            <t-select
              v-if="row.auxiliaryUnitPrices"
              :clearable="false"
              @change="changeProductUnit($event, row)"
              v-model="row.secondaryUnitId"
              :options="row.auxiliaryUnitPrices"
              filterable
              placeholder="输入采购单位"
              :keys="{ value: 'unitId', label: 'unitName' }"
            />
            <span v-else>{{ row.secondaryUnitName }}</span>
          </template>
        </template>
        <template #categoryName="{ row }">
          <!-- 新商品：显示商品类别选择 -->
          <t-select
            v-if="row.isNewProduct"
            v-model="row.newProductCategoryId"
            :options="productCategoryList"
            placeholder="选择类别"
            size="small"
            filterable
            creatable
            :keys="{ value: 'id', label: 'name' }"
            @create="createProductCategory($event, row)"
          />
          <span v-else>{{ row.categoryName }}</span>
        </template>
        <template #warehouse="{ row }">
          <template v-if="!row.isNew">
            <t-select
              :clearable="false"
              v-model="row.warehouseId"
              :options="warehouseList"
              filterable
              @change="handleWarehouseChange(row, $event)"
              :keys="{ value: 'id', label: 'name' }"
            />
          </template>
        </template>
        <template #secondaryQuantity="{ row, rowIndex }">
          <template v-if="!row.isNew">
            <t-tooltip theme="light">
              <template #content>
                <div>当前库存: {{ row.currentStockQuantity || 0 }}</div>
                <div>总库存: {{ row.totalStockQuantity || 0 }}</div>
              </template>
              <t-input-number
                :id="'r' + rowIndex + '' + 3"
                v-model="row.secondaryQuantity"
                theme="normal"
                :min="0"
                :decimal-places="qtyDp"
                style="width: 100%"
                @blur="updateQuantity(row)"
                @focus="showStockQuantity(row)"
              />
            </t-tooltip>
          </template>
        </template>
        <template #secondaryPrice="{ row, rowIndex }">
          <template v-if="!row.isNew">
            <t-tooltip theme="light">
              <template #content>
                <div class="recent-sales-table">
                  <table>
                    <thead>
                      <tr>
                        <th>最近采购时间</th>
                        <th>最近采购价</th>
                        <th>预计采购价</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(item, index) in recentSales || []" :key="index">
                        <td>{{ item.orderDate || '-' }}</td>
                        <td>{{ item.unitPrice || '-' }}</td>
                        <td>{{ item.purchasePrice || '-' }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </template>
              <t-input-number
                :id="'r' + rowIndex + '' + 4"
                v-model="row.secondaryPrice"
                theme="normal"
                :min="0"
                :decimal-places="priceDp"
                style="width: 100%"
                @keyup="handleEnter($event, rowIndex, 4)"
                @blur="updatePrice(row)"
                @focus="showPrice(row.productId)"
              />
            </t-tooltip>
          </template>
        </template>
        <template #discountRate="{ row, rowIndex }">
          <t-input-number
            v-if="!row.isNew"
            :id="'r' + rowIndex + '' + 5"
            v-model="row.discountRate"
            theme="normal"
            :min="0"
            :decimal-places="2"
            style="width: 100%"
            @keyup="handleEnter($event, rowIndex, 5)"
            @blur="updateDiscount(row)"
          />
        </template>
        <template #discountAmount="{ row, rowIndex }">
          <t-input-number
            v-if="!row.isNew"
            :id="'r' + rowIndex + '' + 6"
            v-model="row.discountAmount"
            theme="normal"
            :min="0"
            :decimal-places="2"
            style="width: 100%"
            @keyup="handleEnter($event, rowIndex, 6)"
            @blur="updateDiscountAmount(row)"
          />
        </template>
        <template #subtotal="{ row, rowIndex }">
          <t-input-number
            v-if="!row.isNew"
            :id="'r' + rowIndex + '' + 7"
            v-model="row.subtotal"
            theme="normal"
            :min="0"
            :decimal-places="2"
            style="width: 100%"
            @keyup="handleEnter($event, rowIndex, 7)"
            @blur="updateSubtotal(row)"
          />
        </template>
        <template #remark="{ row, rowIndex }">
          <t-input
            v-if="!row.isNew"
            :id="'r' + rowIndex + '' + 8"
            v-model="row.remark"
            placeholder="输入备注"
            @keyup="handleEnter($event, rowIndex, 8)"
          />
        </template>
      </t-table>
      <div class="mt-10px"></div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px w-80px">备注说明：</label>
          <t-input
            placeholder="请输入备注"
            maxlength="150"
            v-model="form.remarks"
          />
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item">
          <label class="mr-16px w-80px">优惠率：</label>
          <t-input v-model="form.discountRate" @blur="changeDiscountRate" />
          <label class="ml-10px mr-16px w-80px">优惠金额：</label>
          <t-input v-model="form.discountAmount" @blur="changeDiscountAmount" />
          <label class="ml-16px mr-16px w-100px">优惠后金额：</label>
          <t-input v-model="form.finalAmount" @blur="changeFinalAmount" />
        </div>
      </div>
    </div>
    <div class="page-column-footer modal-column-between bg-white-color border">
      <t-button @click="closeWindow" :loading="loading"> 取消 </t-button>
      <div>
        <t-button theme="primary" v-auth="'purchaseOrder:edit'" @click="saveOrder('add')" :loading="loading">
          保存并新增
        </t-button>
        <t-button v-auth="'purchaseOrder:edit'" @click="saveOrder('save')" :loading="loading"> 保存 </t-button>
        <t-button @click="doPrint" :loading="loading"> 打印 </t-button>
        <t-button @click="approved()" :loading="loading" v-if="$can('purchaseOrder:audit') && form.id">
          审核
        </t-button>
      </div>
    </div>
  </div>
</template>
<script>
import {LoadingPlugin, MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import {openPrint} from '@common/print';
import manba from 'manba';
import {CopyObj} from '@common/utils';
import {switchUnit} from '@common/unit';
import PurchaseOrder from '@js/api/purchase/PurchaseOrder';
import Supplier from '@js/api/basic/Supplier';
import Product from '@js/api/basic/Product';
import ProductCategory from '@js/api/basic/ProductCategory';
import Warehouse from '@js/api/basic/Warehouse';
import {mapState} from 'vuex';
import PriceRecord from '@js/api/basic/PriceRecord';
import Inventory from '@js/api/inventory/Inventory';

let rowSeq = 0;
function newRow(extra = {}) {
  return {
    _rowKey: `r-${++rowSeq}`,
    productId: null,
    isNew: true,
    isNewProduct: false,
    newProductName: '',
    newProductCategoryId: null,
    ...extra
  };
}

export default {
  name: 'PurchaseOrderForm',
  props: {
    orderId: [String, Number],
    type: String
  },
  computed: {
    ...mapState(['accountBook']),
    totalAmount() {
      let total = 0;
      this.productData.forEach((val) => {
        if (val.quantity > 0) {
          total += parseFloat(val.subtotal);
        }
      });
      return total.toFixed(2);
    },
    isDeleting() {
      return this.productData.length > 1;
    },
    columns() {
      return [
        {
          colKey: 'seq',
          title: '序号',
          width: 60,
          align: 'center',
          fixed: 'left',
          cell: (h, { rowIndex }) => rowIndex + 1
        },
        {
          colKey: 'ops',
          title: '操作',
          width: 70,
          align: 'center',
          fixed: 'left',
          foot: () => '合计'
        },
        { colKey: 'imgPath', title: '产品图片', width: 100 },
        { colKey: 'productInfo', title: '产品信息', minWidth: 300 },
        { colKey: 'secondaryUnitName', title: '采购单位', align: 'center', width: 80 },
        { colKey: 'categoryName', title: '产品类别', align: 'center', width: 80 },
        { colKey: 'spec', title: '规格型号', align: 'center', width: 80 },
        { colKey: 'warehouse', title: '仓库', align: 'center', width: 120 },
        { colKey: 'secondaryQuantity', title: '数量', width: 90 },
        { colKey: 'baseUnitName', title: '基本单位', align: 'center', width: 80 },
        { colKey: 'quantity', title: '基本数量', width: 90 },
        { colKey: 'secondaryPrice', title: '购货单价', width: 100 },
        { colKey: 'discountRate', title: '折扣率(%)', width: 100 },
        { colKey: 'discountAmount', title: '折扣额', width: 100 },
        { colKey: 'subtotal', title: '购货金额', width: 100 },
        { colKey: 'remark', title: '备注', width: 160 },
      ];
    },
    footData() {
      let quantity = 0;
      let discountAmount = 0;
      let subtotal = 0;
      (this.productData || []).forEach((row) => {
        if (row.quantity) {
          quantity += Number(row.quantity || 0);
        }
        discountAmount += Number(row.discountAmount || 0);
        subtotal += Number(row.subtotal || 0);
      });
      this.allFinalAmount = subtotal;
      return [{
        ops: '合计',
        quantity: quantity.toFixed(0),
        discountAmount: discountAmount.toFixed(2),
        subtotal: subtotal.toFixed(2)
      }];
    }
  },
  data() {
    return {
      loading: false,
      productList: [],
      productCategoryList: [],
      product: null,
      allFinalAmount: 0,
      warehouseList: [],
      supplierList: [],
      supplierId: null,
      warehouseId: null,
      form: {
        id: null,
        orderDate: manba().format('YYYY-MM-dd'),
        supplierId: null,
        discountAmount: 0.0,
        discountRate: 0.0,
        finalAmount: 0.0,
        totalAmount: 0.0,
        remarks: null
      },
      productData: [newRow({ isNew: true })],
      recentSales: []
    };
  },
  watch: {
    allFinalAmount(val) {
      this.form.discountAmount = (val * this.form.discountRate * 0.01).toFixed(
        2
      );
      this.form.finalAmount = (val - this.form.discountAmount).toFixed(2);
    },
    'productData': {
      handler(newVal) {
        // 监听数量和单价变化，自动计算小计
        newVal.forEach(row => {
          if (!row.isNew || row.isNewProduct) {
            const quantity = Number(row.secondaryQuantity) || 0;
            const price = Number(row.secondaryPrice) || 0;
            const discountRate = Number(row.discountRate) || 0;
            const subtotal = (quantity * price * (100 - discountRate) / 100).toFixed(2);
            const discountAmount = (quantity * price * discountRate / 100).toFixed(2);
            row.subtotal = subtotal;
            row.discountAmount = discountAmount;
          }
        });
      },
      deep: true
    }
  },
  methods: {
    productImage(row) {
      const p = (this.productList || []).find((item) => item.productId === row.productId);
      return p?.imgPath || '';
    },
    previewImage(url) {
      if (!url || url === '-') return;
      window.open(url, '_blank');
    },
    doPrint() {
      const items = (this.productData || []).filter(r => r && !r.isNew && r.productId).map(r => {
        const p = (this.productList || []).find(x => x.productId === r.productId) || {};
        return {
          ...r,
          productName: r.productName || p.name || '',
          quantity: r.quantity ?? r.secondaryQuantity,
          price: r.unitPrice ?? r.secondaryPrice ?? r.price,
          amount: r.subtotal ?? r.amount,
        };
      });
      openPrint('采购订单', {
        header: {
          ...this.form,
          partner: (this.supplierList.find(s => s.id === this.supplierId) || {}).name || '',
          amount: this.form.finalAmount ?? this.form.totalAmount,
        },
        items,
      });
    },

    handleEnter(e, index, num) {
      const event = e?.$event || e;
      if (!event) return;
      event.stopPropagation?.();
      const keyCode = event.keyCode || event.which;
      if (keyCode === 13) {
        if (num === 8) {
          if (index >= this.productData.length - 2) {
            this.$refs.ms?.$el?.querySelector('input')?.click();
            this.$refs.ms?.$el?.querySelector('input')?.select();
          } else {
            let str = 'r' + (index + 1) + '' + 3;
            document.getElementById(str)?.querySelector('input')?.focus();
            document.getElementById(str)?.querySelector('input')?.select();
          }
        } else {
          let str = 'r' + index + '' + (num + 1);
          document.getElementById(str)?.querySelector('input')?.focus();
          document.getElementById(str)?.querySelector('input')?.select();
        }
      } else if (keyCode === 38) {
        if (index > 0) {
          let str = 'r' + (index - 1) + '' + num;
          document.getElementById(str)?.querySelector('input')?.focus();
          document.getElementById(str)?.querySelector('input')?.select();
        }
      } else if (keyCode === 40) {
        if (index < this.productData.length - 2) {
          let str = 'r' + (index + 1) + '' + num;
          document.getElementById(str)?.querySelector('input')?.focus();
          document.getElementById(str)?.querySelector('input')?.select();
        } else {
          this.$refs.ms?.$el?.querySelector('input')?.click();
          this.$refs.ms?.$el?.querySelector('input')?.select();
        }
      }
    },
    handleWarehouseChange(row) {
      this.showStockQuantity(row);
    },

    showStockQuantity(row) {
      let productId = row.productId;
      let warehouseId = row.warehouseId;
      if (!productId) {
        return;
      }
      let param = {
        productId: productId,
        page: 1,
        pageSize: 1000
      };
      Inventory.list(param).then((res) => {
        const { data } = res;
        if (data && data.results) {
          let totalQuantity = 0;
          let quantity = 0;
          data.results.forEach((item) => {
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

    createProduct(value, index) {
      const name = (typeof value === 'string' ? value : (value?.label || value?.productName || '')).trim();
      if (!name) return;
      // 标记为新商品，不立即创建
      this.productData[index] = {
        ...this.productData[index],
        isNewProduct: true,
        newProductName: name,
        productId: null,
        isNew: false,
        quantity: 1,
        secondaryQuantity: 1,
        secondaryPrice: 0,
        price: 0,
        subtotal: 0,
        secondaryUnitName: '个', // 默认单位
        baseUnitName: '个',
      };
    },
    createProductCategory(value, row) {
      const name = (typeof value === 'string' ? value : (value?.label || value?.name || '')).trim();
      if (!name) return;
      // 先清空，避免把输入的名称字符串留在 newProductCategoryId 上
      row.newProductCategoryId = null;
      ProductCategory.save({ name }).then(({ data }) => {
        if (data && data.id) {
          this.productCategoryList.push(data);
          row.newProductCategoryId = data.id;
        }
      });
    },

    selectProduct(value, index) {
      const d = (this.productList || []).find((item) => String(item.productId) === String(value));
      if (d) {
        const defaultWarehouseId = this.resolveDefaultWarehouseId();
        let g = newRow({
          isNew: false,
          quantity: 1,
          secondaryQuantity: 1,
          secondaryPrice: d.price || 0,
          warehouseId: defaultWarehouseId,
          price: d.price || 0,
          discountAmount: 0.0,
          discountRate: 0.0,
          subtotal: d.price || 0,
          conversionRate: 1,
          secondaryUnitId: d.unitId,
          secondaryUnitName: d.unitName,
          baseUnitId: d.unitId,
          baseUnitName: d.unitName,
          categoryName: d.categoryName,
          spec: d.spec,
          remark: ''
        });
        this.productData[index] = Object.assign({}, d, g, {
          productId: d.productId,
          isNew: false,
        });
        if (!this.productData[index].warehouseId) {
          this.productData[index].warehouseId = defaultWarehouseId;
        }
        if (!this.productData[index + 1]) {
          this.productData.push(newRow({ isNew: true }));
        }
        this.$nextTick(() => {
          let str = index + '' + 3;
          let element = document.querySelector('#r' + str + ' input');
          setTimeout(() => {
            element?.focus();
            element?.select();
          }, 100);
        });
        this.showPrice(d.productId);
      }
      this.product = null;
    },

    resolveDefaultWarehouseId() {
      if (this.warehouseId) {
        return this.warehouseId;
      }
      const list = this.warehouseList || [];
      const found = list.find((w) => w.systemDefault || w.isDefault);
      const id = found?.id || null;
      this.warehouseId = id;
      return id;
    },

    showPrice(productId) {
      if (!productId) {
        return;
      }
      let param = {
        productId: productId
      };
      PriceRecord.purchasePrice(param)
        .then(({ data }) => {
          this.recentSales = data || [];
        })
        .finally(() => (this.loading = false));
    },

    saveOrder(type) {
      LoadingPlugin(true);
      if (!this.form.supplierId) {
        MessagePlugin.error('请选择购货商~');
        LoadingPlugin(false);
        return;
      }

      // 收集新商品
      const newProducts = this.productData
        .filter(c => c.isNewProduct && c.newProductName && c.quantity > 0)
        .map(c => ({
          name: c.newProductName,
          productCategoryId: c.newProductCategoryId,
          unitName: c.secondaryUnitName || c.baseUnitName
        }));

      // 创建新商品的函数
      const createNewProducts = async () => {
        const createdProducts = [];
        for (const newProduct of newProducts) {
          const { data } = await Product.quickCreate(newProduct);
          if (data && data.productId) {
            createdProducts.push({...data, inputName: newProduct.name});
            this.productList.push(data);
          }
        }
        return createdProducts;
      };

      // 保存订单的函数
      const saveOrderData = (createdProducts) => {
        // 更新新商品行的完整信息
        this.productData.forEach(row => {
          if (row.isNewProduct && row.newProductName) {
            const createdProduct = createdProducts.find(p => p.inputName === row.newProductName);
            if (createdProduct) {
              row.productId = createdProduct.productId;
              row.productCode = createdProduct.productCode || '';
              row.productName = createdProduct.productName || row.newProductName;
              row.baseUnitId = createdProduct.unitId;
              row.secondaryUnitId = createdProduct.unitId;
              row.baseUnitName = createdProduct.unitName || row.secondaryUnitName || '个';
              row.secondaryUnitName = createdProduct.unitName || row.secondaryUnitName || '个';
            }
          }
        });

        let productData = this.productData
          .filter((c) => c.quantity > 0 && c.productId)
          .map(({ _rowKey, isNewProduct, newProductName, newProductCategoryId, ...rest }) => rest);

        if (productData.length <= 0) {
          MessagePlugin.error('请选择产品~');
          LoadingPlugin(false);
          return;
        }

        let warehouse = productData.filter((c) => c.warehouseId === null);
        if (warehouse.length > 0) {
          MessagePlugin.error('请选择仓库~');
          LoadingPlugin(false);
          return;
        }

        PurchaseOrder.save({
          purchaseOrder: Object.assign(this.form, {
            totalAmount: this.allFinalAmount
          }),
          type: this.type,
          purchaseOrderItemList: productData
        })
          .then((success) => {
            if (success) {
              MessagePlugin.success('保存成功~');
              this.clearForm();
              if (type === 'save') {
                this.closeWindow();
              }
            }
          })
          .finally(() => LoadingPlugin(false));
      };

      // 如果有新商品，先创建新商品，再保存订单
      if (newProducts.length > 0) {
        createNewProducts().then((createdProducts) => {
          saveOrderData(createdProducts);
        }).catch((error) => {
          MessagePlugin.error("创建商品失败~");
          LoadingPlugin(false);
        });
      } else {
        saveOrderData([]);
      }
    },

    clearForm() {
      this.form = {
        id: null,
        orderDate: manba().format('YYYY-MM-dd'),
        supplierId: null,
        remark: null,
        discountAmount: 0.0,
        discountRate: 0.0,
        finalAmount: 0.0
      };
      this.allFinalAmount = 0;
      this.productData = [newRow({ isNew: true })];
      this.supplierId = null;
    },

    adjustRows(type, index) {
      if (type === 'insert') {
        this.productData.splice(index + 1, 0, newRow({ isNew: true }));
      } else {
        this.productData.splice(index, 1);
      }
    },

    changeSupplier(value) {
      if (value == null || value === '') {
        this.form.supplierId = null;
        this.productData = [newRow({ isNew: true })];
        return;
      }
      const e = (this.supplierList || []).find((item) => String(item.id) === String(value));
      if (!e) return;
      if (e.id !== this.form.supplierId) {
        if (this.productData.length > 1) {
          DialogPlugin.confirm({
            header: '系统提示',
            body: `修改供货商后，将清除已选择的产品数据，确定修改？`,
            onConfirm: () => {
              this.productData = [newRow({ isNew: true })];
              this.form.supplierId = e.id;
              this.loadProductsBySupplier();
            }
          });
        } else {
          this.form.supplierId = e.id;
          this.productData = [newRow({ isNew: true })];
          this.loadProductsBySupplier();
        }
      }
    },

    loadProductsBySupplier() {
      if (this.form.supplierId) {
        Supplier.selectProduct(this.form.supplierId)
          .then(({ data }) => {
            this.productList = data || [];
            if (!this.form.id) {
              this.productData = [newRow({ isNew: true })];
            }
          })
          .finally(() => {
            this.$nextTick(() => {
              this.$refs.ms?.$el?.querySelector('input')?.click();
              this.$refs.ms?.$el?.querySelector('input')?.select();
            });
          });
      }
    },

    changeDiscountRate() {
      this.form.discountRate = parseFloat(this.form.discountRate) || 0;
      this.form.discountAmount = (
        this.allFinalAmount *
        this.form.discountRate *
        0.01
      ).toFixed(2);
      this.form.finalAmount = (
        this.allFinalAmount - this.form.discountAmount
      ).toFixed(2);
    },
    changeDiscountAmount() {
      this.form.discountAmount = parseFloat(this.form.discountAmount) || 0;
      this.form.finalAmount = (
        this.allFinalAmount - this.form.discountAmount
      ).toFixed(2);
      this.form.discountRate =
        this.form.discountAmount === 0
          ? 0
          : ((this.form.discountAmount / this.allFinalAmount) * 100).toFixed(2);
    },
    changeFinalAmount() {
      this.form.finalAmount = parseFloat(this.form.finalAmount) || 0;
      this.form.discountAmount = (
        this.allFinalAmount - this.form.finalAmount
      ).toFixed(2);
      this.form.discountRate =
        this.form.finalAmount === 0
          ? 0
          : ((this.form.finalAmount / this.allFinalAmount) * 100).toFixed(2);
    },
    changeProductUnit(value, row) {
      const item = (row.auxiliaryUnitPrices || []).find((u) => String(u.unitId) === String(value));
      if (item) {
        switchUnit(row, item);
      }
    },

    updateQuantity(item) {
      item.secondaryQuantity = item.secondaryQuantity || 1;
      item.subtotal = (
        (item.secondaryQuantity *
          item.secondaryPrice *
          (100 - item.discountRate)) /
        100
      ).toFixed(2);
      item.discountAmount = (
        (item.secondaryQuantity * item.secondaryPrice * item.discountRate) /
        100
      ).toFixed(2);
      item.quantity = (
        item.secondaryQuantity * (item.conversionRate || 1)
      ).toFixed(2);
    },

    updatePrice(item) {
      item.secondaryPrice = item.secondaryPrice || 0.0;
      item.discountAmount = (
        (item.secondaryPrice * item.secondaryQuantity * item.discountRate) /
        100
      ).toFixed(2);
      item.subtotal = (
        item.secondaryPrice * item.secondaryQuantity -
        item.discountAmount
      ).toFixed(2);
    },

    updateDiscount(item) {
      item.discountRate = item.discountRate || 0.0;
      item.subtotal = (
        ((item.secondaryQuantity || 0) *
          item.secondaryPrice *
          (100 - item.discountRate || 0)) /
        100
      ).toFixed(2);
      item.discountAmount = (
        (item.secondaryQuantity || 0) * item.secondaryPrice -
        item.subtotal
      ).toFixed(2);
    },

    updateDiscountAmount(item) {
      item.discountAmount = item.discountAmount || 0.0;
      item.discountRate = (
        (item.discountAmount / (item.secondaryPrice * item.secondaryQuantity)) *
          100 || 0
      ).toFixed(2);
      item.subtotal = (
        item.secondaryPrice * item.secondaryQuantity -
        item.discountAmount
      ).toFixed(2);
    },

    updateSubtotal(item) {
      item.subtotal = item.subtotal || 0;
      item.secondaryPrice = (
        ((item.subtotal / (100 - item.discountRate)) * 100) /
        item.secondaryQuantity
      ).toFixed(2);
      item.discoutPrice = (item.secondaryPrice - item.subtotal).toFixed(2);
    },

    approved() {
      let ids = [this.form.id];
      DialogPlugin.confirm({
        header: '审核提示',
        body: `确认审核该订单?`,
        onConfirm: () => {
          PurchaseOrder.approved('已审核', ids).then(() => {
            MessagePlugin.success('操作成功~');
            this.closeWindow();
          });
        }
      });
    },

    closeWindow() {
      this.$store.commit('closeTabKey', this.$store.state.currentTab);
      this.$store.commit('newTab', 'PurchaseOrderList');
      this.$nextTick(() => {
        this.$store.commit('SET_TAB_DATA', { refresh: true });
      });
    }
  },
  beforeDestroy() {
    DialogPlugin.confirm({
      header: '系统提示',
      body: `确认?`,
      onConfirm: () => {}
    });
  },
  created() {
    LoadingPlugin(true);
    Promise.all([Supplier.select(), Warehouse.select(), ProductCategory.select()])
      .then((results) => {
        this.supplierList = results[0].data || [];
        this.warehouseList = results[1].data || [];
        this.productCategoryList = results[2].data || [];
        if (this.warehouseList != null) {
          this.warehouseId = this.warehouseList.find(
            (val) => val.systemDefault || val.isDefault
          )?.id;
        }
        if (this.orderId) {
          PurchaseOrder.load(this.orderId).then(
            ({ data: { purchaseOrder, purchaseOrderItemList } }) => {
              if (purchaseOrder) {
                CopyObj(this.form, purchaseOrder);
                this.supplierId = purchaseOrder.supplierId;
                if ('copy' === this.type) {
                  this.form.id = null;
                }
              }
              this.loadProductsBySupplier();
              this.productData = (purchaseOrderItemList || []).map((row) =>
                newRow({ ...row, isNew: false })
              );
              this.productData.push(newRow({ isNew: true }));
            }
          );
        }
      })
      .finally(() => LoadingPlugin(false));
  }
};
</script>
<style scoped>

.product-img {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
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
</style>
