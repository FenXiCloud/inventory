<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <Form ref="form" :model="model" mode="threecolumn" :rules="validationRules" :showErrorTip="true" :labelWidth="90">
        <FormItem label="模板名称" prop="title">
          <Input v-model="model.title"/>
        </FormItem>
        <FormItem label="模板类型" prop="type">
          <Select :datas="documentTypeDataList" keyName="documentType" v-model="model.type" filterable
                  titleName="documentType" placeholder="选择模板类型"/>
        </FormItem>
        <FormItem label="凭证字" prop="wordId">
          <Select :datas="voucherWords" keyName="id" v-model="model.wordId" filterable :deletable="false"
                  titleName="word" placeholder="选择凭证字"/>
        </FormItem>
      </Form>
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
            <div class="h-input-group goodsSelect">
              <Select :deletable="false" ref="ms" v-model="scope.row.subjectId" :datas="subjects" filterable
                      placeholder="请选择会计科目" keyName="id" titleName="subjectName" @change="changeSubject(scope)">
                <template v-slot:item="{ item }">
                  <div>{{ item.subjectName }}</div>
                </template>
              </Select>
            </div>
          </template>
        </vxe-column>
        <vxe-column field="balanceDirection" title="借贷方向" width="100"></vxe-column>
      </vxe-table>
    </div>
    <div class="modal-column-right">
      <Button icon="fa fa-close" @click="$emit('close')" :loading="loading">
        取消
      </Button>
      <Button icon="fa fa-save" color="primary" @click="confirm" :loading="loading">
        保存
      </Button>
    </div>
  </div>
</template>

<script>
import FinanceAccountLink from "@js/api/setting/FinanceAccountLink";
import FinanceVoucherTemplate from "@js/api/setting/FinanceVoucherTemplate";
import {message} from "heyui.ext";
import {ObjectUtil} from "../../js/common/utils";

export default {
  name: "TemplateConfigFrom",
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
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        const filter = this.templateData.filter(item => {
          return !ObjectUtil.isEmpty(item.subjectId)
        });
        if (filter && filter.length === 0) {
          message("请添加会计科目～");
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
          message("保存成功~");
          this.$emit('success');
        }).finally(() => this.loading = false);
      }
    },
    init() {
      FinanceAccountLink.loadVoucherWord().then(({data}) => {
        this.voucherWords = data;
        this.voucherWords.forEach(word => {
          if (word.isDefault) {
            this.model.wordId = word.id;
            this.model.word = word.word;
          }
        });
      });
      FinanceAccountLink.loadSubject().then(({data}) => {
        console.info("subjects:", data)
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
      console.log(filter[0])
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
