<template>
  <div class="sys-tabs-vue" :class="{ 'sys-tabs-oversize': tabs.length > 15 }">
    <div class="tabs-container" ref="scrollOuter">
      <div class="tabs-body">
        <DropdownMenu :datas="menus" @clickItem="trigger" @show="show" trigger="contextMenu" :toggleIcon="false">
          <div @click="updateTab('DashboardMain')" class="tabs-item" :class="{ 'tabs-item-chosen': currentTab=== 'DashboardMain' }">
            <div class="tabs-item-background"></div>
            <div class="tabs-item-title">
              <span class="tabs-item-icon icon-monitor"></span>
              <span>桌面</span>
            </div>
          </div>
          <div v-for="(item, index) of tabs"
               :key="`sys-tab-${index}`"
               :index="index"
               @click="handleClick(item)"
               class="tabs-item"
               :class="{ 'tabs-item-chosen': currentTab=== item.key }">
            <div class="tabs-item-background"></div>
            <div class="tabs-item-title">
              <span :class="item.icon" v-if="item.icon" class="tabs-item-icon"></span>
              <span>{{ item.title }}</span>
            </div>
            <span class="tabs-item-close h-icon-close" @click.stop="handleClose(item,index)"></span>
          </div>
        </DropdownMenu>
      </div>
    </div>
  </div>
</template>

<script>
import {mapMutations, mapState} from "vuex";

export default {
  name: 'TagsNav',
  props: {
    value: Object,
    homePage: {
      type: String,
      default: 'DashboardMain'
    }
  },
  data() {
    return {
      nowIndex: null,
      menus: {}
    };
  },
  computed: {
    ...mapState(['tabs', 'currentTab']),
  },
  methods: {
    ...mapMutations(['updateTab', 'clearTabs', 'closeOtherTab', 'closeSelfTab']),
    show(event) {
      let parent = event.target.parentNode;
      this.nowIndex = parent.getAttribute('index') || parent.parentNode.getAttribute('index');
      if (this.nowIndex == null) {
        this.menus = {
          closeAll: '关闭所有标签页'
        };
      } else {
        this.menus = {
          closeSelf: '关闭标签页',
          closeOther: '关闭其他标签页',
          closeAll: '关闭所有标签页'
        };
      }
    },
    trigger(key) {
      if (key === 'closeAll') {
        this.clearTabs();
      } else if (this.nowIndex) {
        if (key === 'closeOther') {
          this.closeOtherTab(this.nowIndex);
        } else if (key === 'closeSelf') {
          this.closeSelfTab(this.nowIndex);
        }
      }
    },
    handleClose(item, index) {
      this.closeSelfTab(index);
    },
    handleClick(item) {
      this.updateTab(item.key)
    }
  }
};
</script>
<style lang="less">
.sys-tabs-vue {
  position: relative;
  background: #fff;
  user-select: none;
  z-index: 1;
  height: 40px;

  .close-con {
    position: absolute;
    right: 0;
    top: 0;
    height: 100%;
    width: 32px;
    background: #fff;
    text-align: center;
    z-index: 10;
  }

  .tabs-container {
    position: absolute;
    left: 0;
    right: 0;
    top: 0;
    bottom: 0;


    .tabs-body {
      height: 100%;
      //box-shadow: 0px 6px 10px rgba(0, 0, 0, 0.09);
      padding: 0px 0 0;


      .h-dropdownmenu {
        display: block;
      }

      .h-dropdowncustom-show-content {
        overflow: hidden;
        padding: 0 8px;
        white-space: nowrap;
        display: flex;
      }
    }

    .tabs-item {
      transition: 0.2s;
      line-height: @sys-tabs-height - 9px;
      height: @sys-tabs-height - 10px;
      padding: 0 15px 0 15px;
      position: relative;
      max-width: 120px;
      flex: 1;
      margin-left: -4px;
      margin-right: -4px;
      cursor: pointer;
      overflow: hidden;


      &:after {
        content: '';
        display: inline-block;
        position: absolute;
        right: 0;
        height: 20px;
        bottom: 6px;
        //border-right: 1px solid #b5b5b5;
      }

      &-title {
        position: relative;
        z-index: 2;
        font-size: 14px;
        overflow: hidden;
        margin-right: 15px;


        .tabs-item-icon {
          margin-right: 5px;
          position: relative;
          top: -1px;
        }
      }

      &-background {
        color: #ebecef;
        position: absolute;
        right: 4px;
        top: 0;
        left: 3px;
        bottom: 0;
        border-top: #e6e7ea 1px solid;
        /* 确保其他边框不可见 */
        border-right: #e6e7ea 1px solid;
        border-bottom: #e6e7ea 1px solid;
        border-left: #e6e7ea 1px solid;
        //border-radius: 3px 3px 0 0;
      }

      &.tabs-item-chosen,
      &:hover {
        z-index: 1;

        &:after,
        &:before {
          content: '';
          bottom: 0;
          position: absolute;
          top: auto;
          border-right: none;
        }

        &:after {
          width: 0;
          height: 0;
          //border-right: 1px solid transparent;
          right: 0px;
          z-index: 3;
        }

        &:before {
          width: 0;
          height: 0;
          //border-left: 1px solid transparent;
          left: 0px;
          z-index: 3;
        }
      }

      @sysTabHoverColor: #ebecef;

      &:hover {
        .tabs-item-background {
          background: @sysTabHoverColor;
        }

        &:after {
          border-bottom: 1px solid @sysTabHoverColor;
        }

        &:before {
          border-bottom: 1px solid @sysTabHoverColor;
        }
      }

      &.tabs-item-chosen {
        z-index: 2;

        .tabs-item-background {
          background: #f5f7fa;
          border-top: #3d74ff 2px solid;
          border-bottom: none;
        }

        &:after {
          border-bottom: 10px solid #fff;
        }

        &:before {
          border-bottom: 10px solid #fff;
        }
      }

      &-close {
        transition: 0.2s;
        font-size: 12px;
        position: absolute;
        right: 10px;
        color: #999;
        cursor: pointer;
        border-radius: 50%;
        padding: 4px;
        top: 50%;
        margin-top: -9px;
        transform: scale(0.8);

        &:hover {
          color: @gray4-color;
          background: @dark4-color;
        }
      }
    }
  }

  &.sys-tabs-oversize {
    .tabs-item:not(.tabs-item-chosen) {
      padding-right: 10px;

      .tabs-item-close {
        display: none;
      }

      .tabs-item-title {
        margin-right: 0;
      }
    }
  }
}
</style>

