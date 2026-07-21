<template>
  <div class="simple-page">
    <div class="simple-page__split">
      <div class="simple-page__side">
        <t-table
            row-key="id"
            size="small"
            bordered
            hover
            height="100%"
            table-layout="auto"
            :data="documentTypeDataList"
            :columns="documentTypeColumns"
            :selected-row-keys="selectedDocumentTypeKeys"
            :active-row-keys="selectedDocumentTypeKeys"
            @row-click="onDocumentTypeRowClick"
            @select-change="onDocumentTypeSelect"
        />
      </div>

      <div class="simple-page__main">
        <div class="simple-page__toolbar">
          <t-space break-line>
            <t-button theme="primary" style="border-radius: 4px" @click="showForm()">新 增</t-button>
            <t-input
                v-model="params.name"
                clearable
                placeholder="请输入规则名称"
                style="width: 240px; border-radius: 4px"
                @enter="doSearch"
            >
              <template #suffixIcon>
                <t-icon name="search" style="cursor:pointer" @click="doSearch"/>
              </template>
            </t-input>
            <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="doSearch">查询</t-button>
          </t-space>
        </div>

        <div class="simple-page__table">
          <t-table
              row-key="id"
              size="medium"
              bordered
              stripe
              hover
              height="100%"
              table-layout="auto"
              :data="dataList"
              :columns="columns"
              :loading="loading"
          >
            <template #systemDefault="{ row }">
              <t-tag
                  :theme="row.systemDefault ? 'primary' : 'danger'"
                  variant="light"
                  style="cursor:pointer"
                  @click="trigger(row)"
              >
                {{ row.systemDefault ? '是' : '否' }}
              </t-tag>
            </template>
            <template #ops="{ row }">
              <t-space size="small">
                <t-link theme="primary" @click="showForm(row)">编辑</t-link>
                <t-link theme="primary" @click="doRemove(row)">删除</t-link>
              </t-space>
            </template>
          </t-table>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import CodeRule from "@js/api/setting/CodeRule";
import {MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';
import CodeRuleForm from "./CodeRuleForm.vue";
import {openDialog, closeDialog} from '@common/dialog';
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
        {id: 16, documentType: '其他付款单', type: 1},
        {id: 17, documentType: '转帐单', type: 1},
        {id: 18, documentType: '商品', type: 2},
        {id: 19, documentType: '仓库', type: 2},
        {id: 20, documentType: '客户', type: 2},
        {id: 21, documentType: '供货商', type: 2}
      ],
      selectedDocumentTypeKeys: [1],
      opened: true,
      loading: false,
      params: {
        name: null,
        documentType: '采购订单',
      },
      checkedRows: [],
      dataList: [],
      areaList: [],
      merchantList: [],
      param: [
        {title: '启用', key: 'enabled'},
        {title: '禁用', key: 'disabled'},
      ],
      documentTypeColumns: [
        {colKey: 'row-select', type: 'single', width: 46},
        {colKey: 'documentType', title: '单据类型', minWidth: 120, ellipsis: true}
      ],
      columns: [
        {colKey: 'name', title: '规则名称', width: 200, ellipsis: true},
        {colKey: 'documentType', title: '单据类型', minWidth: 120},
        {colKey: 'format', title: '编码规则', minWidth: 160, ellipsis: true},
        {colKey: 'serialNumberLength', title: '流水号位数', width: 120},
        {colKey: 'createdAt', title: '创建时间', width: 140},
        {colKey: 'systemDefault', title: '默认', width: 80, align: 'center'},
        {colKey: 'ops', title: '操作', width: 120, fixed: 'right', align: 'center'}
      ]
    }
  },
  computed: {
    queryParams() {
      return Object.assign({}, this.params)
    },
    currentDocumentType() {
      const row = this.documentTypeDataList.find(item => this.selectedDocumentTypeKeys.includes(item.id));
      return row ? row.documentType : this.params.documentType;
    }
  },
  methods: {
    onDocumentTypeSelect(keys) {
      if (!keys || !keys.length) return;
      this.selectedDocumentTypeKeys = keys.slice(0, 1);
      const row = this.documentTypeDataList.find(item => item.id === keys[0]);
      if (row) {
        this.params.documentType = row.documentType;
        this.loadList();
      }
    },
    onDocumentTypeRowClick({row}) {
      this.onDocumentTypeSelect([row.id]);
    },
    showForm(CodeRule) {
      if (!CodeRule && this.currentDocumentType) {
        CodeRule = {documentType: this.currentDocumentType};
      }

      let dialogId = openDialog({
        header: "规则编码",
        closeOnOverlayClick: false,
        width: '50vw',
        body: h(CodeRuleForm, {
          CodeRule,
          onClose: () => {
            closeDialog(dialogId);
          },
          onSuccess: () => {
            this.doSearch();
            closeDialog(dialogId);
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
      DialogPlugin.confirm({
        title: "系统提示",
        content: `确认删除规则：${row.name}?`,
        onConfirm: () => {
          CodeRule.remove(row.id).then(() => {
            MessagePlugin.success("删除成功~");
            this.doSearch();
          })
        }
      })
    },
    trigger(row) {
      let systemDefault = !row.systemDefault;
      let documentType = row.documentType;
      DialogPlugin.confirm({
        title: "系统提示",
        content: `确认要「${systemDefault ? "启用" : "禁用"}」规则：${row.name}?`,
        onConfirm: () => {
          CodeRule.save({id: row.id, systemDefault: systemDefault, documentType: documentType}).then((success) => {
            console.log(success);
            MessagePlugin.success("操作成功~");
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

<style scoped>
.simple-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
  padding: 0 12px;
  box-sizing: border-box;
  overflow: hidden;
}

.simple-page__split {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 12px;
  overflow: hidden;
}

.simple-page__side {
  width: 260px;
  flex-shrink: 0;
  min-height: 0;
  overflow: hidden;
  padding: 8px 0;
}

.simple-page__side :deep(.t-table__header th),
.simple-page__side :deep(.t-table__body td) {
  padding-top: 4px;
  padding-bottom: 4px;
  line-height: 22px;
  height: auto;
}

.simple-page__main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.simple-page__toolbar {
  flex-shrink: 0;
  padding: 8px 0;
}

.simple-page__table {
  flex: 1 1 0;
  height: 0;
  min-height: 0;
  overflow: hidden;
}
</style>
