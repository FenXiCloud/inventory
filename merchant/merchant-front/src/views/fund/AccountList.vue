<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <Button @click="showForm()" color="primary">新 增</Button>
      </template>
      <template #tools>
        <Input id="name" v-model="params.filter" class="flex-1" placeholder="请输入名称"/>
        <Button color="primary" :loading="loading" @click="doSearch">查询</Button>
      </template>
    </vxe-toolbar>
    <div class="flex1">
      <vxe-table row-id="id"
                 ref="table"
                 :data="dataList"
                 highlight-hover-row
                 show-overflow
                 stripe
                 :row-config="{height: 48}"
                 :column-config="{resizable: true}"
                 :loading="loading">
        <vxe-column type="seq" width="40" title="#"/>
        <vxe-column title="账户类型" field="accountType" width="80"/>
        <vxe-column title="账户类型名称" field="accountTypeItem" width="100"/>
        <vxe-column title="名称" field="name"/>
        <vxe-column title="币别" field="currency" width="80"/>
        <vxe-column title="账户余额" field="balance" width="100"/>
        <vxe-column title="状态" field="enabled" width="80" align="center">
          <template #default="{row}">
            <Tag color="primary" @click="trigger(row)" v-if="row.enabled">启用</Tag>
            <Tag color="red" @click="trigger(row)" v-else>禁用</Tag>
          </template>
        </vxe-column>
        <vxe-column title="操作" align="center" width="150">
          <template #default="{row}">
            <i class="primary-color ml-10px" @click="addForm(row)">明细账</i>
            <i class="primary-color h-icon-edit ml-10px" @click="showForm(row)"></i>
            <i class="primary-color h-icon-trash ml-10px" @click="doRemove(row)"></i>
          </template>
        </vxe-column>
      </vxe-table>
    </div>
  </div>
</template>

<script>
import Account from "@js/api/fund/Account";
import AccountForm from "./AccountForm.vue";
import AccountFlowReport from "./AccountFlowReport.vue";
import {confirm, message} from "heyui.ext";
import {layer} from "@layui/layer-vue";
import {h} from "vue";
import Product from "@js/api/basic/Product";
import {mapMutations} from "vuex";

/**
 * @功能描述: 账户管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "AccountList",
  data() {
    return {
      loading: false,
      dataList: [],
      params: {
        filter: null,
      },
    }
  },
  methods: {
    ...mapMutations(['pushTab']),
    addForm(accountId) {
      this.pushTab({
        key: 'AccountFlowReport',
        title: '资金明细表',
        params: {accountId: accountId}
      });
    },
    trigger(row) {
      let enabled = !row.enabled;
      confirm({
        title: "系统提示",
        content: `确认要「${enabled ? "启用" : "禁用"}」名称：${row.name}?`,
        onConfirm: () => {
          Product.save({id: row.id, enabled}).then(() => {
            message("操作成功~");
            this.loadList();
          })
        }
      })
    },
    showForm(entity) {
      let type = 0;
      let layerId = layer.open({
        title: "账户信息",
        shadeClose: false,
        closeBtn: false,
        area: ['600px', '480px'],
        content: h(AccountForm, {
          entity, type,
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
      Account.list(this.params).then(({data}) => {
        this.dataList = data;
      }).finally(() => this.loading = false);
    },
    doRemove(row) {
      confirm({
        title: "系统提示",
        content: `确认删除：${row.name}?`,
        onConfirm: () => {
          Account.remove(row.id).then(() => {
            message("删除成功~");
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
