<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <t-form
          ref="form"
          :data="model"
          :rules="rules"
          label-width="140px"
          :disabled="loading"
      >
        <t-form-item label="成本核算方法" name="costAccounting">
          <t-select
              v-model="model.costAccounting"
              :options="costAccountingOptions"
              placeholder="请选择"
              clearable
              style="width: 100%; border-radius: 4px"
          />
        </t-form-item>
        <t-form-item label="可用库存允许为负" name="availableInventory">
          <t-select
              v-model="model.availableInventory"
              :options="availableInventoryOptions"
              placeholder="请选择"
              clearable
              style="width: 100%; border-radius: 4px"
          />
        </t-form-item>
        <t-form-item label="数量小数位" name="quantityDecimal">
            <!-- 参数自身为整数，恒0位 -->
          <t-input-number
              v-model="model.quantityDecimal"
              theme="column"
              :min="0"
              :max="4"
              :decimal-places="0"
              placeholder="0-4（只可增大）"
              help="与库存数量列精度一致，仅允许从小到大调整"
              style="width: 100%"
          />
        </t-form-item>
        <t-form-item label="单价小数位" name="priceDecimal">
            <!-- 参数自身为整数，恒0位 -->
          <t-input-number
              v-model="model.priceDecimal"
              theme="column"
              :min="0"
              :max="6"
              :decimal-places="0"
              placeholder="0-6（只可增大）"
              help="与单价列精度一致，仅允许从小到大调整；金额恒2位"
              style="width: 100%"
          />
        </t-form-item>
        <t-divider>以销定购</t-divider>
        <t-form-item label="默认单据状态" name="toOrderDefaultStatus">
          <t-select
              v-model="model.toOrderDefaultStatus"
              :options="toOrderDefaultStatusOptions"
              placeholder="请选择"
              style="width: 100%; border-radius: 4px"
          />
        </t-form-item>
        <t-form-item label="是否自动审核" name="toOrderAutoAudit">
          <t-select
              v-model="model.toOrderAutoAudit"
              :options="booleanOptions"
              placeholder="请选择"
              style="width: 100%; border-radius: 4px"
          />
        </t-form-item>
        <t-form-item label="是否允许分批采购" name="toOrderAllowPartial">
          <t-select
              v-model="model.toOrderAllowPartial"
              :options="booleanOptions"
              placeholder="请选择"
              style="width: 100%; border-radius: 4px"
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
import AccountBook from '@js/api/setting/AccountBook';
import { MessagePlugin } from 'tdesign-vue-next';
import { DialogPlugin } from '@common/dialog-plugin';
import { h } from 'vue';

const COST_LABEL = (v) => (v === 2 ? '先进先出法' : '移动平均法');
const NEG_LABEL = (v) => (v === 1 ? '是' : '否');
const BOOL_LABEL = (v) => (v ? '是' : '否');

export default {
  name: 'SystemConfigForm',
  emits: ['close', 'success'],
  props: {
    accountBook: Object
  },
  data() {
    return {
      loading: false,
      costAccountingOptions: [
        { value: 1, label: '移动平均法' },
        { value: 2, label: '先进先出法' }
      ],
      availableInventoryOptions: [
        { value: 1, label: '是' },
        { value: 2, label: '否' }
      ],
      toOrderDefaultStatusOptions: [
        { value: '草稿', label: '草稿（未审核，可编辑）' },
        { value: '已审核', label: '已审核（自动审核）' }
      ],
      booleanOptions: [
        { value: true, label: '是' },
        { value: false, label: '否' }
      ],
      model: {
        id: null,
        accountBookId: null,
        costAccounting: 1,
        availableInventory: 2,
        quantityDecimal: 2,
        priceDecimal: 2,
        toOrderDefaultStatus: '草稿',
        toOrderAutoAudit: false,
        toOrderAllowPartial: true
      },
      // 打开时快照，保存前 diff 用（二次确认文案按变更字段生成影响说明）
      original: null,
      rules: {
        costAccounting: [{ required: true, message: '请选择成本核算方法' }],
        availableInventory: [{ required: true, message: '请选择是否允许库存为负' }],
        quantityDecimal: [
          { required: true, message: '请输入数量小数位' },
          {
            validator: (val) => val != null && val >= 0 && val <= 4,
            message: '数量小数位须在 0~4 之间（与库存列精度一致）'
          },
          {
            validator: (val) => this.original == null || this.original.quantityDecimal == null || val >= this.original.quantityDecimal,
            message: '数量小数位不能由大改小'
          }
        ],
        priceDecimal: [
          { required: true, message: '请输入单价小数位' },
          {
            validator: (val) => val != null && val >= 0 && val <= 6,
            message: '单价小数位须在 0~6 之间（与单价列精度一致）'
          },
          {
            validator: (val) => this.original == null || this.original.priceDecimal == null || val >= this.original.priceDecimal,
            message: '单价小数位不能由大改小'
          }
        ]
      }
    };
  },
  methods: {
    buildChanges() {
      const o = this.original || {};
      const m = this.model;
      const changes = [];
      if (m.costAccounting !== o.costAccounting) {
        changes.push(`成本核算方法：${COST_LABEL(o.costAccounting)} → ${COST_LABEL(m.costAccounting)}。保存时将自动完成批次期初衔接`
          + (m.costAccounting === 2
            ? '（结余库存按当前均价登记为第一批次，后续按批次单位成本核算）'
            : '（清空批次成本标记，按批次剩余加权成本回写期初均价，后续按移动平均核算）')
          + '；历史单据与成本不改写。');
      }
      if (m.availableInventory !== o.availableInventory) {
        changes.push(`可用库存允许为负：${NEG_LABEL(o.availableInventory)} → ${NEG_LABEL(m.availableInventory)}。`
          + (m.availableInventory === 1
            ? '出库单/货位调拨在库存不足时将放行（允许负库存）；月末结账仍强制要求负库存清零。'
            : '出库单/货位调拨恢复库存不足拦截。'));
      }
      if (m.quantityDecimal !== o.quantityDecimal) {
        changes.push(`数量小数位：${o.quantityDecimal} → ${m.quantityDecimal}。全部页面的数量录入与显示同步生效（只可增大，上限4位）。`);
      }
      if (m.priceDecimal !== o.priceDecimal) {
        changes.push(`单价小数位：${o.priceDecimal} → ${m.priceDecimal}。全部页面的单价录入与单价类显示同步生效（只可增大，上限6位）；金额保持2位。`);
      }
      if (m.toOrderDefaultStatus !== o.toOrderDefaultStatus) {
        changes.push(`以销定购默认单据状态：${o.toOrderDefaultStatus} → ${m.toOrderDefaultStatus}。`);
      }
      if (m.toOrderAutoAudit !== o.toOrderAutoAudit) {
        changes.push(`以销定购自动审核：${BOOL_LABEL(o.toOrderAutoAudit)} → ${BOOL_LABEL(m.toOrderAutoAudit)}。`);
      }
      if (m.toOrderAllowPartial !== o.toOrderAllowPartial) {
        changes.push(`以销定购允许分批采购：${BOOL_LABEL(o.toOrderAllowPartial)} → ${BOOL_LABEL(m.toOrderAllowPartial)}。`);
      }
      return changes;
    },
    confirm() {
      this.$refs.form.validate().then((result) => {
        if (result !== true) return;
        const changes = this.buildChanges();
        if (!changes.length) {
          MessagePlugin.info('未检测到任何变更~');
          return;
        }
        const costChanged = changes.some((c) => c.indexOf('成本核算方法') === 0);
        if (costChanged) {
          // 期间内切换需警告：先拉预览（本期已审核流水数），再决定确认框样式
          AccountBook.costSwitchPreview(this.model.accountBookId ?? this.accountBook?.id)
            .then(({ data }) => this.openConfirm(changes, data || {}))
            .catch(() => this.openConfirm(changes, {}));
          return;
        }
        this.openConfirm(changes, {});
      }).catch(() => {});
    },
    openConfirm(changes, preview) {
      const midPeriod = (preview.periodFlowCount || 0) > 0;
      const lines = [];
      if (midPeriod) {
        lines.push(h('p', { style: 'color:#e37318;font-weight:600;margin:0 0 8px' },
          `警告：当前期间（自 ${preview.periodStart || '本期期初'} 起）已有 ${preview.periodFlowCount} 条已审核出入库流水，属于期间内切换成本核算方法，`
          + '可能存在新旧口径衔接风险，建议结账后在期间初切换。已知限制：切换后反审核切换前的出库单会复活旧的零成本批次层。'));
      }
      lines.push(h('p', { style: 'margin:0 0 6px' }, '以下变更将立即生效，请确认：'));
      changes.forEach((c) => lines.push(h('p', { style: 'margin:0 0 6px' }, '· ' + c)));
      DialogPlugin.confirm({
        header: midPeriod ? '期间内切换成本核算方法' : '确认修改账套参数',
        theme: midPeriod ? 'warning' : 'info',
        body: () => h('div', { style: 'max-height:320px;overflow:auto;line-height:1.7' }, lines),
        confirmBtn: midPeriod ? '仍要切换并保存' : '确认保存',
        width: '560px',
        onConfirm: () => {
          // dialog-plugin 封装会在确认后自动关闭弹窗，无需手动 destroy
          this.submit(midPeriod);
        }
      });
    },
    submit(costMethodConfirmed) {
      this.loading = true;
      AccountBook.saveParameters(Object.assign({}, this.model, { costMethodConfirmed }))
        .then(({ data }) => {
          if (data && data.switched) {
            MessagePlugin.success(`保存成功~ 已完成 ${data.affectedCombos} 个品仓组合的批次期初衔接（封闭 ${data.closedLayers} 层、新建衔接层 ${data.createdLayers} 个）`, 6);
          } else {
            MessagePlugin.success('保存成功~');
          }
          this.original = Object.assign({}, this.model);
          // 修改的是当前登录账套 → 立即重拉 /init，小数位等全局参数即时生效
          const cur = this.$store.state.accountBook;
          const refresh = this.model.accountBookId != null && String(this.model.accountBookId) === String(cur && cur.key)
            ? this.$store.dispatch('init').catch(() => {})
            : Promise.resolve();
          refresh.finally(() => this.$emit('success'));
        })
        .catch((err) => {
          // 后端确认闸兜底（如预览后期间状态有变）：重新弹带流水数的警告确认
          if (err && err.msg && err.msg.indexOf('已审核出入库流水') >= 0) {
            const changes = this.buildChanges();
            this.openConfirm(changes, { periodStart: null, periodFlowCount: (err.msg.match(/已有 (\d+) 条/) || [])[1] });
          }
        })
        .finally(() => (this.loading = false));
    },
    loadList() {
      if (!this.accountBook?.id) return;
      this.loading = true;
      AccountBook.getByAccountBookId({ id: this.accountBook.id })
        .then(({ data }) => {
          if (data) {
            this.model = {
              id: data.id,
              accountBookId: data.accountBookId,
              costAccounting: data.costAccounting ?? 1,
              availableInventory: data.availableInventory ?? 2,
              quantityDecimal: data.quantityDecimal ?? 2,
              priceDecimal: data.priceDecimal ?? 2,
              toOrderDefaultStatus: data.toOrderDefaultStatus ?? '草稿',
              toOrderAutoAudit: data.toOrderAutoAudit ?? false,
              toOrderAllowPartial: data.toOrderAllowPartial ?? true
            };
          } else {
            this.model.accountBookId = this.accountBook.id;
          }
          this.original = Object.assign({}, this.model);
        })
        .finally(() => (this.loading = false));
    }
  },
  created() {
    this.loadList();
  }
};
</script>
