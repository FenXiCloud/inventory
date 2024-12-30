<template>
  <div class="frame-page" >
    <div class="h-panel mt-10px">
      <div class="h-panel-body">
        <div class="table-toolbar">
          <Row :space-x="10">
            <Cell width="30" class="flex items-center">
              <Input id="name" v-model="params.filter" class="flex-1" placeholder="请输入仓库名称"/>
              <Button color="primary" icon="fa fa-search" :loading="loading" @click="doSearch">查询</Button>
            </Cell>
          </Row>
          <div> <Button @click="showForm()" color="primary">新 增</Button></div>
        </div>
        <vxe-table row-id="id"
                   ref="table"
                   :data="dataList"
                   show-overflow
                   :loading="loading">
          <vxe-column type="seq" width="60" align="center"/>
          <vxe-column title="仓库编码" field="code" width="200"/>
          <vxe-column title="仓库名称" field="name"/>
          <vxe-column title="仓库地址" field="address"/>
          <vxe-column title="默认" field="isDefault" width="80">
            <template #default="{row:{isDefault}}">
              <Tag color="primary" v-if="isDefault">是</Tag>
              <Tag color="yellow" v-else>否</Tag>
            </template>
          </vxe-column>
          <vxe-column title="状态" field="enabled" width="80">
            <template #default="{row:{enabled}}">
              <Tag color="primary" v-if="enabled">启用</Tag>
              <Tag color="red" v-else>禁用</Tag>
            </template>
          </vxe-column>
          <vxe-column title="操作" align="center" width="130">
            <template #default="{row}">
              <div class="flex items-center justify-center">
                <span class=" primary-color text-hover ml-10px" @click="showForm(row)" size="s">编辑</span>
                <span class="primary-color ml-10px text-hover" @click="doRemove(row)" size="s">删除</span>
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
