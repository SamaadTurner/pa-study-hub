import { describe, it, expect } from 'vitest'
import { formatDuration, formatMinutes, formatCountdown } from '@/utils/formatDuration'

describe('formatDuration', () => {
  it('formats 125 seconds as "2m 5s"', () => {
    expect(formatDuration(125)).toBe('2m 5s')
  })

  it('formats 3600 seconds as "1h 0m"', () => {
    expect(formatDuration(3600)).toBe('1h 0m')
  })

  it('formats 45 seconds as "45s"', () => {
    expect(formatDuration(45)).toBe('45s')
  })

  it('formats 0 seconds as "0s"', () => {
    expect(formatDuration(0)).toBe('0s')
  })

  it('formats 3661 seconds as "1h 1m"', () => {
    expect(formatDuration(3661)).toBe('1h 1m')
  })

  it('formats 60 seconds as "1m 0s"', () => {
    expect(formatDuration(60)).toBe('1m 0s')
  })

  it('handles negative values gracefully', () => {
    expect(formatDuration(-10)).toBe('0s')
  })
})

describe('formatMinutes', () => {
  it('formats 30 minutes as "30 min"', () => {
    expect(formatMinutes(30)).toBe('30 min')
  })

  it('formats 75 minutes as "1 hr 15 min"', () => {
    expect(formatMinutes(75)).toBe('1 hr 15 min')
  })

  it('formats 60 minutes as "1 hr"', () => {
    expect(formatMinutes(60)).toBe('1 hr')
  })

  it('formats 0 minutes as "0 min"', () => {
    expect(formatMinutes(0)).toBe('0 min')
  })
})

describe('formatCountdown', () => {
  it('formats 90 seconds as "01:30"', () => {
    expect(formatCountdown(90)).toBe('01:30')
  })

  it('formats 0 seconds as "00:00"', () => {
    expect(formatCountdown(0)).toBe('00:00')
  })

  it('formats 605 seconds as "10:05"', () => {
    expect(formatCountdown(605)).toBe('10:05')
  })
})
