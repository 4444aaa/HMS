/** 与后端单号规则一致：前缀 + yyyyMMdd + 6 位随机数 */
export function previewDocumentNo (prefix) {
  const d = new Date()
  const dateStr = `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}${String(d.getDate()).padStart(2, '0')}`
  const randomStr = String(Math.floor(Math.random() * 1000000)).padStart(6, '0')
  return prefix + dateStr + randomStr
}

/** 表单展示用创建时间（打开新增时固定） */
export function formatDraftCreateTime () {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}
