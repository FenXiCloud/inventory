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
      <vxe-table size="mini" ref="xTable" border="border" show-overflow keep-source
                 :row-config="{ height: 40, isCurrent: true, isHover: true }"
                 show-footer stripe
                 :data="templateData">
        <vxe-column title="操作" field="seq" width="70" align="center" fixed="left">
          <template #default="{ row, rowIndex }">
            <div>
              <div class="fa fa-plus text-hover mr-5px" @click="adjustRows('insert', rowIndex)"></div>
              <div v-if="templateData.length !== 1" class="fa fa-minus text-hover-danger"
                   @click="adjustRows('delete', rowIndex)"></div>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="会计科目" field="warehouseName">
          <template #default="scope">
            <div class="input-group goodsSelect">
              <t-select
                  :clearable="false"
                  ref="ms"
                  v-model="scope.row.subjectId"
                  :options="subjects"
                  filterable
                  placeholder="请选择会计科目"
                  :keys="{ value: 'id', label: 'subjectName' }"
                  @change="changeSubject(scope)"
              />
            </div>
          </template>
        </vxe-column>
        <vxe-column field="balanceDirection" title="借贷方向" width="100"></vxe-column>
      </vxe-table>
    </div>
    <div class="modal-column-between">
      <t-button variant="outline" :loading="loading" @click="$emit('close')">取消</t-button>
      <t-button theme="primary" :loading="loading" @click="confirm">保存</t-button>
    </div>
  </div>
</template>

<script>
import FinanceAccountLink from "@js/api/setting/FinanceAccountLink";
import FinanceVoucherTemplate from "@js/api/setting/FinanceVoucherTemplate";
import {MessagePlugin} from "tdesign-vue-next";
import {ObjectUtil} from "../../js/common/utils";

export default {
  name: "TemplateConfigForm",
  props: {
    id: [Number, String],
  },
  data() {
    return {
      loading: false,
      canSave: false,
      voucherWords: [],
      subjects: [],
      model: {
        id: null,
        type: null,
        wordId: null,
        title: null,
      },
      documentTypeDataList: [
        {id: 1, documentType: '期初余额', type: 1},
        {id: 2, documentType: '采购入库', type: 1},
        {id: 3, documentType: '销售退货', type: 1},
        {id: 4, documentType: '其他入库', type: 1},
        {id: 5, documentType: '盘盈入库', type: 1},
        {id: 6, documentType: '调拨入库', type: 1},
        {id: 7, documentType: '采购退货', type: 1},
        {id: 8, documentType: '销售出库', type: 1},
        {id: 9, documentType: '其他出库', type: 1},
        {id: 10, documentType: '调拨出库', type: 1},
        {id: 11, documentType: '盘亏出库', type: 1},
        {id: 12, documentType: '成本调整', type: 1}
      ],
      validationRules: {
        required: ['title', 'type', 'wordId']
      },
      templateData: []
    }
  },
  watch: {},
  methods: {
    //添加行或减少行
    adjustRows(type, index) {
      if (type === "insert") {
        this.templateData.splice(index + 1, 0, {isNew: true});
      } else {
        this.templateData.splice(index, 1);
      }
    },
    confirm() {
      this.$refs.form.validate().then((res) => {
        if (res === true || res.result === true) {
          const filter = this.templateData.filter(item => {
            return !ObjectUtil.isEmpty(item.subjectId)
          });
          if (filter && filter.length === 0) {
            MessagePlugin.success("请添加会计科目～");
            return;
          }
          const details = [];
          filter.forEach(item => {
            details.push({
              subjectId: item.subjectId,
              subjectName: item.subjectName,
              subjectCode: item.subjectCode,
              balanceDirection: item.balanceDirection,
              auxiliaryAccounting: item.auxiliaryAccounting
            });
          });
          this.model.details = details;
          this.loading = true;
          FinanceVoucherTemplate.save(this.model).then(() => {
            MessagePlugin.success("保存成功~");
            this.$emit('success');
          }).finally(() => this.loading = false);
        }
      }).catch(() => {});
    },
    init() {
      FinanceAccountLink.voucherWord().then(({data}) => {
        this.voucherWords = data;
        this.voucherWords.forEach(word => {
          if (word.isDefault) {
            this.model.wordId = word.id;
            this.model.word = word.word;
          }
        });
      });
      FinanceAccountLink.subject().then(({data}) => {
        this.subjects = data;
        this.subjects.forEach(subject => {
          subject.subjectName = `${subject.code}-${subject.name}`;
        });
      });
    },
    changeSubject({rowIndex}) {
      const subjectId = this.templateData[rowIndex].subjectId;
      const filter = this.subjects.filter((item) => {
        return item.id === subjectId
      });
      this.templateData[rowIndex].balanceDirection = filter[0].balanceDirection;
      this.templateData[rowIndex].subjectName = filter[0].subjectName;
      this.templateData[rowIndex].subjectCode = filter[0].code;
      this.templateData[rowIndex].auxiliaryAccounting = filter[0].auxiliaryAccounting;
    },
    load() {
      const id = this.id;
      FinanceVoucherTemplate.load(id).then(({data}) => {
        this.model = data;
        this.templateData = data.details;
      });
    }
  },
  created() {
    this.init();
    if (this.id) {
      this.load();
    } else {
      for (let i = 0; i < 5; i++) {
        this.templateData.push({subjectId: null});
      }
    }
  }
}
</script>

<style scoped lang="less">

</style>
