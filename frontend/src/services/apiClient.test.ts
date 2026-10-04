import assert from 'node:assert/strict'
import test from 'node:test'

import { ApiError, apiClient, validateUploadFileSize } from './apiClient.ts'

const mib = 1024 * 1024

test('accepts images up to 10 MiB', () => {
  assert.doesNotThrow(() => validateUploadFileSize(file('image/png', 10 * mib)))
})

test('rejects images over 10 MiB', () => {
  assert.throws(
    () => validateUploadFileSize(file('image/png', (10 * mib) + 1)),
    /A imagem não pode exceder 10 MB\./,
  )
})

test('accepts videos up to 30 MiB', () => {
  assert.doesNotThrow(() => validateUploadFileSize(file('video/mp4', 30 * mib)))
})

test('rejects videos over 30 MiB', () => {
  assert.throws(
    () => validateUploadFileSize(file('video/mp4', (30 * mib) + 1)),
    /O vídeo não pode exceder 30 MB\./,
  )
})

function file(type: string, size: number): Pick<File, 'size' | 'type'> {
  return { size, type }
}

test('HTTP failures retain status and remain compatible with Error consumers', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => new Response(JSON.stringify({ detail: 'Link indisponível' }), { status: 400 }))
  await assert.rejects(apiClient('/auth/reset-password/validate'), (error: unknown) => {
    assert.ok(error instanceof Error)
    assert.ok(error instanceof ApiError)
    assert.equal(error.status, 400)
    assert.equal(error.message, 'Link indisponível')
    assert.deepEqual(error.fieldErrors, {})
    return true
  })
})

test('field validation errors remain distinguishable from token rejection', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => new Response(JSON.stringify({
    detail: 'Campos inválidos', errors: { newPassword: 'Password demasiado longa' },
  }), { status: 400 }))
  await assert.rejects(apiClient('/auth/reset-password'), (error: unknown) => {
    assert.ok(error instanceof ApiError)
    assert.equal(error.message, 'Password demasiado longa')
    assert.deepEqual(error.fieldErrors, { newPassword: 'Password demasiado longa' })
    return true
  })
})

test('401 keeps the existing login message and HTTP status', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => new Response(null, { status: 401 }))
  await assert.rejects(apiClient('/auth/login'), { status: 401, message: 'Email ou palavra-passe inválidos.' })
})

test('network failures are not converted to HTTP token errors', async (t) => {
  const failure = new TypeError('fetch failed')
  t.mock.method(globalThis, 'fetch', async () => { throw failure })
  await assert.rejects(apiClient('/auth/reset-password/validate'), (error) => error === failure)
})
