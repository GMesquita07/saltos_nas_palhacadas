export type ResetTokenValidationState = 'idle' | 'validating' | 'valid' | 'invalid' | 'error'
export type ResetTokenValidationFailure = 'invalid' | 'error' | 'cancelled'

export const invalidResetLinkTitle = 'Link inválido ou expirado'
export const invalidResetLinkMessage = 'Este link de recuperação já não pode ser utilizado. Pede um novo link para definires uma nova palavra-passe.'
export const resetLinkValidationErrorMessage = 'Não foi possível validar o link neste momento. Tenta novamente.'

export function initialResetTokenValidationState(mode: 'login' | 'register' | 'forgot' | 'reset', token?: string | null): ResetTokenValidationState {
  if (mode !== 'reset') return 'idle'
  return token?.trim() ? 'validating' : 'invalid'
}

export function shouldShowResetPasswordForm(state: ResetTokenValidationState) {
  return state === 'valid'
}

export function canSubmitResetPassword(state: ResetTokenValidationState, isSubmitting: boolean) {
  return state === 'valid' && !isSubmitting
}

export function classifyResetTokenValidationFailure(reason: unknown): ResetTokenValidationFailure {
  if (isAbortError(reason)) return 'cancelled'
  if (hasStatus(reason, 400)) return 'invalid'
  return 'error'
}

export function classifyResetPasswordSubmitFailure(reason: unknown): 'invalid' | 'error' {
  if (typeof reason === 'object' && reason !== null && 'fieldErrors' in reason
    && reason.fieldErrors && typeof reason.fieldErrors === 'object'
    && Object.keys(reason.fieldErrors).length > 0) return 'error'
  if (hasStatus(reason, 400)) return 'invalid'
  return 'error'
}

export function resetRecoveryMode() {
  return 'forgot' as const
}

function hasStatus(reason: unknown, status: number) {
  return typeof reason === 'object'
    && reason !== null
    && 'status' in reason
    && (reason as { status?: unknown }).status === status
}

function isAbortError(reason: unknown) {
  return typeof reason === 'object'
    && reason !== null
    && 'name' in reason
    && (reason as { name?: unknown }).name === 'AbortError'
}
