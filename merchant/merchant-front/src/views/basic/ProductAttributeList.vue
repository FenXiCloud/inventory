<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="openAdd">新 增</t-button>
        <t-button variant="outline" style="border-radius: 4px" :loading="loading" @click="loadList">刷新</t-button>
      </t-space>
    </div>

    <div class="simple-page__hint">辅助属性用于给商品附加颜色、尺码等多规格属性，当前仅做展示与筛选，不做 SKU 级库存区分。</div>

    <div class="simple-page__table">
      <t-table
          row-key="id"
          size="medium"
          bordered
          stripe
          hover
          height="100%"
          :data="dataList"
          :columns="columns"
          :loading="loading"
      >
        <template #values="{ row }">
          <t-space size="4" break-line>
            <t-tag v-for="(v, i) in (row.values || [])" :key="i" variant="light">{{ v }}</t-tag>
          </t-space>
        </template>
        <template #ops="{ row }">
          <t-space size="small">
            <t-link theme="primary" @click="openEdit(row)">编辑</t-link>
            <t-link theme="danger" @click="doRemove(row)">删除</t-link>
          </t-space>
        </template>
      </t-table>
    </div>

    <t-dialog
        v-model:visible="dialogVisible"
        :header="form.id ? '编辑辅助属性' : '新增辅助属性'"
        :confirm-loading="saving"
        @confirm="save"
    >
      <t-form label-width="80px" :colon="false">
        <t-form-item label="属性名">
          <t-input v-model="form.name" placeholder="如：颜色、尺码" style="width: 100%"/>
        </t-form-item>
        <t-form-item label="属性值">
          <t-textarea
              v-model="valuesText"
              placeholder="多个值用逗号或换行分隔，如：红,蓝,M,L"
              :autosize="{minRows: 3, maxRows: 6}"
          />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script>
import ProductAttribute from "@js/api/basic/ProductAttribute";
import {MessagePlugin, DialogPlugin} from "tdesign-vue-next";

export default {
  name: "ProductAttributeList",
  data() {
    return {
      loading: false,
      saving: false,
      dataList: [],
      dialogVisible: false,
      form: {},
      valuesText: '',
      columns: [
        {colKey: 'name', title: '属性名', width: 180},
        {colKey: 'values', title: '属性值', minWidth: 320},
        {colKey: 'ops', title: '操作', width: 120, align: 'center'},
      ],
    };
  },
  methods: {
    loadList() {
      this.loading = true;
      ProductAttribute.list()
        .then(({data}) => {
          this.dataList = data || [];
        })
        .finally(() => (this.loading = false));
    },
    openAdd() {
      this.form = {};
      this.valuesText = '';
      this.dialogVisible = true;
    },
    openEdit(row) {
      this.form = {...row};
      this.valuesText = (row.values || []).join(',');
      this.dialogVisible = true;
    },
    save() {
      if (!this.form.name || !this.form.name.trim()) {
        MessagePlugin.warning('请输入属性名');
        return;
      }
      const values = this.valuesText
        .split(/[,，\n]/)
        .map((s) => s.trim())
        .filter((s) => s);
      if (!values.length) {
        MessagePlugin.warning('请至少输入一个属性值');
        return;
      }
      this.saving = true;
      ProductAttribute.save({...this.form, name: this.form.name.trim(), values})
        .then(() => {
          MessagePlugin.success('保存成功');
          this.dialogVisible = false;
          this.loadList();
        })
        .finally(() => (this.saving = false));
    },
    doRemove(row) {
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认删除辅助属性「${row.name}」？`,
        onConfirm: () => {
          return ProductAttribute.remove(row.id).then(() => {
            MessagePlugin.success("删除成功");
            this.loadList();
          });
        }
      });
    },
  },
  created() {
    this.loadList();
  }
};
</script>
