import { useEffect, useState } from 'react'

export function useAuthenticatedMediaUrl(url?: string, token?: string) {
  const [privateMedia, setPrivateMedia] = useState<{ sourceUrl: string, objectUrl: string } | null>(null)

  useEffect(() => {
    if (!url || !isPrivateMediaUrl(url) || !token) {
      return
    }
    const privateUrl = url

    const controller = new AbortController()
    let isCurrent = true
    let objectUrl: string | undefined

    void fetch(privateUrl, {
      headers: { Authorization: `Bearer ${token}` },
      signal: controller.signal,
    })
      .then((response) => {
        if (!response.ok) throw new Error('private media unavailable')
        return response.blob()
      })
      .then((blob) => {
        if (!isCurrent) return
        objectUrl = URL.createObjectURL(blob)
        setPrivateMedia({ sourceUrl: privateUrl, objectUrl })
      })
      .catch(() => undefined)

    return () => {
      isCurrent = false
      controller.abort()
    }
  }, [token, url])

  useEffect(() => {
    const objectUrl = privateMedia?.objectUrl
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [privateMedia?.objectUrl])

  if (!url) return undefined
  if (!isPrivateMediaUrl(url)) return url
  if (!token) return undefined
  return privateMedia?.sourceUrl === url ? privateMedia.objectUrl : undefined
}

function isPrivateMediaUrl(url?: string) {
  return Boolean(url?.includes('/api/v1/private-media/') || url?.includes('/api/v1/auth/me/avatar'))
}
