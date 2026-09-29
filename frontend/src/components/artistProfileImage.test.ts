import assert from 'node:assert/strict'
import test from 'node:test'

import {
  artistInitials,
  artistProfileImageCropProps,
  artistProfileImageScaleStyle,
  type ArtistProfileImageVariant,
} from './artistProfileImage.ts'

const variants: ArtistProfileImageVariant[] = ['card', 'heroDesktop', 'heroMobile', 'heroResponsive']

test('artist avatar variants preserve the exact crop inputs', () => {
  for (const variant of variants) {
    assert.deepEqual(
      artistProfileImageCropProps({
        alt: `${variant} avatar`,
        fallback: 'DK',
        position: '37% 81%',
        src: 'https://example.test/profile.jpg',
        zoom: 1.27,
      }),
      {
        alt: `${variant} avatar`,
        fallback: 'DK',
        position: '37% 81%',
        shape: 'circle',
        src: 'https://example.test/profile.jpg',
        zoom: 1.27,
      },
    )
  }
})

test('artist avatar defaults are shared by every surface', () => {
  assert.deepEqual(artistProfileImageCropProps({}), {
    alt: '',
    fallback: undefined,
    position: '50% 50%',
    shape: 'circle',
    src: undefined,
    zoom: 1,
  })
})

test('admin compact previews scale the same card surface instead of changing crop math', () => {
  assert.deepEqual(artistProfileImageScaleStyle('card', 0.56), {
    '--artist-profile-image-layout-size': '106.4px',
    '--artist-profile-image-scale': '0.56',
  })
})

test('artist initials are stable across public cards and admin previews', () => {
  assert.equal(artistInitials('DJ KidG'), 'DK')
  assert.equal(artistInitials('João Tomás'), 'JT')
  assert.equal(artistInitials(''), '')
})
