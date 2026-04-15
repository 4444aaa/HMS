/** 处方状态：0 待提交 1 待取药 2 已取药 */
export function prescriptionStatusLabel (status) {
  if (status === 0) return '待提交'
  if (status === 1) return '待取药'
  if (status === 2) return '已取药'
  return '未知'
}

export function prescriptionStatusTagType (status) {
  if (status === 0) return 'info'
  if (status === 1) return 'warning'
  if (status === 2) return 'success'
  return ''
}
