const apiUrl = import.meta.env?.VITE_API_URL ?? '/api/v1'
const mib = 1024 * 1024
const maxImageUploadSize = 10 * mib
const maxVideoUploadSize = 30 * mib

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
  return uploadMultipart<{ url: string }>('/admin/media', file, token)
}

export async function uploadUserImage(file: File, token: string): Promise<{ id: string; url: string; contentType: string }> {
  return uploadUserMedia(file, token)
}

export async function uploadUserMedia(file: File, token: string): Promise<{ id: string; url: string; contentType: string }> {
  return uploadMultipart<{ id: string; url: string; contentType: string }>('/media', file, token)
}

export function validateUploadFileSize(file: Pick<File, 'size' | 'type'>) {
  if (isImage(file) && file.size > maxImageUploadSize) {
    throw new Error('A imagem não pode exceder 10 MB.')
  }
  if (isVideo(file) && file.size > maxVideoUploadSize) {
    throw new Error('O vídeo não pode exceder 30 MB.')
  }
  if (!isImage(file) && !isVideo(file) && file.size > maxVideoUploadSize) {
    throw new Error('O ficheiro não pode exceder 30 MB.')
  }
}

async function uploadMultipart<T>(path: string, file: File, token: string): Promise<T> {
  validateUploadFileSize(file)
  const body = new FormData()
  body.append('file', file)
  const response = await fetch(`${apiUrl}${path}`, { method: 'POST', headers: { Authorization: `Bearer ${token}` }, body })
  if (!response.ok) throw new Error(parseError(await response.text()) || 'Não foi possível enviar o ficheiro.')
  return response.json() as Promise<T>
}

function isImage(file: Pick<File, 'type'>) {
  return file.type.toLowerCase().startsWith('image/')
}

function isVideo(file: Pick<File, 'type'>) {
  return file.type.toLowerCase().startsWith('video/')
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
