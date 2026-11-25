export function resolveImageUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('/uploads/')) return `/api${url}`
  return url
}

/** Resolve media URL (cover / avatar) */
export function mediaUrl(url) {
  if (!url) return ''
  if (/^https?:\/\//i.test(url) || url.startsWith('data:')) return url
  if (url.startsWith('/uploads/')) return `/api${url}`
  if (url.startsWith('/')) return url
  return `/uploads/${url.replace(/^\/+/, '')}`
}
