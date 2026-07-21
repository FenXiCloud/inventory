<template>
  <t-popup
      :visible="visible"
      :trigger="manual ? 'click' : trigger"
      :placement="resolvedPlacement"
      destroy-on-close
      :overlay-inner-style="overlayStyle"
      attach="body"
      @visible-change="onVisibleChange"
  >
    <div ref="triggerRef" class="manual-popup-trigger" :class="className" :style="triggerStyle">
      <slot/>
    </div>
    <template #content>
      <div class="manual-popup-content" @mousedown.stop>
        <slot name="content"/>
      </div>
    </template>
  </t-popup>
</template>

<script>
/**
 * 手动控制显隐的弹出层（凭证表格编辑等场景）
 * 支持 trigger="manual" + show()/hide()、equalWidth
 */
export default {
  name: 'ManualPopup',
  props: {
    trigger: { type: String, default: 'click' },
    placement: { type: String, default: 'bottom-left' },
    equalWidth: { type: Boolean, default: false },
    className: { type: String, default: '' }
  },
  emits: ['hide', 'show'],
  data() {
    return {
      visible: false,
      triggerWidth: undefined,
      openedByShow: false
    };
  },
  computed: {
    manual() {
      return this.trigger === 'manual';
    },
    resolvedPlacement() {
      const map = {
        top: 'top-left',
        bottom: 'bottom-left',
        left: 'left',
        right: 'right'
      };
      return map[this.placement] || this.placement || 'bottom-left';
    },
    triggerStyle() {
      return this.equalWidth ? { width: '100%', display: 'block' } : undefined;
    },
    overlayStyle() {
      if (this.equalWidth && this.triggerWidth) {
        return { width: `${this.triggerWidth}px`, padding: '0' };
      }
      return { padding: '4px 0' };
    }
  },
  methods: {
    show() {
      if (this.equalWidth && this.$refs.triggerRef) {
        this.triggerWidth = this.$refs.triggerRef.offsetWidth;
      }
      this.openedByShow = true;
      this.visible = true;
      this.$emit('show');
    },
    hide() {
      if (!this.visible) return;
      this.visible = false;
      this.openedByShow = false;
      this.$emit('hide');
    },
    onVisibleChange(v) {
      if (this.manual) {
        if (v) {
          if (!this.openedByShow) {
            this.visible = false;
            return;
          }
          this.visible = true;
          return;
        }
        if (this.visible) {
          this.visible = false;
          this.openedByShow = false;
          this.$emit('hide');
        }
        return;
      }
      this.visible = v;
      if (!v) this.$emit('hide');
      else this.$emit('show');
    }
  }
};
</script>

<style scoped>
.manual-popup-trigger {
  display: inline-block;
}
</style>
