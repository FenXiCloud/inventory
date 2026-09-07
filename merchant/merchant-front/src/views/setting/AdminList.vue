<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" style="border-radius: 4px" @click="synchronization()">同步钉钉用户</t-button>
        <span v-if="ddSyncing" class="dd-sync__tip" :title="ddProgress">
          <t-loading size="small"/>
          <span class="dd-sync__text">{{ ddProgress }}</span>
        </span>
        <t-button v-auth="'admin:edit'" theme="primary" style="border-radius: 4px" @click="showForm()">新 增</t-button>
        <t-input
            v-model="params.username"
            clearable
            placeholder="请输入用户名"
            style="width: 240px; border-radius: 4px"
            @enter="doSearch"
        >
          <template #suffixIcon>
            <t-icon name="search" style="cursor:pointer" @click="doSearch"/>
          </template>
        </t-input>
        <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="doSearch">查询</t-button>
      </t-space>
    </div>

    <div class="simple-page__table">
      <t-table
          row-key="id"
          size="medium"
          bordered
          stripe
          hover
          height="100%"
          table-layout="auto"
          :data="dataList"
          :columns="columns"
          :loading="loading"
      >
        <template #ops="{ row }">
          <t-space size="small">
            <t-link theme="primary" @click="resetPassword(row)"><t-icon name="lock-on"/></t-link>
            <t-link v-auth="'admin:edit'" theme="primary" @click="showForm(row)"><t-icon name="edit"/></t-link>
            <t-link v-if="$can('admin:delete') && !row.systemDefault" theme="primary" @click="doRemove(row)"><t-icon name="delete"/></t-link>
          </t-space>
        </template>
        <template #systemDefault="{ row }">
          <t-tag
              v-if="row.systemDefault"
              theme="primary"
              variant="light"
              title="商户主账号：随商户开通自动创建，不可删除、不可禁用"
          >主账号</t-tag>
          <span v-else>—</span>
        </template>
        <template #enabled="{ row }">
          <t-tag
              v-if="$can('admin:edit') && !row.systemDefault"
              :theme="row.enabled ? 'primary' : 'danger'"
              variant="light"
              style="cursor: pointer"
              @click="trigger(row)"
          >
            {{ row.enabled ? '启用' : '禁用' }}
          </t-tag>
          <t-tag v-else :theme="row.enabled ? 'primary' : 'danger'" variant="light">
            {{ row.enabled ? '启用' : '禁用' }}
          </t-tag>
        </template>
      </t-table>
    </div>
  </div>
</template>

<script>
import AdminForm from './AdminForm.vue';
import Admin from '@js/api/setting/Admin';
import {MessagePlugin} from 'tdesign-vue-next';
import {DialogPlugin} from '@common/dialog-plugin';
import {openDialog, closeDialog} from '@common/dialog';
import {h} from 'vue';
export default {
  name: 'AdminList',
  data() {
    return {
      loading: false,
      ddSyncing: false,
      ddProgress: '',
      ddTimer: null,
      params: {
        name: null,
        username: null,
        phone: null
      },
      dataList: [],
      columns: [
        {colKey: 'ops', title: '操作', width: 120, fixed: 'left', align: 'center'},
        {colKey: 'username', title: '账号', minWidth: 120},
        {colKey: 'name', title: '姓名', minWidth: 120},
        {colKey: 'mobile', title: '电话', minWidth: 120},
        {colKey: 'roleName', title: '角色', minWidth: 120},
        {colKey: 'systemDefault', title: '主账号', width: 100, align: 'center'},
        {colKey: 'enabled', title: '状态', width: 90, align: 'center', fixed: 'right'}
      ]
    };
  },
  computed: {
    queryParams() {
      return Object.assign({}, this.params);
    }
  },
  methods: {
    synchronization() {
      // 钉钉同步为后台异步任务（全量串行调钉钉接口较慢），提交后轮询进度
      Admin.addUserByDingDing()
        .then(({data}) => {
          MessagePlugin.info(String(data || '同步任务已提交~'));
          this.watchDingDing();
        })
        .catch((err) => {
          // 已有任务在跑（重复点击/其他页签提交）时也挂上进度显示
          if (err && err.msg && err.msg.indexOf('进行中') >= 0) {
            this.watchDingDing();
          }
        });
    },
    watchDingDing() {
      if (this.ddTimer) return;
      this.ddSyncing = true;
      const tick = () => {
        Admin.syncProgress().then(({data}) => {
          if (!data) return;
          if (data.running) {
            this.ddSyncing = true;
            this.ddProgress = data.progress || '同步中…';
            return;
          }
          this.stopDingDingWatch();
          const res = data.result || '';
          if (res.indexOf('SYNC_ERROR:') === 0) {
            MessagePlugin.error(res.substring('SYNC_ERROR:'.length));
          } else if (res) {
            MessagePlugin.success(res.replace(/<br\/?>/g, ' ').replace(/[；;]\s*$/, ''));
          } else {
            MessagePlugin.info('同步任务已结束');
          }
          this.loadList();
        }).catch(() => {});
      };
      this.ddTimer = setInterval(tick, 2500);
      tick();
    },
    stopDingDingWatch() {
      if (this.ddTimer) {
        clearInterval(this.ddTimer);
        this.ddTimer = null;
      }
      this.ddSyncing = false;
    },
    showForm(entity) {
      let dialogId = openDialog({
        header: '用户信息',
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '600px',
        body: h(AdminForm, {
          entity,
          onClose: () => {
            closeDialog(dialogId);
          },
          onSuccess: () => {
            this.doSearch();
            closeDialog(dialogId);
          }
        })
      });
    },
    loadList() {
      this.loading = true;
      Admin.list(this.queryParams)
        .then(({data}) => {
          this.dataList = data;
        })
        .finally(() => (this.loading = false));
    },
    doSearch() {
      this.loadList();
    },
    doRemove(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认删除用户：${row.name}?`,
        onConfirm: () => {
          Admin.remove(row.id).then(() => {
            MessagePlugin.success('删除成功~');
            this.loadList();
          });
        }
      });
    },
    resetPassword(row) {
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认要重置【${row.name}】的登录密码?`,
        onConfirm: () => {
          Admin.resetPassword(row.id).then(() => {
            MessagePlugin.success('重置成功~');
          });
        }
      });
    },
    trigger(row) {
      let enabled = !row.enabled;
      DialogPlugin.confirm({
        header: '系统提示',
        body: `确认要「${enabled ? '启用' : '禁用'}」用户：${row.name}?`,
        onConfirm: () => {
          Admin.save({id: row.id, enabled}).then(() => {
            MessagePlugin.success('操作成功~');
            this.loadList();
          });
        }
      });
    }
  },
  created() {
    this.loadList();
    // 进入页面时若服务器仍有在跑的同步任务（如刷新过页面），继续显示进度
    Admin.syncProgress().then(({data}) => {
      if (data && data.running) {
        this.watchDingDing();
      }
    }).catch(() => {});
  },
  beforeDestroy() {
    this.stopDingDingWatch();
  }
};
</script>

<style scoped>
.dd-sync__tip {
  display: inline-flex;
  align-items: center;
  max-width: 420px;
  font-size: 12px;
  color: #0052d9;
}
.dd-sync__text {
  margin-left: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

