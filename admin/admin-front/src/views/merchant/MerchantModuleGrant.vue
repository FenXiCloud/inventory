<template>
  <vxe-table row-id="id"
             height="auto"
             @checkbox-change="checkBoxChange"
             @checkbox-all="checkBoxChange"
             :checkbox-config="{labelField: 'name',checkMethod:checkMethod, checkRowKeys: grantMenu}"
             :tree-config="{transform:true,expandAll:true,rowField: 'id', parentField: 'parentId'}"
             ref="table"
             :data="dataList"
             highlight-hover-row
             :row-config="{useKey:true}"
             :stripe="false"
             :loading="loading">
    <vxe-column title="菜单模块" width="100" align="center" field="menuModule" :formatter="({ cellValue })=>{return {MERCHANT:'集团视角',CUSTOM:'客户视角',SUPPLIER:'货商视角'}[cellValue]}"/>
    <vxe-column title="菜单分组" width="100" align="center" field="menuGroup" :formatter="({ cellValue })=>{return {MERCHANT:'集团菜单',STORE:'门店菜单'}[cellValue]}"/>
    <vxe-column type="checkbox" title="名称" field="name" tree-node/>
  </vxe-table>
</template>

<script>
/**
 * <p>****************************************************************************</p>
 * <p><b>Copyright © 2010-2022 soho team All Rights Reserved<b></p>
 * <ul style="margin:15px;">
 * <li>Description : </li>
 * <li>Version     : 1.0</li>
 * <li>Creation    : 2022年03月11日</li>
 * <li>@author     : ____′↘夏悸</li>
 * </ul>
 * <p>****************************************************************************</p>
 */
import Menu from "@js/api/Menu";
import {message} from "heyui.ext";

export default {
  name: "MerchantModuleGrant",
  props: {
    merchant: Object,
  },
  data() {
    return {
      showForm: false,
      grantMerchantForm: false,
      loading: false,
      grantMenu: [],
      checkBoxList: [],
      params: {
        enabled: true,
      },
      checkedRows: [],
      dataList: [],
    }
  },
  watch: {
    'params.merchantId'() {
      this.loadList();
    }
  },
  methods: {
    checkBoxChange({records}) {
      this.checkRequire();
      this.loading = true;
      let indeterminateRecords = this.$refs.table.getCheckboxIndeterminateRecords().map(val => val.id);
      let menus = this.$refs.table.getCheckboxRecords().map(val => val.id).concat(indeterminateRecords);
      let merchantMenu = {menus: menus, merchants: [this.merchant.id]};

      Menu.grantMerchant(merchantMenu).then(() => {
        message("授权成功~");
      }).finally(() => this.loading = false);
    },
    checkRequire() {
      this.grantMenu.forEach(id => {
        let rowById = this.$refs.table.getRowById(id);
        if (!rowById.children || !rowById.children.length) {
          this.$refs.table.setCheckboxRow(rowById, true);
        }
      })
    },
    loadList() {
      this.loading = true;
      Promise.all([
        Menu.list(this.params),
        Menu.queryGrantMenu(this.merchant.id)
      ]).then((results) => {
        this.dataList = results[0].data
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
    this.loadList();
  }
}
</script>
