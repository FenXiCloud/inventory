<template>
  <div class="dashboard">
    <div class="dashboard__welcome">
      <div class="dashboard__hello">
        <div class="dashboard__name">{{ user.admin.name }}</div>
        <div class="dashboard__text">你好，欢迎使用进销存管理系统</div>
      </div>
      <div class="dashboard__meta">
        <div class="dashboard__date-label">今日</div>
        <div class="dashboard__date-value">{{ todayText }}</div>
      </div>
    </div>
    <div class="dashboard__calendar">
      <t-date-picker v-model="date" mode="date" :clearable="false"/>
    </div>
  </div>
</template>

<script>
import {mapState} from "vuex";
import manba from "manba";
import {openDialog, closeDialog} from '@common/dialog';
import {h} from "vue";
import AccountBookForm from "@views/setting/AccountBookForm.vue";

export default {
  name: "DashboardMain",
  data() {
    return {
      date: manba().format("YYYY-MM-dd"),
    }
  },
  computed: {
    ...mapState(['user', 'accountBooks']),
    todayText() {
      return manba().format("YYYY年MM月DD日");
    }
  },
  methods: {
    addAccountBook() {
      let dialogId = openDialog({
        header: "请先添加账套信息",
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '50vw',
        body: h(AccountBookForm, {
          onClose: () => {
            closeDialog(dialogId);
          },
          onSuccess: () => {
            window.location.replace("/");
            closeDialog(dialogId);
          }
        })
      });
    },
  },
  created() {
    if (!this.accountBooks || this.accountBooks === null) {
      this.addAccountBook()
    }
  }
}
</script>

<style scoped>
.dashboard {
  height: 100%;
  min-height: 0;
  display: flex;
  gap: 12px;
  box-sizing: border-box;
  overflow: hidden;
}

.dashboard__welcome {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 4px;
  padding: 28px 32px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  box-sizing: border-box;
}

.dashboard__name {
  font-size: 28px;
  font-weight: 600;
  color: #1f2329;
  line-height: 1.2;
  margin-bottom: 10px;
}

.dashboard__text {
  font-size: 15px;
  color: #646a73;
}

.dashboard__meta {
  text-align: right;
  flex-shrink: 0;
}

.dashboard__date-label {
  font-size: 13px;
  color: #8f959e;
  margin-bottom: 6px;
}

.dashboard__date-value {
  font-size: 16px;
  color: #1f2329;
  font-weight: 500;
}

.dashboard__calendar {
  flex-shrink: 0;
  background: #fff;
  border-radius: 4px;
  padding: 8px;
  box-sizing: border-box;
}
</style>
