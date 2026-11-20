import { useEffect, useState } from 'react'
import { slowRequests } from '@/utils/serverStatus'

/**
 * Shown while a request has been waiting a few seconds, which on the free plan
 * almost always means the server is waking up. Says why and about how long,
 * with a running count of seconds so it is clear something is happening.
 */
export default function ServerWakeBanner() {
  const [slow, setSlow] = useState(slowRequests.isSlow())
  const [seconds, setSeconds] = useState(0)

  useEffect(() => slowRequests.subscribe(setSlow), [])

  useEffect(() => {
    if (!slow) {
      setSeconds(0)
      return
    }
    const started = Date.now()
    const timer = setInterval(() => setSeconds(Math.round((Date.now() - started) / 1000) + 4), 1000)
    return () => clearInterval(timer)
  }, [slow])

  if (!slow) return null

  return (
    <div role="status" aria-live="polite"
      className="fixed top-0 inset-x-0 z-50 bg-amber-50 border-b border-amber-200 text-amber-900 text-sm">
      <div className="max-w-3xl mx-auto px-4 py-2 flex items-center gap-3">
        <span className="h-4 w-4 flex-shrink-0 rounded-full border-2 border-amber-600 border-t-transparent animate-spin" aria-hidden="true" />
        <p>
          Waking up the server{seconds > 0 ? ` (${seconds} s)` : ''}. It sleeps when nobody has used the app for
          15 minutes and takes up to a minute to start. Your request will go through.
        </p>
      </div>
    </div>
  )
}
