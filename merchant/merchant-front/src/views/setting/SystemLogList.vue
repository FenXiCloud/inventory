<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-date-range-picker
            v-model="dateRangeValue"
            clearable
            allow-input
            placeholder="操作时间"
            style="width: 260px; border-radius: 4px"
        />
        <t-select
            v-model="params.operationType"
            :options="operationTypeOptions"
            clearable
            placeholder="操作类型"
            style="width: 140px; border-radius: 4px"
        />
        <t-input
            v-model.trim="params.keyword"
            clearable
            placeholder="模块/描述/IP"
            style="width: 220px; border-radius: 4px"
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
          table-layout="fixed"
          :data="dataList"
          :columns="columns"
          :loading="loading"
          empty="暂无操作日志"
      >
        <template #operationType="{ row }">
          <t-tag :theme="operationTheme(row.operationType)" variant="light">
            {{ row.operationType || '-' }}
          </t-tag>
        </template>
        <template #createdAt="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
        <template #requestParams="{ row }">
          <t-link v-if="row.requestParams" theme="primary" @click="showDetail(row)">查看</t-link>
          <span v-else>-</span>
        </template>
        <template #ops="{ row }">
          <t-link theme="primary" @click="showDetail(row)">详情</t-link>
        </template>
      </t-table>
    </div>

    <div class="simple-page__pager">
      <t-pagination
          v-model:current="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :show-jumper="true"
          :show-page-size="true"
          :popup-props="{ attach: 'body' }"
          @change="onPageChange"
      />
    </div>
  </div>
</template>

<script>
import SystemLog from '@js/api/setting/SystemLog';
import { DialogPlugin } from 'tdesign-vue-next';
import manba from 'manba';

const startTime = manba().startOf(manba.MONTH).format('YYYY-MM-DD');
const endTime = manba().endOf(manba.DAY).format('YYYY-MM-DD');

/**
 * @功能描述: 操作日志
 */
export default {
  name: 'SystemLogList',
  data() {
    return {
      loading: false,
      dateRangeValue: [startTime, endTime],
      params: {
        keyword: null,
        operationType: null
      },
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0
      },
      dataList: [],
      operationTypeOptions: [
        { label: '登录', value: '登录' },
        { label: '新增', value: '新增' },
        { label: '修改', value: '修改' },
        { label: '删除', value: '删除' },
        { label: '查询', value: '查询' },
        { label: '导出', value: '导出' },
        { label: '导入', value: '导入' }
      ],
      columns: [
        { colKey: 'createdAt', title: '操作时间', width: 170, align: 'center' },
        { colKey: 'module', title: '操作模块', width: 140, ellipsis: true },
        { colKey: 'operationType', title: '操作类型', width: 100, align: 'center' },
        { colKey: 'description', title: '描述', minWidth: 200, ellipsis: true },
        { colKey: 'createdByName', title: '操作人', width: 100, align: 'center' },
        { colKey: 'ipAddress', title: 'IP', width: 130, align: 'center' },
        { colKey: 'requestParams', title: '请求参数', width: 90, align: 'center' },
        { colKey: 'ops', title: '操作', width: 80, fixed: 'right', align: 'center' }
      ]
    };
  },
  computed: {
    queryParams() {
      const [start, end] = this.dateRangeValue || [];
      return {
        ...this.params,
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
        startTime: start || null,
        endTime: end || null
      };
    }
  },
  methods: {
    formatTime(value) {
      if (!value) return '-';
      return String(value).replace('T', ' ').substring(0, 19);
    },
    operationTheme(type) {
      const map = {
        登录: 'primary',
        新增: 'success',
        修改: 'warning',
        删除: 'danger',
        查询: 'default',
        导出: 'primary',
        导入: 'primary'
      };
      return map[type] || 'default';
    },
    showDetail(row) {
      const lines = [
        `操作时间：${this.formatTime(row.createdAt)}`,
        `操作模块：${row.module || '-'}`,
        `操作类型：${row.operationType || '-'}`,
        `操作人：${row.createdByName || '-'}`,
        `IP：${row.ipAddress || '-'}`,
        `对象ID：${row.objectId || '-'}`,
        `描述：${row.description || '-'}`,
        `请求参数：${row.requestParams || '-'}`
      ];
      DialogPlugin.confirm({
        header: '操作日志详情',
        content: lines.join('\n'),
        width: 560,
        confirmBtn: '关闭',
        cancelBtn: null
      });
    },
    loadList() {
      this.loading = true;
      SystemLog.list(this.queryParams)
        .then(({ data }) => {
          this.dataList = (data && data.results) || [];
          this.pagination.total = (data && data.total) || 0;
        })
        .finally(() => (this.loading = false));
    },
    doSearch() {
      this.pagination.page = 1;
      this.loadList();
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadList();
    }
  },
  created() {
    this.loadList();
  }
};
</script>

<style scoped>
.simple-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
  padding: 0 12px;
  box-sizing: border-box;
  overflow: hidden;
}

.simple-page__toolbar {
  flex-shrink: 0;
  padding: 8px 0;
}

.simple-page__table {
  flex: 1 1 0;
  height: 0;
  min-height: 0;
  overflow: hidden;
}

.simple-page__pager {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid var(--td-component-border, #dcdcdc);
}
</style>
