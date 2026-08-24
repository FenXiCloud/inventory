<template>
  <OrderPaymentList :preset-type="2"/>
</template>
<script>
import OrderPaymentList from '@views/fund/OrderPaymentList.vue';
export default { name: 'AdvancePaymentList', components: { OrderPaymentList } };
</script>
