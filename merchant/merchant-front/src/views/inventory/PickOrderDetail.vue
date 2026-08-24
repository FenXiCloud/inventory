<template>
  <div class="pick-order-detail" v-if="data">
    <div class="pick-header">
      <div class="pick-title">
        <h3>{{ data.locationType === 'WHOLE' ? '整货区拣货单' : '零货区拣货单' }}</h3>
        <t-tag :theme="data.locationType === 'WHOLE' ? 'primary' : 'warning'" variant="light">
          {{ data.locationType === 'WHOLE' ? '整货区' : '零货区' }}
        </t-tag>
      </div>
      <div class="pick-info">
        <div class="info-item">
          <span class="label">拣货单号：</span>
          <span class="value">{{ data.orderNo }}</span>
        </div>
        <div class="info-item">
          <span class="label">关联单号：</span>
          <span class="value">{{ data.sourceNo }}</span>
        </div>
        <div class="info-item">
          <span class="label">拣货日期：</span>
          <span class="value">{{ data.pickDate }}</span>
        </div>
        <div class="info-item">
          <span class="label">状态：</span>
          <t-tag :theme="statusTheme(data.status)" variant="light" size="small">
            {{ statusText(data.status) }}
          </t-tag>
        </div>
      </div>
    </div>

    <div class="pick-table">
      <t-table
          :data="data.items || []"
          :columns="columns"
          bordered
          stripe
          size="small"
      >
        <template #isCase="{ row }">
          <t-tag :theme="row.isCase === 1 ? 'primary' : 'warning'" variant="light" size="small">
            {{ row.isCase === 1 ? '整件' : '零货' }}
          </t-tag>
        </template>
        <template #actualQuantity="{ row }">
          <t-input-number
              v-if="data.status === 1"
              :value="row.actualQuantity"
              :min="0"
              :decimal-places="2"
              size="small"
              style="width: 100px"
              @change="(val) => updateActual(row.id, val)"
          />
          <span v-else>{{ row.actualQuantity || '-' }}</span>
        </template>
      </t-table>
    </div>

    <div class="pick-footer">
      <div class="pick-summary">
        <span>共 {{ (data.items || []).length }} 种商品</span>
      </div>
      <div class="pick-actions">
        <t-button theme="primary" variant="outline" @click="printPickOrder">
          <template #icon><t-icon name="print" /></template>
          打印拣货单
        </t-button>
        <t-button variant="outline" @click="$emit('close')">关 闭</t-button>
      </div>
    </div>

    <!-- 打印区域 -->
    <div ref="printArea" class="print-area" v-show="isPrinting">
      <div class="print-content">
        <div class="print-header">
          <h2>{{ data.locationType === 'WHOLE' ? '整货区拣货单' : '零货区拣货单' }}</h2>
          <div class="print-info">
            <div>拣货单号：{{ data.orderNo }}</div>
            <div>关联单号：{{ data.sourceNo }}</div>
            <div>拣货日期：{{ data.pickDate }}</div>
          </div>
        </div>
        <table class="print-table">
          <thead>
            <tr>
              <th>序号</th>
              <th>商品编码</th>
              <th>商品名称</th>
              <th>规格</th>
              <th>货位</th>
              <th>应拣数量</th>
              <th>实拣数量</th>
              <th>备注</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in data.items" :key="item.id">
              <td>{{ index + 1 }}</td>
              <td>{{ item.productCode }}</td>
              <td>{{ item.productName }}</td>
              <td>{{ item.specification }}</td>
              <td>{{ item.locationCode }}</td>
              <td>{{ item.planQuantity }}</td>
              <td>{{ item.actualQuantity }}</td>
              <td>{{ item.remark }}</td>
            </tr>
          </tbody>
        </table>
        <div class="print-footer">
          <div>拣货人：________________</div>
          <div>日期：________________</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {MessagePlugin} from "tdesign-vue-next";
import PickOrder from "@js/api/inventory/PickOrder";

export default {
  name: "PickOrderDetail",
  props: {
    data: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      isPrinting: false,
      columns: [
        {colKey: 'serial-number', title: '序号', width: 60},
        {colKey: 'productCode', title: '商品编码', width: 120},
        {colKey: 'productName', title: '商品名称', width: 150},
        {colKey: 'specification', title: '规格', width: 100},
        {colKey: 'locationCode', title: '货位', width: 100},
        {colKey: 'isCase', title: '类型', width: 80, align: 'center'},
        {colKey: 'planQuantity', title: '应拣数量', width: 100, align: 'right'},
        {colKey: 'caseQuantity', title: '整件数', width: 100, align: 'right'},
        {colKey: 'actualQuantity', title: '实拣数量', width: 120, align: 'center'},
        {colKey: 'remark', title: '备注', minWidth: 100}
      ]
    };
  },
  methods: {
    statusText(status) {
      const map = {0: '待拣货', 1: '拣货中', 2: '已完成'};
      return map[status] || '未知';
    },
    statusTheme(status) {
      const map = {0: 'warning', 1: 'primary', 2: 'success'};
      return map[status] || 'default';
    },
    updateActual(itemId, value) {
      PickOrder.updateActualQuantity(itemId, value).then(() => {
        MessagePlugin.success('已更新');
      });
    },
    printPickOrder() {
      this.isPrinting = true;
      this.$nextTick(() => {
        const printWindow = window.open('', '_blank');
        printWindow.document.write(`
          <html>
            <head>
              <title>拣货单</title>
              <style>
                body { font-family: Arial, sans-serif; font-size: 12px; }
                .print-content { padding: 20px; }
                .print-header { text-align: center; margin-bottom: 20px; }
                .print-header h2 { margin: 0 0 10px; }
                .print-info { display: flex; justify-content: space-between; margin-bottom: 10px; }
                .print-table { width: 100%; border-collapse: collapse; }
                .print-table th, .print-table td { border: 1px solid #000; padding: 5px; text-align: center; }
                .print-table th { background-color: #f0f0f0; }
                .print-footer { margin-top: 30px; display: flex; justify-content: space-between; }
              </style>
            </head>
            <body>
              ${this.$refs.printArea.innerHTML}
            </body>
          </html>
        `);
        printWindow.document.close();
        printWindow.print();
        this.isPrinting = false;
      });
    }
  }
};
</script>

<style scoped>
.pick-order-detail {
  padding: 16px;
}

.pick-header {
  margin-bottom: 16px;
}

.pick-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.pick-title h3 {
  margin: 0;
}

.pick-info {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.label {
  color: #666;
}

.pick-table {
  margin-bottom: 16px;
}

.pick-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pick-actions {
  display: flex;
  gap: 12px;
}

.print-area {
  display: none;
}
</style>
