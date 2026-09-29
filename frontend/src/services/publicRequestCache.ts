type PublicRequestOptions = {
  force?: boolean
}

type PublicRequestCacheOptions = {
  maxAgeMs?: number
  now?: () => number
}

type Loader<T> = (requestOptions: RequestInit) => Promise<T>

const defaultMaxAgeMs = 60_000

export function createPublicRequestCache<T>(load: Loader<T>, options: PublicRequestCacheOptions = {}) {
  const maxAgeMs = options.maxAgeMs ?? defaultMaxAgeMs
  const now = options.now ?? Date.now
  let cachedValue: T | null = null
  let hasCachedValue = false
  let cachedAt = 0
  let inFlightRequest: Promise<T> | null = null
  let inFlightIsForceRefresh = false
  let generation = 0

  function get(options: PublicRequestOptions = {}) {
    const force = options.force === true

    if (force) {
      cachedValue = null
      hasCachedValue = false
      cachedAt = 0

      if (inFlightRequest && inFlightIsForceRefresh) {
        return inFlightRequest
      }

      generation += 1
      inFlightRequest = null
      inFlightIsForceRefresh = false
    }

    if (!force && hasFreshCachedValue()) {
      return Promise.resolve(cachedValue as T)
    }

    if (inFlightRequest) {
      return inFlightRequest
    }

    const requestGeneration = generation
    const requestOptions: RequestInit = force ? { cache: 'reload' } : {}
    const request = load(requestOptions)
      .then((value) => {
        if (requestGeneration === generation) {
          cachedValue = value
          hasCachedValue = true
          cachedAt = now()
        }
        return value
      })
      .finally(() => {
        if (inFlightRequest === request) {
          inFlightRequest = null
          inFlightIsForceRefresh = false
        }
      })

    inFlightRequest = request
    inFlightIsForceRefresh = force
    return request
  }

  function invalidate() {
    generation += 1
    cachedValue = null
    hasCachedValue = false
    cachedAt = 0
    inFlightRequest = null
    inFlightIsForceRefresh = false
  }

  function prime(value: T) {
    generation += 1
    cachedValue = value
    hasCachedValue = true
    cachedAt = now()
    inFlightRequest = null
    inFlightIsForceRefresh = false
  }

  function hasFreshCachedValue() {
    return hasCachedValue && now() - cachedAt < maxAgeMs
  }

  return { get, invalidate, prime }
}
