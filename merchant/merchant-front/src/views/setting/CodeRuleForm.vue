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
          <Select v-if="documentTypes.includes(model.documentType)"   placeholder="类别编码" v-model="model.format"    @change="formatValueChange" :datas="codeBusinessValueList"/>
          <Select v-else  placeholder="请选择日期格式" v-model="model.format"    @change="formatValueChange" :datas="formatValueList"/>
        </FormItem>
        <FormItem label="流水号位数" required prop="serialNumberLength">
          <Select placeholder="请输入流水号位数" v-model="model.serialNumberLength"  @change="serialNumberLengthValueChange" :datas="serialNumberLengthValueList"/>
        </FormItem>
        <FormItem label="起始值" required prop="startValue">
          <NumberInput  placeholder="起始值" v-model="model.startValue" @blur="handleBlurForStartValue"  :min="0"/>
        </FormItem>
        <FormItem label="流水号清零" required prop="resetPeriod">
          <Select placeholder="流水号清零" v-model="model.resetPeriod" :datas="resetPeriodValueList"/>
        </FormItem>
        <FormItem label="单据类型" required prop="documentType">
          <Select placeholder="单据类型" v-model="model.documentType" :disabled = "documentTypeDisabled"  :datas="documentTypeValueList"/>
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
      documentTypes: ['商品', '仓库', '客户', '供货商'],
      documentTypeDisabled: true,
      loading: false,
      merchantList: [],
      areaList: [],
      levelList: [],
      warehouseList: [],
      codeBusinessValueList:[{ title: '所属分类编码', key: '所属分类编码' }],
      formatValueList:[
        { title: 'yyyyMMdd', key: 'yyyyMMdd' }, { title: 'yyMMdd', key: 'yyMMdd'}
      ],
      resetPeriodValueList: [{ title: '日', key: '日' }, { title: '月', key: '月'}, { title: '季', key: '季' },{ title: '年', key: '年' }],
      serialNumberLengthValueList: [
        { title: '3位', key: '3' },
        { title: '4位', key: '4'},
        { title: '5位', key: '5' },
        { title: '6位', key: '6' },
        { title: '7位', key: '7' },
        { title: '8位', key: '8' },
        { title: '9位', key: '9' },
        { title: '10位', key: '10' }],
      documentTypeValueList:[
          {key: '采购订单',  title: '采购订单', type: 1},
          {key: '采购入库单',  title: '采购入库单', type: 1},
          {key: '采购退货单',  title: '采购退货单', type: 1},
          {key: '销售订单',  title: '销售订单', type: 1},
          {key: '销售出库单',  title: '销售出库单', type: 1},
          {key: '销售退货单',  title: '销售退货单', type: 1},
          {key: '调拨单',  title: '调拨单', type: 1},
          {key: '盘点单',  title: '盘点单', type: 1},
          {key: '其他入库单',  title: '其他入库单', type: 1},
          {key: '其他出库单', title: '其他出库单', type: 1},
          {key: '成本调整单', title: '成本调整单', type: 1},
          {key: '收款单', title: '收款单', type: 1},
          {key: '付款单', title: '付款单', type: 1},
          {key: '核销单', title: '核销单', type: 1},
          {key: '其他收款单', title: '其他收款单', type: 1},
          {key: '转帐单', title: '转帐单', type: 1},
          {key: '商品', title: '商品', type: 2},
          {key: '仓库', title: '仓库', type: 2},
          {key: '客户', title: '客户', type: 2},
          {key: '供货商', title: '供货商', type: 2}
      ],
      model: {
        id: null,
        name: null,
        prefix: null,
        code: null,
        format: null,
        serialNumberLength: 5,
        startValue: 1,
        resetPeriod: '日',
        documentType: null,
        systemDefault: null,
      },
      validationRules: {}
    }
  },
  methods: {


    handleBlurForStartValue(event) {
      let startValue = parseInt(this.model.startValue);
      let serialNumberLength = parseInt(this.model.serialNumberLength);
      this.formatSerialNumber(startValue,serialNumberLength);
    },

    formatValueChange(){
       console.log("formatValueChange: "+this.model.format);
    },

    serialNumberLengthValueChange(){
      console.log("formatValueChange: "+this.model.serialNumberLength);
      let startValue = parseInt(this.model.startValue);
      let serialNumberLength = parseInt(this.model.serialNumberLength);
      this.formatSerialNumber(startValue,serialNumberLength);
    },

    formatSerialNumber(startValue, serialNumberLength) {
      this.model.startValue = startValue.toString().padStart(serialNumberLength, '0');
    },

    confirm() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        this.model.startDate = manba(this.model.startDate).format("YYYY-MM");
        CodeRule.save(this.model).then((result) => {
          if (result.success){
            message("保存成功~");
          }else {
            message(result.msg);
          }

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

    // 界面默认值设置 begin
    if (!this.CodeRule.serialNumberLength){
      this.CodeRule.serialNumberLength = this.model.serialNumberLength;
    }

    if (!this.CodeRule.startValue){
      this.CodeRule.startValue = this.model.startValue;
    }

    if (!this.CodeRule.resetPeriod){
      this.CodeRule.resetPeriod = this.model.resetPeriod;
    }

    // 新增情况下 设置默认值
    if (this.CodeRule.id == null || typeof this.CodeRule.id == 'undefined' ){
      if (this.documentTypes.includes(this.CodeRule.documentType)  ){
        this.CodeRule.format = '所属分类编码';
      }else {
        this.CodeRule.format = 'yyyyMMdd';
      }
    }

    // 把数字 startValue 按 长度 serialNumberLength 格式化，长度不够补 0
    let num = parseInt(this.CodeRule.startValue);
    let len = parseInt(this.CodeRule.serialNumberLength);
    this.CodeRule.startValue = num.toString().padStart(len, '0');

    // 界面默认值设置 end

    CopyObj(this.model, this.CodeRule);
  }
}
</script>
