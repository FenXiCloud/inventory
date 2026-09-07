<template>
  <div class="modal-column customer-form">
    <div class="modal-column-full-body">
      <t-form
          ref="form"
          :data="model"
          :rules="rules"
          layout="vertical"
          label-align="top"
          scroll-to-first-error="smooth"
      >
        <t-row :gutter="[16, 8]">
          <t-col :span="6">
            <t-form-item label="客户编码" name="code">
              <t-input
                  v-model="model.code"
                  placeholder="编码（不填写系统自动生成）"
                  :disabled="!!model.id"
              />
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="客户名称" name="name">
              <t-input v-model="model.name" placeholder="请输入客户名称"/>
            </t-form-item>
          </t-col>

          <t-col :span="6">
            <t-form-item label="客户分类" name="customerCategoryId">
              <t-select
                  v-model="model.customerCategoryId"
                  :options="customerCategoryList"
                  :keys="{ value: 'id', label: 'name' }"
                  filterable
                  clearable
                  placeholder="请选择客户分类"
              />
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="客户等级" name="customerLevelId">
              <t-select
                  v-model="model.customerLevelId"
                  :options="customerLevelList"
                  :keys="{ value: 'id', label: 'name' }"
                  filterable
                  clearable
                  placeholder="请选择客户等级"
              />
            </t-form-item>
          </t-col>

          <t-col :span="6">
            <t-form-item label="联系人" name="contact">
              <t-input v-model.trim="model.contact" placeholder="联系人"/>
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="电话" name="phone">
              <t-input v-model.trim="model.phone" placeholder="电话"/>
            </t-form-item>
          </t-col>

          <t-col :span="6">
            <t-form-item label="税号" name="taxNo">
              <t-input v-model.trim="model.taxNo" placeholder="税号" :maxlength="32"/>
            </t-form-item>
          </t-col>

          <t-col :span="6">
            <t-form-item label="应收余额" name="balance" tips="正数=客户欠款，负数=预收；由单据/期初维护">
              <t-input-number
                  v-model="model.balance"
                  theme="normal"
                  :decimal-places="2"
                  placeholder="0.00"
                  disabled
                  style="width: 100%"
              />
            </t-form-item>
          </t-col>

          <t-col :span="12">
            <t-form-item label="客户描述" name="remarks">
              <t-textarea
                  v-model="model.remarks"
                  placeholder="客户描述"
                  :maxlength="150"
                  :autosize="{ minRows: 3, maxRows: 5 }"
              />
            </t-form-item>
          </t-col>
        </t-row>
      </t-form>

      <div class="customer-attachments">
        <div v-for="group in attachmentGroups" :key="group.value" class="customer-attachments__panel">
          <div class="customer-attachments__hd">
            <div class="customer-attachments__title">
              {{ group.label }}
              <span v-if="attachmentsOf(group.value).length" class="customer-attachments__count">
                {{ attachmentsOf(group.value).length }}
              </span>
            </div>
            <div class="customer-attachments__hd-right">
              <span class="customer-attachments__hint">{{ group.hint }}</span>
              <t-button
                  size="small"
                  variant="outline"
                  theme="primary"
                  :loading="uploadingGroup === group.value"
                  @click="pickFor(group)"
              >上传附件</t-button>
              <input
                  :ref="(el) => (fileInputs[group.value] = el)"
                  type="file"
                  :accept="accept"
                  multiple
                  class="customer-attachments__input"
                  @change="onPickFiles($event, group.value)"
              />
            </div>
          </div>

          <ul v-if="attachmentsOf(group.value).length" class="customer-attachments__list">
            <li v-for="att in attachmentsOf(group.value)" :key="att.filePath" class="customer-attachments__item">
              <span class="customer-attachments__ext" :class="extKind(att.name)">{{ extLabel(att.name) }}</span>
              <span class="customer-attachments__name" :title="att.name">{{ att.name }}</span>
              <span class="customer-attachments__size">{{ humanSize(att.fileSize) }}</span>
              <span class="customer-attachments__links">
                <t-link theme="primary" @click="openAttachment(att)">查看</t-link>
                <t-link theme="danger" @click="removeAttachment(att)">删除</t-link>
              </span>
            </li>
          </ul>
          <div v-else class="customer-attachments__empty">暂无附件</div>
        </div>
      </div>
    </div>

    <div class="modal-column-between">
      <t-button variant="outline" :loading="loading" @click="$emit('close')">取消</t-button>
      <t-button v-auth="'customer:edit'" theme="primary" :loading="loading" @click="confirm">保存</t-button>
    </div>
  </div>
</template>

<script>
import Customer from '@js/api/basic/Customer';
import {MessagePlugin} from 'tdesign-vue-next';
import {CopyObj} from '@common/utils';
import CustomerCategory from '@js/api/basic/CustomerCategory';
import CustomerLevel from '@js/api/basic/CustomerLevel';
import {Upload} from '@js/api/App';

export default {
  name: 'CustomerForm',
  emits: {close: null, success: null},
  props: {
    entity: Object
  },
  data() {
    return {
      loading: false,
      customerCategoryList: [],
      customerLevelList: [],
      attachmentGroups: [
        {value: 'certificate', label: '证件附件', hint: '营业执照、开户许可证、法人身份证等'},
        {value: 'qualification', label: '资格证附件', hint: '若客户行业有资质要求（医疗/建筑/进出口/环保等），请上传对应许可证'}
      ],
      uploadingGroup: null,
      fileInputs: {},
      accept: '.pdf,.jpg,.jpeg,.png,.webp,.gif,.doc,.docx,.xls,.xlsx,.ppt,.pptx',
      allowExts: ['pdf', 'jpg', 'jpeg', 'png', 'webp', 'gif', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'],
      maxSize: 20 * 1024 * 1024,
      model: {
        balance: 0,
        id: null,
        code: null,
        name: null,
        contact: null,
        phone: null,
        taxNo: null,
        customerCategoryId: null,
        customerLevelId: null,
        remarks: null,
        attachments: []
      },
      rules: {
        name: [{required: true, message: '请输入客户名称', type: 'error'}],
        customerCategoryId: [{required: true, message: '请选择客户分类', type: 'error'}],
        customerLevelId: [{required: true, message: '请选择客户等级', type: 'error'}]
      }
    };
  },
  methods: {
    confirm() {
      this.$refs.form.validate().then((result) => {
        if (result !== true) return;
        this.loading = true;
        const payload = {...this.model};
        if (!payload.id) {
          payload.balance = 0;
        }
        Customer.save(payload)
          .then(() => {
            MessagePlugin.success('保存成功~');
            this.$emit('success');
          })
          .finally(() => (this.loading = false));
      }).catch(() => {});
    },
    attachmentsOf(category) {
      return (this.model.attachments || []).filter((att) => att.category === category);
    },
    extLabel(fileName) {
      const ext = this.extOf(fileName);
      return ext ? ext.toUpperCase() : 'FILE';
    },
    extKind(fileName) {
      const ext = this.extOf(fileName);
      if (['jpg', 'jpeg', 'png', 'webp', 'gif'].includes(ext)) return 'is-img';
      if (ext === 'pdf') return 'is-pdf';
      if (ext === 'doc' || ext === 'docx') return 'is-doc';
      if (ext === 'xls' || ext === 'xlsx') return 'is-xls';
      if (ext === 'ppt' || ext === 'pptx') return 'is-ppt';
      return 'is-other';
    },
    extOf(fileName) {
      const idx = (fileName || '').lastIndexOf('.');
      return idx >= 0 ? fileName.slice(idx + 1).toLowerCase() : '';
    },
    humanSize(size) {
      if (size == null) return '';
      if (size < 1024) return size + ' B';
      if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB';
      return (size / (1024 * 1024)).toFixed(1) + ' MB';
    },
    pickFor(group) {
      const input = this.fileInputs[group.value];
      if (input) input.click();
    },
    async onPickFiles(e, category) {
      const files = Array.from(e.target.files || []);
      e.target.value = '';
      if (!files.length) return;

      const invalid = files.filter((file) => !this.allowExts.includes(this.extOf(file.name)) || file.size > this.maxSize);
      if (invalid.length) {
        MessagePlugin.warning(`已忽略 ${invalid.length} 个文件：仅支持图片/PDF/Word/Excel/PPT，且单个不超过 20MB`);
      }
      const valid = files.filter((file) => this.allowExts.includes(this.extOf(file.name)) && file.size <= this.maxSize);
      if (!valid.length) return;

      this.uploadingGroup = category;
      let ok = 0;
      let fail = 0;
      for (const file of valid) {
        const params = new FormData();
        params.append('file', file);
        try {
          const {data} = await Upload('customer', params);
          if (data && data.path) {
            this.model.attachments.push({
              category,
              name: file.name,
              filePath: data.path,
              fileSize: file.size
            });
            ok++;
          } else {
            fail++;
          }
        } catch (err) {
          fail++;
        }
      }
      this.uploadingGroup = null;
      if (ok && !fail) MessagePlugin.success(`${ok} 个附件上传成功~`);
      else if (ok && fail) MessagePlugin.success(`${ok} 个附件上传成功，${fail} 个失败`);
      else if (fail) MessagePlugin.error('附件上传失败，请重试');
    },
    openAttachment(att) {
      if (att && att.filePath) window.open(att.filePath, '_blank');
    },
    removeAttachment(att) {
      const list = this.model.attachments || [];
      const idx = list.findIndex((x) => x === att || (x.filePath && x.filePath === att.filePath));
      if (idx > -1) list.splice(idx, 1);
    }
  },
  created() {
    CopyObj(this.model, this.entity);
    Promise.all([CustomerCategory.select(), CustomerLevel.select()]).then((results) => {
      this.customerCategoryList = results[0].data || [];
      this.customerLevelList = results[1].data || [];
    });
  }
};
</script>

<style scoped>
.customer-form :deep(.t-form__item) {
  margin-bottom: 8px;
}

.customer-form :deep(.t-form__label) {
  padding-bottom: 4px !important;
}

.customer-attachments {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.customer-attachments__panel {
  border: 1px solid var(--td-component-border, #e7e7e7);
  border-radius: 6px;
  padding: 10px 12px;
  background: #fafafa;
  min-width: 0;
}

.customer-attachments__hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.customer-attachments__title {
  font-weight: 600;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.customer-attachments__count {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  box-sizing: border-box;
  border-radius: 9px;
  background: var(--td-brand-color, #0052d9);
  color: #fff;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
}

.customer-attachments__hd-right {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.customer-attachments__hint {
  font-size: 12px;
  color: #999;
  line-height: 1.5;
}

.customer-attachments__input {
  display: none;
}

.customer-attachments__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.customer-attachments__item {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fff;
  border: 1px solid var(--td-component-border, #ececec);
  border-radius: 4px;
  padding: 5px 8px;
  min-width: 0;
}

.customer-attachments__ext {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 600;
  padding: 1px 5px;
  border-radius: 3px;
  color: #fff;
}

.customer-attachments__ext.is-img {
  background: #00a870;
}

.customer-attachments__ext.is-pdf {
  background: #e34d59;
}

.customer-attachments__ext.is-doc {
  background: #1470cc;
}

.customer-attachments__ext.is-xls {
  background: #2f9b32;
}

.customer-attachments__ext.is-ppt {
  background: #ed7b2f;
}

.customer-attachments__ext.is-other {
  background: #909399;
}

.customer-attachments__name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #333;
  font-size: 13px;
}

.customer-attachments__size {
  flex-shrink: 0;
  font-size: 12px;
  color: #999;
}

.customer-attachments__links {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.customer-attachments__empty {
  text-align: center;
  color: #bbb;
  font-size: 12px;
  padding: 12px 0;
  border: 1px dashed #ddd;
  border-radius: 4px;
  background: #fff;
}
</style>
