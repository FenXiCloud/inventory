<template>
  <t-form
      ref="form"
      :data="formData"
      :rules="rules"
      label-width="100px"
      @submit="onSubmit"
  >
    <t-form-item label="所属仓库" name="warehouseId">
      <t-select
          v-model="formData.warehouseId"
          :options="warehouseList"
          :keys="{ value: 'id', label: 'name' }"
          filterable
          placeholder="请选择仓库"
      />
    </t-form-item>

    <t-form-item label="货位编码" name="code">
      <t-input v-model="formData.code" placeholder="如：A-01-01" />
    </t-form-item>

    <t-form-item label="货位名称" name="name">
      <t-input v-model="formData.name" placeholder="如：整件区-1排-1层" />
    </t-form-item>

    <t-form-item label="货位类型" name="type">
      <t-radio-group v-model="formData.type">
        <t-radio value="WHOLE">整货</t-radio>
        <t-radio value="ZERO">零货</t-radio>
      </t-radio-group>
    </t-form-item>

    <t-form-item label="容量" name="capacity">
      <t-input-number v-model="formData.capacity" :min="0" :decimal-places="2" placeholder="可选" />
    </t-form-item>

    <t-form-item label="状态" name="enabled">
      <t-switch v-model="formData.enabled" :label="['禁用', '启用']" />
    </t-form-item>

    <t-form-item label="排序" name="sort">
      <t-input-number v-model="formData.sort" :min="0" placeholder="可选" />
    </t-form-item>

    <t-form-item label="备注" name="remark">
      <t-textarea v-model="formData.remark" placeholder="可选" />
    </t-form-item>

    <t-form-item>
      <t-space>
        <t-button theme="primary" type="submit">保 存</t-button>
        <t-button variant="outline" @click="onCancel">取 消</t-button>
      </t-space>
    </t-form-item>
  </t-form>
</template>

<script>
import {MessagePlugin} from "tdesign-vue-next";
import WarehouseLocation from "@js/api/basic/WarehouseLocation";

export default {
  name: "WarehouseLocationForm",
  props: {
    data: {
      type: Object,
      default: null
    },
    warehouseList: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      formData: {
        id: null,
        warehouseId: null,
        code: '',
        name: '',
        type: 'WHOLE',
        capacity: null,
        enabled: true,
        sort: 0,
        remark: ''
      },
      rules: {
        warehouseId: [{required: true, message: '请选择仓库', trigger: 'change'}],
        code: [{required: true, message: '请输入货位编码', trigger: 'blur'}],
        name: [{required: true, message: '请输入货位名称', trigger: 'blur'}],
        type: [{required: true, message: '请选择货位类型', trigger: 'change'}]
      }
    };
  },
  watch: {
    data: {
      handler(val) {
        if (val) {
          this.formData = Object.assign({}, val);
        } else {
          this.resetForm();
        }
      },
      immediate: true
    }
  },
  methods: {
    resetForm() {
      this.formData = {
        id: null,
        warehouseId: null,
        code: '',
        name: '',
        type: 'WHOLE',
        capacity: null,
        enabled: true,
        sort: 0,
        remark: ''
      };
    },
    onSubmit({validateResult}) {
      if (validateResult !== true) return;

      const api = this.formData.id ? WarehouseLocation.update : WarehouseLocation.save;
      api(this.formData).then(() => {
        MessagePlugin.success('保存成功');
        this.$emit('success');
      });
    },
    onCancel() {
      this.$emit('cancel');
    }
  }
};
</script>

<style scoped>
</style>
