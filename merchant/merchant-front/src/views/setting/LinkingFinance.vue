<template>
  <div class="frame-page flex flex-column">
    <div class="flex1">
      <vxe-table
          ref="table"
          :data="dataList"
          highlight-hover-row
          show-overflow
          :loading="loading">
        <vxe-column title="关联状态" width="100">
          <template #default="{row}">
            <div>已关联</div>
          </template>
        </vxe-column>
        <vxe-column title="进销存组织编码" field="organizationCode" width="150"/>
        <vxe-column title="进销存组织名称" field="organizationName" min-width="150"/>
        <vxe-column title="财务系统账号" min-width="150">
          <template #default="{row}">
            <div @click="editAccount(row)">
              <template v-if="row.relationAccountMobile">
                {{ row.relationAccountMobile }}
                <i class="primary-color h-icon-edit ml-10px"></i>
              </template>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="关联财务系统帐套" field="companyName" min-width="150"/>
        <vxe-column title="操作" align="center" width="120" fixed="right">
          <template #default="{row}">
            <div class="flex items-center justify-center">
              <span class=" primary-color text-hover ml-10px" @click="showForm()" size="s">编辑</span>
            </div>
          </template>
        </vxe-column>
      </vxe-table>
      <div class="mt-10px">
        <div class="mb-10px" style="font-weight: bold">操作指引:</div>
        <div class="flex w-900px" style="text-align: center;margin: 0 auto">
          <div class="w-150px p-10px bg-gray4-color br" @click="showForm">
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
          <div class="w-150px p-10px bg-gray4-color br" @click="toSubject">
            <div class="pt-20px">
              <Icon type="h-icon-setting" :size="40"/>
            </div>
            <div class="p-10px" style="font-weight: bold">设置进销存参数</div>
            <div class="pb-20px" style="text-align: left;color: gray;font-size: small">检查确定进销存核算参数的设置
            </div>
          </div>
          <div class="m-10px icon-center ">
            <Icon type="h-icon-right" :size="50" color="gainsboro"/>
          </div>
          <div class="w-150px p-10px bg-gray4-color br">
            <div class="pt-20px">
              <Icon type="h-icon-setting" :size="40"/>
            </div>
            <div class="p-10px" style="font-weight: bold">完善进销存资料</div>
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
// import RelationForm from "@components/group/setting/RelationForm.vue";
import LinkingFinance from "@js/api/setting/LinkingFinance";
import {message} from "heyui.ext";
// import RelationSubjectFrom from "@components/group/setting/RelationSubjectFrom.vue";
// import RelationAccountForm from "@components/group/setting/RelationAccountForm.vue";
import {mapMutations} from "vuex";


export default {
  name: "LinkingFinance",
  props: {
    merchant: Object,
  },
  data() {
    return {
      loading: false,
      isRelation: false,
      dataList: [],
    }
  },
  methods: {
    ...mapMutations(['pushTab']),
    toVoucher() {
      this.pushTab({key: 'OrderVoucher', title: '订单凭证'});
    },
    editAccount(item) {
      this.showAccountForm(item.id, item.relationAccountId)
    },
    showForm() {
      let cwRelation = this.dataList[0]
      if (cwRelation.relationAccountId) {
        let layerId = layer.open({
          title: "关联财务系统",
          shadeClose: false,
          area: ['600px', '360px'],
          content: h(RelationForm, {
            cwRelation,
            onClose: () => {
              layer.close(layerId);
            },
            onSuccess: () => {
              this.loadList();
              layer.close(layerId);
            }
          })
        });
      } else {
        this.showAccountForm(cwRelation.id)
      }
    },
    showAccountForm(cwId, id) {
      console.log(cwId, id)
      let layerId = layer.open({
        title: "财务系统信息",
        shadeClose: false,
        area: ['600px', '360px'],
        content: h(RelationAccountForm, {
          cwId, id,
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
      LinkingFinance.load().then(({data}) => {
        this.dataList = data;
        this.isRelation = this.dataList[0].isRelation
      }).finally(() => this.loading = false);
    },
    toSubject() {
      if (this.isRelation) {
        let relationCwId = this.dataList[0].id
        let layerId = layer.drawer({
          title: "默认对应会计科目",
          shadeClose: false,
          area: ['80vw', '100vh'],
          content: h(RelationSubjectFrom, {
            relationCwId,
            onClose: () => {
              layer.close(layerId);
            },
            onSuccess: () => {
              this.loadList();
              layer.close(layerId);
            }
          })
        });
      } else {
        message.error("请先关联财务系统帐套!")
      }
    }
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
