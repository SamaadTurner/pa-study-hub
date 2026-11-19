import { useEffect, useRef, useState } from 'react'
import { getGoogleStatus } from '@/api/auth'

// The parts of Google's sign-in script this app uses
interface GoogleCredentialResponse {
  credential: string
}

interface GoogleAccountsId {
  initialize: (options: { client_id: string; callback: (response: GoogleCredentialResponse) => void }) => void
  renderButton: (parent: HTMLElement, options: Record<string, unknown>) => void
}

declare global {
  interface Window {
    google?: { accounts: { id: GoogleAccountsId } }
  }
}

const GOOGLE_SCRIPT = 'https://accounts.google.com/gsi/client'

// The script is only loaded on pages that show the button, and only once
let scriptPromise: Promise<void> | null = null

function loadGoogleScript(): Promise<void> {
  if (!scriptPromise) {
    scriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = GOOGLE_SCRIPT
      script.async = true
      script.onload = () => resolve()
      script.onerror = () => {
        scriptPromise = null
        reject(new Error('Google sign-in could not be loaded'))
      }
      document.head.appendChild(script)
    })
  }
  return scriptPromise
}

/** The Google client ID when Google sign-in is on, else null (also while loading). */
export function useGoogleClientId(): string | null {
  const [clientId, setClientId] = useState<string | null>(null)
  useEffect(() => {
    let ignore = false
    getGoogleStatus()
      .then(status => {
        if (!ignore && status.enabled && status.clientId) setClientId(status.clientId)
      })
      .catch(() => { /* no Google button then */ })
    return () => { ignore = true }
  }, [])
  return clientId
}

interface Props {
  clientId: string
  // Google's wording on the button
  text: 'continue_with' | 'signup_with' | 'signin_with'
  onCredential: (credential: string) => void
  onLoadError: () => void
}

/** Google's own button, drawn by Google's script. Hands the ID token to onCredential. */
export default function GoogleButton({ clientId, text, onCredential, onLoadError }: Props) {
  const buttonRef = useRef<HTMLDivElement>(null)

  // The script keeps the first callback it is given, so it reads the latest one from a ref
  const callbackRef = useRef(onCredential)
  callbackRef.current = onCredential
  const errorRef = useRef(onLoadError)
  errorRef.current = onLoadError

  useEffect(() => {
    let ignore = false
    loadGoogleScript()
      .then(() => {
        if (ignore || !window.google || !buttonRef.current) return
        window.google.accounts.id.initialize({
          client_id: clientId,
          callback: response => callbackRef.current(response.credential),
        })
        window.google.accounts.id.renderButton(buttonRef.current, {
          theme: 'outline',
          size: 'large',
          text,
          width: Math.min(buttonRef.current.offsetWidth || 320, 400),
        })
      })
      .catch(() => {
        if (!ignore) errorRef.current()
      })
    return () => { ignore = true }
  }, [clientId, text])

  return <div ref={buttonRef} className="flex justify-center min-h-[44px]" />
}
