<template>
  <div class="coming-soon">
    <t-icon name="time" size="64px" class="icon"/>
    <div class="title">{{ pageTitle || '功能建设中' }}</div>
    <div class="desc">该功能正在开发中，敬请期待。</div>
  </div>
</template>

<script>
export default {
  name: 'ComingSoon',
  computed: {
    pageTitle() {
      const tab = (this.$store.state.tabs || []).find(
        (t) => t.key === this.$store.state.currentTab
      );
      return tab ? tab.title : '';
    }
  }
};
</script>

<style scoped>
.coming-soon {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: #fff;
  border-radius: 4px;
  color: #909399;
}

.icon {
  color: #d0d0d0;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.desc {
  font-size: 14px;
}
</style>
