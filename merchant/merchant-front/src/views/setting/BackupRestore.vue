<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-input
            v-model="remarks"
            clearable
            placeholder="备份说明（可选）"
            style="width: 240px; border-radius: 4px"
            :maxlength="100"
        />
        <t-button theme="primary" style="border-radius: 4px" :loading="creating" @click="doBackup">立即备份</t-button>
        <t-upload
            v-model="uploadFiles"
            :auto-upload="false"
            theme="custom"
            accept=".json,application/json"
            :show-upload-progress="false"
            :allow-upload-duplicate="true"
            @change="onUploadChange"
        >
          <t-button variant="outline" style="border-radius: 4px" :loading="restoring">上传恢复</t-button>
        </t-upload>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="loadList">刷新</t-button>
      </t-space>
    </div>

    <div class="simple-page__hint">
      备份内容：打印模板、编码规则、收支类别、结算方式。
      恢复时：打印模板与编码规则整表覆盖；收支类别与结算方式按名称合并（保留原 ID，避免单据外键失效）。
    </div>

    <div class="simple-page__table">
      <t-table
          row-key="id"
          size="medium"
          bordered
          stripe
          hover
          height="100%"
          table-layout="fixed"
          :data="dataList"
          :columns="columns"
          :loading="loading"
          empty="暂无备份记录，可点击「立即备份」生成"
      >
        <template #fileSize="{ row }">
          {{ formatSize(row.fileSize) }}
        </template>
        <template #ops="{ row }">
          <t-space size="small">
            <t-link theme="primary" @click="doDownload(row)">下载</t-link>
            <t-link theme="warning" @click="doRestore(row)">恢复</t-link>
            <t-link theme="danger" @click="doRemove(row)">删除</t-link>
          </t-space>
        </template>
      </t-table>
    </div>
  </div>
</template>

<script>
import DataBackup from '@js/api/setting/DataBackup';
import { DialogPlugin, MessagePlugin } from 'tdesign-vue-next';
import { downloadBlob } from 'download.js';

/**
 * @功能描述: 备份与恢复
 */
export default {
  name: 'BackupRestore',
  data() {
    return {
      loading: false,
      creating: false,
      restoring: false,
      remarks: '',
      uploadFiles: [],
      dataList: [],
      columns: [
        { colKey: 'fileName', title: '备份文件', minWidth: 220, ellipsis: true },
        { colKey: 'fileSize', title: '大小', width: 100, align: 'right' },
        { colKey: 'remarks', title: '说明', minWidth: 200, ellipsis: true },
        { colKey: 'createdByName', title: '操作人', width: 100, align: 'center' },
        { colKey: 'createdAt', title: '备份时间', width: 180, align: 'center' },
        { colKey: 'ops', title: '操作', width: 180, fixed: 'right', align: 'center' }
      ]
    };
  },
  methods: {
    formatSize(size) {
      const num = Number(size || 0);
      if (num < 1024) return num + ' B';
      if (num < 1024 * 1024) return (num / 1024).toFixed(1) + ' KB';
      return (num / 1024 / 1024).toFixed(2) + ' MB';
    },
    pickUploadFile(files) {
      const list = Array.isArray(files) ? files : [];
      const item = list[0];
      if (!item) return null;
      return item.raw || item.originFileObj || item.file || (item instanceof File ? item : null);
    },
    loadList() {
      this.loading = true;
      DataBackup.list()
        .then(({ data }) => {
          this.dataList = data || [];
        })
        .finally(() => (this.loading = false));
    },
    doBackup() {
      DialogPlugin.confirm({
        title: '系统提示',
        content: '确认立即备份当前账套的系统设置（打印模板/编码规则/收支类别/结算方式）？',
        onConfirm: () => {
          this.creating = true;
          DataBackup.create({ remarks: (this.remarks || '').trim() || '手动备份' })
            .then(() => {
              MessagePlugin.success('备份成功~');
              this.remarks = '';
              this.loadList();
            })
            .finally(() => (this.creating = false));
        }
      });
    },
    doDownload(row) {
      DataBackup.download(row.id).then((blob) => {
        downloadBlob(row.fileName || 'backup.json', blob);
      });
    },
    doRestore(row) {
      DialogPlugin.confirm({
        title: '系统提示',
        content: `确认用备份「${row.fileName}」恢复当前账套设置？打印模板与编码规则将被覆盖，收支类别与结算方式按名称合并。`,
        onConfirm: () => {
          this.restoring = true;
          DataBackup.restore(row.id)
            .then(() => {
              MessagePlugin.success('恢复成功~');
              this.loadList();
            })
            .finally(() => (this.restoring = false));
        }
      });
    },
    onUploadChange(files) {
      const file = this.pickUploadFile(files);
      this.uploadFiles = [];
      if (!file) return;
      if (file.name && !/\.json$/i.test(file.name)) {
        MessagePlugin.warning('请选择 .json 备份文件');
        return;
      }
      DialogPlugin.confirm({
        title: '系统提示',
        content: `确认上传并恢复文件「${file.name}」？打印模板与编码规则将被覆盖，收支类别与结算方式按名称合并。`,
        onConfirm: () => {
          const formData = new FormData();
          formData.append('file', file);
          this.restoring = true;
          DataBackup.restoreUpload(formData)
            .then(() => {
              MessagePlugin.success('恢复成功~');
              this.loadList();
            })
            .finally(() => (this.restoring = false));
        }
      });
    },
    doRemove(row) {
      DialogPlugin.confirm({
        title: '系统提示',
        content: `确认删除备份「${row.fileName}」？`,
        onConfirm: () => {
          DataBackup.remove(row.id).then(() => {
            MessagePlugin.success('删除成功~');
            this.loadList();
          });
        }
      });
    }
  },
  created() {
    this.loadList();
  }
};
</script>

<style scoped>
.simple-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
  padding: 0 12px;
  box-sizing: border-box;
  overflow: hidden;
}

.simple-page__toolbar {
  flex-shrink: 0;
  padding: 8px 0;
}

.simple-page__hint {
  flex-shrink: 0;
  margin-bottom: 8px;
  padding: 10px 12px;
  border-radius: 4px;
  background: #f3f3f3;
  color: #555;
  font-size: 13px;
  line-height: 1.6;
}

.simple-page__table {
  flex: 1 1 0;
  height: 0;
  min-height: 0;
  overflow: hidden;
}
</style>
