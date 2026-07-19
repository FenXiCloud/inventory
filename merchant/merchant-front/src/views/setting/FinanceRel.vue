<template>
  <div class="simple-page">
    <div class="simple-page__body">
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
          <template #linkStatus="{ row }">
            {{ row.linkStatus }}
          </template>
          <template #ops="{ row }">
            <t-space size="small">
              <template v-if="row.linkStatus === '关联'">
                <t-link theme="primary" @click="showForm(row.id)">编辑</t-link>
              </template>
              <template v-else>
                <t-link theme="primary" @click="showForm(row.id)">关联云财务</t-link>
              </template>
            </t-space>
          </template>
        </t-table>
      </div>

      <div class="guide">
        <div class="guide__title">操作指引:</div>
        <div class="guide__steps">
          <div class="guide__card" @click="showForm()">
            <div class="guide__icon">
              <t-icon name="link" :size="40"/>
            </div>
            <div class="guide__name">关联财务帐套</div>
            <div class="guide__desc">
              只有同时拥有财务软件和进销存的账套管理员才有权限设置关联
            </div>
          </div>
          <div class="guide__arrow">
            <t-icon name="chevron-right" :size="50" color="gainsboro"/>
          </div>
          <div class="guide__card" @click="templateConfig">
            <div class="guide__icon">
              <t-icon name="setting" :size="40"/>
            </div>
            <div class="guide__name">进销存凭证模板</div>
            <div class="guide__desc">检查确定进销存核算参数的设置</div>
          </div>
          <div class="guide__arrow">
            <t-icon name="chevron-right" :size="50" color="gainsboro"/>
          </div>
          <div class="guide__card" @click="itemMapping">
            <div class="guide__icon">
              <t-icon name="setting" :size="40"/>
            </div>
            <div class="guide__name">进销存辅助资料</div>
            <div class="guide__desc">
              设置进销存基础资料与财务软件账套的会计科目之间的对应关系
            </div>
          </div>
          <div class="guide__arrow">
            <t-icon name="chevron-right" :size="50" color="gainsboro"/>
          </div>
          <div class="guide__card" @click="toVoucher">
            <div class="guide__icon">
              <t-icon name="check" :size="40"/>
            </div>
            <div class="guide__name">生成凭证</div>
            <div class="guide__desc">选择进销存单据生成凭证</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {openDialog, closeDialog} from '@common/dialog';
import {h} from "vue";
import FinanceRelForm from "./FinanceRelForm.vue";
import FinanceAccountLink from "@js/api/setting/FinanceAccountLink";
import {mapMutations} from "vuex";
import VoucherTemplate from "@views/setting/VoucherTemplate.vue";
import {ObjectUtil} from "../../js/common/utils";
import FinanceItemMap from "./FinanceItemMap.vue";
import Voucher from "./Voucher.vue";

export default {
  name: "FinanceRel",
  props: {
    merchant: Object,
  },
  data() {
    return {
      loading: false,
      dataList: [],
      columns: [
        {colKey: 'linkStatus', title: '关联状态', width: 100},
        {colKey: 'accountBookName', title: '进销存软件账套', minWidth: 150, ellipsis: true},
        {colKey: 'financeAccountName', title: '财务软件帐套', minWidth: 150, ellipsis: true},
        {colKey: 'ops', title: '操作', width: 160, align: 'center', fixed: 'right'}
      ]
    }
  },
  methods: {
    ...mapMutations(['pushTab']),
    toVoucher() {
      let dialogId = openDialog({
        header: "订单凭证",
        closeOnOverlayClick: false,
        width: '100%',
        body: h(Voucher, {
          onClose: () => {
            closeDialog(dialogId);
          },
          onSuccess: () => {
            this.loadList();
            closeDialog(dialogId);
          }
        })
      });
    },
    itemMapping() {
      let dialogId = openDialog({
        header: "辅助项映射",
        closeOnOverlayClick: false,
        width: '1200px',
        body: h(FinanceItemMap, {
          onClose: () => {
            closeDialog(dialogId);
          },
          onSuccess: () => {
            this.loadList();
            closeDialog(dialogId);
          }
        })
      });
    },
    templateConfig() {
      let dialogId = openDialog({
        header: "凭证模板设置",
        closeOnOverlayClick: false,
        width: '1200px',
        body: h(VoucherTemplate, {
          onClose: () => {
            closeDialog(dialogId);
          },
          onSuccess: () => {
            this.loadList();
            closeDialog(dialogId);
          }
        })
      });
    },
    showForm(id) {
      let dialogId = openDialog({
        header: "关联财务软件",
        closeOnOverlayClick: false,
        width: '600px',
        body: h(FinanceRelForm, {
          id,
          type: ObjectUtil.isEmpty(id) ? this.dataList && this.dataList.length > 0 ? 'load' : 'add' : 'edit',
          onClose: () => {
            closeDialog(dialogId);
          },
          onSuccess: () => {
            this.loadList();
            closeDialog(dialogId);
          }
        })
      });
    },
    loadList() {
      this.loading = true;
      FinanceAccountLink.list().then(({data}) => {
        this.dataList = data || [];
      }).finally(() => this.loading = false);
    },
  },
  created() {
    this.loadList();
  }
}
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

.simple-page__body {
  width: 980px;
  max-width: 100%;
  margin: 0 auto;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 8px 0;
}

.simple-page__table {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.guide {
  flex-shrink: 0;
  margin-top: 10px;
}

.guide__title {
  margin-bottom: 10px;
  font-weight: bold;
}

.guide__steps {
  display: flex;
  width: 900px;
  max-width: 100%;
  margin: 0 auto;
  text-align: center;
  align-items: stretch;
}

.guide__card {
  width: 150px;
  padding: 10px;
  background: var(--td-bg-color-secondarycontainer, #f3f3f3);
  border-radius: 10px;
  cursor: pointer;
  box-sizing: border-box;
}

.guide__card:hover {
  border: 1px solid #3d74ff;
}

.guide__icon {
  padding-top: 20px;
}

.guide__name {
  padding: 10px;
  font-weight: bold;
}

.guide__desc {
  padding-bottom: 20px;
  text-align: left;
  color: gray;
  font-size: small;
}

.guide__arrow {
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 10px;
}
</style>
