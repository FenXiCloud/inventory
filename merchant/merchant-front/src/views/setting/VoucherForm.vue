<template>
  <div class="h-panel" style="width: 1000px;margin: 0 auto;padding-top: 20px;">
    <div class="padding-right-left">
      <Row type="flex" :space-x="20" v-if="type !== 'look'">
        <Cell>
          <Button color="primary" @click="save(false)" :loading="loading">保存
          </Button>
        </Cell>
      </Row>
      <Row class="mt-20px mb-20px margin" type="flex" :space-x="10">
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
                <textarea placeholder="请输入备注内容" v-model="form.remark" rows="3"
                          style="width: 100%;"></textarea>
              </div>
            </template>
          </DropdownCustom>
        </Cell>
      </Row>
    </div>
    <voucher-table ref="voucherTable" v-model="voucherTable"/>
    <div class="padding-right-left padding-bottom mt-20px">
      制单人：{{ user.admin.name }}
    </div>
  </div>
</template>
<script>
import VoucherTable from "@/views/common/VoucherTable.vue";
import FinanceAccountLink from "@js/api/setting/FinanceAccountLink";
import {mapState} from 'vuex';
import manba from "manba";
import {MessagePlugin} from "tdesign-vue-next";
import FinanceVoucher from "@js/api/setting/FinanceVoucher";

export default {
  name: "VoucherForm",
  components: {VoucherTable},
  inject: ['reload'],
  props: {
    voucherId: [Number, String],
    type: String
  },
  computed: {
    ...mapState(["user", "accountBook"]),
    voucherItems() {
      return this.$refs.voucherTable.voucherItems;
    },
    canSave() {
      return this.$refs.voucherTable.voucherItems.length > 0
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
    loadWords() {
      // 加载凭证字
      FinanceAccountLink.voucherWord().then(({data}) => {
        this.voucherWords = data || [];
        this.form.word = data.find(value => value.isDefault).word
      });
    },
    loadCode() {
      FinanceAccountLink.code({
        word: this.form.word,
        currentAccountDate: this.form.voucherDate
      }).then(({data}) => {
        this.form.code = data
      });
    },
    save(next) {
      if (!this.form.code) {
        MessagePlugin.warning("亲，请输入编号！");
        return
      }

      if (!this.voucherItems.length) {
        MessagePlugin.warning("亲，第1行不能为空！");
        return
      }

      if (this.checkItem("摘要", 'summary') || this.checkItem("科目", 'subjectId') || this.checkItem("金额")) {
        return
      }

      if (this.voucherTable.jfTotal != this.voucherTable.dfTotal) {
        MessagePlugin.warning("亲，借贷不平衡！");
        return
      }

      this.loading = true;
      console.info("details",this.voucherItems)
      // FinanceVoucher.upVoucher(Object.assign({}, this.form, {
      //   details: this.voucherItems,
      //   createMember: this.user.id
      // })).then(({success, data}) => {
      //   this.loading = false;
      //   MessagePlugin.warning("亲，保存成功！");
      //   this.$emit('success', data);
      // }).catch(() => {
      //   this.loading = false;
      // });
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
        MessagePlugin.warning(`亲，第${row}行，请输入${name}！`);
        return true;
      }
    },
    init() {
      if (this.voucherId) {
        FinanceVoucher.remote({
          voucherId: this.voucherId,
        }).then(({data: response}) => {
          const {data} = response;
          this.$refs.voucherTable.initValue(data.details);
          console.info(this.$refs.voucherTable.value);
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
        });
      } else {
        this.form.voucherDate = manba(this.accountBook.currentAccountDate).endOf('month').format('YYYY-MM-DD');
      }


      this.loadWords();
    },
  },
  mounted() {
    console.info("this.accountBook:", this.accountBook);
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

.margin {
  display: flex;
  align-items: center;
}
</style>

