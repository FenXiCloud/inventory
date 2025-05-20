<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <Form
          ref="form"
          :model="model"
          :rules="validationRules"
          :labelWidth="140"
      >
        <FormItem label="成本核算方法" required prop="costAccounting">
          <Select
              v-model="model.costAccounting"
              :datas="accSelectParams"
          ></Select>
        </FormItem>
        <FormItem label="可用库存允许为负" required prop="availableInventory">
          <Select
              v-model="model.availableInventory"
              :datas="costAccSelectParams"
          ></Select>
        </FormItem>
        <FormItem label="数量小数位" required prop="quantityDecimal">
          <Input
              type="number"
              min="0"
              max="8"
              placeholder="请输入数量小数位"
              v-model="model.quantityDecimal"
          />
        </FormItem>
        <FormItem label="单价小数位" required prop="priceDecimal">
          <Input
              type="number"
              min="0"
              max="8"
              placeholder="请输入数量小数位"
              v-model="model.priceDecimal"
          />
        </FormItem>
      </Form>
    </div>
    <div class="modal-column-right">
      <Button
          icon="fa fa-save"
          style="justify-content: right"
          color="primary"
          @click="confirm"
          :loading="loading"
      >
        保存
      </Button>
    </div>
    <!-- <vxe-table
      row-id="id"
      ref="table"
      :data="dataList"
      highlight-hover-row
      show-overflow
      :loading="loading"
    >
      <vxe-column type="seq" width="60" align="center" />
      <vxe-column title="成本核算方法" field="accountBookId" min-width="150" />
      <vxe-column
        title="可用库存允许为负"
        field="costAccounting"
        min-width="150"
      >
        <template #default="{ row }">
          <Tag
            color="primary"
            v-if="row.accountBookId == 1"
            @click="trigger(row)"
            class="cursor-pointer"
            >启用</Tag
          >
          <Tag color="red" v-else @click="trigger(row)" class="cursor-pointer"
            >禁用</Tag
          >
        </template>
      </vxe-column>
      <vxe-column title="数量小数位" width="150" field="availableInventory" />
      <vxe-column
        title="单价小数位"
        field="quantityDecimal"
        width="150"
        align="center"
      >
      </vxe-column>
    </vxe-table> -->
  </div>
</template>

<script>
import AccountBook from '@js/api/setting/AccountBook';
import SystemConfig from '@js/api/setting/SystemConfig';
import { message } from 'heyui.ext';
import { CopyObj } from '@common/utils';
import manba from 'manba';

export default {
  name: 'SystemConfigForm',
  emits: {
    close: null,
    success: null
  },
  props: {
    accountBook: Object
  },
  data() {
    return {
      accSelectParams: [
        {
          key: 1,
          title: '移动平均法'
        },
        {
          key: 2,
          title: '先进先出法'
        }
      ],
      costAccSelectParams: [
        {
          key: 1,
          title: '是'
        },
        {
          key: 2,
          title: '否'
        }
      ],

      loading: false,
      // dataList: [],
      model: {
        // accountBookId: null,
        // costAccounting: null,
        // availableInventory: null,
        // quantityDecimal: null,
        // id: null,
        // priceDecimal: null
      }
    };
  },
  methods: {
    confirm() {
      let validResult = this.$refs.form.valid();
      console.log(validResult, this.model, 'validResult');
      if (validResult.result) {
        this.loading = true;
        AccountBook.saveParameters(this.model)
            .then(() => {
              message('保存成功~');
              this.$emit('success');
            })
            .finally(() => (this.loading = false));
      }
    },
    loadList() {
      this.loading = true;
      AccountBook.getByAccountBookId({ id: this.accountBook.id })
          .then(({ data }) => {
            console.log(data);
            this.model = data;
          })
          .finally(() => (this.loading = false));
    },
    loadList22() {
      this.loading = true;
      SystemConfig.list(this.queryParams)
          .then(({ data }) => {
            console.log(data);
            this.dataList = data;
          })
          .finally(() => (this.loading = false));
    }
  },
  created() {
    this.loadList();
    // CopyObj(this.model, this.accountBook);
  }
};
</script>
