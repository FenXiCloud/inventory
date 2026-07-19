/** v-width="90" → style.width */
function apply(el, binding) {
  const val = binding.value
  if (val == null || val === '') return
  el.style.width = typeof val === 'number' ? `${val}px` : String(val)
}

export default {
  mounted: apply,
  updated: apply
}
