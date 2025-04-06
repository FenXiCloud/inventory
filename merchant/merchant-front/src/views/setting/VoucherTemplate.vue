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
              模板名称：{{ items.title }} 模板类型：{{ getDocumentType(items.type) }} 凭证：{{ items.word }}
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
import TemplateConfigFrom from "./TemplateConfigFrom.vue";
import FinanceVoucherTemplate from "@js/api/setting/FinanceVoucherTemplate";
import {confirm, message} from "heyui.ext";

export default {
  name: "VoucherTemplate",
  data() {
    return {
      dataList: [],
      documentTypeDataList: [
        {id: 1, documentType: '采购订单', type: 1},
        {id: 2, documentType: '采购入库单', type: 1},
        {id: 3, documentType: '采购退货单', type: 1},
        {id: 4, documentType: '销售订单', type: 1},
        {id: 5, documentType: '销售出库单', type: 1},
        {id: 6, documentType: '销售退货单', type: 1},
        {id: 7, documentType: '调拨单', type: 1},
        {id: 8, documentType: '盘点单', type: 1},
        {id: 9, documentType: '其他入库单', type: 1},
        {id: 10, documentType: '其他出库单', type: 1},
        {id: 11, documentType: '成本调整单', type: 1}
      ]
    }
  },
  methods: {
    showForm(id) {
      console.info("showForm:", id)
      let layerId = layer.open({
        title: "凭证模板",
        shadeClose: false,
        area: ['800px', '600px'],
        content: h(TemplateConfigFrom, {
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
        console.log(data);
        this.dataList = data;
      })
    },
    doRemove(id) {
      confirm({
        title: "系统提示",
        content: `是否删除当前数据?`,
        onConfirm: () => {
          FinanceVoucherTemplate.delete(id).then(({data}) => {
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
