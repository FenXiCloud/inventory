<template>
  <div class="modal-column" style="background: #f5f5f5">
    <div class="modal-column-full-body">
      <div class="flex pt-15px">
        <Form ref="form" class="mr-10px" :model="model" mode="twocolumn" :label-width="90">
          <FormItem label="商品编码" prop="code">
            <Input placeholder="商品编码" v-model="model.code" disabled/>
          </FormItem>
          <FormItem label="商品名称" prop="name">
            <Input placeholder="商品名称" v-model="model.name" maxlength="64" disabled/>
          </FormItem>
          <FormItem label="商品分类" prop="productCategoryName">
            <Input placeholder="商品分类" v-model="model.productCategoryName" maxlength="64" disabled/>
          </FormItem>
          <FormItem label="规格" prop="specification">
            <Input placeholder="请输入规格" v-model="model.specification" maxlength="120" disabled />
          </FormItem>
          <FormItem label="计量单位" prop="unitName">
            <Input placeholder="计量单位" v-model="model.unitName" maxlength="64" disabled/>
          </FormItem>
          <div style="clear: both"></div>
        </Form>
      </div>
      <div>
        <Form ref="form" class="mr-10px" :model="model" mode="threecolumn" :label-width="90">
          <FormItem label="预计采购价">
            <vxe-input placeholder="请输入预计采购价" v-model.number="model.purchasePrice" type="float" min="0"/>
          </FormItem>
          <FormItem label="最高采购价">
            <vxe-input placeholder="请输入最高采购价" v-model="model.maxPurchasePrice" type="float" min="0"/>
          </FormItem>
          <FormItem label="最近采购价">
            <vxe-input placeholder="请输入最近采购价" v-model="model.recentlyPurchasePrice" disabled type="float" min="0"/>
          </FormItem>

          <FormItem label="零售客户价">
            <vxe-input placeholder="请输入零售客户价" v-model="model.retailCustomerPrice" type="float" min="0"/>
          </FormItem>
          <FormItem label="批发客户价">
            <vxe-input placeholder="请输入批发客户价" v-model="model.wholesaleCustomerPrice" type="float" min="0"/>
          </FormItem>
          <FormItem label="VIP客户价">
            <vxe-input placeholder="请输入VIP客户价" v-model="model.vipCustomerPrice" type="float" min="0"/>
          </FormItem>

          <FormItem label="最低销售价">
            <vxe-input placeholder="请输入最低销售价" v-model="model.minSalesPrice" type="float" min="0"/>
          </FormItem>
          <FormItem label="最近销售价">
            <vxe-input placeholder="请输入最近销售价" v-model="model.recentlySalesPrice" disabled type="float" min="0"/>
          </FormItem>

          <div style="clear: both"></div>
        </Form>
      </div>
    </div>
    <div class="flex justify-between py-5px px-5px bg-white-color">
      <Button class="ml-10px" @click="$emit('close')" :loading="loading">
        取 消
      </Button>
      <Button class="mr-10px" color="primary" @click="save" :loading="loading">
        保 存
      </Button>
    </div>
    <Loading text="运行中" :loading="loading"></Loading>
  </div>
</template>

<script>
/**
 * @功能描述: 商品FORM
 * @创建时间: 2024年05月06日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import {confirm, message} from "heyui.ext";
import {CopyObj} from "@common/utils";
import Product from "@js/api/basic/Product";
import ProductCategory from "@js/api/basic/ProductCategory";
import CustomerLevel from "@js/api/basic/CustomerLevel";
import Unit from "@js/api/basic/Unit";
import {OssUpload} from "@js/api/App";
import PriceRecord from "@js/api/basic/PriceRecord";

export default {
  name: "ProductForm",
  props: {
    entity: Object,
  },
  data() {
    return {
      loading: false,
      model: {
        id: null,
        code: null,
        name: null,
        productCategoryId: null,
        imgPath: null,
        unitId: null,
        enableMultiUnit: false,
        auxiliaryUnitPrices: [],
        specification: null,
        sort: 0,
        remarks: null,
        enabled: true,
      },
    }
  },
  watch: {
  },
  computed: {
  },
  methods: {
    save() {
      this.loading = true;
      PriceRecord.productSave({
        product: this.model,
      }).then(() => {
        message("保存成功~");
        this.$emit('close');
      }).finally(() => this.loading = false);
    },
  },
  created() {
    // console.log("this.entity",this.entity)
    this.model = this.entity;
    // console.log("this.model",this.model)
    // CopyObj(this.model, this.entity);

  }
}
</script>
<style lang="less">
.goods-form .h-form .h-form-item {
  padding-bottom: 15px !important;
}
</style>
