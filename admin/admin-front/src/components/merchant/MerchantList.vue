<template>
  <div class="frame-page" style="margin: 0">
    <div class="h-panel">
      <div class="h-panel-body">
        <div class="table-toolbar">
          <div class="table-toolbar-left">
            <label for="name" class="mr-10px">商户名称</label>
            <Input id="name" v-model="params.name" class="flex-1" placeholder="请输入商户名称"/>
            <Button color="primary" :loading="loading" @click="doSearch">查询</Button>
          </div>
          <div class="table-toolbar-right">
            <Button @click="showForm()" color="primary">添加</Button>
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
          <vxe-column title="商户编码" field="code" width="80"/>
          <vxe-column title="商户名称" field="name" width="220">
            <template #default="{row}">
              <span @click="doConfig(row)" class="text-hover primary-color"> <i class="h-icon-edit"></i> {{ row.name }}</span>
            </template>
          </vxe-column>
          <vxe-column title="联系人" field="linkman" width="200"/>
          <vxe-column title="联系人电话" field="mobile" width="130"/>
          <vxe-column title="服务时间" field="serviceDate" align="center" width="200">
            <template #default="{row}">
              {{ row.serviceStartDate }} - {{ row.serviceEndDate }}
            </template>
          </vxe-column>
          <vxe-column title="创建时间" field="createDate" align="center" width="150"/>
          <vxe-column title="状态" field="enabled" align="center" width="100">
            <template #default="{row}">
              <Tag color="primary" v-if="row.enabled" @click="trigger(row)">启用</Tag>
              <Tag color="red" v-else @click="trigger(row)">禁用</Tag>
            </template>
          </vxe-column>
          <vxe-column title="操作" align="center">
            <template #default="{row}">
              <div class="flex items-center justify-center">
                <span class=" primary-color text-hover ml-10px" @click="showForm(row)" size="s">编辑</span>
              </div>
            </template>
          </vxe-column>
        </vxe-table>
        <Pagination align="right" class="mt-16px" v-model="pagination" @change="pageChange" small/>
      </div>
    </div>
  </div>
</template>

<script>
import Merchant from "@js/api/Merchant";
import {confirm, message} from "heyui.ext";
import MerchantForm from "@components/merchant/MerchantForm";
import MerchantSetting from "@components/merchant/MerchantSetting";
import {layer} from "@layui/layer-vue";
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
      param: [
        {title: '启用', key: 'enabled'},
        {title: '禁用', key: 'disabled'},
      ]
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
      let layerId = layer.open({
        title: "商户信息",
        shadeClose: false,
        closeBtn: false,
        area: ['600px', 'auto'],
        content: h(MerchantForm, {
          merchant,
          onClose: () => {
            layer.close(layerId);
          },
          onSuccess: () => {
            this.doSearch();
            layer.close(layerId);
          }
        })
      });
    }
    ,
    loadList() {
      this.loading = true;
      Merchant.list(this.queryParams).then(({data}) => {
        this.dataList = data.results;
        this.pagination.total = data.total;
      }).finally(() => this.loading = false);
    }
    ,
    pageChange() {
      this.loadList();
    }
    ,
    tableCheck() {
      this.checkedRows = this.$refs.table.getCheckboxRecords();
    }
    ,
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    }
    ,
    doConfig(merchant) {
      layer.drawer({
        title: merchant.name,
        area: ['80vw', '100vh'],
        content: h(MerchantSetting, {
          merchant
        })
      });
    }
    ,
    doRemove(row) {
      confirm({
        title: "系统提示",
        content: `确认删除商户：${row.name}?`,
        onConfirm: () => {
          Merchant.remove(row.id).then(() => {
            message("删除成功~");
            this.loadList();
          })
        }
      })
    }
    ,
    trigger(row) {
      let enabled = !row.enabled;
      confirm({
        title: "系统提示",
        content: `确认要「${enabled ? "启用" : "禁用"}」商户：${row.name}?`,
        onConfirm: () => {
          Merchant.save({id: row.id, enabled}).then(() => {
            message("操作成功~");
            this.loadList();
          })
        }
      })
    }
  }
  ,
  created() {
    this.loadList();
  }
}
</script>
