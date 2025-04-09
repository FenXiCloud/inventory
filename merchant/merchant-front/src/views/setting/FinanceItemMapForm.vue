<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <Form ref="form" :model="model" :rules="validationRules" :showErrorTip="true" :labelWidth="130">
        <FormItem label="财务软件辅助项" prop="financeId">
          <Select :datas="financeItemMappings" keyName="id" v-model="model.financeId" filterable
                  titleName="titleName" placeholder="选择财务软件辅助项" @change="changeMappings('financeId')"/>
        </FormItem>
        <FormItem label="进销存辅助项" prop="inventoryId">
          <Select :datas="itemMappings" keyName="id" v-model="model.inventoryId" filterable
                  titleName="titleName" placeholder="选择进销存辅助项" @change="changeMappings('inventoryId')"/>
        </FormItem>
      </Form>
    </div>
    <div class="modal-column-right">
      <Button icon="fa fa-close" @click="$emit('close')" :loading="loading">
        取消
      </Button>
      <Button icon="fa fa-save" color="primary" @click="confirm" :loading="loading">
        保存
      </Button>
    </div>
  </div>
</template>

<script>
import FinanceAccountLink from "@js/api/setting/FinanceAccountLink";
import FinanceItemMap from "@js/api/setting/FinanceItemMap";
import Customer from "@js/api/basic/Customer";
import Product from "@js/api/basic/Product";
import Supplier from "@js/api/basic/Supplier";
import {message} from "heyui.ext";
import {ObjectUtil} from "../../js/common/utils";

export default {
  name: "FinanceItemMapForm",
  props: {
    id: [Number, String],
    categoryId: [Number, String],
  },
  data() {
    return {
      loading: false,
      canSave: false,
      voucherWords: [],
      subjects: [],
      model: {
        id: null,
        categoryId: null,
        inventoryId: null,
        inventoryName: null,
        financeId: null,
        financeName: null,
      },
      validationRules: {
        required: ['inventoryId', 'financeId']
      },
      itemMappings: [],
      financeItemMappings: []
    }
  },
  watch: {},
  methods: {
    confirm() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        FinanceItemMap.save(this.model).then(() => {
          message("保存成功~");
          this.$emit('success');
        }).finally(() => this.loading = false);
      }
    },
    init() {
      const categoryId = this.categoryId;
      FinanceAccountLink.loadAccountingCategory({ids: this.categoryId}).then(({data}) => {
        console.info("categoryId", data)
        this.model.categoryType = data.data[0].categoryType;
        this.model.categoryId = data.data[0].id;
        this.model.categoryName = data.data[0].name;
        this.financeItemMappings = data.data[0].details || [];
        this.financeItemMappings.forEach(item => {
          item.id = Number(item.id);
          item.titleName = `${item.code} - ${item.name}`;
        });
      });
      switch (Number(categoryId)) {
        case 0:
          Customer.list().then(({data}) => {
            this.itemMappings = data.results || [];
            this.itemMappings.forEach(item => {
              item.titleName = `${item.name}`;
            });
          });
          break;
        case 1:
          Supplier.list().then(({data}) => {
            this.itemMappings = data.results;
            this.itemMappings.forEach(item => {
              item.titleName = `${item.code} - ${item.name}`;
            });
          });
          break;
        case 6:
          Product.list().then(({data}) => {
            this.itemMappings = data.results;
            this.itemMappings.forEach(item => {
              item.titleName = `${item.code} - ${item.name}`;
            });
          });
          break;
      }
    },
    changeMappings(type) {
      switch (type) {
        case 'financeId': {
          const filter = this.financeItemMappings.filter(item => {
            return item.id === this.model.financeId;
          });
          this.model.financeName = filter[0].name;
          break;
        }
        case 'inventoryId': {
          const filter = this.itemMappings.filter(item => {
            return item.id === this.model.inventoryId;
          });
          this.model.inventoryName = filter[0].name;
          break;
        }
      }
    },
    load() {
      const id = this.id;
      FinanceItemMap.load(id).then(({data}) => {
        this.model = data;
      });
    }
  },
  created() {
    this.init();
    if (this.id) {
      this.load();
    }
  }
}
</script>

<style scoped lang="less">

</style>
