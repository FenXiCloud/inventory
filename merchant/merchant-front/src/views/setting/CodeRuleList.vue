<template>
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
        <vxe-toolbar>
          <template #buttons>
            <Button @click="showForm()" color="primary">新 增</Button>
          </template>
          <template #tools>
            <Input id="name" v-model="params.name" class="flex-1" placeholder="请输入规则名称"/>
            <Button color="primary" :loading="loading" @click="doSearch">查询</Button>
          </template>
        </vxe-toolbar>

        <vxe-table row-id="id"
                   ref="table"
                   :data="dataList"
                   highlight-hover-row
                   show-overflow
                   :loading="loading">
          <vxe-column type="seq" width="60" align="center"/>
          <vxe-column title="规则名称" field="name" width="200"/>
          <vxe-column title="单据类型" field="documentType"/>
          <vxe-column title="编码规则" field="format"/>
          <vxe-column title="流水号位数" field="serialNumberLength" width="120"/>
          <vxe-column title="创建时间" field="createdAt" width="120"/>
          <vxe-column title="默认" field="systemDefault" width="80" align="center">
            <template #default="{row}">
              <Tag color="primary" v-if="row.systemDefault" @click="trigger(row)" class="cursor-pointer">是</Tag>
              <Tag color="red" v-else @click="trigger(row)" class="cursor-pointer">否</Tag>
            </template>
          </vxe-column>
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
</template>

<script>
import CodeRule from "@js/api/setting/CodeRule";
import {confirm, message} from "heyui.ext";
import CodeRuleForm from "./CodeRuleForm.vue";
import {layer} from "@layui/layer-vue";
import {h} from "vue";
export default {
  name: "CodeRuleList",
  props: {
    merchant: Object,
  },
  components: {CodeRuleForm},
  data() {
    return {
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
        {id: 11, documentType: '成本调整单', type: 1},
        {id: 12, documentType: '收款单', type: 1},
        {id: 13, documentType: '付款单', type: 1},
        {id: 14, documentType: '核销单', type: 1},
        {id: 15, documentType: '其他收款单', type: 1},
        {id: 16, documentType: '转帐单', type: 1},
        {id: 17, documentType: '商品', type: 2},
        {id: 18, documentType: '仓库', type: 2},
        {id: 19, documentType: '客户', type: 2},
        {id: 20, documentType: '供货商', type: 2}
      ],
      opened: true,
      loading: false,
      params: {
        name:null,
        documentType: '采购订单',
      },
      checkedRows: [],
      dataList: [],
      areaList: [],
      merchantList: [],
      param: [
        {title: '启用', key: 'enabled'},
        {title: '禁用', key: 'disabled'},
      ]

    }
  },
  computed: {
    queryParams() {
      return Object.assign(this.params, {})
    }
  },
  mounted() {
    // 默认选中第一个单据类型
    this.selectFirstDocumentType();
  },
  methods: {
    selectFirstDocumentType() {
      // 默认选中第一个单据类型
      const table = this.$refs.documentTypeGridRef;
      table.setRadioRow(this.documentTypeDataList[0]);
    },

    handleDocumentTypeChange(data) {
      // 单选框变化时的处理函数
      this.params.documentType = data.row.documentType;
      this.loadList();

    },
    showForm(CodeRule) {
      const table = this.$refs.documentTypeGridRef;
      if (table) {
        const currRow = table.getRadioRecord();
        if (!CodeRule && currRow){
          CodeRule =  {documentType: currRow.documentType};
        }
      }

      let layerId = layer.open({
        title: "规则编码",
        shadeClose: false,
        area: ['50vw', 'auto'],
        content: h(CodeRuleForm, {
          CodeRule,
          onClose: () => {
            layer.close(layerId);
          },
          onSuccess: () => {
            this.doSearch();
            layer.close(layerId);
          }
        })
      });
    },
    loadList() {
      this.loading = true;
      CodeRule.list(this.queryParams).then(({data}) => {
        this.dataList = data;
      }).finally(() => this.loading = false);
    },
    doSearch() {
      this.loadList();
    },
    doRemove(row) {
      confirm({
        title: "系统提示",
        content: `确认删除规则：${row.name}?`,
        onConfirm: () => {
          CodeRule.remove(row.id).then(() => {
            message("删除成功~");
            this.doSearch();
          })
        }
      })
    },
    trigger(row) {
      let systemDefault = !row.systemDefault;
      let documentType = row.documentType;
      confirm({
        title: "系统提示",
        content: `确认要「${systemDefault ? "启用" : "禁用"}」规则：${row.name}?`,
        onConfirm: () => {
          CodeRule.save({id: row.id,systemDefault: systemDefault,documentType: documentType}).then((success) => {
            console.log(success);
            message("操作成功~");
            this.loadList();
          })
        }
      })
    }
  },
  created() {
    this.doSearch();
  }
}
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