<template>
  <t-form
      ref="form"
      :data="formData"
      :rules="rules"
      label-width="100px"
      @submit="onSubmit"
  >
    <t-row :gutter="16">
      <t-col :span="6">
        <t-form-item label="调拨日期" name="transferDate">
          <t-date-picker v-model="formData.transferDate" style="width: 100%" />
        </t-form-item>
      </t-col>
      <t-col :span="6">
        <t-form-item label="商品" name="productId">
          <t-select
              v-model="formData.productId"
              :options="productList"
              :keys="{ value: 'id', label: 'name' }"
              filterable
              placeholder="请选择商品"
              @change="onProductChange"
          />
        </t-form-item>
      </t-col>
    </t-row>

    <t-row :gutter="16">
      <t-col :span="6">
        <t-form-item label="源仓库" name="fromWarehouseId">
          <t-select
              v-model="formData.fromWarehouseId"
              :options="warehouseList"
              :keys="{ value: 'id', label: 'name' }"
              filterable
              placeholder="请选择源仓库"
              @change="onFromWarehouseChange"
          />
        </t-form-item>
      </t-col>
      <t-col :span="6">
        <t-form-item label="源货位" name="fromLocationId">
          <t-select
              v-model="formData.fromLocationId"
              :options="fromLocationList"
              :keys="{ value: 'id', label: 'name' }"
              filterable
              placeholder="请选择源货位"
          />
        </t-form-item>
      </t-col>
    </t-row>

    <t-row :gutter="16">
      <t-col :span="6">
        <t-form-item label="目标仓库" name="toWarehouseId">
          <t-select
              v-model="formData.toWarehouseId"
              :options="warehouseList"
              :keys="{ value: 'id', label: 'name' }"
              filterable
              placeholder="请选择目标仓库"
              @change="onToWarehouseChange"
          />
        </t-form-item>
      </t-col>
      <t-col :span="6">
        <t-form-item label="目标货位" name="toLocationId">
          <t-select
              v-model="formData.toLocationId"
              :options="toLocationList"
              :keys="{ value: 'id', label: 'name' }"
              filterable
              placeholder="请选择目标货位"
          />
        </t-form-item>
      </t-col>
    </t-row>

    <t-row :gutter="16">
      <t-col :span="6">
        <t-form-item label="调拨数量" name="quantity">
          <t-input-number
              v-model="formData.quantity"
              :min="0"
              :decimal-places="qtyDp"
              placeholder="请输入调拨数量"
              style="width: 100%"
          />
        </t-form-item>
      </t-col>
      <t-col :span="6">
        <t-form-item label="整件数">
          <t-input-number
              v-model="formData.caseQuantity"
              :min="0"
              :decimal-places="2"
              placeholder="整件数（可选）"
              style="width: 100%"
          />
        </t-form-item>
      </t-col>
    </t-row>

    <t-form-item label="备注" name="remark">
      <t-textarea v-model="formData.remark" placeholder="可选" />
    </t-form-item>

    <t-form-item>
      <t-space>
        <t-button v-auth="'locationTransfer:edit'" theme="primary" type="submit">保 存</t-button>
        <t-button variant="outline" @click="onCancel">取 消</t-button>
      </t-space>
    </t-form-item>
  </t-form>
</template>

<script>
import {MessagePlugin} from "tdesign-vue-next";
import LocationTransfer from "@js/api/inventory/LocationTransfer";
import Warehouse from "@js/api/basic/Warehouse";
import WarehouseLocation from "@js/api/basic/WarehouseLocation";
import Product from "@js/api/basic/Product";

export default {
  name: "LocationTransferForm",
  props: {
    data: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      formData: {
        id: null,
        transferDate: null,
        fromWarehouseId: null,
        fromLocationId: null,
        toWarehouseId: null,
        toLocationId: null,
        productId: null,
        quantity: null,
        caseQuantity: null,
        remark: ''
      },
      warehouseList: [],
      productList: [],
      fromLocationList: [],
      toLocationList: [],
      rules: {
        transferDate: [{required: true, message: '请选择调拨日期', trigger: 'change'}],
        productId: [{required: true, message: '请选择商品', trigger: 'change'}],
        fromWarehouseId: [{required: true, message: '请选择源仓库', trigger: 'change'}],
        fromLocationId: [{required: true, message: '请选择源货位', trigger: 'change'}],
        toWarehouseId: [{required: true, message: '请选择目标仓库', trigger: 'change'}],
        toLocationId: [{required: true, message: '请选择目标货位', trigger: 'change'}],
        quantity: [{required: true, message: '请输入调拨数量', trigger: 'blur'}]
      }
    };
  },
  watch: {
    data: {
      handler(val) {
        if (val) {
          this.formData = Object.assign({}, val);
          if (this.formData.fromWarehouseId) {
            this.loadFromLocations(this.formData.fromWarehouseId);
          }
          if (this.formData.toWarehouseId) {
            this.loadToLocations(this.formData.toWarehouseId);
          }
        } else {
          this.resetForm();
        }
      },
      immediate: true
    }
  },
  methods: {
    resetForm() {
      this.formData = {
        id: null,
        transferDate: null,
        fromWarehouseId: null,
        fromLocationId: null,
        toWarehouseId: null,
        toLocationId: null,
        productId: null,
        quantity: null,
        caseQuantity: null,
        remark: ''
      };
      this.fromLocationList = [];
      this.toLocationList = [];
    },
    loadWarehouses() {
      Warehouse.select().then(({data}) => {
        this.warehouseList = data || [];
      });
    },
    loadProducts() {
      Product.select().then(({data}) => {
        this.productList = data || [];
      });
    },
    onFromWarehouseChange(warehouseId) {
      this.formData.fromLocationId = null;
      if (warehouseId) {
        this.loadFromLocations(warehouseId);
      } else {
        this.fromLocationList = [];
      }
    },
    onToWarehouseChange(warehouseId) {
      this.formData.toLocationId = null;
      if (warehouseId) {
        this.loadToLocations(warehouseId);
      } else {
        this.toLocationList = [];
      }
    },
    loadFromLocations(warehouseId) {
      WarehouseLocation.listByWarehouse(warehouseId).then(({data}) => {
        this.fromLocationList = data || [];
      });
    },
    loadToLocations(warehouseId) {
      WarehouseLocation.listByWarehouse(warehouseId).then(({data}) => {
        this.toLocationList = data || [];
      });
    },
    onProductChange(productId) {
      // 可以根据商品自动填充默认货位
      if (productId) {
        const product = this.productList.find(p => p.id === productId);
        if (product) {
          // 如果商品有默认整件货位，自动填充到源货位
          if (product.defaultWholeLocationId) {
            this.formData.fromLocationId = product.defaultWholeLocationId;
          }
          // 如果商品有默认零货货位，自动填充到目标货位
          if (product.defaultZeroLocationId) {
            this.formData.toLocationId = product.defaultZeroLocationId;
          }
        }
      }
    },
    onSubmit({validateResult}) {
      if (validateResult !== true) return;

      const api = this.formData.id ? LocationTransfer.update : LocationTransfer.save;
      api(this.formData).then(() => {
        MessagePlugin.success('保存成功');
        this.$emit('success');
      });
    },
    onCancel() {
      this.$emit('cancel');
    }
  },
  created() {
    this.loadWarehouses();
    this.loadProducts();
  }
};
</script>

<style scoped>
</style>
