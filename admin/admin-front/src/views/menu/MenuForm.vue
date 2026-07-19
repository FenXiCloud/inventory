<template>
  <div class="m-16px">
    <t-form ref="form" :data="model" :rules="rules" label-align="right">
      <t-form-item label="名称" name="name">
        <t-input placeholder="请输入菜单名称" v-model="model.name"/>
      </t-form-item>
      <t-form-item label="组件名称" name="component">
        <t-input placeholder="请输入组件名称" v-model="model.component"/>
      </t-form-item>
      <t-form-item label="菜单模块" name="menuModule">
        <t-radio-group :disabled="model.parentId || model.id" v-model="model.menuModule">
          <t-radio value="MERCHANT">集团</t-radio>
        </t-radio-group>
      </t-form-item>
      <t-form-item label="菜单分组" name="menuGroup">
        <t-radio-group :disabled="model.parentId || model.id" v-model="model.menuGroup">
          <t-radio value="MERCHANT">集团菜单</t-radio>
        </t-radio-group>
      </t-form-item>
      <t-form-item label="类型" name="menuType">
        <t-radio-group v-model="model.menuType">
          <t-radio value="MENU">菜单</t-radio>
          <t-radio value="FUNCTION">功能</t-radio>
        </t-radio-group>
      </t-form-item>
      <t-form-item label="权限控制" name="requireAuth">
        <t-radio-group v-model="model.requireAuth">
          <t-radio :value="true">是</t-radio>
          <t-radio :value="false">否</t-radio>
        </t-radio-group>
      </t-form-item>
      <t-form-item label="图标" name="iconCls">
        <t-input placeholder="请输入图标" v-model="model.iconCls"/>
      </t-form-item>
      <t-form-item label="显示位置" name="pos">
        <t-input placeholder="请输入显示位置" v-model="model.pos"/>
      </t-form-item>
    </t-form>
  </div>
  <div class="dialog-footer">
    <t-button @click="$emit('close')" :loading="loading">取消</t-button>
    <t-button theme="primary" @click="confirm" :loading="loading">保存</t-button>
  </div>
</template>

<script>
/**
 * @功能描述: 菜单FORM
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import Menu from "@js/api/Menu";
import {MessagePlugin} from "tdesign-vue-next";
import {CopyObj} from "@common/utils";

export default {
  name: "MenuForm",
  emits: {
    close: null,
    success: null
  },
  props: {
    menu: Object,
    parent: Object,
  },
  data() {
    return {
      opened: true,
      loading: false,
      merchantList: [],
      model: {
        id: null,
        name: null,
        parentId: null,
        component: null,
        iconCls: null,
        requireAuth: true,
        pos: 0,
        menuType: 'MENU',
        menuGroup: 'MERCHANT',
        menuModule:'MERCHANT'
      },
      rules: {
        name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
      }
    }
  },
  methods: {
    confirm() {
      this.$refs.form.validate().then((result) => {
        if (result === true) {
          this.loading = true;
          Menu.save(this.model).then(() => {
            MessagePlugin.success("保存成功~");
            this.$emit('success');
          }).finally(() => this.loading = false);
        }
      });
    },
  },
  created() {
    CopyObj(this.model, this.menu);
    if (this.parent) {
      this.model.parentId = this.parent.id;
      this.model.menuGroup = this.parent.menuGroup;
      this.model.menuType = this.parent.menuType;
      this.model.menuModule = this.parent.menuModule;
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
