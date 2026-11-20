import { useEffect, useRef } from 'react'

// The parts of Cloudflare's Turnstile script this app uses
interface Turnstile {
  render: (element: HTMLElement, options: {
    sitekey: string
    callback: (token: string) => void
    'expired-callback': () => void
    'error-callback': () => void
    theme?: 'auto' | 'light' | 'dark'
  }) => string
  reset: (widgetId: string) => void
  remove: (widgetId: string) => void
}

declare global {
  interface Window {
    turnstile?: Turnstile
  }
}

const TURNSTILE_SCRIPT = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit'

// Loaded only on the sign-up page, and only once
let scriptPromise: Promise<void> | null = null

function loadScript(): Promise<void> {
  if (!scriptPromise) {
    scriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = TURNSTILE_SCRIPT
      script.async = true
      script.onload = () => resolve()
      script.onerror = () => {
        scriptPromise = null
        reject(new Error('The check could not be loaded'))
      }
      document.head.appendChild(script)
    })
  }
  return scriptPromise
}

interface Props {
  siteKey: string
  // The one-time token when the check passed, null when it ran out or failed
  onToken: (token: string | null) => void
  // Changing this number asks for a fresh check (a token works only once)
  resetCount: number
}

/**
 * Cloudflare Turnstile, the check that a person is signing up. Most people see a
 * small box that ticks itself; sometimes it asks for one click.
 */
export default function TurnstileWidget({ siteKey, onToken, resetCount }: Props) {
  const boxRef = useRef<HTMLDivElement>(null)
  const widgetId = useRef<string | null>(null)
  // The latest handler, without rendering the widget again when it changes
  const onTokenRef = useRef(onToken)
  onTokenRef.current = onToken

  useEffect(() => {
    let cancelled = false
    loadScript()
      .then(() => {
        if (cancelled || !boxRef.current || !window.turnstile) return
        widgetId.current = window.turnstile.render(boxRef.current, {
          sitekey: siteKey,
          theme: 'light',
          callback: token => onTokenRef.current(token),
          'expired-callback': () => onTokenRef.current(null),
          'error-callback': () => onTokenRef.current(null),
        })
      })
      .catch(() => onTokenRef.current(null))
    return () => {
      cancelled = true
      if (widgetId.current && window.turnstile) window.turnstile.remove(widgetId.current)
      widgetId.current = null
    }
  }, [siteKey])

  useEffect(() => {
    if (resetCount > 0 && widgetId.current && window.turnstile) {
      window.turnstile.reset(widgetId.current)
      onTokenRef.current(null)
    }
  }, [resetCount])

  return <div ref={boxRef} className="min-h-[65px]" aria-label="Check that you are not a robot" />
}
