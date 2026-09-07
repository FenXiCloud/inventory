<template>
  <div class="print-preview">
    <div class="print-preview__toolbar no-print">
      <t-space>
        <t-select
          v-if="templates.length > 1"
          v-model="templateId"
          :options="templateOptions"
          placeholder="选择模板"
          style="width: 220px"
          @change="applyTemplate"
        />
        <t-button theme="primary" @click="doPrint">打 印</t-button>
        <t-button variant="outline" @click="$emit('close')">关 闭</t-button>
      </t-space>
    </div>
    <div class="print-preview__body" ref="printArea">
      <PrintSheet :document-type="documentType" :content="content" :data="data" />
    </div>
  </div>
</template>

<script>
import PrintTemplate from '@js/api/setting/PrintTemplate';
import PrintSheet from '@/views/common/PrintSheet.vue';
import { MessagePlugin } from 'tdesign-vue-next';

/** 打印预览（真实单据）：加载模板 + 工具栏，纸张渲染复用 PrintSheet */
export default {
  name: 'PrintPreview',
  components: { PrintSheet },
  emits: ['close'],
  props: {
    documentType: {type: String, required: true},
    data: {type: Object, default: () => ({})}
  },
  data() {
    return {
      templates: [],
      templateId: null,
      content: []
    };
  },
  computed: {
    templateOptions() {
      return this.templates.map(t => ({label: t.name, value: t.id}));
    }
  },
  methods: {
    applyTemplate() {
      const t = this.templates.find(x => x.id === this.templateId);
      if (!t) return;
      let content = t.content;
      if (typeof content === 'string') {
        try { content = JSON.parse(content); } catch (e) { content = []; }
      }
      this.content = Array.isArray(content) ? content : [];
    },
    doPrint() {
      window.print();
    },
    loadTemplates() {
      const q = PrintTemplate.byType
        ? PrintTemplate.byType(this.documentType)
        : PrintTemplate.list({documentType: this.documentType});
      q.then(({data}) => {
        let list = data || [];
        if (list.results) list = list.results;
        this.templates = list;
        const def = list.find(t => t.systemDefault) || list[0];
        if (def) {
          this.templateId = def.id;
          this.applyTemplate();
        }
      }).catch(() => {
        MessagePlugin.warning('未找到打印模板，使用默认字段');
      });
    }
  },
  created() {
    this.loadTemplates();
  }
};
</script>

<style scoped>
.print-preview { padding: 8px 12px 16px; }
.print-preview__toolbar { margin-bottom: 12px; }
@media print {
  .no-print { display: none !important; }
  .print-preview { padding: 0; }
}
</style>
