<template>
  <div class="frame-page flex flex-column !p-0px" style="background: none!important;height: 0px">
    <div class="flex">
      <div class="flex1 flex-column bg-white-color">
        <div class="flex border-bottom p-16px m-10px justify-between">
          <div class="flex  items-center">
            <span class="text-28px mr-9px">{{ user.admin.name }} </span>
            你好，欢迎使用进销存管理系统！
          </div>
        </div>
      </div>
      <div class="flex bg-white-color ml-10px">
        <DatePicker v-model="date" :inline="true"/>
      </div>
    </div>
  </div>
</template>


<script>
/**
 * @功能描述: 桌面
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
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

<style scoped lang="less">
.card-header {
  padding: 16px;
  color: #000;
  text-align: center;
  display: flex;
  background-color: #fff;
}

.common-card__footer-order {
  cursor: pointer;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;

  .common-card__footer-title {
    cursor: pointer;
    margin-top: 10px;
  }
}

.backlog-card__orderreview {
  cursor: pointer;
  display: flex;
  align-items: center;
  flex: 1 1;
  margin-right: 12px;
  height: 72px;
  background: #e9f1da;
  border-radius: 10px;

  .backlog-card__suffix {
    margin-left: 23px;

    .backlog-card__suffix-number {
      cursor: pointer;
      font-size: 18px;
      color: #333;
      letter-spacing: 0;
      font-weight: 500;
    }
  }

  .backlog-card__prefix {
    display: flex;
    justify-content: center;
    align-items: center;
    margin-left: 10px;
    width: 44px;
    height: 44px;
    background: #2ba471;
    border-radius: 50%;
  }
}

.common-card__header {
  display: flex;
}

.order-list a:not(:last-child) {
  margin-right: 10px;
}

.order-list a {
  font-size: 0.7rem;
  color: white;
  flex: 1;
  height: 95px;
  margin-bottom: 10px;


  .order-item {
    display: flex;
    flex-direction: initial;
    padding: 15px;
    border-radius: 6px;
  }

  h3 {
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: initial;
  }

  .order-item-content {
    width: 100%;
    text-align: left;
    color: #fff;
    font-size: 0.7rem;
  }
}


</style>
