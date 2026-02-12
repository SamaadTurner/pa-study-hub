import { describe, it, expect } from 'vitest'
import { getCategoryColor, getCategoryLabel, getCategoryBadgeVariant, ALL_CATEGORIES } from '@/utils/categoryColors'

describe('getCategoryColor', () => {
  it('returns the correct hex color for CARDIOLOGY', () => {
    expect(getCategoryColor('CARDIOLOGY')).toBe('#ef4444')
  })

  it('returns the correct hex color for NEUROLOGY', () => {
    expect(getCategoryColor('NEUROLOGY')).toBe('#a855f7')
  })

  it('returns the correct hex color for PHARMACOLOGY', () => {
    expect(getCategoryColor('PHARMACOLOGY')).toBe('#4f46e5')
  })

  it('returns the correct hex color for ANATOMY', () => {
    expect(getCategoryColor('ANATOMY')).toBe('#6b7280')
  })

  it('returns gray fallback for unknown category', () => {
    expect(getCategoryColor('UNKNOWN')).toBe('#6b7280')
  })
})

describe('getCategoryLabel', () => {
  it('returns "Cardiology" for CARDIOLOGY', () => {
    expect(getCategoryLabel('CARDIOLOGY')).toBe('Cardiology')
  })

  it('returns "Infectious Disease" for INFECTIOUS_DISEASE', () => {
    expect(getCategoryLabel('INFECTIOUS_DISEASE')).toBe('Infectious Disease')
  })

  it('returns "Emergency Medicine" for EMERGENCY_MEDICINE', () => {
    expect(getCategoryLabel('EMERGENCY_MEDICINE')).toBe('Emergency Medicine')
  })

  it('returns the key itself for unknown categories', () => {
    expect(getCategoryLabel('UNKNOWN_CAT')).toBe('UNKNOWN_CAT')
  })
})

describe('getCategoryBadgeVariant', () => {
  it('returns "red" for CARDIOLOGY', () => {
    expect(getCategoryBadgeVariant('CARDIOLOGY')).toBe('red')
  })

  it('returns "purple" for NEUROLOGY', () => {
    expect(getCategoryBadgeVariant('NEUROLOGY')).toBe('purple')
  })

  it('returns "gray" fallback for unknown category', () => {
    expect(getCategoryBadgeVariant('UNKNOWN')).toBe('gray')
  })
})

describe('ALL_CATEGORIES', () => {
  it('contains all 17 NCCPA categories', () => {
    expect(ALL_CATEGORIES).toHaveLength(17)
  })

  it('includes CARDIOLOGY', () => {
    expect(ALL_CATEGORIES).toContain('CARDIOLOGY')
  })

  it('includes EMERGENCY_MEDICINE', () => {
    expect(ALL_CATEGORIES).toContain('EMERGENCY_MEDICINE')
  })
})
