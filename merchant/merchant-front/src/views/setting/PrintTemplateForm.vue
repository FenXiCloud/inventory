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
              @change="resetFieldsByType"
          />
        </t-form-item>
        <t-form-item label="默认模板" name="systemDefault">
          <t-switch v-model="model.systemDefault" />
        </t-form-item>
        <t-form-item label="打印字段" name="content">
          <div class="field-designer">
            <div class="field-designer__group">
              <div class="field-designer__title">表头字段</div>
              <t-checkbox-group v-model="headerChecked" :options="headerOptions" />
            </div>
            <div class="field-designer__group">
              <div class="field-designer__title">明细列</div>
              <t-checkbox-group v-model="itemChecked" :options="itemOptions" />
            </div>
          </div>
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
  '收款单', '付款单', '核销单', '其他收款单', '其他付款单', '转账单'
];

const HEADER_FIELDS = [
  {key: 'orderNo', label: '单据编号'},
  {key: 'orderDate', label: '单据日期'},
  {key: 'partner', label: '往来单位'},
  {key: 'amount', label: '金额'},
  {key: 'remarks', label: '备注'},
];

const ITEM_FIELDS = [
  {key: 'productName', label: '产品名称'},
  {key: 'quantity', label: '数量'},
  {key: 'price', label: '单价'},
  {key: 'amount', label: '金额'},
  {key: 'remarks', label: '备注'},
];

function buildDefaultContent() {
  return [
    ...HEADER_FIELDS.map(f => ({...f, enabled: true, section: 'header'})),
    ...ITEM_FIELDS.map(f => ({...f, enabled: true, section: 'item'})),
  ];
}

/**
 * @功能描述: 打印模板表单（字段勾选设计器）
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
      headerOptions: HEADER_FIELDS.map(f => ({label: f.label, value: f.key})),
      itemOptions: ITEM_FIELDS.map(f => ({label: f.label, value: f.key})),
      headerChecked: HEADER_FIELDS.map(f => f.key),
      itemChecked: ITEM_FIELDS.map(f => f.key),
      model: {
        id: null,
        name: null,
        documentType: null,
        systemDefault: false,
        content: []
      },
      rules: {
        name: [{ required: true, message: '请输入模板名称', type: 'error' }],
        documentType: [{ required: true, message: '请选择单据类型', type: 'error' }]
      }
    };
  },
  methods: {
    resetFieldsByType() {
      if (!this.model.id) {
        this.headerChecked = HEADER_FIELDS.map(f => f.key);
        this.itemChecked = ITEM_FIELDS.map(f => f.key);
      }
    },
    syncCheckedFromContent(content) {
      let list = content;
      if (typeof list === 'string') {
        try { list = JSON.parse(list); } catch (e) { list = []; }
      }
      if (!Array.isArray(list) || !list.length) {
        this.headerChecked = HEADER_FIELDS.map(f => f.key);
        this.itemChecked = ITEM_FIELDS.map(f => f.key);
        return;
      }
      this.headerChecked = list
        .filter(f => (f.section === 'header' || HEADER_FIELDS.some(h => h.key === f.key && f.section !== 'item')) && f.enabled !== false)
        .filter(f => HEADER_FIELDS.some(h => h.key === f.key))
        .map(f => f.key);
      // dedupe
      this.headerChecked = [...new Set(this.headerChecked)];
      const itemEnabled = list.filter(f => f.section === 'item' || (ITEM_FIELDS.some(i => i.key === f.key) && f.section !== 'header'));
      // Prefer section=item; fallback enabled item keys
      let items = list.filter(f => f.section === 'item' && f.enabled !== false).map(f => f.key);
      if (!items.length) {
        items = list.filter(f => ITEM_FIELDS.some(i => i.key === f.key) && f.enabled !== false).map(f => f.key);
      }
      this.itemChecked = [...new Set(items)];
    },
    buildContent() {
      const content = [];
      HEADER_FIELDS.forEach(f => {
        content.push({
          key: f.key,
          label: f.label,
          enabled: this.headerChecked.includes(f.key),
          section: 'header'
        });
      });
      ITEM_FIELDS.forEach(f => {
        content.push({
          key: f.key,
          label: f.label,
          enabled: this.itemChecked.includes(f.key),
          section: 'item'
        });
      });
      return content;
    },
    confirm() {
      this.$refs.form.validate().then((result) => {
        if (result !== true) return;
        this.model.content = this.buildContent();
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
    if (this.model.content) {
      this.syncCheckedFromContent(this.model.content);
    } else {
      this.model.content = buildDefaultContent();
      this.syncCheckedFromContent(this.model.content);
    }
  }
};
</script>

<style scoped>
.field-designer { width: 100%; }
.field-designer__group { margin-bottom: 12px; }
.field-designer__title { font-weight: 600; margin-bottom: 8px; color: #333; }
</style>
