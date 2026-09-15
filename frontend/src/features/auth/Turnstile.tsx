import { forwardRef, useEffect, useImperativeHandle, useRef } from 'react'

const turnstileScriptId = 'cloudflare-turnstile-script'
const turnstileScriptUrl = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit'

let turnstileScriptPromise: Promise<TurnstileApi> | null = null

type TurnstileRenderOptions = {
  sitekey: string
  action: string
  callback: (token: string) => void
  'expired-callback': () => void
  'error-callback': () => void
}

type TurnstileApi = {
  render: (container: HTMLElement, options: TurnstileRenderOptions) => string
  reset: (widgetId: string) => void
  remove: (widgetId: string) => void
}

declare global {
  interface Window {
    turnstile?: TurnstileApi
  }
}

export type TurnstileHandle = {
  reset: () => void
}

type TurnstileProps = {
  siteKey: string
  action: string
  onToken: (token: string) => void
  onExpire: () => void
  onError: () => void
  className?: string
}

export const Turnstile = forwardRef<TurnstileHandle, TurnstileProps>(function Turnstile({
  siteKey,
  action,
  onToken,
  onExpire,
  onError,
  className,
}, ref) {
  const containerRef = useRef<HTMLDivElement | null>(null)
  const widgetIdRef = useRef<string | null>(null)

  useImperativeHandle(ref, () => ({
    reset: () => {
      if (widgetIdRef.current && window.turnstile) {
        window.turnstile.reset(widgetIdRef.current)
      }
    },
  }), [])

  useEffect(() => {
    let isCurrent = true
    const container = containerRef.current

    void loadTurnstileScript()
      .then((turnstile) => {
        if (!isCurrent || !container) return

        removeWidget(widgetIdRef.current)
        container.innerHTML = ''
        widgetIdRef.current = turnstile.render(container, {
          sitekey: siteKey,
          action,
          callback: onToken,
          'expired-callback': onExpire,
          'error-callback': onError,
        })
      })
      .catch(() => {
        if (isCurrent) onError()
      })

    return () => {
      isCurrent = false
      removeWidget(widgetIdRef.current)
      widgetIdRef.current = null
      if (container) {
        container.innerHTML = ''
      }
    }
  }, [action, onError, onExpire, onToken, siteKey])

  return <div className={className} ref={containerRef} />
})

function loadTurnstileScript(): Promise<TurnstileApi> {
  if (window.turnstile) {
    return Promise.resolve(window.turnstile)
  }

  if (turnstileScriptPromise) {
    return turnstileScriptPromise
  }

  turnstileScriptPromise = new Promise<TurnstileApi>((resolve, reject) => {
    const existingScript = document.getElementById(turnstileScriptId) as HTMLScriptElement | null
    const script = existingScript ?? document.createElement('script')

    const handleLoad = () => {
      if (window.turnstile) {
        resolve(window.turnstile)
      } else {
        reject(new Error('Cloudflare Turnstile não ficou disponível.'))
      }
    }

    const handleError = () => {
      turnstileScriptPromise = null
      reject(new Error('Não foi possível carregar Cloudflare Turnstile.'))
    }

    script.addEventListener('load', handleLoad, { once: true })
    script.addEventListener('error', handleError, { once: true })

    if (!existingScript) {
      script.id = turnstileScriptId
      script.src = turnstileScriptUrl
      script.async = true
      script.defer = true
      document.head.appendChild(script)
    }
  })

  return turnstileScriptPromise
}

function removeWidget(widgetId: string | null) {
  if (widgetId && window.turnstile) {
    window.turnstile.remove(widgetId)
  }
}
