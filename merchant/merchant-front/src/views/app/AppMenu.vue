<template>
  <div class="app-menu">
    <AppLogo/>
    <div class="h-menu h-menu-dark" :class="siderCollapsed?'h-menu-mode-collapse':'h-menu-mode-vertical'">
      <li class="h-menu-li h-menu-first-level" v-for="m in menus" @click.stop="trigger(m,1)">
        <Tooltip v-if="siderCollapsed&& m.children &&!m.children.length" :content="m.title" placement="right">
          <div class="h-menu-show" :class="{'h-menu-li-selected':selected(m)}">
          <span class="h-menu-show-icon" v-if="m.icon">
            <i :class="m.icon"></i>
          </span>
            <span class="h-menu-show-desc text-16px">{{ m.title }}</span>
          </div>
        </Tooltip>
        <template v-else>
          <div class="h-menu-show" :class="{'h-menu-li-selected':selected(m)}">
          <span class="h-menu-show-icon" v-if="m.icon">
            <i :class="m.icon"></i>
          </span>
            <span class="h-menu-show-desc text-16px">{{ m.title }}</span>
          </div>
        </template>
        <div class="second-menu">
          <div class="second-menu-wrap">
            <ul class="second-menu-item" v-for="c1 in m.children" :key="c1.key">
              <span class="second-menu-title text-14px">{{ c1.title }}</span>
              <li class="text-14px" v-for="c2 in c1.children" @click.stop="trigger(c2,2)" :key="c2.key">{{ c2.title }}</li>
            </ul>
          </div>
        </div>
      </li>
    </div>
  </div>
</template>

<script>
import AppLogo from "@views/app/AppLogo";
import {mapState,mapMutations} from 'vuex';
import {pick} from "xe-utils";

/**
 * @功能描述: 菜单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
export default {
  name: "AppMenu",
  props: {
    theme: String
  },
  computed: {
    ...mapState(['siderCollapsed', 'menus', "currentTab", 'tabs']),
    menuMode() {
      return this.siderCollapsed ? 'collapse' : 'vertical';
    }
  },
  watch: {
    currentTab(val) {
      location.hash = val;
      localStorage.setItem("currentTab", JSON.stringify(pick(this.tabs.find(value => value.key === val), ['key', 'title'])))
    }
  },
  methods: {
    ...mapMutations(['pushTab', 'updateTab']),
    selected(item) {
      if ("DashboardMain" !== this.currentTab) {
        if (this.menus) {
          let find = this.menus.find(val => {
            if (val.key === this.currentTab) {
              return true;
            } else if (val.children) {
              for (const c1 of val.children) {
                if (c1.key === this.currentTab) {
                  return true;
                } else if (c1.children) {
                  for (const c2 of c1.children) {
                    if (c2.key === this.currentTab) {
                      return true;
                    }
                  }
                }
              }
            }
          })
          return find && (item.key === find.key);
        }
      }
      return false;
    },
    trigger(data, level) {
      if (data.key === 'DashboardMain') {
        this.updateTab('DashboardMain');
      } else if (data.key && level === 2) {
        this.pushTab(data);
      } else {
        if (!data.children){
          this.pushTab(data);
        }
      }
    },
    hideMenu() {
      this.$store.commit('updateSiderCollapse', true);
    }
  },
  components: {
    AppLogo
  }
}
</script>
<style lang="less" scoped>
.h-menu-mode-vertical {
  > .h-menu-show {
    padding-left: 15px;
    position: relative;
  }


  .h-menu-show {
    overflow: hidden;
    justify-content: left;
    text-overflow: ellipsis;
    display: flex;
    align-items: center;
    color: #b3b3b3;
    //padding: 16px 10px;
    height: 56px;
    line-height: 56px;
    transition: color @transition-time;

    &-icon {
      width: 18px;
      height: 18px;
      display: inline-flex;
      justify-content: center;
      align-items: center;
      margin-right: 10px;
    }
  }

  .h-menu-li:hover {
    .second-menu {
      display: block !important;
    }
  }

  .second-menu {
    box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

    position: absolute;
    display: none;
    //left: 100%;
    left: 150px;
    z-index: 1055;
    top: 0;
    border-left: 1px solid rgba(239, 236, 236, 0.89);

    .second-menu-wrap {
      overflow: hidden;
      display: table;
      padding: 20px 0;
      background-color: #3d74ff;
      color: #FFF;
      white-space: nowrap;
      position: relative;
      z-index: 1055;
    }

    .second-menu-item {
      display: table-cell;
      line-height: 30px;
      height: 100%;
      border-right: 1px solid rgba(239, 236, 236, 0.89);

      .second-menu-title {
        color: #dddbe3;
        padding: 0 40px;
      }

      li {
        display: block;
        list-style: none;
        cursor: pointer;
        vertical-align: top;
        padding: 13px 40px;
        &:hover{
          color: @yellow-color;
        }
      }
    }
  }

  &:hover{
    .h-menu-li-selected{
      color: #b3b3b3;
      background-color: transparent;
    }
  }

  .h-menu-show-icon{
    i{
      font-size: 18px !important;
    }
  }

  .h-menu-first-level:hover{

    background-color: #3d74ff;
    .h-menu-show-desc{
      color: @white-color;
    }
    .h-menu-show-icon{
      color: @white-color;
    }
  }


  .h-menu-li-selected {
    color: @white-color;
    background: #3d74ff;
  }
}
</style>
