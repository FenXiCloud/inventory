<template>
  <div class="simple-page">
    <div class="simple-page__toolbar">
      <t-space break-line>
        <t-button theme="primary" variant="outline" :loading="loading" @click="load">刷新</t-button>
      </t-space>
    </div>

    <div class="quota-cards">
      <div class="quota-card">
        <div class="quota-card__label">开票总限额</div>
        <div class="quota-card__value">¥{{ fmt(totalQuota) }}</div>
      </div>
      <div class="quota-card">
        <div class="quota-card__label">已使用额度</div>
        <div class="quota-card__value quota-card__value--warning">¥{{ fmt(usedQuota) }}</div>
      </div>
      <div class="quota-card">
        <div class="quota-card__label">剩余额度</div>
        <div class="quota-card__value quota-card__value--blue">¥{{ fmt(remainingQuota) }}</div>
      </div>
      <div class="quota-card">
        <div class="quota-card__label">累计开票张数</div>
        <div class="quota-card__value">{{ invoiceCount }} 张</div>
      </div>
    </div>

    <div class="quota-hint">剩余额度为税控平台返回的可用开票金额，实际开票时以平台实时额度为准。</div>
  </div>
</template>

<script>
import Invoice from '@js/api/invoice/Invoice';

export default {
  name: 'InvoiceQuota',
  data() {
    return {
      loading: false,
      totalQuota: 0,
      usedQuota: 0,
      remainingQuota: 0,
      invoiceCount: 0
    };
  },
  methods: {
    load() {
      this.loading = true;
      Invoice.stats()
        .then(({data}) => {
          if (data) {
            this.totalQuota = data.totalQuota || 0;
            this.usedQuota = data.usedQuota || 0;
            this.remainingQuota = data.remainingQuota || 0;
            this.invoiceCount = data.invoiceCount || 0;
          }
        })
        .catch(() => {})
        .finally(() => (this.loading = false));
    },
    fmt(v) {
      const n = Number(v) || 0;
      return n.toLocaleString('zh-CN', {minimumFractionDigits: 2, maximumFractionDigits: 2});
    }
  },
  created() {
    this.load();
  }
};
</script>

<style scoped>
.quota-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  padding: 4px 0;
}

.quota-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 20px 22px;
  box-sizing: border-box;
}

.quota-card__label {
  font-size: 13px;
  color: #646a73;
  margin-bottom: 10px;
}

.quota-card__value {
  font-size: 24px;
  font-weight: 600;
  color: #1f2329;
  line-height: 1;
}

.quota-card__value--warning {
  color: #ed7b2f;
}

.quota-card__value--blue {
  color: #0052d9;
}

.quota-hint {
  margin-top: 12px;
  padding: 8px 12px;
  border-radius: 4px;
  background: #f3f3f3;
  color: #555;
  font-size: 13px;
}
</style>
