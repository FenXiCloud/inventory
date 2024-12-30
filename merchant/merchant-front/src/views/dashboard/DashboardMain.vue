<template>
  <div class="frame-page flex flex-column !p-0px" style="background: none!important;height: 0px">
    <div class="flex">
      <div class="flex1 flex-column bg-white-color">
        <div class="flex border-bottom p-16px m-10px justify-between">
          <div class="flex  items-center">
            <span class="text-28px mr-9px">{{ user.admin.name }} </span>
            你好，纷析云进销存管理系统！
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
import {HomeView} from "@js/api/App";
import {mapMutations, mapState} from "vuex";
import manba from "manba";
import {layer} from "@layui/layer-vue";
import {h} from "vue";
import AccountBookForm from "@views/setting/AccountBookForm.vue";

export default {
  name: "DashboardMain",
  data() {
    return {
      date: manba().format("YYYY-MM-dd"),
      dateTitle: manba().format("YYYY年MM月dd日"),
    }
  },
  computed: {
    ...mapState(['user', 'accountBooks',]),
  },
  methods: {
    ...mapMutations(['newTab']),
    addAccountBook() {
      let layerId = layer.open({
        title: "请先添加账套信息",
        shadeClose: false,
        closeBtn: false,
        area: ['50vw', 'auto'],
        content: h(AccountBookForm, {
          onClose: () => {
            layer.close(layerId);
          },
          onSuccess: () => {
            window.location.replace("/");
            layer.close(layerId);
          }
        })
      });
    },
    homeView() {
      this.loading = true;
      Promise.all([
        HomeView()
      ]).then((results) => {
        // this.inAmount = results[0].data.inAmount;
      }).finally(() => this.loading = false);
    },
    trigger(data) {
      this.newTab(data.key);
      this.$router.push({name: data.key});
    },
  },
  created() {
    if (!this.accountBooks || this.accountBooks === null) {
      this.addAccountBook()
    }
    this.homeView();
  }
}
</script>

<style scoped lang="less">
.card-header {
  padding: 16px 16px;
  //padding-top: 0 !important;
  color: #000;
  //border-radius: 3px;
  text-align: center;
  display: flex;
  background-color: @white-color;
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
    background: @bg-green-color;
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
