<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <t-form
          ref="form"
          :data="model"
          :rules="rules"
          layout="vertical"
          label-align="top"
      >
        <t-form-item label="模板名称" name="name">
          <t-input v-model="model.name" placeholder="请输入模板名称" :maxlength="32" />
        </t-form-item>
        <t-form-item label="单据类型" name="documentType">
          <t-select
              v-model="model.documentType"
              :options="documentTypeOptions"
              :disabled="!!model.id"
              placeholder="请选择单据类型"
          />
        </t-form-item>
        <t-form-item label="默认模板" name="systemDefault">
          <t-switch v-model="model.systemDefault" />
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
import PrintTemplate from '@js/api/setting/PrintTemplate';
import { MessagePlugin } from 'tdesign-vue-next';
import { CopyObj } from '@common/utils';

const DOCUMENT_TYPES = [
  '采购订单', '采购入库单', '采购退货单',
  '销售订单', '销售出库单', '销售退货单',
  '调拨单', '盘点单', '其他入库单', '其他出库单', '成本调整单',
  '收款单', '付款单', '核销单', '其他收款单', '其他付款单', '转帐单'
];

/**
 * @功能描述: 打印模板表单
 */
export default {
  name: 'PrintTemplateForm',
  emits: { close: null, success: null },
  props: {
    printTemplate: Object
  },
  data() {
    return {
      loading: false,
      documentTypeOptions: DOCUMENT_TYPES.map((value) => ({ label: value, value })),
      model: {
        id: null,
        name: null,
        documentType: null,
        systemDefault: false
      },
      rules: {
        name: [{ required: true, message: '请输入模板名称', type: 'error' }],
        documentType: [{ required: true, message: '请选择单据类型', type: 'error' }]
      }
    };
  },
  methods: {
    confirm() {
      this.$refs.form.validate().then((result) => {
        if (result !== true) return;
        this.loading = true;
        PrintTemplate.save(this.model)
          .then(() => {
            MessagePlugin.success('保存成功~');
            this.$emit('success');
          })
          .finally(() => (this.loading = false));
      }).catch(() => {});
    }
  },
  created() {
    CopyObj(this.model, this.printTemplate);
    if (this.model.systemDefault == null) {
      this.model.systemDefault = false;
    }
  }
};
</script>
