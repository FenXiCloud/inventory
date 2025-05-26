<template>
  <div class="modal-column">
    <div class="mt-10px">
      <Tabs :datas="param" v-model="selected" @change="changeTab"></Tabs>
    </div>
    <div class="mt-10px">
      <Button @click="showForm()" status="primary" class="float-right mt-4px mr-16px">新 增</Button>
      <Button @click="batchShowForm()" status="primary" class="float-right mt-4px mr-16px">批 量 新 增</Button>
    </div>
    <div class="flex-1 p-16px">
      <div class="border p-8px mb-16px" v-for="(items,key) in dataList" :key="key">
        <vxe-toolbar>
          <template #tools>
            <Button @click="showForm(items.id)" color="primary">编辑</Button>
            <Button @click="doRemove(items.id)">删除</Button>
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

import {layer} from "@layui/layer-vue";
import {h} from "vue";
import FinanceItemMap from "@js/api/setting/FinanceItemMap";
import {confirm, message} from "heyui.ext";
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
        6: '商品'
      },
      selected: '0',
      categoryType: ''
    }
  },
  methods: {
    showForm(id) {
      console.info("showForm:", id)
      let layerId = layer.open({
        title: `${this.param[this.selected]}-辅助项映射设置`,
        shadeClose: false,
        area: ['800px', '600px'],
        content: h(FinanceItemMapForm, {
          id,
          categoryId: this.selected,
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
    batchShowForm(id) {
      let layerId = layer.open({
        title: `${this.param[this.selected]}-辅助项映射设置`,
        shadeClose: false,
        area: ['800px', '600px'],
        content: h(FinanceItemMapBatchForm, {
          id,
          categoryId: this.selected,
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
      FinanceItemMap.list({categoryId: this.selected}).then(({data}) => {
        console.log(data);
        this.dataList = data;
      })
    },
    doRemove(id) {
      confirm({
        title: "系统提示",
        content: `是否删除当前数据?`,
        onConfirm: () => {
          FinanceItemMap.delete(id).then(({data}) => {
            message.success("操作成功～");
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
