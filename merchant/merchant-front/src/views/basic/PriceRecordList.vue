<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <!--        <Button @click="showForm()" color="primary">新 增</Button>-->
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
        <vxe-column title="编码" field="code" width="150"/>
        <vxe-column title="名称" field="name"/>
        <vxe-column title="状态" field="enabled" width="120" align="center">
          <template #default="{row:{enabled}}">
            <Tag color="primary" v-if="enabled">启用</Tag>
            <Tag color="red" v-else>禁用</Tag>
          </template>
        </vxe-column>
        <vxe-column title="操作" align="center" width="150">
          <template #default="{row}">
            <i class="primary-color h-icon-edit ml-10px" @click="showForm(row)"></i>
            <i class="primary-color h-icon-trash ml-10px" @click="doRemove(row)"></i>
          </template>
        </vxe-column>
      </vxe-table>
    </div>
  </div>
</template>

<script>
import PriceRecord from "@js/api/basic/PriceRecord";
import {confirm, message} from "heyui.ext";

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
    doRemove(row) {
      confirm({
        title: "系统提示",
        content: `确认删除：${row.name}?`,
        onConfirm: () => {
          PriceRecord.remove(row.id).then(() => {
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
