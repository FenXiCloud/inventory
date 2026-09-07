<template>
  <div class="simple-page">
    <div class="import-card">
      <div class="import-explain">
        <h6>导入说明：</h6>
        <p>1. 每行数据生成一张独立的采购入库单（含一个商品明细）</p>
        <p>2. 导入文件支持 xls、xlsx 格式，大小不超过 4M，数据不超过 1000 行.</p>
        <p>3. 导入文件不能包含"合并单元格"，否则无法导入.</p>
      </div>
      <div class="import-row">
        <label class="import-row__label">下载模版：</label>
        <a class="text-hover" href="/import/purchase_inbound.xlsx" download="采购入库单导入模版.xlsx">采购入库单导入模版</a>
      </div>
      <div class="import-row">
        <label class="import-row__label">选择上传文件：</label>
        <t-button theme="primary" :loading="loading" @click="$refs.file.click()">选择文件</t-button>
        <span class="import-row__file" v-if="file">{{ file.name }}
          <t-icon name="close" class="text-hover" @click="clearFile"/>
        </span>
        <input type="file" style="display: none" @change="fileChange" ref="file"
               accept="application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet">
      </div>
      <div class="import-row">
        <t-button theme="primary" :disabled="!file" :loading="loading" v-auth="'purchaseInbound:edit'" @click="importData">导 入</t-button>
      </div>
    </div>
  </div>
</template>

<script>
import {MessagePlugin} from 'tdesign-vue-next';
import PurchaseInbound from '@js/api/purchase/PurchaseInbound';

export default {
  name: 'PurchaseInboundImport',
  data() {
    return {loading: false, file: null};
  },
  methods: {
    fileChange() {
      this.file = this.$refs.file.files[0];
      if (this.file && this.file.size > (4 * 1024 * 1024)) {
        this.file = null;
        this.$refs.file.value = '';
        MessagePlugin.error('一次最大导入4MB数据...');
      }
    },
    clearFile() {
      this.file = null;
      this.$refs.file.value = '';
    },
    importData() {
      if (!this.file) {
        MessagePlugin.error('请选择上传的文件...');
        return;
      }
      this.loading = true;
      const formData = new FormData();
      formData.append('file', this.file);
      PurchaseInbound.importData(formData)
        .then(() => {
          MessagePlugin.success('导入成功');
          this.clearFile();
        })
        .finally(() => (this.loading = false));
    }
  }
};
</script>

<style scoped>
.import-card {
  background: #fff;
  border-radius: 4px;
  padding: 24px;
  box-sizing: border-box;
}

.import-explain {
  margin-bottom: 16px;
  padding: 12px 16px;
  border-radius: 4px;
  background: #f3f3f3;
  color: #555;
  font-size: 13px;
  line-height: 1.8;
}

.import-explain h6 {
  margin: 0 0 4px;
  font-size: 14px;
  color: #333;
}

.import-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.import-row__label {
  width: 110px;
  color: #555;
  font-size: 14px;
  text-align: right;
}

.import-row__file {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #333;
}

.text-hover {
  cursor: pointer;
  color: #0052d9;
}

.text-hover:hover {
  text-decoration: underline;
}
</style>
