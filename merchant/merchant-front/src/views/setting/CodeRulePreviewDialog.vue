<template>
  <div class="modal-column">
    <div class="crp-dialog__meta">
      <div class="crp-dialog__meta-left">
        <span class="crp-dialog__name">{{ codeRule.name || '未命名规则' }}</span>
        <t-tag size="small" variant="light">{{ codeRule.documentType }}</t-tag>
        <t-tag
            v-if="codeRule.systemDefault"
            size="small"
            theme="success"
            variant="light"
        >默认规则</t-tag>
        <t-tag
            v-else
            size="small"
            theme="primary"
            variant="light"
        >自定义规则</t-tag>
      </div>
      <div class="crp-dialog__hint">
        <t-icon name="info-circle" size="14px" />
        <span>预览按当前账套流水进度计算，不消耗号码</span>
      </div>
    </div>
    <div class="modal-column-full-body">
      <CodeRulePreview :rule="codeRule" :data="preview" :loading="loading" />
    </div>
    <div class="modal-column-between">
      <span></span>
      <t-button variant="outline" @click="$emit('close')">关 闭</t-button>
    </div>
  </div>
</template>

<script>
/** 编码规则预览弹窗（规则列表页「预览」入口） */
import CodeRulePreview from './CodeRulePreview.vue';
import CodeRule from '@js/api/setting/CodeRule';

export default {
  name: 'CodeRulePreviewDialog',
  components: { CodeRulePreview },
  emits: ['close'],
  props: {
    codeRule: { type: Object, required: true }
  },
  data() {
    return {
      loading: false,
      preview: null
    };
  },
  created() {
    this.loading = true;
    CodeRule.preview(this.codeRule)
      .then(({ data }) => { this.preview = data; })
      .finally(() => { this.loading = false; });
  }
};
</script>

<style scoped>
.crp-dialog__meta {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--td-border-level-1-color, #eee);
  background: #fff;
}
.crp-dialog__meta-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.crp-dialog__name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.crp-dialog__hint {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}
</style>
