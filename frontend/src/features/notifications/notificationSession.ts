export function isNotificationStateVisible(stateToken: string | null, activeToken: string | null) {
  return stateToken === activeToken
}

export function isCurrentNotificationRequest(
  activeToken: string | null,
  activeGeneration: number,
  requestToken: string,
  requestGeneration: number,
) {
  return activeToken === requestToken && activeGeneration === requestGeneration
}
