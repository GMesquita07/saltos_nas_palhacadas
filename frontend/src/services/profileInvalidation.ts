type ProfileInvalidationOptions = {
  broadcast?: boolean
}

let staleProfiles = false
let seenProfilesInvalidation: string | null = null

export const profileInvalidationStorageKey = 'saltos:profiles:changed'

export function invalidatePublicProfiles(options: ProfileInvalidationOptions = {}) {
  staleProfiles = true
  if (options.broadcast !== false) {
    writeProfilesInvalidation()
  }
}

export function shouldReloadProfiles(force = false) {
  return force || staleProfiles || hasExternalProfilesInvalidation()
}

export function markProfilesLoaded() {
  staleProfiles = false
}

export function profilesInvalidationMatches(value: string | null) {
  return parseProfilesInvalidation(value) !== null
}

function hasExternalProfilesInvalidation() {
  const value = readProfilesInvalidation()
  if (!profilesInvalidationMatches(value)) return false
  if (seenProfilesInvalidation === value) return false

  seenProfilesInvalidation = value
  return true
}

function readProfilesInvalidation() {
  if (typeof window === 'undefined') return null

  try {
    return window.localStorage.getItem(profileInvalidationStorageKey)
  } catch {
    return null
  }
}

function writeProfilesInvalidation() {
  if (typeof window === 'undefined') return

  try {
    window.localStorage.setItem(profileInvalidationStorageKey, JSON.stringify({ updatedAt: Date.now() }))
  } catch {
    // A falha do storage não deve impedir a atualização na janela atual.
  }
}

function parseProfilesInvalidation(value: string | null) {
  if (!value) return null

  try {
    const parsed = JSON.parse(value) as { updatedAt?: unknown }
    return typeof parsed.updatedAt === 'number' ? { updatedAt: parsed.updatedAt } : null
  } catch {
    return null
  }
}
