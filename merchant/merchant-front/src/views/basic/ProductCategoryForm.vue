<template>
  <div class="modal-column">
    <div class="modal-column-full-body">
      <Form ref="form" :model="model" :rules="validationRules">
        <FormItem label="编码" required prop="code">
          <Input placeholder="请输入分类编码" :disabled="model.id" v-model="model.code"/>
        </FormItem>
        <FormItem label="名称" required prop="name">
          <Input placeholder="请输入分类名称" v-model="model.name"/>
        </FormItem>
        <FormItem label="排序" required prop="sort">
          <Input placeholder="请输入排序" v-model="model.sort"/>
        </FormItem>
        <FormItem label="分类图片" prop="imgPath" single>
          <div class="h-uploader-image-empty h-uploader-browse-button" @click="$refs.uploads.click()">
            <div class="h-uploader-image" v-if="model.imgPath">
              <img :src="model.imgPath" v-if="model.imgPath" style="height: 70px;width: 70px"/>
            </div>
            <i class="h-icon-plus" v-else style="font-size: 25px; position: absolute;  top: 50%;  left: 50%; transform: translate(-50%,-50%);"></i>
            <input type="file" id="uploads" style="position:absolute; clip:rect(0 0 0 0);" ref="uploads" accept="image/png, image/jpeg, image/gif, image/jpg"
                   @change="selectImg($event)">
          </div>
        </FormItem>
        <!--        <FormItem label="上级分类" prop="parent">-->
        <!--          <CategoryPicker :option="categoryOption" type="key" v-model="model.pid"></CategoryPicker>-->
        <!--        </FormItem>-->
      </Form>
    </div>
    <div class="modal-column-between">
      <Button @click="$emit('close')" :loading="loading">
        取消
      </Button>
      <Button color="primary" @click="confirm" :loading="loading">
        保存
      </Button>
    </div>
  </div>
</template>

<script>
/**
 * @功能描述: 组织机构Form
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import ProductCategory from "@js/api/basic/ProductCategory";
import {message} from "heyui.ext";
import {CopyObj} from "@common/utils";
import {OssUpload} from "@js/api/App";

export default {
  name: "ProductCategoryForm",
  emits: {
    close: null,
    success: null
  },
  props: {
    productCategory: Object,
    parent: Object,
    list: Array,
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
      model: {
        id: null,
        code: null,
        pid: null,
        name: null,
        imgPath: null,
        path: null,
        sort: 1,
      },
      validationRules: {}
    }
  },
  methods: {
    confirm() {
      let validResult  = this.$refs.form.valid();
      if (validResult.result) {

        if (this.model.pid) {
          let parent = this.list.find(c => c.id === this.model.pid);
          this.model.path = (parent.path || '') + '/' + parent.id;
        } else {
          this.model.path = null;
        }
        if (this.model.id) {
          if (this.model.id === this.model.pid) {
            message.error("不能引用当前分类")
            return
          }
        }
        this.loading = true;
        ProductCategory.save(this.model).then(() => {
          message("保存成功~");
          this.$emit('success');
        }).finally(() => this.loading = false);
      }
    },
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
      OssUpload('product', params).then(({data}) => {
        if (data) {
          this.model.imgPath = data;
        }
      })
    },
  },
  created() {
    this.categoryOption.datas = this.list || [];
    CopyObj(this.model, this.productCategory);
    if (this.parent) {
      this.model.pid = this.parent.id;
    }
  }
}
</script>
