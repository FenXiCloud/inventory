<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>

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
        <vxe-column title="编码" field="code" align="left" width="100"/>
        <vxe-column title="名称" field="name" align="left"/>
        <vxe-column title="商品类别" field="productCategoryName" align="left"/>
        <vxe-column title="规格" field="specification" align="left"/>
        <vxe-column title="单位" field="unitName" align="left"/>
        <vxe-column title="价格" field="price" align="left"/>
        <vxe-column title="价格类型" field="" align="left"/>
        <vxe-column title="价格来源" field="" align="left"/>
        <vxe-column title="创建时间" field="" align="left"/>
      </vxe-table>
    </div>
  </div>
</template>

<script>
import PriceRecord from "@js/api/basic/PriceRecord";

/**
 * @功能描述: 价格记录表
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "PriceRecordList",
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
      PriceRecord.list(this.params).then(({data}) => {
        this.dataList = data;
      }).finally(() => this.loading = false);
    },
  },
  created() {
    this.loadList();
  }
}
</script>
