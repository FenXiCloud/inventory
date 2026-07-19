<template>
  <div class="frame-page" style="margin: 0">
    <div class="t-panel p-16px">
      <div class="toolbar">
        <div class="toolbar-left">
          <label for="name" class="mr-10px">商户名称</label>
          <t-input id="name" v-model="params.name" class="flex-1" placeholder="请输入商户名称"/>
          <t-button theme="primary" :loading="loading" @click="doSearch">查询</t-button>
        </div>
        <div class="toolbar-right">
          <t-button @click="showForm()" theme="primary">添加</t-button>
        </div>
      </div>
      <vxe-table row-id="id"
                 ref="table"
                 :data="dataList"
                 highlight-hover-row
                 show-overflow
                 :row-config="{height: 48}"
                 :loading="loading">
        <vxe-column field="id" width="60" title="ID"/>
        <vxe-column title="商户编码" field="code" width="280"/>
        <vxe-column title="商户名称" field="name" >
          <template #default="{row}">
            <span @click="showForm(row)" class="text-hover primary-color"><t-icon name="edit" /> {{ row.name }}</span>
          </template>
        </vxe-column>
        <vxe-column title="联系人" field="contact" width="100"/>
        <vxe-column title="联系人电话" field="mobile" width="130"/>
        <vxe-column title="创建时间" field="createdAt" align="center" width="150"/>
        <vxe-column title="状态" field="enabled" align="center" width="100">
          <template #default="{row}">
            <t-tag theme="primary" v-if="row.enabled" @click="trigger(row)">启用</t-tag>
            <t-tag theme="danger" v-else @click="trigger(row)">禁用</t-tag>
          </template>
        </vxe-column>
        <vxe-column title="操作" align="center" width="100">
          <template #default="{row}">
            <div class="flex items-center justify-center">
              <span class=" primary-color text-hover ml-10px" @click="showForm(row)">编辑</span>
            </div>
          </template>
        </vxe-column>
      </vxe-table>
      <t-pagination class="mt-16px" v-model:current="pagination.page" :total="pagination.total" :page-size="pagination.size" @change="pageChange" size="small"/>
    </div>
  </div>
</template>

<script>
import Merchant from "@js/api/Merchant";
import {DialogPlugin, MessagePlugin} from "tdesign-vue-next";
import MerchantForm from "@/views/merchant/MerchantForm";
import MerchantSetting from "@/views/merchant/MerchantSetting";
import {DrawerPlugin} from "tdesign-vue-next";
import {h} from "vue";

/**
 * @功能描述: 商户列表
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "MerchantList",
  components: {MerchantForm, MerchantSetting},
  data() {
    return {
      merchant: null,
      loading: false,
      params: {
        name: null
      },
      checkedRows: [],
      dataList: [],
      pagination: {
        page: 1,
        size: 20,
        total: 0
      },
    }
  },
  computed: {
    queryParams() {
      return Object.assign(this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.size
      })
    }
  },
  watch: {
    'pagination.page'() {
      this.pageChange();
    }
  },
  methods: {
    showForm(merchant) {
      const dialog = DialogPlugin({
        header: "商户信息",
        closeOnOverlayClick: false,
        closeBtn: false,
        footer: false,
        width: '800px',
        body: h(MerchantForm, {
          merchant,
          onClose: () => dialog.hide(),
          onSuccess: () => {
            this.doSearch();
            dialog.hide();
          }
        })
      });
    },
    loadList() {
      this.loading = true;
      Merchant.list(this.queryParams).then(({data}) => {
        this.dataList = data.results;
        this.pagination.total = data.total;
      }).finally(() => this.loading = false);
    },
    pageChange() {
      this.loadList();
    },
    tableCheck() {
      this.checkedRows = this.$refs.table.getCheckboxRecords();
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    doConfig(merchant) {
      DrawerPlugin({
        header: merchant.name,
        size: '800px',
        body: h(MerchantSetting, { merchant })
      });
    },
    doRemove(row) {
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认删除商户：${row.name}?`,
        onConfirm: () => {
          Merchant.remove(row.id).then(() => {
            MessagePlugin.success("删除成功~");
            this.loadList();
          })
        }
      })
    },
    trigger(row) {
      let enabled = !row.enabled;
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认要「${enabled ? "启用" : "禁用"}」商户：${row.name}?`,
        onConfirm: () => {
          Merchant.save({id: row.id, enabled}).then(() => {
            MessagePlugin.success("操作成功~");
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
