<template>
  <t-input-number
      v-model="innerValue"
      :min="min"
      :max="max"
      :step="step"
      :disabled="disabled"
      :placeholder="placeholder"
      theme="normal"
      :class="$attrs.class"
      :style="$attrs.style"
      @blur="$emit('blur', $event)"
      @change="$emit('change', $event)"
  />
</template>

<script>
/** HeyUI NumberInput → TDesign InputNumber */
export default {
  name: 'NumberInput',
  inheritAttrs: false,
  props: {
    modelValue: { type: [Number, String], default: null },
    min: { type: Number, default: undefined },
    max: { type: Number, default: undefined },
    step: { type: Number, default: 1 },
    disabled: { type: Boolean, default: false },
    placeholder: { type: String, default: undefined }
  },
  emits: ['update:modelValue', 'blur', 'change'],
  computed: {
    innerValue: {
      get() {
        return this.modelValue
      },
      set(v) {
        this.$emit('update:modelValue', v)
      }
    }
  }
}
</script>
