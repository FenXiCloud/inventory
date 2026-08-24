import Ajax from '@common/Request';

export default {
  overview() {
    return Ajax.get('/dashboard/overview');
  },
};
