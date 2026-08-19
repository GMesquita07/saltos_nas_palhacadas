const apiUrl = import.meta.env.VITE_API_URL ?? '/api/v1'

export async function apiClient<T>(path: string, options: RequestInit = {}, token?: string): Promise<T> {
  const response = await fetch(`${apiUrl}${path}`, { ...options, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers } })
  if (!response.ok) throw new Error(response.status === 401 ? 'Email ou palavra-passe inválidos.' : 'Não foi possível concluir a operação.')
  return response.status === 204 ? undefined as T : response.json() as Promise<T>
}

export async function uploadFile(file: File, token: string): Promise<{ url: string }> {
  const body = new FormData()
  body.append('file', file)
  const response = await fetch(`${apiUrl}/admin/media`, { method: 'POST', headers: { Authorization: `Bearer ${token}` }, body })
  if (!response.ok) throw new Error('Não foi possível enviar o ficheiro.')
  return response.json() as Promise<{ url: string }>
}
