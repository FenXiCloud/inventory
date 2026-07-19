<template>
  <div class="compat-cell" :style="cellStyle" v-bind="$attrs">
    <slot/>
  </div>
</template>

<script>
/** HeyUI Cell → flex 子项 */
export default {
  name: 'Cell',
  inheritAttrs: false,
  props: {
    flex: { type: [Number, String], default: undefined },
    width: { type: [Number, String], default: undefined }
  },
  computed: {
    cellStyle() {
      const style = {}
      if (this.flex != null) style.flex = this.flex
      if (this.width != null) {
        style.width = typeof this.width === 'number' ? `${this.width}px` : this.width
      }
      return style
    }
  }
}
</script>
