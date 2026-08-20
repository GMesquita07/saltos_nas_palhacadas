import { apiClient } from './apiClient'
import type { AuthSession, AuthUser, UserRole } from '../types/auth'

type TokenResponse = {
  accessToken: string
  role: UserRole
}

type Credentials = {
  email: string
  password: string
}

export async function login(credentials: Credentials): Promise<AuthSession> {
  const response = await apiClient<TokenResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  })

  return toSession(response, credentials.email)
}

export async function register(credentials: Credentials): Promise<AuthSession> {
  const response = await apiClient<TokenResponse>('/auth/register', {
    method: 'POST',
    body: JSON.stringify(credentials),
  })

  return toSession(response, credentials.email)
}

export function getCurrentUser(token: string): Promise<AuthUser> {
  return apiClient<AuthUser>('/auth/me', {}, token)
}

function toSession(response: TokenResponse, email: string): AuthSession {
  return {
    token: response.accessToken,
    email: email.trim().toLowerCase(),
    role: response.role,
  }
}
