import { describe, it, expect } from 'vitest'
import type { GoalProgress } from '@/types'

// Helper function extracted for testability
function calculateGoalPercentage(current: number, target: number): number {
  if (target === 0) return 100
  return Math.min(100, Math.round((current / target) * 100))
}

function isGoalMet(progress: GoalProgress): boolean {
  return progress.cardGoalMet && progress.minuteGoalMet
}

describe('Goal progress calculation', () => {
  it('22/30 minutes calculates to 73%', () => {
    expect(calculateGoalPercentage(22, 30)).toBe(73)
  })

  it('30/30 minutes calculates to 100%', () => {
    expect(calculateGoalPercentage(30, 30)).toBe(100)
  })

  it('0/30 calculates to 0%', () => {
    expect(calculateGoalPercentage(0, 30)).toBe(0)
  })

  it('exceeding target caps at 100%', () => {
    expect(calculateGoalPercentage(35, 30)).toBe(100)
  })

  it('zero target returns 100% (no goal set)', () => {
    expect(calculateGoalPercentage(5, 0)).toBe(100)
  })

  it('all targets met returns goalMet true', () => {
    const progress: GoalProgress = {
      targetCardsPerDay: 20,
      targetMinutesPerDay: 30,
      todayCardsReviewed: 20,
      todayMinutesStudied: 30,
      cardGoalMet: true,
      minuteGoalMet: true,
    }
    expect(isGoalMet(progress)).toBe(true)
  })

  it('partial completion returns goalMet false', () => {
    const progress: GoalProgress = {
      targetCardsPerDay: 20,
      targetMinutesPerDay: 30,
      todayCardsReviewed: 10,
      todayMinutesStudied: 30,
      cardGoalMet: false,
      minuteGoalMet: true,
    }
    expect(isGoalMet(progress)).toBe(false)
  })
})
