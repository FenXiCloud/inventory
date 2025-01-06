<template>
  <div class="m-16px">
    <Form ref="form" :model="model" :rules="validationRules" :labelWidth="120" >
      <FormItem label="仓库编码" required prop="code">
        <Input placeholder="请输入仓库编码" v-model="model.code"/>
      </FormItem>
      <FormItem label="仓库名称" required prop="name">
        <Input placeholder="请输入仓库名称" v-model="model.name"/>
      </FormItem>
      <FormItem label="仓库地址"  prop="address">
        <Input placeholder="请输入仓库地址" v-model="model.address"/>
      </FormItem>
      <FormItem label="是否启用" prop="enabled" single>
        <Radio v-model="model.enabled" dict="enableRadios"/>
      </FormItem>
      <FormItem label="是否默认" prop="isDefault" single>
        <Radio v-model="model.systemDefault" dict="defaultRadios"/>
      </FormItem>
    </Form>
  </div>
  <div class="layui-layer-btn layui-layer-btn-r">
    <Button @click="$emit('close')" :loading="loading">
      取消
    </Button>
    <Button color="primary" @click="confirm" :loading="loading">
      保存
    </Button>
  </div>
</template>

<script>
import Warehouse from "@js/api/basic/Warehouse";
import {message} from "heyui.ext";
import {CopyObj} from "@common/utils";

export default {
  name: "WarehouseForm",
  props: {
    warehouse: Object,
  },
  data() {
    return {
      loading: false,
      model: {
        id: null,
        name: null,
        code:null,
        address:null,
        enabled:true,
        systemDefault: 0,
      },
      validationRules: {
      }
    }
  },
  methods: {
    confirm() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        Warehouse.save(this.model).then(() => {
          message("保存成功~");
          this.$emit('success');
        }).finally(() => this.loading = false);
      }
    },
  },
  created() {
    CopyObj(this.model, this.warehouse);
  }
}
</script>

<style scoped>

</style>
