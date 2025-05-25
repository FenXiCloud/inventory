<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <!--      <Form ref="form" :model="model" :rules="validationRules" :showErrorTip="true" :labelWidth="130">-->
      <!--        <FormItem label="财务软件辅助项" prop="financeId">-->
      <!--          <Select :datas="financeItemMappings" keyName="id" v-model="model.financeId" filterable-->
      <!--                  titleName="titleName" placeholder="选择财务软件辅助项" @change="changeMappings('financeId')"/>-->
      <!--        </FormItem>-->
      <!--        <FormItem label="进销存辅助项" prop="inventoryId">-->
      <!--          <Select :datas="itemMappings" keyName="id" v-model="model.inventoryId" filterable-->
      <!--                  titleName="titleName" placeholder="选择进销存辅助项" @change="changeMappings('inventoryId')"/>-->
      <!--        </FormItem>-->
      <!--      </Form>-->
      <vxe-table size="mini" ref="xTable" border="border" show-overflow keep-source
                 :row-config="{ height: 40, isCurrent: true, isHover: true }"
                 show-footer stripe
                 :data="templateData">
        <vxe-column title="操作" field="seq" width="70" align="center" fixed="left">
          <template #default="{ row, rowIndex }">
            <div>
              <div class="fa fa-plus text-hover mr-5px" @click="adjustRows('insert', rowIndex)"></div>
              <div v-if="templateData.length !== 1" class="fa fa-minus text-hover-danger"
                   @click="adjustRows('delete', rowIndex)"></div>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="财务软件辅助项" field="warehouseName">
          <template #default="scope">
            <div class="h-input-group goodsSelect">
              <Select :deletable="false" ref="ms" v-model="scope.row.financeId" :datas="financeItemMappings" filterable
                      placeholder="请选择财务软件辅助项" keyName="id" titleName="titleName"
                      @change="changeMappings(scope,'financeId')">
                <template v-slot:item="{ item }">
                  <div>{{ item.titleName }}</div>
                </template>
              </Select>
            </div>
          </template>
        </vxe-column>
        <vxe-column title="进销存辅助项" field="warehouseName">
          <template #default="scope">
            <div class="h-input-group goodsSelect">
              <Select :deletable="false" ref="ms" v-model="scope.row.inventoryId" :datas="itemMappings" filterable
                      placeholder="请选择进销存辅助项" keyName="id" titleName="titleName"
                      @change="changeMappings(scope,'inventoryId')">
                <template v-slot:item="{ item }">
                  <div>{{ item.titleName }}</div>
                </template>
              </Select>
            </div>
          </template>
        </vxe-column>
      </vxe-table>
    </div>
    <div class="modal-column-right">
      <Button icon="fa fa-close" @click="$emit('close')" :loading="loading">
        取消
      </Button>
      <Button icon="fa fa-save" color="primary" @click="confirm" :loading="loading">
        保存
      </Button>
    </div>
  </div>
</template>

<script>
import FinanceAccountLink from "@js/api/setting/FinanceAccountLink";
import FinanceItemMap from "@js/api/setting/FinanceItemMap";
import Customer from "@js/api/basic/Customer";
import Product from "@js/api/basic/Product";
import Supplier from "@js/api/basic/Supplier";
import {message} from "heyui.ext";
import {ObjectUtil} from "@js/common/utils";

export default {
  name: "FinanceItemMapForm",
  props: {
    id: [Number, String],
    categoryId: [Number, String],
  },
  data() {
    return {
      loading: false,
      canSave: false,
      voucherWords: [],
      subjects: [],
      model: {
        id: null,
        categoryId: null,
        inventoryId: null,
        inventoryCode: null,
        inventoryName: null,
        financeId: null,
        financeCode: null,
        financeName: null,
      },
      validationRules: {
        required: ['inventoryId', 'financeId']
      },
      itemMappings: [],
      financeItemMappings: [],
      templateData: []
    }
  },
  watch: {},
  methods: {
    confirm() {
      if (this.templateData.length === 0) {
        message.info("请添加数据~");
        return;
      }
      const filter = this.templateData.filter((item) => {
        return !ObjectUtil.isEmpty(item.financeId) || !ObjectUtil.isEmpty(item.inventoryId);
      });
      let flag = false;
      filter.forEach((item) => {
        flag = ObjectUtil.isEmpty(item.financeId) || ObjectUtil.isEmpty(item.inventoryId);
        item.categoryType = this.model.categoryType;
        item.categoryId = this.model.categoryId;
        item.categoryName = this.model.categoryName;
      });
      if (flag) {
        message("请选择辅助项～");
        return;
      }
      this.loading = true;
      FinanceItemMap.batchSave(filter).then(() => {
        message("保存成功~");
        this.$emit('success');
      }).finally(() => this.loading = false);
    },
    init() {
      const categoryId = this.categoryId;
      FinanceAccountLink.loadAccountingCategory({ids: this.categoryId}).then(({data}) => {
        this.model.categoryType = data.data[0].categoryType;
        this.model.categoryId = data.data[0].id;
        this.model.categoryName = data.data[0].name;
        this.financeItemMappings = data.data[0].details || [];
        this.financeItemMappings.forEach(item => {
          item.titleName = `${item.code} - ${item.name}`;
        });
      });
      switch (Number(categoryId)) {
        case 0:
          Customer.list().then(({data}) => {
            this.itemMappings = data.results || [];
            this.itemMappings.forEach(item => {
              item.titleName = `${item.name}`;
            });
          });
          break;
        case 1:
          Supplier.list().then(({data}) => {
            this.itemMappings = data.results;
            this.itemMappings.forEach(item => {
              item.titleName = `${item.code} - ${item.name}`;
            });
          });
          break;
        case 6:
          Product.list().then(({data}) => {
            this.itemMappings = data.results;
            this.itemMappings.forEach(item => {
              item.titleName = `${item.code} - ${item.name}`;
            });
          });
          break;
      }
    },
    changeMappings({rowIndex}, type) {
      switch (type) {
        case 'financeId': {
          const filter = this.financeItemMappings.filter(item => {
            return item.id === this.templateData[rowIndex].financeId;
          });
          this.templateData[rowIndex].financeName = filter[0].name;
          this.templateData[rowIndex].financeCode = filter[0].code;
          break;
        }
        case 'inventoryId': {
          const filter = this.itemMappings.filter(item => {
            return item.id === this.templateData[rowIndex].inventoryId;
          });
          this.templateData[rowIndex].inventoryName = filter[0].name;
          this.templateData[rowIndex].inventoryCode = filter[0].code;
          break;
        }
      }
    },
    load() {
      const id = this.id;
      FinanceItemMap.load(id).then(({data}) => {
        this.model = data;
      });
    },
    //添加行或减少行
    adjustRows(type, index) {
      if (type === "insert") {
        this.templateData.splice(index + 1, 0, {isNew: true});
      } else {
        this.templateData.splice(index, 1);
      }
    },
  },
  created() {
    this.init();
    if (this.id) {
      this.load();
    } else {
      for (let i = 0; i < 5; i++) {
        this.templateData.push({inventoryId: null, financeId: null});
      }
    }
  }
}
</script>

<style scoped lang="less">

</style>
