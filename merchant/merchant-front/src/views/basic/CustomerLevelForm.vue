<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <Form ref="form" :model="model" :rules="validationRules" mode="single">
        <FormItem label="名称" required prop="name">
          <Input placeholder="请输入名称" v-model="model.name"/>
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
 * @功能描述: 客户级别FORM
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import CustomerLevel from "@js/api/basic/CustomerLevel";
import {message} from "heyui.ext";
import {CopyObj} from "@common/utils";

export default {
  name: "CustomerLevelForm",
  props: {
    entity: Object,
  },
  data() {
    return {
      loading: false,
      model: {
        id: null,
        name: null,
      },
      validationRules: {}
    }
  },
  methods: {
    confirm() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        CustomerLevel.save(this.model).then(() => {
          message("保存成功~");
          this.$emit('success');
        }).finally(() => this.loading = false);
      }
    }
  },
  created() {
    CopyObj(this.model, this.entity);
  }
}
</script>
