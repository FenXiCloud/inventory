<template>
  <div class="modal-column" style="background: #f5f5f5">
    <div class="modal-column-full-body">
      <div class="flex pt-15px">
        <t-form ref="form" class="mr-10px" :data="model" label-width="90px" layout="inline">
          <t-form-item label="产品编码" name="code">
            <t-input placeholder="产品编码" v-model="model.code" disabled/>
          </t-form-item>
          <t-form-item label="产品名称" name="name">
            <t-input placeholder="产品名称" v-model="model.name" :maxlength="64" disabled/>
          </t-form-item>
          <t-form-item label="产品分类" name="productCategoryName">
            <t-input placeholder="产品分类" v-model="model.productCategoryName" :maxlength="64" disabled/>
          </t-form-item>
          <t-form-item label="规格" name="specification">
            <t-input placeholder="请输入规格" v-model="model.specification" :maxlength="120" disabled/>
          </t-form-item>
          <t-form-item label="计量单位" name="unitName">
            <t-input placeholder="计量单位" v-model="model.unitName" :maxlength="64" disabled/>
          </t-form-item>
        </t-form>
      </div>
      <div>
        <t-form ref="priceForm" class="mr-10px" :data="model" label-width="90px" layout="inline">
          <t-form-item label="预计采购价">
            <vxe-input placeholder="请输入预计采购价" v-model.number="model.purchasePrice" type="float" min="0"/>
          </t-form-item>
          <t-form-item label="最高采购价">
            <vxe-input placeholder="请输入最高采购价" v-model="model.maxPurchasePrice" type="float" min="0"/>
          </t-form-item>
          <t-form-item label="最近采购价">
            <vxe-input placeholder="请输入最近采购价" v-model="model.recentlyPurchasePrice" disabled type="float" min="0"/>
          </t-form-item>

          <t-form-item label="零售客户价">
            <vxe-input placeholder="请输入零售客户价" v-model="model.retailCustomerPrice" type="float" min="0"/>
          </t-form-item>
          <t-form-item label="批发客户价">
            <vxe-input placeholder="请输入批发客户价" v-model="model.wholesaleCustomerPrice" type="float" min="0"/>
          </t-form-item>
          <t-form-item label="VIP客户价">
            <vxe-input placeholder="请输入VIP客户价" v-model="model.vipCustomerPrice" type="float" min="0"/>
          </t-form-item>

          <t-form-item label="最低销售价">
            <vxe-input placeholder="请输入最低销售价" v-model="model.minSalesPrice" type="float" min="0"/>
          </t-form-item>
          <t-form-item label="最近销售价">
            <vxe-input placeholder="请输入最近销售价" v-model="model.recentlySalesPrice" disabled type="float" min="0"/>
          </t-form-item>
        </t-form>
      </div>
    </div>
    <div class="flex justify-between py-5px px-5px bg-white-color">
      <t-button class="ml-10px" @click="$emit('close')" :loading="loading">
        取 消
      </t-button>
      <t-button class="mr-10px" theme="primary" @click="save" :loading="loading">
        保 存
      </t-button>
    </div>
    <t-loading :loading="loading" text="运行中" fullscreen/>
  </div>
</template>

<script>
/**
 * @功能描述: 产品FORM
 * @创建时间: 2024年05月06日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import {MessagePlugin} from "tdesign-vue-next";
import PriceRecord from "@js/api/basic/PriceRecord";

export default {
  name: "ProductPriceForm",
  emits: ['close'],
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
  methods: {
    save() {
      this.loading = true;
      PriceRecord.productSave({
        product: this.model,
      }).then(() => {
        MessagePlugin.success("保存成功~");
        this.$emit('close');
      }).finally(() => this.loading = false);
    },
  },
  created() {
    this.model = this.entity;
  }
}
</script>
<style lang="less">
.goods-form .t-form .t-form-item {
  padding-bottom: 15px !important;
}
</style>
