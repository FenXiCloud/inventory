<template>
  <div class="frame-page" style="margin: 0">
    <div class="t-panel p-16px">
      <div class="toolbar">
        <div class="toolbar-left">
          <t-select v-model="params.menuModule" :options="menuModules" clearable class="mr-8px"></t-select>
          <t-input id="name" v-model="params.name" class="flex-1" placeholder="请输入菜单名称"/>
          <t-button theme="primary" :loading="loading" @click="doSearch">查询</t-button>
        </div>
        <div class="toolbar-right">
          <t-button @click="showForm()" theme="primary">添加</t-button>
          <t-button @click="$refs.table.setAllTreeExpand(true)">展开</t-button>
          <t-button @click="$refs.table.clearTreeExpand()">关闭</t-button>
        </div>
      </div>
      <vxe-table row-id="id"
                 :stripe="false"
                 :tree-config="{transform:true, rowField: 'id', parentField: 'parentId'}"
                 ref="table"
                 :data="dataList"
                 :row-config="{height: 48}"
                 highlight-hover-row
                 show-overflow
                 :loading="loading">
        <vxe-column title="菜单模块" width="80" align="center" field="menuModule"
                    :formatter="({ cellValue })=>{return {MERCHANT:'集团视角',CUSTOM:'客户视角',SUPPLIER:'货商视角'}[cellValue]}"/>
        <vxe-column title="菜单分组" width="80" align="center" field="menuGroup"
                    :formatter="({ cellValue })=>{return {MERCHANT:'集团菜单',STORE:'门店菜单'}[cellValue]}"/>
        <vxe-column title="名称" field="name" tree-node/>
        <vxe-column title="组件" field="component" width="180"/>
        <vxe-column title="图标" field="iconCls" width="100"/>
        <vxe-column title="权限控制" field="requireAuth" align="center" width="100"
                    :formatter="({ cellValue })=> cellValue?'是':'否'"/>
        <vxe-column title="类型" field="menuType" width="100" align="center"
                    :formatter="({ cellValue })=>{return {MENU:'菜单',FUNCTION:'功能'}[cellValue]}"/>
        <vxe-column title="位置" field="pos" width="60" align="center"/>
        <vxe-column title="状态" field="enabled" width="100" align="center">
          <template #default="{row}">
            <t-dropdown trigger="hover">
              <t-tag theme="primary" v-if="row.enabled" class="cursor-pointer">启用</t-tag>
              <t-tag theme="danger" v-else class="cursor-pointer">禁用</t-tag>
              <t-dropdown-menu slot="dropdown">
                <t-dropdown-item value="enabled" @click="trigger('enabled', row)">启用</t-dropdown-item>
                <t-dropdown-item value="disabled" @click="trigger('disabled', row)">禁用</t-dropdown-item>
              </t-dropdown-menu>
            </t-dropdown>
          </template>
        </vxe-column>
        <vxe-column title="操作" align="center" width="170">
          <template #default="{row}">
            <div class="flex items-center justify-center">
              <span class=" primary-color text-hover ml-10px" v-if="row.enabled" @click="showForm(null,row)">创建子节点</span>
              <span class=" primary-color text-hover ml-10px" @click="showForm(row,null)">编辑</span>
              <span class="primary-color ml-10px text-hover" @click="doRemove(row)">删除</span>
            </div>
          </template>
        </vxe-column>
      </vxe-table>
    </div>
  </div>
</template>

<script>
/**
 * @功能描述: 菜单列表
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import Menu from "@js/api/Menu";
import {DialogPlugin, MessagePlugin} from "tdesign-vue-next";
import MenuForm from "@/views/menu/MenuForm";
import {h} from "vue";

export default {
  name: "MenuList",
  components: {MenuForm},
  data() {
    return {
      grantMerchantForm: false,
      loading: false,
      merchantList: [],
      checkBoxList: [],
      merchant: null,
      menuModules: [{label: '全部模块', value: ''},
        {label: '集团视角', value: 'MERCHANT'}, {label: '客户视角', value: 'CUSTOM'}, {
          label: '供货商视角',
          value: 'SUPPLIER'
        }],
      params: {
        name: null,
        menuModule: '',
      },
      checkedRows: [],
      dataList: [],
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
    loadList() {
      this.loading = true;
      Menu.list(this.params).then(({data}) => {
        this.dataList = data;
        this.$nextTick(() => {
          this.$refs.table.setAllTreeExpand(true)
        })
      }).finally(() => this.loading = false);
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
