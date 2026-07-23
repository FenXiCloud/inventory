<template>
  <div class="modal-column product-form">
    <div class="modal-column-full-body">
      <t-form
          ref="form"
          :data="model"
          :rules="rules"
          layout="vertical"
          label-align="top"
          scroll-to-first-error="smooth"
      >
        <t-row :gutter="16">
          <t-col :span="8">
            <t-row :gutter="[16, 8]">
              <t-col :span="6">
                <t-form-item label="产品编码" name="code">
                  <t-input
                      v-model="model.code"
                      placeholder="请输入编码,不填自动生成"
                      :maxlength="255"
                      :disabled="!!model.id"
                  />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="产品名称" name="name">
                  <t-input
                      v-model="model.name"
                      placeholder="请输入产品名称"
                      :maxlength="255"
                  />
                </t-form-item>
              </t-col>

              <t-col :span="6">
                <t-form-item label="产品分类" name="productCategoryId">
                  <t-tree-select
                      v-model="model.productCategoryId"
                      :data="categoryTree"
                      :keys="{ value: 'id', label: 'name', children: 'children' }"
                      filterable
                      clearable
                      placeholder="请选择无下级的分类"
                      :popup-props="{ overlayInnerStyle: { maxHeight: '280px' } }"
                      :tree-props="{ expandAll: true, expandOnClickNode: true }"
                      @change="onCategoryChange"
                  />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="规格" name="specification">
                  <t-input
                      v-model="model.specification"
                      placeholder="请输入规格"
                      :maxlength="120"
                  />
                </t-form-item>
              </t-col>

              <t-col :span="6">
                <t-form-item label="排序号" name="sort">
                  <t-input-number
                      v-model="model.sort"
                      theme="normal"
                      :min="0"
                      placeholder="请输入排序号"
                      style="width: 100%"
                  />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="计量单位" name="unitId">
                  <t-select
                      v-model="model.unitId"
                      :options="unitList"
                      :keys="{ value: 'id', label: 'name' }"
                      filterable
                      clearable
                      placeholder="请选择单位"
                  />
                </t-form-item>
              </t-col>

              <t-col :span="6">
                <t-form-item label="是否启用" name="enabled">
                  <t-radio-group v-model="model.enabled">
                    <t-radio :value="true">启用</t-radio>
                    <t-radio :value="false">禁用</t-radio>
                  </t-radio-group>
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="进货价" name="purchasePrice">
                  <t-input-number
                      v-model="model.purchasePrice"
                      theme="normal"
                      :min="0"
                      :decimal-places="2"
                      placeholder="请输入进货价"
                      style="width: 100%"
                  />
                </t-form-item>
              </t-col>

              <t-col :span="12">
                <t-form-item label="多单位">
                  <t-checkbox v-model="model.enableMultiUnit" :disabled="!model.unitId">
                    启用多单位换算
                  </t-checkbox>
                </t-form-item>
              </t-col>

              <template v-if="model.enableMultiUnit && model.unitId">
                <t-col
                    v-for="(mu, index) in model.auxiliaryUnitPrices"
                    :key="'aux-' + index"
                    :span="6"
                >
                  <t-form-item :label="`单位${index + 1}`" :required="index === 0">
                    <div class="product-form__unit-row">
                      <t-select
                          v-model="mu.unitId"
                          :options="unitList"
                          :keys="{ value: 'id', label: 'name' }"
                          filterable
                          clearable
                          :placeholder="`单位${index + 1}`"
                          class="product-form__unit-select"
                          @change="(val) => auxiliaryUnitPricesChange(val)"
                      />
                      <span class="product-form__unit-eq">=</span>
                      <t-input-number
                          v-model="mu.conversionRate"
                          theme="normal"
                          :min="0.01"
                          :decimal-places="2"
                          class="product-form__unit-rate"
                      />
                      <span class="product-form__unit-name">{{ unitName }}</span>
                    </div>
                  </t-form-item>
                </t-col>
              </template>

              <t-col :span="12">
                <t-form-item label="产品描述" name="remarks">
                  <t-textarea
                      v-model="model.remarks"
                      placeholder="产品描述"
                      :maxlength="150"
                      :autosize="{ minRows: 2, maxRows: 4 }"
                  />
                </t-form-item>
              </t-col>

              <t-col :span="12">
                <t-form-item label="产品图片" name="imgPath">
                  <div>
                    <div class="product-form__upload" @click="$refs.uploads.click()">
                      <img v-if="model.imgPath" :src="model.imgPath" alt="产品图片"/>
                      <t-icon v-else name="add" size="24px"/>
                      <input
                          ref="uploads"
                          type="file"
                          accept="image/png,image/jpeg,image/gif,image/jpg"
                          class="product-form__upload-input"
                          @change="selectImg($event)"
                      />
                    </div>
                    <div class="product-form__upload-tip">jpg/png，正方形且边长 ≤ 480，≤ 100KB</div>
                  </div>
                </t-form-item>
              </t-col>
            </t-row>
          </t-col>

          <t-col :span="4">
            <div class="product-form__price-panel">
              <div class="product-form__price-title">客户级别定价</div>
              <t-table
                  row-key="customerLeveId"
                  size="small"
                  bordered
                  hover
                  table-layout="fixed"
                  :data="customerLevelPriceList"
                  :columns="priceColumns"
                  :max-height="460"
              >
                <template #price="{ row }">
                  <t-input-number
                      v-model="row.price"
                      theme="normal"
                      :min="0"
                      :decimal-places="2"
                      style="width: 100%"
                  />
                </template>
                <template
                    v-for="mu in enabledAuxUnits"
                    :key="'price-' + mu.unitId"
                    #[`aux_${mu.unitId}`]="{ row }"
                >
                  <t-input-number
                      v-model="row[mu.unitId]"
                      theme="normal"
                      :min="0"
                      :decimal-places="2"
                      style="width: 100%"
                  />
                </template>
              </t-table>
            </div>
          </t-col>
        </t-row>
      </t-form>
    </div>

    <div class="modal-column-between">
      <t-button variant="outline" :loading="loading" @click="$emit('close')">取消</t-button>
      <t-button theme="primary" :loading="loading" @click="save">保存</t-button>
    </div>
  </div>
</template>

<script>
import {MessagePlugin} from 'tdesign-vue-next';
import {CopyObj} from '@common/utils';
import Product from '@js/api/basic/Product';
import ProductCategory from '@js/api/basic/ProductCategory';
import CustomerLevel from '@js/api/basic/CustomerLevel';
import Unit from '@js/api/basic/Unit';
import {OssUpload} from '@js/api/App';
import {toArrayTree} from '@common/utils';

export default {
  name: 'ProductForm',
  emits: {close: null, success: null},
  props: {
    entity: Object
  },
  data() {
    return {
      loading: false,
      categoryList: [],
      unitList: [],
      customerLevelPriceList: [],
      model: {
        id: null,
        code: null,
        name: null,
        productCategoryId: null,
        purchasePrice: 0,
        imgPath: null,
        unitId: null,
        enableMultiUnit: false,
        auxiliaryUnitPrices: [],
        specification: null,
        sort: 0,
        remarks: null,
        enabled: true
      },
      rules: {
        name: [{required: true, message: '请输入产品名称', type: 'error'}],
        productCategoryId: [{required: true, message: '请选择产品分类', type: 'error'}],
        sort: [{required: true, message: '请输入排序号', type: 'error'}],
        unitId: [{required: true, message: '请选择计量单位', type: 'error'}]
      }
    };
  },
  watch: {
    'model.enableMultiUnit'(val) {
      if (!val) return;
      const next = [];
      for (let i = 0; i < 2; i++) {
        if (this.model.auxiliaryUnitPrices?.[i]) {
          next.push(this.model.auxiliaryUnitPrices[i]);
        } else {
          next.push({unitId: null, unitName: null, price: 0, isDefault: false, conversionRate: 1});
        }
      }
      this.model.auxiliaryUnitPrices = next;
    }
  },
  computed: {
    categoryTree() {
      // 有下级的不可选；不能给父节点设 disabled（TDesign 会连带禁用子节点）
      const prune = (nodes) => (nodes || []).map((node) => {
        const children = prune(node.children || []);
        const hasChildren = children.length > 0;
        return {
          id: node.id,
          value: node.id,
          label: node.name,
          name: node.name,
          pid: node.pid,
          children: hasChildren ? children : undefined
        };
      });
      const list = (this.categoryList || []).map((item) => {
        const rawPid = item.pid ?? item.parentId;
        return {
          id: item.id,
          name: item.name,
          pid: rawPid == null || rawPid === '' || rawPid === 0 || rawPid === '0' ? null : rawPid
        };
      });
      return prune(toArrayTree(list, {key: 'id', parentKey: 'pid', children: 'children'}));
    },
    /** 无下级分类 id（同时存 number/string，避免选中后类型不一致） */
    leafCategoryIds() {
      const set = new Set();
      const walk = (nodes) => {
        (nodes || []).forEach((n) => {
          if (n.children?.length) walk(n.children);
          else if (n.id != null) {
            set.add(n.id);
            set.add(String(n.id));
          }
        });
      };
      walk(this.categoryTree);
      return set;
    },
    unitName() {
      if (!this.model.unitId || !this.unitList.length) return '';
      return this.unitList.find((val) => val.id === this.model.unitId)?.name || '';
    },
    enabledAuxUnits() {
      if (!this.model.enableMultiUnit) return [];
      return (this.model.auxiliaryUnitPrices || []).filter((mu) => mu.unitId);
    },
    priceColumns() {
      const cols = [
        {colKey: 'customerLeveName', title: '客户级别', width: 110, ellipsis: true},
        {
          colKey: 'price',
          title: this.unitName ? `价格(${this.unitName})` : '价格',
          minWidth: 110
        }
      ];
      this.enabledAuxUnits.forEach((mu) => {
        cols.push({
          colKey: `aux_${mu.unitId}`,
          title: `价格(${this.getUnitName(mu.unitId) || ''})`,
          minWidth: 110
        });
      });
      return cols;
    }
  },
  methods: {
    isSelectableCategory(id) {
      if (id == null || id === '') return false;
      return this.leafCategoryIds.has(id) || this.leafCategoryIds.has(String(id));
    },
    onCategoryChange(value) {
      if (value == null || value === '') return;
      // 有下级的分类不允许选中
      if (!this.isSelectableCategory(value)) {
        this.$nextTick(() => {
          this.model.productCategoryId = null;
        });
        MessagePlugin.warning('有下级的分类不能选择，请选择末级分类');
      }
    },
    selectImg(e) {
      const file = e.target.files?.[0];
      if (!file) return;
      if (!/\.(jpg|jpeg|png|JPG|PNG)$/.test(e.target.value)) {
        MessagePlugin.error('图片类型要求：jpeg、jpg、png');
        e.target.value = '';
        return;
      }
      if (file.size > 102400) {
        e.target.value = '';
        MessagePlugin.error('图片大于100KB');
        return;
      }
      const reader = new FileReader();
      reader.onload = (ev) => {
        const image = new Image();
        image.onload = () => {
          if (image.width !== image.height || image.width > 480) {
            MessagePlugin.error('图片像素要相等，并且不能超过480');
          }
        };
        image.src = ev.target.result;
      };
      reader.readAsDataURL(file);

      const params = new FormData();
      params.append('file', file);
      OssUpload('goods', params).then(({data}) => {
        if (data) this.model.imgPath = data;
      });
    },
    save() {
      this.$refs.form.validate().then((result) => {
        if (result !== true) return;
        if (!this.isSelectableCategory(this.model.productCategoryId)) {
          MessagePlugin.warning('有下级的分类不能选择，请选择末级分类');
          return;
        }

        if (!this.model.enableMultiUnit) {
          this.model.auxiliaryUnitPrices = [];
        } else {
          const used = [this.model.unitId];
          for (let i = 0; i < this.model.auxiliaryUnitPrices.length; i++) {
            const val = this.model.auxiliaryUnitPrices[i];
            if (!val.unitId) continue;
            val.unitName = this.getUnitName(val.unitId);
            if (used.includes(val.unitId)) {
              MessagePlugin.error(val.unitName + '单位不能一样~');
              return;
            }
            used.push(val.unitId);
          }
          this.model.auxiliaryUnitPrices = this.model.auxiliaryUnitPrices.filter((val) => val.unitId);
        }
        this.confirm();
      }).catch(() => {});
    },
    confirm() {
      this.loading = true;
      Product.save({product: this.model, customerLevelPriceList: this.customerLevelPriceList})
        .then(() => {
          MessagePlugin.success('保存成功~');
          this.$emit('success');
        })
        .finally(() => (this.loading = false));
    },
    auxiliaryUnitPricesChange(unitId) {
      if (!unitId) return;
      if (this.model.unitId === unitId) {
        MessagePlugin.error('基础单位和辅助单位不能一致~');
      }
      if (this.model.auxiliaryUnitPrices.filter((val) => val.unitId === unitId).length > 1) {
        MessagePlugin.error('辅助单位不能一致~');
      }
    },
    getUnitName(unitId) {
      if (!unitId || !this.unitList.length) return '';
      return this.unitList.find((val) => val.id === unitId)?.name || '';
    },
    buildCustomerLevelPriceList(levels, priceMap) {
      return (levels || []).map((cl) => {
        const cp = {customerLeveId: cl.id, customerLeveName: cl.name, price: 0};
        const lp = priceMap?.[cl.id];
        if (lp) {
          cp.price = lp.price;
          if (this.entity?.enableMultiUnit && lp.auxiliaryUnitPrices) {
            lp.auxiliaryUnitPrices.forEach((mu) => {
              cp[mu.unitId] = mu.unitPrice || 0;
            });
          }
        }
        return cp;
      });
    }
  },
  created() {
    if (this.entity) {
      CopyObj(this.model, this.entity);
      if (this.model.enableMultiUnit) {
        const list = this.model.auxiliaryUnitPrices || [];
        const next = [];
        for (let i = 0; i < 2; i++) {
          next.push(list[i] || {unitId: null, unitName: null, price: 0, isDefault: false, conversionRate: 1});
        }
        this.model.auxiliaryUnitPrices = next;
      }
    }

    Promise.all([
      ProductCategory.select(),
      Unit.select(),
      CustomerLevel.select()
    ]).then(async (results) => {
      this.categoryList = results[0].data || [];
      this.unitList = results[1].data || [];
      const levels = results[2].data || [];

      if (this.entity) {
        const {data} = await Product.customerLevelPrice(this.entity.id);
        this.customerLevelPriceList = this.buildCustomerLevelPriceList(levels, data);
      } else {
        this.customerLevelPriceList = this.buildCustomerLevelPriceList(levels);
      }
    });
  }
};
</script>

<style scoped>
.product-form {
  background: #fff;
}

.product-form :deep(.t-form__item) {
  margin-bottom: 8px;
}

.product-form :deep(.t-form__label) {
  padding-bottom: 4px !important;
}

.product-form__unit-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.product-form__unit-select {
  flex: 1;
  min-width: 0;
}

.product-form__unit-eq {
  flex-shrink: 0;
  color: var(--td-text-color-secondary);
}

.product-form__unit-rate {
  width: 110px;
  flex-shrink: 0;
}

.product-form__unit-name {
  flex-shrink: 0;
  min-width: 2em;
}

.product-form__price-panel {
  border: 1px solid var(--td-component-border, #dcdcdc);
  border-radius: 4px;
  overflow: hidden;
  height: 100%;
}

.product-form__price-title {
  padding: 10px 12px;
  font-weight: 600;
  border-bottom: 1px solid var(--td-component-border, #dcdcdc);
}

.product-form__upload {
  position: relative;
  width: 80px;
  height: 80px;
  border: 1px dashed var(--td-component-border, #dcdcdc);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--td-text-color-placeholder);
  overflow: hidden;
  background: var(--td-bg-color-container);
}

.product-form__upload:hover {
  border-color: var(--td-brand-color);
  color: var(--td-brand-color);
}

.product-form__upload img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-form__upload-input {
  position: absolute;
  clip: rect(0 0 0 0);
}

.product-form__upload-tip {
  margin-top: 6px;
  font-size: 12px;
  color: var(--td-text-color-placeholder);
}
</style>
