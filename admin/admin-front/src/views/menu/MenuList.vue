<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="showForm()">新 增</t-button>
        <t-button theme="default" variant="outline" style="border-radius: 4px" @click="expandAll">展开</t-button>
        <t-button theme="default" variant="outline" style="border-radius: 4px" @click="collapseAll">关闭</t-button>
        <t-select
            v-model="params.menuModule"
            :options="menuModules"
            clearable
            placeholder="菜单模块"
            style="width: 140px; border-radius: 4px"
        />
        <t-input
            v-model="params.name"
            clearable
            placeholder="请输入菜单名称"
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
      <t-enhanced-table
          ref="table"
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
          :tree="treeConfig"
      >
        <template #ops="{ row }">
          <t-space size="small">
            <t-link v-if="row.enabled" theme="primary" @click="showForm(null, row)">创建子节点</t-link>
            <t-link theme="primary" @click="showForm(row, null)"><t-icon name="edit"/></t-link>
            <t-link theme="primary" @click="doRemove(row)"><t-icon name="delete"/></t-link>
          </t-space>
        </template>
        <template #menuModule="{ row }">
          {{ menuModuleMap[row.menuModule] || row.menuModule || '' }}
        </template>
        <template #menuGroup="{ row }">
          {{ menuGroupMap[row.menuGroup] || row.menuGroup || '' }}
        </template>
        <template #requireAuth="{ row }">
          {{ row.requireAuth ? '是' : '否' }}
        </template>
        <template #menuType="{ row }">
          {{ menuTypeMap[row.menuType] || row.menuType || '' }}
        </template>
        <template #enabled="{ row }">
          <t-dropdown
              trigger="hover"
              :options="enabledOptions"
              @click="(data) => trigger(data.value, row)"
          >
            <t-tag :theme="row.enabled ? 'primary' : 'danger'" variant="light" style="cursor: pointer">
              {{ row.enabled ? '启用' : '禁用' }}
            </t-tag>
          </t-dropdown>
        </template>
      </t-enhanced-table>
    </div>
  </div>
</template>

<script>
import Menu from "@js/api/Menu";
import {DialogPlugin, MessagePlugin} from "tdesign-vue-next";
import MenuForm from "@/views/menu/MenuForm";
import {h} from "vue";
import {toArrayTree} from '@common/utils';

export default {
  name: "MenuList",
  components: {MenuForm},
  data() {
    return {
      loading: false,
      menuModules: [
        {label: '全部模块', value: ''},
        {label: '集团视角', value: 'MERCHANT'},
        {label: '客户视角', value: 'CUSTOM'},
        {label: '供货商视角', value: 'SUPPLIER'}
      ],
      menuModuleMap: {MERCHANT: '集团视角', CUSTOM: '客户视角', SUPPLIER: '货商视角'},
      menuGroupMap: {MERCHANT: '集团菜单', STORE: '门店菜单'},
      menuTypeMap: {MENU: '菜单', FUNCTION: '功能'},
      enabledOptions: [
        {content: '启用', value: 'enabled'},
        {content: '禁用', value: 'disabled'}
      ],
      params: {
        name: null,
        menuModule: '',
      },
      dataList: [],
      treeConfig: {
        childrenKey: 'children',
        treeNodeColumnIndex: 3,
        defaultExpandAll: true,
        indent: 24
      },
      columns: [
        {colKey: 'ops', title: '操作', width: 180, fixed: 'left', align: 'center'},
        {colKey: 'menuModule', title: '菜单模块', width: 100, align: 'center'},
        {colKey: 'menuGroup', title: '菜单分组', width: 100, align: 'center'},
        {colKey: 'name', title: '名称', minWidth: 160, ellipsis: true},
        {colKey: 'component', title: '组件', width: 180, ellipsis: true},
        {colKey: 'iconCls', title: '图标', width: 100},
        {colKey: 'requireAuth', title: '权限控制', width: 100, align: 'center'},
        {colKey: 'menuType', title: '类型', width: 90, align: 'center'},
        {colKey: 'pos', title: '位置', width: 70, align: 'center'},
        {colKey: 'enabled', title: '状态', width: 90, align: 'center', fixed: 'right'}
      ]
    }
  },
  methods: {
    showForm(menu, parent) {
      const dialog = DialogPlugin({
        header: "菜单信息",
        closeOnOverlayClick: false,
        closeBtn: false,
        footer: false,
        width: '600px',
        body: h(MenuForm, {
          menu, parent,
          onClose: () => dialog.hide(),
          onSuccess: () => {
            this.loadList();
            dialog.hide();
          }
        })
      });
    },
    buildTree(list) {
      const flat = Array.isArray(list) ? list : [];
      return toArrayTree(flat, {key: 'id', parentKey: 'parentId', children: 'children'});
    },
    loadList() {
      this.loading = true;
      Menu.list(this.params).then(({data}) => {
        this.dataList = this.buildTree(data);
        this.$nextTick(() => {
          this.expandAll();
        });
      }).finally(() => this.loading = false);
    },
    expandAll() {
      this.$refs.table?.expandAll?.();
    },
    collapseAll() {
      this.$refs.table?.foldAll?.();
    },
    doSearch() {
      this.loadList();
    },
    doRemove(row) {
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认删除：${row.name}?`,
        onConfirm: () => {
          Menu.remove(row.id).then(() => {
            MessagePlugin.success("删除成功~");
            this.loadList();
          })
        }
      })
    },
    trigger(code, row) {
      let enabled = code === 'enabled';
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认要「${enabled ? "启用" : "禁用"}」：${row.name}?`,
        onConfirm: () => {
          Menu.save({id: row.id, enabled}).then(() => {
            MessagePlugin.success("操作成功~");
            this.loadList();
          })
        }
      })
    },
  },
  created() {
    this.loadList();
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
