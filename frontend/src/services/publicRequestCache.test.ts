import assert from 'node:assert/strict'
import test from 'node:test'

import { createPublicRequestCache } from './publicRequestCache.ts'

test('reuses loaded cache within the TTL', async () => {
  const first = deferred<string>()
  const calls: RequestInit[] = []
  let currentTime = 1_000
  const cache = createPublicRequestCache<string>((options) => {
    calls.push(options)
    return first.promise
  }, { now: () => currentTime })

  first.resolve('loaded')
  assert.equal(await cache.get(), 'loaded')
  currentTime += 59_999
  assert.equal(await cache.get(), 'loaded')
  assert.equal(calls.length, 1)
})

test('starts a new request after the TTL expires', async () => {
  const first = deferred<string>()
  const second = deferred<string>()
  const calls: RequestInit[] = []
  let currentTime = 1_000
  const cache = createPublicRequestCache<string>((options) => {
    calls.push(options)
    return calls.length === 1 ? first.promise : second.promise
  }, { now: () => currentTime })

  first.resolve('initial')
  assert.equal(await cache.get(), 'initial')

  currentTime += 60_000
  const refreshed = cache.get()
  assert.equal(calls.length, 2)
  second.resolve('refreshed')
  assert.equal(await refreshed, 'refreshed')
  assert.equal(await cache.get(), 'refreshed')
})

test('deduplicates concurrent public requests', async () => {
  const first = deferred<string>()
  const calls: RequestInit[] = []
  const cache = createPublicRequestCache<string>((options) => {
    calls.push(options)
    return first.promise
  })

  const firstRequest = cache.get()
  const secondRequest = cache.get()

  assert.equal(firstRequest, secondRequest)
  assert.deepEqual(calls, [{}])

  first.resolve('loaded')
  assert.equal(await firstRequest, 'loaded')
  assert.equal(calls.length, 1)
})

test('force refresh bypasses memory cache and asks fetch to reload HTTP cache', async () => {
  const first = deferred<string>()
  const second = deferred<string>()
  const calls: RequestInit[] = []
  const cache = createPublicRequestCache<string>((options) => {
    calls.push(options)
    return calls.length === 1 ? first.promise : second.promise
  })

  first.resolve('initial')
  assert.equal(await cache.get(), 'initial')

  const forced = cache.get({ force: true })
  assert.deepEqual(calls[1], { cache: 'reload' })
  second.resolve('fresh')
  assert.equal(await forced, 'fresh')
  assert.equal(await cache.get(), 'fresh')
  assert.equal(calls.length, 2)
})

test('stale request cannot repopulate cache after force refresh', async () => {
  const stale = deferred<string>()
  const fresh = deferred<string>()
  const calls: RequestInit[] = []
  const cache = createPublicRequestCache<string>((options) => {
    calls.push(options)
    return calls.length === 1 ? stale.promise : fresh.promise
  })

  const staleRequest = cache.get()
  const freshRequest = cache.get({ force: true })

  assert.deepEqual(calls, [{}, { cache: 'reload' }])

  fresh.resolve('fresh')
  assert.equal(await freshRequest, 'fresh')
  stale.resolve('stale')
  assert.equal(await staleRequest, 'stale')
  assert.equal(await cache.get(), 'fresh')
  assert.equal(calls.length, 2)
})

test('failed request clears inflight state and allows retry', async () => {
  const failing = deferred<string>()
  const retry = deferred<string>()
  const calls: RequestInit[] = []
  const cache = createPublicRequestCache<string>((options) => {
    calls.push(options)
    return calls.length === 1 ? failing.promise : retry.promise
  })

  const failedRequest = cache.get()
  failing.reject(new Error('network'))
  await assert.rejects(failedRequest, /network/)

  retry.resolve('retry-ok')
  assert.equal(await cache.get(), 'retry-ok')
  assert.equal(calls.length, 2)
})

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })

  return { promise, resolve, reject }
}
