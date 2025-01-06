<template>
  <div class="frame-page flex flex-column">
    <vxe-toolbar>
      <template #buttons>
        <Button @click="showForm()" color="primary">新 增</Button>
      </template>
      <template #tools>
        <Input id="name" v-model="params.filter" class="flex-1" placeholder="请输入规则名称"/>
        <Button color="primary" :loading="loading" @click="doSearch">查询</Button>
      </template>
    </vxe-toolbar>
    <div class="flex1">
      <vxe-table row-id="id"
                 ref="table"
                 :data="dataList"
                 highlight-hover-row
                 show-overflow
                 :loading="loading">
        <vxe-column type="seq" width="60" align="center"/>
        <vxe-column title="规则名称" field="name" width="200"/>
        <vxe-column title="编码规则" field="format"/>
        <vxe-column title="流水号位数" field="serialNumberLength" width="120"/>
        <vxe-column title="创建时间" field="createdAt" width="120"/>
        <vxe-column title="默认" field="systemDefault" width="80" align="center">
          <template #default="{row}">
            <Tag color="primary" v-if="row.systemDefault" @click="trigger(row)" class="cursor-pointer">是</Tag>
            <Tag color="red" v-else @click="trigger(row)" class="cursor-pointer">否</Tag>
          </template>
        </vxe-column>
        <vxe-column title="操作" align="center" width="120" fixed="right">
          <template #default="{row}">
            <div class="flex items-center justify-center">
              <span class=" primary-color text-hover ml-10px" @click="showForm(row)" size="s">编辑</span>
            </div>
          </template>
        </vxe-column>
      </vxe-table>
    </div>
  </div>
</template>

<script>
import PrintTemplate from "@js/api/setting/PrintTemplate";
import {confirm, message} from "heyui.ext";
import PrintTemplateForm from "./PrintTemplateForm.vue";
import {layer} from "@layui/layer-vue";
import {h} from "vue";


export default {
  name: "PrintTemplateList",
  props: {
    merchant: Object,
  },
  components: {PrintTemplateForm},
  data() {
    return {
      opened: true,
      loading: false,
      params: {
        name: null,
        merchantId: null,
      },
      checkedRows: [],
      dataList: [],
      areaList: [],
      merchantList: [],
      param: [
        {title: '启用', key: 'enabled'},
        {title: '禁用', key: 'disabled'},
      ]

    }
  },
  computed: {
    queryParams() {
      return Object.assign(this.params, {})
    }
  },
  methods: {
    showForm(PrintTemplate) {
      let layerId = layer.open({
        title: "规则编码",
        shadeClose: false,
        area: ['50vw', 'auto'],
        content: h(PrintTemplateForm, {
          PrintTemplate,
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
      PrintTemplate.list(this.queryParams).then(({data}) => {
        this.dataList = data;
      }).finally(() => this.loading = false);
    },
    pageChange() {
      this.loadList();
    },
    doSearch() {
      this.loadList();
    },
    doRemove(row) {
      confirm({
        title: "系统提示",
        content: `确认删除规则：${row.name}?`,
        onConfirm: () => {
          PrintTemplate.remove(row.id).then(() => {
            message("删除成功~");
            this.doSearch();
          })
        }
      })
    },
    trigger(row) {
      let enabled = !row.enabled;
      confirm({
        title: "系统提示",
        content: `确认要「${enabled ? "启用" : "禁用"}」规则：${row.name}?`,
        onConfirm: () => {
          PrintTemplate.save({id: row.id, enabled}).then(() => {
            message("操作成功~");
            this.loadList();
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
