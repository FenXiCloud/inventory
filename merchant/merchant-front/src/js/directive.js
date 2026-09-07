import { hasPerm } from '@common/perm';

export default {
  mounted(el, binding) {
    const { value } = binding;
    const valid = (typeof value === 'string' && value.length > 0)
        || (Array.isArray(value) && value.length > 0);
    if (!valid) {
      throw new Error(`need permission! Like v-auth="'product:edit'" or v-auth="['salesOrder:audit']"`);
    }
    if (!hasPerm(value)) {
      el.parentNode && el.parentNode.removeChild(el);
    }
  }
};
