<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <t-form
        ref="form"
        :data="model"
        :rules="validationRules"
        label-width="120px"
      >
        <t-form-item label="账套名称" name="name">
          <t-input placeholder="请输入账套名称" v-model="model.name" />
        </t-form-item>
        <t-form-item label="启用日期" name="startDate">
          <t-date-picker
            v-model="model.startDate"
            mode="month"
            format="YYYY-MM"
            value-type="YYYY-MM"
            :clearable="false"
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
import { CopyObj } from '@common/utils';
import manba from 'manba';

export default {
  name: 'AccountBookForm',
  emits: {
    close: null,
    success: null
  },
  props: {
    accountBook: Object
  },
  data() {
    return {
      loading: false,
      merchantList: [],
      areaList: [],
      levelList: [],
      warehouseList: [],
      model: {
        id: null,
        address: null,
        code: null,
        merchantId: null,
        type: 0,
        email: null,
        linkman: null,
        name: null,
        phone: null,
        areaId: null,
        warehouseId: null,
        levelId: null,
        startDate: null,
        openAccount: true,
        username: null,
        password: null
      },
      validationRules: {
        name: [{ required: true, message: '请输入账套名称', type: 'error', trigger: 'blur' }],
        startDate: [{ required: true, message: '请选择启用日期', type: 'error' }]
      }
    };
  },
  methods: {
    confirm() {
      this.$refs.form.validate().then((result) => {
        if (result !== true) return;
        this.loading = true;
        this.model.startDate = manba(this.model.startDate).format('YYYY-MM');
        AccountBook.save(this.model)
          .then(() => {
            MessagePlugin.success('保存成功~');
            this.$emit('success');
          })
          .finally(() => (this.loading = false));
      }).catch(() => {});
    },
    init() {
      // this.loading = true;
      // Promise.all([
      //   Area.select(),
      //   Level.select(),
      //   Warehouse.select(),
      // ]).then((results) => {
      //   this.areaList = results[0].data
      //   this.levelList = results[1].data
      //   this.warehouseList = results[2].data
      // }).finally(() => this.loading = false);
    }
  },
  created() {
    this.init();
    CopyObj(this.model, this.accountBook);
  }
};
</script>
