<template>
  <div class="modal-column">
    <div>
      <t-button theme="primary" class="float-right mt-4px mr-16px" @click="showForm()">新 增</t-button>
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
            <t-button theme="primary" @click="showForm(items.id)">编辑</t-button>
            <t-button @click="doRemove(items.id)">删除</t-button>
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

import {openDialog, closeDialog} from '@common/dialog';
import {h} from "vue";
import TemplateConfigForm from "./TemplateConfigForm.vue";
import FinanceVoucherTemplate from "@js/api/setting/FinanceVoucherTemplate";
import {MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';

export default {
  name: "VoucherTemplate",
  data() {
    return {
      dataList: []
    }
  },
  methods: {
    showForm(id) {
      let dialogId = openDialog({
        header: "凭证模板",
        closeOnOverlayClick: false,
        width: '800px',
        body: h(TemplateConfigForm, {
          id,
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
    getDocumentType(id) {
      let result = '';
      this.documentTypeDataList.forEach(item => {
        if (Number(item.id) === Number(id)) {
          result = item.documentType;
        }
      });
      return result;
    },
    loadList() {
      FinanceVoucherTemplate.list({}).then(({data}) => {
        this.dataList = data;
      })
    },
    doRemove(id) {
      DialogPlugin.confirm({
        header: "系统提示",
        body: `是否删除当前数据?`,
        onConfirm: () => {
          FinanceVoucherTemplate.delete(id).then(({data}) => {
            MessagePlugin.success("操作成功～");
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
