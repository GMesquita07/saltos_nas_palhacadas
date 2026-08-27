import { apiClient } from './apiClient'
import type { AuthSession, AuthUser, UserRole } from '../types/auth'

type TokenResponse = {
  accessToken: string
  email: string
  username?: string | null
  firstName?: string | null
  lastName?: string | null
  phone?: string | null
  profileImageUrl?: string | null
  profileImagePosition?: string | null
  profileImageZoom?: number | null
  role: UserRole
}

type Credentials = {
  email: string
  password: string
}

export type RegisterCredentials = Credentials & {
  username: string
  firstName: string
  lastName: string
  phone: string
}

export type UpdateAccountInput = {
  username: string
  firstName: string
  lastName: string
  phone: string
  profileImageUrl: string | null
  profileImageMediaId?: string | null
  profileImagePosition: string
  profileImageZoom: number
}

export type ChangePasswordInput = {
  currentPassword: string
  newPassword: string
}

export type AccountDataExport = {
  exportedAt: string
  profile: Record<string, unknown>
  bookings: unknown[]
  clientContent: unknown[]
  favorites: unknown[]
  reviews: unknown[]
}

export async function login(credentials: Credentials): Promise<AuthSession> {
  const response = await apiClient<TokenResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  })

  return toSession(response, credentials.email)
}

export async function register(credentials: RegisterCredentials): Promise<AuthSession> {
  const response = await apiClient<TokenResponse>('/auth/register', {
    method: 'POST',
    body: JSON.stringify(credentials),
  })

  return toSession(response, credentials.email)
}

export function getCurrentUser(token: string): Promise<AuthUser> {
  return apiClient<AuthUser>('/auth/me', {}, token)
}

export function updateCurrentUser(input: UpdateAccountInput, token: string): Promise<AuthUser> {
  return apiClient<AuthUser>('/auth/me', {
    method: 'PUT',
    body: JSON.stringify(input),
  }, token)
}

export function forgotPassword(email: string): Promise<void> {
  return apiClient<void>('/auth/forgot-password', {
    method: 'POST',
    body: JSON.stringify({ email }),
  })
}

export function resetPassword(token: string, newPassword: string): Promise<void> {
  return apiClient<void>('/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify({ token, newPassword }),
  })
}

export function changePassword(input: ChangePasswordInput, token: string): Promise<void> {
  return apiClient<void>('/auth/me/password', {
    method: 'PUT',
    body: JSON.stringify(input),
  }, token)
}

export function exportAccountData(token: string): Promise<AccountDataExport> {
  return apiClient<AccountDataExport>('/auth/me/export', {}, token)
}

export function deleteAccount(password: string, token: string): Promise<void> {
  return apiClient<void>('/auth/me', {
    method: 'DELETE',
    body: JSON.stringify({ password }),
  }, token)
}

function toSession(response: TokenResponse, email: string): AuthSession {
  return {
    token: response.accessToken,
    email: (response.email || email).trim().toLowerCase(),
    username: response.username ?? undefined,
    firstName: response.firstName ?? undefined,
    lastName: response.lastName ?? undefined,
    phone: response.phone ?? undefined,
    profileImageUrl: response.profileImageUrl ?? undefined,
    profileImagePosition: response.profileImagePosition ?? undefined,
    profileImageZoom: response.profileImageZoom ?? undefined,
    role: response.role,
  }
}
