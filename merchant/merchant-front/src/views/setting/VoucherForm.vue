<template>
  <div class="h-panel" style="width: 1000px;margin: 0 auto;padding-top: 20px;">
    <div class="padding-right-left">
      <Row type="flex" :space-x="20">
        <Cell>
          <Button :disabled="!canSave || !!form.auditMemberId" color="primary" @click="save(false)"
                  :loading="loading">保存
          </Button>
        </Cell>
      </Row>
      <Row class="margin-top" type="flex" :space-x="10">
        <Cell>
          <Select @change="loadCode" keyName="word" titleName="word" style="min-width: 70px" :deletable="false"
                  :datas="voucherWords" v-model="form.word" placeholder="记"/>
        </Cell>
        <Cell class="label">
          <NumberInput :min="1" v-model="form.code" v-width="90" style="display: inline-block"/>
          号
        </Cell>
        <Cell class="label">
          日期：
        </Cell>
        <Cell class="label">
          <DatePicker :disabled="!!carryForward" :option="dpOps" :clearable="false" v-model="form.voucherDate"
                      format="YYYY-MM-DD"/>
        </Cell>
        <Cell class="label">
          <DropdownCustom :toggle-icon="false" class-name="h-text-dropdown">
            <span class="text-hover blue-color font-bold">备注</span>
            <template #content>
              <div style="width: 200px; padding: 20px">
                <textarea placeholder="请输入备注内容" v-model="form.remark" v-autosize rows="3"
                          style="width: 100%;"></textarea>
              </div>
            </template>
          </DropdownCustom>
        </Cell>
      </Row>
    </div>
    <voucher-table ref="voucherTable" v-model="voucherTable"/>
    <div class="padding-right-left padding-bottom">
      制单人：{{ user.admin.name }}
    </div>
  </div>
</template>
<script>
import VoucherTable from "@/views/common/VoucherTable.vue";
import FinanceAccountLink from "@js/api/setting/FinanceAccountLink";
import {mapState} from 'vuex';
import manba from "manba";

export default {
  name: "VoucherForm",
  components: {VoucherTable},
  inject: ['reload'],
  props: {
    voucherId: [Number, String],
  },
  computed: {
    ...mapState(["user", "accountBook"]),
    voucherItems() {
      return this.voucherTable.voucherItems;
    },
    canSave() {
      return this.voucherItems.length > 0
    },
  },
  data() {
    return {
      loading: false,
      carryForward: false,
      voucherWords: [],
      voucherTable: {voucherItems: []},
      dpOps: {
        start: manba().format("YYYY-MM-dd")
      },
      voucher: {},
      form: {
        remark: '',
        word: '',
        code: '',
        voucherDate: '',
        carryForward: false,
      }
    }
  },
  watch: {
    'form.word'(val, old) {
      if (!old && this.voucherId) {
        return;
      }
      val && this.loadCode();
    },
    voucherId(val) {
      if (!val) {
        this.reload()
      } else {
        this.init();
      }
    }
  },
  methods: {
    loadVoucherWords() {
      // 加载凭证字
      FinanceAccountLink.loadVoucherWord().then(({data}) => {
        this.voucherWords = data || [];
        this.form.word = data.find(value => value.isDefault).word
      });
    },
    loadCode() {
      FinanceAccountLink.loadCode({
        word: this.form.word,
        currentAccountDate: this.form.voucherDate
      }).then(({data}) => {
        this.form.code = data
      });
    },
    save(next) {
      if (!this.form.code) {
        this.$Message("亲，请输入编号！");
        return
      }

      if (!this.voucherItems.length) {
        this.$Message("亲，第1行不能为空！");
        return
      }

      if (this.checkItem("摘要", 'summary') || this.checkItem("科目", 'subjectId') || this.checkItem("金额")) {
        return
      }

      if (this.voucherTable.jfTotal != this.voucherTable.dfTotal) {
        this.$Message("亲，借贷不平衡！");
        return
      }

      this.loading = true;
      this.$api.voucher[this.voucherId ? 'update' : 'save'](Object.assign({}, this.form, {
        details: this.voucherItems,
        createMember: this.user.id
      })).then(({success, data}) => {
        if (success && !this.form.id) {
          if (next) {
            if (!this.isCheck) {
              this.reload();
            } else {
              this.$router.back();
            }
          } else {
            this.$router.push({name: 'VoucherForm', params: {voucherId: data.id}});
          }
        }

        this.loading = false;
        this.$Message("亲，保存成功！");

        //如果是结转凭证就返回
        if (this.$route.params.checkData) {
          this.$router.back();
        }
      }).catch(() => {
        this.loading = false;
      });
    },
    checkItem(name, field) {
      let i = 0, len = this.voucherItems.length, row = -1;
      for (; i < len; i++) {
        if ((field && !this.voucherItems[i][field]) || (!field && !this.voucherItems[i].debitAmount && !this.voucherItems[i].creditAmount)) {
          row = i + 1;
          break;
        }
      }

      if (row > -1) {
        this.$Message(`亲，第${row}行，请输入${name}！`);
        return true;
      }
    },
    init() {
      if (this.voucherId) {
        this.$api.voucher.load(this.voucherId).then(({data}) => {
          this.$refs.voucherTable.initValue(data.details);
          this.voucher = data;
          this.form = {
            id: data.id,
            auditMemberId: data.auditMemberId,
            word: data.word,
            remark: data.remark,
            voucherDate: data.voucherDate,
            carryForward: data.carryForward,
            code: data.code
          };
        })
      } else {
        this.form.voucherDate = manba(this.accountBook.currentAccountDate).endOf('month').format('YYYY-MM-DD');
      }


      this.loadVoucherWords();
    },
  },
  mounted() {
    console.info("this.accountBook:",this.accountBook);
    this.dpOps["start"] = manba(this.accountBook.enableDate).format("YYYY-MM-DD");
    this.init();
  }
}
</script>

<style scoped>
.mask {
  position: absolute;
  height: 100%;
  width: 100%;
  z-index: 100;
  bottom: 0;
  top: 110px;
}

.auditpic {
  position: absolute;
  z-index: 100;
  right: 100px;
}
</style>

