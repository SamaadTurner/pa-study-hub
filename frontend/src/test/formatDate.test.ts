import { describe, it, expect } from 'vitest'
import { formatDate, formatShortDate, formatRelativeDate, getGreeting } from '@/utils/formatDate'

// Use date-only strings (no time component) so parseISO treats them as local midnight,
// avoiding timezone-offset issues where UTC midnight can roll back to the previous day.

describe('formatDate', () => {
  it('formats ISO date string as "Feb 9, 2026"', () => {
    expect(formatDate('2026-02-09')).toBe('Feb 9, 2026')
  })

  it('formats a different date correctly', () => {
    expect(formatDate('2025-12-25')).toBe('Dec 25, 2025')
  })

  it('formats a single-digit day without leading zero', () => {
    expect(formatDate('2026-01-01')).toBe('Jan 1, 2026')
  })
})

describe('formatShortDate', () => {
  it('formats as "Feb 9" without year', () => {
    expect(formatShortDate('2026-02-09')).toBe('Feb 9')
  })
})

// Build a local YYYY-MM-DD string (avoids UTC offset causing day shifts)
function localDateString(offsetDays = 0): string {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

describe('formatRelativeDate', () => {
  it('returns "Today" for today\'s date', () => {
    expect(formatRelativeDate(localDateString(0))).toBe('Today')
  })

  it('returns "Yesterday" for yesterday', () => {
    expect(formatRelativeDate(localDateString(-1))).toBe('Yesterday')
  })

  it('returns "Tomorrow" for tomorrow', () => {
    expect(formatRelativeDate(localDateString(1))).toBe('Tomorrow')
  })

  it('returns formatted date for older dates', () => {
    expect(formatRelativeDate('2024-06-15')).toBe('Jun 15, 2024')
  })
})

describe('getGreeting', () => {
  it('returns a valid greeting string', () => {
    const greeting = getGreeting()
    expect(['Good morning', 'Good afternoon', 'Good evening']).toContain(greeting)
  })
})
