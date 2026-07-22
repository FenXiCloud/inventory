<template>
  <div class="grant-panel">
    <div class="grant-panel__tip">勾选后自动保存模块授权</div>
    <div class="grant-panel__tree">
      <t-loading :loading="loading" show-overlay>
        <t-tree
            :data="treeData"
            :keys="{ value: 'id', label: 'label', children: 'children' }"
            checkable
            value-mode="all"
            expand-all
            hover
            line
            transition
            :expand-on-click-node="false"
            :value="checkedKeys"
            :disable-check="disableCheck"
            @change="onCheckChange"
        />
      </t-loading>
    </div>
  </div>
</template>

<script>
import Menu from '@js/api/Menu';
import { MessagePlugin } from 'tdesign-vue-next';
import { toArrayTree } from '@common/utils';

const MODULE_LABEL = { MERCHANT: '集团视角', CUSTOM: '客户视角', SUPPLIER: '货商视角' };
const GROUP_LABEL = { MERCHANT: '集团菜单', STORE: '门店菜单' };

function mapMenus(list) {
  return (list || []).map((item) => {
    const mod = MODULE_LABEL[item.menuModule] || item.menuModule || '';
    const group = GROUP_LABEL[item.menuGroup] || item.menuGroup || '';
    const prefix = [mod, group].filter(Boolean).join(' / ');
    return {
      ...item,
      label: prefix ? `${prefix} / ${item.name}` : item.name
    };
  });
}

function collectLeafIds(nodes, out = []) {
  (nodes || []).forEach((n) => {
    if (n.children && n.children.length) collectLeafIds(n.children, out);
    else out.push(n.id);
  });
  return out;
}

export default {
  name: 'MerchantModuleGrant',
  props: {
    merchant: Object
  },
  data() {
    return {
      loading: false,
      treeData: [],
      checkedKeys: [],
      forcedKeys: [],
      saving: false,
      params: {
        enabled: true
      }
    };
  },
  methods: {
    disableCheck(node) {
      const data = node?.data || node;
      return !data.requireAuth;
    },
    onCheckChange(value) {
      const next = Array.from(new Set([...(value || []), ...this.forcedKeys]));
      this.checkedKeys = next;
      this.saveMenus(next);
    },
    saveMenus(menus) {
      if (this.saving) return;
      this.saving = true;
      this.loading = true;
      Menu.grantMerchant({ menus, merchants: [this.merchant.id] })
        .then(() => {
          MessagePlugin.success('授权成功~');
        })
        .finally(() => {
          this.loading = false;
          this.saving = false;
        });
    },
    loadList() {
      this.loading = true;
      Promise.all([Menu.list(this.params), Menu.queryGrantMenu(this.merchant.id)])
        .then(([menuRes, grantRes]) => {
          const flatList = menuRes.data || [];
          this.treeData = toArrayTree(mapMenus(flatList), {
            key: 'id',
            parentKey: 'parentId',
            children: 'children'
          });
          this.forcedKeys = flatList.filter((val) => !val.requireAuth).map((val) => val.id);

          const granted = grantRes.data || [];
          const leafSet = new Set(collectLeafIds(this.treeData));
          const leafGranted = granted.filter((id) => leafSet.has(id));
          this.checkedKeys = Array.from(new Set([...this.forcedKeys, ...leafGranted]));
        })
        .finally(() => {
          this.loading = false;
        });
    }
  },
  created() {
    this.loadList();
  }
};
</script>

<style scoped>
.grant-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 420px;
  background: #fff;
  box-sizing: border-box;
}

.grant-panel__tip {
  flex-shrink: 0;
  padding: 10px 12px;
  font-size: 13px;
  color: #646a73;
  border-bottom: 1px solid #e7e7e7;
  background: #fafbfc;
}

.grant-panel__tree {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 12px 16px;
}
</style>
