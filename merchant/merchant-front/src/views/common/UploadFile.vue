<script>
import VueCropper from "vue-cropperjs";
import {message} from "heyui.ext";
import Compressor from "compressorjs";
import {Upload} from "@js/api/App";

/**
 * <p>****************************************************************************</p>
 * <p><b>Copyright © 2010-2024纷析云（杭州）科技有限公司All Rights Reserved<b></p>
 * <ul style="margin:15px;">
 * <li>Description : </li>
 * <li>Version     : 1.0</li>
 * <li>Creation    : 2024年04月10日</li>
 * <li>@author     : ____xzy</li>
 * </ul>
 * <p>****************************************************************************</p>
 */
export default {
  name: "UploadFile",
  components: {
    VueCropper
  },
  props: {
    filePath: String,
    fileType: String,
  },
  data() {
    return {
      loading: false,
      fileSrc: this.filePath,
      // options: {
      //   max_file_size: '1mb',
      //   filters: {
      //     mime_types: [
      //       {title: 'Image files', extensions: 'jpg,gif,png'}
      //     ]
      //   }
      // },
    };
  },
  methods: {
    selectImg(e) {
      this.loading = true;
      //直接上传不压缩
      this.fileSrc = e.target.files[0]
      // this.fileSrc = URL.createObjectURL(file)
      this.loading = false;
    },
    uploadFile() {
      const _this = this
      const params = new FormData()
      params.append('file', _this.fileSrc)
      Upload(_this.fileType, params).then(({data}) => {
        if (data && data.path) {
          _this.$emit('success', data.path);
        }
      })
    },
  },
}
</script>

<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <div class="flex text-center items-center pb-5px">
        <Button :loading="loading" @click="$refs.uploads.click()">上传文件</Button>
        <input type="file" id="uploads" style="position:absolute; clip:rect(0 0 0 0);" ref="uploads"
               @change="selectImg($event)">
        <span class=" ml-10px text-12px">可支持各种样式发file文件</span>
      </div>
    </div>
    <div class="modal-column-between">
      <Button @click="$emit('close')" :loading="loading">
        取消
      </Button>
      <Button color="green" @click="uploadFile" :loading="loading">
        上传
      </Button>
    </div>
  </div>
</template>

<style scoped lang="less">

</style>