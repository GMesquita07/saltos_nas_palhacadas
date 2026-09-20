import assert from 'node:assert/strict'
import test from 'node:test'

import {
  adminPageFromPath,
  adminPath,
  authModeFromPath,
  authPath,
  bookingPath,
  isSafeReturnTo,
  legacyResetRedirect,
  loginPath,
  profilePath,
} from './routes.ts'

test('maps admin pages to canonical paths and back', () => {
  assert.equal(adminPath('dashboard'), '/admin')
  assert.equal(adminPath('profile'), '/admin/perfis')
  assert.equal(adminPath('content'), '/admin/publicacoes')
  assert.equal(adminPath('bookings'), '/admin/reservas')
  assert.equal(adminPath('reviews'), '/admin/avaliacoes')
  assert.equal(adminPath('contacts'), '/admin/contactos')
  assert.equal(adminPath('materials'), '/admin/materiais')
  assert.equal(adminPageFromPath('/admin/avaliacoes'), 'reviews')
  assert.equal(adminPageFromPath('/admin/desconhecido'), null)
})

test('builds profile and booking paths from slugs', () => {
  assert.equal(profilePath('dj-kidg'), '/perfis/dj-kidg')
  assert.equal(bookingPath('dj-kidg'), '/agendar/dj-kidg')
  assert.equal(bookingPath(), '/agendar')
})

test('maps auth modes to paths and back', () => {
  assert.equal(authPath('login'), '/login')
  assert.equal(authPath('register'), '/registo')
  assert.equal(authPath('forgot'), '/recuperar-password')
  assert.equal(authPath('reset', 'abc 123'), '/reset-password?resetToken=abc+123')
  assert.equal(authModeFromPath('/recuperar-password'), 'forgot')
  assert.equal(authModeFromPath('/conta'), null)
})

test('validates safe internal returnTo targets', () => {
  assert.equal(isSafeReturnTo('/'), true)
  assert.equal(isSafeReturnTo('/conta'), true)
  assert.equal(isSafeReturnTo('/favoritos'), true)
  assert.equal(isSafeReturnTo('/agendar/dj-kidg'), true)
  assert.equal(isSafeReturnTo('/admin/reservas'), true)
  assert.equal(isSafeReturnTo('/perfis/dj-joao-tomas'), true)
  assert.equal(loginPath('/conta'), '/login?returnTo=%2Fconta')
})

test('rejects external and encoded returnTo targets', () => {
  assert.equal(isSafeReturnTo('https://evil.example'), false)
  assert.equal(isSafeReturnTo('http://evil.example'), false)
  assert.equal(isSafeReturnTo('//evil.example'), false)
  assert.equal(isSafeReturnTo('///evil.example'), false)
  assert.equal(isSafeReturnTo('javascript:alert(1)'), false)
  assert.equal(isSafeReturnTo('data:text/html,test'), false)
  assert.equal(isSafeReturnTo('\\evil.example'), false)
  assert.equal(isSafeReturnTo('/%2F%2Fevil.example'), false)
  assert.equal(isSafeReturnTo('/%5C%5Cevil.example'), false)
  assert.equal(isSafeReturnTo('/%252F%252Fevil.example'), false)
  assert.equal(isSafeReturnTo('/%E0%A4%A'), false)
  assert.equal(loginPath('https://evil.example'), '/login')
})

test('builds legacy root reset-token redirect target', () => {
  assert.equal(legacyResetRedirect('/', '?resetToken=abc123'), '/reset-password?resetToken=abc123')
  assert.equal(legacyResetRedirect('/login', '?resetToken=abc123'), null)
  assert.equal(legacyResetRedirect('/', ''), null)
})
