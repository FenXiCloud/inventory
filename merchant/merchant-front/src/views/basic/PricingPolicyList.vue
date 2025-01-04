<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>

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
        <vxe-column title="取数来源" field="priceSource" width="150"/>
        <vxe-column title="应用说明" field="remarks"/>
        <vxe-column title="状态" field="enabled" width="120" align="center">
          <template #default="{row:{enabled}}">
            <Tag color="primary" v-if="enabled">启用</Tag>
            <Tag color="red" v-else>禁用</Tag>
          </template>
        </vxe-column>
        <vxe-column title="操作" align="center" width="150">
          <template #default="{row}">
            上移 下移
          </template>
        </vxe-column>
      </vxe-table>
    </div>
  </div>
</template>

<script>
import PricingPolicy from "@js/api/PricingPolicy";
import {confirm, message} from "heyui.ext";
import {layer} from "@layui/layer-vue";
import {h} from "vue";

/**
 * @功能描述: 价格策略
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "PricingPolicyList",
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
    doSearch() {
      this.loadList();
    },
    loadList() {
      this.loading = true;
      PricingPolicy.list(this.params).then(({data}) => {
        this.dataList = data;
      }).finally(() => this.loading = false);
    },
    doRemove(row) {
      confirm({
        title: "系统提示",
        content: `确认删除：${row.name}?`,
        onConfirm: () => {
          PricingPolicy.remove(row.id).then(() => {
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
