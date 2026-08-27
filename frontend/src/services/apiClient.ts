const apiUrl = import.meta.env.VITE_API_URL ?? '/api/v1'

export async function apiClient<T>(path: string, options: RequestInit = {}, token?: string): Promise<T> {
  const response = await fetch(`${apiUrl}${path}`, { ...options, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers } })
  if (!response.ok) {
    const body = await response.text()
    const detail = parseError(body)
    if (response.status === 401) {
      throw new Error(path === '/auth/login'
        ? 'Email ou palavra-passe inválidos.'
        : 'A tua sessão terminou. Inicia sessão novamente.')
    }
    throw new Error(detail || `Não foi possível concluir a operação (${response.status}).`)
  }
  if (response.status === 204) return undefined as T
  const body = await response.text()
  return body ? JSON.parse(body) as T : undefined as T
}

export async function uploadFile(file: File, token: string): Promise<{ url: string }> {
  const body = new FormData()
  body.append('file', file)
  const response = await fetch(`${apiUrl}/admin/media`, { method: 'POST', headers: { Authorization: `Bearer ${token}` }, body })
  if (!response.ok) throw new Error(parseError(await response.text()) || 'Não foi possível enviar o ficheiro.')
  return response.json() as Promise<{ url: string }>
}

export async function uploadUserImage(file: File, token: string): Promise<{ url: string }> {
  return uploadUserMedia(file, token)
}

export async function uploadUserMedia(file: File, token: string): Promise<{ url: string; contentType: string }> {
  const body = new FormData()
  body.append('file', file)
  const response = await fetch(`${apiUrl}/media`, { method: 'POST', headers: { Authorization: `Bearer ${token}` }, body })
  if (!response.ok) throw new Error(parseError(await response.text()) || 'Não foi possível enviar o ficheiro.')
  return response.json() as Promise<{ url: string; contentType: string }>
}

export async function uploadClientContentMedia(file: File, token: string): Promise<{ url: string; contentType: string }> {
  const body = new FormData()
  body.append('file', file)
  const response = await fetch(`${apiUrl}/client-posts/media`, { method: 'POST', headers: { Authorization: `Bearer ${token}` }, body })
  if (!response.ok) throw new Error(parseError(await response.text()) || 'Não foi possível enviar o ficheiro.')
  return response.json() as Promise<{ url: string; contentType: string }>
}

function parseError(body: string) {
  try {
    const error = JSON.parse(body) as {
      detail?: string
      message?: string
      title?: string
      errors?: Record<string, string>
    }
    const fieldErrors = error.errors ? Object.values(error.errors).filter(Boolean) : []
    return fieldErrors.join(' ') || error.detail || error.message || error.title
  } catch { return body || undefined }
}
