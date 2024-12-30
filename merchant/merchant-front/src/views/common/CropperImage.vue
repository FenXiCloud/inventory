<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <div class="flex text-center items-center pb-5px">
        <Button :loading="loading" @click="$refs.uploads.click()">上传图片</Button>
        <input type="file" id="uploads" style="position:absolute; clip:rect(0 0 0 0);" ref="uploads" accept="image/png, image/jpeg, image/gif, image/jpg"
               @change="selectImg($event)">
        <span class=" ml-10px text-12px">仅支持JPG,JPEG、PNG格式，文件小于5M（建议使用高质量图片）；图片将自动生成三种尺寸，请注意生成图片是否清晰</span>
      </div>
    </div>
    <div class="modal-column-between">
      <Button @click="$emit('close')" :loading="loading">
        取消
      </Button>
      <Button color="green" @click="uploadImg" :loading="loading">
        上传
      </Button>
    </div>
  </div>
</template>

<script>

import VueCropper from 'vue-cropperjs';
import 'cropperjs/dist/cropper.css';
import {message} from "heyui.ext";
import {OssUpload, Upload} from "@js/api/App";
import Compressor from 'compressorjs';
//https://img-home.csdnimg.cn/images/20201124032511.png
export default {
  name: "CropperImage",
  components: {
    VueCropper
  },
  props: {
    imgPath: String,
  },
  data() {
    return {
      loading: false,
      imgSrc: this.imgPath,
      options: {
        max_file_size: '1mb',
        filters: {
          mime_types: [
            {title: 'Image files', extensions: 'jpg,gif,png'}
          ]
        }
      },
    };
  },
  methods: {
    selectImg(e) {
      this.loading = true;
      let file = e.target.files[0]
      if (!/\.(jpg|jpeg|png|JPG|PNG)$/.test(e.target.value)) {
        message.error('图片类型要求：jpeg、jpg、png');
        return false
      }
      if (file && file.size > 5242880) {
        e.target.value = '';
        message.error("图片大小大于5M");
        return false
      }

      //压缩文件
      let imgSrc1 = null
      let options = {
        maxWidth: 800,
        maxHeight: 800,
        quality: 0.5,
        mimeType: "image/jpeg",
        success(result) {
          imgSrc1 = new File([result],result.name,{type: 'image/jpeg', lastModified: Date.now()});
        }
      };
      let compressor = new Compressor(file, options);
      setTimeout(()=>{
        this.imgSrc = imgSrc1
      },500)

      //直接上传不压缩
      // this.imgSrc = file
      // this.imgSrc = URL.createObjectURL(file)
      this.loading = false;
    },
    uploadImg() {
      const _this = this
      const params = new FormData()
      params.append('file', this.imgSrc)
      OssUpload('product', params).then(({data}) => {
        if (data) {
          _this.$emit('success', data);
        }
      })
    },
  },
}
</script>
