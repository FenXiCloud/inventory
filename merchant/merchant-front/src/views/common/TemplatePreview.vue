<template>
  <div class="modal-column">
    <div class="tpl-preview__meta">
      <div class="tpl-preview__meta-left">
        <span class="tpl-preview__name">{{ template.name || '未命名模板' }}</span>
        <t-tag size="small" variant="light">{{ template.documentType }}</t-tag>
        <t-tag
            v-if="template.systemDefault"
            size="small"
            theme="success"
            variant="light"
        >默认模板</t-tag>
        <t-tag
            v-else
            size="small"
            theme="primary"
            variant="light"
        >自定义模板</t-tag>
      </div>
      <div class="tpl-preview__hint">
        <t-icon name="info-circle" size="14px" />
        <span>预览使用示例数据，实际打印时展示真实单据内容</span>
      </div>
    </div>
    <div class="modal-column-full-body tpl-preview__workspace">
      <div class="tpl-preview__paper">
        <PrintSheet
            :document-type="template.documentType"
            :content="content"
            :data="sampleData"
        />
      </div>
    </div>
    <div class="modal-column-between">
      <span></span>
      <t-button variant="outline" @click="$emit('close')">关 闭</t-button>
    </div>
  </div>
</template>

<script>
/**
 * 打印模板效果预览弹窗（模板列表页「预览」入口）：
 * 顶部模板信息栏 + 灰色工作区中居中渲染 A4 纸张效果，数据为示例单据。
 */
import PrintSheet from '@/views/common/PrintSheet.vue';
import { buildSamplePrintData } from '@common/print-sample';

export default {
  name: 'TemplatePreview',
  components: { PrintSheet },
  emits: ['close'],
  props: {
    template: { type: Object, required: true }
  },
  computed: {
    content() {
      let content = this.template.content;
      if (typeof content === 'string') {
        try { content = JSON.parse(content); } catch (e) { content = []; }
      }
      return Array.isArray(content) ? content : [];
    },
    sampleData() {
      return buildSamplePrintData(this.template.documentType);
    }
  }
};
</script>

<style scoped>
.tpl-preview__meta {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--td-border-level-1-color, #eee);
  background: #fff;
}

.tpl-preview__meta-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.tpl-preview__name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tpl-preview__hint {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.tpl-preview__workspace {
  background: #f2f4f7;
  padding: 24px 16px 32px;
  display: block;
  overflow-y: auto;
}

.tpl-preview__paper {
  width: 690px;
  max-width: 100%;
  margin: 0 auto;
  background: #fff;
  border-radius: 2px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06), 0 6px 20px rgba(0, 0, 0, 0.08);
  padding: 40px 44px 48px;
  box-sizing: border-box;
}
</style>
