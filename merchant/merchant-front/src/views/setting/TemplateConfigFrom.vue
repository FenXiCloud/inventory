<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <Form ref="form" :model="model" :rules="validationRules" :labelWidth="120" mode="twocolumn">
        <template v-for="(relation,index) in relationList">
          <FormItem :label="relation.title">
            <Select :datas="subjectList" v-model="relation.subjectId" keyName="id" titleName="name" :deletable="false"
                    filterable :filter="filter" @change="changeSub($event,relation)">
              <template v-slot:show="{value}">
                {{ value.code }}{{ value.name }}
              </template>
              <template v-slot:item="{ item }">
                {{ item.code }} {{ item.name }}
              </template>
            </Select>
          </FormItem>
        </template>
      </Form>
    </div>
    <div class="modal-column-right">
      <Button icon="fa fa-save" style="justify-content: right" color="primary" @click="confirm" :loading="loading">
        保存
      </Button>
    </div>
  </div>
</template>

<script>

import {message} from "heyui.ext";
import FinanceRel from "@js/api/setting/FinanceRel";

export default {
  name: "TemplateConfigForm",
  emits: {
    close: null,
    success: null
  },
  props: {
    relationCwId: Number,
  },
  data() {
    return {
      loading: false,
      subjectList: [],
      relationList: [],
      model: {
        id: null,
        isRelation: false,
        accountSetsId: null,
        companyName: null,
        organizationName: '',
      },
      validationRules: {}
    }
  },
  methods: {
    filter(item, val) {
      return item.code.indexOf(val) !== -1 || item.name.indexOf(val) !== -1 || item.mnemonicCode.indexOf(val) !== -1;
    },
    confirm() {
      this.loading = true;

      let relations = this.relationList.filter(c => c.subjectId === null);
      if (relations.length > 0) {
        message.error("还有未选择的对应科目~");
        this.loading = false
        return
      }
      RelationSubject.save(this.relationList).then(() => {
        message("保存成功~");
        this.$emit('success');
      }).finally(() => this.loading = false);
    },
    changeSub(event, row) {
      row.subjectId = event.id.toString()
      row.code = event.code
      row.name = event.name
    }
  },
  created() {
    Promise.all([
      FinanceRel.loadSubject(),
      FinanceRel.load(this.relationCwId),
    ]).then((results) => {
      this.subjectList = results[0].data || [];
      this.relationList = results[1].data || [];
    })
  }
}
</script>
