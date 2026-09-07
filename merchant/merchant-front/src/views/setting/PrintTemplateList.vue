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

        <div class="print-tip">每个单据类型至少保留一个打印模板；系统预置的默认模板不可删除（可先新增/设置其他模板为默认后删除旧模板）。</div>
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
                  :title="row.systemDefault ? '当前为默认模板（每个单据类型至少保留一个默认模板）' : '点击设为默认模板'"
                  @click="triggerDefault(row)"
              >
                {{ row.systemDefault ? '默认' : '设为默认' }}
              </t-tag>
            </template>
            <template #createdAt="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
            <template #ops="{ row }">
              <t-space size="small">
                <t-link theme="primary" @click="doPreview(row)">预览</t-link>
                <t-link theme="primary" @click="showForm(row)">编辑</t-link>
                <template v-if="canRemove(row)">
                  <t-link theme="danger" @click="doRemove(row)">删除</t-link>
                </template>
                <template v-else>
                  <span class="print-op-disabled" :title="removeDisabledTip(row)">删除</span>
                </template>
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
import { MessagePlugin } from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import { openDialog, closeDialog } from '@common/dialog';
import { openTemplatePreview } from '@common/print';
import { h } from 'vue';

const DOCUMENT_TYPES = [
  '采购订单', '采购入库单', '采购退货单',
  '销售订单', '销售出库单', '销售退货单',
  '调拨单', '盘点单', '其他入库单', '其他出库单', '成本调整单',
  '收款单', '付款单', '核销单', '其他收款单', '其他付款单', '转帐单'
];
/** 打印模板列表 */
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
        { colKey: 'ops', title: '操作', width: 150, fixed: 'right', align: 'center' }
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
        width: '1080px',
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
    doPreview(row) {
      // 列表若未返回 content，则按 id 拉取完整模板后再预览
      if (row.content) {
        openTemplatePreview(row);
        return;
      }
      PrintTemplate.load(row.id).then(({ data }) => {
        openTemplatePreview(data || row);
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
    canRemove(row) {
      return !row.systemDefault && (this.dataList.length || 0) > 1;
    },
    removeDisabledTip(row) {
      return row.systemDefault
        ? '默认模板不允许删除，可先将其他模板设为默认后再删除'
        : '每个单据类型必须至少保留一个打印模板，不能删除最后一个模板';
    },
    doRemove(row) {
      if (!this.canRemove(row)) {
        MessagePlugin.warning(this.removeDisabledTip(row));
        return;
      }
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除打印模板「${row.name}」？删除后不可恢复。`,
        onConfirm: () => {
          return PrintTemplate.remove(row.id).then(() => {
            MessagePlugin.success('删除成功~');
            this.doSearch();
          }).catch(() => {});
        }
      });
    },
    triggerDefault(row) {
      const systemDefault = !row.systemDefault;
      // 取消默认：每个单据类型必须保留一个默认模板，若本行是当前唯一默认则不允许取消
      if (!systemDefault && !(this.dataList || []).some((item) => item.systemDefault && item.id !== row.id)) {
        MessagePlugin.warning('每个单据类型必须保留一个默认模板，请先将其他模板设为默认');
        return;
      }
      DialogPlugin.confirm({
        header: '系统提示',
        body: systemDefault
          ? `确认将「${row.name}」设为【${row.documentType}】的默认模板？`
          : `确认取消「${row.name}」的默认模板？`,
        onConfirm: () => {
          return PrintTemplate.save({
            id: row.id,
            name: row.name,
            documentType: row.documentType,
            systemDefault
          }).then(() => {
            MessagePlugin.success('操作成功~');
            this.loadList();
          }).catch(() => {});
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

.print-tip {
  flex-shrink: 0;
  font-size: 12px;
  color: #909399;
  padding: 2px 0 6px;
}

.print-op-disabled {
  color: #d0d0d0;
  cursor: not-allowed;
  font-size: 14px;
}

.simple-page__main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

</style>
