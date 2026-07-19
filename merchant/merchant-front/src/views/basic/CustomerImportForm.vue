<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <div class="import-explain">
        <h6>导入说明：</h6>
        <p>1.
          导入模板后,将设置“模板列”与“系统字段”的对应关系来匹配数据,需要导入的模版列都必须设置对应字段,否则将无法导入该列数据</p>
        <p>2. 导入文件支持xls、xlsx格式，大小不超过4M，数据不超过1000行.</p>
        <p>3. 导入文件不能包含“合并单元格”,否则无法导入.</p>
      </div>
      <div class="filler-panel">
        <div class="filler-item" style="flex: 1;margin: 5px 0px!important;">
          <label class="mr-16px  w-110px">下载模版：</label>
          <a class="text-hover" href="/import/customer.xlsx" :download="`客户导入模版.xlsx`">客户导入模版</a>
        </div>
      </div>
      <div class="filler-panel">
        <div class="filler-item" style="flex: 1;margin: 5px 0px!important;">
          <label class="mr-16px  w-110px">选择上传文件：</label>
          <Button color="primary" :loading="loading" @click="$refs.file.click()">选择文件</Button>
          <span class="ml-10px" v-if="file"> {{ fileName }}  <t-icon @click="file =null"
                                                                name="close" class="text-hover ml-10px" /></span>
          <input type="file" style="visibility: hidden;" @change="fileChange($event)" ref="file"
                 accept="application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet">
        </div>
      </div>
    </div>
    <div class="modal-column-between">
      <Button @click="$emit('close')" :loading="loading">
        取消
      </Button>
      <Button color="primary" :disabled="!file" @click="importData" :loading="loading">
        导 入
      </Button>
    </div>
  </div>
</template>

<script>
import {MessagePlugin} from "tdesign-vue-next";
import Customer from "@js/api/basic/Customer";


export default {
  name: "CustomerImportForm",
  data() {
    return {
      loading: false,
      file: null,
      dataList: [],
      tag: 1,
    }
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
        this.loading = false;
        let formData = new FormData();
        formData.append('file', this.file);
        Customer.importData(formData).then(({data}) => {
          this.$emit('success');
        }).finally(() => {
          this.loading = false
          this.file = null;
          this.$refs.file.value = '';
        });
      } else {
        MessagePlugin.error("请选择上传的文件...");
      }
    }
  }
}
</script>
