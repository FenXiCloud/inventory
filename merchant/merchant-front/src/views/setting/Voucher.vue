<template>
  <div class="modal-column">
    <div>
      <Button @click="showForm()" status="primary" class="float-right mt-4px mr-16px">新 增</Button>
    </div>
    <!--    <div class="flex-1 p-16px">-->
    <!--      <div class="border p-8px mb-16px" v-for="(items,key) in dataList" :key="key">-->
    <!--        <vxe-toolbar>-->
    <!--          <template #buttons>-->
    <!--            <div class="text-14px">-->
    <!--              凭证类型：{{ items.type }} 凭证code：{{ items.code }}-->
    <!--            </div>-->
    <!--          </template>-->
    <!--          <template #tools>-->
    <!--&lt;!&ndash;            <Button @click="showForm(items.id)" color="primary">编辑</Button>&ndash;&gt;-->
    <!--&lt;!&ndash;            <Button @click="doRemove(items.id)">删除</Button>&ndash;&gt;-->
    <!--          </template>-->
    <!--        </vxe-toolbar>-->
    <!--      </div>-->
    <!--    </div>-->
    <div class="frame-page flex flex-column">

      <div class="parent_container">
        <div class="left">
          <vxe-table
              border
              ref="documentTypeGridRef"
              size="mini"
              :data="documentTypeDataList"
              @radio-change="handleDocumentTypeChange"
              :rowConfig="{isCurrent: true,isHover: true}"
              :radio-config="{trigger: 'row',labelField: 'documentType',highlight: true}">
            <vxe-column field="documentType" title="单据类型"></vxe-column>
          </vxe-table>
        </div>

        <div class="right">
          <!--          <vxe-toolbar>-->
          <!--            <template #buttons>-->
          <!--              <Button @click="showForm()" color="primary">新 增</Button>-->
          <!--            </template>-->
          <!--            <template #tools>-->
          <!--              <Input id="name" v-model="params.name" class="flex-1" placeholder="请输入规则名称"/>-->
          <!--              <Button color="primary" :loading="loading" @click="doSearch">查询</Button>-->
          <!--            </template>-->
          <!--          </vxe-toolbar>-->

          <vxe-table row-id="id"
                     ref="table"
                     :data="dataList"
                     highlight-hover-row
                     show-overflow
                     :loading="loading">
            <vxe-column type="seq" width="60" align="center"/>
            <vxe-column title="凭证类型" field="name" width="200"/>
            <vxe-column title="凭证code" field="documentType"/>
            <vxe-column title="操作" align="center" width="120" fixed="right">
              <template #default="{row}">
                <div class="flex items-center justify-center">
                  <span class=" primary-color text-hover ml-10px" @click="showForm(row)" size="s">编辑</span>
                  <span class=" primary-color text-hover ml-10px" @click="doRemove(row)" size="s">删除</span>
                </div>
              </template>
            </vxe-column>
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
      loading: false,
      documentTypeDataList: [
        {id: 1, documentType: '期初余额', type: 1},
        {id: 2, documentType: '采购入库', type: 1},
        {id: 3, documentType: '销售退货', type: 1},
        {id: 4, documentType: '其他入库', type: 1},
        {id: 5, documentType: '盘盈入库', type: 1},
        {id: 6, documentType: '调拨入库', type: 1},
        {id: 7, documentType: '采购退货', type: 1},
        {id: 8, documentType: '销售出库', type: 1},
        {id: 9, documentType: '其他出库', type: 1},
        {id: 10, documentType: '调拨出库', type: 1},
        {id: 11, documentType: '盘亏出库', type: 1},
        {id: 12, documentType: '成本调整', type: 1}
      ],
      dataList: [],
      params: {
        name: null,
        documentType: '期初余额',
      },
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
            this.loadList();
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
    },
    handleDocumentTypeChange(data) {
      // 单选框变化时的处理函数
      this.params.type = data.row.documentType;
      this.loadList();

    },
    selectFirstDocumentType() {
      // 默认选中第一个单据类型
      const table = this.$refs.documentTypeGridRef;
      table.setRadioRow(this.documentTypeDataList[0]);
    },
  },
  created() {
    this.loadList();
  },
  mounted() {
    // 默认选中第一个单据类型
    this.selectFirstDocumentType();
  },
}
</script>

<script setup>


</script>

<style lang="less" scoped>

.parent_container {
  display: flex;
  height: 100%;
}

.left {
  width: 300px; /* 固定宽度 */
  padding: 20px;
}

.right {
  flex: 1; /* 占用剩余空间 */
  padding: 20px;
}

.selected {
  background-color: #dddddd;
}

</style>