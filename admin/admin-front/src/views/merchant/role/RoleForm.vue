<template>
  <div class="m-16px">
    <t-form ref="form" :data="model" :rules="rules" label-align="right">
      <t-form-item label="角色名称" name="name">
        <t-input placeholder="请输入角色名称" v-model="model.name"/>
      </t-form-item>
    </t-form>
    <div class="dialog-footer">
      <t-button @click="$emit('close')" :loading="loading">取消</t-button>
      <t-button theme="primary" @click="confirm" :loading="loading">保存</t-button>
    </div>
  </div>
</template>

<script>
import Role from "@js/api/Role";
import {MessagePlugin} from "tdesign-vue-next";
import {CopyObj} from "@common/utils";

export default {
  name: "RoleForm",
  props: {
    entity: Object,
    merchant: Object,
    type: Number,
  },
  data() {
    return {
      loading: false,
      model: {
        id: null,
        name: null,
        merchantId: null,
        enabled: true,
        systemDefault: false,
        type: 0
      },
      rules: {
        name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
      }
    }
  },
  methods: {
    confirm() {
      this.$refs.form.validate().then((result) => {
        if (result === true) {
          this.loading = true;
          Role.save(this.model).then(() => {
            MessagePlugin.success("保存成功~");
            this.$emit('success');
          }).finally(() => this.loading = false);
        }
      });
    }
  },
  created() {
    CopyObj(this.model, this.entity);
    this.model.merchantId = this.merchant.id;
    if (this.type) {
      this.model.type = this.type;
    }
  }
}
</script>

<style>
.dialog-footer {
  text-align: right;
  padding: 12px 16px;
  border-top: 1px solid #e7e7e7;
}
.dialog-footer .t-button {
  margin-left: 8px;
}
</style>
