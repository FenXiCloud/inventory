<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <Form ref="form" :model="model" :rules="validationRules" :labelWidth="160">
        <FormItem label="是否关联云财务" prop="name">
          <Radio v-model="model.state" dict="relRadios"/>
        </FormItem>
        <FormItem label="进销存账套">
          <Input v-model="model.accountBookName" disabled="true"/>
        </FormItem>
        <FormItem label="财务软件URL" prop="url" v-if="model.state==='关联'">
          <Input v-model="model.url"/>
        </FormItem>
        <FormItem label="财务软件账号" prop="mobile" v-if="model.state==='关联'">
          <Input v-model="model.mobile"/>
        </FormItem>
        <FormItem label="财务软件密码" prop="password" v-if="model.state==='关联'">
          <Input v-model="model.password"/>
        </FormItem>
        <FormItem v-if="model.state==='关联'">
          <Button icon="fa fa-save" @click="$emit('close')" :loading="loading">
            连接云财务
          </Button>
        </FormItem>

        <FormItem label="关联财务软件帐套" prop="accountSetsId" v-if="model.state==='关联'">
          <Select :datas="accountSetsList" keyName="accountSetsId" v-model="model.accountSetsId" filterable
                  titleName="companyName" placeholder="关联财务系统帐套" @change="changeSets($event)"/>
        </FormItem>
      </Form>
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

import {message} from "heyui.ext";
import {CopyObj} from "@common/utils";
import manba from "manba";
import FinanceRel from "@js/api/setting/FinanceRel";

export default {
  name: "FinanceRelForm",
  emits: {
    close: null,
    success: null
  },
  props: {
    financeRel: Object,
  },
  data() {
    return {
      loading: false,
      accountSetsList: [],
      model: {
        id: null,
        state: '不关联',
        accountSetsId: null,
        companyName: null,
        accountBookName: '',
      },
      validationRules: {}
    }
  },
  methods: {
    confirm() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        this.model.startDate = manba(this.model.startDate).format("YYYY-MM")
        FinanceRel.save(this.model).then(() => {
          message("保存成功~");
          this.$emit('success');
        }).finally(() => this.loading = false);
      }
    },
    changeSets(item) {
      this.model.companyName = item.companyName
    },
  },
  created() {
    CopyObj(this.model, this.cwRelation);
    // Relation.loadAccountSets(this.cwRelation.id).then(({data})=>{
    //   this.accountSetsList = data||[]
    // })
  }
}
</script>
