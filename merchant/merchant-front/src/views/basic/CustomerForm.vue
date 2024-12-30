<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <Form ref="form" :model="model" :rules="validationRules" mode="twocolumn" :label-width="110">
        <FormItem label="客户编码" prop="code" single>
          <Input placeholder="编码（不填写系统自动生成）" v-model="model.code"/>
        </FormItem>
        <FormItem label="客户名称" required prop="name" single>
          <Input placeholder="请输入客户名称" v-model="model.name"/>
        </FormItem>
        <FormItem label="客户分类" required prop="customersCategoryId">
          <Select :datas="customerCategoryList" keyName="id" titleName="name" v-model="model.customerCategoryId"
                  placeholder="请选择客户分类" :deletable="false"/>
        </FormItem>
        <FormItem label="客户等级" required prop="customersLevelId">
          <Select :datas="customerLevelList" keyName="id" titleName="name" v-model="model.customerLevelId"
                  placeholder="请选择客户等级" :deletable="false"/>
        </FormItem>
        <FormItem label="联系人" prop="linkman">
          <Input placeholder="联系人" v-model.trim="model.linkman"/>
        </FormItem>
        <FormItem label="电话" prop="phone">
          <Input placeholder="电话" v-model.trim="model.phone"/>
        </FormItem>
        <FormItem label="客户描述" prop="remark" single>
          <Textarea v-wordcount="150" rows="3" placeholder="客户描述" v-model="model.remark"/>
        </FormItem>
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
 * @功能描述: 客户FORM
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import Customer from "@js/api/Customer";
import {message} from "heyui.ext";
import {CopyObj} from "@common/utils";
import CustomerCategory from "@js/api/CustomerCategory";
import CustomerLevel from "@js/api/CustomerLevel";

export default {
  name: "CustomerForm",
  computed: {},
  props: {
    entity: Object,
  },
  data() {
    return {
      loading: false,
      customerCategoryList: [],
      customerLevelList: [],
      model: {
        id: null,
        code: null,
        name: null,
        linkman: null,
        phone: null,
        customerCategoryId: null,
        customerLevelId: null,
        remark: null
      },
      validationRules: {}
    }
  },
  methods: {
    confirm() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        Customer.save(this.model).then(() => {
          message("保存成功~");
          this.$emit('success');
        }).finally(() => this.loading = false);
      }
    },
  },
  created() {
    CopyObj(this.model, this.entity);
    Promise.all([
      CustomerCategory.select(),
      CustomerLevel.select()
    ]).then((results) => {
      this.customerCategoryList = results[0].data || [];
      this.customerLevelList = results[1].data || [];
    }).finally(() => this.loading = false);
  }
}
</script>
