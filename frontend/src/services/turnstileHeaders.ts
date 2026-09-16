export const turnstileHeaderName = 'X-Turnstile-Token'

export function turnstileHeaders(token?: string): Record<string, string> {
  const normalizedToken = token?.trim()
  return normalizedToken ? { [turnstileHeaderName]: normalizedToken } : {}
}
