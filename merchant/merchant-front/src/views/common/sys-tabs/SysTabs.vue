<template>
  <t-tabs
      :value="currentTab"
      theme="normal"
      size="medium"
      @change="onChange"
      @remove="onRemove"
  >
    <t-tab-panel value="DashboardMain" label="桌面" :removable="false"/>
    <t-tab-panel
        v-for="item in tabs"
        :key="item.key"
        :value="item.key"
        :label="item.title"
        :removable="true"
    />
  </t-tabs>
</template>

<script>
import {mapMutations, mapState} from "vuex";

export default {
  name: 'TagsNav',
  props: {
    value: Object,
    homePage: {
      type: String,
      default: 'DashboardMain'
    }
  },
  computed: {
    ...mapState(['tabs', 'currentTab']),
  },
  methods: {
    ...mapMutations(['updateTab', 'closeSelfTab', 'closeTabKey']),
    onChange(value) {
      this.updateTab(value);
    },
    onRemove({value}) {
      if (value === 'DashboardMain') return;
      const index = this.tabs.findIndex(t => String(t.key) === String(value));
      if (index > -1) {
        this.closeSelfTab(index);
      }
    }
  }
};
</script>
