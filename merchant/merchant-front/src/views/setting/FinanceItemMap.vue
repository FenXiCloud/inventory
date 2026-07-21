<template>
  <div class="modal-column">
    <div class="mt-10px">
      <t-tabs v-model="selected" @change="changeTab">
        <t-tab-panel value="0" label="客户" />
        <t-tab-panel value="1" label="供应商" />
        <t-tab-panel value="6" label="产品" />
      </t-tabs>
    </div>
    <div class="mt-10px">
      <t-button theme="primary" class="float-right mt-4px mr-16px" @click="showForm()">新 增</t-button>
      <t-button theme="primary" class="float-right mt-4px mr-16px" @click="batchShowForm()">批 量 新 增</t-button>
    </div>
    <div class="flex-1 p-16px">
      <div class="border p-8px mb-16px" v-for="(items,key) in dataList" :key="key">
        <vxe-toolbar>
          <template #tools>
            <t-button theme="primary" @click="showForm(items.id)">编辑</t-button>
            <t-button @click="doRemove(items.id)">删除</t-button>
          </template>
        </vxe-toolbar>
        <div>
          <div class="text-14px">
            财务软件名称：{{ items.financeName }} 进销存名称：{{ items.inventoryName }}
          </div>
          <br/>
          <div class="text-14px">
            财务软件code：{{ items.financeCode }} 进销存code：{{ items.inventoryCode }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

import {openDialog, closeDialog} from '@common/dialog';
import {h} from "vue";
import FinanceItemMap from "@js/api/setting/FinanceItemMap";
import {MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';
import FinanceItemMapForm from "./FinanceItemMapForm.vue";
import FinanceItemMapBatchForm from "./FinanceItemMapBatchForm.vue";

export default {
  name: "FinanceItemMap",
  data() {
    return {
      dataList: [],
      param: {
        0: '客户',
        1: '供应商',
        6: '产品'
      },
      selected: '0',
      categoryType: ''
    }
  },
  methods: {
    showForm(id) {
      let dialogId = openDialog({
        header: `${this.param[this.selected]}-辅助项映射设置`,
        closeOnOverlayClick: false,
        width: '800px',
        body: h(FinanceItemMapForm, {
          id,
          categoryId: this.selected,
          onClose: () => {
            closeDialog(dialogId);
          },
          onSuccess: () => {
            this.loadList();
            closeDialog(dialogId);
          }
        })
      });
    },
    batchShowForm(id) {
      let dialogId = openDialog({
        header: `${this.param[this.selected]}-辅助项映射设置`,
        closeOnOverlayClick: false,
        width: '800px',
        body: h(FinanceItemMapBatchForm, {
          id,
          categoryId: this.selected,
          onClose: () => {
            closeDialog(dialogId);
          },
          onSuccess: () => {
            this.loadList();
            closeDialog(dialogId);
          }
        })
      });
    },
    loadList() {
      FinanceItemMap.list({categoryId: this.selected}).then(({data}) => {
        this.dataList = data;
      })
    },
    doRemove(id) {
      DialogPlugin.confirm({
        header: "系统提示",
        body: `是否删除当前数据?`,
        onConfirm: () => {
          FinanceItemMap.delete(id).then(({data}) => {
            MessagePlugin.success("操作成功～");
            this.loadList();
          });
        },
      });
    },
    changeTab() {
      this.loadList();
    }
  },
  created() {
    this.changeTab();
  }
}
</script>

<script setup>


</script>
