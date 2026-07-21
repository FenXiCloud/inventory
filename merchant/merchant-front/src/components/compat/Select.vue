<template>
  <t-select
      v-model="innerValue"
      :options="options"
      :keys="optionKeys"
      :multiple="multiple"
      :filterable="filterable"
      :clearable="resolvedClearable"
      :placeholder="placeholder"
      :disabled="disabled"
      :loading="loading"
      :class="$attrs.class"
      :style="$attrs.style"
      v-bind="forwardedAttrs"
      @change="onChange"
  />
</template>

<script>
/**
 * HeyUI Select → TDesign TSelect
 * 兼容 :datas / keyName / titleName / filterable / deletable
 * @change 回传选中项对象（与 HeyUI 一致），清空时回传 null
 */
export default {
  name: 'Select',
  inheritAttrs: false,
  props: {
    modelValue: { default: null },
    datas: { type: [Array, Object], default: () => [] },
    keyName: { type: String, default: 'key' },
    titleName: { type: String, default: 'title' },
    multiple: { type: Boolean, default: false },
    filterable: { type: Boolean, default: false },
    deletable: { type: Boolean, default: true },
    placeholder: { type: String, default: undefined },
    disabled: { type: Boolean, default: false },
    loading: { type: Boolean, default: false }
  },
  emits: ['update:modelValue', 'change'],
  computed: {
    innerValue: {
      get() {
        return this.modelValue
      },
      set(val) {
        this.$emit('update:modelValue', val)
      }
    },
    resolvedClearable() {
      return this.deletable
    },
    optionKeys() {
      return { value: this.keyName, label: this.titleName }
    },
    options() {
      const datas = this.datas
      if (!datas) return []
      if (Array.isArray(datas)) {
        return datas.map((item) => {
          if (item == null || typeof item !== 'object') {
            return { [this.keyName]: item, [this.titleName]: String(item) }
          }
          return item
        })
      }
      return Object.keys(datas).map((key) => ({
        [this.keyName]: key,
        [this.titleName]: datas[key]
      }))
    },
    forwardedAttrs() {
      const rest = { ...this.$attrs }
      delete rest.class
      delete rest.style
      ;['datas', 'keyName', 'titleName', 'deletable', 'required'].forEach((k) => delete rest[k])
      return rest
    }
  },
  methods: {
    resolveSelected(val) {
      if (val == null || val === '') return null
      if (this.multiple && Array.isArray(val)) {
        return val.map((v) => this.resolveOne(v)).filter((v) => v != null)
      }
      return this.resolveOne(val)
    },
    resolveOne(val) {
      if (val != null && typeof val === 'object') return val
      const datas = this.datas
      if (Array.isArray(datas)) {
        const found = datas.find((item) => {
          if (item == null || typeof item !== 'object') return item === val
          return String(item[this.keyName]) === String(val)
        })
        return found !== undefined ? found : val
      }
      if (datas && typeof datas === 'object' && Object.prototype.hasOwnProperty.call(datas, val)) {
        return { [this.keyName]: val, [this.titleName]: datas[val] }
      }
      return val
    },
    onChange(val, context) {
      this.$emit('change', this.resolveSelected(val), context)
    }
  }
}
</script>
