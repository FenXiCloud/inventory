<template>
  <div class="modal-column">
    <div>
      <Button @click="showForm()" status="primary" class="float-right mt-4px mr-16px">新 增</Button>
    </div>
    <div class="flex-1 p-16px">
      <div class="border p-8px mb-16px" v-for="(items,key) in dataList" :key="key">
        <vxe-toolbar>
          <template #buttons>
            <div class="text-14px">
              模板名称：{{ items.title }} 模板类型：{{ items.type }} 凭证：{{ items.word }}
            </div>
          </template>
          <template #tools>
            <Button @click="showForm(items.id)" color="primary">编辑</Button>
            <Button @click="doRemove(items.id)">删除</Button>
          </template>
        </vxe-toolbar>
        <div class="mt-16px">
          <vxe-table size="mini" ref="xTable" border="border" show-overflow keep-source
                     :row-config="{ height: 40, isCurrent: true, isHover: true }"
                     stripe
                     :data="items.details">
            <vxe-column title="会计科目" field="subjectName">
            </vxe-column>
            <vxe-column field="balanceDirection" title="借贷方向" width="100"></vxe-column>
          </vxe-table>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

import {layer} from "@layui/layer-vue";
import {h} from "vue";
import FinanceVoucher from "@js/api/setting/FinanceVoucher";
import {confirm, message} from "heyui.ext";
import VoucherForm from "./VoucherForm.vue";

export default {
  name: "Voucher",
  data() {
    return {
      dataList: []
    }
  },
  methods: {
    showForm(id) {
      console.info("showForm:", id)
      let layerId = layer.open({
        title: "选择生成凭证",
        shadeClose: false,
        area: ['90%', '600px'],
        content: h(VoucherForm, {
          id,
          onClose: () => {
            layer.close(layerId);
          },
          onSuccess: () => {
            this.loadList();
            layer.close(layerId);
          }
        })
      });
    },
    loadList() {
      FinanceVoucher.list({}).then(({data}) => {
        console.log(data);
        this.dataList = data;
      })
    },
    doRemove(id) {
      confirm({
        title: "系统提示",
        content: `是否删除当前数据?`,
        onConfirm: () => {
          FinanceVoucher.delete(id).then(({data}) => {
            message.success("操作成功～");
            this.loadList();
          });
        },
      });
    }
  },
  created() {
    this.loadList();
  }
}
</script>

<script setup>


</script>
