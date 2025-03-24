<template>
  <div class="flex1">
    <vxe-table row-id="id"
               ref="table"
               :data="dataList"
               highlight-hover-row
               show-overflow
               :loading="loading">
      <vxe-column type="seq" width="60" align="center"/>
      <vxe-column title="类型" field="configType" min-width="150"/>
      <vxe-column title="功能" field="name" min-width="150"/>
      <vxe-column title="说明" field="description"/>
      <vxe-column title="状态" field="enabled" width="80" align="center">
        <template #default="{row}">
          <Tag color="primary" v-if="row.enabled" @click="trigger(row)" class="cursor-pointer">启用</Tag>
          <Tag color="red" v-else @click="trigger(row)" class="cursor-pointer">禁用</Tag>
        </template>
      </vxe-column>

    </vxe-table>
  </div>
</template>

<script>

import AccountBook from "@js/api/setting/AccountBook";
import SystemConfig from "@js/api/setting/SystemConfig";
import {message} from "heyui.ext";
import {CopyObj} from "@common/utils";
import manba from "manba";

export default {
  name: "SystemConfigForm",
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
      dataList: [],
      model: {
        id: null,
        name: null,
        startDate: null,
        enabled: true,
      },
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

    },
    loadList() {
      this.loading = true;
      SystemConfig.list(this.queryParams).then(({data}) => {
        console.log(data)
        this.dataList = data;
      }).finally(() => this.loading = false);
    },
  },
  created() {
    this.loadList();
    CopyObj(this.model, this.accountBook);
  }
}
</script>
