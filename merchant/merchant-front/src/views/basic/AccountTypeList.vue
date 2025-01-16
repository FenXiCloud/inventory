<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <Button class="mr-20px" @click="showForm()" color="primary">新 增</Button>
        类型： <Select v-model="params.costType" :datas="costTypes"/>
      </template>
      <template #tools>
        <Input id="name" v-model="params.name" class="flex-1" placeholder="请输入名称"/>
        <Button color="primary" :loading="loading" @click="doSearch">查询</Button>
      </template>
    </vxe-toolbar>
    <div class="flex1">
      <vxe-table row-id="id"
                 ref="table"
                 :data="dataList"
                 :tree-config="{transform:true, rowField: 'id', parentField: 'pid'}"
                 highlight-hover-row
                 show-overflow
                 stripe
                 :row-config="{height: 48}"
                 :column-config="{resizable: true}"
                 :loading="loading">
        <vxe-column type="seq" width="40" title="#"/>
        <vxe-column title="收支类型" field="costType" width="120"/>
        <vxe-column title="名称" field="name" tree-node/>
        <vxe-column title="状态" field="enabled" width="120" align="center">
          <template #default="{row}">
            <Tag color="primary" @click="trigger(row)"  v-if="row.enabled">启用</Tag>
            <Tag color="red" @click="trigger(row)"  v-else>禁用</Tag>
          </template>
        </vxe-column>
        <vxe-column title="操作" align="center" width="150">
          <template #default="{row}">
            <span class=" primary-color text-hover ml-10px" @click="showForm(null,row)" size="s">创建下级</span>
            <i class="primary-color h-icon-edit ml-10px" @click="showForm(row)"></i>
            <i class="primary-color h-icon-trash ml-10px" @click="doRemove(row)"></i>
          </template>
        </vxe-column>
      </vxe-table>
    </div>
  </div>
</template>

<script>
import AccountType from "@js/api/basic/AccountType";
import AccountTypeForm from "./AccountTypeForm.vue";
import {confirm, message} from "heyui.ext";
import {layer} from "@layui/layer-vue";
import {h} from "vue";

/**
 * @功能描述: 账户收支类别
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "AccountTypeList",
  data() {
    return {
      loading: false,
      dataList: [],
      params: {
        name: null,
        costType: '收入类型',
      },
      costTypes: [
        {title: '收入类型', key: '收入类型'},
        {title: '支出类型', key: '支出类型'},
      ],
      param: [
        {title: '启用', key: 'enabled'},
        {title: '禁用', key: 'disabled'},
      ]
    }
  },
  methods: {
    showForm(entity, parent) {
      let layerId = layer.open({
        title: "收入类别信息",
        shadeClose: false,
        area: ['450px', '420px'],
        content: h(AccountTypeForm, {
          entity, parent, list: this.dataList,
          onClose: () => {
            layer.close(layerId);
          },
          onSuccess: () => {
            this.doSearch();
            layer.close(layerId);
          }
        })
      });
    },
    doSearch() {
      this.loadList();
    },
    loadList() {
      this.loading = true;
      AccountType.list(this.params).then(({data}) => {
        this.dataList = data;
      }).finally(() => this.loading = false);
    },
    doRemove(row) {
      confirm({
        title: "系统提示",
        content: `确认删除：${row.name}?`,
        onConfirm: () => {
          AccountType.remove(row.id).then(() => {
            message("删除成功~");
            this.loadList();
          })
        }
      })
    },
    trigger(row) {
      let enabled = !row.enabled;
      confirm({
        title: "系统提示",
        content: `确认要「${enabled ? "启用" : "禁用"}」名称：${row.name}?`,
        onConfirm: () => {
          AccountType.save({id: row.id, enabled}).then(() => {
            message("操作成功~");
            this.loadList();
          })
        }
      })
    }
  },
  created() {
    this.loadList();
  }
}
</script>
