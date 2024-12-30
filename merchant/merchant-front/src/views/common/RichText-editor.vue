<script>

import WangEditor from 'wangeditor';
import '@/style/wangeditor/richtext-editor.less';

export default {
  name: "RichText-editor",
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    type: {
      type: String,
      default: 'html' // html, text
    },
    cache: {
      type: Boolean,
      default: true // 是否开启本地存储
    }
  },
  data() {
    return {
      stashValue: this.modelValue
    };
  },
  methods: {
    setHtml(val) {
      this.editor.txt.html(val);
    }
  },
  watch: {
    modelValue() {
      if (this.editor && this.modelValue !== this.stashValue) {
        if (this.modelValue == null) {
          this.editor.txt.clear();
        } else {
          this.editor.txt.html(this.modelValue);
        }
      }
    }
  },
  mounted() {
    this.editor = new WangEditor(this.$el);
    // 开启图片复制  || 转base64可直接展示图片
    // this.editor.customConfig.uploadImgShowBase64 = true;
    // this.editor.customConfig.uploadImgMaxSize = 205 * 1024

    //通过请求上传图片并插入至编辑器中
    this.editor.customConfig.uploadImgServer = '/api/upload/description'
    this.editor.customConfig.withCredentials = true
    // this.editor.customConfig.debug = true
    this.editor.customConfig.uploadFileName = 'file'
    this.editor.customConfig.uploadImgHooks={
      before:function (xhr, editor, files){
      },
      success:function (xhr, editor, result) {
      },
      customInsert:function (insertImg, result, editor){
        let url = "/api"+result.data.path
        insertImg(url)//图片插入编辑器
      }
    }


    this.editor.customConfig.onchange = html => {
      let text = this.editor.txt.text();
      if (this.cache) localStorage.editorCache = html;
      let value = (this.stashValue = this.type === 'html' ? html : text);
      this.$emit('update:modelValue', value);
      this.$emit('change', html, text);
    };
    this.editor.create();
    let html = this.modelValue || localStorage.editorCache;
    if (html) this.editor.txt.html(html);
  }
}
</script>
<template>
  <div class="rich-text-editor-vue"></div>
</template>
