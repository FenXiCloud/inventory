<template>
  <teleport to="body">
    <div class="pic-crop-mask" @click.self="onCancel">
      <div class="pic-crop">
        <div class="pic-crop__hd">
          <div class="pic-crop__title">裁切产品图片 <span class="pic-crop__tag">正方形 1:1</span></div>
          <div class="pic-crop__close" @click="onCancel">×</div>
        </div>

        <div class="pic-crop__bd">
          <div v-if="failed" class="pic-crop__fail">
            <div>图片读取失败，请重新选择</div>
            <t-button theme="primary" variant="outline" @click="onCancel">关 闭</t-button>
          </div>
          <div v-else class="pic-crop__stage-wrap">
            <div class="pic-crop__stage">
              <img
                  v-if="workSrc"
                  ref="img"
                  class="pic-crop__img"
                  :src="workSrc"
                  alt="待裁切图片"
              />
              <div v-else class="pic-crop__loading">图片处理中…</div>
            </div>
            <div class="pic-crop__side">
              <div class="pic-crop__preview-label">效果预览</div>
              <div class="pic-crop__preview-box pic-crop__thumb"></div>
              <div class="pic-crop__hint">
                拖动图片/选框调整位置，滚轮或下方按钮缩放
              </div>
            </div>
          </div>
        </div>

        <div v-if="!failed" class="pic-crop__ops">
          <t-button variant="outline" size="small" @click="rotate(-90)">左旋90°</t-button>
          <t-button variant="outline" size="small" @click="rotate(90)">右旋90°</t-button>
          <t-button variant="outline" size="small" @click="zoom(-0.12)">缩小</t-button>
          <t-button variant="outline" size="small" @click="zoom(0.12)">放大</t-button>
        </div>

        <div class="pic-crop__ft">
          <t-button variant="outline" :disabled="busy" @click="onCancel">取消</t-button>
          <t-button theme="primary" :loading="busy" @click="doConfirm">确定上传</t-button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script>
import 'cropperjs/dist/cropper.css';
import Cropper from 'cropperjs';
import {MessagePlugin} from 'tdesign-vue-next';

/** 导出图片边长上限：足够产品图各尺寸使用，同时控制存储体积 */
const OUT_MAX = 1024;
/** 导出图片边长下限：避免极端缩放下得到过小的图 */
const OUT_MIN = 96;
/** 进入裁切前的最大边长：原图远超展示需要，先缩到该值以内再交给 cropper，避免大图内存开销 */
const WORK_MAX = 2048;
/** 透明格式（png/webp/gif）保留透明通道导出 png，其余导出 jpeg */
const ALPHA_TYPES = ['image/png', 'image/webp', 'image/gif'];

function squareCanvas(srcCanvas, side) {
  const canvas = document.createElement('canvas');
  canvas.width = side;
  canvas.height = side;
  const ctx = canvas.getContext('2d');
  ctx.imageSmoothingEnabled = true;
  ctx.imageSmoothingQuality = 'high';
  const sw = srcCanvas.width;
  const sh = srcCanvas.height;
  const s0 = Math.min(sw, sh);
  const dx = (sw - s0) / 2;
  const dy = (sh - s0) / 2;
  ctx.drawImage(srcCanvas, dx, dy, s0, s0, 0, 0, side, side);
  return canvas;
}

export default {
  name: 'ProductImageCrop',
  props: {
    file: {type: File, required: true}
  },
  emits: ['confirm', 'cancel'],
  data() {
    return {
      workSrc: null,
      failed: false,
      busy: false,
      cropper: null
    };
  },
  computed: {
    alphaType() {
      return ALPHA_TYPES.includes(this.file && this.file.type);
    },
    outMime() {
      return this.alphaType ? 'image/png' : 'image/jpeg';
    },
    outExt() {
      return this.alphaType ? 'png' : 'jpg';
    }
  },
  mounted() {
    this.init();
  },
  beforeUnmount() {
    if (this.cropper) {
      this.cropper.destroy();
      this.cropper = null;
    }
  },
  methods: {
    fileToDataURL(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.onerror = reject;
        reader.readAsDataURL(file);
      });
    },
    loadImage(url) {
      return new Promise((resolve, reject) => {
        const img = new Image();
        img.onload = () => resolve(img);
        img.onerror = reject;
        img.src = url;
      });
    },
    /**
     * 归一化方向(EXIF)并预缩到 WORK_MAX 以内：
     * 现代浏览器 createImageBitmap 会按 EXIF 方向解码，避免手机竖拍图在 canvas 里被转错。
     */
    async buildWorkImage() {
      const file = this.file;
      let sourceW = 0;
      let sourceH = 0;
      let drawTo = null;
      let bitmap = null;

      if (typeof createImageBitmap === 'function') {
        try {
          bitmap = await createImageBitmap(file);
          sourceW = bitmap.width;
          sourceH = bitmap.height;
          drawTo = (ctx) => ctx.drawImage(bitmap, 0, 0, sourceW, sourceH);
        } catch (e) {
          bitmap = null;
        }
      }

      if (!drawTo) {
        const url = await this.fileToDataURL(file);
        const img = await this.loadImage(url);
        sourceW = img.naturalWidth || img.width;
        sourceH = img.naturalHeight || img.height;
        drawTo = (ctx) => ctx.drawImage(img, 0, 0, sourceW, sourceH);
      }

      const ratio = Math.min(1, WORK_MAX / Math.max(sourceW, sourceH));
      const cw = Math.max(1, Math.round(sourceW * ratio));
      const ch = Math.max(1, Math.round(sourceH * ratio));
      const canvas = document.createElement('canvas');
      canvas.width = cw;
      canvas.height = ch;
      const ctx = canvas.getContext('2d');
      ctx.imageSmoothingEnabled = true;
      ctx.imageSmoothingQuality = 'high';
      drawTo(ctx);
      if (bitmap) bitmap.close();
      return canvas.toDataURL(this.alphaType ? 'image/png' : 'image/jpeg', 0.92);
    },
    async init() {
      try {
        const url = await this.buildWorkImage();
        this.workSrc = url;
        await this.$nextTick();
        this.initCropper();
      } catch (e) {
        console.error('图片预处理失败', e);
        this.failed = true;
      }
    },
    initCropper() {
      if (!this.$refs.img) return;
      this.cropper = new Cropper(this.$refs.img, {
        aspectRatio: 1,
        viewMode: 1,
        dragMode: 'move',
        autoCropArea: 1,
        background: false,
        guides: true,
        center: true,
        highlight: true,
        cropBoxMovable: true,
        cropBoxResizable: true,
        wheelZoomRatio: 0.12,
        zoomOnTouch: true,
        preview: '.pic-crop__thumb'
      });
    },
    zoom(ratio) {
      if (this.cropper) this.cropper.zoom(ratio);
    },
    rotate(deg) {
      if (this.cropper) this.cropper.rotate(deg);
    },
    onCancel() {
      if (this.busy) return;
      this.$emit('cancel');
    },
    doConfirm() {
      if (this.busy || !this.cropper) return;
      const data = this.cropper.getData(true);
      if (!data || !data.width || !data.height) {
        MessagePlugin.warning('请先框选裁切区域');
        return;
      }
      this.busy = true;
      this.$nextTick(() => {
        try {
          const src = this.cropper.getCroppedCanvas({imageSmoothingQuality: 'high'});
          const natural = Math.min(src.width, src.height);
          let side = natural;
          if (side > OUT_MAX) side = OUT_MAX;
          else if (side < OUT_MIN) side = OUT_MIN;
          const out = (side === natural && src.width === src.height) ? src : squareCanvas(src, side);
          out.toBlob((blob) => {
            if (blob) {
              this.$emit('confirm', blob, this.outExt);
            } else {
              this.busy = false;
              MessagePlugin.error('图片导出失败，请重试');
            }
          }, this.outMime, 0.92);
        } catch (e) {
          console.error('裁切导出失败', e);
          this.busy = false;
          MessagePlugin.error('图片处理失败，请重试');
        }
      });
    }
  }
};
</script>

<style scoped>
.pic-crop-mask {
  position: fixed;
  inset: 0;
  z-index: 6000;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.pic-crop {
  width: min(92vw, 860px);
  max-height: 92vh;
  background: #fff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.pic-crop__hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid var(--td-component-border, #e7e7e7);
  flex-shrink: 0;
}

.pic-crop__title {
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.pic-crop__tag {
  font-size: 12px;
  font-weight: 400;
  color: #ff8f1f;
  background: #fff3e0;
  border-radius: 3px;
  padding: 1px 6px;
}

.pic-crop__close {
  width: 24px;
  height: 24px;
  line-height: 22px;
  text-align: center;
  font-size: 20px;
  color: #999;
  cursor: pointer;
  border-radius: 50%;
}

.pic-crop__close:hover {
  color: #333;
  background: #f3f3f3;
}

.pic-crop__bd {
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 0;
}

.pic-crop__stage-wrap {
  display: flex;
  gap: 20px;
  align-items: center;
  justify-content: center;
  width: 100%;
}

.pic-crop__stage {
  width: min(62vh, 520px, 68vw);
  aspect-ratio: 1;
  background: #141414;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
  flex-shrink: 0;
}

.pic-crop__img {
  display: block;
  max-width: 100%;
}

.pic-crop__loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #aaa;
}

.pic-crop__side {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.pic-crop__preview-label {
  font-size: 12px;
  color: #888;
}

.pic-crop__preview-box {
  width: 96px;
  height: 96px;
  border-radius: 4px;
  overflow: hidden;
  background: #eee;
}

.pic-crop__hint {
  max-width: 140px;
  font-size: 12px;
  line-height: 1.7;
  color: #999;
}

.pic-crop__fail {
  text-align: center;
  color: #666;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 30px;
}

.pic-crop__ops {
  display: flex;
  gap: 8px;
  justify-content: center;
  padding: 0 20px 16px;
  flex-shrink: 0;
}

.pic-crop__ft {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 20px;
  border-top: 1px solid var(--td-component-border, #e7e7e7);
  flex-shrink: 0;
}

@media (max-width: 720px) {
  .pic-crop__stage-wrap {
    flex-direction: column;
  }
}
</style>
