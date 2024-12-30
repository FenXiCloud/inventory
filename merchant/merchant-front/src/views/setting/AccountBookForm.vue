<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <Form ref="form" :model="model" :rules="validationRules" :labelWidth="120" >
        <FormItem label="门店名称" required prop="name">
          <Input placeholder="请输入门店名称" v-model="model.name"/>
        </FormItem>
        <FormItem label="启用日期" required prop="startDate">
          <DatePicker v-model="model.startDate" format="YYYY-MM" type="month" :clearable="false"/>
        </FormItem>
      </Form>
    </div>
    <div class="modal-column-right">
      <Button icon="fa fa-save" style="justify-content: right" color="primary" @click="confirm" :loading="loading">
        保存
      </Button>
    </div>
  </div>
</template>

<script>

import AccountBook from "@js/api/AccountBook";
import {message} from "heyui.ext";
import {CopyObj} from "@common/utils";
import manba from "manba";

export default {
  name: "AccountBookForm",
  emits: {
    close: null,
    success: null
  },
  props: {
    accountBook: Object,
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
        password: null,
      },
      validationRules: {
        mobile: ['phone']
      }
    }
  },
  methods: {
    confirm() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        this.loading = true;
        this.model.startDate = manba(this.model.startDate).format("YYYY-MM")
        AccountBook.save(this.model).then(() => {
          message("保存成功~");
          this.$emit('success');
        }).finally(() => this.loading = false);
      }
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
}
</script>
