<template>
  <div class="service-items">
    <div class="toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius:4px" @click="openAdd">新 增</t-button>
        <t-button style="border-radius:4px" :loading="syncing" @click="doSync">税局同步</t-button>
        <t-input
            v-model="keyword"
            clearable
            placeholder="请输入开票项目"
            style="width:240px;border-radius:4px"
            @enter="search"
        >
          <template #suffixIcon><t-icon name="search" style="cursor:pointer" @click="search"/></template>
        </t-input>
        <t-button theme="primary" variant="outline" style="border-radius:4px" :loading="loading" @click="search">查询</t-button>
      </t-space>
    </div>

    <div class="table-wrap">
      <t-table
          row-key="id"
          size="medium"
          bordered
          stripe
          hover
          height="100%"
          :data="list"
          :columns="columns"
          :loading="loading"
      >
        <template #isDefault="{ row }">
          <t-tag :theme="row.isDefault ? 'primary' : 'default'" variant="light">{{ row.isDefault ? '是' : '否' }}</t-tag>
        </template>
        <template #taxPreference="{ row }">
          <t-tag :theme="row.taxPreference ? 'primary' : 'default'" variant="light">{{ row.taxPreference ? '是' : '否' }}</t-tag>
        </template>
        <template #taxRateLabel="{ row }">
          <span>{{ row.taxRateLabel || '—' }}</span>
        </template>
        <template #ops="{ row }">
          <t-space size="small">
            <t-link theme="primary" @click="openEdit(row)"><t-icon name="edit"/></t-link>
            <t-link theme="danger" @click="remove(row)"><t-icon name="delete"/></t-link>
          </t-space>
        </template>
      </t-table>
    </div>

    <div class="pager">
      <t-pagination
          v-model:current="page"
          v-model:page-size="pageSize"
          :total="total"
          :show-jumper="true"
          :show-page-size="true"
          @change="load"
      />
    </div>

    <t-dialog
        v-model:visible="dialog.visible"
        :header="dialog.isEdit ? '编辑开票项目' : '新增开票项目'"
        width="520px"
        :footer="false"
        :close-on-overlay-click="false"
    >
      <t-form label-width="120px" :colon="false">
        <t-form-item label="开票项目名称">
          <t-input v-model="dialog.form.goodsName" placeholder="如：*技术服务*信息技术服务费"/>
        </t-form-item>
        <t-form-item label="税收分类编码">
          <t-input v-model="dialog.form.goodsCode" placeholder="19位税收分类编码" maxlength="19"/>
        </t-form-item>
        <t-form-item label="规格型号">
          <t-input v-model="dialog.form.spec"/>
        </t-form-item>
        <t-form-item label="计量单位">
          <t-input v-model="dialog.form.unit"/>
        </t-form-item>
        <t-form-item label="税率">
          <t-select v-model="dialog.form.taxRateLabel" :options="taxRateOptions"/>
        </t-form-item>
        <t-form-item label="是否默认">
          <t-switch v-model="dialog.form.isDefault"/>
        </t-form-item>
        <t-form-item label="享受税收优惠">
          <t-switch v-model="dialog.form.taxPreference"/>
        </t-form-item>
      </t-form>
      <div class="dialog-footer">
        <t-button @click="dialog.visible = false">取消</t-button>
        <t-button theme="primary" :loading="dialog.loading" @click="submit">保存</t-button>
      </div>
    </t-dialog>
  </div>
</template>

<script>
import Goods from '@js/api/invoice/Goods';
import Invoice from '@js/api/invoice/Invoice';
import {MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';

export default {
  name: 'ServiceItems',
  data() {
    return {
      loading: false,
      syncing: false,
      keyword: '',
      page: 1,
      pageSize: 20,
      total: 0,
      list: [],
      taxRateOptions: [
        {label: '0%', value: '0%'},
        {label: '1%', value: '1%'},
        {label: '3%', value: '3%'},
        {label: '6%', value: '6%'},
        {label: '9%', value: '9%'},
        {label: '13%', value: '13%'},
        {label: '免税', value: '免税'}
      ],
      columns: [
        {colKey: 'goodsName', title: '开票项目', minWidth: 220, ellipsis: true},
        {colKey: 'goodsCode', title: '税收分类编码', minWidth: 200, ellipsis: true},
        {colKey: 'spec', title: '规格型号', width: 160},
        {colKey: 'unit', title: '计量单位', width: 100},
        {colKey: 'isDefault', title: '是否默认', width: 100, align: 'center'},
        {colKey: 'taxPreference', title: '税收优惠', width: 110, align: 'center'},
        {colKey: 'taxRateLabel', title: '税率', width: 90, align: 'center'},
        {colKey: 'ops', title: '操作', width: 100, align: 'center', fixed: 'right'}
      ],
      dialog: {
        visible: false,
        isEdit: false,
        loading: false,
        editId: null,
        form: {goodsName: '', goodsCode: '', spec: '', unit: '', taxRateLabel: '3%', isDefault: false, taxPreference: false}
      }
    };
  },
  methods: {
    search() {
      this.page = 1;
      this.load();
    },
    load() {
      this.loading = true;
      Goods.list({keyword: this.keyword || null, page: this.page - 1, size: this.pageSize})
        .then(({data}) => {
          this.list = (data && data.content) || [];
          this.total = (data && data.total) || 0;
        })
        .catch(() => {
          this.list = [];
          this.total = 0;
        })
        .finally(() => (this.loading = false));
    },
    doSync() {
      this.syncing = true;
      const keywords = ['技术服务', '软件开发', '咨询', '租赁', '维护', '培训', '设计', '广告'];
      let added = 0;
      keywords.reduce((p, kw) => p.then(() =>
        Invoice.searchGoodsTax(kw)
          .then(({data}) => {
            if (!data || !data.goodsCode) return;
            const exists = this.list.find((x) => x.goodsCode === data.goodsCode);
            if (exists) return;
            return Goods.create({
              goodsName: `${data.goodsShortName || ''}${kw}`,
              goodsCode: data.goodsCode,
              spec: '',
              unit: '',
              taxRateLabel: data.taxRate ? `${Math.round(data.taxRate * 100)}%` : '3%',
              isDefault: false,
              taxPreference: false
            }).then(() => {
              added++;
            });
          })
          .catch(() => {})
      ), Promise.resolve()).finally(() => {
        this.syncing = false;
        if (added > 0) {
          MessagePlugin.success(`同步完成，新增 ${added} 条`);
          this.load();
        } else {
          MessagePlugin.info('没有新的项目可同步');
        }
      });
    },
    openAdd() {
      this.dialog.isEdit = false;
      this.dialog.editId = null;
      this.dialog.form = {goodsName: '', goodsCode: '', spec: '', unit: '', taxRateLabel: '3%', isDefault: false, taxPreference: false};
      this.dialog.visible = true;
    },
    openEdit(row) {
      this.dialog.isEdit = true;
      this.dialog.editId = row.id;
      this.dialog.form = {
        goodsName: row.goodsName,
        goodsCode: row.goodsCode,
        spec: row.spec || '',
        unit: row.unit || '',
        taxRateLabel: row.taxRateLabel || '3%',
        isDefault: !!row.isDefault,
        taxPreference: !!row.taxPreference
      };
      this.dialog.visible = true;
    },
    submit() {
      const f = this.dialog.form;
      if (!f.goodsName || !f.goodsName.trim()) {
        MessagePlugin.warning('请填写开票项目名称');
        return;
      }
      if (!f.goodsCode || !f.goodsCode.trim()) {
        MessagePlugin.warning('请填写税收分类编码');
        return;
      }
      this.dialog.loading = true;
      const payload = {
        goodsName: f.goodsName.trim(),
        goodsCode: f.goodsCode.trim(),
        spec: f.spec,
        unit: f.unit,
        taxRateLabel: f.taxRateLabel,
        isDefault: f.isDefault,
        taxPreference: f.taxPreference
      };
      const req = this.dialog.isEdit ? Goods.update(this.dialog.editId, payload) : Goods.create(payload);
      req
        .then(() => {
          MessagePlugin.success(this.dialog.isEdit ? '编辑成功' : '新增成功');
          this.dialog.visible = false;
          this.load();
        })
        .finally(() => (this.dialog.loading = false));
    },
    remove(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除「${row.goodsName}」？`,
        onConfirm: () => {
          Goods.remove(row.id).then(() => {
            MessagePlugin.success('删除成功');
            this.load();
          });
        }
      });
    }
  },
  created() {
    this.load();
  }
};
</script>

<style scoped>
.service-items {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
  padding: 0 12px;
  box-sizing: border-box;
}

.toolbar {
  flex-shrink: 0;
  padding: 8px 0;
}

.table-wrap {
  flex: 1;
  height: 0;
  min-height: 0;
  overflow: auto;
}

.pager {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding: 10px 0;
  border-top: 1px solid var(--td-component-border, #dcdcdc);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}
</style>
