<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="showUnitForm()">新 增</t-button>
        <t-input
            v-model="params.name"
            clearable
            placeholder="请输入单位名称"
            style="width: 240px; border-radius: 4px"
            @enter="searchUnit"
        >
          <template #suffixIcon>
            <t-icon name="search" style="cursor:pointer" @click="searchUnit"/>
          </template>
        </t-input>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="searchUnit">查询</t-button>
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
          :data="unitDataList"
          :columns="columns"
          :loading="loading"
      >
        <template #ops="{ row }">
          <t-space size="small">
            <t-link theme="primary" @click="showUnitForm(row)"><t-icon name="edit"/></t-link>
            <t-link theme="primary" @click="deleteUnit(row)"><t-icon name="delete"/></t-link>
          </t-space>
        </template>
      </t-table>
    </div>
  </div>
</template>

<script>
import Unit from '@js/api/basic/Unit';
import UnitForm from '@views/basic/UnitForm.vue';
import {MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import {openDialog, closeDialog} from '@common/dialog';
import {h} from 'vue';

/**
 * @功能描述: 单位列表
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: 'UnitList',
  data() {
    return {
      loading: false,
      unitDataList: [],
      params: {name: ''},
      columns: [
        {colKey: 'ops', title: '操作', width: 90, fixed: 'left', align: 'center'},
        {colKey: 'name', title: '名称', minWidth: 200, ellipsis: true}
      ]
    };
  },
  methods: {
    showUnitForm(entity) {
      const dialogId = openDialog({
        header: '单位信息',
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '400px',
        body: h(UnitForm, {
          entity: entity || null,
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.searchUnit();
            closeDialog(dialogId);
          }
        })
      });
    },
    searchUnit() {
      this.loadUnit();
    },
    loadUnit() {
      this.loading = true;
      const query = {};
      if (this.params.name) query.name = this.params.name;
      Unit.list(query)
        .then(({data}) => {
          this.unitDataList = Array.isArray(data) ? data : [];
        })
        .finally(() => (this.loading = false));
    },
    deleteUnit(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除单位：${row.name}?`,
        onConfirm: () => {
          Unit.delete(row.id).then(() => {
            MessagePlugin.success('删除成功~');
            this.loadUnit();
          });
        }
      });
    }
  },
  created() {
    this.loadUnit();
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
