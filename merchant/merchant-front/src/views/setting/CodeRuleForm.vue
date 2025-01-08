<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <Form ref="form" :model="model" :rules="validationRules" :labelWidth="120">
        <FormItem label="规则名称" required prop="name">
          <Input placeholder="请输入规则名称" v-model="model.name"/>
        </FormItem>
        <FormItem label="规则前缀" required prop="prefix">
          <Input placeholder="请输入规则前缀" v-model="model.prefix"/>
        </FormItem>
        <FormItem label="格式化" required prop="format">
          <Input v-model="model.format"/>
        </FormItem>
        <FormItem label="流水号位数" required prop="serialNumberLength">
          <Input placeholder="请输入流水号位数" v-model="model.serialNumberLength"/>
        </FormItem>
        <FormItem label="起始值" required prop="startValue">
          <Input placeholder="起始值" v-model="model.startValue"/>
        </FormItem>
        <FormItem label="流水号清零" required prop="resetPeriod">
          <Input placeholder="流水号清零" v-model="model.resetPeriod"/>
        </FormItem>
        <FormItem label="单据类型" required prop="documentType">
          <Input placeholder="单据类型" v-model="model.documentType"/>
        </FormItem>
      </Form>
    </div>
    <div class="modal-column-right">
      <Button icon="fa fa-save" style="justify-content: right" color="primary" @click="confirm" :loading="loading">
        保存
      </Button>
    </div>
  </div>
</template>

<script>

import CodeRule from "@js/api/setting/CodeRule";
import {message} from "heyui.ext";
import {CopyObj} from "@common/utils";
import manba from "manba";

export default {
  name: "CodeRuleForm",
  emits: {
    close: null,
    success: null
  },
  props: {
    CodeRule: Object,
  },
  data() {
    return {
      loading: false,
      merchantList: [],
      areaList: [],
      levelList: [],
      warehouseList: [],
      model: {
        id: null,
        name: null,
        prefix: null,
        code: null,
        format: null,
        serialNumberLength: null,
        startValue: null,
        resetPeriod: null,
        documentType: null,
      },
      validationRules: {}
    }
  },
  methods: {
    confirm() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        this.model.startDate = manba(this.model.startDate).format("YYYY-MM")
        CodeRule.save(this.model).then(() => {
          message("保存成功~");
          this.$emit('success');
        }).finally(() => this.loading = false);
      }
    },
    init() {
      // this.loading = true;
      // Promise.all([
      //   Area.select(),
      //   Level.select(),
      //   Warehouse.select(),
      // ]).then((results) => {
      //   this.areaList = results[0].data
      //   this.levelList = results[1].data
      //   this.warehouseList = results[2].data
      // }).finally(() => this.loading = false);
    }
  },
  created() {
    this.init();
    CopyObj(this.model, this.CodeRule);
  }
}
</script>
