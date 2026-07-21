<template>
  <div class="voucher-table bg-white-color">
    <table class="header" cellspacing="0" cellpadding="0">
      <tbody>
      <tr class="bg-white-color">
        <td style="width: 208px; font-weight: bold;">摘要</td>
        <td style="width: 308px; font-weight: bold;">会计科目</td>
        <td>
          <div style="font-weight: bold;">
            借方金额
          </div>
          <div class="moneyUint">
            <span>亿</span> <span>千</span> <span>百</span> <span>十</span> <span>万</span> <span>千</span>
            <span>百</span> <span>十</span> <span>元</span> <span>角</span> <span class="last">分</span>
          </div>
        </td>
        <td class="tdLast">
          <div style="font-weight: bold;">
            贷方金额
          </div>
          <div class="moneyUint">
            <span>亿</span> <span>千</span> <span>百</span> <span>十</span> <span>万</span> <span>千</span>
            <span>百</span> <span>十</span> <span>元</span> <span>角</span> <span class="last">分</span>
          </div>
        </td>
      </tr>
      </tbody>
    </table>
    <table class="body" cellspacing="0" cellpadding="0">
      <tbody>
      <tr class="trDetails" v-for="(d,i) in details" :key="i" :data-idx="i"
          @mouseenter="onRowEnter(i, $event)" @mouseleave="onRowLeave">
        <td class="tdZhaoyao tdInput">
          <div class="display" v-if="!d.zyEdit" @click="doEdit(d,'zy',i)">{{ d.data.summary }}</div>
          <ManualPopup v-show="d.zyEdit" ref="zyDropdown" trigger="manual" style="width: 100%"
                          equal-width @hide="endEdit(d,'zy',i)">
            <textarea class="voucher-input edit" data-type="zy" :data-index="i" :id="'zy'+i" @input="zyInput"></textarea>
            <template #content>
              <div style="height: 180px;overflow-y: scroll;">
                <ul class="summary" :id="`summary${i}`">
                  <li class="summary-item" :data-index="idx" :class="{'summary-item-select':(idx===0&&zyFiler)}"
                      v-for="(s,idx) in summaryData" @click="chooseSummary(d,s.name,i)">
                    {{ s.name }}
                  </li>
                </ul>
              </div>
            </template>
          </ManualPopup>
        </td>
        <td class="tdKemu tdInput">
          <div class="display" v-if="!d.kmEdit" @click="doEdit(d,'km',i)">
            {{ d.data.subjectName || '' }}{{ d.data.auxiliaryTitle || '' }}
          </div>
          <div class="yue" v-if="balanceList[d.data.subjectId]">余额：
            <span
                :class="{'red-color':balanceList[d.data.subjectId]<0}">{{
                numFormat(balanceList[d.data.subjectId])
              }}</span>
          </div>
          <div v-if="d.data.unit || d.data.num" @click.stop="" class="num voucher-flex-row">
            <span>数量:</span>
            <span>
              <span @click.stop="showInput" class="text-hover">{{ d.data.num || 0 }}</span>
              <input class="numInput" @blur.stop="hideInput($event,d.data)" @click="" style="display: none;"
                     v-model="d.data.num" @click.stop="" v-width="40">
            </span>
            <span>单价:</span>
            <span>
              <span @click.stop="showInput" class="text-hover">{{ d.data.price || 0 }}</span>
              <input class="numInput" @blur.stop="hideInput($event,d.data)" style="display: none;"
                     v-model="d.data.price" @click.stop="" v-width="40">
            </span>
          </div>
          <ManualPopup v-show="d.kmEdit" ref="kmDropdown" trigger="manual" style="width: 100%"
                          equal-width @hide="endEdit(d,'km',i)">
            <textarea class="voucher-input edit" data-type="km" :data-index="i" :id="'km'+i" @input="kmInput"></textarea>
            <template #content>
              <div style="height: 180px;overflow-y: scroll;" v-if="!d.auxiliary">
                <ul class="subjects" :id="`subjects${i}`">
                  <li class="subjects-item" :data-index="idx" :class="{'subjects-item-select':(idx===0&&kmFiler)}"
                      v-for="(s,idx) in subjectData" @click="chooseSubject(d,s,i)">
                    {{ s.code }} {{ s.name }}
                  </li>
                </ul>
              </div>
              <div v-else class="auxiliary padding">
                <div v-if="auxiliaryAccounting[i]" v-for="item in auxiliaryAccounting[i]" class="voucher-flex-row"
                     :key="item.id">
                  <div style="width: 180px">
                    <t-select v-model="d.data[item.id]" v-if="auxiliaryAccountingData[d.data.subjectId]"
                              class="auxiliary" :keys="{ value: 'id', label: 'name' }"
                              :options="auxiliaryAccountingData[d.data.subjectId]" filterable clearable/>
                  </div>
                </div>
                <div class="text-center margin-top">
                  <t-button theme="primary" @click="fillAuxiliary(d,auxiliaryAccountingData[d.data.subjectId],i)">确认
                  </t-button>
                </div>
              </div>
            </template>
          </ManualPopup>
        </td>
        <td class="tdJieFang tdInput">
          <div v-if="!d.jfEdit" class="display displayMoney" :class="{'red-color':d.data.debitAmount<0}"
               @click="doEdit(d,'jf',i)">
            <span @click.stop="doEdit(d,'jf',i)">{{ formatMoney(d.data.debitAmount) }}</span>
          </div>
          <ManualPopup v-show="d.jfEdit" ref="jfDropdown" placement="top" trigger="manual"
                          style="width: 100%" equal-width @hide="endEdit(d,'jf',i)">
            <input max="999999999" min="-999999999" type="number" class="voucher-input jf edit" data-type="jf" :data-index="i"
                   :id="'jf'+i" v-model="d.data.debitAmount">
            <template #content>
              <div class="hoverNum">{{ numFormat(d.data.debitAmount) }}</div>
            </template>
          </ManualPopup>
        </td>
        <td class="tdLast tdDaiFang tdInput">
          <div v-if="!d.dfEdit" class="display displayMoney" :class="{'red-color':d.data.creditAmount<0}"
               @click="doEdit(d,'df',i)">
            <span @click.stop="doEdit(d,'df',i)">{{ formatMoney(d.data.creditAmount) }}</span>
          </div>
          <ManualPopup v-show="d.dfEdit" ref="dfDropdown" placement="top" trigger="manual"
                          style="width: 100%" equal-width @hide="endEdit(d,'df',i)">
            <input max="999999999" min="-999999999" :data-index="i" data-type="df" class="voucher-input df edit" :id="'df'+i"
                   v-model="d.data.creditAmount" step="">
            <template #content>
              <div class="hoverNum">{{ numFormat(d.data.creditAmount) }}</div>
            </template>
          </ManualPopup>
        </td>
      </tr>
      <tr class="trDetails total">
        <td colspan="2" class="spTotal padding-left">
          合计: {{ dxMoney(jfTotal) }}
        </td>
        <td style="width: 220px;">
          <div class="display displayMoney">
            <span class="spTotalDebit">{{ formatMoney(jfTotal) }}</span>
          </div>
        </td>
        <td class="tdLast" style="width: 220px;">
          <div class="display displayMoney">
            <span class="spTotalCredit">{{ formatMoney(dfTotal) }}</span>
          </div>
        </td>
      </tr>
      </tbody>
    </table>
    <i class="action fa fa-plus-circle" v-show="actionPos.visible"
       :style="{ left: actionPos.leftPlus + 'px', top: actionPos.top + 'px', display: 'inline-block' }"
       @mouseenter="actionPinned = true" @mouseleave="actionPinned = false; hideActions()"
       @click="addItem()"></i>
    <i class="action fa fa-times-circle" v-show="actionPos.visible"
       :style="{ left: actionPos.leftMinus + 'px', top: actionPos.top + 'px', display: 'inline-block' }"
       @mouseenter="actionPinned = true" @mouseleave="actionPinned = false; hideActions()"
       @click="removeItem()"></i>
  </div>
</template>

<script>
import Decimal from 'decimal.js';
import Pinyin from 'chinese-to-pinyin';
import FinanceAccountLink from "@js/api/setting/FinanceAccountLink";
import FinanceVoucher from "@js/api/setting/FinanceVoucher";
import ManualPopup from '@/components/ManualPopup.vue';
function getDropdownRef(vm, type, idx) {
  const refs = vm.$refs[`${type}Dropdown`];
  if (!refs) return null;
  return Array.isArray(refs) ? refs[idx] : refs;
}

function qs(sel, root = document) {
  return root.querySelector(sel);
}

function qsa(sel, root = document) {
  return Array.from(root.querySelectorAll(sel));
}

const detail = {
  zyEdit: false,
  kmEdit: false,
  jfEdit: false,
  dfEdit: false,
  auxiliary: false,
  data: {summary: '', subjectName: ''}
};

const nextTag = {
  zy: 'km',
  km: 'jf',
  jf: 'df'
};

const FormatNum = function (num, digit = 2) {
  return Decimal(num).toFixed(digit);
};

const ToPy = (name) => {
  let pyArr = [];
  name.split("").forEach(w => {
    w = w.trim();
    if (w) {
      if (/[\u4e00-\u9fa5]/.test(w)) {
        let py = Pinyin(w, {removeTone: true, keepRest: true}).trim();
        pyArr.push(py.substring(0, 1));
      } else {
        pyArr.push(w);
      }
    }
  });

  return pyArr.join("");
};

export default {
  name: "VoucherTable",
  components: { ManualPopup },
  props: {
    value: Object,
    modelValue: Object,
  },
  emits: ['input', 'update:modelValue'],
  data() {
    return {
      details: [{
        zyEdit: false,
        kmEdit: false,
        jfEdit: false,
        dfEdit: false,
        auxiliary: false,
        data: {summary: ''}
      }, {
        zyEdit: false,
        kmEdit: false,
        jfEdit: false,
        dfEdit: false,
        auxiliary: false,
        data: {summary: ''}
      }, {
        zyEdit: false,
        kmEdit: false,
        jfEdit: false,
        dfEdit: false,
        auxiliary: false,
        data: {summary: ''}
      }, {zyEdit: false, kmEdit: false, jfEdit: false, dfEdit: false, auxiliary: false, data: {summary: ''}}],
      hoverIdx: -1,
      actionPos: { visible: false, top: 0, leftPlus: 0, leftMinus: 0 },
      voucherSelect: [],
      summarySelect: [],
      auxiliaryAccounting: [],
      zyFiler: "",
      kmFiler: "",
      zaiYao: [],
      balanceList: {},
      initBalanceList: {},
      auxiliaryAccountingData: {},
      currentEdit: null,
      jfTotal: 0,
      dfTotal: 0,
      actionPinned: false
    }
  },
  computed: {
    subjectData() {
      if (this.kmFiler) {
        return this.voucherSelect.filter(value => {
          return value.code.indexOf(this.kmFiler) !== -1 || value.name.indexOf(this.kmFiler) !== -1 || value.mnemonicCode.indexOf(this.kmFiler) !== -1;
        });
      }
      return this.voucherSelect;
    },
    summaryData() {
      if (this.zyFiler) {
        return this.summarySelect.filter(value => {
          return value.name.indexOf(this.zyFiler) !== -1 || value.mnemonicCode.indexOf(this.zyFiler) !== -1;
        });
      }
      return this.summarySelect;
    },
    voucherItems() {
      return this.details.map(value => value.data).filter(value => {
        return value.subjectId || value.summary || value.debitAmount || value.creditAmount;
      }).map(value => {
        let keys = Object.keys(value).filter(key => !isNaN(Number(key))), auxiliary = [];
        if (keys.length) {
          keys.forEach(key => {
            auxiliary.push(value[key]);
          })
        }

        return {
          auxiliary,
          debitAmount: value.debitAmount,
          creditAmount: value.creditAmount,
          subjectId: value.subjectId,
          direction: value.direction,
          subjectName: value.subjectName || '',
          subjectCode: value.subjectCode || '',
          auxiliaryTitle: value.auxiliaryTitle || '',
          summary: value.summary ? value.summary.trim().replace(/\s+/g, '') : '',
          num: value.num,
          price: value.price
        }
      });
    }
  },
  methods: {
    kmInput(e) {
      this.kmFiler = e.target.value;
      this.details[this.currentEdit.idx]['auxiliary'] = false;
    },
    zyInput(e) {
      this.zyFiler = e.target.value;
      this.details[this.currentEdit.idx].data['summary'] = e.target.value;
    },
    removeItem() {
      if (this.details.length > 4) {
        this.details.splice(this.hoverIdx, 1);
      } else {
        this.details[this.hoverIdx]['data'] = {};
      }
    },
    addItem(idx) {
      this.details.splice(idx ? idx : Math.max((this.hoverIdx), 0), 0, {
        zyEdit: false,
        kmEdit: false,
        jfEdit: false,
        dfEdit: false,
        auxiliary: false,
        data: {summary: '', subjectName: ''}
      });
      this.$nextTick(() => {
        this.bindEnterTabKeydownEvt();
      })
    },
    doEdit(row, type, idx) {
      if (this.currentEdit) {
        this.endEdit(this.currentEdit.row, this.currentEdit.type, this.currentEdit.idx)
      }
      this.$nextTick(() => {
        row[type + 'Edit'] = true;
        this.currentEdit = {row, type, idx};

        this.$nextTick(() => {
          const el = document.getElementById(`${type}${idx}`);
          if (el) {
            if (type === 'km') {
              el.value = row.data.subjectName || '';
              if (row.data.subject && row.data.subject.auxiliaryAccounting) {
                this.details[idx].auxiliary = true;
              }
            }
            el.focus();
            if (typeof el.select === 'function') el.select();
          }
          const dropdown = getDropdownRef(this, type, idx);
          dropdown && dropdown.show();
        });
      });
    },
    endEdit(row, type, idx, doNext = false) {
      if (row[type + 'Edit']) {
        row[type + 'Edit'] = false;
        this.kmFiler = "";
        let debitAmount = Number(row.data.debitAmount);
        let creditAmount = Number(row.data.creditAmount);
        switch (type) {
          case 'jf':
            if (!isNaN(debitAmount) && !isNaN(creditAmount) && debitAmount) {
              row.data['creditAmount'] = '';
            }
            this.calculateBalance(row.data.subjectId);
            break;
          case 'df':
            if (!isNaN(debitAmount) && !isNaN(creditAmount) && creditAmount) {
              row.data['debitAmount'] = '';
            }
            break;
        }

        this.calculateBalance(row.data.subjectId);

        if (row.data.unit && row.data.num > 0) {
          let money = debitAmount || creditAmount;
          this.details[idx].data['price'] = Number((money / row.data.num).toFixed(2));
        }

        const dropdown = getDropdownRef(this, type, idx);
        dropdown && dropdown.hide();
        this.details[idx].auxiliary = false;

        if (doNext) {
          if (type === 'jf' && row.data.debitAmount) {
            this.lastNext(idx);
          } else if (type !== 'df') {
            this.doEdit(row, nextTag[type], idx);
          } else {
            this.lastNext(idx);
          }
        }
      }
    },
    onRowEnter(i, e) {
      this.hoverIdx = i;
      const tr = e.currentTarget;
      const parent = this.$el;
      const trRect = tr.getBoundingClientRect();
      const parentRect = parent.getBoundingClientRect();
      this.actionPos = {
        visible: true,
        top: trRect.top - parentRect.top + 25,
        leftPlus: trRect.left - parentRect.left - 14,
        leftMinus: trRect.left - parentRect.left + 960
      };
    },
    onRowLeave() {
      if (!this.actionPinned) this.hideActions();
    },
    hideActions() {
      if (!this.actionPinned) {
        this.actionPos = { ...this.actionPos, visible: false };
      }
    },
    lastNext(idx) {
      let row = this.details[idx + 1];
      if (!row) {
        this.addItem(idx + 1);
      }
      this.doEdit(this.details[idx + 1], 'zy', idx + 1);
    },
    calculateBalance(subjectId) {
      if (subjectId) {
        let balance = this.initBalanceList[subjectId] || 0;
        let items = this.voucherItems.filter(val => val.subjectId === subjectId);
        items.forEach(val => {
          if (val.direction === "借") {
            if (val.debitAmount) {
              balance += Number(val.debitAmount);
            } else if (val.creditAmount) {
              balance -= Number(val.creditAmount);
            }
          } else {
            if (val.debitAmount) {
              balance -= Number(val.debitAmount);
            } else if (val.creditAmount) {
              balance += Number(val.creditAmount);
            }
          }
        });
        this.balanceList[subjectId] = FormatNum(balance);
      }
    },
    loadSelect() {
      let tmp = localStorage.getItem('voucherSelect');
      let tmp2 = localStorage.getItem('summarySelect');
      if (tmp) {
        this.voucherSelect = JSON.parse(tmp);
      }
      if (tmp2) {
        this.summarySelect = JSON.parse(tmp2);
      }
      FinanceAccountLink.voucherSelect().then(({data}) => {
        this.voucherSelect = data.data || [];
        localStorage.setItem("voucherSelect", JSON.stringify(data));
      });

      // FinanceAccountLink.voucherSummary().then(({data}) => {
      //   let newData = data.map(val => {
      //     return {
      //       name: val,
      //       mnemonicCode: ToPy(val)
      //     }
      //   });
      //   this.summarySelect = newData;
      //   localStorage.setItem("summarySelect", JSON.stringify(newData));
      // });
    },
    chooseSummary(d, summary, idx) {
      d.data.summary = summary;
      const el = document.getElementById(`zy${idx}`);
      if (el) el.value = this.currentEdit.row.data.summary;
      const dropdown = getDropdownRef(this, 'zy', idx);
      dropdown && dropdown.hide();
    },
    chooseSubject(d, subject, idx) {
      if (d.data.subjectId !== subject.id) {
        d.data = {
          subjectName: `${subject.code}-${subject.name}`,
          subjectId: subject.id,
          subjectCode: subject.code,
          unit: subject.unit,
          direction: subject.balanceDirection,
          summary: d.data.summary,
          creditAmount: d.data.creditAmount,
          debitAmount: d.data.debitAmount,
          subject: subject
        };
        this.loadSubjectBalance(d.data, subject.id);
      }
      //判断是否有辅助项目
      if (subject.auxiliaryAccounting) {
        this.auxiliaryAccounting[idx] = JSON.parse(subject.auxiliaryAccounting);
        this.loadAuxiliaryAccountingData(idx, subject);
        //开启辅助项输入
        this.$nextTick(() => {
          this.details[idx]['auxiliary'] = true;
          this.currentEdit['auxiliary'] = true;
        });
        const el = document.getElementById(`km${idx}`);
        if (el) el.value = this.currentEdit.row.data.subjectName || '';
      } else {
        this.endEdit(d, 'km', idx, true);
      }
    },
    loadSubjectBalance(d, subjectId) {
      if (!this.balanceList[subjectId]) {
        FinanceVoucher.balance({subjectId: subjectId}).then(({data}) => {
          this.balanceList[subjectId] = data;
          this.initBalanceList[subjectId] = data;
          d.balance = data;
        });
      } else {
        d.balance = this.balanceList[subjectId];
      }
    },
    clear() {
      this.details = initDetails();
    },
    loadAuxiliaryAccountingData(idx, subject) {
      if (!this.auxiliaryAccountingData[subject.id]) {
        FinanceVoucher.auxiliary({ids: this.auxiliaryAccounting[idx]}).then(({data}) => {
          this.auxiliaryAccountingData[subject.id] = data;
        });
      }
    },
    fillAuxiliary(row, auxiliaryData, idx) {
      let title = "", rowData = row.data;
      const categories = this.auxiliaryAccounting[idx] || [];
      const optionList = Array.isArray(auxiliaryData)
          ? auxiliaryData
          : Object.values(auxiliaryData || {}).flatMap(v => Array.isArray(v) ? v : (v ? [v] : []));
      const resolveSelected = (raw) => {
        if (!raw) return null;
        if (typeof raw === 'object') return raw;
        return optionList.find(x => x && x.id === raw) || null;
      };
      if (categories.length) {
        categories.forEach(cat => {
          const selected = resolveSelected(rowData[cat.id]);
          if (selected) {
            title += '_' + selected.code + "_" + selected.name;
          }
        });
      } else {
        Object.keys(auxiliaryData || {}).forEach(val => {
          const selected = resolveSelected(rowData[val]);
          if (selected) {
            title += '_' + selected.code + "_" + selected.name;
          }
        });
      }
      this.endEdit(row, 'km', idx, true);
      this.$nextTick(() => {
        rowData['auxiliaryTitle'] = title;
      });
    },
    showInput(e) {
      const span = e.target;
      const input = span.nextElementSibling;
      span.style.display = 'none';
      if (input) {
        input.style.display = '';
        input.focus();
        if (typeof input.select === 'function') input.select();
      }
    },
    hideInput(e, row) {
      const input = e.target;
      const span = input.previousElementSibling;
      input.style.display = 'none';
      if (span) span.style.display = '';
      const numRoot = input.closest('.num');
      const inputs = numRoot ? qsa('.numInput', numRoot) : [];
      if (inputs.length < 2) return;
      switch (row.direction) {
        case '借':
          row['debitAmount'] = FormatNum(Number(inputs[0].value) * Number(inputs[1].value));
          row['creditAmount'] = "";
          break;
        case '贷':
          row['creditAmount'] = FormatNum(Number(inputs[0].value) * Number(inputs[1].value));
          row['debitAmount'] = "";
          break;
      }
    },
    initValue(voucherItems) {
      this.details = [{
        zyEdit: false,
        kmEdit: false,
        jfEdit: false,
        dfEdit: false,
        auxiliary: false,
        data: {summary: ''}
      }, {
        zyEdit: false,
        kmEdit: false,
        jfEdit: false,
        dfEdit: false,
        auxiliary: false,
        data: {summary: ''}
      }, {
        zyEdit: false,
        kmEdit: false,
        jfEdit: false,
        dfEdit: false,
        auxiliary: false,
        data: {summary: ''}
      }, {zyEdit: false, kmEdit: false, jfEdit: false, dfEdit: false, auxiliary: false, data: {summary: ''}}];
      voucherItems.forEach((item, idx) => {
        item.subjectName = item.subjectName;
        item.auxiliaryTitle = item.auxiliaryTitle;

        if (item.subject) {
          item.direction = item.subject.balanceDirection;
          item.unit = item.subject.unit;
          if (item.subject.auxiliaryAccounting) {
            this.auxiliaryAccounting[idx] = JSON.parse(item.subject.auxiliaryAccounting);
            this.loadAuxiliaryAccountingData(idx, item.subject);
          }
        }

        if (item.auxiliary) {
          item.auxiliary.forEach(a => {
            item[a.accountingCategoryId] = a.accountingCategoryDetails;
          });
          delete item.auxiliary;
        }
        this.details[idx] = Object.assign({}, detail, {data: item});
      });
    },
    editMeta(el) {
      const edit = el && el.closest ? el.closest('.edit') : null;
      if (!edit) return {};
      return {
        type: edit.dataset.type,
        index: Number(edit.dataset.index)
      };
    },
    bindEnterTabKeydownEvt() {
      const table = this.$el && this.$el.querySelector('table.body');
      if (!table || table._voucherBound) return;
      table._voucherBound = true;
      const that = this;

      table.addEventListener('blur', (e) => {
        if (!e.target.classList.contains('jf') && !e.target.classList.contains('df')) return;
        const {type, index} = that.editMeta(e.target);
        if (type == null || isNaN(index)) return;
        that.endEdit(that.details[index], type, index, false);
      }, true);

      table.addEventListener('keyup', (e) => {
        if (!e.target.classList.contains('edit') && !e.target.closest('.edit')) return;
        const {type, index} = that.editMeta(e.target);
        if ((e.keyCode === 187 || e.code === 'Equal' || e.code === 'NumpadEqual' || e.key === '=') && (type === 'jf' || type === 'df')) {
          e.preventDefault();
          let details = that.details;
          let totalCredit = 0, totalDebit = 0;
          details.forEach((value, i) => {
            if (index !== i) {
              let creditAmount = Number(value.data.creditAmount), debitAmount = Number(value.data.debitAmount);
              if (!isNaN(creditAmount)) totalCredit += creditAmount;
              if (!isNaN(debitAmount)) totalDebit += debitAmount;
            }
          });
          if (type === 'jf' && totalCredit !== 0) {
            details[index].data.debitAmount = FormatNum(totalCredit - totalDebit);
            details[index].data.creditAmount = 0;
            e.target.value = Number(details[index].data.debitAmount).toFixed(2);
          } else if (type === 'df' && totalDebit !== 0) {
            details[index].data.creditAmount = FormatNum(totalDebit - totalCredit);
            details[index].data.debitAmount = 0;
            e.target.value = Number(details[index].data.creditAmount).toFixed(2);
          }
        }
      });

      table.addEventListener('keydown', (e) => {
        if (!e.target.classList.contains('edit') && !e.target.closest('.edit')) return;
        const {type, index} = that.editMeta(e.target);
        if (type == null || isNaN(index)) return;

        if (e.keyCode === 13 || e.keyCode === 9) {
          e.preventDefault();
          if (type === 'km') {
            const sel = qs(`#subjects${index} li.subjects-item-select`);
            if (sel) sel.click();
            return;
          }
          if (type === 'zy') {
            const select = qs(`#summary${index} .summary-item-select`);
            if (select) {
              that.details[index].data.summary = select.textContent.trim();
            }
          }
          that.endEdit(that.details[index], type, index, true);
          return;
        }

        if (e.keyCode === 38 || e.keyCode === 40) {
          e.preventDefault();
          if (type === 'km' || type === 'zy') {
            const idPre = type === 'km' ? 'subjects' : 'summary';
            const list = qs(`#${idPre}${index}`);
            if (!list) return;
            let select = qs(`.${idPre}-item-select`, list);
            if (!select) {
              const first = list.querySelector('li');
              if (first) first.classList.add(`${idPre}-item-select`);
              return;
            }
            let next = e.keyCode === 40 ? select.nextElementSibling : select.previousElementSibling;
            if (next && next.tagName === 'LI') {
              next.classList.add(`${idPre}-item-select`);
              select.classList.remove(`${idPre}-item-select`);
              const i = Number(next.dataset.index);
              if (i > 7) {
                const scroller = list.parentElement;
                if (scroller) scroller.scrollTop = scroller.scrollTop + 22;
              }
            }
          }
        }
      });
    },
    calculationOfTotal() {
      let jfTotal = 0, dfTotal = 0;
      this.details.forEach(value => {
        let debit, credit;
        if ((debit = Number(value.data.debitAmount))) {
          jfTotal += debit;
        }
        if ((credit = Number(value.data.creditAmount))) {
          dfTotal += credit;
        }
      });

      this.jfTotal = FormatNum(jfTotal);
      this.dfTotal = FormatNum(dfTotal);
    },
    formatMoney: function (value) {
      if (Number(value)) {
        let sp = (Math.abs(FormatNum(value)) + "").split(".");
        if (!sp[1]) {
          return sp[0] + "00"
        } else if (sp[1].length < 2) {
          return sp[0] + sp[1] + "0"
        }
        return sp[0] + sp[1];
      }
      return '';
    },
    fixNum(value) {
      return Number(value) ? FormatNum(value) : '';
    },
    toFixed(num, s) {
      return Decimal(num).toFixed(s);
    },
    numFormat(number) {
      if (!number) {
        return ''
      }
      /*
    　　 * 参数说明：
    　　 * number：要格式化的数字
    　　 * decimals：保留几位小数
    　　 * dec_point：小数点符号
    　　 * thousands_sep：千分位符号
    　　 * */
      number = (number + '').replace(/[^0-9+-Ee.]/g, '');
      let n = !isFinite(+number) ? 0 : +number, prec = 2, sep = ',', dec = '.', s = '';

      s = (prec ? this.toFixed(n, prec) : '' + Math.round(n)).split('.');

      let re = /(-?\d+)(\d{3})/;
      while (re.test(s[0])) {
        s[0] = s[0].replace(re, "$1" + sep + "$2");
      }
      if ((s[1] || '').length < prec) {
        s[1] = s[1] || '';
        s[1] += new Array(prec - s[1].length + 1).join('0');
      }
      return s.join(dec);
    },
    dxMoney(n) {
      if (!/^(0|[1-9]\d*)(\.\d+)?$/.test(n) || n == 0) {
        return "";
      }

      let unit = "仟佰拾亿仟佰拾万仟佰拾元角分", str = "";
      n += "00";
      let p = n.indexOf('.');
      if (p >= 0) {
        n = n.substring(0, p) + n.substr(p + 1, 2);
      }
      unit = unit.substr(unit.length - n.length);
      for (let i = 0; i < n.length; i++) {
        str += '零壹贰叁肆伍陆柒捌玖'.charAt(n.charAt(i)) + unit.charAt(i);
      }

      return str.replace(/零(仟|佰|拾|角)/g, "零").replace(/(零)+/g, "零").replace(/零(万|亿|元)/g, "$1").replace(/(亿)万|壹(拾)/g, "$1$2").replace(/^元零?|零分/g, "").replace(/元$/g, "元整");
    }
  },
  watch: {
    details: {
      deep: true,
      handler() {
        this.calculationOfTotal();
        const payload = {
          voucherItems: this.voucherItems,
          jfTotal: this.jfTotal || null,
          dfTotal: this.dfTotal || null
        };
        this.$emit('update:modelValue', payload);
        this.$emit('input', payload);
      }
    }
  },
  mounted() {
    this._onAppClick = (e) => {
      const t = e.target;
      if (!t) return;
      if (t.classList.contains('display')
          || t.classList.contains('subjects-item')
          || t.closest('.auxiliary')
          || t.closest('.tdInput')
          || t.closest('.t-popup')
          || t.closest('.manual-popup-content')) {
        return;
      }
      if (this.currentEdit) {
        this.endEdit(this.currentEdit.row, this.currentEdit.type, this.currentEdit.idx);
      }
    };
    this.$nextTick(() => {
      const app = document.getElementById('app');
      if (app) app.addEventListener('click', this._onAppClick);
      this.bindEnterTabKeydownEvt();
    });
    this.loadSelect();
  },
  beforeUnmount() {
    const app = document.getElementById('app');
    if (app && this._onAppClick) app.removeEventListener('click', this._onAppClick);
  }
}
</script>

<style lang="less">
.voucher-table {
  position: relative;
  font-size: 12px;
  width: 1000px;
  //padding: 10px 20px;

  .tdLast {
    border-right: 1px solid #dadada;
  }

  .header {
    width: 960px;
    text-align: center;
    line-height: 30px;
  }

  table tbody tr td {
    border-left: 1px solid #dadada;
    border-top: 1px solid #dadada;
    border-bottom: 1px solid #dadada;
    padding: 0;
    border-spacing: 0;
  }

  .moneyUint {
    border-top: 1px solid #dadada;
    height: 30px;
    background-image: url(../../assets/moneyUint.png);
    width: 221px;

    span {
      float: left;
      display: inline;
      width: 19px;
      height: 100%;
      margin-right: 1px;
      background-color: #fff;
      text-align: center;
      font-size: 12px;
    }

    .last {
      margin-right: 0;
    }
  }

  .body {
    width: 960px;
    text-align: center;

    .numInput {
      outline: none;
    }

    td {
      vertical-align: top;
    }

    .trDetails {
      width: 959px;

      .spTotal {
        width: 250px;
        text-align: left;
        vertical-align: middle;
        font-weight: bold;
      }

      .error-icon {
        right: 55px;
        top: 20px;
      }

      .display {
        height: 60px;
        font-weight: bold;
        font-size: 13px;
        text-align: left;
        padding: 2px;
        line-height: 17px;
      }

      .tdZhaoyao {
        width: 207px;
      }

      .tdKemu {
        width: 307px;
        position: relative;

        .yue {
          position: absolute;
          bottom: 2px;
          left: 2px;
          z-index: 2;
          font-weight: bold;
        }

        .num {
          position: absolute;
          bottom: 17px;
          right: 5px;
          line-height: 23px;
          font-weight: normal;
          z-index: 2;
        }
      }

      .tdJieFang, .tdDaiFang {
        width: 221px;
      }

      .displayMoney {
        font-weight: bold;
        font-size: 14px;
        letter-spacing: 11px;
        overflow: hidden;
        text-align: right;
        font-family: 'tahoma', serif;
        background-image: url(../../assets/moneyUint.png);
        line-height: 60px;
        padding: 0;

        span {
          position: relative;
          right: -5px;
        }
      }

      .voucher-autocomplete-show {
        border: none;
        border-radius: 0;
      }

      .voucher-input {
        border: none;
        border-radius: 0;
        height: 60px !important;
        resize: none !important;
        width: 100% !important;
      }

      textarea.voucher-input {
        height: 60px !important;
      }

      .voucher-popup-content {
        width: 100% !important;
      }
    }

    tbody:first-child td {
      border-top: 0 !important;
    }
  }

  .action {
    position: absolute;
    font-size: 16px;
    cursor: pointer;
    display: none;

    &:hover {
      color: #0052d9;
    }
  }

  .voucher-flex-row {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
  }

  .voucher-number-wrap {
    border-radius: 0;

    &.focusing {
      border: 1px solid #44b449;
      box-shadow: none;
    }

    .voucher-input {
      font-size: 16px;
      font-weight: bold;
    }
  }
}

.subjects, .summary {
  li {
    padding: 2px 10px;
    cursor: pointer;
    font-size: 12px;
    white-space: nowrap;

    &:hover {
      background: #0052d9;
      color: #fff;
    }
  }

  &-item-select {
    background: #0052d9;
    color: #fff;
  }
}

.hoverNum {
  text-align: right;
  padding: 0 10px;
  font-size: 26px;
  font-weight: bold;
  height: 60px;
  line-height: 60px;
}
</style>