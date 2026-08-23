export type UserRole = 'ADMIN' | 'CUSTOMER'

export type AuthUser = {
  email: string
  role: UserRole
  username?: string
  firstName?: string
  lastName?: string
  phone?: string
  profileImageUrl?: string
  profileImagePosition?: string
  profileImageZoom?: number
}

export type AuthSession = AuthUser & {
  token: string
}
