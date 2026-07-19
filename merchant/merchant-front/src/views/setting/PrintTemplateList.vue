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
            <t-button theme="primary" style="border-radius: 4px" @click="showForm()">新 增</t-button>
            <t-input
                v-model="params.name"
                clearable
                placeholder="请输入模板名称"
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
              table-layout="fixed"
              :data="dataList"
              :columns="columns"
              :loading="loading"
          >
            <template #systemDefault="{ row }">
              <t-tag
                  :theme="row.systemDefault ? 'success' : 'warning'"
                  variant="light"
                  style="cursor:pointer"
                  @click="triggerDefault(row)"
              >
                {{ row.systemDefault ? '默认' : '否' }}
              </t-tag>
            </template>
            <template #createdAt="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
            <template #ops="{ row }">
              <t-space size="small">
                <t-link theme="primary" @click="showForm(row)">编辑</t-link>
                <t-link theme="danger" @click="doRemove(row)">删除</t-link>
              </t-space>
            </template>
          </t-table>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import PrintTemplate from '@js/api/setting/PrintTemplate';
import PrintTemplateForm from './PrintTemplateForm.vue';
import { DialogPlugin, MessagePlugin } from 'tdesign-vue-next';
import { openDialog, closeDialog } from '@common/dialog';
import { h } from 'vue';

const DOCUMENT_TYPES = [
  '采购订单', '采购入库单', '采购退货单',
  '销售订单', '销售出库单', '销售退货单',
  '调拨单', '盘点单', '其他入库单', '其他出库单', '成本调整单',
  '收款单', '付款单', '核销单', '其他收款单', '其他付款单', '转帐单'
];

/**
 * @功能描述: 打印模板列表
 */
export default {
  name: 'PrintTemplateList',
  components: { PrintTemplateForm },
  data() {
    return {
      loading: false,
      params: {
        name: null,
        documentType: DOCUMENT_TYPES[0]
      },
      dataList: [],
      selectedDocumentTypeKeys: [1],
      documentTypeDataList: DOCUMENT_TYPES.map((documentType, index) => ({
        id: index + 1,
        documentType
      })),
      documentTypeColumns: [
        { colKey: 'row-select', type: 'single', width: 46 },
        { colKey: 'documentType', title: '单据类型', minWidth: 120, ellipsis: true }
      ],
      columns: [
        { colKey: 'name', title: '模板名称', minWidth: 180, ellipsis: true },
        { colKey: 'documentType', title: '单据类型', width: 140, align: 'center' },
        { colKey: 'systemDefault', title: '默认模板', width: 100, align: 'center' },
        { colKey: 'createdAt', title: '创建时间', width: 180, align: 'center' },
        { colKey: 'ops', title: '操作', width: 120, fixed: 'right', align: 'center' }
      ]
    };
  },
  computed: {
    queryParams() {
      return { ...this.params };
    },
    currentDocumentType() {
      const row = this.documentTypeDataList.find((item) => this.selectedDocumentTypeKeys.includes(item.id));
      return row ? row.documentType : this.params.documentType;
    }
  },
  methods: {
    formatTime(value) {
      if (!value) return '-';
      return String(value).replace('T', ' ').substring(0, 19);
    },
    onDocumentTypeSelect(keys) {
      if (!keys || !keys.length) return;
      this.selectedDocumentTypeKeys = keys.slice(0, 1);
      const row = this.documentTypeDataList.find((item) => item.id === keys[0]);
      if (row) {
        this.params.documentType = row.documentType;
        this.loadList();
      }
    },
    onDocumentTypeRowClick({ row }) {
      this.onDocumentTypeSelect([row.id]);
    },
    showForm(entity) {
      const printTemplate = entity || { documentType: this.currentDocumentType, systemDefault: false };
      const dialogId = openDialog({
        header: entity ? '编辑打印模板' : '新增打印模板',
        closeOnOverlayClick: false,
        width: '520px',
        body: h(PrintTemplateForm, {
          printTemplate,
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.doSearch();
            closeDialog(dialogId);
          }
        })
      });
    },
    loadList() {
      this.loading = true;
      PrintTemplate.list(this.queryParams)
        .then(({ data }) => {
          this.dataList = data || [];
        })
        .finally(() => (this.loading = false));
    },
    doSearch() {
      this.loadList();
    },
    doRemove(row) {
      DialogPlugin.confirm({
        title: '系统提示',
        content: row.systemDefault
          ? `「${row.name}」当前为默认模板，确认删除？`
          : `确认删除打印模板「${row.name}」？`,
        onConfirm: () => {
          PrintTemplate.remove(row.id).then(() => {
            MessagePlugin.success('删除成功~');
            this.doSearch();
          });
        }
      });
    },
    triggerDefault(row) {
      const systemDefault = !row.systemDefault;
      DialogPlugin.confirm({
        title: '系统提示',
        content: systemDefault
          ? `确认将「${row.name}」设为【${row.documentType}】的默认模板？`
          : `确认取消「${row.name}」的默认模板？`,
        onConfirm: () => {
          PrintTemplate.save({
            id: row.id,
            name: row.name,
            documentType: row.documentType,
            systemDefault
          }).then(() => {
            MessagePlugin.success('操作成功~');
            this.loadList();
          });
        }
      });
    }
  },
  created() {
    this.doSearch();
  }
};
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
