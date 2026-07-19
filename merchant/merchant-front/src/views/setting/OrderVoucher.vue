<template>
  <div class="simple-page">
    <div class="simple-page__split">
      <div class="simple-page__side">
        <t-table
            row-key="id"
            size="medium"
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
            <t-button variant="outline" style="border-radius: 4px" @click="showForm()">批量删除凭证</t-button>
            <t-button theme="primary" style="border-radius: 4px" :loading="loading" @click="doSearch">生成凭证</t-button>
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
              :selected-row-keys="selectedRowKeys"
              @select-change="onSelectChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import CodeRule from "@js/api/setting/CodeRule";
import {DialogPlugin, MessagePlugin} from "tdesign-vue-next";
import {openDialog, closeDialog} from '@common/dialog';
import {h} from "vue";
import VoucherForm from "@views/setting/VoucherForm.vue";

export default {
  name: "OrderVoucher",
  props: {
    merchant: Object,
  },
  components: {VoucherForm},
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
        {id: 17, documentType: '产品', type: 2},
        {id: 18, documentType: '仓库', type: 2},
        {id: 19, documentType: '客户', type: 2},
        {id: 20, documentType: '供货商', type: 2}
      ],
      selectedDocumentTypeKeys: [1],
      selectedRowKeys: [],
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
        {colKey: 'row-select', type: 'multiple', width: 46},
        {colKey: 'name', title: '日期', width: 200},
        {colKey: 'documentType', title: '单据编码', minWidth: 120},
        {colKey: 'format', title: '单据类型', minWidth: 120},
        {colKey: 'supplier', title: '供应商', width: 120, cell: (h, {row}) => row.serialNumberLength},
        {colKey: 'customer', title: '客户', width: 120, cell: (h, {row}) => row.serialNumberLength},
        {colKey: 'amount', title: '单据金额', width: 120, cell: (h, {row}) => row.serialNumberLength},
        {colKey: 'creator', title: '制单人', width: 120, cell: (h, {row}) => row.createdAt},
        {colKey: 'voucherNo', title: '凭证号', width: 120, cell: (h, {row}) => row.createdAt}
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
    onSelectChange(keys) {
      this.selectedRowKeys = keys;
      this.checkedRows = this.dataList.filter(item => keys.includes(item.id));
    },
    showForm() {
      let dialogId = openDialog({
        header: "规则编码",
        closeOnOverlayClick: false,
        width: '50vw',
        body: h(VoucherForm, {
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
