<template>
  <t-button
      :theme="resolvedTheme"
      :variant="resolvedVariant"
      :loading="loading"
      :disabled="disabled"
      :size="size"
      :block="block"
      :class="$attrs.class"
      :style="$attrs.style"
      v-bind="forwardedAttrs"
      @click="onClick"
  >
    <i v-if="icon" :class="['compat-btn-icon', icon]" aria-hidden="true"/>
    <slot/>
  </t-button>
</template>

<script>
/**
 * HeyUI Button → TDesign TButton
 * 兼容 color="primary" / no-border / icon
 */
const THEME_MAP = {
  primary: 'primary',
  success: 'success',
  warning: 'warning',
  error: 'danger',
  danger: 'danger',
  default: 'default',
  blue: 'primary',
  green: 'success',
  yellow: 'warning',
  red: 'danger',
  gray: 'default'
}

export default {
  name: 'Button',
  inheritAttrs: false,
  props: {
    color: { type: String, default: undefined },
    theme: { type: String, default: undefined },
    noBorder: { type: Boolean, default: false },
    loading: { type: Boolean, default: false },
    disabled: { type: Boolean, default: false },
    size: { type: String, default: undefined },
    block: { type: Boolean, default: false },
    icon: { type: String, default: undefined },
    variant: { type: String, default: undefined }
  },
  emits: ['click'],
  computed: {
    resolvedTheme() {
      if (this.theme) return this.theme
      if (this.color && THEME_MAP[this.color]) return THEME_MAP[this.color]
      return 'default'
    },
    resolvedVariant() {
      if (this.variant) return this.variant
      if (this.noBorder) return 'text'
      return undefined
    },
    forwardedAttrs() {
      const rest = { ...this.$attrs }
      delete rest.class
      delete rest.style
      ;['color', 'no-border', 'noBorder', 'icon'].forEach((k) => delete rest[k])
      return rest
    }
  },
  methods: {
    onClick(e) {
      this.$emit('click', e)
    }
  }
}
</script>

<style scoped>
.compat-btn-icon {
  margin-right: 4px;
}
</style>
