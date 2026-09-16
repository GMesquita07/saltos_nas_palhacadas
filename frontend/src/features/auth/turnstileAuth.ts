import type { AuthMode } from './authTypes'

export const turnstileActions = {
  login: 'login',
  register: 'register',
  forgot: 'forgot_password',
} as const

export type TurnstileAction = typeof turnstileActions[keyof typeof turnstileActions]

export function turnstileActionForMode(mode: AuthMode): TurnstileAction | null {
  if (mode === 'login') return turnstileActions.login
  if (mode === 'register') return turnstileActions.register
  if (mode === 'forgot') return turnstileActions.forgot
  return null
}

export function protectedAuthRequiresSiteKey(mode: AuthMode, isProduction: boolean, siteKey: string) {
  return turnstileActionForMode(mode) !== null && isProduction && siteKey.trim().length === 0
}

export function canSubmitProtectedAuth(mode: AuthMode, siteKey: string, token: string | null, isProduction: boolean) {
  const action = turnstileActionForMode(mode)
  if (action === null) return true
  if (protectedAuthRequiresSiteKey(mode, isProduction, siteKey)) return false
  if (siteKey.trim().length === 0) return true
  return token !== null && token.trim().length > 0
}
