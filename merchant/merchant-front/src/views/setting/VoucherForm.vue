<template>
  <app-content style="position: relative;">
    <Loading text="操作中~" :loading="loading"></Loading>
    <div class="mask" v-if="isReadOnly"></div>
    <div class="h-panel voucher-form-panel">
      <img v-if="form.id && form.auditMemberId " class="auditpic" src="" alt="已审核">
      <div class="mx-10px mb-10px w-980px">
        <div class="border-bottom text-center font-bold text-20px pb-8px mb-16px">{{ wordTitle }}
          <template v-if="insert || reverse">({{ insert ? '插入' : '红冲' }})</template>
        </div>
        <div class="flex justify-between mb-20px items-center">
          <div>
            <vxe-button :disabled="!canSave && !reverse" status="primary" @click="save(false,'assets')"
                        :loading="loading" content="保存"/>
            <vxe-button @click="$emit('close')">返回</vxe-button>
          </div>
          <!--          <div class="flex" v-if="!isCheck && !carryover">-->
          <!--            <vxe-button :loading="loading" @click="before" icon="h-icon-left"></vxe-button>-->
          <!--            <vxe-button class="reverse" :loading="loading" @click="next" icon="h-icon-right"></vxe-button>-->
          <!--          </div>-->
        </div>
        <div class="flex mt-16px items-center justify-between">
          <div class="flex items-center">
            <Select keyName="word" titleName="word" style="min-width: 80px" :disabled="insert" :deletable="false"
                    :datas="voucherWords" v-model="form.word" placeholder="记"/>
            <span class="split-vertical"></span>
            <NumberInput :min="1" v-model.number="form.code" class="v-code" :disabled="insert"/>
            <span class="split-vertical"></span>
            号
            <span class="split-vertical mr-16px"></span>
            日期：
            <DatePicker class="v-date" :disabled="!!carryForward" :option="dpOps" :clearable="false"
                        v-model="form.voucherDate" format="YYYY-MM-DD"/>
            <span class="yellow-color ml-10px">按回车键或Tab键能快速选择单元格</span>
          </div>
          <div class="flex items-center">
            <DropdownCustom :toggle-icon="false" class-name="h-text-dropdown" placement="bottom-end">
              <span class="text-hover blue-color font-bold">
                备注
              </span>
              <template #content>
                <Textarea placeholder="请输入备注内容" v-model="form.remark" v-autosize rows="5"
                          style="width: 300px"></Textarea>
              </template>
            </DropdownCustom>
            <Attachment v-model="form.billList" v-model:receiptNum="form.receiptNum"/>
          </div>
        </div>
      </div>
      <VoucherTable2 :carryover="isCarryover" ref="voucherTable2" :is-edit="!!form.auditMemberId"
                     @update="onItemsUpdate" :voucherDetails="voucherDetails"/>
      <div class="flex w-full px-20px pb-20px justify-between items-center">
        <div>
          <!--        <span>-->
          <!--          制单人：{{ voucher.createMemberName || User.realName }}-->
          <!--        </span>-->
          <!--          <span class="ml-20px">-->
          <!--          审核人：{{ voucher.auditMemberName ? voucher.auditMemberName : "无" }}-->
          <!--        </span>-->
        </div>
      </div>
    </div>
  </app-content>
</template>

<script>
import {mapState} from 'vuex';
import {message} from "heyui.ext";
import manba from "manba";
import {add, floor} from "xe-utils";

export default {
  name: "VoucherForm",
  props: {
    voucherId: [Number, String],
    voucherDate: String,
    voucherCode: Number,
    voucherWord: String,
    checkData: Object,
    assetsData: Array,
    profitCheckData: Array,
    type: Object,
    copy: Boolean,
    reverse: Boolean,
    insert: Boolean,
    isView: Boolean,
    carryover: Boolean,
    localDate: ''
  },
  computed: {
    ...mapState(['tabs',]),
    canSave() {
      return this.voucherItems.length > 0 && !this.form.locked
    },
    isCheck() {
      return !!this.checkData
    },
    isCarryover() {
      return this.carryover || this.form.carryForward;
    },
    isReadOnly() {
      return this.isView || this.form.locked || (this.form.id && this.form.auditMemberId)
    }
  },
  data() {
    return {
      entityId: null,
      loading: false,
      isCopy: false,
      carryForward: false,
      cachedDetails: [],
      voucherItems: [],
      voucherDetails: [],
      voucherTable: {voucherItems: []},
      dpOps: {
        start: manba().format(),
        end: null
      },
      voucher: {},
      form: {
        id: null,
        remark: '',
        billList: [],
        word: '',
        code: '',
        voucherDate: '',
        receiptNum: 0,
        locked: false,
        carryForward: false,
        checkTplId: null,
      },
    }
  },
  watch: {
    'form.word'(val, old) {
      if (!old && this.entityId) {
        return;
      }
      val && this.loadCode();
    },
    voucherItems: {
      deep: true,
      handler(val) {
        if (!this.form.id) {
          localStorage.setItem(this.cacheKey, JSON.stringify(val));
        }
      }
    },
    'form.voucherDate'(val) {
      this.loadCode(val);
    },
  },
  methods: {
    onItemsUpdate(val) {
      this.voucherItems = val;
      this.voucherTable.jfTotal = floor(val.reduce((total, row) => add(total, (row.debitAmount || 0)), 0), 2)
      this.voucherTable.dfTotal = floor(val.reduce((total, row) => add(total, (row.creditAmount || 0)), 0), 2)
    },
    loadVoucherWords() {
      RelationVoucher.loadWord().then(({data}) => {
        this.voucherWords = data
      })
      // this.form.word = this.voucherWords.find(value => value.isDefault)?.word
    },
    loadCode(voucherDate = null) {
      if (this.insert) {
        return
      }
      if (this.voucher && this.voucher.id && voucherDate && manba(this.voucher.voucherDate).format("YYYYMM") === manba(voucherDate).format("YYYYMM")) {
        this.form.code = this.voucher.code;
      } else if (this.form.word) {
        this.form.voucherDate && RelationVoucher.loadCode({
          word: this.form.word,
          currentAccountDate: voucherDate || this.form.voucherDate
        }).then(({data}) => {
          this.form.code = data
        })
      }
    },
    formValid() {
      if (!this.form.voucherDate) {
        message("亲，请选择日期！");
        return false;
      }

      if (!this.form.code) {
        message("亲，请输入编号！");
        return false;
      }

      if (!this.voucherItems.length) {
        message("亲，第1行不能为空！");
        return false;
      }

      if (this.checkItem("摘要", 'summary') || this.checkItem("科目", 'subjectId') || this.checkItem("金额")) {
        return false;
      }

      if (this.voucherTable.jfTotal !== this.voucherTable.dfTotal) {
        message("亲，借贷不平衡！");
        return false;
      }
      return true;
    },
    save(next, type = "") {
      if (!this.formValid()) {
        return
      }
      let formData = Object.assign({}, this.form, {
        details: this.voucherItems,
        carryForward: this.carryForward
      });
      if (this.type) {
        formData.checkTplId = this.type.id;
      }
      this.loading = true;
      RelationVoucher.saveVoucher(this.voucherId, formData).then(({success, data}) => {
        if (success) {
          message("操作成功")
          this.$emit('success')
        }
      }).catch(() => {
        this.loading = false;
      }).finally(() => this.loading = false);
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
        message(`亲，第${row}行，请输入${name}！`);
        return true;
      }
    },
    async init() {
      if ((this.voucherId || this.entityId) && !this.insert) {
        this.loading = true;
        RelationVoucher.loadVoucher(this.entityId || this.voucherId).then(({data}) => {
          const oldVoucher = structuredClone(data);
          data.details.forEach(val => {
            if (this.reverse) {
              val.creditAmount = 0 - val.creditAmount;
              val.debitAmount = 0 - val.debitAmount;
              val.num = 0 - val.num;
              val.summary = `冲销${manba(oldVoucher.voucherDate).format("YYYYMM")}${oldVoucher.word}-${oldVoucher.code}_` + val.summary
            }
            if (!this.isCopy && !this.reverse) {
              val.originalCreditAmount = val.creditAmount;
              val.originalDebitAmount = val.debitAmount;
            }
          })
          this.voucherDetails = data.details;
          this.voucher = data;

          this.form = {
            id: data.id,
            auditMemberId: data.auditMemberId,
            createMember: data.createMember,
            createMemberName: data.createMemberName,
            word: data.word,
            reverseVoucherId: data.reverseVoucherId,
            locked: data.locked,
            remark: data.remark,
            receiptNum: data.receiptNum,
            voucherDate: data.voucherDate,
            carryForward: data.carryForward,
            billList: data.billList || [],
            code: data.code
          };
        }).finally(() => this.loading = false)
      } else {
        if (this.insert) {
          this.form.insert = true;
          this.form.code = this.voucherCode;
          this.form.word = this.voucherWord;
          this.form.voucherDate = this.voucherDate;
        } else {
          const resp = await voucher.lastDate();
          this.form.voucherDate = resp.data;
          this.$nextTick(() => {
            this.voucherDetails = this.cachedDetails || [];
          })
        }
      }

      //结转凭证初始化
      this.loadVoucherWords();
    },
  },
  created() {
    const cachedDetails = localStorage.getItem(this.cacheKey);
    if (cachedDetails && !this.voucherId) {
      this.cachedDetails = JSON.parse(cachedDetails);
    }

    this.isCopy = this.copy;
    if (!this.isCopy) {
      this.entityId = this.voucherId;
    }
    this.dpOps.start = this.firstCheckoutDate;
    this.init();
  }
}
</script>

<style scoped lang="less">
.mask {
  position: absolute;
  height: 100%;
  width: 100%;
  z-index: 100;
  bottom: 0;
  top: 60px;
}

.auditpic {
  position: absolute;
  z-index: 100;
  right: 100px;
}

.voucher-form-panel {
  width: 1020px;
  margin: 0 auto;
  padding-top: 20px;
  border-radius: 5px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.reverse {
  display: inline-flex;
  flex-direction: row-reverse;
  align-items: center;

  &:deep(i) {
    margin-left: 5px;
  }
}

.v-code {
  display: inline-block;
  width: 60px;

  :deep(.h-numberinput-input) {
    min-width: unset;
    text-align: center;
  }
}

.v-date {
  width: 130px;
}

.v-receiptNum {
  text-align: center;
  height: 25px;
}
</style>
