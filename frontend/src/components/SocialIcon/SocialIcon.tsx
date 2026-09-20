export type SocialIconName = 'instagram' | 'tiktok' | 'youtube' | 'facebook' | 'spotify' | 'email' | 'website' | 'link'

export function SocialIcon({ name }: { name: SocialIconName }) {
  if (name === 'instagram') {
    return (
      <svg aria-hidden="true" viewBox="0 0 24 24">
        <rect x="5" y="5" width="14" height="14" rx="4" />
        <circle cx="12" cy="12" r="3.1" />
        <circle cx="16.4" cy="7.6" r="0.7" />
      </svg>
    )
  }

  if (name === 'tiktok') {
    return (
      <svg aria-hidden="true" viewBox="0 0 24 24">
        <path d="M14 4v9.2a4.2 4.2 0 1 1-3.8-4.18" />
        <path d="M14 4c.45 2.9 2.13 4.63 5 5" />
      </svg>
    )
  }

  if (name === 'youtube') {
    return (
      <svg aria-hidden="true" viewBox="0 0 24 24">
        <rect x="4" y="7" width="16" height="10" rx="3" />
        <path
          d="m10.5 10 4 2-4 2z"
          fill="currentColor"
          stroke="none"
        />
      </svg>
    )
  }

  if (name === 'facebook') {
    return (
      <svg aria-hidden="true" viewBox="0 0 24 24">
        <path
          d="M14 8h2V5h-2.4A4.1 4.1 0 0 0 9.5 9.1V11H7v3h2.5v6H13v-6h2.5l.5-3h-3V9.3c0-.8.3-1.3 1-1.3Z"
          fill="currentColor"
          stroke="none"
        />
      </svg>
    )
  }

  if (name === 'spotify') {
    return (
      <svg aria-hidden="true" viewBox="0 0 24 24">
        <circle cx="12" cy="12" r="8" />
        <path d="M8.2 10.2c2.5-.7 5.3-.4 7.6.8" />
        <path d="M8.8 12.9c2-.5 4.2-.3 6 .7" />
        <path d="M9.4 15.4c1.4-.3 3-.2 4.3.5" />
      </svg>
    )
  }

  if (name === 'email') {
    return (
      <svg aria-hidden="true" viewBox="0 0 24 24">
        <rect x="4" y="6" width="16" height="12" rx="2" />
        <path d="m5 8 7 5 7-5" />
      </svg>
    )
  }

  if (name === 'website') {
    return (
      <svg aria-hidden="true" viewBox="0 0 24 24">
        <circle cx="12" cy="12" r="8" />
        <path d="M4 12h16" />
        <path d="M12 4c2 2.2 3 4.8 3 8s-1 5.8-3 8c-2-2.2-3-4.8-3-8s1-5.8 3-8Z" />
      </svg>
    )
  }

  return (
    <svg aria-hidden="true" viewBox="0 0 24 24">
      <path d="M10.5 13.5a3 3 0 0 0 4.24 0l2.83-2.83a3 3 0 0 0-4.24-4.24l-1.06 1.06" />
      <path d="M13.5 10.5a3 3 0 0 0-4.24 0l-2.83 2.83a3 3 0 0 0 4.24 4.24l1.06-1.06" />
    </svg>
  )
}
