<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <Form ref="form" :model="model" :rules="validationRules" :labelWidth="160" :showErrorTip="true">
        <FormItem label="是否关联云财务" prop="linkStatus">
          <Radio v-model="model.linkStatus" :datas="linkRadios"/>
        </FormItem>
        <FormItem label="进销存账套">
          <Select v-model="model.accountBookId" :datas="accountBooks" :deletable="false" @change="changeAccountBook($event)"></Select>
        </FormItem>
        <FormItem label="财务软件URL" prop="url" v-if="model.linkStatus==='关联'">
          <Input v-model="model.url"/>
        </FormItem>
        <FormItem label="财务软件账号" prop="financeAccount" v-if="model.linkStatus==='关联'">
          <Input v-model="model.financeAccount"/>
        </FormItem>
        <FormItem label="财务软件密码" prop="financePassword" v-if="model.linkStatus==='关联'">
          <Input v-model="model.financePassword"/>
        </FormItem>
        <FormItem v-if="model.linkStatus==='关联'">
          <Button icon="fa fa-save" @click="relatedClick" :loading="loading">
            连接云财务
          </Button>
        </FormItem>

        <FormItem label="关联财务软件帐套" prop="financeAccountId" v-if="model.linkStatus==='关联'">
          <Select :datas="accountSetsList" keyName="id" v-model="model.financeAccountId" filterable
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
import FinanceAccountLink from "@js/api/setting/FinanceAccountLink";
import {mapState} from "vuex";
import {ObjectUtil} from "../../js/common/utils";

export default {
  name: "FinanceRelForm",
  emits: {
    close: null,
    success: null
  },
  props: {
    financeAccountLink: Object,
    id: Number,
    type: String
  },
  computed: {
    ...mapState(['accountBooks', 'accountBook'])
  },
  data() {
    return {
      loading: false,
      accountSetsList: [],
      model: {
        id: null,
        financeAccount: null,
        financePassword: null,
        linkStatus: '不关联',
        financeAccountId: null,
        financeAccountName: null,
        accountBookId: null,
        accountBookName: '',
      },
      validationRules: {required: ['financeAccount','financePassword', 'linkStatus', 'financeAccountId', 'accountBookId']},
      linkRadios: [{key: '不关联', title: '不关联'}, {key: '关联', title: '关联'}]
    }
  },
  methods: {
    confirm() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        FinanceAccountLink.save(this.model).then(() => {
          message("保存成功~");
          this.$emit('success');
        }).finally(() => this.loading = false);
      }
    },
    changeSets(item) {
      this.model.financeAccountName = item.companyName
    },
    changeAccountBook(item) {
      const id = item.key;
      // 根据accountBookId加载账套信息
      FinanceAccountLink.loadByAccountBookId(id).then(({data}) => {
        if (ObjectUtil.isEmpty(data)) {
          this.model.accountBookName = item.title;
          return;
        }
        this.model = data || {};
        this.relatedClick();
      });
    },
    relatedClick() {
      const {url, financeAccount, financePassword} = this.model;
      console.info("relatedClick:", url, financeAccount, financePassword);
      if (ObjectUtil.isEmpty(url)) {
        message("请输入财务软件URL～");
        return;
      }
      if (ObjectUtil.isEmpty(financeAccount)) {
        message("请输入财务软件账号～");
        return;
      }
      if (ObjectUtil.isEmpty(financePassword)) {
        message("请输入财务软件密码～");
        return;
      }
      FinanceAccountLink.loadAccountSetsList({
        url: url,
        financeAccount: financeAccount,
        financePassword: financePassword
      }).then(({data: resultData}) => {
        const {data, cookie} = resultData;
        this.accountSetsList = data || [];
        this.model.financeCookie = cookie;
        if (this.accountSetsList.length > 0) {
          this.model.financeAccountId = this.accountSetsList[0].id;
          this.model.financeAccountName = this.accountSetsList[0].companyName;
        }
      });
    },
    init() {
      const id = this.id;
      const type = this.type;
      console.info("id:", id);
      console.info("type:", type);
      console.info("accountBooks:", this.accountBooks);
      switch (type) {
        case "add": {
          if (this.accountBooks) {
            this.model.accountBookId = this.accountBook.key;
            this.model.accountBookName = this.accountBook.title;
          }
          break;
        }
        case "load": {
          if (this.accountBooks) {
            this.model.accountBookId = this.accountBook.key;
            this.model.accountBookName = this.accountBook.title;
            // 根据accountBookId加载账套信息
            FinanceAccountLink.loadByAccountBookId(this.model.accountBookId).then(({data}) => {
              console.log(data);
              if (ObjectUtil.isEmpty(data)) {
                this.model.accountBookId = this.accountBook.key;
                this.model.accountBookName = this.accountBook.title;
                return;
              }
              this.model = data || {};
              this.relatedClick();
            });
          }
          break;
        }
        case "edit": {
          // 根据id加载信息
          FinanceAccountLink.load(id).then(({data}) => {
            if (ObjectUtil.isEmpty(data)) {
              this.model.accountBookId = this.accountBook.key;
              this.model.accountBookName = this.accountBook.title;
              return;
            }
            this.model = data || {};
            this.relatedClick();
          })
          break;
        }
        default:
          break;
      }
    }
  },
  created() {
    this.init();
  }
}
</script>
