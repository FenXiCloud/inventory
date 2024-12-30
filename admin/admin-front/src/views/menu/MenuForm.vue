<template>
  <div class="m-16px">
    <Form ref="form" :model="model" :mode="mode"  :rules="validationRules">
      <FormItem label="名称" required prop="name">
        <Input placeholder="请输入菜单名称" v-model="model.name"/>
      </FormItem>
      <FormItem label="组件名称" prop="component">
        <Input  placeholder="请输入组件名称" v-model="model.component"/>
      </FormItem>
      <FormItem label="菜单模块" prop="menuModule">
        <Radio :disabled="model.parentId || model.id" v-model="model.menuModule" :datas="{MERCHANT:'集团'}"></Radio>
      </FormItem>
      <FormItem label="菜单分组" prop="menuGroup">
        <Radio :disabled="model.parentId || model.id" v-model="model.menuGroup" :datas="{MERCHANT:'集团菜单'}"></Radio>
      </FormItem>
      <FormItem label="类型" prop="menuType">
        <Radio v-model="model.menuType" :datas="{MENU:'菜单',FUNCTION:'功能'}"></Radio>
      </FormItem>
      <FormItem label="权限控制" prop="requireAuth">
        <Radio v-model="model.requireAuth" :datas="[{title:'是',key:true},{title:'否',key:false}]"></Radio>
      </FormItem>
      <FormItem label="图标" prop="iconCls">
        <Input placeholder="请输入图标" v-model="model.iconCls"/>
      </FormItem>
      <FormItem label="显示位置" prop="pos">
        <Input placeholder="请输入显示位置" v-model="model.pos"/>
      </FormItem>
    </Form>
  </div>
  <div class="layui-layer-btn layui-layer-btn-r">
    <Button  @click="$emit('close')" :loading="loading">
      取消
    </Button>
    <Button  color="primary" @click="confirm" :loading="loading">
      保存
    </Button>
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
import {message} from "heyui.ext";
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
      mode:'twocolumn',
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
      }
    }
  },
  methods: {
    confirm() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        Menu.save(this.model).then(() => {
          message("保存成功~");
          this.$emit('success');
        }).finally(() => this.loading = false);
      }
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
