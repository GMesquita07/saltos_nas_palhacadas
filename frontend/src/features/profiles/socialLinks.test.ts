import assert from 'node:assert/strict'
import test from 'node:test'

import {
  isExternalSocialLink,
  socialLinkHref,
  socialLinkLabel,
  socialPlatformIcon,
  socialPlatformLabel,
  validateSocialLink,
} from './socialLinks.ts'

test('builds social link labels and hrefs', () => {
  assert.equal(socialPlatformLabel('INSTAGRAM'), 'Instagram')
  assert.equal(socialPlatformLabel('custom_network'), 'Custom Network')
  assert.equal(socialLinkLabel({ platform: 'YOUTUBE', label: 'Canal' }), 'Canal')
  assert.equal(socialLinkLabel({ platform: 'YOUTUBE', label: '' }), 'YouTube')
  assert.equal(socialLinkHref({ platform: 'EMAIL', url: 'artista@example.test' }), 'mailto:artista@example.test')
  assert.equal(socialLinkHref({ platform: 'WEBSITE', url: 'https://example.test' }), 'https://example.test')
  assert.equal(socialPlatformIcon('INSTAGRAM'), 'instagram')
  assert.equal(socialPlatformIcon('CUSTOM_NETWORK'), 'link')
})

test('distinguishes email from external social links', () => {
  assert.equal(isExternalSocialLink({ platform: 'EMAIL' }), false)
  assert.equal(isExternalSocialLink({ platform: 'INSTAGRAM' }), true)
})

test('validates social link inputs', () => {
  assert.equal(validateSocialLink('EMAIL', 'artista@example.test'), null)
  assert.equal(validateSocialLink('WEBSITE', 'https://saltosnaspalhacadas.pt'), null)
  assert.equal(validateSocialLink('WEBSITE', 'http://saltosnaspalhacadas.pt'), null)
  assert.equal(validateSocialLink('EMAIL', 'email-invalido'), 'Indica um email válido para este link.')
  assert.equal(validateSocialLink('WEBSITE', 'javascript:alert(1)'), 'Indica um URL que comece por http:// ou https://.')
  assert.equal(validateSocialLink('WEBSITE', 'data:text/html,test'), 'Indica um URL que comece por http:// ou https://.')
  assert.equal(validateSocialLink('WEBSITE', '//evil.example'), 'Indica um URL que comece por http:// ou https://.')
})
