<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <t-form ref="form" :data="model" :rules="validationRules" label-width="90px">
        <t-form-item label="模板名称" name="title">
          <t-input v-model="model.title"/>
        </t-form-item>
        <t-form-item label="模板类型" name="type">
          <t-select
              :options="documentTypeDataList"
              :keys="{ value: 'documentType', label: 'documentType' }"
              v-model="model.type"
              filterable
              clearable
              placeholder="选择模板类型"
          />
        </t-form-item>
        <t-form-item label="凭证字" name="wordId">
          <t-select
              :options="voucherWords"
              :keys="{ value: 'id', label: 'word' }"
              v-model="model.wordId"
              filterable
              :clearable="false"
              placeholder="选择凭证字"
          />
        </t-form-item>
      </t-form>

      <t-table
          row-key="_rowKey"
          size="small"
          bordered
          stripe
          hover
          table-layout="fixed"
          :data="templateData"
          :columns="columns"
          max-height="360"
      >
        <template #ops="{ rowIndex }">
          <t-space size="small">
            <t-link theme="primary" @click="adjustRows('insert', rowIndex)">增加</t-link>
            <t-link
                v-if="templateData.length > 1"
                theme="danger"
                @click="adjustRows('delete', rowIndex)"
            >删除</t-link>
          </t-space>
        </template>
        <template #subjectId="{ row, rowIndex }">
          <t-select
              v-model="row.subjectId"
              :options="subjects"
              :keys="{ value: 'id', label: 'subjectName' }"
              filterable
              :clearable="false"
              placeholder="请选择会计科目"
              style="width: 100%"
              @change="() => changeSubject(rowIndex)"
          />
        </template>
      </t-table>
    </div>
    <div class="modal-column-between">
      <t-button variant="outline" :loading="loading" @click="$emit('close')">取消</t-button>
      <t-button theme="primary" :loading="loading" @click="confirm">保存</t-button>
    </div>
  </div>
</template>

<script>
import FinanceAccountLink from '@js/api/setting/FinanceAccountLink';
import FinanceVoucherTemplate from '@js/api/setting/FinanceVoucherTemplate';
import { MessagePlugin } from 'tdesign-vue-next';
import { ObjectUtil } from '@common/utils';

let rowSeq = 0;

function newRow(extra = {}) {
  return { _rowKey: `r-${++rowSeq}`, subjectId: null, ...extra };
}

export default {
  name: 'TemplateConfigForm',
  props: {
    id: [Number, String]
  },
  emits: ['close', 'success'],
  data() {
    return {
      loading: false,
      voucherWords: [],
      subjects: [],
      model: {
        id: null,
        type: null,
        wordId: null,
        title: null
      },
      documentTypeDataList: [
        { id: 1, documentType: '期初余额', type: 1 },
        { id: 2, documentType: '采购入库', type: 1 },
        { id: 3, documentType: '销售退货', type: 1 },
        { id: 4, documentType: '其他入库', type: 1 },
        { id: 5, documentType: '盘盈入库', type: 1 },
        { id: 6, documentType: '调拨入库', type: 1 },
        { id: 7, documentType: '采购退货', type: 1 },
        { id: 8, documentType: '销售出库', type: 1 },
        { id: 9, documentType: '其他出库', type: 1 },
        { id: 10, documentType: '调拨出库', type: 1 },
        { id: 11, documentType: '盘亏出库', type: 1 },
        { id: 12, documentType: '成本调整', type: 1 }
      ],
      validationRules: {
        title: [{ required: true, message: '请输入模板名称', type: 'error' }],
        type: [{ required: true, message: '请选择模板类型', type: 'error' }],
        wordId: [{ required: true, message: '请选择凭证字', type: 'error' }]
      },
      templateData: [],
      columns: [
        { colKey: 'ops', title: '操作', width: 110, align: 'center' },
        { colKey: 'subjectId', title: '会计科目', minWidth: 220 },
        { colKey: 'balanceDirection', title: '借贷方向', width: 100, align: 'center' }
      ]
    };
  },
  methods: {
    adjustRows(type, index) {
      if (type === 'insert') {
        this.templateData.splice(index + 1, 0, newRow());
      } else {
        this.templateData.splice(index, 1);
      }
    },
    confirm() {
      this.$refs.form.validate().then((res) => {
        if (res === true || res.result === true) {
          const filter = this.templateData.filter((item) => !ObjectUtil.isEmpty(item.subjectId));
          if (!filter.length) {
            MessagePlugin.warning('请添加会计科目～');
            return;
          }
          this.model.details = filter.map((item) => ({
            subjectId: item.subjectId,
            subjectName: item.subjectName,
            subjectCode: item.subjectCode,
            balanceDirection: item.balanceDirection,
            auxiliaryAccounting: item.auxiliaryAccounting
          }));
          this.loading = true;
          FinanceVoucherTemplate.save(this.model)
            .then(() => {
              MessagePlugin.success('保存成功~');
              this.$emit('success');
            })
            .finally(() => (this.loading = false));
        }
      }).catch(() => {});
    },
    init() {
      FinanceAccountLink.voucherWord().then(({ data }) => {
        this.voucherWords = data || [];
        this.voucherWords.forEach((word) => {
          if (word.isDefault) {
            this.model.wordId = word.id;
            this.model.word = word.word;
          }
        });
      });
      FinanceAccountLink.subject().then(({ data }) => {
        this.subjects = (data || []).map((subject) => ({
          ...subject,
          subjectName: `${subject.code}-${subject.name}`
        }));
      });
    },
    changeSubject(rowIndex) {
      const row = this.templateData[rowIndex];
      if (!row) return;
      const subject = this.subjects.find((item) => item.id === row.subjectId);
      if (!subject) return;
      row.balanceDirection = subject.balanceDirection;
      row.subjectName = subject.subjectName;
      row.subjectCode = subject.code;
      row.auxiliaryAccounting = subject.auxiliaryAccounting;
    },
    load() {
      FinanceVoucherTemplate.load(this.id).then(({ data }) => {
        this.model = data;
        this.templateData = (data.details || []).map((item) => newRow(item));
      });
    }
  },
  created() {
    this.init();
    if (this.id) {
      this.load();
    } else {
      for (let i = 0; i < 5; i++) {
        this.templateData.push(newRow());
      }
    }
  }
};
</script>
