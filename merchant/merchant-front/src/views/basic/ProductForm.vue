<template>
  <div class="modal-column" style="background: #f5f5f5">
    <div class="flex justify-between py-5px px-5px bg-white-color">
      <Button class="ml-10px" @click="$emit('close')" :loading="loading">
        取 消
      </Button>
      <Button class="mr-10px" color="primary" @click="save" :loading="loading">
        保 存
      </Button>
    </div>
    <div class="flex goods-form flex1">
      <div class="flex1">
        <div class="h-panel m-10px ">
          <div class="flex p-8px border-bottom">
            <span class="font-bold">商品信息</span>
          </div>
          <div class="flex pt-15px">
            <Form ref="form" class="mr-10px" :model="model" :rules="validationRules" mode="twocolumn" :label-width="90">
              <FormItem label="商品编码" prop="code">
                <Input placeholder="请输入编码,不填自动生成" maxlength="64" :disabled="model.id" v-model="model.code"/>
              </FormItem>
              <FormItem label="商品名称" required prop="name">
                <Input placeholder="长度<64" v-model="model.name" maxlength="64"/>
              </FormItem>
              <FormItem label="商品分类" required prop="categoryId">
                <CategoryPicker :option="categoryOption" type="key" filterable v-model="model.categoryId"></CategoryPicker>
              </FormItem>
              <FormItem label="规格" required prop="specification">
                <Input placeholder="请输入规格" v-model="model.specification" maxlength="120"/>
              </FormItem>
              <FormItem label="是否启用" prop="enabled">
                <Radio v-model="model.enabled" dict="statusRadios"/>
              </FormItem>
              <FormItem label="排序号" required prop="sort">
                <Input placeholder="请输入排序号" v-model="model.sort"/>
              </FormItem>
              <FormItem label="计量单位" required prop="unitId">
                <div class="h-input-group">
                  <Select :datas="unitList" keyName="id" titleName="name" :deletable="false" v-model="model.unitId" placeholder="基本单位"/>
                  <span class="h-input-addon">  <Checkbox v-model="model.enableMultiUnit" :trueValue="true" :falseValue="false">启用多单位</Checkbox></span>
                </div>
              </FormItem>
              <template v-if="model.enableMultiUnit && model.unitId">
                <FormItem :label="`单位`+(index+1)" v-for="(mu,index) in model.multiUnit" :required="index===0">
                  <div class="h-input-group">
                    <Select :datas="unitList" keyName="id" v-model="mu.unitId" @change="multiUnitChange" filterable titleName="name" :placeholder="`选择单位`+(index+1)"/>
                    <span class="ml-10px mr-10px">=</span>
                    <NumberInput type="number" :min="0.01" v-model.number="mu.num"/>
                    <span class="h-input-addon">{{ unitName }}</span>
                  </div>
                </FormItem>
              </template>
              <FormItem label="商品描述" prop="remarks" single>
                <Textarea v-wordcount="150" rows="2" placeholder="商品描述" v-model="model.remarks"/>
              </FormItem>
              <FormItem label="商品图片" prop="imgPath" single>
                <div class="h-uploader-image-empty h-uploader-browse-button" @click="$refs.uploads.click()">
                  <div class="h-uploader-image" v-if="model.imgPath">
                    <img :src="model.imgPath" v-if="model.imgPath" style="height: 70px;width: 70px" />
                  </div>
                  <i class="h-icon-plus" v-else style="font-size: 25px; position: absolute;  top: 50%;  left: 50%; transform: translate(-50%,-50%);"></i>
                  <input type="file" id="uploads" style="position:absolute; clip:rect(0 0 0 0);" ref="uploads" accept="image/png, image/jpeg, image/gif, image/jpg"
                         @change="selectImg($event)">
                </div>
              </FormItem>
              <div style="clear: both"></div>
            </Form>
          </div>
        </div>
        <div class="h-panel m-10px">
          <div class="flex p-8px border-bottom">
            <span class="font-bold">客户级别定价</span>
          </div>
          <div style="height:calc(100vh - 665px); overflow-y:auto;">
            <vxe-table height="auto" row-id="customerLeveId" ref="tableCustomLevelPrice" :data="customerLevelPriceList"
                       highlight-hover-row show-overflow border :row-config="{height: 48}" >
              <vxe-column title="客户级别" field="customLeveName" width="130"/>
              <vxe-column :title="`订货价${unitName?'('+unitName+')':''}`" field="name" align="right">
                <template #default="{ row }">
                  <Input class="tnumber" type="text" v-model.number="row.price"/>
                </template>
              </vxe-column>
              <template v-if=" model.enableMultiUnit && model.multiUnit">
                <template v-for="mu in model.multiUnit">
                  <vxe-column :title="`订货价(${getUnitName(mu.unitId)})`" v-if="mu.unitId" :field="mu.unitId" align="right">
                    <template #default="{ row }">
                      <Input class="tnumber" type="text" v-model.number="row[mu.unitId]"/>
                    </template>
                  </vxe-column>
                </template>
              </template>
            </vxe-table>
          </div>
        </div>
      </div>
    </div>
    <Loading text="运行中" :loading="loading"></Loading>
  </div>
</template>

<script>
/**
 * @功能描述: 商品FORM
 * @创建时间: 2024年05月06日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import {confirm, message} from "heyui.ext";
import {CopyObj} from "@common/utils";
import Product from "@js/api/Product";
import ProductCategory from "@js/api/ProductCategory";
import CustomerLevel from "@js/api/CustomerLevel";
import Unit from "@js/api/Unit";
import {OssUpload} from "@js/api/App";

export default {
  name: "ProductForm",
  props: {
    entity: Object,
  },
  data() {
    return {
      loading: false,
      categoryOption: {
        keyName: 'id',
        titleName: 'name',
        dataMode: 'list',
        parentName: 'pid',
        datas: []
      },
      unitList: [],
      customerLevel: [],
      customerLevelPriceList: [],
      model: {
        id: null,
        code: null,
        name: null,
        categoryId: null,
        imgPath: null,
        unitId: null,
        enableMultiUnit: false,
        multiUnit: [],
        specification: null,
        sort: 0,
        remarks: null,
        enabled: true,
      },
      validationRules: {}
    }
  },
  watch: {
    'model.enableMultiUnit'(val) {
      if (val) {
        let multiUnit = [];
        if (this.model.multiUnit) {
          for (let i = 0; i < 2; i++) {
            if (this.model.multiUnit[i]) {
              multiUnit.push(this.model.multiUnit[i]);
            } else {
              multiUnit.push({unitId: null, unitName: null, price: 0.0, isDefault: false, num: null});
            }
          }
          this.model.multiUnit = multiUnit;
        } else if (!this.model.multiUnit || this.model.multiUnit.length === 0) {
          multiUnit.push({unitId: null, unitName: null, price: 0.0, isDefault: false, num: null});
          multiUnit.push({unitId: null, unitName: null, price: 0.0, isDefault: false, num: null});
          this.model.multiUnit = multiUnit;
        }
      }
    },
  },
  computed: {
    unitName() {
      if (this.model.unitId && this.unitList.length) {
        return this.unitList.find(val => val.id === this.model.unitId).name;
      }
    },
  },
  methods: {
    selectImg(e) {
      let file = e.target.files[0]
      if (!/\.(jpg|jpeg|png|JPG|PNG)$/.test(e.target.value)) {
        message.error('图片类型要求：jpeg、jpg、png');
        return false
      }
      if (file && file.size > 102400) {
        e.target.value = '';
        message.error("图片大于100KB");
        return false
      }
      var reader = new FileReader();  //通过FileReader类型读取文件中的数据（异步文件读取）
      reader.onload = function (e) {
        var data = e.target.result;  //返回文件框内上传的对象
        //加载图片获取图片真实宽度和高度
        var image = new Image();
        image.onload = function () {
          var width = image.width;
          var height = image.height;
          if (width !== height || width > 480) {
            message.error("图片像素要相等，并且不能超过480");
            return false
          }
          console.log('文件像素宽：' + width + '，文件像素高：' + height);
        };
        image.src = data;
      };

      reader.readAsDataURL(file);

      const params = new FormData()
      params.append('file', file)

      OssUpload('goods', params).then(({data}) => {
        if (data) {
          this.model.imgPath = data;
        }
      })
    },
    save() {
      let validResult = this.$refs.form.valid();
      if (validResult.result) {
        if (!this.model.enableMultiUnit) {
          this.model.multiUnit = [];
        } else {
          let mus = [];
          mus.push(this.model.unitId);
          for (let i = 0; i < this.model.multiUnit.length; i++) {
            let val = this.model.multiUnit[i];
            if (val.unitId) {
              val.unitName = this.getUnitName(val.unitId);
              if (mus.includes(val.unitId)) {
                message.error(val.unitName + "单位不能一样~")
                return;
              }
            }
          }
          this.model.multiUnit = this.model.multiUnit.filter(val => val.unitId);
        }
        let checkPrice = false;
        this.customerLevelPriceList.forEach(val => {
          if (Number(val.price) === 0) {
            checkPrice = true;
          }
          if (this.model.enableMultiUnit) {
            this.model.multiUnit.forEach(mu => {
              if (mu.unitId ) {
                if (Number(val[mu.unitId] || 0) === 0) {
                  val[mu.unitId] = 0;
                  checkPrice = true;
                }
              }
            })
          }
        })
        if (checkPrice) {
          confirm({
            title: "系统提示",
            content: `检测到部分商品订货价为0，是否继续?`,
            onConfirm: () => {
              this.confirm();
            }
          })
        } else {
          this.confirm();
        }
      }
    },
    confirm() {
      this.loading = true;
      Product.save({product: this.model, levelPriceList: this.customerLevelPriceList}).then(() => {
        message("保存成功~");
        this.$emit('success');
      }).finally(() => this.loading = false);
    },
    multiUnitChange(data) {
      if (data) {
        if (this.model.unitId === data.id) {
          message.error("基础单位和辅助单位不能一致~");
        }
        if (this.model.multiUnit.filter(val => val.unitId === data.id).length > 1) {
          message.error("辅助单位不能一致~");
        }
      }
    },
    getUnitName(unitId) {
      if (unitId && this.unitList.length) {
        return this.unitList.find(val => val.id === unitId).name;
      }
    },

  },
  created() {
    Promise.all([
      ProductCategory.select(),
      Unit.select(),
      CustomerLevel.select(),
    ]).then((results) => {
      let categoryOptionList = results[0].data || [];
      this.categoryOption.datas = categoryOptionList;
      this.unitList = results[1].data;
      if (this.entity) {
        Product.levelPrice(this.entity.id).then(({data}) => {
          let levelPrice = data;
          if (results[2].data) {
            let customLevelPriceList = [];
            results[2].data.forEach(cl => {
              let cp = {customerLeveId: cl.id, customerLeveName: cl.name, price: 0};
              if (levelPrice) {
                let lp = levelPrice[cl.id];
                if (lp) {
                  cp.price = lp.price;
                  if (this.entity.enableMultiUnit) {
                    lp.unitPrice.forEach(mu => {
                      cp[mu.unitId] = mu.price || 0;
                    })
                  }
                }
              }
              customLevelPriceList.push(cp);
            })
            console.log(customLevelPriceList)
            this.customerLevelPriceList = customerLevelPriceList;
          }
        })
      } else {
        if (results[2].data) {
          let customLevelPriceList = [];
          results[2].data.forEach(cl => {
            let cp = {customerLeveId: cl.id, customLeveName: cl.name, price: 0};
            customerLevelPriceList.push(cp);
          })
          this.customerLevelPriceList = customerLevelPriceList;
        }
      }

    }).finally(() => this.loading = false);

    if (this.entity) {
      CopyObj(this.model, this.entity);
      if (this.model.enableMultiUnit) {
        if (this.model.multiUnit && this.model.multiUnit.length > 0 && this.model.multiUnit.length < 3) {
          let multiUnit = []
          let unitPrice = null
          for (let i = 0; i < 2; i++) {
            if (this.model.multiUnit[i]) {
              multiUnit.push(this.model.multiUnit[i])
            } else {
              multiUnit.push({unitId: null, unitName: null, price: 0.0, isDefault: false, num: null});
            }
          }
          if (unitPrice) {
            multiUnit.push(unitPrice);
          } else {
            multiUnit.push({unitId: null, unitName: null, price: 0.0, isDefault: false, num: null});
          }
          this.model.multiUnit = multiUnit;
        }
      }
    }
  }
}
</script>
<style lang="less">
.goods-form .h-form .h-form-item {
  padding-bottom: 15px !important;
}
</style>