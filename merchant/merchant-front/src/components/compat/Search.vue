<template>
  <div class="compat-search" :class="$attrs.class" :style="$attrs.style">
    <t-input
        v-model="innerValue"
        :placeholder="placeholder"
        clearable
        @enter="onSearch"
    >
      <template v-if="$slots.default" #suffixIcon>
        <span class="compat-search-icon" @click="onSearch">
          <slot/>
        </span>
      </template>
    </t-input>
    <t-button v-if="showSearchButton" theme="default" variant="outline" @click="onSearch">
      搜索
    </t-button>
  </div>
</template>

<script>
/**
 * HeyUI Search → TDesign Input + Button
 */
export default {
  name: 'Search',
  inheritAttrs: false,
  props: {
    modelValue: { type: [String, Number], default: '' },
    placeholder: { type: String, default: '请输入' },
    showSearchButton: { type: Boolean, default: false }
  },
  emits: ['update:modelValue', 'search'],
  computed: {
    innerValue: {
      get() {
        return this.modelValue
      },
      set(val) {
        this.$emit('update:modelValue', val)
      }
    }
  },
  methods: {
    onSearch() {
      this.$emit('search', this.innerValue)
    }
  }
}
</script>

<style scoped>
.compat-search {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.compat-search :deep(.t-input) {
  flex: 1;
  min-width: 0;
}

.compat-search-icon {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
}
</style>
