import type { SVGProps } from 'react'

export type NavIconName = 'profiles' | 'booking' | 'contacts' | 'favorites' | 'admin' | 'account' | 'logout' | 'login'

type NavIconProps = SVGProps<SVGSVGElement> & {
  name: NavIconName
}

export function NavIcon({ name, ...props }: NavIconProps) {
  return (
    <svg
      aria-hidden="true"
      fill="none"
      focusable="false"
      stroke="currentColor"
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth="1.8"
      viewBox="0 0 24 24"
      {...props}
    >
      {name === 'profiles' && <><circle cx="9" cy="8" r="3" /><path d="M3.5 20a5.5 5.5 0 0 1 11 0" /><path d="M15 5.5a3 3 0 0 1 0 5" /><path d="M18 20a4.6 4.6 0 0 0-2.3-4" /></>}
      {name === 'booking' && <><rect x="4" y="5" width="16" height="15" rx="1.5" /><path d="M8 3v4M16 3v4M4 10h16" /><path d="M8 14h3M8 17h6" /></>}
      {name === 'contacts' && <><path d="M20.5 16.2a2.2 2.2 0 0 1-2.4 2.4A16.4 16.4 0 0 1 5.4 5.9a2.2 2.2 0 0 1 2.4-2.4l2.1.4a1.8 1.8 0 0 1 1.4 1.3l.4 1.7a1.8 1.8 0 0 1-.5 1.7L10 9.8a12.1 12.1 0 0 0 4.2 4.2l1.2-1.2a1.8 1.8 0 0 1 1.7-.5l1.7.4a1.8 1.8 0 0 1 1.3 1.4Z" /></>}
      {name === 'favorites' && <path d="M20.8 8.7c0 5.1-8.8 10.2-8.8 10.2S3.2 13.8 3.2 8.7A4.7 4.7 0 0 1 12 6.4a4.7 4.7 0 0 1 8.8 2.3Z" />}
      {name === 'admin' && <><path d="M12 3 4.8 6.2v5.1c0 4.1 2.8 7.9 7.2 9.7 4.4-1.8 7.2-5.6 7.2-9.7V6.2L12 3Z" /><path d="m9.5 12 1.7 1.7 3.5-3.5" /></>}
      {name === 'account' && <><circle cx="12" cy="8" r="3.5" /><path d="M4.8 20a7.2 7.2 0 0 1 14.4 0" /></>}
      {name === 'logout' && <><path d="M10 5H5.5A1.5 1.5 0 0 0 4 6.5v11A1.5 1.5 0 0 0 5.5 19H10" /><path d="M14 8l4 4-4 4" /><path d="M18 12H9" /></>}
      {name === 'login' && <><circle cx="10" cy="8" r="3" /><path d="M4.5 20a5.5 5.5 0 0 1 11 0" /><path d="M17 10l3 2-3 2" /><path d="M20 12h-6" /></>}
    </svg>
  )
}
