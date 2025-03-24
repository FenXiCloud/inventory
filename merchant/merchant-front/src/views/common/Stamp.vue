<template>
  <canvas ref="canvas" :width="size" :height="size"></canvas>
</template>

<script>
export default {
  name: 'Stamp',
  props: {
    color: {type: String, default: '#f00'},
    size: {type: Number, default: 100},
    text: {type: String, default: '已审核'}
  },
  mounted() {
    this.drawStamp();
  },
  methods: {
    drawStamp() {
      const ctx = this.$refs.canvas.getContext('2d');
      ctx.clearRect(0, 0, this.size, this.size);

      // 绘制正方形边框
      ctx.strokeStyle = this.color;
      ctx.lineWidth = 2;
      ctx.strokeRect(10, 10, this.size - 20, this.size - 60);

      // 绘制文字
      ctx.font = '20px 黑体';
      ctx.textAlign = 'center';
      ctx.fillStyle = this.color;
      ctx.fillText(this.text, this.size / 2, (this.size - 25) / 2 );
    }
  },
  watch: {
    color() {
      this.drawStamp();
    },
    size() {
      this.drawStamp();
    },
    text() {
      this.drawStamp();
    }
  }
}
</script>
