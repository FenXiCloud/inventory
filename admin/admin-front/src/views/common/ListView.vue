<template>
  <div class="h-list-view">
    <div @click="onSelect(item)" class="h-list-view-item"
         :class="{'h-list-view-item-selected':selected[keyField]===item[keyField],'h-list-view-action-hover':hoverAction}"
         v-for="item in listData" :key="item[keyField]">
      <div class="h-list-view-content" :class="contentClass" :style="contentStyle">
        <template v-if="!$scopedSlots.title">
          {{ item[nameField] }}
        </template>
        <slot v-else name="title" :data="item"></slot>
      </div>
      <div v-if="$scopedSlots.actions" class="h-list-view-action" @click.stop>
        <slot name="actions" :data="item"></slot>
      </div>
    </div>
  </div>
</template>

<script>
/**
 * @功能描述: LISTVIEW
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "ListView",
  props: {
    datas: [Array, Object],
    hoverAction: {
      type: Boolean,
      default: true
    },
    value: [Object, String, Number, Boolean],
    contentClass: String,
    contentStyle: Object,
    keyField: {
      type: String,
      default: 'id'
    },
    nameField: {
      type: String,
      default: 'name'
    },
    type: {
      type: String,
      default: 'object'
    }
  },
  data() {
    return {
      selected: {}
    };
  },
  computed: {
    listData() {
      if (this.datas instanceof Array) {
        return this.datas;
      } else {
        return Object.keys(this.datas).map(val => {
          return {
            id: val,
            name: this.datas[val]
          }
        })
      }
    }
  },
  watch: {
    value(val) {
      if (this.type === 'object') {
        this.selected = val || {};
      } else {
        this.selected = this.listData.find(v => v.id === val) || {};
      }
    }
  },
  methods: {
    onSelect(item) {
      this.selected = item;
      if (this.type === 'object') {
        this.$emit('input', item);
      } else {
        this.$emit('input', item[this.keyField]);
      }
    }
  },
  mounted() {
    if (this.type === 'object') {
      this.selected = this.value || {};
    } else {
      this.selected = this.value ? {[this.keyField]: this.value} : {};
    }
  }
}
</script>

<style scoped lang="less">
@list-view-prefix: ~"@{prefix}list-view";

.@{list-view-prefix} {
  &-item {
    cursor: pointer;
    position: relative;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-right: 10px;
    transition: color 0.2s;

    &-selected {
      background-color: @primary2-color;
      color: @primary-color;

      &:after {
        position: absolute;
        content: '';
        top: 0;
        right: 0;
        bottom: 0;
        background-color: @primary-color;
        width: 5px;
        transition: 0.2s;
      }
    }

    &:hover {
      background-color: fade(@primary2-color, 10%);
    }
  }

  &-content {
    overflow: hidden;
    text-overflow: ellipsis;
    position: relative;
    padding: 15px;
    cursor: pointer;
    white-space: nowrap;

    &:hover {
      color: @primary-color;
    }
  }

  &-action-hover &-action {
    display: none;
  }

  &-action-hover:hover &-action {
    display: inline-block;
  }
}
</style>
