<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <div class="import-explain">
        <h6>导入说明：</h6>
        <p>1. 请先下载模板，按模板列填写数据（分类、单位需与系统中已有名称一致）。</p>
        <p>2. 导入文件支持xls、xlsx格式，大小不超过4M，数据不超过1000行。</p>
        <p>3. 导入文件不能包含"合并单元格"，否则无法导入。</p>
      </div>
      <div class="filler-panel">
        <div class="filler-item" style="flex: 1;margin: 5px 0px!important;">
          <label class="mr-16px  w-110px">下载模版：</label>
          <a class="text-hover" href="/import/product.xlsx" :download="`商品导入模版.xlsx`">商品导入模版</a>
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item" style="flex: 1;margin: 5px 0px!important;">
          <label class="mr-16px  w-110px">选择上传文件：</label>
          <t-button theme="primary" :loading="loading" @click="$refs.file.click()">选择文件</t-button>
          <span class="ml-10px" v-if="file"> {{ fileName }}
            <t-icon @click="file = null" name="close" class="text-hover ml-10px"/>
          </span>
          <input type="file" style="visibility: hidden;" @change="fileChange($event)" ref="file"
                 accept="application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet">
        </div>
      </div>
    </div>
    <div class="modal-column-between">
      <t-button @click="$emit('close')" :loading="loading">取消</t-button>
      <t-button theme="primary" :disabled="!file" @click="importData" :loading="loading">导 入</t-button>
    </div>
  </div>
</template>

<script>
import {MessagePlugin} from "tdesign-vue-next";
import Product from "@js/api/basic/Product";

export default {
  name: "ProductImportForm",
  emits: ['close', 'success'],
  data() {
    return {
      loading: false,
      file: null
    };
  },
  computed: {
    fileName() {
      if (this.file) {
        return this.file.name;
      }
      return '';
    }
  },
  methods: {
    fileChange() {
      this.file = this.$refs.file.files[0];
      if (this.file && this.file.size > (4 * 1024 * 1024)) {
        this.file = null;
        this.$refs.file.value = '';
        MessagePlugin.error("一次最大导入4MB数据...");
      }
    },
    importData() {
      if (this.file) {
        this.loading = true;
        let formData = new FormData();
        formData.append('file', this.file);
        Product.importData(formData).then(() => {
          MessagePlugin.success('导入成功~');
          this.$emit('success');
        }).finally(() => {
          this.loading = false;
          this.file = null;
          this.$refs.file.value = '';
        });
      } else {
        MessagePlugin.error("请选择上传的文件...");
      }
    }
  }
};
</script>

<style scoped>
.import-explain {
  padding: 8px 0;
  color: var(--td-text-color-secondary);
  font-size: 13px;
}

.import-explain h6 {
  margin: 0 0 6px;
  font-size: 14px;
  color: var(--td-text-color-primary);
}

.import-explain p {
  margin: 0 0 4px;
}

.filler-panel {
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--td-component-border, #dcdcdc);
  padding: 6px 0;
}

.filler-item {
  display: flex;
  align-items: center;
}

.mr-16px {
  margin-right: 16px;
}

.w-110px {
  width: 110px;
}

.ml-10px {
  margin-left: 10px;
}

.text-hover {
  cursor: pointer;
}
</style>
