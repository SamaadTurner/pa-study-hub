import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { SLOW_AFTER_MS, SlowRequestTracker } from '@/utils/serverStatus'

describe('SlowRequestTracker', () => {
  beforeEach(() => { vi.useFakeTimers() })
  afterEach(() => { vi.useRealTimers() })

  it('says nothing about requests that finish quickly', () => {
    const tracker = new SlowRequestTracker()
    const changes: boolean[] = []
    tracker.subscribe(slow => changes.push(slow))

    const id = tracker.start()
    vi.advanceTimersByTime(SLOW_AFTER_MS - 1)
    tracker.end(id)
    vi.advanceTimersByTime(10_000)

    expect(changes).toEqual([])
    expect(tracker.isSlow()).toBe(false)
  })

  it('turns slow once when requests wait too long, and back when the last one ends', () => {
    const tracker = new SlowRequestTracker()
    const changes: boolean[] = []
    tracker.subscribe(slow => changes.push(slow))

    const first = tracker.start()
    const second = tracker.start()
    vi.advanceTimersByTime(SLOW_AFTER_MS)
    expect(changes).toEqual([true])
    expect(tracker.isSlow()).toBe(true)

    tracker.end(first)
    expect(changes).toEqual([true])
    tracker.end(second)
    expect(changes).toEqual([true, false])
    // Ending again does nothing
    tracker.end(second)
    expect(changes).toEqual([true, false])
  })

  it('a retry for a waking server is slow right away', () => {
    const tracker = new SlowRequestTracker()
    const changes: boolean[] = []
    tracker.subscribe(slow => changes.push(slow))
    const id = tracker.start(true)
    expect(changes).toEqual([true])
    vi.advanceTimersByTime(SLOW_AFTER_MS)
    expect(changes).toEqual([true])
    tracker.end(id)
    expect(changes).toEqual([true, false])
  })

  it('stops telling a listener after it unsubscribes', () => {
    const tracker = new SlowRequestTracker()
    const listener = vi.fn()
    const unsubscribe = tracker.subscribe(listener)
    unsubscribe()
    tracker.start()
    vi.advanceTimersByTime(SLOW_AFTER_MS)
    expect(listener).not.toHaveBeenCalled()
  })
})
