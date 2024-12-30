<template>
  <div class="frame-page" style="margin: 0">
    <div class="h-panel">
      <div class="h-panel-body">
        <div class="table-toolbar">
          <div class="table-toolbar-left">
            <Select v-model="params.menuModule" :datas="menuModules" :deletable="false"></Select>
            <Input id="name" v-model="params.name" class="flex-1" placeholder="请输入菜单名称"/>
            <Button color="primary" :loading="loading" @click="doSearch">查询</Button>
          </div>
          <div class="table-toolbar-right">
            <Button @click="showForm()" color="primary">添加</Button>
            <Button @click="$refs.table.setAllTreeExpand(true)">展开</Button>
            <Button @click="$refs.table.clearTreeExpand()">关闭</Button>
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
              <DropdownMenu
                  trigger="hover"
                  class="primary-color ml-8px  text-hover"
                  :datas="param"
                  @clickItem="trigger($event,row)">
                <Tag color="primary" v-if="row.enabled">启用</Tag>
                <Tag color="red" v-else>禁用</Tag>
              </DropdownMenu>
            </template>
          </vxe-column>
          <vxe-column title="操作" align="center" width="170">
            <template #default="{row}">
              <div class="flex items-center justify-center">
                <span class=" primary-color text-hover ml-10px" v-if="row.enabled" @click="showForm(null,row)" size="s">创建子节点</span>
                <span class=" primary-color text-hover ml-10px" @click="showForm(row,null)" size="s">编辑</span>
                <span class="primary-color ml-10px text-hover" @click="doRemove(row)" size="s">删除</span>
              </div>
            </template>
          </vxe-column>
        </vxe-table>
      </div>
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
import {confirm, message} from "heyui.ext";
import MenuForm from "@components/menu/MenuForm";
import {layer} from "@layui/layer-vue";
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
      menuModules: [{title: '全部模块', key: '',},
        {title: '集团视角', key: 'MERCHANT'}, {title: '客户视角', key: 'CUSTOM'}, {
          title: '供货商视角',
          key: 'SUPPLIER'
        }],
      params: {
        name: null,
        menuModule: '',
      },
      checkedRows: [],
      dataList: [],
      param: [
        {title: '启用', key: 'enabled'},
        {title: '禁用', key: 'disabled'},
      ]
    }
  },
  methods: {
    showForm(menu, parent) {
      let layerId = layer.open({
        title: "菜单信息",
        shadeClose: false,
        closeBtn: false,
        area: ['600px', '400px'],
        content: h(MenuForm, {
          menu, parent,
          onClose: () => {
            layer.close(layerId);
          },
          onSuccess: () => {
            this.loadList();
            layer.close(layerId);
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
      confirm({
        title: "系统提示",
        content: `确认删除：${row.name}?`,
        onConfirm: () => {
          Menu.remove(row.id).then(() => {
            message("删除成功~");
            this.loadList();
          })
        }
      })
    },
    trigger(code, row) {
      let enabled = code === 'enabled';
      confirm({
        title: "系统提示",
        content: `确认要「${enabled ? "启用" : "禁用"}」：${row.name}?`,
        onConfirm: () => {
          Menu.save({id: row.id, enabled}).then(() => {
            message("操作成功~");
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

</style>
