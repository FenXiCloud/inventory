<template>
  <div class="compat-row" :class="rowClass" :style="rowStyle" v-bind="$attrs">
    <slot/>
  </div>
</template>

<script>
/** HeyUI Row → flex 容器 */
export default {
  name: 'Row',
  inheritAttrs: false,
  props: {
    type: { type: String, default: undefined },
    space: { type: [Number, String], default: undefined },
    spaceX: { type: [Number, String], default: undefined },
    spaceY: { type: [Number, String], default: undefined }
  },
  computed: {
    rowClass() {
      return this.type === 'flex' ? 'compat-row--flex' : ''
    },
    rowStyle() {
      const gapX = this.spaceX ?? this.space
      const gapY = this.spaceY ?? this.space
      const style = {}
      if (gapX != null || gapY != null) {
        style.gap = `${gapY || 0}px ${gapX || 0}px`
      }
      return style
    }
  }
}
</script>

<style scoped>
.compat-row--flex {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}
</style>
