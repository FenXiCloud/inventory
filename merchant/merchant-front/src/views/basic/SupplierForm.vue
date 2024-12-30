<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <Form ref="form" :model="model" :rules="validationRules" mode="twocolumn" :label-width="110">
        <FormItem label="编码" required prop="code">
          <Input placeholder="请输入编码" :disabled="model.id" v-model="model.code"/>
        </FormItem>
        <FormItem label="名称" required prop="name">
          <Input placeholder="请输入名称" v-model="model.name"/>
        </FormItem>
        <FormItem label="联系人" required prop="linkman">
          <Input placeholder="联系人" v-model="model.linkman"/>
        </FormItem>
        <FormItem label="电话" required prop="phone">
          <Input placeholder="电话" v-model="model.phone"/>
        </FormItem>
        <FormItem label="货商分类" required prop="vendorsCategoryId" single>
          <Select :datas="vendorsCategoryList" keyName="id" titleName="name" v-model="model.vendorsCategoryId"
                  placeholder="请选择货商分类"/>
        </FormItem>
        <FormItem label="是否启用" prop="enabled" single>
          <Radio v-model="model.enabled" dict="enableRadios"/>
        </FormItem>
        <FormItem label="地址" required prop="address" single>
          <Input placeholder="地址" v-model="model.address"/>
        </FormItem>
<!--        <FormItem label="登录账号" required prop="username" single>-->
<!--          <Input placeholder="登录账号,长度<8" maxlength="8" :disabled="model.id" v-model.trim="model.username"/>-->
<!--          <span class="red-color">初始密码:123456</span>-->
<!--        </FormItem>-->
      </Form>
    </div>
    <div class="modal-column-between">
      <Button @click="$emit('close')" :loading="loading">
        取消
      </Button>
      <Button color="primary" @click="confirm" :loading="loading">
        保存
      </Button>
    </div>
  </div>
</template>

<script>
/**
 * @功能描述: 供货商FORM
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import Supplier from "@js/api/Supplier";
import {message} from "heyui.ext";
import {CopyObj} from "@common/utils";
import SupplierCategory from "@js/api/SupplierCategory";

export default {
  name: "SupplierForm",
  props: {
    entity: Object,
  },
  data() {
    return {
      loading: false,
      supplierCategoryList: [],
      model: {
        id: null,
        code: null,
        name: null,
        linkman: null,
        phone: null,
        supplierCategoryId: null,
        address: null,
        enabled:true,
      },
      validationRules: {}
    }
  },
  methods: {
    confirm() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        Supplier.save(this.model).then(() => {
          message("保存成功~");
          this.$emit('success');
        }).finally(() => this.loading = false);
      }
    }
  },
  created() {
    CopyObj(this.model, this.entity);
    Promise.all([
      SupplierCategory.select(),
    ]).then((results) => {
      this.supplierCategoryList = results[0].data;
    }).finally(() => this.loading = false);
  }
}
</script>
