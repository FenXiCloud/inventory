<template>
  <div class="product-page">
    <aside class="product-page__aside">
      <div class="product-category-panel">
        <div class="product-category-panel__hd">
          <span>分类名称</span>
          <t-link theme="primary" title="新增分类" @click="showProductCategoryForm()">
            <t-icon name="add"/>
          </t-link>
        </div>
        <div class="product-category-panel__bd">
          <t-tree
              :data="categoryTree"
              :keys="{ value: 'id', label: 'name', children: 'children' }"
              activable
              hover
              line
              expand-all
              transition
              :expand-on-click-node="false"
              :actived="selectedCategoryKeys"
              @active="onCategoryActive"
          >
            <template #operations="{ node }">
              <div v-if="node.value !== 'ALL'" class="product-category-ops" @click.stop>
                <!-- leaf=true 表示分类下有产品（末级），不可再创建下级 -->
                <t-link
                    v-if="!node.data.leaf"
                    theme="primary"
                    title="新增下级"
                    @click="showProductCategoryForm(null, node.data)"
                >
                  <t-icon name="add"/>
                </t-link>
                <t-link theme="primary" title="编辑" @click="showProductCategoryForm(node.data)">
                  <t-icon name="edit"/>
                </t-link>
                <t-link theme="primary" title="删除" @click="deleteProductCategory(node.data)">
                  <t-icon name="delete"/>
                </t-link>
              </div>
            </template>
          </t-tree>
        </div>
      </div>
    </aside>

    <section class="product-main">
      <div class="product-main__toolbar">
        <t-space break-line>
          <t-button
              v-auth="'product:edit'"
              theme="primary"
              style="border-radius: 4px"
              :disabled="!canAddProduct"
              :title="canAddProduct ? '' : '请先在最左侧选中最末级（无下级）分类，再新增产品'"
              @click="showProductForm()">新 增</t-button>
          <t-button v-auth="'product:edit'" style="border-radius: 4px" @click="showProductImportForm()">导 入</t-button>
          <t-input
              v-model="params.filter"
              clearable
              placeholder="请输入名称/编码/拼音"
              style="width: 240px; background: #fff; border-radius: 4px"
              @enter="searchProduct"
          >
            <template #suffixIcon>
              <t-icon name="search" style="cursor:pointer" @click="searchProduct"/>
            </template>
          </t-input>
          <t-input
              v-model="params.brand"
              clearable
              placeholder="请输入品牌"
              style="width: 160px; background: #fff; border-radius: 4px"
              @enter="searchProduct"
          >
            <template #suffixIcon>
              <t-icon name="search" style="cursor:pointer" @click="searchProduct"/>
            </template>
          </t-input>
          <t-button theme="primary" variant="outline" style="border-radius: 4px" :loading="loading" @click="searchProduct">查询</t-button>
        </t-space>
      </div>

      <div class="product-main__table">
        <t-table
            row-key="id"
            size="medium"
            bordered
            stripe
            hover
            max-height="100%"
            table-layout="fixed"
            :data="productDataList"
            :columns="productColumns"
            :loading="loading"
        >
          <template #ops="{ row }">
            <t-space size="small">
              <t-link v-auth="'product:edit'" theme="primary" @click="showProductForm(row)"><t-icon name="edit"/></t-link>
              <t-link v-auth="'product:edit'" theme="primary" @click="copyProduct(row)">复制</t-link>
              <t-link v-auth="'product:delete'" theme="primary" @click="deleteProduct(row)"><t-icon name="delete"/></t-link>
            </t-space>
          </template>
          <template #enabled="{ row }">
            <t-tag
                v-auth="'product:edit'"
                :theme="row.enabled ? 'primary' : 'danger'"
                variant="light"
                style="cursor:pointer"
                @click="trigger(row)"
            >
              {{ row.enabled ? '启用' : '禁用' }}
            </t-tag>
          </template>
        </t-table>
      </div>

      <div class="product-main__pager">
        <t-pagination
            v-model:current="pagination.page"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :show-jumper="true"
            :show-page-size="true"
            :popup-props="{ attach: 'body' }"
            @change="onPageChange"
        />
      </div>
    </section>
  </div>
</template>

<script>
import ProductForm from "./ProductForm.vue";
import ProductImportForm from "./ProductImportForm.vue";
import Product from "@js/api/basic/Product";
import {MessagePlugin} from "tdesign-vue-next";
import {DialogPlugin} from '@common/dialog-plugin';
import {openDialog, closeDialog} from '@common/dialog';
import {h} from "vue";
import ProductCategoryForm from "@views/basic/ProductCategoryForm.vue";
import ProductCategory from "@js/api/basic/ProductCategory";
import {toArrayTree} from '@common/utils';
export default {
  name: "ProductList",
  components: {ProductForm},
  data() {
    return {
      loading: false,
      params: {
        filter: null,
        brand: null,
        productCategoryId: null
      },
      productDataList: [],
      productCategoryDataList: [],
      selectedCategoryKeys: ['ALL'],
      pagination: {
        page: 1,
        pageSize: 10,
        total: 0
      },
      productColumns: [
        {colKey: 'ops', title: '操作', width: 110, fixed: 'left', align: 'center'},
        {colKey: 'code', title: '编码', width: 80},
        {colKey: 'name', title: '产品名称', minWidth: 160, ellipsis: true},
        {colKey: 'productCategoryName', title: '分类', width: 100},
        {colKey: 'specification', title: '规格', width: 100},
        {colKey: 'brand', title: '品牌', width: 90},
        {colKey: 'unitName', title: '单位', width: 80},
        {colKey: 'purchasePrice', title: '参考进价', width: 90},
        {colKey: 'stockQuantity', title: '当前库存', width: 90},
        {colKey: 'remarks', title: '备注', ellipsis: true},
        {colKey: 'sort', title: '排序号', width: 80},
        {colKey: 'enabled', title: '状态', width: 90, fixed: 'right', align: 'center'}
      ]
    }
  },
  computed: {
    categoryTree() {
      const prune = (nodes) => (nodes || []).map((node) => {
        const children = prune(node.children || []);
        const next = {
          id: node.id,
          name: node.name,
          pid: node.pid,
          // leaf：后端按「分类下是否有产品」计算
          leaf: !!node.leaf
        };
        if (children.length) next.children = children;
        return next;
      });
      const list = (this.productCategoryDataList || [])
        .filter((item) => item.id !== 'ALL')
        .map((item) => {
          const rawPid = item.pid ?? item.parentId;
          return {
            id: item.id,
            name: item.name,
            leaf: !!item.leaf,
            pid: rawPid == null || rawPid === '' || rawPid === 0 || rawPid === '0' ? null : rawPid
          };
        });
      const tree = prune(toArrayTree(list, {key: 'id', parentKey: 'pid', children: 'children'}));
      return [{id: 'ALL', name: '全部分类', leaf: false}, ...tree];
    },
    queryParams() {
      return Object.assign({}, this.params, {
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
      })
    },
    /** 仅当左侧选中了最末级(无下级)分类时，才允许新增产品 */
    canAddProduct() {
      const cid = this.params.productCategoryId;
      if (cid == null || cid === '') return false; // 全部分类/未选中
      const rawPid = (item) => {
        const p = item.pid ?? item.parentId;
        return (p == null || p === '' || p === 0 || p === '0') ? null : String(p);
      };
      const key = String(cid);
      // 该分类被其它分类当作父级 => 有下级，非末级
      return !(this.productCategoryDataList || []).some((item) => rawPid(item) === key);
    }
  },
  methods: {
    onCategoryActive(value) {
      const key = (value && value.length ? value[0] : 'ALL');
      this.selectedCategoryKeys = [key];
      this.params.productCategoryId = key === 'ALL' ? null : key;
      this.pagination.page = 1;
      this.loadProduct();
    },
    onPageChange(pageInfo) {
      this.pagination.page = pageInfo.current;
      this.pagination.pageSize = pageInfo.pageSize;
      this.loadProduct();
    },
    trigger(row) {
      let enabled = !row.enabled;
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认要「${enabled ? "启用" : "禁用"}」名称：${row.name}?`,
        onConfirm: () => {
          Product.update({id: row.id, enabled}).then(() => {
            MessagePlugin.success("操作成功~");
            this.loadProduct();
          })
        }
      })
    },
    showProductCategoryForm(productCategory, parent) {
      // 末级=分类下有产品，有产品时不允许创建下级
      if (parent?.leaf) {
        MessagePlugin.warning('该分类下已有产品，属于末级，不能创建下级');
        return;
      }
      let dialogId = openDialog({
        header: "产品分类",
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '400px',
        body: h(ProductCategoryForm, {
          productCategory, parent,
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.loadProductCategory();
            closeDialog(dialogId);
          }
        })
      });
    },
    deleteProductCategory(row) {
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认删除产品分类：${row.name}?`,
        onConfirm: () => {
          ProductCategory.remove(row.id).then(() => {
            MessagePlugin.success("删除成功~");
            this.loadProductCategory();
          })
        }
      })
    },
    loadProductCategory() {
      ProductCategory.select().then(({data}) => {
        this.productCategoryDataList = data || [];
        this.selectedCategoryKeys = ['ALL'];
        this.params.productCategoryId = null;
      });
    },
    searchProduct() {
      this.pagination.page = 1;
      this.loadProduct();
    },
    showProductForm(entity) {
      // 新增且已选中末级分类时，自动把该分类带进产品表单
      if (!entity && this.params.productCategoryId != null) {
        entity = {productCategoryId: this.params.productCategoryId};
      }
      let dialogId = openDialog({
        header: "产品信息",
        closeOnOverlayClick: false,
        closeBtn: false,
        width: '1000px',
        body: h(ProductForm, {
          entity,
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.searchProduct();
            closeDialog(dialogId);
          }
        })
      });
    },
    copyProduct(row) {
      this.showProductForm({
        ...row,
        id: null,
        code: '',
        name: row.name + '（副本）',
      });
    },
    showProductImportForm() {
      let dialogId = openDialog({
        header: "商品导入",
        closeOnOverlayClick: false,
        width: '50vw',
        body: h(ProductImportForm, {
          onClose: () => closeDialog(dialogId),
          onSuccess: () => {
            this.loadProductCategory();
            this.searchProduct();
            closeDialog(dialogId);
          }
        })
      });
    },
    deleteProduct(row) {
      DialogPlugin.confirm({
        header: "系统提示",
        body: `确认删除产品：${row.name}?`,
        onConfirm: () => {
          Product.remove(row.id).then(() => {
            MessagePlugin.success("删除成功~");
            this.loadProduct();
          })
        }
      })
    },
    loadProduct() {
      this.loading = true;
      Product.list(this.queryParams).then(({data: {results, total}}) => {
        this.productDataList = results || [];
        this.pagination.total = total;
      }).finally(() => this.loading = false);
    },
  },
  created() {
    this.loadProductCategory();
    this.loadProduct();
  }
}
</script>

<style scoped>
.product-page {
  height: 100%;
  min-height: 0;
  display: flex;
  gap: 5px;
  overflow: hidden;
  box-sizing: border-box;
}

.product-page__aside {
  width: 260px;
  flex-shrink: 0;
  min-height: 0;
  background: #fff;
  border-radius: 4px;
  overflow: hidden;
}

.product-category-panel {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.product-category-panel__hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid var(--td-component-border, #dcdcdc);
  font-weight: 600;
  flex-shrink: 0;
}

.product-category-panel__bd {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 4px 5px;
}

.product-category-ops {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.product-main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  background: #fff;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  padding: 0 12px;
  box-sizing: border-box;
  overflow: hidden;
}

.product-main__toolbar {
  flex-shrink: 0;
  padding: 8px 0;
}

.product-main__table {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
}

.product-main__pager {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid var(--td-component-border, #dcdcdc);
  background: #fff;
}
</style>
