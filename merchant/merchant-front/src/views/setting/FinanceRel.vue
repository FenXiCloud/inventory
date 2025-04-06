<template>
  <div class="frame-page flex flex-column">
    <div class="flex1" style="width: 980px;margin: 0 auto">
      <vxe-table
          ref="table"
          :data="dataList"
          highlight-hover-row
          border
          show-overflow
          :loading="loading">
        <vxe-column title="关联状态" width="100">
          <template #default="{row}">
            <div>{{ row.linkStatus }}</div>
          </template>
        </vxe-column>
        <vxe-column title="进销存软件账套" field="accountBookName" min-width="150"/>
        <vxe-column title="财务软件帐套" field="financeAccountName" min-width="150"/>
        <vxe-column title="操作" align="center" width="300" fixed="right">
          <template #default="{row}">
            <template v-if="row.linkStatus==='关联'">
              <div class="flex items-center justify-center">
                <span class=" primary-color text-hover ml-10px" @click="showForm(row.id)" size="s">编辑</span>
                <!--                <span class=" primary-color text-hover ml-10px" @click="showForm(row.id)" size="s">进入云财务账套</span>-->
              </div>
            </template>
            <template v-else>
              <div class="flex items-center justify-center">
                <span class=" primary-color text-hover ml-10px" @click="showForm(row.id)" size="s">关联云财务</span>
              </div>
            </template>
          </template>
        </vxe-column>
      </vxe-table>
      <div class="mt-10px">
        <div class="mb-10px" style="font-weight: bold">操作指引:</div>
        <div class="flex w-900px" style="text-align: center;margin: 0 auto">
          <div class="w-150px p-10px bg-gray4-color br" @click="showForm()">
            <div class="pt-20px">
              <Icon type="h-icon-link" :size="40"/>
            </div>
            <div class="p-10px" style="font-weight: bold">关联财务帐套</div>
            <div class="pb-20px" style="text-align: left;color: gray;font-size: small">
              只有同时拥有财务软件和进销存的账套管理员才有权限设置关联
            </div>
          </div>
          <div class="m-10px icon-center">
            <Icon type="h-icon-right" :size="50" color="gainsboro"/>
          </div>
          <div class="w-150px p-10px bg-gray4-color br" @click="templateConfig">
            <div class="pt-20px">
              <Icon type="h-icon-setting" :size="40"/>
            </div>
            <div class="p-10px" style="font-weight: bold">进销存凭证模板</div>
            <div class="pb-20px" style="text-align: left;color: gray;font-size: small">检查确定进销存核算参数的设置
            </div>
          </div>
          <div class="m-10px icon-center ">
            <Icon type="h-icon-right" :size="50" color="gainsboro"/>
          </div>
          <div class="w-150px p-10px bg-gray4-color br" @click="itemMapping">
            <div class="pt-20px">
              <Icon type="h-icon-setting" :size="40"/>
            </div>
            <div class="p-10px" style="font-weight: bold">进销存辅助资料</div>
            <div class="pb-20px" style="text-align: left;color: gray;font-size: small">
              设置进销存基础资料与财务软件账套的会计科目之间的对应关系
            </div>
          </div>
          <div class="m-10px icon-center">
            <Icon type="h-icon-right" :size="50" color="gainsboro"/>
          </div>
          <div class="w-150px p-10px bg-gray4-color br" @click="toVoucher">
            <div class="pt-20px">
              <Icon type="h-icon-check" :size="40"/>
            </div>
            <div class="p-10px" style="font-weight: bold">生成凭证</div>
            <div class="pb-20px" style="text-align: left;color: gray;font-size: small">选择进销存单据生成凭证</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {layer} from "@layui/layer-vue";
import {h} from "vue";
import FinanceRelForm from "./FinanceRelForm.vue";
import FinanceAccountLink from "@js/api/setting/FinanceAccountLink";
import {mapMutations} from "vuex";
import VoucherTemplate from "@views/setting/VoucherTemplate.vue";
import {ObjectUtil} from "../../js/common/utils";


export default {
  name: "FinanceRel",
  props: {
    merchant: Object,
  },
  data() {
    return {
      loading: false,
      dataList: [],
    }
  },
  methods: {
    ...mapMutations(['pushTab']),
    toVoucher() {
      this.pushTab({key: 'OrderVoucher', title: '订单凭证'});
    },
    itemMapping() {
      this.pushTab({key: 'ItemMapping', title: '辅助项映射'});
    },
    templateConfig() {
      let layerId = layer.open({
        title: "凭证模板设置",
        shadeClose: false,
        area: ['1200px', '600px'],
        content: h(VoucherTemplate, {
          onClose: () => {
            layer.close(layerId);
          },
          onSuccess: () => {
            this.loadList();
            layer.close(layerId);
          }
        })
      });
    },
    showForm(id) {
      let layerId = layer.open({
        title: "关联财务软件",
        shadeClose: false,
        area: ['600px', '600px'],
        content: h(FinanceRelForm, {
          id,
          type: ObjectUtil.isEmpty(id) ? this.dataList && this.dataList.length > 0 ? 'load' : 'add' : 'edit',
          onClose: () => {
            layer.close(layerId);
          },
          onSuccess: () => {
            this.loadList();
            layer.close(layerId);
          }
        })
      });
    },
    loadList() {
      this.loading = true;
      FinanceAccountLink.list().then(({data}) => {
        console.log(data);
        this.dataList = data;
      }).finally(() => this.loading = false);
    },
  },
  created() {
    this.loadList();
  }
}
</script>
<style>
.icon-center {
  display: flex;
  justify-content: center;
  align-items: center;
}

.cw-content :hover {
  border: 1px solid #3d74ff;
}

.br {
  border-radius: 10px
}
</style>
