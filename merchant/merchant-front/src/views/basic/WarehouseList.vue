<template>
  <div class="frame-page flex flex-column">
        <div class="table-toolbar">
          <Button @click="showForm()" color="primary">新 增</Button>
          <div>
            <Row :space-x="10">
              <Cell width="50" class="flex items-center">
                <Input id="name" v-model="params.filter" class="flex-1" placeholder="请输入仓库名称"/>
                <Button color="primary" :loading="loading" @click="doSearch">查询</Button>
              </Cell>
            </Row>
          </div>
        </div>
        <vxe-table row-id="id"
                   ref="table"
                   :row-config="{height: 48}"
                   stripe
                   :data="dataList"
                   show-overflow
                   :column-config="{resizable: true}"
                   :loading="loading">
          <vxe-column type="seq" width="60" align="center"/>
          <vxe-column title="仓库编码" field="code" width="200"/>
          <vxe-column title="仓库名称" field="name" width="300"/>
          <vxe-column title="仓库地址" field="address"/>
          <vxe-column title="默认" field="isDefault" width="120" align="center">
            <template #default="{row:{systemDefault}}">
              <Tag color="primary" v-if="systemDefault">是</Tag>
              <Tag color="yellow" v-else>否</Tag>
            </template>
          </vxe-column>
          <vxe-column title="状态" field="enabled" width="120" align="center">
            <template #default="{row:{enabled}}">
              <Tag color="primary" v-if="enabled">启用</Tag>
              <Tag color="red" v-else>禁用</Tag>
            </template>
          </vxe-column>
          <vxe-column title="操作" align="center" width="150">
            <template #default="{row}">
              <div class="flex items-center justify-center">
                <i class="primary-color h-icon-edit ml-10px" @click="showForm(row)"></i>
                <i class="primary-color h-icon-trash ml-10px" @click="doRemove(row)"></i>
              </div>
            </template>
          </vxe-column>
        </vxe-table>
  </div>
</template>

<script>
import {layer} from "@layui/layer-vue";
import {h} from "vue";
import WarehouseForm from "@views/basic/WarehouseForm";
import Warehouse from "@js/api/Warehouse";
import {confirm, message} from "heyui.ext";


export default {
  name: "WarehouseList",
  data() {
    return {
      loading: false,
      params: {
        name: '',
      },
      checkedRows: [],
      dataList: [],
    }
  },
  computed: {
    queryParams() {
      return Object.assign(this.params, {})
    }
  },
  methods: {
    showForm(warehouse = null) {
      let layerId = layer.open({
        title: "仓库信息",
        shadeClose: false,
        closeBtn: false,
        area: ['500px', '450px'],
        content: h(WarehouseForm, {
          warehouse,
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
    loadList() {
      this.loading = true;
      Warehouse.list(this.queryParams).then(({data}) => {
        this.dataList = data.results;
      }).finally(() => this.loading = false);
    },
    pageChange() {
      this.loadList();
    },
    tableCheck() {
      this.checkedRows = this.$refs.table.getCheckboxRecords();
    },
    doSearch() {
      this.loadList();
    },
    doRemove(row) {
      confirm({
        title: "系统提示",
        content: `确认删除：${row.name}?`,
        onConfirm: () => {
          Warehouse.remove(row.id).then(() => {
            message("删除成功~");
            this.doSearch();
          })
        }
      })
    }
  },
  created() {
    this.doSearch();
  }
}
</script>

<style scoped>

</style>
