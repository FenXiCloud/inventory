<template>
  <div class="flow-guide">
    <div class="flow-guide__title">
      <t-icon name="info-circle" />
      <span>{{ title }}</span>
    </div>
    <div class="flow-guide__steps">
      <div
        v-for="(step, index) in steps"
        :key="index"
        class="flow-step"
        :class="{
          'flow-step--active': index === current,
          'flow-step--done': index < current
        }"
      >
        <div class="flow-step__dot">
          <t-icon v-if="index < current" name="check" />
          <span v-else>{{ index + 1 }}</span>
        </div>
        <div class="flow-step__content">
          <div class="flow-step__title">{{ step.title }}</div>
          <div class="flow-step__desc">{{ step.content }}</div>
        </div>
        <div v-if="index < steps.length - 1" class="flow-step__line" />
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'FlowGuide',
  props: {
    current: {
      type: Number,
      default: 0
    },
    steps: {
      type: Array,
      default: () => []
    },
    title: {
      type: String,
      default: '操作流程'
    }
  }
}
</script>

<style scoped>
.flow-guide {
  background: linear-gradient(135deg, #e8f4fd 0%, #f0f9ff 100%);
  border: 1px solid #b8daff;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 16px;
}

.flow-guide__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #0052d9;
  margin-bottom: 16px;
}

.flow-guide__title .t-icon {
  font-size: 16px;
}

.flow-guide__steps {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 0 20px;
}

.flow-step {
  display: flex;
  align-items: flex-start;
  position: relative;
  flex: 1;
}

.flow-step:last-child {
  flex: 0 0 auto;
}

.flow-step__dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e0e0e0;
  color: #999;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
  flex-shrink: 0;
  transition: all 0.3s;
}

.flow-step--active .flow-step__dot {
  background: #0052d9;
  color: #fff;
  box-shadow: 0 2px 8px rgba(0, 82, 217, 0.3);
}

.flow-step--done .flow-step__dot {
  background: #00a870;
  color: #fff;
}

.flow-step__dot .t-icon {
  font-size: 14px;
}

.flow-step__content {
  margin-left: 12px;
  padding-top: 2px;
}

.flow-step__title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.flow-step--active .flow-step__title {
  color: #0052d9;
}

.flow-step--done .flow-step__title {
  color: #00a870;
}

.flow-step__desc {
  font-size: 12px;
  color: #999;
}

.flow-step__line {
  position: absolute;
  top: 16px;
  left: 40px;
  right: -20px;
  height: 2px;
  background: #e0e0e0;
  z-index: 0;
}

.flow-step--done .flow-step__line {
  background: #00a870;
}

.flow-step--active .flow-step__line {
  background: linear-gradient(90deg, #00a870 50%, #e0e0e0 50%);
}
</style>
