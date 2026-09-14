import assert from 'node:assert/strict'
import test from 'node:test'

import { validateUploadFileSize } from './apiClient.ts'

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
