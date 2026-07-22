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
          <t-input-number
              v-model="model.quantityDecimal"
              theme="column"
              :min="0"
              :max="8"
              :decimal-places="0"
              placeholder="0-8"
              style="width: 100%"
          />
        </t-form-item>
        <t-form-item label="单价小数位" name="priceDecimal">
          <t-input-number
              v-model="model.priceDecimal"
              theme="column"
              :min="0"
              :max="8"
              :decimal-places="0"
              placeholder="0-8"
              style="width: 100%"
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
      model: {
        id: null,
        accountBookId: null,
        costAccounting: 1,
        availableInventory: 2,
        quantityDecimal: 2,
        priceDecimal: 2
      },
      rules: {
        costAccounting: [{ required: true, message: '请选择成本核算方法' }],
        availableInventory: [{ required: true, message: '请选择是否允许库存为负' }],
        quantityDecimal: [
          { required: true, message: '请输入数量小数位' },
          {
            validator: (val) => val != null && val >= 0 && val <= 8,
            message: '数量小数位须在 0~8 之间'
          }
        ],
        priceDecimal: [
          { required: true, message: '请输入单价小数位' },
          {
            validator: (val) => val != null && val >= 0 && val <= 8,
            message: '单价小数位须在 0~8 之间'
          }
        ]
      }
    };
  },
  methods: {
    confirm() {
      this.$refs.form.validate().then((result) => {
        if (result !== true) return;
        this.loading = true;
        AccountBook.saveParameters(this.model)
          .then(() => {
            MessagePlugin.success('保存成功~');
            this.$emit('success');
          })
          .finally(() => (this.loading = false));
      }).catch(() => {});
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
              priceDecimal: data.priceDecimal ?? 2
            };
          } else {
            this.model.accountBookId = this.accountBook.id;
          }
        })
        .finally(() => (this.loading = false));
    }
  },
  created() {
    this.loadList();
  }
};
</script>
