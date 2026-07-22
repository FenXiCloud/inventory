<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <t-form ref="form" :data="model" :rules="validationRules" layout="vertical" label-align="top">
        <t-form-item label="角色名称" name="name">
          <t-input placeholder="请输入角色名称" v-model="model.name"/>
        </t-form-item>
      </t-form>
    </div>
    <div class="modal-column-between">
      <t-button variant="outline" :loading="loading" @click="$emit('close')">取消</t-button>
      <t-button theme="primary" :loading="loading" @click="confirm">保存</t-button>
    </div>
  </div>
</template>

<script>
import Role from "@js/api/setting/Role";
import {MessagePlugin} from "tdesign-vue-next";
import {CopyObj} from "@common/utils";

export default {
  name: "RoleForm",
  props: {
    entity: Object,
  },
  data() {
    return {
      loading: false,
      model: {
        id: null,
        name: null,
        accountBookId: null,
        systemDefault: false,
      },
      validationRules: {
        name: [{ required: true, message: '请输入角色名称', type: 'error', trigger: 'blur' }]
      }
    }
  },
  methods: {
    confirm() {
      this.$refs.form.validate().then((result) => {
        if (result !== true) return;
        this.loading = true;
        Role.save(this.model).then(() => {
          MessagePlugin.success("保存成功~");
          this.$emit('success');
        }).finally(() => this.loading = false);
      }).catch(() => {});
    }
  },
  created() {
    CopyObj(this.model, this.entity);
  }
}
</script>
