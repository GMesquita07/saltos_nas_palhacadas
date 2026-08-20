export type UserRole = 'ADMIN' | 'CUSTOMER'

export type AuthUser = {
  email: string
  role: UserRole
}

export type AuthSession = AuthUser & {
  token: string
}
