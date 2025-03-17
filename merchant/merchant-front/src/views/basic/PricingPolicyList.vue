<template>
  <div class="frame-page flex flex-column">
    <div align="center">
      <Tabs :datas="param" v-model="selected" @change="change"></Tabs>
    </div>

    <vxe-toolbar>
      <template #buttons>

      </template>
      <template #tools>
        <Button color="primary" :loading="loading" @click="doSearch">刷新</Button>
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
        <vxe-column title="取数来源" field="policySource" width="150"/>
        <vxe-column title="应用说明" field="remarks"/>
        <vxe-column title="状态" field="enabled" width="120" align="center">
          <template #default="{row}">
            <Switch v-model="row.enabled" @change="toggleStatus(row)"></Switch>
          </template>
        </vxe-column>
        <vxe-column title="操作" align="center" width="150">
          <template #default="{row}">
            <span class="primary-color  text-hover ml-10px" @click="moveUp(row)">上移</span>
            <span class="primary-color  text-hover ml-10px" @click="moveDown(row)">下移</span>
          </template>
        </vxe-column>
      </vxe-table>
    </div>
    <div class="flex justify-center items-center p-15px bg-white-color border">
      <Button color="primary" @click="saveOrder" :loading="loading">
        保存
      </Button>
    </div>
  </div>
</template>

<script>
import PricingPolicy from "@js/api/basic/PricingPolicy";
import {confirm, message} from "heyui.ext";

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
      param: {
        module1: '销售价格取数',
        module2: '采购价格取数',
        // module3: '异常成本处理'
      },
      selected: '销售价格取数',
      params: {
        policyType: '销售价格取数',
      }
    }
  },
  methods: {
    change(data) {
      console.log(data)
      this.params.policyType = data.title;
      this.loadList();
    },
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
    },
    moveUp(row){
      const index = this.dataList.findIndex(item => item.id === row.id);
      if (index <= 0) {
        message("已经是第一条数据了");
        return;
      }
      // 创建新数组并交换位置
      const newList = [...this.dataList];
      [newList[index - 1], newList[index]] = [newList[index], newList[index - 1]];
      this.dataList = newList;

    },
    moveDown(row){
      const index = this.dataList.findIndex(item => item.id === row.id);
      if (index >= this.dataList.length - 1) {
        message("已经是最后一条数据了");
        return;
      }
      // 创建新数组并交换位置
      const newList = [...this.dataList];
      [newList[index], newList[index + 1]] = [newList[index + 1], newList[index]];
      this.dataList = newList;
    },
    saveOrder(){
      //保存排序
      PricingPolicy.sort({
        dataList: this.dataList
      }).then(() => {
        message("保存成功");
      });
    },
    toggleStatus(row) {
      console.log("row",row.enabled)
      // PricingPolicy.toggleStatus({
      //   id: row.id,
      //   enabled: row.enabled
      // }).then(() => {
      //   message("状态修改成功");
      // }).catch(() => {
      //   // 如果失败，回滚状态
      //   row.enabled = !row.enabled;
      // });
    }
  },
  created() {
    this.loadList();
  }
}
</script>
