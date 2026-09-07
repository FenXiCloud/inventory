<template>
  <div class="crp">
    <div class="crp__head">
      <span class="crp__head-title">编码效果预览</span>
      <t-tag size="small" variant="light" :title="data && data.seedUsed ? '当前周期已用到 ' + (parseInt(data.serialPart) - 1) + '，下一号接续' : '当前周期尚未使用，从起始值开始'">
        {{ rule && rule.documentType ? rule.documentType : '未选类型' }}
      </t-tag>
    </div>

    <div v-if="loading" class="crp__loading">
      <t-loading size="small" text="预览计算中…" />
    </div>

    <div v-else-if="!data" class="crp__empty">选择单据类型后，这里会实时显示按规则拼装出的编码示例</div>

    <template v-else>
      <!-- 组成公式：前缀 + 格式化 + 流水号 -->
      <div class="crp__segments">
        <div class="crp__seg">
          <div class="crp__seg-label">规则前缀</div>
          <div class="crp__seg-value crp__seg-value--prefix">{{ data.prefixPart || '（无）' }}</div>
        </div>
        <div class="crp__seg-plus">+</div>
        <div class="crp__seg">
          <div class="crp__seg-label">{{ isDocumentType ? '日期（今日）' : '格式化' }}</div>
          <div class="crp__seg-value crp__seg-value--middle">{{ data.middlePart || '（无）' }}</div>
        </div>
        <div class="crp__seg-plus">+</div>
        <div class="crp__seg">
          <div class="crp__seg-label">流水号（{{ (rule.serialNumberLength || '') + '位' }}）</div>
          <div class="crp__seg-value crp__seg-value--serial">{{ data.serialPart }}</div>
        </div>
      </div>

      <!-- 下一号 -->
      <div class="crp__main">
        <div class="crp__main-label">下一张新单将生成</div>
        <div class="crp__main-code">{{ data.nextCode }}</div>
      </div>

      <!-- 连号示例 -->
      <div class="crp__rows">
        <div class="crp__row" v-if="data.samples && data.samples.length > 1">
          <span class="crp__row-label">再往后两单</span>
          <span class="crp__row-codes">
            <code v-for="(s, i) in data.samples.slice(1)" :key="i">{{ s }}</code>
          </span>
        </div>
        <div class="crp__row">
          <span class="crp__row-label">本周期首单</span>
          <code class="crp__row-codes">{{ data.firstCode }}</code>
        </div>
        <div class="crp__row">
          <span class="crp__row-label">清零规则</span>
          <span class="crp__row-text">{{ data.resetHint }}</span>
        </div>
      </div>

      <!-- 特殊场景提示 -->
      <div class="crp__note crp__note--warn" v-if="literalFormatWarn">
        <t-icon name="error-circle" size="14px" />
        <span>基础资料类型的格式化按字面量拼入编码，不会解析为日期（日期样式「{{ rule.format }}」将原样出现在编码中）</span>
      </div>
      <div class="crp__note" v-if="basicNote">
        <t-icon name="info-circle" size="14px" />
        <span>{{ basicNote }}</span>
      </div>
    </template>
  </div>
</template>

<script>
/**
 * 编码规则效果预览（纯展示组件，编码拼装由 /codeRule/preview 服务端计算，与真实取号同源）。
 * props:
 *   rule  - 草稿规则字段 {documentType, serialNumberLength, ...}
 *   data  - 服务端 preview 返回（prefixPart/middlePart/serialPart/nextCode/samples/firstCode/seedUsed/resetHint）
 *   loading
 */
const RANDOM_CODE_TYPES = ['仓库', '客户', '供货商'];
// 与后端 CodeSeedService.BASIC_DATA_TYPES 一致：这四类 format 为字面量，其余单据类型按日期模式解析
const BASIC_DATA_TYPES = ['商品', '仓库', '客户', '供货商'];

export default {
  name: 'CodeRulePreview',
  props: {
    rule: {type: Object, default: () => ({})},
    data: {type: Object, default: null},
    loading: {type: Boolean, default: false}
  },
  computed: {
    isDocumentType() {
      const t = this.rule.documentType;
      return !!t && !BASIC_DATA_TYPES.includes(t);
    },
    literalFormatWarn() {
      const fmt = this.rule.format || '';
      return !this.isDocumentType && /y|M|d/.test(fmt);
    },
    basicNote() {
      const t = this.rule.documentType;
      if (t === '商品') {
        return '商品编码实际按本规则拼接，但流水号接续现有商品条数，可能与预览序号不同';
      }
      if (RANDOM_CODE_TYPES.includes(t)) {
        return t + '编码目前由系统随机生成 8 位数字，此规则暂不参与实际编号';
      }
      return '';
    }
  }
};
</script>

<style scoped>
.crp { width: 100%; }

.crp__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.crp__head-title { font-size: 14px; font-weight: 600; color: #303133; }

.crp__loading { padding: 24px 0; text-align: center; }
.crp__empty {
  padding: 24px 12px;
  text-align: center;
  font-size: 12px;
  color: #a8abb2;
  background: #f7f8fa;
  border-radius: 6px;
}

.crp__segments {
  display: flex;
  align-items: stretch;
  gap: 6px;
  margin-bottom: 12px;
}
.crp__seg {
  flex: 1;
  min-width: 0;
  background: #f7f8fa;
  border-radius: 6px;
  padding: 8px 10px;
  text-align: center;
}
.crp__seg-label { font-size: 12px; color: #909399; margin-bottom: 4px; white-space: nowrap; }
.crp__seg-value {
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.crp__seg-value--prefix { color: #0052d9; }
.crp__seg-value--middle { color: #2ba471; }
.crp__seg-value--serial { color: #e37318; }
.crp__seg-plus {
  align-self: center;
  color: #c0c4cc;
  font-size: 14px;
  flex-shrink: 0;
}

.crp__main {
  background: #f2f4f7;
  border-radius: 6px;
  padding: 12px 16px;
  margin-bottom: 12px;
}
.crp__main-label { font-size: 12px; color: #909399; margin-bottom: 4px; }
.crp__main-code {
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  letter-spacing: 1px;
  word-break: break-all;
}

.crp__rows { margin-bottom: 8px; }
.crp__row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 0;
  font-size: 12px;
}
.crp__row-label { flex-shrink: 0; width: 78px; color: #909399; }
.crp__row-codes { display: inline-flex; gap: 8px; flex-wrap: wrap; }
.crp__row-codes code,
.crp__row code {
  font-family: Consolas, Monaco, 'Courier New', monospace;
  background: #f7f8fa;
  border-radius: 4px;
  padding: 2px 8px;
  color: #606266;
  font-size: 12px;
}
.crp__row-text { color: #606266; }

.crp__note {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 12px;
  color: #909399;
  background: #f7f8fa;
  border-radius: 6px;
  padding: 8px 10px;
  margin-top: 8px;
  line-height: 1.5;
}
.crp__note--warn { color: #d54616; background: #fff3e8; }
.crp__note .t-icon { margin-top: 2px; flex-shrink: 0; }
</style>
