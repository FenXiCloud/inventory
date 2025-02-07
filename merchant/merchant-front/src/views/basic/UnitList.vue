<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <Button @click="showUnitForm()" color="primary">新 增</Button>
      </template>
      <template #tools>
        <Input id="name" v-model="params.name" class="flex-1" placeholder="请输入单位名称"/>
        <Button color="primary" :loading="loading" @click="searchUnit">查询</Button>
      </template>
    </vxe-toolbar>
    <div class="flex1">
      <vxe-table row-id="id"
                 ref="table"
                 :data="unitDataList"
                 highlight-hover-row
                 show-overflow
                 stripe
                 :row-config="{height: 48}"
                 :column-config="{resizable: true}"
                 :loading="loading">
        <vxe-column type="seq" width="40" title="#"/>
        <vxe-column title="名称" field="name"/>
        <vxe-column title="操作" align="center" width="150">
          <template #default="{row}">
            <i class="primary-color h-icon-edit ml-10px" @click="showUnitForm(row)"></i>
            <i class="primary-color h-icon-trash ml-10px" @click="deleteUnit(row)"></i>
          </template>
        </vxe-column>
      </vxe-table>
    </div>
  </div>
</template>

<script>
import Unit from "@js/api/basic/Unit";
import UnitForm from "./UnitForm.vue";
import {confirm, message} from "heyui.ext";
import {layer} from "@layui/layer-vue";
import {h} from "vue";

/**
 * @功能描述: 单位列表
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "UnitList",
  data() {
    return {
      loading: false,
      unitDataList: [],
      params: {
        name: null,
      },
    }
  },
  methods: {
    showUnitForm(entity) {
      let type = 0;
      let layerId = layer.open({
        title: "单位信息",
        shadeClose: false,
        closeBtn: false,
        area: ['300px', '230px'],
        content: h(UnitForm, {
          entity, type,
          onClose: () => {
            layer.close(layerId);
          },
          onSuccess: () => {
            this.searchUnit();
            layer.close(layerId);
          }
        })
      });
    },
    searchUnit() {
      this.loadUnit();
    },
    loadUnit() {
      this.loading = true;
      Unit.list(this.params).then(({data}) => {
        this.unitDataList = data;
      }).finally(() => this.loading = false);
    },
    deleteUnit(row) {
      confirm({
        title: "系统提示",
        content: `确认删除单位：${row.name}?`,
        onConfirm: () => {
          Unit.delete(row.id).then(() => {
            message("删除成功~");
            this.loadUnit();
          })
        }
      })
    }
  },
  created() {
    this.loadUnit();
  }
}
</script>
