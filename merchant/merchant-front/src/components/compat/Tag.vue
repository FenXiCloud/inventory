<template>
  <t-tag
      :theme="resolvedTheme"
      :variant="variant"
      :size="size"
      :class="$attrs.class"
      :style="$attrs.style"
      v-bind="forwardedAttrs"
      @click="onClick"
  >
    <slot/>
  </t-tag>
</template>

<script>
/**
 * HeyUI Tag → TDesign TTag
 * 兼容 color="primary|red|green|yellow|gray"
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
  name: 'Tag',
  inheritAttrs: false,
  props: {
    color: { type: String, default: undefined },
    theme: { type: String, default: undefined },
    variant: { type: String, default: 'light' },
    size: { type: String, default: undefined }
  },
  emits: ['click'],
  computed: {
    resolvedTheme() {
      if (this.theme) return this.theme
      if (this.color && THEME_MAP[this.color]) return THEME_MAP[this.color]
      return 'default'
    },
    forwardedAttrs() {
      const rest = { ...this.$attrs }
      delete rest.class
      delete rest.style
      delete rest.color
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
