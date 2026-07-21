<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
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
            <div class="input-group goodsSelect">
              <t-select
                  :clearable="false"
                  ref="ms"
                  v-model="scope.row.financeId"
                  :options="financeItemMappings"
                  filterable
                  placeholder="请选择财务软件辅助项"
                  :keys="{ value: 'id', label: 'titleName' }"
                  @change="changeMappings(scope, 'financeId')"
              />
            </div>
          </template>
        </vxe-column>
        <vxe-column title="进销存辅助项" field="warehouseName">
          <template #default="scope">
            <div class="input-group goodsSelect">
              <t-select
                  :clearable="false"
                  ref="ms"
                  v-model="scope.row.inventoryId"
                  :options="itemMappings"
                  filterable
                  placeholder="请选择进销存辅助项"
                  :keys="{ value: 'id', label: 'titleName' }"
                  @change="changeMappings(scope, 'inventoryId')"
              />
            </div>
          </template>
        </vxe-column>
      </vxe-table>
    </div>
    <div class="modal-column-between">
      <t-button variant="outline" :loading="loading" @click="$emit('close')">取消</t-button>
      <t-button theme="primary" :loading="loading" @click="confirm">保存</t-button>
    </div>
  </div>
</template>

<script>
import FinanceAccountLink from "@js/api/setting/FinanceAccountLink";
import FinanceItemMap from "@js/api/setting/FinanceItemMap";
import Customer from "@js/api/basic/Customer";
import Product from "@js/api/basic/Product";
import Supplier from "@js/api/basic/Supplier";
import {MessagePlugin} from "tdesign-vue-next";
import {ObjectUtil} from "@js/common/utils";

export default {
  name: "FinanceItemMapBatchForm",
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
        inventoryId: [{ required: true, message: '请选择进销存辅助项' }],
        financeId: [{ required: true, message: '请选择财务软件辅助项' }]
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
        MessagePlugin.info("请添加数据~");
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
        MessagePlugin.success("请选择辅助项～");
        return;
      }
      this.loading = true;
      FinanceItemMap.batch(filter).then(() => {
        MessagePlugin.success("保存成功~");
        this.$emit('success');
      }).finally(() => this.loading = false);
    },
    init() {
      const categoryId = this.categoryId;
      FinanceAccountLink.accountingCategory({ids: this.categoryId}).then(({data}) => {
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
