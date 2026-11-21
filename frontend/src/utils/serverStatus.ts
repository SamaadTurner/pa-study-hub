// The hosted demo runs on a free plan that puts the backend to sleep after 15
// minutes without visitors. The first request after that gets no answer (or a
// gateway error) while the server starts, which takes about a minute. Without
// this the user only saw "An unexpected error occurred."
export const SERVER_UNREACHABLE_MESSAGE =
  'The server is not answering. If nobody has used the app for a while it is starting up, ' +
  'which takes about a minute. Please try again shortly.'

// status is the HTTP status of the response, or undefined when there was no response
// at all (network error or timeout)
export function isServerUnreachable(status: number | undefined): boolean {
  if (status === undefined) {
    return true
  }
  // Bad gateway, service unavailable, gateway timeout: the request reached the
  // host's proxy but not our backend
  if (status === 502 || status === 503 || status === 504) {
    return true
  }
  // 520 to 524: Render's edge (Cloudflare) gave up on the backend, for example
  // after about a minute of waiting for it to wake up (seen on 2026-11-20)
  return status >= 520 && status <= 524
}

// ---- Waiting for a sleeping server ------------------------------------------
//
// A request that takes longer than this is most likely waiting for the server to
// wake up. The app then shows a banner that says so, instead of a spinner that
// gives no reason. Normal requests take well under a second.
export const SLOW_AFTER_MS = 4_000

type Listener = (slow: boolean) => void

/**
 * Counts the requests that are still waiting and tells listeners when at least one
 * has waited longer than SLOW_AFTER_MS, and again when none is left waiting.
 */
export class SlowRequestTracker {
  private waiting = new Map<number, ReturnType<typeof setTimeout>>()
  private slow = new Set<number>()
  private nextId = 1
  private listeners = new Set<Listener>()

  // alreadySlow: the request is a retry for a server that is waking up, so the
  // banner should stay up without waiting another few seconds
  start(alreadySlow = false): number {
    const id = this.nextId++
    const markSlow = () => {
      if (this.slow.has(id)) return
      this.slow.add(id)
      if (this.slow.size === 1) this.emit(true)
    }
    this.waiting.set(id, setTimeout(markSlow, SLOW_AFTER_MS))
    if (alreadySlow) markSlow()
    return id
  }

  end(id: number) {
    const timer = this.waiting.get(id)
    if (timer === undefined) return
    clearTimeout(timer)
    this.waiting.delete(id)
    if (this.slow.delete(id) && this.slow.size === 0) this.emit(false)
  }

  isSlow(): boolean {
    return this.slow.size > 0
  }

  subscribe(listener: Listener): () => void {
    this.listeners.add(listener)
    return () => { this.listeners.delete(listener) }
  }

  private emit(slow: boolean) {
    this.listeners.forEach(listener => listener(slow))
  }
}

// The one tracker the API client reports to and the banner listens to
export const slowRequests = new SlowRequestTracker()

