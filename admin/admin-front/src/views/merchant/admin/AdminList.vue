<template>
  <div class="frame-page" style="margin: 0">
    <div class="h-panel">
      <div class="h-panel-body">
        <div class="table-toolbar">
          <div class="table-toolbar-left">
            <label for="username" class="mr-10px">账号</label>
            <Input id="username" v-model="params.username" class="flex-1" placeholder="请输入账号"/>
            <label for="name" class="mr-10px">姓名</label>
            <Input id="name" v-model="params.name" class="flex-1" placeholder="请输入姓名"/>
            <Button color="primary" :loading="loading" @click="doSearch">查询</Button>
          </div>
          <div class="table-toolbar-right">
            <Button @click="showForm()" color="primary">添加</Button>
          </div>
        </div>
        <div>
          <vxe-table row-id="id"
                     ref="table"
                     :data="dataList"
                     highlight-hover-row
                     show-overflow
                     :row-config="{height: 48}"
                     :loading="loading">
            <vxe-column type="seq" width="60" title="序列"/>
            <vxe-column title="账号" field="username"/>
            <vxe-column title="姓名" field="name"/>
            <vxe-column title="角色" field="roleName"/>
            <vxe-column title="默认管理员" field="systemDefault">
              <template #default="{row:{systemDefault}}">
                <Tag color="primary" v-if="systemDefault">是</Tag>
                <Tag color="yellow" v-else>否</Tag>
              </template>
            </vxe-column>
            <vxe-column title="状态" field="enabled">
              <template #default="{row:{enabled}}">
                <Tag color="primary" v-if="enabled">启用</Tag>
                <Tag color="red" v-else>禁用</Tag>
              </template>
            </vxe-column>
            <vxe-column title="操作" align="center" width="200">
              <template #default="{row}">
                <div class="flex items-center justify-center">
                  <span class="primary-color text-hover " @click="resetPassword(row)" size="s">重置密码</span>
                  <span class=" primary-color text-hover ml-10px" @click="showForm(row)" size="s">编辑</span>
                  <template v-if="!row.systemDefault">
                    <span class="primary-color ml-10px text-hover" @click="doRemove(row)" size="s">删除</span>
                    <DropdownMenu
                        trigger="hover"
                        class="primary-color ml-8px  text-hover"
                        :datas="param"
                        @clickItem="trigger($event,row)">
                      更多
                    </DropdownMenu>
                  </template>
                </div>
              </template>
            </vxe-column>
          </vxe-table>
          <Pagination align="right" class="mt-16px" v-model="pagination" @change="pageChange" small/>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import AdminForm from "./AdminForm.vue";
import Admin from "@js/api/Admin";
import {confirm, message} from "heyui.ext";
import {layer} from "@layui/layer-vue";
import {h} from "vue";

/**
 * @功能描述: 管理员列表
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "AdminList",
  components: {AdminForm},
  props: {
    merchant: Object,
  },
  data() {
    return {
      loading: false,
      params: {
        name: null,
        username: null,
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
        merchantId: this.merchant.id,
        page: this.pagination.page,
        pageSize: this.pagination.size
      })
    }
  },
  methods: {
    showForm(entity) {
      let layerId = layer.open({
        title: "管理员信息",
        shadeClose: false,
        closeBtn: false,
        area: ['500px', entity ? '370px' : '430px'],
        content: h(AdminForm, {
          entity,merchant:this.merchant,
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
      Admin.list(this.queryParams).then(({data}) => {
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
        content: `确认删除管理员：${row.name}?`,
        onConfirm: () => {
          Admin.remove(row.id).then(() => {
            message("删除成功~");
            this.loadList();
          })
        }
      })
    },
    resetPassword(row) {
      confirm({
        title: "系统提示",
        content: `确认要重置【${row.name}】的登录密码?`,
        onConfirm: () => {
          Admin.resetPassword(row.id).then(() => {
            message("重置成功~");
          })
        }
      })
    },
    trigger(code, row) {
      let enabled = code === 'enabled';
      confirm({
        title: "系统提示",
        content: `确认要「${enabled ? "启用" : "禁用"}」管理员：${row.name}?`,
        onConfirm: () => {
          Admin.save({id: row.id, enabled}).then(() => {
            message("操作成功~");
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
