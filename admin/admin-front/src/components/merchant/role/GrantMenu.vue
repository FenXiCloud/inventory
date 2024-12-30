<template>
  <div style="display: flex;flex-direction: column;height: calc(100vh - 55px)">
    <div class="flex-1">
      <vxe-table row-id="id"
                 height="auto"
                 @checkbox-change="checkBoxChange"
                 @checkbox-all="checkBoxChange"
                 :checkbox-config="{labelField: 'name',checkMethod:checkMethod,checkRowKeys:grantMenu}"
                 :tree-config="{transform:true, expandAll:true, rowField: 'id', parentField: 'parentId'}"
                 ref="table"
                 :data="dataList"
                 highlight-hover-row
                 :row-config="{useKey:true}"
                 :stripe="false"
                 :loading="loading">
        <vxe-column title="菜单分组" width="100" align="center" field="menuGroup" :formatter="({ cellValue })=>{return {MERCHANT:'集团菜单',STORE:'门店菜单'}[cellValue]}"/>
        <vxe-column type="checkbox" title="名称" field="name" tree-node/>
      </vxe-table>
    </div>
  </div>
</template>

<script>
import Role from "@js/api/Role";
import {message} from "heyui.ext";
import Menu from "@js/api/Menu";

/**
 * @功能描述: 角色授权
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "GrantMenu",
  props: {
    entity: Object,
    merchant: Object,
  },
  data() {
    return {
      loading: false,
      dataList: [],
      grantMenu: [],
    }
  },
  methods: {
    checkBoxChange({records}) {
      this.loading = true;
      let indeterminateRecords = this.$refs.table.getCheckboxIndeterminateRecords().map(val => val.id);
      let merchantMenu = records.map(val => val.id).concat(indeterminateRecords)

      this.loading = true;
      Role.roleGrant(this.entity.id, merchantMenu).then(() => {
        message("授权成功~");
      }).finally(() => this.loading = false);
    },
    loadData() {
      this.loading = true;
      Promise.all([
        Menu.merchantMenu(this.merchant.id),
        Role.getMenuRole(this.entity.id)
      ]).then((results) => {
        this.dataList = results[0].data || [];
        this.grantMenu = this.dataList.filter(val => !val.requireAuth).map(val => val.id);

        this.$nextTick(() => {
          if (results[1].data) {
            results[1].data.forEach(id => {
              let rowById = this.$refs.table.getRowById(id);
              if (!rowById.children || !rowById.children.length) {
                this.$refs.table.setCheckboxRow(rowById, true);
              }
            })
          }
        })
      }).finally(() => this.loading = false);
    },
    checkMethod({row}) {
      return row.requireAuth;
    }
  },
  created() {
    this.loadData();
  }
}
</script>

<style scoped lang="less">
.m-cus-menu {
  height: calc(100vh - 150px);
  overflow: hidden;
}
</style>
