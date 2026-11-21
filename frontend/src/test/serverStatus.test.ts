import { describe, it, expect } from 'vitest'
import { isServerUnreachable } from '@/utils/serverStatus'

describe('isServerUnreachable', () => {
  it('is true when there was no response at all', () => {
    expect(isServerUnreachable(undefined)).toBe(true)
  })

  it('is true for the gateway errors a sleeping host returns', () => {
    expect(isServerUnreachable(502)).toBe(true)
    expect(isServerUnreachable(503)).toBe(true)
    expect(isServerUnreachable(504)).toBe(true)
  })

  it("is true for the 520 to 524 errors Render's edge returns when it gives up waiting", () => {
    for (const status of [520, 521, 522, 523, 524]) {
      expect(isServerUnreachable(status), String(status)).toBe(true)
    }
    expect(isServerUnreachable(525)).toBe(false)
  })

  it('is false for answers that came from the backend', () => {
    expect(isServerUnreachable(200)).toBe(false)
    expect(isServerUnreachable(400)).toBe(false)
    expect(isServerUnreachable(401)).toBe(false)
    expect(isServerUnreachable(500)).toBe(false)
  })
})
