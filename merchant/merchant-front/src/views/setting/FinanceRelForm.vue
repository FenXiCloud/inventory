<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <t-form ref="form" :data="model" :rules="validationRules" label-width="160px">
        <t-form-item label="是否关联云财务" name="linkStatus">
          <t-radio-group v-model="model.linkStatus">
            <t-radio value="不关联">不关联</t-radio>
            <t-radio value="关联">关联</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item label="进销存账套">
          <t-select
              v-model="model.accountBookId"
              :disabled="true"
              :options="accountBooks"
              :keys="{ value: 'key', label: 'title' }"
              :clearable="false"
              @change="changeAccountBook"
          />
        </t-form-item>
        <t-form-item label="财务软件URL" name="url" v-if="model.linkStatus==='关联'">
          <t-input v-model="model.url"/>
        </t-form-item>
        <t-form-item label="财务软件账号" name="financeAccount" v-if="model.linkStatus==='关联'">
          <t-input v-model="model.financeAccount"/>
        </t-form-item>
        <t-form-item label="财务软件密码" name="financePassword" v-if="model.linkStatus==='关联'">
          <t-input v-model="model.financePassword"/>
        </t-form-item>
        <t-form-item v-if="model.linkStatus==='关联'">
          <t-button @click="relatedClick" :loading="loading">
            连接云财务
          </t-button>
        </t-form-item>

        <t-form-item label="关联财务软件帐套" name="financeAccountId" v-if="model.linkStatus==='关联'">
          <t-select
              :options="accountSetsList"
              :keys="{ value: 'id', label: 'companyName' }"
              v-model="model.financeAccountId"
              filterable
              clearable
              placeholder="关联财务系统帐套"
              @change="changeSets"
          />
        </t-form-item>
      </t-form>
    </div>
    <div class="modal-column-between">
      <t-button variant="outline" :loading="loading" @click="$emit('close')">取消</t-button>
      <t-button theme="primary" :loading="loading" @click="confirm">保存</t-button>
    </div>
  </div>
</template>

<script>
import {MessagePlugin} from "tdesign-vue-next";
import FinanceAccountLink from "@js/api/setting/FinanceAccountLink";
import {mapState} from "vuex";
import {ObjectUtil} from '@common/utils';

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
      this.$refs.form.validate().then((res) => {
        if (res === true || res.result === true) {
          this.loading = true;
          FinanceAccountLink.save(this.model).then(() => {
            MessagePlugin.success("保存成功~");
            this.$emit('success');
          }).finally(() => this.loading = false);
        }
      }).catch(() => {});
    },
    changeSets(item) {
      this.model.financeAccountName = item.companyName
    },
    changeAccountBook(item) {
      const id = item.key;
      // 根据accountBookId加载账套信息
      FinanceAccountLink.byAccountBook(id).then(({data}) => {
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
      if (ObjectUtil.isEmpty(url)) {
        MessagePlugin.success("请输入财务软件URL～");
        return;
      }
      if (ObjectUtil.isEmpty(financeAccount)) {
        MessagePlugin.success("请输入财务软件账号～");
        return;
      }
      if (ObjectUtil.isEmpty(financePassword)) {
        MessagePlugin.success("请输入财务软件密码～");
        return;
      }
      FinanceAccountLink.accountSets({
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
            FinanceAccountLink.byAccountBook(this.model.accountBookId).then(({data}) => {
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
