<template>
  <div class="grant-panel">
    <div class="grant-panel__tip">
      页面节点=查看权限（勾选即菜单可见），其下子项为操作权限（新增/编辑、审核/反审核、删除）。勾选页面会连同子项一起授权（可再单独取消某个子项），子项也可单独勾选；取消页面则连带回收其下全部子项。变更均自动保存，无需额外确认
    </div>
    <div class="grant-panel__tree">
      <t-loading :loading="loading" show-overlay>
        <t-tree
            :data="treeData"
            :keys="{ value: 'id', label: 'label', children: 'children' }"
            checkable
            check-strictly
            value-mode="all"
            expand-all
            hover
            line
            transition
            :expand-on-click-node="false"
            :value="checkedKeys"
            :disable-check="disableCheck"
            @change="onCheckChange"
        >
          <template #label="{ node }">
            <span v-if="node.data && node.data.menuType === 'FUNCTION'" class="grant-panel__op">{{ node.data.name }}</span>
            <span v-else>{{ node.data && node.data.label }}</span>
          </template>
        </t-tree>
      </t-loading>
    </div>
  </div>
</template>

<script>
import Role from '@js/api/setting/Role';
import Menu from '@js/api/setting/Menu';
import { MessagePlugin } from 'tdesign-vue-next';
import { toArrayTree } from '@common/utils';

const GROUP_LABEL = { MERCHANT: '集团菜单' };

function mapMenus(list) {
  return (list || []).map((item) => {
    const group = GROUP_LABEL[item.menuGroup] || item.menuGroup || '';
    return {
      ...item,
      label: group ? `${group} / ${item.name}` : item.name
    };
  });
}

export default {
  name: 'GrantMenu',
  props: {
    entity: Object
  },
  data() {
    return {
      loading: false,
      treeData: [],
      checkedKeys: [],
      forcedKeys: [],
      saving: false,
      pending: null,
      // 结构索引（loadData 时基于扁平列表构建）
      nodeIds: new Set(),
      parentOf: new Map(),
      childrenOf: new Map(),
      menuIdsByDepth: []   // MENU 类型节点 id，浅→深
    };
  },
  methods: {
    disableCheck(node) {
      const data = node?.data || node;
      // 仅显式 false 才强制勾选不可取消（存量数据 require_auth 为 NULL 的行应可正常勾选）
      return data.requireAuth === false;
    },
    descendants(id) {
      const out = [];
      const stack = [...(this.childrenOf.get(id) || [])];
      while (stack.length) {
        const n = stack.pop();
        out.push(n.id);
        (this.childrenOf.get(n.id) || []).forEach((c) => stack.push(c));
      }
      return out;
    },
    onCheckChange(value) {
      const prev = new Set(this.checkedKeys);
      const next = new Set([...(value || []), ...this.forcedKeys]);
      // check-strictly 下父子互不联动，手工维护语义：
      // ⓪ 新勾选的页面/分组 → 连带勾中其全部后代（操作子项自动跟上，可再单独取消）
      (value || []).forEach((id) => {
        if (!prev.has(id)) {
          this.descendants(id).forEach((d) => next.add(d));
        }
      });
      // ① 取消的 MENU 节点（分组/页面）→ 其全部后代（含操作子项）连带取消，自顶向下
      for (const id of this.menuIdsByDepth) {
        if (!next.has(id) && !this.forcedKeys.includes(id)) {
          this.descendants(id).forEach((d) => next.delete(d));
        }
      }
      // ② 勾中的节点 → 向上补齐祖先链（勾操作必带页面与分组，否则菜单树断链）
      Array.from(next).forEach((id) => {
        let p = this.parentOf.get(id);
        while (p != null && this.nodeIds.has(p) && !next.has(p)) {
          next.add(p);
          p = this.parentOf.get(p);
        }
      });
      const menus = Array.from(next);
      // 与上一次生效的勾选集对比：区分「授权 / 取消授权 / 两者皆有」，无净变化不打扰
      const added = menus.some((id) => !prev.has(id));
      const removed = Array.from(prev).some((id) => !next.has(id));
      this.checkedKeys = menus;
      if (!added && !removed) return;
      this.saveMenus(menus, added, removed);
    },
    saveMenus(menus, added, removed) {
      // 请求进行中不丢状态：暂存最新一次，完成后自动补发（连点快速勾选/取消也能全部落库）
      if (this.saving) {
        this.pending = {menus, added, removed};
        return;
      }
      this.saving = true;
      this.loading = true;
      Role.saveMenuRole(this.entity.id, menus)
        .then(() => {
          if (added && removed) {
            MessagePlugin.success('授权已更新~');
          } else if (removed) {
            MessagePlugin.success('取消授权成功~');
          } else {
            MessagePlugin.success('授权成功~');
          }
        })
        .finally(() => {
          this.loading = false;
          this.saving = false;
          const queued = this.pending;
          this.pending = null;
          if (queued) {
            this.saveMenus(queued.menus, queued.added, queued.removed);
          }
        });
    },
    buildIndex(flatList) {
      this.nodeIds = new Set(flatList.map((v) => v.id));
      this.parentOf = new Map(flatList.map((v) => [v.id, v.parentId]));
      this.childrenOf = new Map();
      flatList.forEach((v) => {
        if (!this.childrenOf.has(v.parentId)) {
          this.childrenOf.set(v.parentId, []);
        }
        this.childrenOf.get(v.parentId).push(v);
      });
      // BFS 得到 MENU 节点 浅→深 顺序（FUNCTION 行是叶子，不参与级联维护）
      const ordered = [];
      const queue = flatList.filter((v) => v.parentId == null || !this.nodeIds.has(v.parentId));
      while (queue.length) {
        const n = queue.shift();
        if (n.menuType !== 'FUNCTION') {
          ordered.push(n.id);
        }
        (this.childrenOf.get(n.id) || []).forEach((c) => queue.push(c));
      }
      this.menuIdsByDepth = ordered;
    },
    loadData() {
      this.loading = true;
      Promise.all([Menu.merchantMenu('MERCHANT'), Role.getMenuRole(this.entity.id)])
        .then(([menuRes, roleRes]) => {
          const flatList = menuRes.data || [];
          this.buildIndex(flatList);
          this.treeData = toArrayTree(mapMenus(flatList), {
            key: 'id',
            parentKey: 'parentId',
            children: 'children'
          });
          this.forcedKeys = flatList.filter((val) => val.requireAuth === false).map((val) => val.id);

          // check-strictly 回显：授权表里有谁就勾谁（存量数据自带祖先 id，无需补叶子/父级换算）
          const granted = (roleRes.data || []).filter((id) => this.nodeIds.has(id));
          this.checkedKeys = Array.from(new Set([...this.forcedKeys, ...granted]));
        })
        .finally(() => {
          this.loading = false;
        });
    }
  },
  created() {
    this.loadData();
  }
};
</script>

<style scoped>
.grant-panel {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 55px);
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

.grant-panel__op {
  font-size: 12px;
  color: #8a94a6;
}
</style>
