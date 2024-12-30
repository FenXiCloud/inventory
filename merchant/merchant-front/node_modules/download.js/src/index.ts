export function downloadUrl(title: string, url: string) {
  const a = document.createElement('a')
  a.href = url
  a.download = title
  a.click()
}

export function downloadBlob(title: string, blob: Blob) {
  const url = URL.createObjectURL(blob)
  downloadUrl(title, url)
  URL.revokeObjectURL(url)
}

export function downloadText(title: string, content: string) {
  downloadBlob(title, new Blob([content], { type: 'octet/stream' }))
}
