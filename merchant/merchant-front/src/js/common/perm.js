import store from '@/js/store';

/**
 * 操作级权限判断。权限码形如 'salesOrder:edit' / 'salesOrder:audit' / 'salesOrder:delete'。
 * 商户主账号角色（systemDefault）全量放行；其余按登录时 /init 下发并回写会话的 granted 列表判断。
 *
 * @param {string|string[]} value 单个权限码，或「任一命中即可」的权限码数组
 */
export function hasPerm(value) {
  const role = store.state.user && store.state.user.role;
  if (role && role.systemDefault) {
    return true;
  }
  const granted = store.state.granted || [];
  if (Array.isArray(value)) {
    return value.some(v => granted.includes(v));
  }
  return typeof value === 'string' && granted.includes(value);
}
